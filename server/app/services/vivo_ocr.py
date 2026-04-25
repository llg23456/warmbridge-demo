"""vivo 通用 OCR §8：application/x-www-form-urlencoded + base64 图。"""

from __future__ import annotations

import base64
import json
import logging
import uuid
from typing import Any

import httpx

from app.config import settings

_log = logging.getLogger(__name__)


def _extract_text_from_result(result: Any) -> str:
    """兼容多种 result 结构（words 字符串/数组、OCR 块、JSON 字符串、嵌套 data 等）。"""
    if result is None:
        return ""
    if isinstance(result, str):
        s = result.strip()
        if s.startswith("[") or s.startswith("{"):
            try:
                return _extract_text_from_result(json.loads(s))
            except json.JSONDecodeError:
                pass
        return s
    if isinstance(result, list):
        parts: list[str] = []
        for item in result:
            t = _extract_text_from_result(item)
            if t:
                parts.append(t)
        return "".join(parts).strip()
    if not isinstance(result, dict):
        return ""
    for k in ("text", "content", "full_text", "all_text", "recognize_text", "Word", "word"):
        v = result.get(k)
        if isinstance(v, str) and v.strip():
            return v.strip()
    words = result.get("words")
    if isinstance(words, str) and words.strip():
        return _extract_text_from_result(words)
    if isinstance(words, list):
        parts: list[str] = []
        for w in words:
            if isinstance(w, str):
                parts.append(w)
            elif isinstance(w, dict):
                # 官方 §8 pos=0：words 为 [{"words":"一行"}, ...]（键名为 words，不是 word）
                t = (
                    w.get("words")
                    or w.get("word")
                    or w.get("text")
                    or w.get("chars")
                    or w.get("content")
                    or w.get("char")
                    or w.get("d")
                )
                if isinstance(t, str) and t.strip():
                    parts.append(t.strip())
                elif t is not None:
                    nested = _extract_text_from_result(t)
                    if nested:
                        parts.append(nested)
        return "\n".join(parts).strip()
    for key in ("OCR", "ocr", "blocks", "lines", "line_words", "char_list", "CharList"):
        ocr_list = result.get(key)
        if isinstance(ocr_list, list):
            parts = []
            for blk in ocr_list:
                if isinstance(blk, dict):
                    t = (
                        blk.get("words")
                        or blk.get("word")
                        or blk.get("text")
                        or blk.get("chars")
                        or blk.get("char")
                        or blk.get("d")
                        or blk.get("c")
                    )
                    if isinstance(t, str) and t.strip():
                        parts.append(t.strip())
                    else:
                        nested = _extract_text_from_result(blk)
                        if nested:
                            parts.append(nested)
                elif isinstance(blk, str):
                    parts.append(blk)
            if parts:
                return "\n".join(parts).strip()
    nested = result.get("data") or result.get("result")
    if nested is not None and nested is not result:
        return _extract_text_from_result(nested)
    return ""


def _unwrap_result(payload: dict[str, Any]) -> Any:
    r = payload.get("result")
    if r is not None:
        return r
    data = payload.get("data")
    if isinstance(data, dict):
        return data.get("result") or data.get("ocr_result") or data.get("text") or data
    if isinstance(data, str):
        return data
    return None


def _ocr_success(payload: dict[str, Any]) -> bool:
    """文档为 error_code==0；兼容 code / ret；若无状态字段但有 result/data 则视为成功。"""
    keys = ("error_code", "err_code", "code", "ret", "status")
    seen: list[Any] = []
    for k in keys:
        if k not in payload:
            continue
        v = payload[k]
        seen.append(v)
        try:
            if int(v) == 0:
                return True
        except (TypeError, ValueError):
            if str(v).strip() in ("0", "success", "SUCCESS", "ok", "OK"):
                return True
    if seen:
        return False
    return payload.get("result") is not None or payload.get("data") is not None


async def _ocr_post(
    client: httpx.AsyncClient,
    url: str,
    b64: str,
    pos: str,
    bid: str,
    request_id: str,
) -> dict[str, Any]:
    data = {"image": b64, "pos": pos, "businessid": bid}
    headers = {
        "Authorization": f"Bearer {(settings.vivo_app_key or '').strip()}",
        "Content-Type": "application/x-www-form-urlencoded",
    }
    # §8 表仅列 requestId；勿与 Chat 的 request_id 双参混传，避免网关拒识
    r = await client.post(
        url,
        params={"requestId": request_id},
        headers=headers,
        data=data,
    )
    r.raise_for_status()
    return r.json()


async def _run_ocr_once(b64: str, pos: str, businessid: str) -> str:
    url = (settings.vivo_ocr_url or "").strip() or "http://api-ai.vivo.com.cn/ocr/general_recognition"
    request_id = str(uuid.uuid4())
    try:
        async with httpx.AsyncClient(timeout=60.0, follow_redirects=True) as client:
            payload = await _ocr_post(client, url, b64, pos, businessid, request_id)
    except httpx.HTTPStatusError as e:
        body = (e.response.text or "")[:500]
        raise RuntimeError(f"OCR 网关 HTTP {e.response.status_code}，响应片段：{body}") from e

    if not _ocr_success(payload):
        try:
            ec = int(payload.get("error_code", payload.get("code", -1)))
        except (TypeError, ValueError):
            ec = -1
        msg = payload.get("error_msg") or payload.get("message") or payload.get("msg") or "ocr fail"
        raise RuntimeError(
            f"OCR 失败（error_code={ec}）：{msg}。"
            f"可换 JPG/PNG；businessid 可试文档「aigc+AppId」或另一固定串；或设 VIVO_OCR_POS=2。"
        )

    raw = _unwrap_result(payload)
    text = _extract_text_from_result(raw)
    if not (text or "").strip():
        _log.warning(
            "OCR 判定成功但解析不到文字，pos=%s businessid 前8位=%s…，payload 摘要=%s",
            pos,
            businessid[:8],
            str(payload)[:900],
        )
    return text


def _aigc_businessid() -> str:
    aid = (settings.vivo_app_id or "").strip()
    if not aid:
        return ""
    return f"aigc{aid}"


async def ocr_image_bytes(image: bytes) -> str:
    key = (settings.vivo_app_key or "").strip()
    bid = (settings.vivo_ocr_businessid or "").strip()
    if not key:
        raise RuntimeError("未配置 VIVO_APP_KEY，无法调用 OCR。")
    if not bid:
        raise RuntimeError(
            "未配置 VIVO_OCR_BUSINESSID。请在 server/.env 填写 §8 文档中的 businessid 固定串或 aigc+AppId。"
        )
    b64 = base64.b64encode(image).decode("ascii")
    pos = (settings.vivo_ocr_pos or "0").strip() or "0"

    async def run_pos_chain(businessid: str) -> str:
        t = await _run_ocr_once(b64, pos, businessid)
        if not (t or "").strip() and pos == "0":
            t = await _run_ocr_once(b64, "2", businessid)
        return t

    text = await run_pos_chain(bid)
    # 文档主表：businessid 可为 "aigc" + AppId；与固定串二选一，无字时自动换试一次
    alt = _aigc_businessid()
    if not (text or "").strip() and alt and alt != bid:
        _log.info("OCR 在固定 businessid 下无字，改用 aigc+AppId 再试")
        text = await run_pos_chain(alt)

    return text
