"""vivo §5 文生图（网关 api-ai.vivo.com.cn；底层 Doubao 要求 size ≥ 1920×1920）。"""

from __future__ import annotations

import logging
import re
import time
import uuid
from dataclasses import dataclass, field
from typing import Any

import httpx

from app.config import settings

_log = logging.getLogger(__name__)


class ContentPolicyError(RuntimeError):
    """vivo/Seedream 内容安全拦截（常见 code=1004）。"""

    def __init__(self, code: int, message: str, *, size: str = "") -> None:
        self.code = code
        self.size = size
        super().__init__(f"文生图内容策略拦截 code={code} size={size} {message}")


class QuotaExceededError(RuntimeError):
    """vivo 限流/配额用尽（常见 code=1003）。"""

    def __init__(self, code: int, message: str, *, body: dict[str, Any] | None = None) -> None:
        self.code = code
        self.body = body or {}
        super().__init__(f"文生图配额/限流 code={code} {message}")


# Doubao-Seedream 经 vivo 转发时的最小像素（1920×1920）
_MIN_PIXELS = 3686400
_VIVO_IMAGE_API = "https://api-ai.vivo.com.cn/api/v1/image_generation"
# 文档 §5 推荐；合成层会用 ffmpeg 再裁成 16:9
_DEFAULT_SIZE = "2K"


@dataclass
class ImageGenResult:
    urls: list[str] = field(default_factory=list)
    image_count: int = 0
    size: str = ""


def _parse_size_pixels(size: str) -> int | None:
    m = re.match(r"^(\d+)\s*[xX×]\s*(\d+)$", (size or "").strip())
    if not m:
        return None
    return int(m.group(1)) * int(m.group(2))


def resolve_image_size(requested: str | None = None) -> str:
    """
    解析文生图 size。1280x720 等小于 3686400 像素的值自动升为 2K。
    文档 §5 可用：2K、2048x2048 等。
    """
    raw = (requested or settings.vivo_image_size or _DEFAULT_SIZE).strip() or _DEFAULT_SIZE
    low = raw.lower().replace(" ", "")
    if low in ("1280x720", "1024x576", "720p", "1080p", "hd"):
        _log.warning("WbVideoGen image size %r too small for Seedream, using 2K", raw)
        return _DEFAULT_SIZE
    px = _parse_size_pixels(raw)
    if px is not None and px < _MIN_PIXELS:
        _log.warning(
            "WbVideoGen image size %r = %s px < %s, using 2K",
            raw,
            px,
            _MIN_PIXELS,
        )
        return _DEFAULT_SIZE
    return raw


def _assert_vivo_endpoint(api_url: str) -> None:
    u = (api_url or "").strip().lower()
    if "volces.com" in u or "ark.cn-beijing" in u:
        raise RuntimeError(
            f"文生图 URL 误配为火山方舟 ({api_url[:80]})，"
            f"请改 .env 为 VIVO_IMAGE_URL={_VIVO_IMAGE_API}"
        )
    if "api-ai.vivo.com.cn" not in u:
        _log.warning("WbVideoGen image url not vivo default: %s", api_url[:100])


def _url_from_image_item(item: Any) -> str:
    if isinstance(item, str) and item.startswith(("http://", "https://")):
        return item.strip()
    if isinstance(item, dict):
        u = item.get("url") or item.get("image_url")
        if isinstance(u, str) and u.startswith(("http://", "https://")):
            return u.strip()
    return ""


def _extract_image_urls(body: dict[str, Any]) -> ImageGenResult:
    """标准路径 data.images[]；废弃字段 data.image 仅作兜底。"""
    block = body.get("data")
    if not isinstance(block, dict):
        return ImageGenResult()

    urls: list[str] = []
    images = block.get("images")
    if isinstance(images, list):
        for item in images:
            u = _url_from_image_item(item)
            if u:
                urls.append(u)

    if not urls:
        legacy = block.get("image")
        if legacy:
            u = _url_from_image_item(legacy)
            if u:
                _log.warning("WbVideoGen image response used deprecated data.image; prefer data.images[]")
                urls.append(u)

    usage = block.get("usage")
    image_count = len(urls)
    if isinstance(usage, dict) and usage.get("image_count") is not None:
        try:
            image_count = max(image_count, int(usage["image_count"]))
        except (TypeError, ValueError):
            pass

    return ImageGenResult(urls=urls, image_count=image_count or len(urls))


