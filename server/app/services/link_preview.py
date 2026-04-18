"""尽力抓取页面标题（演示用，失败则返回默认）。"""

from __future__ import annotations

import re
import httpx


async def fetch_page_title(url: str, timeout: float = 8.0) -> str:
    try:
        async with httpx.AsyncClient(follow_redirects=True, timeout=timeout) as client:
            r = await client.get(url, headers={"User-Agent": "WarmBridgeDemo/1.0"})
            r.raise_for_status()
            text = r.text[:500_000]
            m = re.search(r"<title[^>]*>([^<]+)</title>", text, re.I)
            if m:
                return m.group(1).strip()[:200]
    except Exception:
        pass
    return "分享的链接"
