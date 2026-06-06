"""调用 vivo Chat Completions（支持纯文本或多模态 user 消息，见文档 §3.4）。"""

from __future__ import annotations

import ast
import json
import re
import uuid
from typing import Any, Optional, Union

import httpx

from app.config import settings
from app.schemas import ExplainResponse

DEFAULT_SUGGESTIONS = ["这对我有啥用？", "要注意啥？", "还有啥说法？"]

# 文档 §3.3 深度思考字段（按模型分支，勿混用）
CHAT_MODELS_DOUBAO_SEED = (
    "Doubao-Seed-2.0-mini",
    "Doubao-Seed-2.0-lite",
    "Doubao-Seed-2.0-pro",
)
CHAT_MODEL_VOLC_DEEPSEEK = "Volc-DeepSeek-V3.2"
CHAT_MODEL_QWEN = "qwen3.5-plus"


def apply_thinking_params(payload: dict[str, Any], model: str, *, enabled: bool = False) -> None:
    """
    Volc-DeepSeek / Doubao-Seed → thinking.type；
    qwen3.5-plus → enable_thinking。
    结构化 JSON 任务默认 disabled，减少延迟与 reasoning 干扰正文。
    """
    m = (model or "").strip()
    low = m.lower()
    if low == CHAT_MODEL_QWEN.lower() or low.startswith("qwen3.5"):
        payload["enable_thinking"] = enabled
        return
    if "deepseek" in low or low.startswith("doubao-seed"):
        payload["thinking"] = {"type": "enabled" if enabled else "disabled"}

SYSTEM_PROMPT = """你是「暖桥」家庭助手的撰稿人，读者是 50–70 岁、少上网的长辈。

【五条输出字段】
输出必须是一个 JSON 对象，不要用 Markdown 代码块，键为：
{"plain_summary":"","background":"","glossary":"","disclaimer":"","suggested_questions":["","",""]}

1) plain_summary（用长辈能懂的话）
- 像**有经验、常帮家人看内容的人**当面讲：直接说「这条视频是讲什么的」，温柔、肯定、好懂。
- **只给结论，不讲推理过程**：禁止「从标题/关键词/推荐语看」「根据材料」「推测」「推断」等机器人腔；不要逐步交代你怎么分析出来的。
- 开头示例：✅「孩子给您推的是一条游戏攻略，讲的是……」 ❌「从标题和关键词看，这条内容是……」
- **禁止**整段只复述标题或背诵链接；最多一句「想细看可以点进原视频」。
- 若分享备注/标题含具体实体（角色、学校、人名、作品），**围绕该实体**说清是什么、和孩子分享的关系。

2) background（背景小知识）
- 补充长辈可能缺的**常识背景**（游戏/梗/事件是什么、大家为啥关注），语气像家人聊天。
- **不要**写「暂无更多可靠公开背景」敷衍；说不清出处时，用 1～3 句观看建议（如抖音评论区常有解释）。

3) glossary（词语小抄）
- 从标题/简介抽 **2～5 个**陌生词，**必须**用编号分行输出，格式固定：
  1. 词：一句话解释
  2. 词：一句话解释
  （每条单独一行，禁止输出 Python 列表、禁止方括号 JSON。）
- 至少解释标题里最核心的一个词。

4) disclaimer
- 必须包含：内容仅供家庭交流参考，不构成专业建议；具体情节、人物关系与细节请以原视频/原文为准。

5) suggested_questions（固定 **3 条**追问建议）
- JSON 数组，**恰好 3 个**字符串；每条 **不超过 14 个汉字**，口语、像家人随口问。
- 风格示例：「这对我有啥用？」「要注意啥？」「还有啥说法？」
- 忌书面语、忌「综上所述」；三条**不要重复**。

【家人追问时的硬性要求】
- 当用户消息里出现 **【家人追问】** 或 **【追问】** 小节时：**plain_summary 开头必须用 2～5 句优先直接回答该追问**，再补充与原分享相关的背景；**禁止**与「无追问」时逐字或仅换序重复同一段开头。
- background、glossary 也需体现追问角度的新信息；suggested_questions 仍可给 3 条新的随口问法。

【联网检索材料】
- 若材料中有「联网新鲜摘要」或「联网检索摘要」，那是刚搜到的公开片段，**优先转述**进 plain_summary / background。
- 若摘要仍不够新或不够具体，**请调用 web_search** 用标题/口令关键词再查一轮（与追问同款工具）。
- 你的任务是：把检索信息**转述成温柔、口语、耐心**的长辈向说明；**不要**发明摘要里没有的具体日期、数字、爆料细节。
- 若摘要与分享标题明显不一致，以「孩子分享的标题/口令」为准，并提醒长辈「网上说法杂，一起看原视频最准」。
- 本地内置常识（若有）仅在检索明显不足时作补充，**不得**替代新鲜检索。

【事实边界】
- 不得捏造具体日期、数据、采访原话。
- 无检索摘要时，可运用与标题相符的常识帮助理解；不确定时标明是推测或「常见说法」。
- 若用户消息里带有封面图，可结合画面氛围（人数、场景大致类型）辅助描述，**不要**编造图片中看不清的人物姓名或身份。"""


