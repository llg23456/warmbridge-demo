"""vivo §6 视频生成（D3 图生视频片头：submit_task + 轮询 query_task）。"""

from __future__ import annotations

import asyncio
import logging
import time
import uuid
from dataclasses import dataclass, field
from typing import Any

import httpx

from app.config import settings

_log = logging.getLogger(__name__)

_SUBMIT_API = "https://api-ai.vivo.com.cn/api/v1/submit_task"
_QUERY_API = "https://api-ai.vivo.com.cn/api/v1/query_task"

_SUCCESS_STATUS = frozenset({"succeeded", "success", "completed", "done"})
_FAIL_STATUS = frozenset({"failed", "error", "cancelled", "canceled"})


class VideoQuotaExceededError(RuntimeError):
    def __init__(self, code: int, message: str, *, body: dict[str, Any] | None = None) -> None:
        self.code = code
        self.body = body or {}
        super().__init__(f"图生视频配额/限流 code={code} {message}")


@dataclass
class VivoVideoTaskResult:
    task_id: str = ""
    video_url: str = ""
    status: str = ""
    skipped: bool = False
    skip_reason: str = ""
    error: str = ""
    raw: dict[str, Any] = field(default_factory=dict)


def _public_cdn_url(url: str) -> bool:
    u = (url or "").strip().lower()
    if not u.startswith(("http://", "https://")):
        return False
    if u.startswith(("http://10.", "http://192.168.", "http://127.", "http://localhost")):
        return False
    if "://10." in u or "://192.168." in u:
        return False
    return True


def _auth_headers() -> dict[str, str]:
    key = (settings.vivo_app_key or "").strip()
    if not key:
        raise RuntimeError("未配置 VIVO_APP_KEY")
    return {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json; charset=utf-8",
    }


def _query_params() -> dict[str, str | int]:
    return {
        "module": "aigc",
        "request_id": str(uuid.uuid4()),
        "system_time": int(time.time()),
    }


def _ensure_video_prompt(prompt: str, *, duration_sec: int) -> str:
    p = (prompt or "").strip()
    if not p:
        p = "gentle cinematic documentary intro, slow camera movement, warm lighting"
    low = p.lower()
    if "--dur" not in low:
        p = f"{p} --dur {duration_sec}"
    if "--ratio" not in low:
        p = f"{p} --ratio 16:9"
    return p[:800]


def _extract_video_url(data: dict[str, Any]) -> str:
    content = data.get("content")
    if isinstance(content, dict):
        u = content.get("video_url") or content.get("url")
        if isinstance(u, str) and u.startswith(("http://", "https://")):
            return u.strip()
    return ""


async def submit_vivo_video(
    *,
    first_frame_url: str,
    prompt: str,
    duration_sec: int | None = None,
) -> VivoVideoTaskResult:
    """
    图生视频（首帧）：first_frame_url 须为 vivo 云端可访问的公网 CDN（如文生图返回的 xuanji URL）。
    局域网 cover.jpg 不可用。
    """
    dur = duration_sec if duration_sec is not None else settings.vivo_video_duration_sec
    frame = (first_frame_url or "").strip()
    if not _public_cdn_url(frame):
        reason = "first_frame 非公网 CDN，跳过 D3（可用文生图 img1 URL）"
        _log.info("WbVideoGen vivo video skip: %s url=%s", reason, frame[:80])
        return VivoVideoTaskResult(skipped=True, skip_reason=reason)

    text = _ensure_video_prompt(prompt, duration_sec=int(dur))
    payload: dict[str, Any] = {
        "model": settings.vivo_video_model,
        "content": [
            {"type": "text", "text": text},
            {
                "type": "image_url",
                "image_url": {"url": frame},
                "role": "first_frame",
            },
        ],
    }
    api_url = (settings.vivo_video_submit_url or _SUBMIT_API).strip()
    _log.info(
        "WbVideoGen vivo video submit model=%s dur=%s frame=%s prompt_len=%s",
        settings.vivo_video_model,
        dur,
        frame[:80],
        len(text),
    )

    async with httpx.AsyncClient(timeout=float(settings.vivo_video_submit_timeout_sec)) as client:
        r = await client.post(
            api_url,
            params=_query_params(),
            headers=_auth_headers(),
            json=payload,
        )
        body: dict[str, Any]
        try:
            body = r.json()
        except Exception:
            body = {}
        if r.status_code >= 400:
            _log.warning("WbVideoGen vivo video submit HTTP %s body=%s", r.status_code, (r.text or "")[:400])
            r.raise_for_status()

    code = body.get("code")
    if code != 0:
        msg = body.get("message") or body.get("msg") or str(body)[:300]
        _log.warning("WbVideoGen vivo video submit fail code=%s msg=%s", code, msg)
        if code == 1003:
            raise VideoQuotaExceededError(int(code or 1003), str(msg), body=body)
        return VivoVideoTaskResult(
            skipped=True,
            skip_reason=f"submit fail code={code} {msg}",
            raw=body,
        )

    data = body.get("data") if isinstance(body.get("data"), dict) else {}
    task_id = str(data.get("id") or data.get("task_id") or "").strip()
    if not task_id:
        return VivoVideoTaskResult(
            skipped=True,
            skip_reason="submit ok but no task id",
            raw=body,
        )
    _log.info("WbVideoGen vivo video submitted task_id=%s", task_id)
    return VivoVideoTaskResult(task_id=task_id, raw=body)


