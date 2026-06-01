"""通俗视频生成任务（内存；重启清空）。"""

from __future__ import annotations

import time
import uuid
from dataclasses import dataclass, field
from typing import Optional

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


def new_job_id() -> str:
    return f"pv-{uuid.uuid4().hex[:12]}"


def put(job: PopularVideoJob) -> None:
    job.updated_at = time.time()
    _jobs[job.job_id] = job


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
