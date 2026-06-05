"""通俗视频 D3：图生视频片头（静默降级）。"""

from __future__ import annotations

import logging
from pathlib import Path

from app.services import popular_video_quota
from app.services.popular_video_prompt_safety import safe_video_intro_prompt, sanitize_visual_prompt
from app.services.vivo_video import (
    VideoContentPolicyError,
    VideoQuotaExceededError,
    generate_intro_video,
)

_log = logging.getLogger(__name__)


async def _try_intro_once(
    work: Path,
    *,
    first_frame_cdn_url: str,
    prompt: str,
    job_id: str,
) -> Path | None:
    if not popular_video_quota.consume_video_slot():
        return None
    try:
        intro = await generate_intro_video(
            work,
            first_frame_url=first_frame_cdn_url,
            prompt=prompt,
        )
        if intro is None:
            popular_video_quota.refund_video_slot()
        return intro
    except VideoQuotaExceededError as e:
        popular_video_quota.refund_video_slot()
        popular_video_quota.sync_video_quota_from_api(e.body)
        _log.warning("WbVideoGen job=%s intro quota 1003: %s", job_id, e)
        raise
    except VideoContentPolicyError:
        popular_video_quota.refund_video_slot()
        raise
    except Exception as e:
        popular_video_quota.refund_video_slot()
        _log.warning("WbVideoGen job=%s intro fail: %s", job_id, e)
        return None


async def try_generate_intro(
    work: Path,
    *,
    first_frame_cdn_url: str,
    video_prompt: str,
    job_id: str = "",
    safe_topic: str = "",
) -> Path | None:
    """
    用文生图 CDN 首帧 + video_prompt 生成约 5s 片头。
    配额/开关/URL 不满足时返回 None，不抛错。
    """
    if not popular_video_quota.vivo_intro_enabled():
        _log.info("WbVideoGen job=%s intro disabled (POPULAR_VIDEO_USE_VIVO_INTRO)", job_id)
        return None
    if not (first_frame_cdn_url or "").strip():
        _log.info("WbVideoGen job=%s intro skip: no first_frame CDN (need ≥1 slide ok)", job_id)
        return None
    if popular_video_quota.available_video_slots() <= 0:
        _log.info(
            "WbVideoGen job=%s intro skip: video quota exhausted %s",
            job_id,
            popular_video_quota.quota_snapshot(),
        )
        return None
    topic = (safe_topic or "").strip()
    policy_blocked = False
    try:
        intro = await _try_intro_once(
            work,
            first_frame_cdn_url=first_frame_cdn_url,
            prompt=video_prompt,
            job_id=job_id,
        )
        if intro is not None:
            return intro
    except VideoContentPolicyError:
        policy_blocked = True

    if not policy_blocked:
        return None

    sanitized = sanitize_visual_prompt(video_prompt, topic=topic)
    if sanitized and sanitized != (video_prompt or "").strip():
        _log.info("WbVideoGen job=%s intro policy retry sanitize", job_id)
        try:
            intro = await _try_intro_once(
                work,
                first_frame_cdn_url=first_frame_cdn_url,
                prompt=sanitized,
                job_id=job_id,
            )
            if intro is not None:
                return intro
        except VideoContentPolicyError:
            pass

    fallback = safe_video_intro_prompt(topic)
    _log.info("WbVideoGen job=%s intro policy retry safe fallback", job_id)
    return await _try_intro_once(
        work,
        first_frame_cdn_url=first_frame_cdn_url,
        prompt=fallback,
        job_id=job_id,
    )
