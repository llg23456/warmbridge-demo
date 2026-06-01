from __future__ import annotations

from pathlib import Path

from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import FileResponse

from app.schemas import (
    PopularVideoJobDto,
    PopularVideoJobsResponse,
    PopularVideoStartRequest,
    PopularVideoStartResponse,
    PopularVideoStatusResponse,
)
from app.services import popular_video_job, popular_video_store, store, video_platform
from app.services.popular_video_job import output_mp4_path, schedule_job
from app.services.popular_video_store import PopularVideoJob

router = APIRouter(prefix="/api", tags=["video-popular"])


def _public_base(request: Request) -> str:
    host = request.headers.get("host") or "127.0.0.1:8000"
    scheme = request.url.scheme or "http"
    return f"{scheme}://{host}"


def _job_dto(j: PopularVideoJob, request: Request) -> PopularVideoJobDto:
    base = _public_base(request)
    video_url = j.video_url
    if j.status == "done" and not video_url:
        video_url = f"{base}/api/video/popular/files/{j.job_id}.mp4"
    return PopularVideoJobDto(
        job_id=j.job_id,
        item_id=j.item_id,
        title=j.title,
        status=j.status,
        step=j.step,
        progress=j.progress,
        error_step=j.error_step,
        error_message=j.error_message,
        video_url=video_url,
        share_page_url=j.share_page_url,
        narration_preview=j.narration_preview,
        created_at=j.created_at,
    )


def item_eligible(item_id: str) -> tuple[bool, str]:
    it = store.get_any_item(item_id)
    if not it:
        return False, "条目不存在"
    url = (it.url or "").strip()
    src = (it.source or "").strip()
    if src not in ("孩子推荐", "快解析"):
        return False, "仅「孩子推荐」或「快解析」条目可生成通俗视频。"
    if not url:
        return False, "缺少视频链接。"
    if video_platform.is_supported_video_url(url):
        return True, ""
    return False, "暂仅支持哔哩哔哩、抖音链接的通俗视频生成。"


@router.get("/video/popular/eligible/{item_id}")
def eligible(item_id: str):
    ok, reason = item_eligible(item_id)
    return {"eligible": ok, "reason": reason}


@router.post("/video/popular/start", response_model=PopularVideoStartResponse)
async def start(req: PopularVideoStartRequest, request: Request):
    ok, reason = item_eligible(req.item_id)
    if not ok:
        raise HTTPException(status_code=400, detail=reason)

    running = popular_video_store.find_running_for_item(req.item_id)
    if running:
        return PopularVideoStartResponse(
            job_id=running.job_id,
            reused=True,
            message="已有任务进行中，继续等待即可。",
        )

    it = store.get_any_item(req.item_id)
    assert it is not None
    job = PopularVideoJob(
        job_id=popular_video_store.new_job_id(),
        item_id=req.item_id,
        title=(it.title or "通俗视频解读")[:80],
    )
    popular_video_store.put(job)
    schedule_job(job.job_id, public_base=_public_base(request))
    return PopularVideoStartResponse(job_id=job.job_id, reused=False, message="")


@router.get("/video/popular/{job_id}/status", response_model=PopularVideoStatusResponse)
def status(job_id: str, request: Request):
    j = popular_video_store.get(job_id)
    if not j:
        raise HTTPException(status_code=404, detail="任务不存在")
    step_labels = {
        "prepare": "正在准备视频信息…",
        "script": "正在撰写通俗讲解稿…",
        "tts": "正在合成语音…",
        "merge": "正在把封面做成画面并合成视频…",
        "done": "完成",
    }
    return PopularVideoStatusResponse(
        job=_job_dto(j, request),
        step_label=step_labels.get(j.step, j.step),
    )


@router.get("/video/popular/jobs", response_model=PopularVideoJobsResponse)
def list_jobs(request: Request, limit: int = 30):
    jobs = [_job_dto(j, request) for j in popular_video_store.list_recent(limit=limit)]
    return PopularVideoJobsResponse(jobs=jobs)


@router.get("/video/popular/files/{filename}")
def serve_file(filename: str):
    if not filename.endswith(".mp4") or ".." in filename or "/" in filename:
        raise HTTPException(status_code=400, detail="invalid filename")
    job_id = filename[:-4]
    path = output_mp4_path(job_id)
    if not path.is_file():
        raise HTTPException(status_code=404, detail="file not ready")
    return FileResponse(path, media_type="video/mp4", filename=f"warmbridge_{job_id}.mp4")
