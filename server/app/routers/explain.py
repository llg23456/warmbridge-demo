from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.schemas import ExplainRequest, ExplainResponse
from app.services import store
from app.services.vivo_llm import explain_from_material

router = APIRouter(prefix="/api", tags=["explain"])


@router.post("/explain", response_model=ExplainResponse)
async def explain(req: ExplainRequest):
    it = store.get_any_item(req.item_id)
    if not it:
        raise HTTPException(status_code=404, detail="item not found")

    cached = store.get_cached_explain(req.item_id)
    has_question = bool((req.question or "").strip())
    if cached and not has_question:
        return cached

    parts = [
        f"标题：{it.title}",
        f"来源：{it.source}",
        f"列表摘要：{it.summary}",
    ]
    desc = (it.page_description or "").strip()
    if desc:
        parts.append(f"页面简介（站点摘录）：{desc}")
    web = (it.web_context or "").strip()
    if web:
        parts.append(f"联网检索摘要（第三方自动摘要，仅供家庭参考，不保证与视频完全一致）：\n{web}")
    parts.append(f"链接：{it.url}")
    material_core = "\n".join(parts)
    q = (req.question or "").strip()
    if has_question and q and cached and (cached.plain_summary or "").strip():
        material = (
            "【上一轮已生成的「用长辈能懂的话」摘要（仅供衔接，请针对家人追问补充或改写，禁止大段照抄）】\n"
            f"{cached.plain_summary.strip()}\n\n"
            f"【原始材料】\n{material_core}"
        )
    else:
        material = material_core
    preview_img = (it.preview_image_url or "").strip() or None
    resp = await explain_from_material(material, req.question, preview_image_url=preview_img)
    # 勿缓存离线占位：否则先无密钥请求一次后，即使已配置 key 仍会命中旧缓存
    if not has_question and resp.from_llm:
        store.cache_explain(req.item_id, resp)
    return resp
