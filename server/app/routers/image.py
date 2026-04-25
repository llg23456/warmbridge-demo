from __future__ import annotations

from fastapi import APIRouter, File, HTTPException, UploadFile

from app.schemas import FeedItem, ImageExplainResponse
from app.services import store, vivo_ocr
from app.services.vivo_llm import explain_from_material

router = APIRouter(prefix="/api", tags=["image"])

_MAX_IMAGE_BYTES = 8 * 1024 * 1024


@router.post("/image/explain", response_model=ImageExplainResponse)
async def image_explain(file: UploadFile = File(..., description="图片二进制，表单字段名必须为 file")):
    raw = await file.read()
    if not raw or len(raw) < 32:
        raise HTTPException(
            status_code=400,
            detail=(
                "未收到有效图片数据。请确认客户端用 ContentResolver 读取相册 Uri 后，"
                "以 multipart/form-data 字段名 **file** 上传二进制（勿只传 Uri 字符串或空包）。"
            ),
        )
    if len(raw) > _MAX_IMAGE_BYTES:
        raise HTTPException(status_code=413, detail="图片过大，请选较小的截图。")
    try:
        ocr_text = await vivo_ocr.ocr_image_bytes(raw)
    except RuntimeError as e:
        msg = str(e)
        # 配置类问题用 400，便于与「服务不可用」区分
        if "未配置" in msg:
            raise HTTPException(status_code=400, detail=msg) from e
        raise HTTPException(status_code=502, detail=msg) from e
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"OCR 调用异常：{e}") from e

    if not (ocr_text or "").strip():
        raise HTTPException(
            status_code=400,
            detail=(
                "图中没有识别到清晰文字。请：① 换 JPG/PNG、字大图清；② .env 设 VIVO_OCR_POS=2；"
                "③ 将 VIVO_OCR_BUSINESSID 改为文档「aigc+AppId」（与控制台 AppId 拼接，无分隔符）"
                "或换另一固定串；④ 确认 VIVO_OCR_URL 与文档一致（默认 http://api-ai.vivo.com.cn/ocr/general_recognition）。"
                "服务端日志中有本次 OCR 响应摘要可供核对。"
            ),
        )

    material = (
        "以下是从长辈或孩子上传的截图里识别出的文字（仅 OCR，程序未“看图猜意”）：\n"
        f"{ocr_text.strip()}\n\n"
        "请判断里面可能的网络梗、段子背景或新闻点，用温柔口语写给孩子家长辈看。"
    )
    item_id = store.new_session_id("img")
    item = FeedItem(
        id=item_id,
        title="图片识梗 · 截图",
        summary=ocr_text[:500],
        source="识图",
        url="",
        tag="生活",
        channel="session",
        page_description=ocr_text[:2000],
    )
    store.put_session_item(item)
    resp = await explain_from_material(material, question=None, preview_image_url=None)
    if resp.from_llm:
        store.cache_explain(item_id, resp)
    return ImageExplainResponse(item_id=item_id, ocr_text=ocr_text[:2000])
