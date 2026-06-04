"""抓取分享链接的标题、简介与封面（演示用），供解读页拼材料。"""

from __future__ import annotations

import html
import re
from dataclasses import dataclass
from urllib.parse import urljoin

import httpx

_DEFAULT_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
)


@dataclass
class LinkContext:
    title: str
    description: str
    image_url: str
    page_text: str = ""


def extract_visible_text(html_text: str, *, max_chars: int = 2000) -> str:
    """去 script/style 与 HTML 标签，取可见正文前 N 字（无头浏览器）。"""
    if not html_text:
        return ""
    text = re.sub(r"<(script|style|noscript)[^>]*>.*?</\1>", " ", html_text, flags=re.I | re.S)
    text = re.sub(r"<!--.*?-->", " ", text, flags=re.S)
    text = re.sub(r"<[^>]+>", " ", text)
    text = html.unescape(text)
    text = re.sub(r"\s+", " ", text).strip()
    if len(text) > max_chars:
        return text[:max_chars].rstrip() + "…"
    return text


def _meta_content(html_text: str, *, prop: str | None = None, name: str | None = None) -> str:
    """从 <meta property=...> 或 <meta name=...> 取 content。"""
    if prop:
        patterns = (
            rf'<meta\s+[^>]*property\s*=\s*["\']{re.escape(prop)}["\'][^>]*\s+content\s*=\s*["\']([^"\']*)["\']',
            rf'<meta\s+[^>]*content\s*=\s*["\']([^"\']*)["\'][^>]*property\s*=\s*["\']{re.escape(prop)}["\']',
        )
        for p in patterns:
            m = re.search(p, html_text, re.I | re.DOTALL)
            if m:
                return html.unescape(m.group(1).strip())
        return ""
    if name:
        patterns = (
            rf'<meta\s+[^>]*name\s*=\s*["\']{re.escape(name)}["\'][^>]*\s+content\s*=\s*["\']([^"\']*)["\']',
            rf'<meta\s+[^>]*content\s*=\s*["\']([^"\']*)["\'][^>]*name\s*=\s*["\']{re.escape(name)}["\']',
        )
        for p in patterns:
            m = re.search(p, html_text, re.I | re.DOTALL)
            if m:
                return html.unescape(m.group(1).strip())
    return ""


async def fetch_link_context(page_url: str, timeout: float = 12.0) -> LinkContext:
    try:
        async with httpx.AsyncClient(follow_redirects=True, timeout=timeout) as client:
            r = await client.get(
                page_url,
                headers={"User-Agent": _DEFAULT_UA, "Accept-Language": "zh-CN,zh;q=0.9"},
            )
            r.raise_for_status()
            text = r.text[:800_000]
    except Exception:
        return LinkContext(title="分享的链接", description="", image_url="", page_text="")

    title = "分享的链接"
    m = re.search(r"<title[^>]*>([^<]+)</title>", text, re.I)
    if m:
        title = html.unescape(m.group(1).strip())[:200]

    description = _meta_content(text, prop="og:description") or _meta_content(
        text, name="description"
    )
    description = re.sub(r"\s+", " ", description)[:2000]

    image_url = _meta_content(text, prop="og:image") or _meta_content(text, name="twitter:image")
    image_url = image_url.strip()
    if image_url.startswith("//"):
        image_url = "https:" + image_url
    elif image_url and not image_url.startswith(("http://", "https://")):
        image_url = urljoin(str(r.url), image_url)

    page_text = extract_visible_text(text, max_chars=2000)

    return LinkContext(
        title=title,
        description=description,
        image_url=image_url[:800],
        page_text=page_text,
    )


async def fetch_page_title(url: str, timeout: float = 8.0) -> str:
    ctx = await fetch_link_context(url, timeout=timeout)
    return ctx.title
