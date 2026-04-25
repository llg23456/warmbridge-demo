"""每日 7:00（上海时区）刷新：为每个白名单标签生成一条「浓缩」条目（演示数据）。"""

from __future__ import annotations

from datetime import date

from app.schemas import FeedItem
from app.services import feed_mock
from app.tags_catalog import TAGS_LIST


def refresh_daily_digest() -> None:
    today = date.today().isoformat()
    suffix = today.replace("-", "")
    items: list[FeedItem] = []
    for tag in TAGS_LIST:
        items.append(
            FeedItem(
                id=f"dig-{tag}-{suffix}",
                title=f"今日浓缩 · {tag}：一眼读懂的热点",
                summary=(
                    "（每日 7:00 自动刷新 · 演示）把公开资讯收成一两句家常话，"
                    "方便您饭后与孩子聊聊今天网上在讨论什么。"
                ),
                source="暖桥日报",
                url="https://www.news.cn/",
                tag=tag,
                channel="tag",
                updated_at=today,
            )
        )
    feed_mock.replace_daily_digest(items)
