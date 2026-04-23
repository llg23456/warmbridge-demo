from pydantic import BaseModel, Field
from typing import Optional


class FeedItem(BaseModel):
    id: str
    title: str
    summary: str
    source: str
    url: str
    tag: str
    channel: str = "tag"
    updated_at: str = ""
    # 分享入库时由服务端抓取，供 /api/explain 丰富材料（可选）
    page_description: str = ""
    preview_image_url: str = ""
    # 关键词联网摘要（DuckDuckGo 等），仅服务端用于解读
    web_context: str = ""


class FeedResponse(BaseModel):
    items: list[FeedItem]


class ExplainRequest(BaseModel):
    item_id: str
    question: Optional[str] = None


class ExplainResponse(BaseModel):
    plain_summary: str
    background: str
    glossary: str
    disclaimer: str
    # True 表示 plain/background 来自蓝心解析成功；离线占位为 False（客户端可忽略）
    from_llm: bool = False


class ShareRequest(BaseModel):
    url: str
    note: str = ""
    # App 端「链接」框里用户粘贴的整段口令（与 url 同时传，便于抽标题/关键词/检索）
    raw_paste: str = ""
    parent_user_id: str = "parent_demo"


class ShareResponse(BaseModel):
    ok: bool
    item_id: str


class ReminderRequest(BaseModel):
    message: str = "记得喝水呀"
    delay_seconds: int = Field(default=10, ge=5, le=3600)


class ReminderResponse(BaseModel):
    ok: bool
    detail: str
