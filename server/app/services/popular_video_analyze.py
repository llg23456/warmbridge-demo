"""通俗视频 D1：一次 LLM 分析 → 核心词 + 深度口播 + D2/D3 预留提示词（不回写 explain 缓存）。"""

from __future__ import annotations

import json
import re
import uuid
from dataclasses import dataclass, field
from typing import Any

import httpx

from app.config import settings
from app.services.vivo_llm import apply_thinking_params

_DISCLAIMER_TAIL = "具体细节还请去看原视频，咱们一起点开慢慢看。"

ANALYZE_SYSTEM = """你是「暖桥」家庭助手的视频口播策划，读者是 50–70 岁长辈。

任务：根据【材料】为「讲解视频」生成结构化 JSON（仅用于 TTS 旁白与后续画面生成，不是详情页文字摘要）。

只输出 JSON，不要 Markdown 代码块。键必须为：
{
  "core_keyword": "一个最有信息量的核心实体（学校/人物/梗/技术等，2～12字）",
  "narration": "中文口播稿",
  "video_prompt": "English prompt for 5s cinematic video intro (D3 reserved)",
  "image_prompts": ["English prompt 1", "English prompt 2", "English prompt 3"]
}

【叙述视角 — 硬性】
- 你是**旁白**，说给**长辈（听众）**听，全程用第二人称 **「您」**。
- 孩子是「孩子 / 咱家孩子」；分享行为是 **「推送给您 / 分享给您 / 发给您」**。
- **禁止**第一人称收分享：不许「推给我 / 给我推 / 孩子给我 / 分享给我 / 发给我 / 告诉我」。
- 示例：✅「孩子给您推了这条视频」 ❌「孩子给我推了个视频」。

【口播 narration 硬性要求】
1) 纯中文口语，温柔耐心，**像 2024–2026 年家人在微信里讲件事**：短句、有温度；少用「据悉」「近年来」「随着…的发展」「众所周知」等老播音/公文腔。
2) **务必讲透、有信息量**：面向长辈要把「是什么、为什么火」说清楚，**禁止**空泛敷衍（如「不用纠结」「网上梗看看就行」「不懂也没关系」就带过）。
   - **单标签视频**：约 **55%** 展开 core_keyword 的背景、特点、衍生（游戏/动漫/作品/人物公开经历等）。
   - **多标签玩梗视频**（如同时有「剑风传奇」「牢大」）：约 **40%** 讲主作品（《剑风传奇》是哪类动漫、讲什么、有哪些动画游戏衍生），约 **30%** 讲关联梗人物（「牢大」即科比·布莱恩特，NBA 传奇、主要荣誉与「曼巴精神」，语气尊重），约 **15%** 这条视频在玩什么梗，约 **15%** 和孩子分享的关联。
   - **优先采用【背景常识】与可靠检索**；检索偏题时仍以【背景常识】展开；可用「一般来说」「大家常把它当作…」；**禁止**编造具体日期、采访原话或未证实事件。
   - **音乐/专辑事实**：须以【背景常识】为准；**禁止**把《七里香》《晴天》说成《八度空间》专辑曲目（七里香为同名专辑主打歌，晴天出自《叶惠美》）。无可靠曲目信息时只介绍歌手/专辑概况，**勿乱点歌名**。
3) narration **最后一句**必须含「原视频」并引导点开看。
4) narration **全段约 240～320 字**（含最后引导句）；多实体玩梗视频**勿短于 220 字**；普通单主题**勿短于 180 字**。

【材料优先级】
- 抖音/B 站口令页常无 og/正文（标注「不适用」时属正常）。
- **玩梗/抽象/二创视频**：**分享口令 #话题# 标签优先于标题字面**。标题可能是作者名、比喻或整活文案（如「赴约的鹤」），**勿按标题臆测成动物/自然纪录片**。
- **优先选具体作品/角色/梗实体**作 core_keyword（如「剑风传奇」「牢大」），**勿选纯情绪标签**（如「绝望」「破防」「搞笑」）作核心。
- 「牢大/劳大」= 网上对 **科比·布莱恩特** 的梗称；**须**用长辈能听懂的口语介绍他是谁、打什么球、拿过什么荣誉（公开常识），并说明和本条视频/《剑风传奇》梗素材的关系；语气尊重，**禁止**调侃逝者、禁止当成鹤/鸟类。
- 联网摘要若与标签明显无关，**以标签+推荐语为准**；若标题是「皇室战争」游戏、摘要是 Clash 代理/VPN，**以标题/标签为准，忽略偏题摘要**。
- 搜「周杰伦」却返回汉字「周」、搜「八度空间」却返回字典/地名释义时，**整段忽略联网摘要**，只用【背景常识】与推荐语。
- 有效材料：口令关键词（全部）、推荐语、标题（次要）、**相关的**联网检索摘要。

【core_keyword 硬性】
- 须与【分享口令关键词】【孩子推荐语】同一主题实体；**有「剑风传奇」「牢大」等标签时不得输出标题「赴约的鹤」作 core_keyword**。
- 禁止把「皇室战争」手游说成 Clash 代理/VPN/翻墙软件。

【video_prompt / image_prompts】
- 全英文；围绕 core_keyword；写实摄影或纪录片风格；image_prompts 恰好 3 条，角度不同（场景/物件/细节 B-roll）。
- **内容安全**：家庭向、温和；避免政治、暴力、血腥、裸露、争议符号、可识别真人明星肖像；人物用泛化「people in distance / silhouettes」即可。
- video_prompt 可含 --dur 5 --ratio 16:9（D3 预留，当前可不生成视频）。"""


