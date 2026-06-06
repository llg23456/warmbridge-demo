"""轻量联网摘要：DuckDuckGo HTML / Bing + 中文维基 + 百科（通俗视频口播刷新）。"""

from __future__ import annotations

import html as html_module
import logging
import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from datetime import datetime
from typing import Any
from urllib.parse import quote_plus

import httpx

from app.config import settings
from app.services import paste_intel

_log = logging.getLogger(__name__)

_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
)
# https://meta.wikimedia.org/wiki/User-Agent_policy
_WIKI_UA = "WarmBridgeDemo/1.0 (contest demo; httpx; no commercial use)"

# 常见昵称 → 更易检索本名的词（仅作补充查询，不展示给长辈）
_NICK_EXTRA_QUERIES: dict[str, str] = {
    "嘎子": "谢孟伟",
    "牢大": "剑风传奇 漫画",
    "劳大": "剑风传奇 漫画",
}

_IMAGE_QUERY_OVERRIDES: dict[str, str] = {
    "皇室战争": "部落冲突 皇室战争 游戏截图 Supercell",
    "剑风传奇": "剑风传奇 漫画 格斯 封面",
    "牢大": "剑风传奇 漫画 暗黑奇幻 插画",
    "劳大": "剑风传奇 漫画 暗黑奇幻 插画",
    "原神": "原神 游戏 截图 提瓦特",
    "原神深渊": "原神 深境螺旋 游戏截图",
}

# 标题尾部噪声，剥离后更利于检索（如「皇室战争主义」→「皇室战争」）
_TITLE_SUFFIX_STRIP = (
    "主义",
    "论",
    "攻略",
    "教程",
    "宣传片",
    "短视频",
    "分享",
    "官方",
    "视频",
)

# 兴趣标签过泛，不宜作为检索词
_GENERIC_SEARCH_TAGS = frozenset(
    {
        "短视频",
        "视频",
        "生活",
        "健康",
        "数码",
        "吃瓜",
        "AI",
        "孩子推荐",
        "分享的链接",
        "招生",
        "宣传",
    }
)

# 情绪/氛围标签：可作补充，但不宜作为 core_keyword 与检索主词（玩梗视频常带 #绝望# 等）
_EMOTION_TAGS = frozenset(
    {
        "绝望",
        "开心",
        "破防",
        "泪目",
        "搞笑",
        "抽象",
        "治愈",
        "虐心",
        "社死",
        "尴尬",
        "真实",
        "火了",
        "热门",
        "共鸣",
        "心疼",
        "炸裂",
        "离谱",
        "整活",
        "玩梗",
        "梗",
        "emo",
        "泪崩",
        "破大防",
        "哈基米",
    }
)

# 作品/角色/梗实体提示（优先于标题字面、情绪词）
_WORK_ENTITY_MARKERS = (
    "传奇",
    "漫画",
    "动画",
    "动漫",
    "游戏",
    "手游",
    "番",
    "剧",
    "电影",
    "牢大",
    "劳大",
    "科比",
    "嘎子",
    "皇室战争",
    "原神",
)

# 封面检索跳过（真人肖像敏感 / 纯情绪无图）
_COVER_SKIP_KEYWORDS = frozenset(
    {
        "绝望",
        "牢大",
        "劳大",
        "科比",
        "开心",
        "破防",
        "搞笑",
        "抽象",
        "emo",
    }
)


def _collect_related_topics(nodes: list[Any], limit: int) -> list[str]:
    texts: list[str] = []
    for t in nodes:
        if len(texts) >= limit:
            break
        if isinstance(t, dict):
            tx = t.get("Text")
            if isinstance(tx, str) and tx.strip():
                texts.append(tx.strip())
            subs = t.get("Topics")
            if isinstance(subs, list):
                texts.extend(_collect_related_topics(subs, limit - len(texts)))
    return texts


def _strip_html(s: str) -> str:
    t = re.sub(r"<[^>]+>", "", s or "")
    return html_module.unescape(t).strip()


def _is_blocked_search_html(text: str) -> bool:
    """识别反爬跳转页 / 验证码页（如百度 227 字节 HTTPS→HTTP 脚本）。"""
    if not text:
        return True
    low = text.lower()
    if len(text) < 500 and (
        "location.replace" in low
        or "captcha" in low
        or "验证" in text
        or "security check" in low
    ):
        return True
    return False


def _normalize_title_for_search(title: str) -> str:
    t = title.strip()
    t = re.sub(r"\d{4}", "", t)
    t = re.sub(r"[招生宣传官方作品]+$", "", t).strip()
    return t


def _rank_entity_keywords(candidates: list[str], *, title: str) -> list[str]:
    title = title.strip()
    uniq = paste_intel._dedupe_keep_order(c for c in candidates if (c or "").strip())

    def score(kw: str) -> tuple[int, int]:
        s = 0
        if kw == title:
            s -= 20
        if any(title == kw + suf for suf in _TITLE_SUFFIX_STRIP):
            s += 30
        if 3 <= len(kw) <= 10:
            s += 10
        if kw.endswith("主义") or kw.endswith("论"):
            s -= 25
        if re.search(r"\d", kw):
            s -= 8
        return (-s, -len(kw))

    return sorted(uniq, key=score)


def _prune_substring_keywords(keywords: list[str]) -> list[str]:
    """保留更短的核心实体，去掉含它的更长碎片（如留「皇室战争」、去「皇室战争主义」）。"""
    ordered = paste_intel._dedupe_keep_order(k for k in keywords if (k or "").strip())
    drop: set[str] = set()
    for short in ordered:
        if len(short) < 3:
            continue
        for long in ordered:
            if long == short or long in drop:
                continue
            if len(long) > len(short) and short in long:
                drop.add(long)
    kept = [k for k in ordered if k not in drop]
    long_kws = [k for k in kept if len(k) >= 4]
    short_keep = frozenset({"牢大", "劳大", "嘎子"})
    out: list[str] = []
    for kw in kept:
        if long_kws and len(kw) <= 2 and kw not in short_keep:
            continue
        if long_kws and kw in ("战争", "主义", "皇室", "大学", "招生"):
            continue
        out.append(kw)
    return out


def primary_search_keyword(
    *,
    title: str = "",
    summary: str = "",
    tag: str = "",
    share_keywords: str = "",
) -> str:
    """单条核心检索词（封面 Bing 图 + 联网检索主词）。"""
    kws = extract_video_search_keywords(
        title=title,
        summary=summary,
        tag=tag,
        share_keywords=share_keywords,
    )
    return kws[0] if kws else ""


def keywords_for_cover(
    *,
    title: str = "",
    summary: str = "",
    tag: str = "",
    share_keywords: str = "",
) -> list[str]:
    """封面 Bing 检索词列表（跳过情绪词与真人梗昵称）。"""
    kws = extract_video_search_keywords(
        title=title,
        summary=summary,
        tag=tag,
        share_keywords=share_keywords,
    )
    return [k for k in kws if k not in _COVER_SKIP_KEYWORDS]


def _parse_share_keywords(share_keywords: str) -> list[str]:
    if not (share_keywords or "").strip():
        return []
    parts = re.split(r"[,，、|]", share_keywords)
    return [p.strip() for p in parts if p.strip()]


