"""通俗视频生成流水线：元数据 → 解读 → 口播稿 → TTS → ffmpeg 幻灯片。"""

from __future__ import annotations

import asyncio
import logging
from pathlib import Path

import httpx

from app.schemas import FeedItem
from app.services import link_preview, popular_video_store, store, video_platform
from app.services.popular_video_cleanup import cleanup_work_dir, purge_disk
from app.services.popular_video_narration import build_narration_script
from app.services.popular_video_store import PopularVideoJob
from app.services.video_slideshow import (
    ffmpeg_available,
    image_to_jpeg,
    merge_cover_and_audio,
    placeholder_cover_jpeg,
)
from app.services.vivo_llm import explain_from_material
from app.services.vivo_tts import synthesize_wav

_log = logging.getLogger(__name__)

_DATA_DIR = Path(__file__).resolve().parent.parent.parent / "data" / "popular_videos"
_STEP_PROGRESS = {
    "prepare": (5, "正在准备视频信息…"),
    "script": (30, "正在撰写通俗讲解稿…"),
    "tts": (55, "正在合成语音…"),
    "merge": (80, "正在把封面做成画面并合成视频…"),
    "done": (100, "完成"),
}


def output_mp4_path(job_id: str) -> Path:
    return _DATA_DIR / f"{job_id}.mp4"


def _set_step(job: PopularVideoJob, step: str, *, failed: bool = False, err: str = "") -> None:
    job.step = step
    if failed:
        job.status = "failed"
        job.error_step = step
        job.error_message = err[:500]
        prog = _STEP_PROGRESS.get(step, (job.progress, ""))[0]
        job.progress = max(0, prog - 5)
    else:
        job.progress = _STEP_PROGRESS.get(step, (job.progress, ""))[0]
    popular_video_store.put(job)
    _log.info(
        "WbVideoGen job=%s step=%s progress=%s status=%s err=%s",
        job.job_id,
        step,
        job.progress,
        job.status,
        err[:120] if err else "",
    )


async def _material_for_item(it: FeedItem) -> str:
    parts = [
        f"标题：{it.title}",
        f"来源：{it.source}",
        f"列表摘要：{it.summary}",
    ]
    if (it.page_description or "").strip():
        parts.append(f"页面简介：{it.page_description}")
    if (it.web_context or "").strip():
        parts.append(f"联网检索摘要：\n{it.web_context}")
    parts.append(f"链接：{it.url}")
    parts.append(
        "说明：本演示未下载完整视频，仅根据公开页面信息与文字解读生成 30～60 秒通俗讲解短片。"
    )
    return "\n".join(parts)


async def _resolve_cover_url(it: FeedItem) -> str:
    url = (it.preview_image_url or "").strip()
    page_url = (it.url or "").strip()
    if page_url and video_platform.is_supported_video_url(page_url):
        try:
            ctx = await link_preview.fetch_link_context(page_url, timeout=14.0)
            if ctx.image_url:
                it.preview_image_url = ctx.image_url
                if ctx.title and len(ctx.title) > 2:
                    it.title = ctx.title[:120]
                if ctx.description and not (it.page_description or "").strip():
                    it.page_description = ctx.description[:2000]
                url = ctx.image_url
        except Exception as e:
            _log.warning("WbVideoGen cover refresh fail item=%s: %s", it.id, e)
    return url


async def _download_cover(url: str, dest: Path, *, referer: str = "") -> bool:
    if not url.startswith(("http://", "https://")):
        return False
    headers = {
        "User-Agent": (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        ),
        "Accept": "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
    }
    ref = (referer or url).strip()
    if ref.startswith(("http://", "https://")):
        headers["Referer"] = ref
    try:
        async with httpx.AsyncClient(follow_redirects=True, timeout=25.0) as client:
            r = await client.get(url, headers=headers)
            r.raise_for_status()
            body = r.content[:6_000_000]
            if len(body) < 500:
                _log.warning("WbVideoGen cover too small url=%s bytes=%s", url[:80], len(body))
                return False
            dest.write_bytes(body)
        _log.info("WbVideoGen cover ok url=%s bytes=%s", url[:80], dest.stat().st_size)
        return True
    except Exception as e:
        _log.warning("WbVideoGen cover download fail url=%s: %s", url[:80], e)
        return False


