"""从抖音/B 站整段口令里抽标题关键词，用于列表展示与联网检索。"""

from __future__ import annotations

import re
from typing import Iterable

_STOP = frozenset(
    {
        "作品",
        "视频",
        "点击",
        "链接",
        "复制",
        "打开",
        "看看",
        "抖音",
        "哔哩",
        "bilibili",
        "河美",
    }
)


def _dedupe_keep_order(items: Iterable[str]) -> list[str]:
    seen: set[str] = set()
    out: list[str] = []
    for x in items:
        x = (x or "").strip()
        if not x or x in seen:
            continue
        seen.add(x)
        out.append(x)
    return out


def _extract_hashtags(text: str) -> list[str]:
    """解析 #话题# 与未闭合尾标签（如 #牢大 https://，旧正则会漏掉）。"""
    text_no_url = re.sub(r"https?://\S+", " ", text)
    segments = re.split(r"#+", text_no_url)
    out: list[str] = []
    for seg in segments[1:]:
        tag = seg.strip().split("\n")[0].strip().rstrip(".,，…、 ")
        if not tag or tag in _STOP or tag in out or re.match(r"^\.+$", tag):
            continue
        if len(tag) > 14:
            continue
        if any(x in tag for x in ("复制打开", "看看", "抖音", "哔哩", "http")):
            continue
        out.append(tag)
    return out


def extract_note_entities(note: str) -> list[str]:
    """从分享备注/摘要抽实体（如「那维莱特深渊」「西华大学招生」）。"""
    n = (note or "").strip()
    if not n or len(n) < 2:
        return []
    out: list[str] = []
    for part in re.split(r"[,，、|/\n]", n):
        w = part.strip().strip("「」\"' ")
        if not w or w in _STOP or len(w) < 2 or len(w) > 16:
            continue
        if re.fullmatch(r"[A-Za-z0-9_]+", w):
            continue
        if w not in out:
            out.append(w)
    return out[:4]


def extract_keywords(raw: str) -> list[str]:
    if not raw or not raw.strip():
        return []
    text = re.sub(r"https?://\S+", " ", raw)
    from_note = extract_note_entities(raw)
    from_hash: list[str] = _extract_hashtags(raw)

    brackets = re.findall(r"【([^】]{1,40})】", text)
    from_bracket: list[str] = []
    for b in brackets:
        b = re.sub(r"^[✨⭐🌟\s\u200d]+", "", b)
        b = re.sub(r"[的作品UP主视频]+$", "", b).strip()
        if 2 <= len(b) <= 18 and b not in _STOP and "作品" not in b:
            from_bracket.append(b)

    from_phrase: list[str] = []
    m = re.search(r"([\u4e00-\u9fff]{2,4})来[\u4e00-\u9fff]", text)
    if m:
        from_phrase.append(m.group(1))
    m2 = re.search(r"([\u4e00-\u9fff]{2,6})在[\u4e00-\u9fff]{2,}", text)
    if m2:
        w = m2.group(1)
        if w not in _STOP:
            from_phrase.append(w)

    # 备注实体 > 话题标签 > 括号词
    if from_note:
        merged = _dedupe_keep_order(from_note + from_hash + from_phrase)
    elif from_hash:
        merged = _dedupe_keep_order(from_hash + from_phrase)
    else:
        merged = _dedupe_keep_order(from_hash + from_phrase + from_bracket)
    return [c for c in merged if c not in _STOP][:6]


def sanitize_display_title(s: str) -> str:
    """列表/详情展示用标题：去掉链接、抖音口令尾部噪声。"""
    if not (s or "").strip():
        return ""
    t = s.strip()
    t = re.split(r"https?://", t, maxsplit=1)[0]
    t = re.sub(r"https?://\S+", "", t)
    t = re.sub(r"\s*oqr:/\S*", "", t, flags=re.I)
    t = re.sub(r"\s*\d{1,2}/\d{1,2}\s+", " ", t)
    t = re.sub(r"\s*:\d{1,2}[ap]m\s*", " ", t, flags=re.I)
    t = re.sub(r"\s*e@[a-z0-9.]+\s*", " ", t, flags=re.I)
    t = re.sub(r"复制打开抖音[，,]?", "", t, flags=re.I)
    t = re.sub(r"^[\d.:\s]+", "", t)
    t = re.sub(r"\s+", " ", t).strip().rstrip("，,、 ")
    t = re.sub(r"\.{3,}\s*$", "…", t)
    return t[:120] if t else ""


def title_from_share_paste(raw: str) -> str:
    # 】 后接正文：抖音口令最常见
    m = re.search(r"】\s*([^#\n【]{3,100}?)(?=\s*#|\s*$|\n)", raw)
    if m:
        line = sanitize_display_title(m.group(1))
        if len(line) >= 4:
            return line

    text = re.sub(r"https?://\S+", "", raw).strip()
    text = re.sub(r"^[\d.:\s]+", "", text)
    text = re.sub(r"复制打开抖音[，,]?", "", text, flags=re.I)
    text = re.sub(r"^看看", "", text)

    m2 = re.search(r"【([^】]+)】", raw)
    if m2:
        inner = re.sub(r"^[✨⭐🌟\s\u200d]+", "", m2.group(1)).strip()
        inner = re.sub(r"[的作品]+$", "", inner).strip()
        if inner and 2 <= len(inner) <= 40:
            tail = ""
            m3 = re.search(r"】\s*([^#\n]{2,40})", raw)
            if m3:
                tail = m3.group(1).split("#")[0].strip().rstrip("，,")[:30]
            if tail and tail not in inner:
                return sanitize_display_title(f"{inner[:16]}｜{tail}")
            return sanitize_display_title(inner)

    first = sanitize_display_title(re.split(r"[\n#]", text, maxsplit=1)[0])
    if len(first) >= 4:
        return first
    return ""


def suggest_title_from_paste(raw: str, fetched_title: str) -> str:
    """口令优先：避免抖音抓取失败时列表只显示「分享的链接」。"""
    ft = (fetched_title or "").strip()
    raw = (raw or "").strip()
    paste_title = title_from_share_paste(raw) if raw else ""

    is_bad_ft = ft in ("", "分享的链接") or len(ft) < 3

    if is_bad_ft and paste_title:
        return paste_title

    if ft and ("bilibili" in ft.lower() or "哔哩" in ft):
        return sanitize_display_title(_clean_bili_title(ft))

    if paste_title and ("复制打开" in raw or "抖音" in raw):
        return paste_title

    if not is_bad_ft:
        return sanitize_display_title(ft)

    kws = extract_keywords(raw)
    if kws:
        return f"关于「{kws[0]}」的分享"[:120]

    return sanitize_display_title(ft) or "分享的链接"


def _clean_bili_title(title: str) -> str:
    t = re.sub(r"[_\-]\s*哔哩哔哩[_\-]?bilibili.*$", "", title, flags=re.I).strip()
    t = re.sub(r"\s+", " ", t)
    return t[:120] if t else title[:120]