def extract_title_entities(title: str) -> list[str]:
    """从视频标题抽 1～3 个可检索实体（jieba 名词 + 去后缀/年份噪声）。"""
    t = (title or "").strip()
    if not t:
        return []

    candidates: list[str] = []
    normalized = _normalize_title_for_search(t)
    if normalized and normalized != t:
        candidates.append(normalized)

    for suf in _TITLE_SUFFIX_STRIP:
        if t.endswith(suf) and len(t) > len(suf) + 1:
            candidates.append(t[: -len(suf)])

    try:
        import jieba.posseg as pseg

        for word, flag in pseg.cut(t):
            if len(word) < 2:
                continue
            if word in _GENERIC_SEARCH_TAGS or word in paste_intel._STOP:
                continue
            if flag.startswith("n") or flag in ("eng", "nz", "nr", "ns", "nt"):
                candidates.append(word)
                if len(word) >= 4:
                    candidates.append(_normalize_title_for_search(word))
    except Exception as e:
        _log.debug("web_lookup: jieba skip title=%s: %s", t[:24], e)

    for m in re.finditer(r"[\u4e00-\u9fff]{3,12}", t):
        w = m.group(0)
        if w not in paste_intel._STOP:
            candidates.append(w)
            stripped = _normalize_title_for_search(w)
            if stripped and stripped != w:
                candidates.append(stripped)

    ranked = _rank_entity_keywords(candidates, title=t)
    return _prune_substring_keywords(ranked)


async def duckduckgo_instant_summary(query: str, timeout: float = 10.0) -> str:
    q = query.strip()
    if not q or not settings.web_search_enabled:
        return ""
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            r = await client.get(
                "https://api.duckduckgo.com/",
                params={
                    "q": q,
                    "format": "json",
                    "no_html": "1",
                    "no_redirect": "1",
                },
                headers={"User-Agent": _UA},
            )
            r.raise_for_status()
            data = r.json()
    except Exception:
        return ""

    parts: list[str] = []
    ab = data.get("AbstractText")
    if isinstance(ab, str) and ab.strip():
        parts.append(ab.strip())
    df = data.get("Definition")
    if isinstance(df, str) and df.strip():
        parts.append(df.strip())

    rel = data.get("RelatedTopics")
    if isinstance(rel, list):
        parts.extend(_collect_related_topics(rel, 5))

    out = "\n".join(parts)
    return out[:2800]


def _parse_ddg_html_results(html: str, *, max_items: int = 6) -> tuple[list[str], list[str]]:
    titles: list[str] = []
    snippets: list[str] = []
    for pat in (
        r'class="result__a"[^>]*>([^<]{4,200})',
        r'class="result__title"[^>]*>.*?<a[^>]*>([^<]{4,200})',
    ):
        for m in re.finditer(pat, html, re.I | re.S):
            t = _strip_html(m.group(1))
            if t and t not in titles:
                titles.append(t)
            if len(titles) >= max_items:
                break
        if titles:
            break

    for pat in (
        r'class="result__snippet"[^>]*>([^<]{12,400})',
        r'class="result__snippet[^"]*"[^>]*>([^<]{12,400})',
    ):
        for m in re.finditer(pat, html, re.I | re.S):
            s = _strip_html(m.group(1))
            if s and s not in snippets:
                snippets.append(s)
            if len(snippets) >= max_items:
                break
        if snippets:
            break

    return titles[:max_items], snippets[:max_items]


def _format_search_result_lines(
    *,
    engine_label: str,
    search_q: str,
    titles: list[str],
    snippets: list[str],
    max_items: int = 6,
) -> str:
    if not titles and not snippets:
        return ""
    lines: list[str] = [f"（{engine_label}：{search_q}）"]
    n = max(len(titles), len(snippets))
    for i in range(min(n, max_items)):
        if i < len(titles):
            line = f"{i + 1}. {titles[i]}"
            if i < len(snippets) and snippets[i] not in titles[i]:
                line += f" — {snippets[i][:180]}"
            lines.append(line)
        elif i < len(snippets):
            lines.append(f"{i + 1}. {snippets[i][:200]}")
    return "\n".join(lines)[:2200]


async def duckduckgo_html_search_snippets(
    keyword: str,
    *,
    timeout: float = 12.0,
    max_items: int = 6,
) -> str:
    """DuckDuckGo HTML 版（纯 HTML SERP，适合无 JS 客户端）。"""
    kw = keyword.strip()
    if not kw or not settings.web_search_enabled:
        return ""
    search_q = _disambiguate_web_query(kw)
    if search_q == kw and "简介" not in search_q:
        search_q = f"{kw} 简介"
    text = ""
    status = 0
    try:
        async with httpx.AsyncClient(timeout=timeout, follow_redirects=True) as client:
            r = await client.post(
                "https://html.duckduckgo.com/html/",
                data={"q": search_q, "b": "", "kl": "wt-wt"},
                headers={
                    "User-Agent": _UA,
                    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
                    "Content-Type": "application/x-www-form-urlencoded",
                },
            )
            status = r.status_code
            text = r.text[:400_000]
    except Exception as e:
        _log.info(
            "web_lookup: engine=duckduckgo query=%s status=error len=0 err=%s",
            search_q,
            e,
        )
        return ""

    _log.info(
        "web_lookup: engine=duckduckgo query=%s status=%s len=%s",
        search_q,
        status,
        len(text),
    )
    _log.info(
        "web_lookup: engine=duckduckgo query=%s snippet_preview=%s",
        search_q,
        text[:200].replace("\n", " "),
    )

    if status >= 400 or _is_blocked_search_html(text):
        _log.info(
            "web_lookup: engine=duckduckgo query=%s extract_len=0 reason=blocked_or_error",
            search_q,
        )
        return ""

    titles, snippets = _parse_ddg_html_results(text, max_items=max_items * 2)
    titles, snippets = _filter_web_serp_results(
        titles, snippets, keyword=kw, max_items=max_items
    )
    extracted = _format_search_result_lines(
        engine_label="DuckDuckGo HTML",
        search_q=search_q,
        titles=titles,
        snippets=snippets,
        max_items=max_items,
    )
    _log.info(
        "web_lookup: engine=duckduckgo query=%s extract_len=%s",
        search_q,
        len(extracted),
    )
    return extracted


def _parse_bing_html_results(html: str, *, max_items: int = 6) -> tuple[list[str], list[str]]:
    titles: list[str] = []
    snippets: list[str] = []
    for block in re.finditer(r'<li class="b_algo"[\s\S]*?</li>', html, re.I):
        chunk = block.group(0)
        tm = re.search(r"<h2[^>]*>\s*<a[^>]*>([^<]{4,200})", chunk, re.I | re.S)
        sm = re.search(r'<p[^>]*>([^<]{12,400})', chunk, re.I | re.S)
        if tm:
            t = _strip_html(tm.group(1))
            if t and t not in titles:
                titles.append(t)
        if sm:
            s = _strip_html(sm.group(1))
            if s and s not in snippets:
                snippets.append(s)
        if len(titles) >= max_items:
            break
    return titles[:max_items], snippets[:max_items]