@dataclass
class VideoAnalyzeResult:
    core_keyword: str = ""
    narration: str = ""
    video_prompt: str = ""
    image_prompts: list[str] = field(default_factory=list)
    from_llm: bool = False


def _align_core_keyword(
    core: str,
    *,
    title: str,
    summary: str,
    share_keywords: str = "",
) -> str:
    """修正 LLM 被偏题检索/标题字面带歪的 core_keyword。"""
    from app.services import web_lookup

    blob = f"{title} {summary} {share_keywords}"
    c = (core or "").strip()
    if "皇室战争" in blob:
        bad = ("Clash客户端", "Clash客户端", "代理", "VPN", "翻墙", "订阅")
        if any(b in c for b in bad) or (c.lower().startswith("clash") and "royale" not in c.lower()):
            return "皇室战争"
        if c and "皇室" not in c and "Royale" not in c and "部落" not in c:
            return "皇室战争"

    preferred = web_lookup.primary_search_keyword(
        title=title,
        summary=summary,
        share_keywords=share_keywords,
    )
    if preferred:
        titleish = {title.strip(), title.strip()[:12]}
        emotion_or_title = web_lookup.is_emotion_tag(c) or c in titleish
        if emotion_or_title and not web_lookup.is_emotion_tag(preferred):
            return preferred[:12]
        if c in titleish and preferred != c and len(preferred) >= 3:
            return preferred[:12]
    return c or preferred[:12] or title[:12]


def normalize_narration_perspective(narration: str) -> str:
    """口播 TTS 面向长辈，修正「推给我」等第一人称表述。"""
    t = (narration or "").strip()
    if not t:
        return t
    reps = (
        ("孩子给我", "孩子给您"),
        ("给我推", "给您推"),
        ("推给我", "推给您"),
        ("分享给我", "分享给您"),
        ("发给我", "发给您"),
        ("告诉我个", "跟您说个"),
        ("给我发了", "给您发了"),
    )
    for old, new in reps:
        t = t.replace(old, new)
    return t


def _ensure_disclaimer(narration: str) -> str:
    t = (narration or "").strip()
    if not t:
        return _DISCLAIMER_TAIL
    if "原视频" in t[-80:]:
        return t
    return f"{t.rstrip('。，,…')}。{_DISCLAIMER_TAIL}"


def _fallback_analyze(
    *,
    title: str,
    summary: str,
    page_description: str,
    page_text: str,
    cover_ocr: str,
    web_context: str,
    tag: str,
    share_keywords: str = "",
) -> VideoAnalyzeResult:
    from app.services import web_lookup

    core = (tag or "").strip()
    if not core or core in ("短视频", "生活"):
        for blob in (cover_ocr, title, summary, page_description):
            m = _guess_entity(blob)
            if m:
                core = m
                break
    if not core:
        core = (title or "这条分享")[:12]

    parts: list[str] = []
    parts.append(f"孩子给您分享的这个视频呀，标题说的是「{title}」。")
    entity_ctx = web_lookup.build_builtin_entity_context(
        share_keywords=share_keywords,
        summary=summary,
        title=title,
    )
    if entity_ctx:
        parts.append(entity_ctx[:420])
    hint = (page_description or summary or page_text or "").strip()
    if hint and not entity_ctx:
        parts.append(f"大概是在讲：{hint[:200]}。")
    if web_context.strip():
        chunk = web_context.strip().split("\n\n")[0][:280]
        parts.append(f"最近网上一些说法是：{chunk}")
    elif cover_ocr.strip():
        parts.append(f"封面上的字提到了：{cover_ocr[:120]}。")
    parts.append(f"今天咱们重点聊聊「{core}」——您要是感兴趣，可以多了解了解。")
    parts.append(_DISCLAIMER_TAIL)
    narration = normalize_narration_perspective(" ".join(parts))
    if len(narration) > 520:
        narration = narration[:500].rstrip() + f"……{_DISCLAIMER_TAIL}"

    slug = core if _is_mostly_ascii(core) else "topic"
    return VideoAnalyzeResult(
        core_keyword=core,
        narration=narration,
        video_prompt=f"Slow cinematic push-in on {slug}, warm documentary style --dur 5 --ratio 16:9",
        image_prompts=[
            f"{slug} wide establishing shot, golden hour, realistic photography",
            f"{slug} close detail, soft natural light, documentary style",
            f"{slug} everyday scene, warm tones, 16:9 composition",
        ],
        from_llm=False,
    )


