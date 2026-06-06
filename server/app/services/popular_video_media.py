"""通俗视频 D2/D3：vivo 文生图轮播 + 首帧 CDN（供 D3 图生视频）。"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from pathlib import Path

from app.services import popular_video_quota, vivo_image
from app.services.popular_video_prompt_safety import sanitize_visual_prompt
from app.services.video_slideshow import image_to_jpeg

_log = logging.getLogger(__name__)

TARGET_SLIDES = 3

_SAFE_ANGLES = (
    "wide establishing shot of a peaceful public space at golden hour",
    "close-up of everyday objects on a wooden table, soft window light",
    "gentle documentary detail shot, shallow depth of field, warm tones",
)


@dataclass
class SlideGenResult:
    paths: list[Path] = field(default_factory=list)
    """首张成功文生图的 vivo CDN URL，供 D3 first_frame（非公网 LAN）。"""
    first_cdn_url: str = ""


def _safe_fallback_prompt(topic: str, index: int) -> str:
    angle = _SAFE_ANGLES[index % len(_SAFE_ANGLES)]
    t = (topic or "daily life").strip()[:40] or "daily life"
    return (
        f"Family-friendly documentary photograph inspired by the theme '{t}', "
        f"{angle}, no text, no logos, no recognizable celebrities, "
        f"widescreen 16:9 landscape, cinematic realism"
    )


def _prompt_preview(prompt: str) -> str:
    return (prompt or "").strip()[:120].replace("\n", " ")


async def _fetch_slide_image(work: Path, index: int, prompt: str) -> tuple[Path, str]:
    work.mkdir(parents=True, exist_ok=True)
    cdn_url = await vivo_image.generate_image_url(prompt)
    raw = work / f"gen_raw_{index}.bin"
    data = await vivo_image.download_image_bytes(cdn_url)
    if len(data) < 500:
        raise RuntimeError(f"文生图下载过小 index={index} bytes={len(data)}")
    raw.write_bytes(data)
    if not raw.is_file():
        raise FileNotFoundError(f"文生图缓存写入失败: {raw}")
    out = work / f"slide_{index}.jpg"
    image_to_jpeg(raw, out)
    _log.info("WbVideoGen slide_%s ok bytes=%s cdn=%s", index, out.stat().st_size, cdn_url[:80])
    return out, cdn_url


async def _generate_one_slide(
    work: Path,
    index: int,
    prompt: str,
    *,
    safe_topic: str = "",
) -> tuple[Path | None, str]:
    if not popular_video_quota.consume_image_slot():
        return None, ""
    preview = _prompt_preview(prompt)
    try:
        return await _fetch_slide_image(work, index, prompt)
    except vivo_image.QuotaExceededError as e:
        popular_video_quota.refund_image_slot()
        popular_video_quota.sync_from_vivo_rate_limit(e.body)
        raise
    except vivo_image.ContentPolicyError:
        _log.warning(
            "WbVideoGen slide_%s policy reject preview=%s -> sanitize then safe prompt",
            index,
            preview,
        )
        popular_video_quota.refund_image_slot()
        sanitized = sanitize_visual_prompt(prompt, topic=safe_topic)
        if sanitized and sanitized != prompt:
            if popular_video_quota.consume_image_slot():
                try:
                    out, cdn = await _fetch_slide_image(work, index, sanitized)
                    _log.info("WbVideoGen slide_%s policy sanitize retry ok", index)
                    return out, cdn
                except vivo_image.ContentPolicyError:
                    popular_video_quota.refund_image_slot()
                    _log.warning("WbVideoGen slide_%s policy sanitize retry still blocked", index)
                except vivo_image.QuotaExceededError as e:
                    popular_video_quota.refund_image_slot()
                    popular_video_quota.sync_from_vivo_rate_limit(e.body)
                    raise
                except Exception as e_s:
                    popular_video_quota.refund_image_slot()
                    _log.warning("WbVideoGen slide_%s policy sanitize retry fail: %s", index, e_s)
        fallback = _safe_fallback_prompt(safe_topic, index)
        if not popular_video_quota.consume_image_slot():
            _log.warning("WbVideoGen slide_%s policy retry skipped (no quota)", index)
            return None, ""
        try:
            out, cdn = await _fetch_slide_image(work, index, fallback)
            _log.info("WbVideoGen slide_%s policy safe fallback ok", index)
            return out, cdn
        except vivo_image.QuotaExceededError as e:
            popular_video_quota.refund_image_slot()
            popular_video_quota.sync_from_vivo_rate_limit(e.body)
            raise
        except Exception as e2:
            popular_video_quota.refund_image_slot()
            _log.warning("WbVideoGen slide_%s policy retry fail: %s", index, e2)
            return None, ""
    except Exception as e:
        popular_video_quota.refund_image_slot()
        _log.warning(
            "WbVideoGen slide_%s fail preview=%s err=%s",
            index,
            preview,
            e,
        )
        return None, ""


async def generate_slide_images(
    work: Path,
    prompts: list[str],
    *,
    job_id: str = "",
    safe_topic: str = "",
) -> SlideGenResult:
    """
    **顺序**生成最多 3 张轮播图；返回本地路径 + 首张 CDN URL（D3 首帧）。
    """
    if not popular_video_quota.vivo_media_enabled():
        _log.info("WbVideoGen job=%s media disabled (POPULAR_VIDEO_USE_VIVO_MEDIA or no key)", job_id)
        return SlideGenResult()

    usable = [p.strip() for p in prompts if (p or "").strip()][:TARGET_SLIDES]
    if not usable:
        _log.warning("WbVideoGen job=%s no image_prompts", job_id)
        return SlideGenResult()

    if popular_video_quota.available_image_slots() <= 0:
        _log.info("WbVideoGen job=%s image quota exhausted %s", job_id, popular_video_quota.quota_snapshot())
        return SlideGenResult()

    _log.info(
        "WbVideoGen job=%s generating up to %s slides sequential quota=%s",
        job_id,
        len(usable),
        popular_video_quota.quota_snapshot(),
    )

    paths: list[Path] = []
    first_cdn = ""
    attempted = 0
    for i, prompt in enumerate(usable):
        if popular_video_quota.available_image_slots() <= 0:
            _log.info(
                "WbVideoGen job=%s stop sequential: local quota empty after %s ok",
                job_id,
                len(paths),
            )
            break
        attempted += 1
        try:
            out, cdn = await _generate_one_slide(work, i, prompt, safe_topic=safe_topic)
        except vivo_image.QuotaExceededError as e:
            _log.warning(
                "WbVideoGen job=%s vivo quota/limit (1003) after %s ok, stop: %s",
                job_id,
                len(paths),
                e,
            )
            break
        if out is not None:
            paths.append(out)
            if cdn and not first_cdn:
                first_cdn = cdn

    failed = attempted - len(paths)
    if failed and paths:
        _log.info(
            "WbVideoGen job=%s slides partial ok=%s fail=%s (partial carousel is expected)",
            job_id,
            len(paths),
            failed,
        )
    _log.info(
        "WbVideoGen job=%s slides done count=%s first_cdn=%s",
        job_id,
        len(paths),
        bool(first_cdn),
    )
    return SlideGenResult(paths=paths, first_cdn_url=first_cdn)