async def bing_web_search_snippets(
    keyword: str,
    *,
    timeout: float = 12.0,
    max_items: int = 6,
) -> str:
    """Bing 网页搜索（DDG 失败时的 fallback）。"""
    kw = keyword.strip()
    if not kw or not settings.web_search_enabled:
        return ""
    search_q = _disambiguate_web_query(kw)
    if search_q == kw and "简介" not in search_q and "最新" not in kw:
        search_q = f"{kw} 简介"
    text = ""
    status = 0
    try:
        async with httpx.AsyncClient(timeout=timeout, follow_redirects=True) as client:
            r = await client.get(
                "https://www.bing.com/search",
                params={"q": search_q, "setlang": "zh-Hans", "cc": "CN"},
                headers={
                    "User-Agent": _UA,
                    "Accept-Language": "zh-CN,zh;q=0.9",
                },
            )
            status = r.status_code
            text = r.text[:400_000]
    except Exception as e:
        _log.info(
            "web_lookup: engine=bing query=%s status=error len=0 err=%s",
            search_q,
            e,
        )
        return ""

    _log.info(
        "web_lookup: engine=bing query=%s status=%s len=%s",
        search_q,
        status,
        len(text),
    )
    _log.info(
        "web_lookup: engine=bing query=%s snippet_preview=%s",
        search_q,
        text[:200].replace("\n", " "),
    )

    if status >= 400 or _is_blocked_search_html(text):
        _log.info(
            "web_lookup: engine=bing query=%s extract_len=0 reason=blocked_or_error",
            search_q,
        )
        return ""

    titles, snippets = _parse_bing_html_results(text, max_items=max_items * 2)
    titles, snippets = _filter_web_serp_results(
        titles, snippets, keyword=kw, max_items=max_items
    )
    if not titles and not snippets:
        _log.info("web_lookup: engine=bing query=%s extract_len=0 reason=all_filtered", search_q)
        return ""
    extracted = _format_search_result_lines(
        engine_label="Bing",
        search_q=search_q,
        titles=titles,
        snippets=snippets,
        max_items=max_items,
    )
    _log.info(
        "web_lookup: engine=bing query=%s extract_len=%s",
        search_q,
        len(extracted),
    )
    return extracted


async def _wikipedia_page_extract(client: httpx.AsyncClient, title: str) -> str:
    r = await client.get(
        "https://zh.wikipedia.org/w/api.php",
        params={
            "action": "query",
            "format": "json",
            "prop": "extracts",
            "exintro": "true",
            "explaintext": "true",
            "titles": title,
            "redirects": "1",
        },
        headers={"User-Agent": _WIKI_UA, "Accept-Language": "zh-CN"},
    )
    r.raise_for_status()
    pages = r.json().get("query", {}).get("pages", {})
    for _pid, p in pages.items():
        ex = p.get("extract")
        if isinstance(ex, str) and ex.strip():
            return ex.strip()[:2800]
    return ""


async def wikipedia_zh_best_effort(search: str, timeout: float = 14.0) -> str:
    q = search.strip()
    if not q or not settings.web_search_enabled:
        return ""
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            r = await client.get(
                "https://zh.wikipedia.org/w/api.php",
                params={
                    "action": "opensearch",
                    "search": q,
                    "limit": "3",
                    "namespace": "0",
                    "format": "json",
                },
                headers={"User-Agent": _WIKI_UA, "Accept-Language": "zh-CN"},
            )
            r.raise_for_status()
            data = r.json()
            titles: list[str] = []
            if isinstance(data, list) and len(data) >= 2 and isinstance(data[1], list):
                titles = [t for t in data[1] if isinstance(t, str)]
            for title in titles[:2]:
                ex = await _wikipedia_page_extract(client, title)
                if ex:
                    return f"（中文维基摘录：{title}）\n{ex}"
    except Exception:
        return ""
    return ""


async def lookup_keyword_blurb(kw: str) -> str:
    """单关键词：维基 → DDG Instant，尽量凑一段可给 LLM 转述的材料。"""
    kw = kw.strip()
    if not kw:
        return ""
    blob = await wikipedia_zh_best_effort(kw)
    if blob:
        return blob
    q_short = kw if len(kw) > 5 else f"{kw} 是谁"
    blob = await duckduckgo_instant_summary(q_short)
    if blob:
        return blob
    if len(kw) <= 8:
        blob = await duckduckgo_instant_summary(f"{kw} 梗")
        if blob:
            return blob
        blob = await wikipedia_zh_best_effort(f"{kw} 演员")
        if blob:
            return blob
    return ""


async def build_web_context(keywords: list[str]) -> str:
    if not settings.web_search_enabled or not keywords:
        return ""
    chunks: list[str] = []
    seen: set[str] = set()
    for kw in keywords[:3]:
        kw = kw.strip()
        if not kw or kw in seen:
            continue
        seen.add(kw)
        blurb = await lookup_keyword_blurb(kw)
        if not blurb:
            extra = _NICK_EXTRA_QUERIES.get(kw)
            if extra:
                blurb = await lookup_keyword_blurb(extra)
        if blurb:
            chunks.append(f"【关键词：{kw}】\n{blurb}")
        if len("\n\n".join(chunks)) > 4000:
            break
    return "\n\n".join(chunks)[:4500]


@dataclass
class FreshWebResult:
    text: str = ""
    keywords_tried: list[str] = field(default_factory=list)
    attempt_log: list[str] = field(default_factory=list)
    fresh_len: int = 0
    stored_len: int = 0
    merged_len: int = 0
    enabled: bool = True
    empty_reason: str = ""


def _filter_search_keywords(keywords: list[str]) -> list[str]:
    out: list[str] = []
    for kw in keywords:
        k = (kw or "").strip()
        if not k or k in _GENERIC_SEARCH_TAGS:
            continue
        if len(k) < 2:
            continue
        out.append(k)
    return out


def _is_low_value_latin_tag(kw: str) -> bool:
    """纯英文粉丝号类口令标签（如 GOATFANS），联网检索价值低。"""
    k = (kw or "").strip()
    if not k or re.search(r"[\u4e00-\u9fff]", k):
        return False
    return bool(re.fullmatch(r"[A-Za-z0-9_]{3,20}", k))


def _score_search_keyword(
    kw: str,
    *,
    title: str,
    from_hashtag: bool,
    from_summary: bool,
) -> int:
    score = 0
    if from_hashtag:
        score += 50
    if from_summary:
        score += 35
    if _is_low_value_latin_tag(kw):
        score -= 55
    if re.search(r"(才是|多么|怎么样|看看|打开|复制)", kw) and len(kw) > 5:
        score -= 50
    if kw in _EMOTION_TAGS:
        score -= 60
    if len(kw) >= 3:
        score += 12
    if kw == title:
        score -= 30
    if any(m in kw for m in _WORK_ENTITY_MARKERS):
        score += 28
    if len(kw) <= 2 and kw not in ("牢大", "劳大", "嘎子"):
        score -= 10
    return score


def is_emotion_tag(kw: str) -> bool:
    return (kw or "").strip() in _EMOTION_TAGS


def pick_display_tag(keywords: list[str]) -> str:
    """列表展示用标签：优先作品/梗实体，跳过情绪词。"""
    for kw in keywords:
        k = (kw or "").strip()
        if k and k not in _EMOTION_TAGS and k not in _GENERIC_SEARCH_TAGS:
            return k[:8]
    return (keywords[0][:8] if keywords else "短视频")


