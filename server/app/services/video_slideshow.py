"""封面幻灯片 + TTS 旁白 + 字幕 → MP4（依赖本机 ffmpeg）。"""

from __future__ import annotations

import logging
import shutil
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

from app.services.video_subtitles import find_cjk_font, write_srt

_log = logging.getLogger(__name__)

_ASSETS_DIR = Path(__file__).resolve().parent.parent.parent / "assets"
_BUILTIN_COVER = _ASSETS_DIR / "default_cover.jpg"

OUT_W = 1280
OUT_H = 720
FPS = 25


def ffmpeg_available() -> bool:
    return shutil.which("ffmpeg") is not None


def ffprobe_available() -> bool:
    return shutil.which("ffprobe") is not None


def wav_duration_seconds(wav_path: Path) -> float:
    with wave.open(str(wav_path), "rb") as w:
        frames = w.getnframes()
        rate = w.getframerate() or 24000
        return max(1.0, frames / float(rate))


def _run(cmd: list[str], *, timeout: int = 180) -> None:
    _log.info("ffmpeg cmd: %s", " ".join(cmd))
    proc = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    if proc.returncode != 0:
        err = (proc.stderr or proc.stdout or "")[-1200:]
        raise RuntimeError(err)


def _ffmpeg_filter_path(p: Path) -> str:
    """ffmpeg filter 内路径转义（Windows 友好）。"""
    s = p.resolve().as_posix()
    s = s.replace("\\", "/").replace(":", "\\:")
    s = s.replace("'", "\\'")
    return s


def ensure_builtin_cover() -> Path:
    _ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    if _BUILTIN_COVER.is_file() and _BUILTIN_COVER.stat().st_size > 500:
        return _BUILTIN_COVER
    _run(
        [
            "ffmpeg", "-y", "-f", "lavfi", "-i", "color=c=0xE07A3D:s=1280x720",
            "-vf", "scale=1280:720,drawbox=x=80:y=280:w=1120:h=160:color=0xFFFFFF@0.15:t=fill,format=yuv420p",
            "-frames:v", "1", str(_BUILTIN_COVER),
        ],
        timeout=30,
    )
    return _BUILTIN_COVER


def _path_needs_ascii_stage(p: Path) -> bool:
    """Windows 下 ffmpeg 对中文等非 ASCII 路径常报「找不到文件」。"""
    if sys.platform != "win32":
        return False
    try:
        str(p.resolve()).encode("ascii")
        return False
    except UnicodeEncodeError:
        return True


def image_to_jpeg(
    src: Path,
    dest: Path,
    *,
    width: int = OUT_W,
    height: int = OUT_H,
) -> None:
    """任意图片 → 16:9 JPEG（居中裁剪填满，避免左右黑边）。"""
    dest.parent.mkdir(parents=True, exist_ok=True)
    if not src.is_file():
        raise FileNotFoundError(f"图片源不存在: {src}")
    if src.stat().st_size < 100:
        raise RuntimeError(f"图片源过小: {src} ({src.stat().st_size} bytes)")
    vf = (
        f"scale={width}:{height}:force_original_aspect_ratio=increase,"
        f"crop={width}:{height},format=yuv420p"
    )
    use_stage = _path_needs_ascii_stage(src) or _path_needs_ascii_stage(dest)
    if use_stage:
        with tempfile.TemporaryDirectory(prefix="wb_ff_") as td:
            tin = Path(td) / "in.bin"
            tout = Path(td) / "out.jpg"
            shutil.copyfile(src, tin)
            _run(
                ["ffmpeg", "-y", "-i", str(tin), "-vf", vf, "-frames:v", "1", "-q:v", "2", str(tout)],
                timeout=90,
            )
            if not tout.is_file() or tout.stat().st_size < 300:
                raise RuntimeError("封面转 JPEG 失败（临时目录），文件过小。")
            shutil.copyfile(tout, dest)
    else:
        _run(
            ["ffmpeg", "-y", "-i", str(src), "-vf", vf, "-frames:v", "1", "-q:v", "2", str(dest)],
            timeout=90,
        )
    if not dest.is_file() or dest.stat().st_size < 300:
        raise RuntimeError("封面转 JPEG 失败，文件过小。")


def placeholder_cover_jpeg(dest: Path, *, title: str = "") -> None:
    _log.info("placeholder cover, title ignored: %s", (title or "")[:40])
    builtin = ensure_builtin_cover()
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(builtin, dest)