_ROBOT_SUMMARY_PREFIXES = (
    "从标题和关键词看，",
    "从标题和关键词来看，",
    "从标题和标签看，",
    "从标题看，",
    "从标题来看，",
    "从分享的内容看，",
    "从分享内容看，",
    "从关键词看，",
    "从推荐语看，",
    "根据标题和关键词，",
    "根据材料，",
    "根据分享内容，",
)


def normalize_plain_summary(text: str) -> str:
    """去掉「从标题看」等推理过程表述，保留服务者口吻。"""
    t = (text or "").strip()
    if not t:
        return t
    for prefix in _ROBOT_SUMMARY_PREFIXES:
        if t.startswith(prefix):
            t = t[len(prefix) :].lstrip()
    t = re.sub(r"^从[^，,。]{2,18}[看来说]，?", "", t)
    t = re.sub(r"^根据[^，,。]{2,18}[，,]?", "", t)
    return t.strip()


def normalize_glossary(raw: Any) -> str:
    """词语小抄 →「1. 词：解释」分行；兼容 LLM 误输出 list 字符串。"""
    items: list[str] = []
    if isinstance(raw, list):
        items = [str(x).strip() for x in raw if str(x).strip()]
    else:
        s = str(raw or "").strip()
        if not s:
            return "暂无特别需要解释的词语。"
        if s.startswith("[") and "]" in s:
            try:
                parsed = ast.literal_eval(s)
                if isinstance(parsed, list):
                    items = [str(x).strip() for x in parsed if str(x).strip()]
            except (SyntaxError, ValueError):
                pass
        if not items:
            for line in re.split(r"[\n；;]+", s):
                line = re.sub(r"^\d+[.．、)\s]+", "", line.strip())
                if line and line not in ("[]", "['']"):
                    items.append(line)
    if not items:
        return str(raw).strip() or "暂无特别需要解释的词语。"
    lines: list[str] = []
    for i, item in enumerate(items[:5], start=1):
        item = item.strip().strip("'\"")
        if "：" not in item and ":" in item:
            item = item.replace(":", "：", 1)
        if "：" not in item:
            item = f"{item}：长辈视频里常见的说法"
        lines.append(f"{i}. {item}")
    return "\n".join(lines)


def normalize_explain_response(resp: ExplainResponse) -> ExplainResponse:
    """解读结果后处理（缓存读出时也走一遍，兼容旧数据）。"""
    return resp.model_copy(
        update={
            "plain_summary": normalize_plain_summary(resp.plain_summary),
            "glossary": normalize_glossary(resp.glossary),
        }
    )


def _normalize_suggestions(raw: Any) -> list[str]:
    if isinstance(raw, list) and len(raw) >= 3:
        out: list[str] = []
        for i in range(3):
            s = str(raw[i]).strip().replace("\n", "")[:14]
            out.append(s if s else DEFAULT_SUGGESTIONS[i])
        return out
    return list(DEFAULT_SUGGESTIONS)


def _user_message_parts(text: str, preview_image_url: Optional[str]) -> Union[str, list[dict[str, Any]]]:
    url = (preview_image_url or "").strip()
    if url.startswith(("http://", "https://")):
        return [
            {"type": "text", "text": text},
            {"type": "image_url", "image_url": {"url": url}},
        ]
    return text


def _fallback_no_key(material: str, question: Optional[str]) -> ExplainResponse:
    q = (question or "").strip()
    extra = f"\n（家人追问：{q}）" if q else ""
    return ExplainResponse(
        plain_summary=f"当前为离线演示模式：根据已有摘要向长辈说明如下。{material[:300]}…{extra}",
        background=(
            "服务端未读取到 VIVO_APP_KEY。请确认：① 文件为 server/.env（与 app 同级）；② 一行 "
            "VIVO_APP_KEY=你的密钥（勿加引号、勿多空格）；③ 保存后重启 uvicorn；④ 在 server 目录执行 "
            "`python -c \"from app.config import settings; print(bool(settings.vivo_app_key.strip()))\"` 应输出 True。"
        ),
        glossary="暂无特别需要解释的词语（离线模式）。",
        disclaimer="内容仅供家庭交流参考，不构成专业建议；请以原视频或原文为准。",
        suggested_questions=list(DEFAULT_SUGGESTIONS),
    )