def extract_video_search_keywords(
    *,
    title: str = "",
    summary: str = "",
    page_text: str = "",
    cover_ocr: str = "",
    tag: str = "",
    share_keywords: str = "",
) -> list[str]:
    """
    检索词优先级：口令 #话题#（作品/梗）> 推荐语实体 > 标题实体。
    玩梗/抽象视频勿让情绪词（绝望）或标题字面（赴约的鹤）压过剑风传奇/牢大等。
    """
    title = (title or "").strip()
    summary = (summary or "").strip()
    from_paste = _parse_share_keywords(share_keywords)

    candidates: list[tuple[str, bool, bool]] = []
    seen: set[str] = set()

    def add(kw: str, *, from_hashtag: bool = False, from_summary: bool = False) -> None:
        k = (kw or "").strip()
        if not k or k in seen or k in _GENERIC_SEARCH_TAGS or len(k) < 2:
            return
        seen.add(k)
        candidates.append((k, from_hashtag, from_summary))

    for k in from_paste:
        add(k, from_hashtag=True)

    for w in paste_intel.extract_note_entities(summary):
        add(w, from_summary=True)
    for part in re.split(r"[,，、|]", summary):
        w = part.strip()
        if 2 <= len(w) <= 12 and w not in _EMOTION_TAGS:
            add(w, from_summary=True)
    for m in re.finditer(r"(牢大|劳大|剑风传奇|科比|八度空间|周杰伦|[\u4e00-\u9fff]{3,10})", summary):
        w = m.group(1)
        if w not in _EMOTION_TAGS:
            add(w, from_summary=True)

    blob = "\n".join(
        x for x in (title, summary, cover_ocr, page_text[:800]) if (x or "").strip()
    )
    for k in paste_intel.extract_keywords(blob) if blob else []:
        add(k, from_hashtag=True)

    for k in extract_title_entities(title) if title else []:
        add(k)

    tag_kw = (tag or "").strip()
    if tag_kw and tag_kw not in _GENERIC_SEARCH_TAGS:
        add(tag_kw, from_hashtag=tag_kw in from_paste)

    if not candidates and title:
        for k in extract_title_entities(title)[:1]:
            add(k)
        if not candidates:
            add(title[:24])

    ranked = sorted(
        candidates,
        key=lambda x: (
            -_score_search_keyword(x[0], title=title, from_hashtag=x[1], from_summary=x[2]),
            -len(x[0]),
        ),
    )

    pruned = _prune_substring_keywords([k for k, _, _ in ranked])
    pruned_set = set(pruned)
    ordered = [k for k, _, _ in ranked if k in pruned_set]

    has_summary_entity = any(fs for _, _, fs in ranked)
    out: list[str] = []
    for kw in ordered:
        if kw in out:
            continue
        if not out and kw in _EMOTION_TAGS:
            continue
        if _is_low_value_latin_tag(kw) and any(
            not _is_low_value_latin_tag(x) for x, _, _ in ranked
        ):
            continue
        if has_summary_entity and re.search(r"(才是|多么|怎么样|看看|复制|打开)", kw):
            continue
        out.append(kw)
        if len(out) >= 3:
            break

    if not out and ranked:
        out = [ranked[0][0]]
    return out[:3]


# 易歧义词 → 联网/图片检索 query（勿用裸 "Clash"，易命中 VPN 代理客户端）
_SEARCH_QUERY_OVERRIDES: dict[str, str] = {
    "皇室战争": "部落冲突 皇室战争 Supercell 卡牌手游",
    "剑风传奇": "剑风传奇 漫画 三浦建太郎",
    "牢大": "剑风传奇 漫画 网络梗",
    "劳大": "剑风传奇 漫画 网络梗",
    "周杰伦": "周杰伦 歌手 华语流行 音乐人",
    "八度空间": "周杰伦 专辑 八度空间 音乐",
    "原神": "原神 游戏 米哈游 开放世界 RPG",
    "原神深渊": "原神 深境螺旋 深渊 配队攻略",
}

_BAIKE_ALIASES: dict[str, list[str]] = {
    "皇室战争": ["部落冲突：皇室战争", "皇室战争", "Clash Royale"],
    "剑风传奇": ["剑风传奇", "烙印战士", "BERSERK"],
    "牢大": ["科比·布莱恩特", "科比"],
    "劳大": ["科比·布莱恩特", "科比"],
    "周杰伦": ["周杰伦", "周杰倫"],
    "八度空间": ["八度空间", "八度空間"],
    "原神": ["原神", "Genshin Impact"],
    "原神深渊": ["深境螺旋", "原神深渊", "原神"],
}

# Bing 误命中：Clash 代理 / VPN（与手游 Clash Royale 无关）
_PROXY_SERP_BAD = (
    "代理客户端",
    "VMess",
    "Trojan",
    "Shadowsocks",
    "订阅链接",
    "订阅导入",
    "机场",
    "clashbk",
    "Mihomo",
    "TUN 模式",
    "规则分流",
    "Clash官网",
    "Clash 客户端",
    "Clash 使用教程",
    "YAML 配置",
    "GitHub - clash",
)

_KNOWN_TOPIC_BLURBS: dict[str, str] = {
    "皇室战争": (
        "《部落冲突:皇室战争》(Clash Royale) 是 Supercell 出品的卡牌策略手游，"
        "与《部落冲突》同世界观，玩家用卡牌在竞技场对战。"
    ),
    "剑风传奇": (
        "《剑风传奇》是日本暗黑奇幻漫画，又译《烙印战士》(Berserk)，作者三浦建太郎。"
        "讲述主角格斯在残酷中世纪幻想世界里抗争的故事，以硬朗画风和深刻剧情著称。"
        "衍生有 TV 动画、剧场版、游戏《剑风传奇 无双》等；因作者离世，正传未完也是圈内常聊的话题。"
    ),
    "牢大": (
        "「牢大」是年轻人网络上的梗称呼，多指 NBA 传奇球星科比·布莱恩特（Kobe Bryant）。"
        "科比是美国著名篮球运动员，主要效力洛杉矶湖人队，五次 NBA 总冠军、两届总决赛 MVP、"
        "18 次全明星，以坚韧好胜的「曼巴精神」闻名。"
        "近期常和《剑风传奇》等画面做二创混剪；面向长辈解释时语气尊重，勿调侃逝者。"
    ),
    "劳大": (
        "「劳大」与「牢大」同为网上对科比·布莱恩特的梗称（见「牢大」条目）。"
        "科比是 NBA 名人堂级别得分后卫，湖人队传奇，五次总冠军。"
    ),
    "周杰伦": (
        "周杰伦（Jay Chou）是华语流行歌手、音乐人，代表作有《青花瓷》《晴天》《七里香》《稻香》等，"
        "在长辈群体里知名度很高。"
    ),
    "八度空间": (
        "《八度空间》是周杰伦 2002 年发行的录音室专辑，代表曲目有《半岛铁盒》《暗号》《回到过去》《最后的战役》等。"
        "《七里香》是 2004 年同名专辑主打歌，《晴天》出自专辑《叶惠美》——"
        "口播时不要把《七里香》《晴天》说成《八度空间》里的歌。"
    ),
    "原神": (
        "《原神》是米哈游出品的开放世界冒险 RPG，玩家在幻想世界「提瓦特」里探索、打怪、做任务。"
        "游戏里不同角色带火、水、雷、冰等元素属性，战斗常讲究元素反应和队伍搭配。"
    ),
    "原神深渊": (
        "「深渊」在《原神》里一般指「深境螺旋」高难度闯关：要组满两队角色轮流上场，"
        "讲究元素搭配、谁打输出谁辅助，玩家口语里常说「配队」。"
        "每轮刷新过关能拿原石、培养材料，所以孩子会跟着攻略视频练手法和阵容。"
    ),
}