def _has_video_stream(mp4: Path) -> bool:
    if not ffprobe_available():
        return mp4.stat().st_size > 8000
    proc = subprocess.run(
        [
            "ffprobe", "-v", "error", "-select_streams", "v:0",
            "-show_entries", "stream=codec_type,width,height", "-of", "csv=p=0", str(mp4),
        ],
        capture_output=True, text=True, timeout=30,
    )
    out = (proc.stdout or "").strip()
    _log.info("ffprobe %s: %s", mp4.name, out)
    return proc.returncode == 0 and "video" in out


def render_ken_burns_clip(
    image_path: Path,
    out_mp4: Path,
    *,
    duration_sec: float,
    width: int = OUT_W,
    height: int = OUT_H,
    fps: int = FPS,
    end_zoom: float = 1.06,
) -> None:
    """
    平滑 Ken Burns：scale eval=frame + crop（替代 zoompan，减少抖动）。
    输入图应已为 16:9 crop。
    """
    out_mp4.parent.mkdir(parents=True, exist_ok=True)
    duration_sec = min(max(duration_sec, 8.0), 90.0)
    frame_count = max(int(duration_sec * fps), fps * 8)
    n_max = max(frame_count - 1, 1)
    z_delta = max(end_zoom - 1.0, 0.02)
    vf = (
        f"scale={width}:{height}:force_original_aspect_ratio=increase,"
        f"crop={width}:{height},"
        f"scale=w='{width}*(1+{z_delta:.4f}*n/{n_max})':"
        f"h='{height}*(1+{z_delta:.4f}*n/{n_max})':eval=frame,"
        f"crop={width}:{height}:(iw-{width})/2:(ih-{height})/2,"
        f"fps={fps},format=yuv420p,setsar=1"
    )
    _log.info(
        "WbVideoGen ken_burns_smooth frames=%s dur=%.1fs zoom=1.0->%.2f",
        frame_count, duration_sec, end_zoom,
    )
    _run(
        [
            "ffmpeg", "-y", "-loop", "1", "-framerate", str(fps), "-i", str(image_path),
            "-vf", vf, "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p",
            "-frames:v", str(frame_count), "-an", str(out_mp4),
        ],
        timeout=240,
    )
    if not out_mp4.is_file() or out_mp4.stat().st_size < 2000:
        raise RuntimeError("Ken Burns 画面轨生成失败。")


def _subtitle_vf(srt_path: Path) -> str | None:
    if not srt_path.is_file() or srt_path.stat().st_size < 4:
        return None
    esc = _ffmpeg_filter_path(srt_path)
    style = (
        "FontName=Microsoft YaHei,FontSize=22,PrimaryColour=&H00FFFFFF,"
        "OutlineColour=&H00000000,BorderStyle=1,Outline=2,Shadow=0,"
        "MarginV=28,Alignment=2"
    )
    font = find_cjk_font()
    if font:
        font_dir = _ffmpeg_filter_path(font.parent)
        return f"subtitles='{esc}':fontsdir='{font_dir}':force_style='{style}'"
    return f"subtitles='{esc}':force_style='{style}'"


def _mux_video_audio(
    silent_mp4: Path,
    wav_path: Path,
    out_mp4: Path,
    *,
    narration: str = "",
    work: Path | None = None,
) -> None:
    duration = wav_duration_seconds(wav_path)
    srt_path: Path | None = None
    sub_vf: str | None = None
    if work is not None:
        srt_path = work / "narration.srt"
        if not srt_path.is_file() or srt_path.stat().st_size < 4:
            if (narration or "").strip():
                write_srt(narration.strip(), duration, srt_path)
        if srt_path.is_file() and srt_path.stat().st_size >= 4:
            sub_vf = _subtitle_vf(srt_path)

    if sub_vf:
        _log.info("WbVideoGen merge burn subtitles srt=%s", srt_path.name if srt_path else "")
        _run(
            [
                "ffmpeg", "-y", "-i", str(silent_mp4), "-i", str(wav_path),
                "-vf", sub_vf,
                "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart", "-shortest", str(out_mp4),
            ],
            timeout=300,
        )
    else:
        _run(
            [
                "ffmpeg", "-y", "-i", str(silent_mp4), "-i", str(wav_path),
                "-c:v", "copy", "-c:a", "aac", "-b:a", "128k",
                "-movflags", "+faststart", "-shortest", str(out_mp4),
            ],
            timeout=180,
        )

    if not out_mp4.is_file() or out_mp4.stat().st_size < 3000:
        raise RuntimeError("成片文件过小。")
    if not _has_video_stream(out_mp4):
        raise RuntimeError("成片缺少视频画面轨。")