def _build_parameters(
    img_size: str,
    *,
    sequential_image_generation: str | None = None,
    sequential_image_generation_options: dict[str, Any] | None = None,
) -> dict[str, Any]:
    params: dict[str, Any] = {"size": img_size}
    seq = (sequential_image_generation or "disabled").strip().lower()
    if seq not in ("auto", "disabled"):
        seq = "disabled"
    params["sequential_image_generation"] = seq
    if sequential_image_generation_options:
        params["sequential_image_generation_options"] = dict(sequential_image_generation_options)
    return params


async def generate_images(
    prompt: str,
    *,
    size: str | None = None,
    sequential_image_generation: str = "disabled",
    sequential_image_generation_options: dict[str, Any] | None = None,
) -> ImageGenResult:
    """提交文生图，返回全部图片 URL 与 usage.image_count。"""
    key = (settings.vivo_app_key or "").strip()
    if not key:
        raise RuntimeError("未配置 VIVO_APP_KEY")
    p = (prompt or "").strip()
    if len(p) < 8:
        raise RuntimeError("文生图 prompt 过短")
    if "16:9" not in p.lower() and "landscape" not in p.lower():
        p = f"{p}, widescreen 16:9 landscape, cinematic photography"

    img_size = resolve_image_size(size)
    api_url = (settings.vivo_image_url or _VIVO_IMAGE_API).strip()
    _assert_vivo_endpoint(api_url)

    payload: dict[str, Any] = {
        "model": settings.vivo_image_model,
        "prompt": p[:800],
        "parameters": _build_parameters(
            img_size,
            sequential_image_generation=sequential_image_generation,
            sequential_image_generation_options=sequential_image_generation_options,
        ),
    }
    params = {
        "module": "aigc",
        "request_id": str(uuid.uuid4()),
        "system_time": int(time.time()),
    }
    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json; charset=utf-8",
    }

    prompt_preview = p[:120].replace("\n", " ")
    _log.info(
        "WbVideoGen image request url=%s model=%s size=%s seq=%s prompt_len=%s preview=%s",
        api_url,
        settings.vivo_image_model,
        img_size,
        payload["parameters"].get("sequential_image_generation"),
        len(p),
        prompt_preview,
    )

    async with httpx.AsyncClient(timeout=float(settings.vivo_image_timeout_sec)) as client:
        r = await client.post(api_url, params=params, headers=headers, json=payload)
        body: dict[str, Any]
        try:
            body = r.json()
        except Exception:
            body = {}
        if r.status_code >= 400:
            snippet = (r.text or "")[:400]
            _log.warning(
                "WbVideoGen image HTTP %s url=%s size=%s body=%s",
                r.status_code,
                api_url,
                img_size,
                snippet,
            )
            r.raise_for_status()

    code = body.get("code")
    if code != 0:
        msg = body.get("message") or body.get("msg") or str(body)[:300]
        _log.warning(
            "WbVideoGen image fail code=%s size=%s url=%s preview=%s msg=%s",
            code,
            img_size,
            api_url,
            prompt_preview,
            msg,
        )
        if code == 1004 or "violates policy" in str(msg).lower():
            raise ContentPolicyError(int(code or 1004), str(msg), size=img_size)
        if code == 1003 or "rate_limit" in str(body).lower() or "quota" in str(msg).lower():
            raise QuotaExceededError(int(code or 1003), str(msg), body=body)
        raise RuntimeError(f"文生图失败 code={code} size={img_size} {msg}")

    result = _extract_image_urls(body)
    result.size = img_size
    if not result.urls:
        raise RuntimeError(f"文生图响应无 images URL (size={img_size})")
    _log.info(
        "WbVideoGen image ok size=%s count=%s first=%s",
        img_size,
        result.image_count or len(result.urls),
        result.urls[0][:80],
    )
    return result


async def generate_image_url(
    prompt: str,
    *,
    size: str | None = None,
    sequential_image_generation: str = "disabled",
    sequential_image_generation_options: dict[str, Any] | None = None,
) -> str:
    """提交文生图，返回首张图片 URL（data.images[0].url）。"""
    result = await generate_images(
        prompt,
        size=size,
        sequential_image_generation=sequential_image_generation,
        sequential_image_generation_options=sequential_image_generation_options,
    )
    return result.urls[0]


async def download_image_bytes(url: str) -> bytes:
    async with httpx.AsyncClient(follow_redirects=True, timeout=60.0) as client:
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
        data = r.content[:8_000_000]
        if len(data) < 500:
            raise RuntimeError("文生图下载过小")
        return data
