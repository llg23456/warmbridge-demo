"""口播稿 → SRT 字幕（供 ffmpeg 烧录）。"""

from __future__ import annotations

import logging
import re
from pathlib import Path

_log = logging.getLogger(__name__)

# 长辈向字幕：每行不宜过长
_MAX_CHARS = 18
_MIN_SEG_SEC = 1.8


def _split_sentences(text: str) -> list[str]:
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
            buf += p if p != "\n" else "。"
            s = buf.strip()
            if s:
                merged.append(s)
            buf = ""
        else:
            buf += p
    if buf.strip():
        merged.append(buf.strip())
    return merged


def _wrap_line(s: str, max_len: int) -> list[str]:
    s = re.sub(r"\s+", "", s)
    if len(s) <= max_len:
        return [s] if s else []
    out: list[str] = []
    while len(s) > max_len:
        out.append(s[:max_len])
        s = s[max_len:]
    if s:
        out.append(s)
    return out


def narration_to_cues(narration: str) -> list[str]:
    """拆成多条字幕 cue。"""
    cues: list[str] = []
    for sent in _split_sentences(narration):
        for line in _wrap_line(sent, _MAX_CHARS):
            cues.append(line)
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
