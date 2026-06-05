from __future__ import annotations

import asyncio
import logging

from fastapi import APIRouter

from app.schemas import FeedItem, ShareRequest, ShareResponse
from app.services import link_preview, store
from app.services import paste_intel, web_lookup

router = APIRouter(prefix="/api", tags=["share"])
_log = logging.getLogger(__name__)


async def _enrich_share_item(item_id: str, url: str, raw: str, note: str) -> None:
    """后台拉 og 与联网摘要，不阻塞孩子端「发送成功」反馈。"""
    try:
        ctx = await link_preview.fetch_link_context(url, timeout=12.0)
        keywords = paste_intel.extract_keywords(f"{raw}\n{note}")
        web_context = await web_lookup.build_web_context(keywords)
        title = paste_intel.suggest_title_from_paste(raw, ctx.title)[:120]
        patch: dict[str, object] = {}
        if title and title not in ("", "分享的链接"):
            patch["title"] = title
        if ctx.description:
            patch["page_description"] = ctx.description[:2000]
            if not note.strip():
                patch["summary"] = ctx.description[:500]
        if ctx.image_url:
            patch["preview_image_url"] = ctx.image_url[:2000]
        if web_context:
            patch["web_context"] = web_context
        if patch:
            store.patch_child_item(item_id, **patch)
            _log.info("WbShare enriched item=%s keys=%s", item_id, list(patch.keys()))
    except Exception as e:
        _log.warning("WbShare enrich failed item=%s: %s", item_id, e)


@router.post("/share", response_model=ShareResponse)
async def share(req: ShareRequest):
    raw = (req.raw_paste or "").strip()
    note = (req.note or "").strip()
    # 先本地抽标题/摘要，立即入库返回（避免等 og + 维基导致孩子端卡 20～40s）
    title = paste_intel.suggest_title_from_paste(raw, "")[:120] or "孩子分享的链接"
    keywords = paste_intel.extract_keywords(f"{raw}\n{note}")
    tag = web_lookup.pick_display_tag(keywords)
    share_keywords = "，".join(keywords[:6])

    if note:
        summary = note[:500]
    elif raw:
        summary = (
            paste_intel.title_from_share_paste(raw)[:500]
            or f"孩子分享了一条链接：{title}"[:500]
        )
    else:
        summary = f"孩子分享了一条链接，标题大致为：{title}"

    item = FeedItem(
        id=store.new_child_id(),
        title=title,
        summary=summary,
        source="孩子推荐",
        url=req.url,
        tag=tag,
        channel="child",
        updated_at="",
        page_description="",
        preview_image_url="",
        web_context="",
        share_keywords=share_keywords,
    )
    store.add_child_item(item)
    asyncio.create_task(_enrich_share_item(item.id, req.url, raw, note))
    return ShareResponse(ok=True, item_id=item.id)
