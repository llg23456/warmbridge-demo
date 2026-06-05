"""通俗视频 Job 磁盘持久化（重启可恢复 done/failed/interrupted）。"""

from __future__ import annotations

import json
import logging
from dataclasses import asdict
from pathlib import Path

from app.services.popular_video_store import PopularVideoJob

_log = logging.getLogger(__name__)

_JOBS_DIR = Path(__file__).resolve().parent.parent.parent / "data" / "popular_videos" / "jobs"


def _job_path(job_id: str) -> Path:
    return _JOBS_DIR / f"{job_id}.json"


def save_job(job: PopularVideoJob) -> None:
    _JOBS_DIR.mkdir(parents=True, exist_ok=True)
    data = asdict(job)
    _job_path(job.job_id).write_text(
        json.dumps(data, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def delete_job(job_id: str) -> None:
    path = _job_path(job_id)
    if path.is_file():
        path.unlink(missing_ok=True)


def load_all_jobs() -> list[PopularVideoJob]:
    if not _JOBS_DIR.is_dir():
        return []
    out: list[PopularVideoJob] = []
    for path in sorted(_JOBS_DIR.glob("pv-*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            out.append(PopularVideoJob(**data))
        except Exception as e:
            _log.warning("WbVideoGen persist load fail %s: %s", path.name, e)
    return out


def restore_jobs_on_startup() -> int:
    """加载磁盘任务；running → interrupted。"""
    jobs = load_all_jobs()
    if not jobs:
        return 0
    from app.services import popular_video_store

    n = 0
    for job in jobs:
        if job.status == "running":
            job.status = "interrupted"
            job.step = "interrupted"
            job.error_step = "interrupted"
            job.error_message = "服务重启，请重新生成"
        popular_video_store.put(job, persist=False)
        n += 1
    _log.info("WbVideoGen persist restored jobs=%s", n)
    return n
