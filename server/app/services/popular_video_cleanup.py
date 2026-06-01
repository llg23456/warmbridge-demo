"""通俗视频磁盘与任务缓存清理（避免 data/popular_videos 无限增大）。"""

from __future__ import annotations

import logging
import shutil
import time
from pathlib import Path

from app.services import popular_video_store

_log = logging.getLogger(__name__)

_DATA_DIR = Path(__file__).resolve().parent.parent.parent / "data" / "popular_videos"
# 仅保留最近若干条成片；其余删除 mp4 与工作目录
_MAX_KEEP_MP4 = 10
_MAX_JOB_AGE_SEC = 6 * 3600


def cleanup_work_dir(work_dir: Path) -> None:
    """删除单次任务中间文件目录（保留已输出的 {job_id}.mp4）。"""
    if not work_dir.is_dir():
        return
    try:
        shutil.rmtree(work_dir, ignore_errors=True)
        _log.info("WbVideoGen cleanup work_dir=%s", work_dir.name)
    except Exception as e:
        _log.warning("WbVideoGen cleanup work_dir failed %s: %s", work_dir, e)


def purge_disk(*, keep_job_id: str | None = None) -> None:
    """
    清理过期 mp4 与孤立工作目录。
    keep_job_id：当前刚完成的任务尽量不删（若已超过保留数仍会按时间清理）。
    """
    _DATA_DIR.mkdir(parents=True, exist_ok=True)
    now = time.time()

    mp4s: list[tuple[float, Path]] = []
    for p in _DATA_DIR.glob("*.mp4"):
        if p.is_file():
            mp4s.append((p.stat().st_mtime, p))

    mp4s.sort(key=lambda x: x[0], reverse=True)
    for idx, (mtime, path) in enumerate(mp4s):
        job_id = path.stem
        too_old = (now - mtime) > _MAX_JOB_AGE_SEC
        over_quota = idx >= _MAX_KEEP_MP4
        is_keep = keep_job_id and job_id == keep_job_id and not too_old
        if is_keep:
            continue
        if too_old or over_quota:
            try:
                path.unlink(missing_ok=True)
                work = _DATA_DIR / job_id
                if work.is_dir():
                    shutil.rmtree(work, ignore_errors=True)
                popular_video_store.remove(job_id)
                _log.info("WbVideoGen purged mp4 job=%s too_old=%s over_quota=%s", job_id, too_old, over_quota)
            except Exception as e:
                _log.warning("WbVideoGen purge failed %s: %s", path, e)

    # 孤立工作目录（无对应 mp4 或已过期）
    for work in _DATA_DIR.iterdir():
        if not work.is_dir():
            continue
        mp4 = _DATA_DIR / f"{work.name}.mp4"
        if not mp4.is_file():
            shutil.rmtree(work, ignore_errors=True)
            _log.info("WbVideoGen removed orphan work_dir=%s", work.name)
