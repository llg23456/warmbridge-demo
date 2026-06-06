"""口播稿 → SRT 字幕（供 ffmpeg 烧录）。"""

from __future__ import annotations

import logging
import re
from dataclasses import dataclass
from pathlib import Path

_log = logging.getLogger(__name__)

# 长辈向字幕：单条最多 28 字，默认整句一条；超长仅在逗号/顿号处切分
_MAX_SUBTITLE_CHARS = 28
_MIN_SEG_SEC = 1.8
_SUBTITLE_BREAK_RE = re.compile(r"[，、,；;]")


@dataclass
class TimedSegment:
    """一句口播及其在整段 WAV 中的时间区间（秒）。"""

    text: str
    start_sec: float
    end_sec: float


_PUNCT_ONLY_RE = re.compile(r"^[。！？；.…、，,\s;:：!?]+$")
_SPEAKABLE_RE = re.compile(r"[\u4e00-\u9fffA-Za-z0-9]")


def is_speakable_sentence(text: str) -> bool:
    """TTS 可读：含至少一个汉字/字母/数字，非纯标点。"""
    s = re.sub(r"\s+", "", (text or "").strip())
    if not s:
        return False
    if _PUNCT_ONLY_RE.fullmatch(s):
        return False
    return bool(_SPEAKABLE_RE.search(s))


def _commit_sentence(buf: str, merged: list[str]) -> None:
    s = (buf or "").strip()
    if not s:
        return
    if is_speakable_sentence(s):
        merged.append(re.sub(r"\s+", "", s))
    elif merged:
        # 连续句号「。。」等：并入上一句，避免单独送 TTS
        merged[-1] += s.replace("\n", "")
    else:
        _log.debug("video_subtitles: drop leading punct-only segment %r", s[:8])


def split_sentences(text: str) -> list[str]:
    t = (text or "").strip()
    if not t:
        return []
    parts = re.split(r"([。！？；\n])", t)
    merged: list[str] = []
    buf = ""
    for p in parts:
        if not p:
            continue
        if p in "。！？；\n":
            buf += "。" if p == "\n" else p
            _commit_sentence(buf, merged)
            buf = ""
        else:
            buf += p
    if buf.strip():
        _commit_sentence(buf, merged)
    return merged


def subtitle_cues_for_sentence(sent: str, max_len: int = _MAX_SUBTITLE_CHARS) -> list[str]:
    """
    一句口播 → 字幕 cue 列表。
    默认整句一条；超过 max_len 时仅在逗号/顿号处切分，禁止按字数拆词。
    """
    s = re.sub(r"\s+", "", (sent or "").strip())
    if not s:
        return []
    if len(s) <= max_len:
        return [s]

    parts = [p.strip() for p in _SUBTITLE_BREAK_RE.split(s) if p.strip()]
    if not parts:
        return [s]

    cues: list[str] = []
    buf = ""
    for part in parts:
        if not buf:
            candidate = part
        else:
            candidate = f"{buf}，{part}"
        if len(candidate) <= max_len:
            buf = candidate
        else:
            if buf:
                cues.append(buf)
            buf = part
    if buf:
        cues.append(buf)
    return cues if cues else [s]


def narration_to_cues(narration: str) -> list[str]:
    """拆成多条字幕 cue（按句 + 逗号切分，不硬切字数）。"""
    cues: list[str] = []
    for sent in split_sentences(narration):
        cues.extend(subtitle_cues_for_sentence(sent))
    return cues


def _fmt_ts(sec: float) -> str:
    sec = max(0.0, sec)
    h = int(sec // 3600)
    m = int((sec % 3600) // 60)
    s = int(sec % 60)
    ms = int(round((sec - int(sec)) * 1000))
    return f"{h:02d}:{m:02d}:{s:02d},{ms:03d}"


def write_srt(narration: str, duration_sec: float, dest: Path) -> Path:
    """按字数比例分配时间轴，写入 UTF-8 SRT。"""
    dest.parent.mkdir(parents=True, exist_ok=True)
    cues = narration_to_cues(narration)
    if not cues:
        dest.write_text("", encoding="utf-8")
        return dest

    duration_sec = max(duration_sec, 5.0)
    weights = [max(len(c), 1) for c in cues]
    total_w = sum(weights) or 1
    lines: list[str] = []
    t = 0.0
    for idx, (cue, w) in enumerate(zip(cues, weights), start=1):
        seg = max(duration_sec * w / total_w, _MIN_SEG_SEC)
        if idx == len(cues):
            end = duration_sec
        else:
            end = min(t + seg, duration_sec - 0.2)
        if end <= t:
            end = min(t + _MIN_SEG_SEC, duration_sec)
        lines.append(str(idx))
        lines.append(f"{_fmt_ts(t)} --> {_fmt_ts(end)}")
        lines.append(cue)
        lines.append("")
        t = end
    dest.write_text("\n".join(lines), encoding="utf-8-sig")
    _log.info("WbVideoGen srt cues=%s duration=%.1fs path=%s", len(cues), duration_sec, dest.name)
    return dest


def write_srt_from_segments(segments: list[TimedSegment], dest: Path) -> Path:
    """按句级 TTS 实测时长写 SRT；句内按字数比例切行，不跨句漂移。"""
    dest.parent.mkdir(parents=True, exist_ok=True)
    lines: list[str] = []
    cue_idx = 1
    total_cues = 0

    for seg in segments:
        text = (seg.text or "").strip()
        if not text:
            continue
        cues = subtitle_cues_for_sentence(text)
        if not cues:
            continue
        seg_start = max(0.0, seg.start_sec)
        seg_end = max(seg_start + 0.05, seg.end_sec)
        seg_dur = seg_end - seg_start
        weights = [max(len(c), 1) for c in cues]
        total_w = sum(weights) or 1
        t = seg_start
        for i, (cue, w) in enumerate(zip(cues, weights)):
            if i == len(cues) - 1:
                end = seg_end
            else:
                end = min(t + seg_dur * w / total_w, seg_end - 0.05)
            if end <= t:
                end = min(t + 0.15, seg_end)
            lines.append(str(cue_idx))
            lines.append(f"{_fmt_ts(t)} --> {_fmt_ts(end)}")
            lines.append(cue)
            lines.append("")
            cue_idx += 1
            total_cues += 1
            t = end

    dest.write_text("\n".join(lines), encoding="utf-8-sig")
    total_dur = segments[-1].end_sec if segments else 0.0
    _log.info(
        "WbVideoGen srt aligned segments=%s cues=%s duration=%.1fs path=%s",
        len(segments),
        total_cues,
        total_dur,
        dest.name,
    )
    return dest


def find_cjk_font() -> Path | None:
    """Windows / Linux 常见中文字体路径。"""
    candidates = [
        Path(r"C:\Windows\Fonts\msyh.ttc"),
        Path(r"C:\Windows\Fonts\msyhbd.ttc"),
        Path(r"C:\Windows\Fonts\simhei.ttf"),
        Path("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"),
        Path("/System/Library/Fonts/PingFang.ttc"),
    ]
    assets = Path(__file__).resolve().parent.parent.parent / "assets" / "fonts"
    if assets.is_dir():
        for f in assets.glob("*"):
            if f.suffix.lower() in (".ttf", ".ttc", ".otf"):
                candidates.insert(0, f)
    for p in candidates:
        if p.is_file():
            return p
    return None
