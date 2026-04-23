"""轻量联网摘要：DuckDuckGo Instant Answer + 中文维基（开放 API），失败则静默跳过。"""

from __future__ import annotations

from typing import Any

import httpx

from app.config import settings

_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
)
# https://meta.wikimedia.org/wiki/User-Agent_policy
_WIKI_UA = "WarmBridgeDemo/1.0 (contest demo; httpx; no commercial use)"

# 常见昵称 → 更易检索本名的词（仅作补充查询，不展示给长辈）
_NICK_EXTRA_QUERIES: dict[str, str] = {
    "嘎子": "谢孟伟",
}


def _collect_related_topics(nodes: list[Any], limit: int) -> list[str]:
    texts: list[str] = []
    for t in nodes:
        if len(texts) >= limit:
            break
        if isinstance(t, dict):
            tx = t.get("Text")
            if isinstance(tx, str) and tx.strip():
                texts.append(tx.strip())
            subs = t.get("Topics")
            if isinstance(subs, list):
                texts.extend(_collect_related_topics(subs, limit - len(texts)))
    return texts


async def duckduckgo_instant_summary(query: str, timeout: float = 10.0) -> str:
    q = query.strip()
    if not q or not settings.web_search_enabled:
        return ""
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            r = await client.get(
                "https://api.duckduckgo.com/",
                params={
                    "q": q,
                    "format": "json",
                    "no_html": "1",
                    "no_redirect": "1",
                },
                headers={"User-Agent": _UA},
            )
            r.raise_for_status()
            data = r.json()
    except Exception:
        return ""

    parts: list[str] = []
    ab = data.get("AbstractText")
    if isinstance(ab, str) and ab.strip():
        parts.append(ab.strip())
    df = data.get("Definition")
    if isinstance(df, str) and df.strip():
        parts.append(df.strip())

    rel = data.get("RelatedTopics")
    if isinstance(rel, list):
        parts.extend(_collect_related_topics(rel, 5))

    out = "\n".join(parts)
    return out[:2800]


async def _wikipedia_page_extract(client: httpx.AsyncClient, title: str) -> str:
    r = await client.get(
        "https://zh.wikipedia.org/w/api.php",
        params={
            "action": "query",
            "format": "json",
            "prop": "extracts",
            "exintro": "true",
            "explaintext": "true",
            "titles": title,
            "redirects": "1",
        },
        headers={"User-Agent": _WIKI_UA, "Accept-Language": "zh-CN"},
    )
    r.raise_for_status()
    pages = r.json().get("query", {}).get("pages", {})
    for _pid, p in pages.items():
        ex = p.get("extract")
        if isinstance(ex, str) and ex.strip():
            return ex.strip()[:2800]
    return ""


async def wikipedia_zh_best_effort(search: str, timeout: float = 14.0) -> str:
    q = search.strip()
    if not q or not settings.web_search_enabled:
        return ""
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            r = await client.get(
                "https://zh.wikipedia.org/w/api.php",
                params={
                    "action": "opensearch",
                    "search": q,
                    "limit": "3",
                    "namespace": "0",
                    "format": "json",
                },
                headers={"User-Agent": _WIKI_UA, "Accept-Language": "zh-CN"},
            )
            r.raise_for_status()
            data = r.json()
            titles: list[str] = []
            if isinstance(data, list) and len(data) >= 2 and isinstance(data[1], list):
                titles = [t for t in data[1] if isinstance(t, str)]
            for title in titles[:2]:
                ex = await _wikipedia_page_extract(client, title)
                if ex:
                    return f"（中文维基摘录：{title}）\n{ex}"
    except Exception:
        return ""
    return ""


async def lookup_keyword_blurb(kw: str) -> str:
    """单关键词：DDG → 维基，尽量凑一段可给 LLM 转述的材料。"""
    kw = kw.strip()
    if not kw:
        return ""
    q_short = kw if len(kw) > 5 else f"{kw} 是谁"
    blob = await duckduckgo_instant_summary(q_short)
    if blob:
        return blob
    blob = await wikipedia_zh_best_effort(kw)
    if blob:
        return blob
    if len(kw) <= 8:
        blob = await duckduckgo_instant_summary(f"{kw} 梗")
        if blob:
            return blob
        blob = await wikipedia_zh_best_effort(f"{kw} 演员")
        if blob:
            return blob
    return ""


async def build_web_context(keywords: list[str]) -> str:
    if not settings.web_search_enabled or not keywords:
        return ""
    chunks: list[str] = []
    seen: set[str] = set()
    for kw in keywords[:3]:
        kw = kw.strip()
        if not kw or kw in seen:
            continue
        seen.add(kw)
        blurb = await lookup_keyword_blurb(kw)
        if not blurb:
            extra = _NICK_EXTRA_QUERIES.get(kw)
            if extra:
                blurb = await lookup_keyword_blurb(extra)
        if blurb:
            chunks.append(f"【关键词：{kw}】\n{blurb}")
        if len("\n\n".join(chunks)) > 4000:
            break
    return "\n\n".join(chunks)[:4500]
