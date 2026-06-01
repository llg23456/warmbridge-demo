"""抖音 / B 站链接识别（通俗视频生成入口校验）。"""

from __future__ import annotations

from urllib.parse import urlparse

_BILIBILI = (
    "bilibili.com",
    "www.bilibili.com",
    "m.bilibili.com",
    "b23.tv",
)
_DOUYIN = (
    "douyin.com",
    "www.douyin.com",
    "v.douyin.com",
    "iesdouyin.com",
)


def _host(url: str) -> str:
    try:
        return (urlparse(url.strip()).hostname or "").lower()
    except Exception:
        return ""


def is_bilibili(url: str) -> bool:
    h = _host(url)
    return any(h == d or h.endswith("." + d) for d in _BILIBILI)


def is_douyin(url: str) -> bool:
    h = _host(url)
    return any(h == d or h.endswith("." + d) for d in _DOUYIN)


def is_supported_video_url(url: str) -> bool:
    u = (url or "").strip()
    if not u.startswith(("http://", "https://")):
        return False
    return is_bilibili(u) or is_douyin(u)


def platform_label(url: str) -> str:
    if is_bilibili(url):
        return "哔哩哔哩"
    if is_douyin(url):
        return "抖音"
    return "视频"
