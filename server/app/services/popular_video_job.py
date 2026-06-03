"""通俗视频生成流水线 MVP-D2：提取 → analyze → media(文生图+TTS) → 轮播合成。"""



from __future__ import annotations



import asyncio

import json

import logging

from pathlib import Path



import httpx



from app.schemas import FeedItem

from app.services import link_preview, popular_video_store, store, video_platform

from app.services.popular_video_analyze import analyze_video_content

from app.services.popular_video_cleanup import cleanup_work_dir, purge_disk

from app.services.popular_video_media import generate_slide_images

from app.services.popular_video_store import PopularVideoJob

from app.services.video_slideshow import (

    ffmpeg_available,

    image_to_jpeg,

    merge_cover_and_audio,

    merge_slides_and_audio,

    placeholder_cover_jpeg,

)

from app.services.vivo_tts import synthesize_wav



_log = logging.getLogger(__name__)



_DATA_DIR = Path(__file__).resolve().parent.parent.parent / "data" / "popular_videos"

_STEP_PROGRESS = {

    "prepare": (8, "正在准备视频信息与提取文字…"),

    "analyze": (28, "正在分析内容并撰写口播…"),

    "media": (55, "正在生成画面与语音…"),

    "merge": (82, "正在合成轮播画面与语音…"),

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





async def _ocr_cover_optional(cover_raw: Path) -> str:

    if not cover_raw.is_file() or cover_raw.stat().st_size < 100:

        return ""

    try:

        from app.services.vivo_ocr import ocr_image_bytes



        text = await ocr_image_bytes(cover_raw.read_bytes())

        return (text or "").strip()

    except Exception as e:

        _log.warning("WbVideoGen cover ocr skip: %s", e)

        return ""





async def _resolve_link_context(it: FeedItem) -> link_preview.LinkContext:

    page_url = (it.url or "").strip()

    if not page_url:

        return link_preview.LinkContext(title=it.title, description="", image_url="", page_text="")

    try:

        ctx = await link_preview.fetch_link_context(page_url, timeout=14.0)

        if ctx.image_url:

            it.preview_image_url = ctx.image_url

        if ctx.title and len(ctx.title) > 2 and it.title in ("", "分享的链接"):

            it.title = ctx.title[:120]

        if ctx.description and not (it.page_description or "").strip():

            it.page_description = ctx.description[:2000]

        return ctx

    except Exception as e:

        _log.warning("WbVideoGen link context fail item=%s: %s", it.id, e)

        return link_preview.LinkContext(

            title=it.title,

            description=(it.page_description or "")[:2000],

            image_url=(it.preview_image_url or "")[:800],

            page_text="",

        )





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





async def _synthesize_wav_to(narration: str, wav_path: Path) -> None:

    wav_bytes = await synthesize_wav(narration)

    wav_path.write_bytes(wav_bytes)





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

    analyze_path = work / "analyze.json"

    mp4_path = output_mp4_path(job_id)



    page_text = ""

    cover_ocr = ""

    used_placeholder = False

    slide_paths: list[Path] = []



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



        ctx = await _resolve_link_context(it)

        page_text = (ctx.page_text or "").strip()

        if page_text:

            (work / "page_text.txt").write_text(page_text, encoding="utf-8")



        cover_url = (ctx.image_url or it.preview_image_url or "").strip()

        ok_cover = await _download_cover(cover_url, cover_raw, referer=(it.url or "").strip())

        if ok_cover:

            cover_ocr = await _ocr_cover_optional(cover_raw)

            if cover_ocr:

                (work / "cover_ocr.txt").write_text(cover_ocr, encoding="utf-8")

            try:

                image_to_jpeg(cover_raw, cover_path)

            except Exception as e:

                _log.warning("WbVideoGen job=%s cover convert fail: %s", job_id, e)

                ok_cover = False

        if not ok_cover:

            used_placeholder = True

            if ffmpeg_available():

                placeholder_cover_jpeg(cover_path, title=job.title)

            if not cover_path.exists():

                _set_step(job, "prepare", failed=True, err="封面下载失败且无法生成占位图，请检查 ffmpeg。")

                return



        _set_step(job, "analyze")

        analyze = await analyze_video_content(

            title=it.title,

            source=it.source,

            summary=it.summary,

            page_description=it.page_description or ctx.description,

            page_text=page_text,

            cover_ocr_text=cover_ocr,

            web_context=it.web_context or "",

            tag=it.tag or "",

        )

        job.narration_preview = analyze.narration[:120]

        popular_video_store.put(job)



        _set_step(job, "media")

        try:

            slide_paths, _ = await asyncio.gather(

                generate_slide_images(
                    work,
                    analyze.image_prompts,
                    job_id=job_id,
                    safe_topic=analyze.core_keyword or tag,
                ),

                _synthesize_wav_to(analyze.narration, wav_path),

            )

        except Exception as e:

            _set_step(job, "media", failed=True, err=f"语音合成失败：{e}")

            return



        analyze_path.write_text(

            json.dumps(

                {

                    "core_keyword": analyze.core_keyword,

                    "narration_preview": analyze.narration[:160],

                    "video_prompt": analyze.video_prompt,

                    "image_prompts": analyze.image_prompts,

                    "from_llm": analyze.from_llm,

                    "used_placeholder_cover": used_placeholder,

                    "slides_generated": len(slide_paths),

                },

                ensure_ascii=False,

                indent=2,

            ),

            encoding="utf-8",

        )

        _log.info(

            "WbVideoGen job=%s media slides=%s narration_len=%s core=%s",

            job_id,

            len(slide_paths),

            len(analyze.narration),

            analyze.core_keyword,

        )



        _set_step(job, "merge")

        if not ffmpeg_available():

            _set_step(job, "merge", failed=True, err="未安装 ffmpeg，无法合成视频。")

            return

        try:

            if slide_paths:

                merge_slides_and_audio(
                    slide_paths, wav_path, mp4_path, narration=analyze.narration
                )

            else:

                _log.info("WbVideoGen job=%s merge fallback cover ken_burns", job_id)

                merge_cover_and_audio(
                    cover_path, wav_path, mp4_path, narration=analyze.narration
                )

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

        _log.info(

            "WbVideoGen job=%s done url=%s slides=%s placeholder=%s",

            job_id,

            job.video_url,

            len(slide_paths),

            used_placeholder,

        )

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


