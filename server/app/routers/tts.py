from __future__ import annotations

import base64
import logging

from fastapi import APIRouter

from app.schemas import TtsRequest, TtsResponse
from app.services.vivo_tts import synthesize_wav

router = APIRouter(prefix="/api", tags=["tts"])
_log = logging.getLogger(__name__)


@router.post("/tts", response_model=TtsResponse)
async def tts_endpoint(req: TtsRequest):
    text = (req.text or "").strip()
    if not text:
        return TtsResponse(
            ok=False,
            from_llm=False,
            message="没有可朗读的文字。",
        )
    try:
        wav = await synthesize_wav(text)
    except Exception as e:
        _log.warning("TTS 合成失败（已降级为 JSON 提示，不返回 502）: %s", e, exc_info=True)
        return TtsResponse(
            ok=False,
            from_llm=False,
            message="语音服务暂时不可用，请阅读下方文字版解读。",
        )
    return TtsResponse(
        ok=True,
        from_llm=True,
        message="",
        audio_base64=base64.b64encode(wav).decode("ascii"),
    )