_WEB_SERP_GOOD = (
    "部落冲突",
    "Clash Royale",
    "Supercell",
    "手游",
    "游戏",
    "塔防",
    "卡牌",
    "策略",
)

_BING_IMAGE_GOOD = (
    "游戏",
    "clash",
    "royale",
    "部落",
    "手游",
    "supercell",
    "截图",
    "game",
    "screenshot",
    "猫",
    "狗",
    "宠物",
    "pet",
    "cat",
    "dog",
    "kitten",
    "puppy",
    "萌宠",
    "猫咪",
    "小狗",
)

_BING_IMAGE_BAD = (
    "王室",
    "皇室成员",
    "君主",
    "royal family",
    "埃及",
    "artifact",
    "canopic",
    "宫内",
    "国王",
    "天皇",
    "卢森堡",
    "宗室",
    "皇族",
    "王储",
)

_WEB_SERP_BAD = _BING_IMAGE_BAD + _PROXY_SERP_BAD + (
    "全球九大",
    "君主制",
    "宮内",
    "王储",
    "爱新觉罗",
    "皇室是以",
    "royal family",
    "君主为核心",
    "天皇皇后",
)


def _score_web_serp_item(title: str, snippet: str, keyword: str) -> int:
    blob = f"{title} {snippet}"
    score = 0
    for g in _WEB_SERP_GOOD:
        if g.lower() in blob.lower() or g in blob:
            score += 12
    for b in _WEB_SERP_BAD:
        if b.lower() in blob.lower() or b in blob:
            score -= 22
    if keyword and keyword in blob:
        score += 3
    if keyword == "周杰伦":
        for g in ("歌手", "音乐", "专辑", "Jay", "华语"):
            if g in blob or g.lower() in blob.lower():
                score += 15
        for b in ("周朝", "甲骨文", "汉字", "周易", "西周"):
            if b in blob:
                score -= 40
    if keyword == "八度空间":
        for g in ("周杰伦", "专辑", "半岛铁盒", "暗号", "音乐"):
            if g in blob:
                score += 15
        for b in ("汉字", "拼音", "八家户", "说文"):
            if b in blob:
                score -= 40
    if "皇室" in blob and not any(x in blob for x in ("部落", "Royale", "游戏", "手游", "Supercell", "皇室战争")):
        score -= 30
    low = blob.lower()
    if "clash" in low and "royale" not in low and "皇室战争" not in blob:
        if any(p.lower() in low or p in blob for p in _PROXY_SERP_BAD):
            score -= 60
    return score


def _web_blurb_relevant(keyword: str, text: str) -> bool:
    """检索摘要是否与关键词主题一致（防「皇室战争」→ Clash 代理、「剑风传奇」→ 古剑）。"""
    if not (text or "").strip():
        return False
    kw = keyword.strip()
    if kw == "剑风传奇":
        good = (
            "剑风传奇",
            "烙印战士",
            "Berserk",
            "三浦",
            "格斯",
            "漫画",
            "动漫",
            "奇幻",
        )
        bad = (
            "古名剑",
            "龙泉宝剑",
            "十大名剑",
            "越王",
            "欧冶子",
            "湛卢",
            "百兵之君",
            "刀剑网",
        )
        if any(g in text for g in good):
            return True
        if any(b in text for b in bad):
            return False
        return False
    if kw in ("牢大", "劳大"):
        good = ("科比", "Kobe", "NBA", "湖人", "篮球", "曼巴", "布莱恩特")
        if any(g in text for g in good):
            return True
        if "古名剑" in text or "龙泉宝剑" in text:
            return False
        return False
    if kw == "周杰伦":
        good = ("周杰伦", "Jay", "歌手", "音乐", "专辑", "华语", "流行", "艺人", "创作", "演唱")
        bad = ("周朝", "甲骨文", "汉字", "周易", "周武王", "西周", "东周", "字本义", "简体字", "仓颉")
        if any(g in text for g in good):
            return True
        if any(b in text for b in bad):
            return False
        return False
    if kw == "八度空间":
        good = ("周杰伦", "专辑", "音乐", "唱片", "歌曲", "半岛铁盒", "暗号", "回到过去", "录音室")
        bad = ("汉字", "拼音", "八家户", "管委会", "书法", "说文", "字拼音", "独体字", "仓颉")
        if any(g in text for g in good):
            return True
        if any(b in text for b in bad):
            return False
        return False
    if _is_low_value_latin_tag(kw):
        return False
    if kw != "皇室战争":
        return True
    game_signals = ("皇室战争", "部落冲突", "Supercell", "Clash Royale", "卡牌", "手游", "塔防")
    result_lines = [ln for ln in text.splitlines() if re.match(r"^\d+\.\s", ln.strip())]
    check_lines = result_lines if result_lines else text.splitlines()
    game_hits = sum(1 for ln in check_lines if any(g in ln for g in game_signals))
    proxy_hits = sum(1 for ln in check_lines if any(p in ln for p in _PROXY_SERP_BAD))
    if proxy_hits > 0 and game_hits == 0:
        return False
    if game_hits > 0:
        return True
    return any(g in text for g in game_signals) and proxy_hits == 0


def _filter_web_serp_results(
    titles: list[str],
    snippets: list[str],
    *,
    keyword: str,
    max_items: int = 6,
) -> tuple[list[str], list[str]]:
    """去掉 Bing/DDG 中「王室/皇室成员」等偏题条目。"""
    scored: list[tuple[int, str, str]] = []
    n = max(len(titles), len(snippets))
    for i in range(n):
        t = titles[i] if i < len(titles) else ""
        s = snippets[i] if i < len(snippets) else ""
        if not t and not s:
            continue
        sc = _score_web_serp_item(t, s, keyword)
        if sc >= 0:
            scored.append((sc, t, s))
    scored.sort(key=lambda x: -x[0])
    picked = scored[:max_items]
    if not picked and scored:
        picked = scored[:1]
    return [p[1] for p in picked], [p[2] for p in picked]


def _baike_looks_relevant(text: str, keyword: str) -> bool:
    if not text:
        return False
    if any(g in text for g in ("游戏", "手游", "Supercell", "部落冲突", "Clash")):
        return True
    if any(b in text for b in ("君主", "王朝", "天皇", "王室", "皇族")):
        return False
    return len(text) > 40


def _disambiguate_web_query(keyword: str) -> str:
    kw = keyword.strip()
    if not kw:
        return kw
    if kw in _SEARCH_QUERY_OVERRIDES:
        return _SEARCH_QUERY_OVERRIDES[kw]
    if "最新" in kw or "新闻" in kw or "热点" in kw:
        return kw
    return f"{kw} 简介"


def _is_pet_keyword(keyword: str) -> bool:
    kw = (keyword or "").strip()
    return any(x in kw for x in ("猫", "狗", "宠", "pet", "哈基米", "萌宠"))


def _bing_image_search_query(keyword: str) -> str:
    kw = keyword.strip()
    if not kw:
        return kw
    if kw in _IMAGE_QUERY_OVERRIDES:
        return _IMAGE_QUERY_OVERRIDES[kw]
    if _is_pet_keyword(kw):
        return "可爱猫咪 宠物 高清"
    if "战争" in kw and len(kw) <= 8:
        return f"{kw} 手机游戏 截图"
    return f"{kw} 高清"


