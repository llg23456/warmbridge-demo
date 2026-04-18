from __future__ import annotations

from fastapi import APIRouter

from app.schemas import FeedItem, ShareRequest, ShareResponse
from app.services import link_preview, store

router = APIRouter(prefix="/api", tags=["share"])


@router.post("/share", response_model=ShareResponse)
async def share(req: ShareRequest):
    title = await link_preview.fetch_page_title(req.url)
    summary = (req.note or "").strip() or f"孩子分享了一条链接，标题大致为：{title}"
    item = FeedItem(
        id=store.new_child_id(),
        title=title[:120],
        summary=summary[:500],
        source="孩子推荐",
        url=req.url,
        tag="社会",
        channel="child",
        updated_at="",
    )
    store.add_child_item(item)
    return ShareResponse(ok=True, item_id=item.id)
