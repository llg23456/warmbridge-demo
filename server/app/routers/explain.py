from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.schemas import ExplainRequest, ExplainResponse
from app.services import store
from app.services.vivo_llm import (
    answer_follow_up,
    explain_from_material,
    normalize_explain_response,
)

router = APIRouter(prefix="/api", tags=["explain"])


def _build_material(it) -> str:
    parts = [
        f"标题：{it.title}",
        f"来源：{it.source}",
        f"列表摘要：{it.summary}",
    ]
    if (it.share_keywords or "").strip():
        parts.append(f"分享关键词：{it.share_keywords}")
    desc = (it.page_description or "").strip()
    if desc:
        parts.append(f"页面简介（站点摘录）：{desc}")
    web = (it.web_context or "").strip()
    if web:
        parts.append(f"联网检索摘要（第三方自动摘要，仅供家庭参考，不保证与视频完全一致）：\n{web}")
    parts.append(f"链接：{it.url}")
    return "\n".join(parts)


@router.post("/explain", response_model=ExplainResponse)
async def explain(req: ExplainRequest):
    it = store.get_any_item(req.item_id)
    if not it:
        raise HTTPException(status_code=404, detail="item not found")

    material = _build_material(it)
    q = (req.question or "").strip()
    has_question = bool(q)

    cached = store.get_cached_explain(req.item_id)

    if has_question:
        if not cached:
            cached = await explain_from_material(
                material,
                question=None,
                preview_image_url=(it.preview_image_url or "").strip() or None,
            )
            if cached.from_llm:
                store.cache_explain(req.item_id, cached)

        answer, from_llm, searched = await answer_follow_up(
            material=material,
            question=q,
            prior_summary=cached.plain_summary if cached else "",
        )
        base = cached or ExplainResponse(
            plain_summary="",
            background="",
            glossary="",
            disclaimer="内容仅供家庭交流参考，不构成专业建议；请以原视频或原文为准。",
            suggested_questions=[],
        )
        return normalize_explain_response(
            base.model_copy(
                update={
                    "follow_up_answer": answer,
                    "follow_up_from_llm": from_llm,
                    "follow_up_searched": searched,
                }
            )
        )

    if cached:
        return normalize_explain_response(cached)

    preview_img = (it.preview_image_url or "").strip() or None
    resp = await explain_from_material(material, question=None, preview_image_url=preview_img)
    resp = normalize_explain_response(resp)
    if resp.from_llm:
        store.cache_explain(req.item_id, resp)
    return resp