def _bing_image_search_queries(keyword: str) -> list[str]:
    """同一关键词多组查询，提高 Bing 封面命中率。"""
    kw = (keyword or "").strip()
    if not kw:
        return []
    primary = _bing_image_search_query(kw)
    queries = [primary]
    if _is_pet_keyword(kw):
        queries.extend(["小猫 萌宠 照片", f"{kw} 猫咪", "可爱宠物猫"])
    elif any(x in kw for x in ("游戏", "手游")):
        queries.append(f"{kw} 游戏截图")
    elif len(kw) > 10:
        short = kw[:8].strip()
        if short and short != kw:
            queries.append(f"{short} 高清")
    seen: set[str] = set()
    out: list[str] = []
    for q in queries:
        q = q.strip()
        if q and q not in seen:
            seen.add(q)
            out.append(q)
    return out


def _score_bing_image_candidate(alt: str, keyword: str) -> int:
    text = (alt or "").lower()
    score = 0
    for g in _BING_IMAGE_GOOD:
        if g.lower() in text or g in (alt or ""):
            score += 8
    for b in _BING_IMAGE_BAD:
        if b.lower() in text or b in (alt or ""):
            score -= 18
    if "皇室" in (alt or "") and not any(
        x in (alt or "") for x in ("部落", "Royale", "游戏", "手游", "Supercell", "皇室战争")
    ):
        score -= 30
    low = (alt or "").lower()
    if "clash" in low and "royale" not in low and "皇室战争" not in (alt or ""):
        score -= 20
    return score


def _parse_bing_image_candidates(html: str, *, max_items: int = 16) -> list[tuple[str, str]]:
    """从 Bing 图片页解析 (murl, alt/title)。"""
    items: list[tuple[str, str]] = []
    seen: set[str] = set()

    for block in re.finditer(r'class="iusc"[\s\S]{0,4000}?m="([^"]+)"', html, re.I):
        raw = block.group(1)
        murl_m = re.search(r'murl&quot;:&quot;([^&]+?)&quot;', raw) or re.search(
            r'"murl"\s*:\s*"([^"]+)"', html_module.unescape(raw.replace("&quot;", '"'))
        )
        if not murl_m:
            continue
        murl = html_module.unescape(murl_m.group(1)).strip()
        if not murl.startswith(("http://", "https://")) or murl in seen:
            continue
        alt_m = re.search(r'&quot;t&quot;:&quot;([^&]*?)&quot;', raw) or re.search(
            r'&quot;desc&quot;:&quot;([^&]*?)&quot;', raw
        )
        alt = html_module.unescape(alt_m.group(1)).strip() if alt_m else ""
        seen.add(murl)
        items.append((murl, alt))
        if len(items) >= max_items:
            return items

    if not items:
        for u in _extract_bing_image_murls(html, max_items=max_items):
            if u not in seen:
                items.append((u, ""))
    return items


_BING_MURL_PATTERNS = (
    re.compile(r'murl&quot;:&quot;(https?://[^&]+?)&quot;', re.I),
    re.compile(r'"murl"\s*:\s*"(https?://[^"\\]+)"', re.I),
    re.compile(r"'murl'\s*:\s*'(https?://[^']+)'", re.I),
)


def _extract_bing_image_murls(html: str, *, max_items: int = 8) -> list[str]:
    urls: list[str] = []
    for pat in _BING_MURL_PATTERNS:
        for m in pat.finditer(html):
            u = html_module.unescape(m.group(1)).strip()
            if not u.startswith(("http://", "https://")):
                continue
            low = u.lower()
            if "bing.com/th" in low and "id=" in low:
                continue
            if u not in urls:
                urls.append(u)
            if len(urls) >= max_items:
                return urls
    return urls


@dataclass
class BingImageResult:
    url: str = ""
    keyword: str = ""
    query_used: str = ""
    best_score: int = 0
    candidate_count: int = 0
    skip_reason: str = ""
    detail: str = ""


def _bing_image_accept_score(score: int, alt: str, keyword: str) -> bool:
    """游戏/宠物类放宽阈值，避免「有图但 alt 不含关键词」被误杀。"""
    alt_l = (alt or "").lower()
    if score >= 8:
        return True
    if any(x in alt_l or x in (alt or "") for x in ("clash", "royale", "部落", "游戏", "手游", "supercell")):
        return score >= 0
    if _is_pet_keyword(keyword) or any(
        x in alt_l or x in (alt or "") for x in ("猫", "狗", "宠", "pet", "cat", "dog", "kitten")
    ):
        return score >= 0
    return score >= 4


async def _bing_image_search_one_query(
    search_q: str,
    keyword: str,
    *,
    timeout: float = 12.0,
) -> BingImageResult:
    kw = keyword.strip()
    text = ""
    status = 0
    try:
        async with httpx.AsyncClient(timeout=timeout, follow_redirects=True) as client:
            r = await client.get(
                "https://cn.bing.com/images/search",
                params={"q": search_q, "first": "1", "form": "HDRSC2"},
                headers={
                    "User-Agent": _UA,
                    "Accept-Language": "zh-CN,zh;q=0.9",
                    "Referer": "https://cn.bing.com/",
                },
            )
            status = r.status_code
            text = r.text[:600_000]
    except Exception as e:
        _log.info(
            "web_lookup: engine=bing_images query=%s status=error len=0 err=%s",
            search_q,
            e,
        )
        return BingImageResult(keyword=kw, query_used=search_q, skip_reason=f"请求异常：{e}")

    _log.info(
        "web_lookup: engine=bing_images query=%s status=%s len=%s",
        search_q,
        status,
        len(text),
    )
    if status >= 400 or _is_blocked_search_html(text):
        return BingImageResult(
            keyword=kw,
            query_used=search_q,
            skip_reason="Bing 返回拦截页或 HTTP 错误",
        )

    candidates = _parse_bing_image_candidates(text)
    if not candidates:
        return BingImageResult(
            keyword=kw,
            query_used=search_q,
            skip_reason="未解析到图片候选",
        )

    scored = [(u, alt, _score_bing_image_candidate(alt, kw)) for u, alt in candidates]
    scored.sort(key=lambda x: (-x[2], -len(x[1])))
    best_url, best_alt, best_score = scored[0]
    _log.info(
        "web_lookup: engine=bing_images query=%s picked score=%s alt=%s url=%s",
        search_q,
        best_score,
        (best_alt or "")[:80],
        best_url[:120],
    )
    if not _bing_image_accept_score(best_score, best_alt, kw):
        return BingImageResult(
            keyword=kw,
            query_used=search_q,
            best_score=best_score,
            candidate_count=len(candidates),
            skip_reason=f"最佳候选相关性偏低 score={best_score}",
        )
    return BingImageResult(
        url=best_url,
        keyword=kw,
        query_used=search_q,
        best_score=best_score,
        candidate_count=len(candidates),
        detail=f"Bing 查询「{search_q}」score={best_score}",
    )


async def bing_image_search(keyword: str, *, timeout: float = 12.0) -> BingImageResult:
    """Bing 图片搜索（多查询重试），返回诊断信息。"""
    kw = keyword.strip()
    if not kw:
        return BingImageResult(skip_reason="关键词为空")

    queries = _bing_image_search_queries(kw)
    last = BingImageResult(keyword=kw, skip_reason="所有 Bing 查询均未命中")
    for search_q in queries:
        result = await _bing_image_search_one_query(search_q, kw, timeout=timeout)
        if result.url:
            return result
        last = result
    return last


