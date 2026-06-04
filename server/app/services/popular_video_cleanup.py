"""通俗视频磁盘与任务缓存清理（避免 data/popular_videos 无限增大）。"""

from __future__ import annotations

import logging
import shutil
import time
from pathlib import Path

from app.config import settings
from app.services import popular_video_store

_log = logging.getLogger(__name__)

_DATA_DIR = Path(__file__).resolve().parent.parent.parent / "data" / "popular_videos"
_REPORTS_DIR = _DATA_DIR / "reports"


def cleanup_work_dir(work_dir: Path) -> None:
    """删除单次任务中间文件目录（保留已输出的 {job_id}.mp4）。"""
    if not work_dir.is_dir():
        return
    try:
        shutil.rmtree(work_dir, ignore_errors=True)
        _log.info("WbVideoGen cleanup work_dir=%s", work_dir.name)
    except Exception as e:
        _log.warning("WbVideoGen cleanup work_dir failed %s: %s", work_dir, e)


def release_job_files(job_id: str) -> None:
    """用户离开且未保存到相册：立即删除成片与工作目录，保留 reports/*.md。"""
    mp4 = _DATA_DIR / f"{job_id}.mp4"
    work = _DATA_DIR / job_id
    try:
        mp4.unlink(missing_ok=True)
        if work.is_dir():
            shutil.rmtree(work, ignore_errors=True)
        popular_video_store.mark_released(job_id)
        _log.info("WbVideoGen released job=%s (mp4+work removed, report kept)", job_id)
    except Exception as e:
        _log.warning("WbVideoGen release failed job=%s: %s", job_id, e)


def purge_disk(*, keep_job_id: str | None = None) -> None:
    """
    清理过期 mp4 与孤立工作目录。
    - 用户已 release 的：立即删（release_job_files 已处理，此处兜底）
    - 未访问且超过 idle 秒数：删
    - 超过 max_keep 最旧条目：删
    """
    _DATA_DIR.mkdir(parents=True, exist_ok=True)
    now = time.time()
    idle_sec = max(300, int(settings.popular_video_mp4_idle_sec))
    max_keep = max(1, int(settings.popular_video_mp4_max_keep))

    mp4s: list[tuple[float, Path]] = []
    for p in _DATA_DIR.glob("*.mp4"):
        if p.is_file():
            mp4s.append((p.stat().st_mtime, p))

    mp4s.sort(key=lambda x: x[0], reverse=True)
    for idx, (mtime, path) in enumerate(mp4s):
        job_id = path.stem
        job = popular_video_store.get(job_id)
        too_old = (now - mtime) > idle_sec
        over_quota = idx >= max_keep
        is_keep = keep_job_id and job_id == keep_job_id and not too_old
        if job and job.released:
            too_old = True
        if is_keep:
            continue
        if too_old or over_quota:
            try:
                path.unlink(missing_ok=True)
                work = _DATA_DIR / job_id
                if work.is_dir():
                    shutil.rmtree(work, ignore_errors=True)
                if job and not job.released:
                    popular_video_store.remove(job_id)
                _log.info(
                    "WbVideoGen purged mp4 job=%s too_old=%s over_quota=%s released=%s",
                    job_id,
                    too_old,
                    over_quota,
                    bool(job and job.released),
                )
            except Exception as e:
                _log.warning("WbVideoGen purge failed %s: %s", path, e)

    for work in _DATA_DIR.iterdir():
        if not work.is_dir():
            continue
        if work.name == "reports":
            continue
        mp4 = _DATA_DIR / f"{work.name}.mp4"
        if not mp4.is_file():
            shutil.rmtree(work, ignore_errors=True)
            _log.info("WbVideoGen removed orphan work_dir=%s", work.name)