def _is_mostly_ascii(s: str) -> bool:
    if not s:
        return False
    ascii_count = sum(1 for c in s if ord(c) < 128)
    return ascii_count / max(len(s), 1) > 0.5


def _guess_entity(text: str) -> str:
    if not text:
        return ""
    import re

    for pat in (
        r"([\u4e00-\u9fff]{2,8}大学)",
        r"([\u4e00-\u9fff]{2,6}学院)",
        r"#\s*([^#\s]{2,14})\s*#",
    ):
        m = re.search(pat, text)
        if m:
            return m.group(1).strip()[:12]
    return ""


def _normalize_image_prompts(raw: Any, core: str) -> list[str]:
    out: list[str] = []
    if isinstance(raw, list):
        for x in raw[:3]:
            s = str(x).strip()
            if s:
                out.append(s[:500])
    while len(out) < 3:
        out.append(f"{core or 'topic'} scene {len(out) + 1}, cinematic realistic photo")
    return out[:3]


def _narration_music_album_error(narration: str) -> bool:
    """检测口播是否把名曲错误归入《八度空间》专辑。"""
    t = (narration or "").strip()
    if "八度空间" not in t:
        return False
    for song in ("七里香", "晴天"):
        if song not in t:
            continue
        if re.search(
            rf"(八度空间[^。]{{0,48}}{song}|《八度空间》[^。]{{0,40}}{song}|"
            rf"{song}[^。]{{0,32}}八度空间|里面[^。]{{0,12}}《?{song})",
            t,
        ):
            return True
    return False


def _narration_min_chars(share_keywords: str = "", summary: str = "") -> int:
    from app.services import web_lookup

    blob = f"{share_keywords} {summary}"
    tags = [k.strip() for k in re.split(r"[,，、|]", share_keywords or "") if k.strip()]
    entity_tags = [t for t in tags if not web_lookup.is_emotion_tag(t)]
    if len(entity_tags) >= 2 or ("牢大" in blob or "劳大" in blob or "科比" in blob):
        return 220
    return 180


def build_analyze_user_content(
    *,
    title: str,
    source: str,
    summary: str,
    share_keywords: str = "",
    page_description: str = "",
    page_text: str = "",
    cover_ocr_text: str = "",
    web_context: str = "",
    tag: str = "",
    material_diag: Any = None,
    web_diag: Any = None,
) -> str:
    """与 analyze_video_content 发给 LLM 的 user 消息一致（供报告导出）。"""
    from app.services.popular_video_report import PrepareDiag, WebSearchDiag

    prep: PrepareDiag = material_diag if material_diag is not None else PrepareDiag(
        page_description=page_description,
        page_text=page_text,
        cover_ocr=cover_ocr_text,
    )
    wd: WebSearchDiag = web_diag if web_diag is not None else WebSearchDiag()

    og_st, og_note = prep.og_status()
    pt_st, pt_note = prep.page_text_status()
    ocr_st, ocr_note = prep.ocr_status()
    web_st, web_note = wd.status()

    from app.services import web_lookup
    from app.services.paste_intel import sanitize_display_title

    title = sanitize_display_title(title) or (title or "").strip()
    kw_line = (share_keywords or "").strip() or (tag if tag and tag not in ("短视频", "生活", "数码") else "") or "（无）"
    if (share_keywords or "").strip() and "，" in share_keywords:
        kw_line = share_keywords.strip()

    entity_ctx = web_lookup.build_builtin_entity_context(
        share_keywords=share_keywords,
        summary=summary,
        title=title,
    )
    min_chars = _narration_min_chars(share_keywords, summary)

    lines = [
        f"【视频标题】{title}",
        f"【来源】{source}",
        f"【分享口令关键词】{kw_line}",
        f"【孩子推荐语/列表摘要】{summary or '（无）'}",
        f"【兴趣标签】{tag or '（无）'}（仅供分类；检索已跳过「短视频」等泛标签）",
        f"【页面简介 og】[{og_st.split()[0]} {og_note}]\n{(page_description or '（无）')[:1200]}",
        f"【页面正文摘录】[{pt_st.split()[0]} {pt_note}]\n{(page_text or '（无）')[:2000]}",
        f"【封面 OCR 文字】[{ocr_st.split()[0]} {ocr_note}]\n{(cover_ocr_text or '（无）')[:800]}",
    ]
    if entity_ctx:
        lines.append(f"【背景常识（口播须深入展开，优先采用）】\n{entity_ctx}")
    lines.extend(
        [
            f"【联网检索摘要】[{web_st} {web_note}]\n{(web_context or '（无）')[:3800]}",
            "请输出 JSON。口播**面向长辈用「您」**；"
            "**玩梗视频以标签为内核，须讲清作品/人物背景，勿空泛敷衍**；"
            "有《剑风传奇》须介绍漫画动漫与衍生；有牢大/劳大须介绍科比与篮球成就；"
            "联网摘要若明显偏题（如古剑/观鸟/汉字释义）则忽略，以【背景常识】为准；"
            "音乐类勿把《七里香》《晴天》说成《八度空间》里的歌；"
            f"**narration 约 240～320 字，勿短于 {min_chars} 字**。",
        ]
    )
    return "\n".join(lines)


