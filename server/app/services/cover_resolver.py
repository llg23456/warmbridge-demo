"""封面解析：B 站官方 API → RAWG 游戏 → Bing 图（多查询）。"""

from __future__ import annotations

import logging
import re
from dataclasses import dataclass

import httpx

from app.config import settings
from app.services import video_platform, web_lookup

_log = logging.getLogger(__name__)

_BV_RE = re.compile(r"BV[1-9A-HJ-NP-Za-km-z]{10}", re.I)

_GAME_MARKERS = (
    "游戏",
    "手游",
    "网游",
    "端游",
    "电竞",
    "单机",
)
_KNOWN_GAMES = (
    "皇室战争",
    "部落冲突",
    "原神",
    "王者荣耀",
    "和平精英",
    "英雄联盟",
    "minecraft",
    "我的世界",
)


@dataclass
class CoverResolveResult:
    url: str = ""
    source: str = ""  # bilibili_api | rawg | bing_image | ""
    keyword: str = ""
    detail: str = ""


def is_game_topic(*, title: str = "", summary: str = "", tag: str = "", share_keywords: str = "") -> bool:
    blob = f"{title} {summary} {tag} {share_keywords}".lower()
    if any(m in blob for m in _GAME_MARKERS):
        return True
    return any(g.lower() in blob for g in _KNOWN_GAMES)


async def _resolve_bilibili_pic(page_url: str) -> CoverResolveResult:
    url = (page_url or "").strip()
    if not video_platform.is_bilibili(url):
        return CoverResolveResult()

    bvid = _BV_RE.search(url)
    if not bvid and "b23.tv" in url.lower():
        try:
            async with httpx.AsyncClient(timeout=10.0, follow_redirects=True) as client:
                r = await client.head(url, headers={"User-Agent": web_lookup._UA})
                final = str(r.url)
                bvid = _BV_RE.search(final)
        except Exception as e:
            _log.info("cover_resolver bilibili redirect fail url=%s err=%s", url[:80], e)
            return CoverResolveResult(detail=f"B 站短链解析失败：{e}")

    if not bvid:
        return CoverResolveResult(detail="B 站链接未解析到 BV 号")

    bv = bvid.group(0)
    try:
        async with httpx.AsyncClient(timeout=12.0, follow_redirects=True) as client:
            r = await client.get(
                "https://api.bilibili.com/x/web-interface/view",
                params={"bvid": bv},
                headers={"User-Agent": web_lookup._UA, "Referer": "https://www.bilibili.com/"},
            )
            data = r.json()
        if data.get("code") != 0:
            return CoverResolveResult(detail=f"B 站 API code={data.get('code')}")
        pic = (data.get("data") or {}).get("pic") or ""
        if not pic.startswith(("http://", "https://")):
            return CoverResolveResult(detail="B 站 API 无封面 pic")
        _log.info("cover_resolver bilibili ok bvid=%s pic=%s", bv, pic[:80])
        return CoverResolveResult(url=pic, source="bilibili_api", keyword=bv, detail="B 站官方 view API")
    except Exception as e:
        _log.warning("cover_resolver bilibili fail bvid=%s: %s", bv, e)
        return CoverResolveResult(detail=f"B 站 API 异常：{e}")


async def _resolve_rawg_cover(keyword: str) -> CoverResolveResult:
    key = (settings.rawg_api_key or "").strip()
    if not key:
        return CoverResolveResult(detail="未配置 RAWG_API_KEY")
    kw = (keyword or "").strip()
    if not kw:
        return CoverResolveResult()

    search_q = re.sub(r"(游戏|手游|网游|端游)$", "", kw).strip() or kw
    try:
        async with httpx.AsyncClient(timeout=12.0, follow_redirects=True) as client:
            r = await client.get(
                "https://api.rawg.io/api/games",
                params={"search": search_q, "page_size": 3, "key": key},
                headers={"User-Agent": web_lookup._UA},
            )
            r.raise_for_status()
            data = r.json()
        results = data.get("results") or []
        for item in results:
            if not isinstance(item, dict):
                continue
            pic = (item.get("background_image") or "").strip()
            if pic.startswith(("http://", "https://")):
                name = str(item.get("name") or search_q)
                _log.info("cover_resolver rawg ok query=%s game=%s", search_q, name)
                return CoverResolveResult(
                    url=pic,
                    source="rawg",
                    keyword=search_q,
                    detail=f"RAWG 命中「{name}」",
                )
        return CoverResolveResult(detail=f"RAWG 无封面 query={search_q}")
    except Exception as e:
        _log.warning("cover_resolver rawg fail query=%s: %s", search_q, e)
        return CoverResolveResult(detail=f"RAWG 查询失败：{e}")


async def resolve_cover(
    *,
    page_url: str,
    title: str,
    summary: str,
    tag: str,
    share_keywords: str,
) -> CoverResolveResult:
    """og 失败后的封面来源链：B 站 API → RAWG（游戏）→ Bing。"""
    if video_platform.is_bilibili(page_url):
        bili = await _resolve_bilibili_pic(page_url)
        if bili.url:
            return bili

    keywords = web_lookup.extract_video_search_keywords(
        title=title,
        summary=summary,
        tag=tag,
        share_keywords=share_keywords,
    )

    if is_game_topic(title=title, summary=summary, tag=tag, share_keywords=share_keywords):
        for kw in keywords:
            rawg = await _resolve_rawg_cover(kw)
            if rawg.url:
                return rawg

    tried: list[str] = []
    last_detail = ""
    cover_kws = web_lookup.keywords_for_cover(
        title=title,
        summary=summary,
        tag=tag,
        share_keywords=share_keywords,
    )
    skipped = [k for k in keywords if k not in cover_kws]
    for kw in skipped:
        tried.append(f"{kw}(跳过敏感/情绪)")
    for kw in cover_kws:
        tried.append(kw)
        bing = await web_lookup.bing_image_search(kw)
        if bing.url:
            return CoverResolveResult(
                url=bing.url,
                source="bing_image",
                keyword=kw,
                detail=bing.detail or f"Bing 图片搜索「{kw}」",
            )
        last_detail = bing.skip_reason or f"「{kw}」未命中"

    primary_kw = keywords[0] if keywords else ""
    detail = last_detail or "无可用封面关键词"
    if tried:
        detail = f"已尝试 {', '.join(tried)}；{detail}"
    return CoverResolveResult(keyword=primary_kw, detail=detail)
