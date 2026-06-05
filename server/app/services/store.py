"""内存存储：子女分享条目 + 解读缓存（演示用，重启即清空）。"""

from __future__ import annotations

import uuid
from typing import Optional

from app.schemas import ExplainResponse, FeedItem

_child_items: dict[str, FeedItem] = {}
_session_items: dict[str, FeedItem] = {}
_explain_cache: dict[str, ExplainResponse] = {}


def _with_clean_title(it: FeedItem) -> FeedItem:
    from app.services.paste_intel import sanitize_display_title

    clean = sanitize_display_title(it.title)
    if clean and clean != it.title:
        return it.model_copy(update={"title": clean})
    return it


def add_child_item(item: FeedItem) -> None:
    _child_items[item.id] = _with_clean_title(item)


def patch_child_item(item_id: str, **fields: object) -> None:
    """后台补全分享条目（标题/封面/联网摘要等）。"""
    it = _child_items.get(item_id)
    if not it:
        return
    _child_items[item_id] = it.model_copy(update=fields)


def put_session_item(item: FeedItem) -> None:
    """识图 / 视频快解析等会话条目：不进「孩子推荐」列表，但可被详情与解读命中。"""
    _session_items[item.id] = item


def child_items_list() -> list[FeedItem]:
    out: list[FeedItem] = []
    for item_id, it in list(_child_items.items()):
        cleaned = _with_clean_title(it)
        if cleaned.title != it.title:
            _child_items[item_id] = cleaned
        out.append(cleaned)
    return out


def get_any_item(item_id: str) -> Optional[FeedItem]:
    from app.services import feed_mock

    if item_id in _session_items:
        return _session_items[item_id]
    if item_id in _child_items:
        it = _child_items[item_id]
        cleaned = _with_clean_title(it)
        if cleaned.title != it.title:
            _child_items[item_id] = cleaned
        return cleaned
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