def _fallback_api_error(material: str, question: Optional[str], hint: str) -> ExplainResponse:
    q = (question or "").strip()
    extra = f"\n（家人追问：{q}）" if q else ""
    return ExplainResponse(
        plain_summary=f"当前为离线演示模式：根据已有摘要向长辈说明如下。{material[:300]}…{extra}",
        background=f"已配置密钥，但调用蓝心 Chat 失败。{hint}",
        glossary="暂无特别需要解释的词语（离线模式）。",
        disclaimer="内容仅供家庭交流参考，不构成专业建议；请以原视频或原文为准。",
        suggested_questions=list(DEFAULT_SUGGESTIONS),
    )


FOLLOW_UP_SYSTEM = """你是「暖桥」家庭助手，正在回答长辈的一句追问。

【硬性要求】
1) 只输出一段中文口语回答，**不要** JSON、不要 Markdown、不要小标题。
2) **第一句必须直接回答追问**（好上手/难不难/值不值得/什么意思等），给出明确倾向：
   例如「挺简单的，新手也能跟着练」或「偏难，得练一阵配队」。
3) 禁止绕弯：不要用「想细看可以点原视频」当主答案；不要大段复述背景。
4) 材料不足时，可结合标题/备注/检索做合理推测，末尾加一句：
   「按标题和常见玩法推测，具体以原视频为准。」
5) 语气温柔、像家人微信解释，80～180 字为宜。"""


async def answer_follow_up(
    *,
    material: str,
    question: str,
    prior_summary: str = "",
) -> tuple[str, bool, bool]:
    """
    追问专用：返回 (answer, from_llm, web_searched)。
    优先 Function Calling 联网；失败则降级 web_lookup + 直答。
    """
    from app.services.vivo_chat_tools import chat_with_tools, run_web_search_tool

    q = (question or "").strip()
    if not q:
        return "您想问啥，再输入一句就好。", False, False

    key = (settings.vivo_app_key or "").strip()
    if not key:
        return f"当前离线演示：关于「{q}」，建议点开原视频看看孩子分享的内容。", False, False

    user_parts = [f"【分享材料】\n{material[:3500]}"]
    if (prior_summary or "").strip():
        user_parts.append(f"【此前摘要（勿重复大段照抄）】\n{prior_summary.strip()[:800]}")
    user_parts.append(f"【家人追问】\n{q}")

    messages = [
        {"role": "system", "content": FOLLOW_UP_SYSTEM},
        {"role": "user", "content": "\n\n".join(user_parts)},
    ]

    use_tools = bool(settings.vivo_explain_use_tools)
    result = await chat_with_tools(
        messages,
        model=settings.vivo_explain_model,
        temperature=0.65,
        max_tokens=512,
        use_tools=use_tools,
    )
    answer = (result.content or "").strip()
    if answer and len(answer) >= 12:
        return answer, True, result.searched

    # 降级：关键词检索 + 再答
    from app.services import web_lookup

    search_blob = ""
    searched = False
    if settings.web_search_enabled:
        for kw in web_lookup.extract_video_search_keywords(summary=material, title=material)[:2]:
            blob, _ = await web_lookup.lookup_fresh_blurb(kw)
            if blob:
                search_blob += f"\n{blob[:1200]}"
                searched = True
        if q:
            extra = await run_web_search_tool(q[:40])
            if extra and "未检索到" not in extra:
                search_blob += f"\n{extra[:1200]}"
                searched = True

    fallback_messages = [
        {"role": "system", "content": FOLLOW_UP_SYSTEM},
        {
            "role": "user",
            "content": "\n\n".join(user_parts)
            + (f"\n\n【联网补充】\n{search_blob[:2500]}" if search_blob else ""),
        },
    ]
    result2 = await chat_with_tools(
        fallback_messages,
        model=settings.vivo_explain_model_fallback,
        temperature=0.65,
        max_tokens=512,
        use_tools=False,
    )
    answer2 = (result2.content or "").strip()
    if answer2:
        return answer2, True, searched or result2.searched
    return (
        f"关于「{q}」：从标题看是孩子分享的教程类内容，您点开原视频最准。"
        f"按常见情况推测，多练几遍一般能上手。具体以原视频为准。",
        False,
        searched,
    )


