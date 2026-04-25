from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.schemas import FeedItem, VideoQuickRequest, VideoQuickResponse
from app.services import link_preview, paste_intel, store, video_paste, web_lookup
from app.services.vivo_llm import explain_from_material

router = APIRouter(prefix="/api", tags=["video"])


@router.post("/video/quickparse", response_model=VideoQuickResponse)
async def video_quickparse(req: VideoQuickRequest):
    blob = (req.paste or "").strip()
    if blob:
        url = video_paste.extract_first_url(blob)
        raw = blob
    else:
        url = (req.url or "").strip()
        raw = ((req.raw_paste or "").strip() or url)
    if not url.startswith(("http://", "https://")):
        raise HTTPException(
            status_code=400,
            detail="未识别到 http(s) 链接。请将含 https:// 的整段口令粘贴到输入框。",
        )

    ctx = await link_preview.fetch_link_context(url)
    title = paste_intel.suggest_title_from_paste(raw, ctx.title)[:120] or "视频/链接快读"
    keywords = paste_intel.extract_keywords(f"{raw}\n{title}")
    web_context = await web_lookup.build_web_context(keywords)
    desc = (ctx.description or "").strip()
    summary = desc[:500] if desc else title

    item_id = store.new_session_id("vq")
    preview = (ctx.image_url or "").strip()[:2000]
    item = FeedItem(
        id=item_id,
        title=title,
        summary=summary,
        source="快解析",
        url=url,
        tag="生活",
        channel="session",
        page_description=desc[:2000],
        preview_image_url=preview,
        web_context=web_context,
    )
    store.put_session_item(item)

    parts = [
        f"标题：{item.title}",
        f"列表摘要：{item.summary}",
    ]
    if desc:
        parts.append(f"页面简介（站点摘录）：{desc}")
    if web_context:
        parts.append(f"联网检索摘要（第三方自动摘要，仅供家庭参考）：\n{web_context}")
    parts.append(f"链接：{url}")
    material = "\n".join(parts)
    resp = await explain_from_material(
        material,
        question=None,
        preview_image_url=preview or None,
    )
    if resp.from_llm:
        store.cache_explain(item_id, resp)
    return VideoQuickResponse(item_id=item_id)
