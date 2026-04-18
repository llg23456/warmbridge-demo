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
    if cached and not req.question:
        return cached

    material = f"标题：{it.title}\n来源：{it.source}\n摘要：{it.summary}\n链接：{it.url}"
    resp = await explain_from_material(material, req.question)
    # 勿缓存离线占位：否则先无密钥请求一次后，即使已配置 key 仍会命中旧缓存
    if not req.question and resp.from_llm:
        store.cache_explain(req.item_id, resp)
    return resp