def _parse_explain_material_fields(material: str) -> dict[str, str]:
    fields = {"title": "", "summary": "", "share_keywords": ""}
    for key, pat in (
        ("title", r"标题[：:]\s*(.+)"),
        ("summary", r"列表摘要[：:]\s*(.+)"),
        ("share_keywords", r"分享关键词[：:]\s*(.+)"),
    ):
        m = re.search(pat, material or "")
        if m:
            fields[key] = m.group(1).strip()
    return fields


def _parse_explain_json(content: str) -> dict[str, Any] | None:
    text = (content or "").strip()
    if not text:
        return None
    if text.startswith("```"):
        text = text.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
    try:
        obj = json.loads(text)
        return obj if isinstance(obj, dict) else None
    except json.JSONDecodeError:
        return None


async def explain_from_material(
    material: str,
    question: Optional[str] = None,
    preview_image_url: Optional[str] = None,
) -> ExplainResponse:
    key = (settings.vivo_app_key or "").strip()
    if not key:
        return _fallback_no_key(material, question)

    from app.services import web_lookup

    fields = _parse_explain_material_fields(material)
    fresh = ""
    if settings.web_search_enabled and not (question or "").strip():
        fresh = await web_lookup.fetch_fresh_context_for_topic(
            title=fields["title"],
            summary=fields["summary"],
            share_keywords=fields["share_keywords"],
        )

    user_content = f"【材料】\n{material}\n\n请严格按 system 要求生成 JSON，五个字段都要填充实内容。"
    if fresh:
        user_content += (
            f"\n\n【联网新鲜摘要（优先转述；仍不足请 web_search）】\n{fresh[:3000]}"
        )
    if question:
        user_content += (
            f"\n【家人追问】\n{question.strip()}\n"
            "请严格遵守 system 中「家人追问时的硬性要求」：plain_summary 开头先答此问，禁止与无追问版雷同。"
        )

    explain_model = settings.vivo_explain_model or settings.vivo_chat_model
    temp = 0.72 if (question or "").strip() else 0.55
    user_msg = _user_message_parts(user_content, preview_image_url)

    try:
        if settings.vivo_explain_use_tools and settings.web_search_enabled:
            from app.services.vivo_chat_tools import chat_with_tools

            tool_result = await chat_with_tools(
                [
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {
                        "role": "user",
                        "content": user_msg
                        + "\n\n若联网摘要不够新，请调用 web_search 再查后输出 JSON。",
                    },
                ],
                model=explain_model,
                temperature=temp,
                max_tokens=3072,
                max_tool_rounds=2,
                use_tools=True,
            )
            obj = _parse_explain_json(tool_result.content)
            if obj:
                return normalize_explain_response(
                    ExplainResponse(
                        plain_summary=str(obj.get("plain_summary", "")),
                        background=str(obj.get("background", "")).strip(),
                        glossary=str(obj.get("glossary", "")),
                        disclaimer=str(obj.get("disclaimer", "")).strip(),
                        from_llm=True,
                        suggested_questions=_normalize_suggestions(
                            obj.get("suggested_questions")
                        ),
                    )
                )

        payload: dict[str, Any] = {
            "model": explain_model,
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_msg},
            ],
            "temperature": temp,
            "max_tokens": 3072,
            "stream": False,
        }
        apply_thinking_params(payload, explain_model, enabled=False)

        request_id = str(uuid.uuid4())
        headers = {
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json; charset=utf-8",
        }
        async with httpx.AsyncClient(timeout=120.0) as client:
            r = await client.post(
                settings.vivo_chat_url,
                params={"request_id": request_id},
                headers=headers,
                json=payload,
            )
            r.raise_for_status()
            data = r.json()
        content = str(data["choices"][0]["message"]["content"]).strip()
        obj = _parse_explain_json(content)
        if not obj:
            raise ValueError("explain JSON parse failed")
        return normalize_explain_response(
            ExplainResponse(
                plain_summary=str(obj.get("plain_summary", "")),
                background=str(obj.get("background", "")).strip(),
                glossary=str(obj.get("glossary", "")),
                disclaimer=str(obj.get("disclaimer", "")).strip(),
                from_llm=True,
                suggested_questions=_normalize_suggestions(obj.get("suggested_questions")),
            )
        )
    except httpx.HTTPStatusError as e:
        body = (e.response.text or "")[:400]
        hint = f"HTTP {e.response.status_code}。请核对 AppKey、模型权限与赛事文档。响应片段：{body}"
        return _fallback_api_error(material, question, hint)
    except Exception as e:
        hint = f"错误类型：{type(e).__name__}，{e}"
        return _fallback_api_error(material, question, hint)