def mp4_duration_seconds(mp4_path: Path) -> float:
    if not mp4_path.is_file():
        return 0.0
    if ffprobe_available():
        proc = subprocess.run(
            [
                "ffprobe", "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", str(mp4_path),
            ],
            capture_output=True,
            text=True,
            timeout=30,
        )
        if proc.returncode == 0:
            try:
                return max(0.5, float((proc.stdout or "").strip()))
            except ValueError:
                pass
    return 5.0


def normalize_video_clip(
    src_mp4: Path,
    dest_mp4: Path,
    *,
    width: int = OUT_W,
    height: int = OUT_H,
    fps: int = FPS,
) -> None:
    """将任意 MP4 规范为 16:9 H.264 无音轨片段。"""
    dest_mp4.parent.mkdir(parents=True, exist_ok=True)
    vf = (
        f"scale={width}:{height}:force_original_aspect_ratio=increase,"
        f"crop={width}:{height},fps={fps},format=yuv420p,setsar=1"
    )
    _run(
        [
            "ffmpeg", "-y", "-i", str(src_mp4), "-vf", vf,
            "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p",
            "-an", str(dest_mp4),
        ],
        timeout=180,
    )


def _concat_video_clips(clip_paths: list[Path], out_mp4: Path, work: Path) -> None:
    concat_txt = work / "body_concat.txt"
    lines = []
    for c in clip_paths:
        p = c.resolve().as_posix().replace("'", "'\\''")
        lines.append(f"file '{p}'")
    concat_txt.write_text("\n".join(lines), encoding="utf-8")
    _run(
        [
            "ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", str(concat_txt),
            "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p", "-r", str(FPS),
            str(out_mp4),
        ],
        timeout=300,
    )


def merge_intro_slides_and_audio(
    intro_mp4: Path,
    image_paths: list[Path],
    wav_path: Path,
    out_mp4: Path,
    *,
    narration: str = "",
    width: int = OUT_W,
    height: int = OUT_H,
    fps: int = FPS,
) -> None:
    """D3：片头 MP4 + 轮播 Ken Burns + 口播硬字幕。"""
    if not ffmpeg_available():
        raise RuntimeError("未找到 ffmpeg，请安装并加入 PATH 后重试。")
    paths = [p for p in image_paths if p.is_file()]
    if not intro_mp4.is_file():
        if paths:
            merge_slides_and_audio(
                paths, wav_path, out_mp4, narration=narration, width=width, height=height, fps=fps,
            )
        else:
            raise RuntimeError("无片头且无轮播图")
        return

    work = intro_mp4.parent
    total_d = min(max(wav_duration_seconds(wav_path), 10.0), 90.0)
    intro_norm = work / "intro_norm.mp4"
    normalize_video_clip(intro_mp4, intro_norm, width=width, height=height, fps=fps)
    intro_d = min(mp4_duration_seconds(intro_norm), total_d * 0.4)
    intro_d = max(intro_d, 2.0)

    body_clips: list[Path] = []
    if paths:
        body_d = max(total_d - intro_d, len(paths) * 4.0)
        per = max(body_d / len(paths), 4.0)
        _log.info(
            "WbVideoGen merge mode=intro+carousel intro=%.1fs slides=%s per=%.1fs",
            intro_d,
            len(paths),
            per,
        )
        for i, src in enumerate(paths):
            frame_jpg = work / f"carousel_{i}.jpg"
            clip_mp4 = work / f"carousel_{i}.mp4"
            image_to_jpeg(src, frame_jpg, width=width, height=height)
            render_ken_burns_clip(
                frame_jpg, clip_mp4, duration_sec=per, width=width, height=height, fps=fps,
            )
            body_clips.append(clip_mp4)
    else:
        _log.info("WbVideoGen merge mode=intro_extend intro=%.1fs total=%.1fs", intro_d, total_d)
        silent_mp4 = work / "silent_intro_extend.mp4"
        _run(
            [
                "ffmpeg", "-y", "-stream_loop", "-1", "-i", str(intro_norm),
                "-t", f"{total_d:.3f}",
                "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p", "-an",
                str(silent_mp4),
            ],
            timeout=180,
        )
        _mux_video_audio(silent_mp4, wav_path, out_mp4, narration=narration, work=work)
        _log.info("merge ok intro_extend final=%s", out_mp4.stat().st_size)
        return

    # 片头可能长于 intro_d：截断到 intro_d
    intro_cut = work / "intro_cut.mp4"
    _run(
        [
            "ffmpeg", "-y", "-i", str(intro_norm), "-t", f"{intro_d:.3f}",
            "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p", "-an", str(intro_cut),
        ],
        timeout=120,
    )
    silent_mp4 = work / "silent_intro_carousel.mp4"
    _concat_video_clips([intro_cut, *body_clips], silent_mp4, work)
    _mux_video_audio(silent_mp4, wav_path, out_mp4, narration=narration, work=work)
    _log.info("merge ok intro+carousel final=%s", out_mp4.stat().st_size)