async def analyze_video_content(
    *,
    title: str,
    source: str,
    summary: str,
    share_keywords: str = "",
    page_description: str = "",
    page_text: str = "",
    cover_ocr_text: str = "",
    web_context: str = "",
    tag: str = "",
    material_diag: Any = None,
    web_diag: Any = None,
) -> VideoAnalyzeResult:
    """一次 LLM 调用；与 /api/explain 缓存无关。"""
    from app.services.paste_intel import sanitize_display_title

    title = sanitize_display_title(title) or (title or "").strip()
    fb_kwargs = dict(
        title=title,
        summary=summary,
        page_description=page_description,
        page_text=page_text,
        cover_ocr=cover_ocr_text,
        web_context=web_context,
        tag=tag,
        share_keywords=share_keywords,
    )
    key = (settings.vivo_app_key or "").strip()
    if not key:
        return _fallback_analyze(**fb_kwargs)

    user_content = build_analyze_user_content(
        title=title,
        source=source,
        summary=summary,
        share_keywords=share_keywords,
        page_description=page_description,
        page_text=page_text,
        cover_ocr_text=cover_ocr_text,
        web_context=web_context,
        tag=tag,
        material_diag=material_diag,
        web_diag=web_diag,
    )
    payload: dict[str, Any] = {
        "model": settings.vivo_chat_model,
        "messages": [
            {"role": "system", "content": ANALYZE_SYSTEM},
            {"role": "user", "content": user_content},
        ],
        "temperature": 0.62,
        "max_tokens": 2048,
        "stream": False,
    }
    apply_thinking_params(payload, settings.vivo_chat_model, enabled=False)
    try:
        async with httpx.AsyncClient(timeout=120.0) as client:
            r = await client.post(
                settings.vivo_chat_url,
                params={"request_id": str(uuid.uuid4())},
                headers={
                    "Authorization": f"Bearer {key}",
                    "Content-Type": "application/json; charset=utf-8",
                },
                json=payload,
            )
            r.raise_for_status()
            data = r.json()
        content = str(data["choices"][0]["message"]["content"]).strip()
        if content.startswith("```"):
            content = content.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
        obj = json.loads(content)
        core = _align_core_keyword(
            str(obj.get("core_keyword", "")).strip()[:24],
            title=title,
            summary=summary,
            share_keywords=share_keywords,
        )
        narration = normalize_narration_perspective(
            _ensure_disclaimer(str(obj.get("narration", "")).strip())
        )
        if "皇室战争" in f"{title} {summary}" and "代理" in narration and "手游" not in narration:
            return _fallback_analyze(**fb_kwargs)
        if _narration_music_album_error(narration):
            return _fallback_analyze(**fb_kwargs)
        min_chars = _narration_min_chars(share_keywords, summary)
        if len(narration) < min_chars:
            return _fallback_analyze(**fb_kwargs)
        if len(narration) > 680:
            narration = narration[:540].rstrip() + f"……{_DISCLAIMER_TAIL}"
        return VideoAnalyzeResult(
            core_keyword=core or tag or title[:12],
            narration=narration,
            video_prompt=str(obj.get("video_prompt", "")).strip()[:600],
            image_prompts=_normalize_image_prompts(obj.get("image_prompts"), core),
            from_llm=True,
        )
    except Exception:
        return _fallback_analyze(**fb_kwargs)
