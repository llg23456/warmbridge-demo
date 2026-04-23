from __future__ import annotations

from fastapi import APIRouter

from app.schemas import FeedItem, ShareRequest, ShareResponse
from app.services import link_preview, store
from app.services import paste_intel, web_lookup

router = APIRouter(prefix="/api", tags=["share"])


@router.post("/share", response_model=ShareResponse)
async def share(req: ShareRequest):
    ctx = await link_preview.fetch_link_context(req.url)
    raw = (req.raw_paste or "").strip()
    note = (req.note or "").strip()
    title = paste_intel.suggest_title_from_paste(raw, ctx.title)[:120]

    keywords = paste_intel.extract_keywords(f"{raw}\n{note}")
    web_context = await web_lookup.build_web_context(keywords)

    if note:
        summary = note[:500]
    elif ctx.description:
        summary = ctx.description[:500]
    elif raw:
        summary = paste_intel.title_from_share_paste(raw)[:500] or f"孩子分享了一条链接：{title}"[:500]
    else:
        summary = f"孩子分享了一条链接，标题大致为：{title}"

    tag = keywords[0][:8] if keywords else "短视频"

    item = FeedItem(
        id=store.new_child_id(),
        title=title,
        summary=summary,
        source="孩子推荐",
        url=req.url,
        tag=tag,
        channel="child",
        updated_at="",
        page_description=ctx.description,
        preview_image_url=ctx.image_url,
        web_context=web_context,
    )
    store.add_child_item(item)
    return ShareResponse(ok=True, item_id=item.id)
