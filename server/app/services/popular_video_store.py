"""通俗视频生成任务（内存 + 磁盘 jobs/*.json）。"""

from __future__ import annotations

import logging
import time
import uuid
from dataclasses import dataclass, field
from typing import Optional

_log = logging.getLogger(__name__)

_jobs: dict[str, "PopularVideoJob"] = {}


@dataclass
class PopularVideoJob:
    job_id: str
    item_id: str
    title: str = ""
    status: str = "running"  # running | done | failed
    step: str = "prepare"
    progress: int = 0
    error_step: str = ""
    error_message: str = ""
    video_url: str = ""
    share_page_url: str = ""
    narration_preview: str = ""
    created_at: float = field(default_factory=time.time)
    updated_at: float = field(default_factory=time.time)
    mp4_served: bool = False
    released: bool = False
    released_at: float = 0.0


def new_job_id() -> str:
    return f"pv-{uuid.uuid4().hex[:12]}"


def put(job: PopularVideoJob, *, persist: bool = True) -> None:
    job.updated_at = time.time()
    _jobs[job.job_id] = job
    if persist:
        try:
            from app.services.popular_video_persist import save_job

            save_job(job)
        except Exception as e:
            _log.warning("WbVideoGen persist save fail job=%s: %s", job.job_id, e)


def get(job_id: str) -> Optional[PopularVideoJob]:
    return _jobs.get(job_id)


def list_recent(limit: int = 30) -> list[PopularVideoJob]:
    items = sorted(_jobs.values(), key=lambda j: j.updated_at, reverse=True)
    return items[:limit]


def find_running_for_item(item_id: str) -> Optional[PopularVideoJob]:
    for j in _jobs.values():
        if j.item_id == item_id and j.status == "running":
            return j
    return None


def remove(job_id: str) -> None:
    _jobs.pop(job_id, None)
    try:
        from app.services.popular_video_persist import delete_job

        delete_job(job_id)
    except Exception as e:
        _log.warning("WbVideoGen persist delete fail job=%s: %s", job_id, e)


def mark_mp4_served(job_id: str) -> None:
    j = _jobs.get(job_id)
    if j:
        j.mp4_served = True
        j.updated_at = time.time()


def mark_released(job_id: str) -> None:
    j = _jobs.get(job_id)
    if j:
        j.released = True
        j.released_at = time.time()
        j.video_url = ""
        j.updated_at = time.time()