async def bing_image_search_murl(keyword: str, *, timeout: float = 12.0) -> str:
    """Bing 图片搜索：按 alt 相关性选图（免费封面，不耗 vivo 文生图额度）。"""
    return (await bing_image_search(keyword, timeout=timeout)).url


async def google_news_rss_snippets(query: str, *, timeout: float = 12.0, max_items: int = 4) -> str:
    """Google News RSS：近几天中文资讯标题+摘要（无需 API Key）。"""
    q = query.strip()
    if not q or not settings.web_search_enabled:
        return ""
    try:
        async with httpx.AsyncClient(timeout=timeout, follow_redirects=True) as client:
            r = await client.get(
                "https://news.google.com/rss/search",
                params={
                    "q": q,
                    "hl": "zh-CN",
                    "gl": "CN",
                    "ceid": "CN:zh-Hans",
                },
                headers={"User-Agent": _UA},
            )
            r.raise_for_status()
            root = ET.fromstring(r.content)
    except Exception as e:
        _log.debug("WbWeb news rss skip q=%s: %s", q[:40], e)
        return ""

    lines: list[str] = []
    for item in root.findall(".//item")[:max_items]:
        title_el = item.find("title")
        pub = item.find("pubDate")
        desc = item.find("description")
        t = (title_el.text or "").strip() if title_el is not None else ""
        p = (pub.text or "").strip() if pub is not None else ""
        d = _strip_html(desc.text if desc is not None and desc.text else "")
        if not t:
            continue
        chunk = f"· {t}"
        if p:
            chunk += f"（{p[:28]}）"
        if d and d != t:
            chunk += f"：{d[:160]}"
        lines.append(chunk)
    if not lines:
        return ""
    return "近期资讯摘录：\n" + "\n".join(lines)


async def baidu_baike_summary(keyword: str, *, timeout: float = 8.0) -> str:
    """百度百科摘要（国内网络通常可访问，作 SERP 的补充；非网页搜索）。"""
    kw = keyword.strip()
    if not kw or not settings.web_search_enabled:
        return ""
    url = f"https://baike.baidu.com/item/{quote_plus(kw)}"
    try:
        async with httpx.AsyncClient(timeout=timeout, follow_redirects=True) as client:
            r = await client.get(url, headers={"User-Agent": _UA, "Accept-Language": "zh-CN"})
            if r.status_code >= 400:
                return ""
            html = r.text[:200_000]
    except Exception:
        return ""

    m = re.search(r'<meta\s+name="description"\s+content="([^"]+)"', html, re.I)
    if m:
        desc = m.group(1).strip()
        if len(desc) > 30:
            return f"（百度百科：{kw}）\n{desc[:1200]}"
    m2 = re.search(r'class="lemmaSummary[^"]*"[^>]*>(.+?)</div>', html, re.S)
    if m2:
        text = _strip_html(m2.group(1))
        text = re.sub(r"\s+", " ", text).strip()
        if len(text) > 30:
            return f"（百度百科：{kw}）\n{text[:1200]}"
    return ""


def builtin_entity_blurb(keyword: str) -> str:
    k = (keyword or "").strip()
    if not k:
        return ""
    if k in _KNOWN_TOPIC_BLURBS:
        return _KNOWN_TOPIC_BLURBS[k]
    return ""


def build_builtin_entity_context(*, share_keywords: str = "", summary: str = "", title: str = "") -> str:
    """为 analyze 注入可靠背景常识（联网偏题时口播仍可深入展开）。"""
    kws = extract_video_search_keywords(
        title=title,
        summary=summary,
        share_keywords=share_keywords,
    )
    blob = f"{share_keywords} {summary}"
    for alias in ("牢大", "劳大", "科比"):
        if alias in blob and alias not in kws:
            kws.append(alias)

    parts: list[str] = []
    seen: set[str] = set()
    for kw in kws:
        if kw in seen:
            continue
        blurb = builtin_entity_blurb(kw)
        if blurb:
            seen.add(kw)
            parts.append(f"· 【{kw}】{blurb}")
    return "\n".join(parts)


async def lookup_fresh_blurb(keyword: str) -> tuple[str, str]:
    """返回 (摘要文本, 状态说明)。歧义词优先百科，再 DDG/Bing（带 SERP 过滤）。"""
    kw = keyword.strip()
    if not kw:
        return "", "空关键词"

    if kw in _KNOWN_TOPIC_BLURBS:
        for alias in _BAIKE_ALIASES.get(kw, [kw]):
            baike = await baidu_baike_summary(alias, timeout=8.0)
            if baike and len(baike) > 40 and _web_blurb_relevant(kw, baike):
                return baike, f"百度百科有结果({alias})"

    if kw in _SEARCH_QUERY_OVERRIDES or kw in _IMAGE_QUERY_OVERRIDES:
        for alias in _BAIKE_ALIASES.get(kw, [kw]):
            baike = await baidu_baike_summary(alias, timeout=8.0)
            if baike and len(baike) > 40 and _baike_looks_relevant(baike, kw):
                return baike, f"百度百科有结果({alias})"
        wiki = await wikipedia_zh_best_effort("Clash Royale", timeout=8.0)
        if wiki and len(wiki) > 50:
            return wiki, "中文维基(Clash Royale)有结果"

    ddg = await duckduckgo_html_search_snippets(kw, timeout=12.0)
    if ddg and len(ddg) > 40 and _web_blurb_relevant(kw, ddg):
        return ddg, "DuckDuckGo HTML 有结果"
    if ddg and len(ddg) > 40:
        _log.info("web_lookup: ddg rejected off-topic kw=%s", kw)

    bing = await bing_web_search_snippets(kw, timeout=12.0)
    if bing and len(bing) > 40 and _web_blurb_relevant(kw, bing):
        return bing, "Bing 有结果"
    if bing and len(bing) > 40:
        _log.info("web_lookup: bing rejected off-topic kw=%s (e.g. Clash proxy)", kw)

    wiki = await wikipedia_zh_best_effort(kw, timeout=10.0)
    if wiki and len(wiki) > 50:
        return wiki, "中文维基有结果"

    baike = await baidu_baike_summary(kw, timeout=8.0)
    if baike and len(baike) > 40:
        return baike, "百度百科有结果"

    year = str(datetime.now().year)
    for q in (f"{kw} 最新", f"{kw} {year} 热点"):
        blob = await duckduckgo_instant_summary(q, timeout=6.0)
        if blob and len(blob) > 50:
            return f"（检索：{q}）\n{blob}", f"DDG Instant 有结果({q})"

    news = await google_news_rss_snippets(kw, timeout=5.0)
    if news:
        return news, "Google News RSS 有结果"

    fallback = await lookup_keyword_blurb(kw)
    if fallback and _web_blurb_relevant(kw, fallback):
        return fallback, "兜底检索有结果"

    known = builtin_entity_blurb(kw)
    if known:
        return known, "内置常识摘要(检索偏题已丢弃)"
    return "", "DDG HTML/Bing/维基/百科均无命中"