async def poll_vivo_video_task(task_id: str) -> VivoVideoTaskResult:
    """轮询 query_task 直至 succeeded / failed 或超时。"""
    tid = (task_id or "").strip()
    if not tid:
        return VivoVideoTaskResult(skipped=True, skip_reason="empty task_id")

    api_url = (settings.vivo_video_query_url or _QUERY_API).strip()
    max_attempts = max(int(settings.vivo_video_poll_max_attempts), 1)
    interval = max(float(settings.vivo_video_poll_interval_sec), 2.0)

    async with httpx.AsyncClient(timeout=float(settings.vivo_video_query_timeout_sec)) as client:
        for attempt in range(1, max_attempts + 1):
            params = _query_params()
            params["task_id"] = tid
            r = await client.get(api_url, params=params, headers=_auth_headers())
            body: dict[str, Any]
            try:
                body = r.json()
            except Exception:
                body = {}
            if r.status_code >= 400:
                _log.warning(
                    "WbVideoGen vivo video query HTTP %s task=%s body=%s",
                    r.status_code,
                    tid,
                    (r.text or "")[:300],
                )
                r.raise_for_status()

            code = body.get("code")
            if code == 1003:
                msg = body.get("message") or body.get("msg") or "rate limit"
                raise VideoQuotaExceededError(int(code), str(msg), body=body)
            if code != 0:
                msg = body.get("message") or body.get("msg") or str(body)[:200]
                if code == 3002:
                    return VivoVideoTaskResult(
                        task_id=tid,
                        skipped=True,
                        skip_reason=f"task not found: {msg}",
                        raw=body,
                    )
                _log.warning("WbVideoGen vivo video query code=%s task=%s msg=%s", code, tid, msg)

            data = body.get("data") if isinstance(body.get("data"), dict) else {}
            status = str(data.get("status") or "").strip().lower()
            video_url = _extract_video_url(data)
            err = data.get("error")
            err_s = str(err).strip() if err else ""

            _log.info(
                "WbVideoGen vivo video poll attempt=%s/%s task=%s status=%s",
                attempt,
                max_attempts,
                tid,
                status or "(pending)",
            )

            if status in _SUCCESS_STATUS and video_url:
                return VivoVideoTaskResult(
                    task_id=tid,
                    video_url=video_url,
                    status=status,
                    raw=body,
                )
            if status in _FAIL_STATUS or err_s:
                return VivoVideoTaskResult(
                    task_id=tid,
                    status=status or "failed",
                    skipped=True,
                    skip_reason=err_s or f"status={status}",
                    error=err_s,
                    raw=body,
                )

            if attempt < max_attempts:
                await asyncio.sleep(interval)

    return VivoVideoTaskResult(
        task_id=tid,
        skipped=True,
        skip_reason=f"poll timeout after {max_attempts} attempts",
    )


async def download_video_bytes(url: str) -> bytes:
    async with httpx.AsyncClient(follow_redirects=True, timeout=120.0) as client:
        r = await client.get(
            url,
            headers={
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                ),
            },
        )
        r.raise_for_status()
        data = r.content[:80_000_000]
        if len(data) < 2000:
            raise RuntimeError("图生视频下载过小")
        return data


async def generate_intro_video(
    work: Path,
    *,
    first_frame_url: str,
    prompt: str,
    duration_sec: int | None = None,
) -> Path | None:
    """提交 → 轮询 → 下载 intro.mp4；失败返回 None（静默降级）。"""
    submitted = await submit_vivo_video(
        first_frame_url=first_frame_url,
        prompt=prompt,
        duration_sec=duration_sec,
    )
    if submitted.skipped or not submitted.task_id:
        _log.info("WbVideoGen intro skip: %s", submitted.skip_reason)
        return None

    polled = await poll_vivo_video_task(submitted.task_id)
    if polled.skipped or not polled.video_url:
        _log.warning("WbVideoGen intro poll fail task=%s reason=%s", submitted.task_id, polled.skip_reason)
        return None

    dest = work / "intro.mp4"
    dest.write_bytes(await download_video_bytes(polled.video_url))
    _log.info("WbVideoGen intro ok bytes=%s task=%s", dest.stat().st_size, submitted.task_id)
    return dest