async def run_popular_video_job(job_id: str, *, public_base: str) -> None:
    job = popular_video_store.get(job_id)
    if not job:
        return
    purge_disk()
    work = _DATA_DIR / job_id
    work.mkdir(parents=True, exist_ok=True)
    cover_raw = work / "cover_raw.bin"
    cover_path = work / "cover.jpg"
    wav_path = work / "narration.wav"
    mp4_path = output_mp4_path(job_id)

    try:
        _set_step(job, "prepare")
        it = store.get_any_item(job.item_id)
        if not it:
            _set_step(job, "prepare", failed=True, err="找不到该条分享，请返回重新打开。")
            return
        if not (it.url or "").strip():
            _set_step(job, "prepare", failed=True, err="该条目没有视频链接。")
            return
        job.title = it.title[:80] or "通俗视频解读"
        popular_video_store.put(job)

        cover_url = await _resolve_cover_url(it)
        _log.info(
            "WbVideoGen job=%s cover_url=%s preview=%s",
            job_id,
            (cover_url or "")[:100],
            (it.preview_image_url or "")[:100],
        )
        ok_cover = await _download_cover(
            cover_url,
            cover_raw,
            referer=(it.url or "").strip(),
        )
        if ok_cover:
            try:
                image_to_jpeg(cover_raw, cover_path)
            except Exception as e:
                _log.warning("WbVideoGen job=%s cover convert fail: %s", job_id, e)
                ok_cover = False
        if not ok_cover:
            _log.warning("WbVideoGen job=%s cover degraded to placeholder", job_id)
            if ffmpeg_available():
                placeholder_cover_jpeg(cover_path, title=job.title)
            if not cover_path.exists():
                _set_step(job, "prepare", failed=True, err="封面下载失败且无法生成占位图，请检查 ffmpeg。")
                return

        _set_step(job, "script")
        cached = store.get_cached_explain(job.item_id)
        if cached and (cached.plain_summary or "").strip():
            explain = cached
        else:
            material = await _material_for_item(it)
            preview = (it.preview_image_url or "").strip() or None
            explain = await explain_from_material(material, None, preview_image_url=preview)
            if explain.from_llm:
                store.cache_explain(job.item_id, explain)

        narration, from_llm = await build_narration_script(
            title=it.title,
            plain_summary=explain.plain_summary,
            background=explain.background,
            glossary=explain.glossary,
            page_hint=it.page_description or it.summary,
        )
        job.narration_preview = narration[:120]
        popular_video_store.put(job)
        _log.info("WbVideoGen job=%s narration from_llm=%s len=%s", job_id, from_llm, len(narration))

        _set_step(job, "tts")
        try:
            wav_bytes = await synthesize_wav(narration)
            wav_path.write_bytes(wav_bytes)
        except Exception as e:
            _set_step(job, "tts", failed=True, err=f"语音合成失败：{e}")
            return

        _set_step(job, "merge")
        if not ffmpeg_available():
            _set_step(job, "merge", failed=True, err="未安装 ffmpeg，无法合成视频。")
            return
        try:
            merge_cover_and_audio(cover_path, wav_path, mp4_path)
        except Exception as e:
            _set_step(job, "merge", failed=True, err=str(e))
            return

        base = public_base.rstrip("/")
        job.video_url = f"{base}/api/video/popular/files/{job_id}.mp4"
        job.share_page_url = (it.url or "").strip()
        job.status = "done"
        job.step = "done"
        job.progress = 100
        job.error_step = ""
        job.error_message = ""
        popular_video_store.put(job)
        purge_disk(keep_job_id=job_id)
        _log.info("WbVideoGen job=%s done url=%s (work dir cleaned)", job_id, job.video_url)
    except Exception as e:
        _log.exception("WbVideoGen job=%s fatal", job_id)
        j = popular_video_store.get(job_id)
        if j and j.status == "running":
            _set_step(j, j.step, failed=True, err=f"任务异常：{e}")
        purge_disk()
    finally:
        cleanup_work_dir(work)


def schedule_job(job_id: str, public_base: str) -> None:
    asyncio.create_task(run_popular_video_job(job_id, public_base=public_base))
