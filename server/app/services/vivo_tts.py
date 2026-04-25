"""vivo §14 TTS：WebSocket 流式 PCM → 合并为 WAV（24kHz / 16bit / mono）。"""

from __future__ import annotations

import base64
import json
import time
import urllib.parse
import uuid
from typing import Any

import websockets

from app.config import settings


def _pcm_to_wav(pcm: bytes, sample_rate: int = 24000) -> bytes:
    import struct

    nchannels = 1
    sampwidth = 2
    nframes = len(pcm) // (nchannels * sampwidth)
    data_size = nframes * nchannels * sampwidth
    fmt_chunk = struct.pack(
        "<HHIIHH",
        1,
        nchannels,
        sample_rate,
        sample_rate * nchannels * sampwidth,
        nchannels * sampwidth,
        16,
    )
    header = (
        b"RIFF"
        + struct.pack("<I", 36 + data_size)
        + b"WAVEfmt "
        + struct.pack("<I", 16)
        + fmt_chunk
        + b"data"
        + struct.pack("<I", data_size)
    )
    return header + pcm[:data_size]


def _clip_tts_text(text: str, max_bytes: int = 1900) -> str:
    t = (text or "").strip()
    if not t:
        return ""
    b = t.encode("utf-8")
    if len(b) <= max_bytes:
        return t
    out = []
    size = 0
    for ch in t:
        cb = ch.encode("utf-8")
        if size + len(cb) > max_bytes:
            break
        out.append(ch)
        size += len(cb)
    return "".join(out).rstrip() + "……"


def _as_int(v: Any, default: int = -1) -> int:
    try:
        if v is None:
            return default
        return int(v)
    except (TypeError, ValueError):
        return default


def _decode_ws_text(raw: str | bytes) -> str:
    if isinstance(raw, (bytes, bytearray)):
        return raw.decode("utf-8", errors="replace")
    return raw


def _absorb_audio_chunk(msg: dict[str, Any], pcm: bytearray) -> bool:
    """解析一帧 JSON；若 data.status==2 表示合成结束。返回 True 表示应停止接收。"""
    chunk = msg.get("data")
    if not isinstance(chunk, dict):
        chunk = {}
    audio_b64 = chunk.get("audio")
    if audio_b64:
        pcm.extend(base64.b64decode(audio_b64))
    st = chunk.get("status")
    return st == 2 or st == "2"


async def synthesize_wav(text: str) -> bytes:
    key = (settings.vivo_app_key or "").strip()
    if not key:
        raise RuntimeError("未配置 VIVO_APP_KEY，无法调用 TTS。")
    clipped = _clip_tts_text(text)
    if not clipped:
        raise RuntimeError("朗读文本为空。")

    rid = str(uuid.uuid4())
    uid = uuid.uuid4().hex[:32]
    # §14 表：Query 含 requestId；公参勿全用 unknown
    q: dict[str, str] = {
        "engineid": (settings.vivo_tts_engineid or "").strip(),
        "system_time": str(int(time.time())),
        "user_id": uid,
        "model": "WarmBridgeServer",
        "product": "warmbridge",
        "package": "com.warmbridge.demo",
        "client_version": "1.0",
        "system_version": "Linux",
        "sdk_version": "1.0",
        "android_version": "34",
        "requestId": rid,
    }
    uri = "wss://api-ai.vivo.com.cn/tts?" + urllib.parse.urlencode(q)

    # §14 文档：Authorization + X-AI-GATEWAY-SIGNATURE 必须；缺签名时握手常直接 HTTP 400
    hdrs: list[tuple[str, str]] = [
        ("Authorization", f"Bearer {key}"),
        ("X-AI-GATEWAY-SIGNATURE", "developers-aigc"),
    ]
    # 官方 tts_examples.open 固定带 vaid；未配置时用与示例相同的占位，勿把 APP_ID 当 vaid
    vaid = (settings.vivo_tts_vaid or "").strip() or "123456789"
    hdrs.append(("vaid", vaid))

    pcm = bytearray()
    # compression=None：避免默认 permessage-deflate 导致部分网关握手直接 HTTP 400
    async with websockets.connect(
        uri,
        extra_headers=dict(hdrs),
        compression=None,
        user_agent_header=(
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 "
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        ),
        max_size=16 * 1024 * 1024,
        open_timeout=30,
    ) as ws:
        raw_first = await ws.recv()
        raw_first = _decode_ws_text(raw_first)
        try:
            first: dict[str, Any] = json.loads(raw_first)
        except json.JSONDecodeError as e:
            raise RuntimeError(f"TTS 首包非 JSON，片段：{raw_first[:240]}") from e

        if _as_int(first.get("error_code"), -1) != 0:
            raise RuntimeError(
                str(first.get("error_msg") or first.get("message") or "TTS 握手失败")
            )

        if _absorb_audio_chunk(first, pcm):
            if not pcm:
                raise RuntimeError("TTS 握手帧标记结束但未收到音频，请核对 engineid/vcn 与控制台权限。")
            return _pcm_to_wav(bytes(pcm))

        synth = {
            "aue": 0,
            "auf": "audio/L16;rate=24000",
            "vcn": settings.vivo_tts_vcn,
            "speed": 50,
            "volume": 55,
            "text": base64.b64encode(clipped.encode("utf-8")).decode("ascii"),
            "encoding": "utf8",
            "reqId": int(time.time() * 1000),
        }
        await ws.send(json.dumps(synth, ensure_ascii=False))

        finished = False
        while True:
            raw = await ws.recv()
            raw = _decode_ws_text(raw)
            try:
                msg: dict[str, Any] = json.loads(raw)
            except json.JSONDecodeError as e:
                raise RuntimeError(f"TTS 下行非 JSON，片段：{raw[:240]}") from e

            ec = _as_int(msg.get("error_code"), 0)
            chunk = msg.get("data")
            if not isinstance(chunk, dict):
                chunk = {}
            if ec != 0 and not chunk.get("audio"):
                raise RuntimeError(str(msg.get("error_msg") or msg.get("message") or "TTS 合成错误"))

            if _absorb_audio_chunk(msg, pcm):
                finished = True
                break

        if not finished or not pcm:
            raise RuntimeError(
                "未收到完整 TTS 音频流。请确认账号已开通 §14 长音频合成，且 engineid="
                f"{settings.vivo_tts_engineid} 与 vcn={settings.vivo_tts_vcn} 匹配；也可对照官网 tts_examples.py 抓包比对。"
            )

    return _pcm_to_wav(bytes(pcm))
