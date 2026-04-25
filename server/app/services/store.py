"""内存存储：子女分享条目 + 解读缓存（演示用，重启即清空）。"""

from __future__ import annotations

import uuid
from typing import Optional

from app.schemas import ExplainResponse, FeedItem

_child_items: dict[str, FeedItem] = {}
_session_items: dict[str, FeedItem] = {}
_explain_cache: dict[str, ExplainResponse] = {}


def add_child_item(item: FeedItem) -> None:
    _child_items[item.id] = item


def put_session_item(item: FeedItem) -> None:
    """识图 / 视频快解析等会话条目：不进「孩子推荐」列表，但可被详情与解读命中。"""
    _session_items[item.id] = item


def child_items_list() -> list[FeedItem]:
    return list(_child_items.values())


def get_any_item(item_id: str) -> Optional[FeedItem]:
    from app.services import feed_mock

    if item_id in _session_items:
        return _session_items[item_id]
    if item_id in _child_items:
        return _child_items[item_id]
    for it in feed_mock.all_mock_items():
        if it.id == item_id:
            return it
    return None


def cache_explain(item_id: str, resp: ExplainResponse) -> None:
    _explain_cache[item_id] = resp


def get_cached_explain(item_id: str) -> Optional[ExplainResponse]:
    return _explain_cache.get(item_id)


def new_child_id() -> str:
    return f"c-{uuid.uuid4().hex[:12]}"


def new_session_id(prefix: str) -> str:
    return f"{prefix}-{uuid.uuid4().hex[:12]}"
