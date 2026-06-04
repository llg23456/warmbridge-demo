"""通俗视频 vivo 文生图配额（内存计数，重启清零；符合 Demo 定位）。"""

from __future__ import annotations

import logging
from datetime import date

from app.config import settings

_log = logging.getLogger(__name__)

# 文档 §5：10 次/天、300 总量
IMAGE_DAILY_MAX = 10
IMAGE_TOTAL_MAX = 300

# 文档 §6：5 次/天、50 总量
VIDEO_DAILY_MAX = 5
VIDEO_TOTAL_MAX = 50

_day: str = ""
_today_count: int = 0
_total_count: int = 0
_video_today_count: int = 0
_video_total_count: int = 0


def _roll_day() -> None:
    global _day, _today_count, _video_today_count
    today = date.today().isoformat()
    if today != _day:
        _day = today
        _today_count = 0
        _video_today_count = 0


def vivo_media_enabled() -> bool:
    return bool(settings.popular_video_use_vivo_media) and bool((settings.vivo_app_key or "").strip())


def vivo_intro_enabled() -> bool:
    return bool(settings.popular_video_use_vivo_intro) and bool((settings.vivo_app_key or "").strip())


def available_video_slots() -> int:
    if not vivo_intro_enabled():
        return 0
    _roll_day()
    by_day = VIDEO_DAILY_MAX - _video_today_count
    by_total = VIDEO_TOTAL_MAX - _video_total_count
    return max(0, min(by_day, by_total))


def consume_video_slot() -> bool:
    global _video_today_count, _video_total_count
    if available_video_slots() <= 0:
        _log.info(
            "WbVideoGen quota video skip today=%s/%s total=%s/%s",
            _video_today_count,
            VIDEO_DAILY_MAX,
            _video_total_count,
            VIDEO_TOTAL_MAX,
        )
        return False
    _roll_day()
    _video_today_count += 1
    _video_total_count += 1
    return True


def refund_video_slot() -> None:
    global _video_today_count, _video_total_count
    _roll_day()
    if _video_today_count > 0:
        _video_today_count -= 1
    if _video_total_count > 0:
        _video_total_count -= 1


def sync_video_quota_from_api(body: dict) -> None:
    global _video_today_count, _video_total_count
    _roll_day()
    data = body.get("data") if isinstance(body.get("data"), dict) else {}
    rl = data.get("rate_limit") if isinstance(data.get("rate_limit"), dict) else {}
    for key in ("daily_used", "day_used", "used"):
        if key in rl:
            try:
                _video_today_count = max(_video_today_count, int(rl[key]))
            except (TypeError, ValueError):
                pass
            break
    else:
        _video_today_count = max(_video_today_count, VIDEO_DAILY_MAX)
    _log.info(
        "WbVideoGen quota video synced from vivo 1003 today=%s/%s total=%s/%s",
        _video_today_count,
        VIDEO_DAILY_MAX,
        _video_total_count,
        VIDEO_TOTAL_MAX,
    )


def image_slots_available(count: int = 1) -> bool:
    return available_image_slots() >= count


def available_image_slots() -> int:
    if not vivo_media_enabled():
        return 0
    _roll_day()
    by_day = IMAGE_DAILY_MAX - _today_count
    by_total = IMAGE_TOTAL_MAX - _total_count
    return max(0, min(by_day, by_total))


def consume_image_slot() -> bool:
    """调用 vivo 文生图前占位；成功返回 True。"""
    global _today_count, _total_count
    if not image_slots_available(1):
        _log.info(
            "WbVideoGen quota image skip today=%s/%s total=%s/%s",
            _today_count,
            IMAGE_DAILY_MAX,
            _total_count,
            IMAGE_TOTAL_MAX,
        )
        return False
    _roll_day()
    _today_count += 1
    _total_count += 1
    return True


def refund_image_slot() -> None:
    """API 未产出图片时退回本地配额（失败/策略拦截重试前）。"""
    global _today_count, _total_count
    _roll_day()
    if _today_count > 0:
        _today_count -= 1
    if _total_count > 0:
        _total_count -= 1


def sync_from_vivo_rate_limit(body: dict) -> None:
    """收到 vivo 1003 时对齐本地计数，避免内存配额与云端不一致时继续空打。"""
    global _today_count, _total_count
    _roll_day()
    data = body.get("data") if isinstance(body.get("data"), dict) else {}
    rl = data.get("rate_limit") if isinstance(data.get("rate_limit"), dict) else {}
    for key in ("daily_used", "day_used", "used"):
        if key in rl:
            try:
                _today_count = max(_today_count, int(rl[key]))
            except (TypeError, ValueError):
                pass
            break
    else:
        _today_count = max(_today_count, IMAGE_DAILY_MAX)
    _log.info(
        "WbVideoGen quota synced from vivo 1003 today=%s/%s total=%s/%s",
        _today_count,
        IMAGE_DAILY_MAX,
        _total_count,
        IMAGE_TOTAL_MAX,
    )


def quota_snapshot() -> dict[str, int | str | bool]:
    _roll_day()
    return {
        "media_enabled": vivo_media_enabled(),
        "intro_enabled": vivo_intro_enabled(),
        "image_today": _today_count,
        "image_daily_max": IMAGE_DAILY_MAX,
        "image_total": _total_count,
        "image_total_max": IMAGE_TOTAL_MAX,
        "video_today": _video_today_count,
        "video_daily_max": VIDEO_DAILY_MAX,
        "video_total": _video_total_count,
        "video_total_max": VIDEO_TOTAL_MAX,
        "day": _day or date.today().isoformat(),
    }