def merge_intro_cover_and_audio(
    intro_mp4: Path,
    image_path: Path,
    wav_path: Path,
    out_mp4: Path,
    *,
    narration: str = "",
    width: int = OUT_W,
    height: int = OUT_H,
    fps: int = FPS,
) -> None:
    """D3 片头 + 封面 Ken Burns 兜底。"""
    merge_intro_slides_and_audio(
        intro_mp4,
        [image_path] if image_path.is_file() else [],
        wav_path,
        out_mp4,
        narration=narration,
        width=width,
        height=height,
        fps=fps,
    )


def merge_cover_and_audio(
    image_path: Path,
    wav_path: Path,
    out_mp4: Path,
    *,
    narration: str = "",
    width: int = OUT_W,
    height: int = OUT_H,
    fps: int = FPS,
) -> None:
    if not ffmpeg_available():
        raise RuntimeError("未找到 ffmpeg，请安装并加入 PATH 后重试。")
    out_mp4.parent.mkdir(parents=True, exist_ok=True)
    work = image_path.parent
    frame_jpg = work / "frame_norm.jpg"
    silent_mp4 = work / "silent.mp4"

    image_to_jpeg(image_path, frame_jpg, width=width, height=height)
    duration = min(max(wav_duration_seconds(wav_path), 10.0), 90.0)
    _log.info("WbVideoGen merge mode=ken_burns duration=%.1fs", duration)
    render_ken_burns_clip(frame_jpg, silent_mp4, duration_sec=duration, width=width, height=height, fps=fps)
    _mux_video_audio(silent_mp4, wav_path, out_mp4, narration=narration, work=work)
    _log.info("merge ok ken_burns final=%s", out_mp4.stat().st_size)


def merge_slides_and_audio(
    image_paths: list[Path],
    wav_path: Path,
    out_mp4: Path,
    *,
    narration: str = "",
    width: int = OUT_W,
    height: int = OUT_H,
    fps: int = FPS,
) -> None:
    if not ffmpeg_available():
        raise RuntimeError("未找到 ffmpeg，请安装并加入 PATH 后重试。")
    paths = [p for p in image_paths if p.is_file()]
    if not paths:
        raise RuntimeError("无可用轮播图")
    if len(paths) == 1:
        merge_cover_and_audio(
            paths[0], wav_path, out_mp4, narration=narration, width=width, height=height, fps=fps,
        )
        return

    work = paths[0].parent
    duration = min(max(wav_duration_seconds(wav_path), 10.0), 90.0)
    per = max(duration / len(paths), 5.0)
    _log.info("WbVideoGen merge mode=carousel slides=%s per=%.1fs", len(paths), per)

    clip_paths: list[Path] = []
    for i, src in enumerate(paths):
        frame_jpg = work / f"carousel_{i}.jpg"
        clip_mp4 = work / f"carousel_{i}.mp4"
        image_to_jpeg(src, frame_jpg, width=width, height=height)
        render_ken_burns_clip(
            frame_jpg, clip_mp4, duration_sec=per, width=width, height=height, fps=fps,
        )
        clip_paths.append(clip_mp4)

    concat_txt = work / "carousel_concat.txt"
    lines = []
    for c in clip_paths:
        p = c.resolve().as_posix().replace("'", "'\\''")
        lines.append(f"file '{p}'")
    concat_txt.write_text("\n".join(lines), encoding="utf-8")

    silent_mp4 = work / "silent_carousel.mp4"
    _run(
        [
            "ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", str(concat_txt),
            "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p", "-r", str(fps),
            str(silent_mp4),
        ],
        timeout=300,
    )
    _mux_video_audio(silent_mp4, wav_path, out_mp4, narration=narration, work=work)
    _log.info("merge ok carousel slides=%s final=%s", len(paths), out_mp4.stat().st_size)