async def build_fresh_video_web_context(
    *,
    title: str = "",
    summary: str = "",
    page_text: str = "",
    cover_ocr: str = "",
    tag: str = "",
    share_keywords: str = "",
    stored_web_context: str = "",
) -> FreshWebResult:
    """
    通俗视频 analyze 前调用：按当前标题/标签刷新联网材料（与分享入库时的 web_context 合并）。
    """
    stored = (stored_web_context or "").strip()
    result = FreshWebResult(stored_len=len(stored))

    if not settings.web_search_enabled:
        result.enabled = False
        result.text = stored[:4500]
        result.empty_reason = "WEB_SEARCH_ENABLED=false"
        return result

    keywords = extract_video_search_keywords(
        title=title,
        summary=summary,
        page_text=page_text,
        cover_ocr=cover_ocr,
        tag=tag,
        share_keywords=share_keywords,
    )
    if not keywords and title.strip():
        keywords = extract_title_entities(title.strip())[:1]
    result.keywords_tried = keywords[:3]

    if not keywords:
        result.text = stored[:4500]
        result.empty_reason = "未能从标题/摘要提取有效检索词（标签过泛或标题过短）"
        return result

    fresh_parts: list[str] = []
    for kw in keywords[:3]:
        blurb, status = await lookup_fresh_blurb(kw)
        result.attempt_log.append(f"「{kw}」→ {status}")
        if not blurb:
            extra = _NICK_EXTRA_QUERIES.get(kw)
            if extra:
                blurb, status2 = await lookup_fresh_blurb(extra)
                result.attempt_log.append(f"「{extra}」(昵称映射) → {status2}")
        if blurb:
            fresh_parts.append(f"【关键词：{kw}】\n{blurb}")
        if len("\n\n".join(fresh_parts)) > 3200:
            break

    fresh_block = "\n\n".join(fresh_parts).strip()
    result.fresh_len = len(fresh_block)

    if fresh_block and stored:
        merged = (
            f"=== 生成时刷新（DuckDuckGo HTML / Bing 等，优先采用）===\n{fresh_block}\n\n"
            f"=== 分享入库摘要（补充）===\n{stored}"
        )
    elif fresh_block:
        merged = fresh_block
    else:
        merged = stored
        result.empty_reason = (
            "本次生成时检索无命中（见 attempt_log）；"
            "常见原因：外网搜索不可达、HTML 解析无条目、或关键词仍过泛；"
            "analyze 将允许基于标题实体做常识性展开"
        )

    result.text = merged[:4500]
    result.merged_len = len(result.text)

    if fresh_block:
        _log.info(
            "WbVideoGen fresh web ok keywords=%s fresh_len=%s merged_len=%s",
            keywords[:3],
            result.fresh_len,
            result.merged_len,
        )
    else:
        _log.info(
            "WbVideoGen fresh web empty keywords=%s use_stored=%s reason=%s",
            keywords[:3],
            bool(stored),
            result.empty_reason,
        )

    return result


def plan_deep_search_queries(
    *,
    title: str = "",
    summary: str = "",
    share_keywords: str = "",
    tag: str = "",
    cover_ocr: str = "",
    page_text: str = "",
) -> list[str]:
    """
    从分享材料自动规划检索词（换话题通用，不依赖 _KNOWN_TOPIC_BLURBS）。
    歧义词仍走 _SEARCH_QUERY_OVERRIDES 消歧（仅改 query，不预写口播）。
    """
    keywords = extract_video_search_keywords(
        title=title,
        summary=summary,
        share_keywords=share_keywords,
        tag=tag,
        cover_ocr=cover_ocr,
        page_text=page_text,
    )
    title_s = (title or "").strip()[:24]
    summary_s = (summary or "").strip()[:20]
    blob = f"{title} {summary} {share_keywords} {tag}"

    queries: list[str] = []
    seen: set[str] = set()

    def add(q: str) -> None:
        q = (q or "").strip()
        if not q or q in seen or len(q) < 2:
            return
        seen.add(q)
        queries.append(q)

    for kw in keywords[:2]:
        add(_disambiguate_web_query(kw))
        if title_s and title_s not in kw and len(title_s) >= 2:
            add(f"{kw} {title_s}")
        if any(x in blob for x in ("教学", "攻略", "实战", "配队", "教程", "玩法")):
            add(f"{kw} 玩法攻略 介绍")

    if summary_s and summary_s not in _GENERIC_SEARCH_TAGS:
        add(_disambiguate_web_query(summary_s))
    if title_s and not queries:
        add(_disambiguate_web_query(title_s))

    return queries[:4]


async def fetch_fresh_context_for_topic(
    *,
    title: str = "",
    summary: str = "",
    share_keywords: str = "",
    tag: str = "",
    cover_ocr: str = "",
    page_text: str = "",
    max_queries: int = 3,
    include_builtin_fallback: bool = True,
) -> str:
    """
    通用联网加深：Bing/DDG/百科（与 tools 同源），供口播/解读优先采用。
    本地 _KNOWN_TOPIC_BLURBS 仅在检索结果过短时作补充，不替代联网。
    """
    from app.config import settings
    from app.services.vivo_chat_tools import run_web_search_tool

    if not settings.web_search_enabled:
        if include_builtin_fallback:
            return build_builtin_entity_context(
                share_keywords=share_keywords,
                summary=summary,
                title=title,
            )
        return ""

    queries = plan_deep_search_queries(
        title=title,
        summary=summary,
        share_keywords=share_keywords,
        tag=tag,
        cover_ocr=cover_ocr,
        page_text=page_text,
    )
    chunks: list[str] = []
    for q in queries[: max(1, max_queries)]:
        block = await run_web_search_tool(q)
        if block and "未检索到" not in block:
            chunks.append(block)

    merged = "\n\n".join(chunks).strip()
    if len(merged) < 200 and include_builtin_fallback:
        builtin = build_builtin_entity_context(
            share_keywords=share_keywords,
            summary=summary,
            title=title,
        )
        if builtin:
            merged = (
                f"{merged}\n\n【背景参考（联网不足时补充，勿优先于上方检索）】\n{builtin}"
                if merged
                else builtin
            ).strip()
    return merged[:3200]


_JUNK_WEB_MARKERS = (
    "原字的本义",
    "拼音是",
    "BTS",
    "腾讯视频",
    "拉丁字母",
    "笔画",
    "仓颉",
    "Kim Tae",
    "罗马数字",
)


def pick_oral_sentences_from_web(
    text: str,
    *,
    core: str = "",
    max_sentences: int = 3,
) -> list[str]:
    """从联网摘要抽取可口语化的句子（兜底口播用，不依赖本地词条）。"""
    if not text:
        return []
    cleaned = re.sub(r"【搜索词：[^】]+】", "", text)
    cleaned = re.sub(r"（Bing：[^）]+）", "", cleaned)
    candidates: list[tuple[int, str]] = []
    for line in re.split(r"[。\n；;]", cleaned):
        line = re.sub(r"^\d+[.．、\s—-]+", "", line).strip()
        if len(line) < 15 or len(line) > 160:
            continue
        if any(j in line for j in _JUNK_WEB_MARKERS):
            continue
        score = 0
        if core and core in line:
            score += 6
        if any(
            w in line
            for w in ("游戏", "角色", "配队", "玩法", "漫画", "动画", "歌手", "专辑", "攻略", "技能")
        ):
            score += 4
        if "《" in line or "」" in line:
            score += 2
        candidates.append((score, line))
    candidates.sort(key=lambda x: -x[0])
    out: list[str] = []
    seen: set[str] = set()
    for _, line in candidates:
        key = line[:40]
        if key in seen:
            continue
        seen.add(key)
        out.append(line.rstrip("。") + "。")
        if len(out) >= max_sentences:
            break
    return out
