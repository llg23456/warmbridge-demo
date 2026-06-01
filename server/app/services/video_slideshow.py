"""封面幻灯片 + TTS 旁白 → MP4（依赖本机 ffmpeg）。"""

from __future__ import annotations

import logging
import shutil
import subprocess
import wave
from pathlib import Path

_log = logging.getLogger(__name__)

_ASSETS_DIR = Path(__file__).resolve().parent.parent.parent / "assets"
_BUILTIN_COVER = _ASSETS_DIR / "default_cover.jpg"


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
        err = (proc.stderr or proc.stdout or "")[-1000:]
        raise RuntimeError(err)


def ensure_builtin_cover() -> Path:
    """生成/复用内置兜底封面（无文字，避免 drawtext 中文方框）。"""
    _ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    if _BUILTIN_COVER.is_file() and _BUILTIN_COVER.stat().st_size > 500:
        return _BUILTIN_COVER
    _run(
        [
            "ffmpeg",
            "-y",
            "-f",
            "lavfi",
            "-i",
            "color=c=0xE07A3D:s=1280x720",
            "-vf",
            (
                "scale=1280:720,"
                "drawbox=x=80:y=280:w=1120:h=160:color=0xFFFFFF@0.15:t=fill,"
                "format=yuv420p"
            ),
            "-frames:v",
            "1",
            str(_BUILTIN_COVER),
        ],
        timeout=30,
    )
    return _BUILTIN_COVER


def image_to_jpeg(src: Path, dest: Path, *, width: int = 1280, height: int = 720) -> None:
    """任意图片/封面字节 → 标准 JPEG。"""
    dest.parent.mkdir(parents=True, exist_ok=True)
    if not src.is_file() or src.stat().st_size < 100:
        raise RuntimeError("封面源文件无效")
    vf = (
        f"scale={width}:{height}:force_original_aspect_ratio=decrease,"
        f"pad={width}:{height}:(ow-iw)/2:(oh-ih)/2:color=0x3D3D3D,format=yuv420p"
    )
    _run(
        [
            "ffmpeg",
            "-y",
            "-i",
            str(src),
            "-vf",
            vf,
            "-frames:v",
            "1",
            "-q:v",
            "2",
            str(dest),
        ],
        timeout=90,
    )
    if not dest.is_file() or dest.stat().st_size < 300:
        raise RuntimeError("封面转 JPEG 失败，文件过小。")


def placeholder_cover_jpeg(dest: Path, *, title: str = "") -> None:
    """
    无外链封面时的占位图：纯色品牌底 + 浅色条，**不写中文**（避免 ffmpeg 缺字体出现方框）。
    """
    _log.info("placeholder cover (no drawtext), title ignored for display: %s", (title or "")[:40])
    builtin = ensure_builtin_cover()
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(builtin, dest)


def _has_video_stream(mp4: Path) -> bool:
    if not ffprobe_available():
        return mp4.stat().st_size > 8000
    proc = subprocess.run(
        [
            "ffprobe",
            "-v",
            "error",
            "-select_streams",
            "v:0",
            "-show_entries",
            "stream=codec_type,width,height",
            "-of",
            "csv=p=0",
            str(mp4),
        ],
        capture_output=True,
        text=True,
        timeout=30,
    )
    out = (proc.stdout or "").strip()
    _log.info("ffprobe %s: %s", mp4.name, out)
    return proc.returncode == 0 and "video" in out


def merge_cover_and_audio(
    image_path: Path,
    wav_path: Path,
    out_mp4: Path,
    *,
    width: int = 1280,
    height: int = 720,
    fps: int = 25,
) -> None:
    """两步：静态图 → silent.mp4，再混流旁白。"""
    if not ffmpeg_available():
        raise RuntimeError("未找到 ffmpeg，请安装并加入 PATH 后重试。")
    out_mp4.parent.mkdir(parents=True, exist_ok=True)
    work = image_path.parent
    frame_jpg = work / "frame_norm.jpg"
    silent_mp4 = work / "silent.mp4"

    image_to_jpeg(image_path, frame_jpg)

    duration = wav_duration_seconds(wav_path)
    duration = min(max(duration, 10.0), 90.0)
    frame_count = max(int(duration * fps), fps * 10)

    scale_pad = (
        f"scale={width}:{height}:force_original_aspect_ratio=decrease,"
        f"pad={width}:{height}:(ow-iw)/2:(oh-ih)/2:color=0x3D3D3D,setsar=1,format=yuv420p"
    )

    _run(
        [
            "ffmpeg",
            "-y",
            "-loop",
            "1",
            "-i",
            str(frame_jpg),
            "-vf",
            scale_pad,
            "-c:v",
            "libx264",
            "-preset",
            "veryfast",
            "-pix_fmt",
            "yuv420p",
            "-r",
            str(fps),
            "-frames:v",
            str(frame_count),
            "-an",
            str(silent_mp4),
        ],
        timeout=180,
    )

    if not silent_mp4.is_file() or silent_mp4.stat().st_size < 2000:
        raise RuntimeError("画面轨生成失败（silent.mp4 过小）。")

    _run(
        [
            "ffmpeg",
            "-y",
            "-i",
            str(silent_mp4),
            "-i",
            str(wav_path),
            "-c:v",
            "copy",
            "-c:a",
            "aac",
            "-b:a",
            "128k",
            "-movflags",
            "+faststart",
            "-shortest",
            str(out_mp4),
        ],
        timeout=180,
    )

    if not out_mp4.is_file() or out_mp4.stat().st_size < 3000:
        raise RuntimeError("成片文件过小。")
    if not _has_video_stream(out_mp4):
        raise RuntimeError("成片缺少视频画面轨，请查看服务端 WbVideoGen 日志。")

    _log.info(
        "merge ok frame=%s silent=%s final=%s duration=%.1fs",
        frame_jpg.stat().st_size,
        silent_mp4.stat().st_size,
        out_mp4.stat().st_size,
        duration,
    )
