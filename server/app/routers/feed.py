from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.schemas import FeedItem, FeedResponse
from app.services import feed_mock, store

router = APIRouter(prefix="/api", tags=["feed"])

TAGS_LIST = ["科技", "军事", "人文", "健康", "社会"]


@router.get("/tags")
def tags():
    return {"tags": TAGS_LIST}


@router.get("/feed", response_model=FeedResponse)
def get_feed(tag: str | None = None, channel: str | None = None):
    if channel == "child":
        items = store.child_items_list()
        return FeedResponse(items=items)
    if channel == "trend":
        items = feed_mock.trend_items()
        return FeedResponse(items=items)
    items = feed_mock.filter_by_tag(tag)
    return FeedResponse(items=items)


@router.get("/items/{item_id}", response_model=FeedItem)
def get_item(item_id: str):
    it = store.get_any_item(item_id)
    if not it:
        raise HTTPException(status_code=404, detail="item not found")
    return it
