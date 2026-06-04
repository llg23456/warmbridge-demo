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
    out: list[str] = []
    for kw in kept:
        if long_kws and len(kw) <= 2:
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


def extract_video_search_keywords(
    *,
    title: str = "",
    summary: str = "",
    page_text: str = "",
    cover_ocr: str = "",
    tag: str = "",
    share_keywords: str = "",
) -> list[str]:
    """检索词：口令 #话题# + 标题实体 + 推荐语；去掉子串碎片（如「皇室」「战争」）。"""
    title = (title or "").strip()
    from_paste = _parse_share_keywords(share_keywords)
    entity_kws = extract_title_entities(title) if title else []

    blob = "\n".join(
        x for x in (title, summary, cover_ocr, page_text[:800]) if (x or "").strip()
    )
    paste_kws = paste_intel.extract_keywords(blob) if blob else []

    tag_kw = (tag or "").strip()
    if tag_kw in _GENERIC_SEARCH_TAGS:
        tag_kw = ""

    merged = paste_intel._dedupe_keep_order(from_paste + paste_kws + entity_kws)
    if tag_kw and tag_kw not in merged:
        merged.append(tag_kw)

    filtered = _filter_search_keywords(merged)
    if not filtered and title:
        fallback = _normalize_title_for_search(title)
        for suf in _TITLE_SUFFIX_STRIP:
            if fallback.endswith(suf) and len(fallback) > len(suf) + 1:
                fallback = fallback[: -len(suf)]
        filtered = _filter_search_keywords([fallback] if fallback else [title[:24]])

    pruned = _prune_substring_keywords(filtered)
    if not pruned:
        return []

    out = [pruned[0]]
    for kw in pruned[1:]:
        if kw in out:
            continue
        if out[0] in kw or kw in out[0]:
            continue
        out.append(kw)
        break
    return out[:2]


# 易歧义词 → 联网/图片检索 query（勿用裸 "Clash"，易命中 VPN 代理客户端）
_SEARCH_QUERY_OVERRIDES: dict[str, str] = {
    "皇室战争": "部落冲突 皇室战争 Supercell 卡牌手游",
}

_IMAGE_QUERY_OVERRIDES: dict[str, str] = {
    "皇室战争": "部落冲突 皇室战争 游戏截图 Supercell",
}

_BAIKE_ALIASES: dict[str, list[str]] = {
    "皇室战争": ["部落冲突：皇室战争", "皇室战争", "Clash Royale"],
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
        "（常识）《部落冲突:皇室战争》(Clash Royale) 是 Supercell 出品的卡牌策略手游，"
        "与《部落冲突》同世界观，玩家用卡牌在竞技场对战。"
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
    if "皇室" in blob and not any(x in blob for x in ("部落", "Royale", "游戏", "手游", "Supercell", "皇室战争")):
        score -= 30
    low = blob.lower()
    if "clash" in low and "royale" not in low and "皇室战争" not in blob:
        if any(p.lower() in low or p in blob for p in _PROXY_SERP_BAD):
            score -= 60
    return score


def _web_blurb_relevant(keyword: str, text: str) -> bool:
    """检索摘要是否与关键词主题一致（防「皇室战争」→ Clash 代理）。"""
    if not (text or "").strip():
        return False
    kw = keyword.strip()
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


def _bing_image_search_query(keyword: str) -> str:
    kw = keyword.strip()
    if not kw:
        return kw
    if kw in _IMAGE_QUERY_OVERRIDES:
        return _IMAGE_QUERY_OVERRIDES[kw]
    if "战争" in kw and len(kw) <= 8:
        return f"{kw} 手机游戏 截图"
    return f"{kw} 高清"


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


async def bing_image_search_murl(keyword: str, *, timeout: float = 12.0) -> str:
    """Bing 图片搜索：按 alt 相关性选图（免费封面，不耗 vivo 文生图额度）。"""
    kw = keyword.strip()
    if not kw:
        return ""
    search_q = _bing_image_search_query(kw)
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
        return ""

    _log.info(
        "web_lookup: engine=bing_images query=%s status=%s len=%s",
        search_q,
        status,
        len(text),
    )
    if status >= 400 or _is_blocked_search_html(text):
        _log.info("web_lookup: engine=bing_images query=%s extract_len=0", search_q)
        return ""

    candidates = _parse_bing_image_candidates(text)
    if not candidates:
        _log.info("web_lookup: engine=bing_images query=%s murl_count=0", search_q)
        return ""

    scored = [(u, alt, _score_bing_image_candidate(alt, kw)) for u, alt in candidates]
    scored.sort(key=lambda x: (-x[2], -len(x[1])))
    best_url, best_alt, best_score = scored[0]
    has_game_signal = any(
        x in (best_alt or "").lower() or x in (best_alt or "")
        for x in ("clash", "royale", "部落", "游戏", "手游", "supercell")
    )
    _log.info(
        "web_lookup: engine=bing_images query=%s picked score=%s alt=%s url=%s",
        search_q,
        best_score,
        (best_alt or "")[:80],
        best_url[:120],
    )
    if best_score < 8 and not has_game_signal:
        _log.info(
            "web_lookup: engine=bing_images query=%s skip weak cover score=%s (use placeholder)",
            search_q,
            best_score,
        )
        return ""
    return best_url


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


async def lookup_fresh_blurb(keyword: str) -> tuple[str, str]:
    """返回 (摘要文本, 状态说明)。歧义词优先百科，再 DDG/Bing（带 SERP 过滤）。"""
    kw = keyword.strip()
    if not kw:
        return "", "空关键词"

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

    known = _KNOWN_TOPIC_BLURBS.get(kw, "").strip()
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
    result.keywords_tried = keywords[:2]

    if not keywords:
        result.text = stored[:4500]
        result.empty_reason = "未能从标题/摘要提取有效检索词（标签过泛或标题过短）"
        return result

    fresh_parts: list[str] = []
    for kw in keywords[:2]:
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
