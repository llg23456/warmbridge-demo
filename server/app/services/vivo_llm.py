"""调用 vivo Chat Completions（支持纯文本或多模态 user 消息，见文档 §3.4）。"""

from __future__ import annotations

import json
import uuid
from typing import Any, Optional, Union

import httpx

from app.config import settings
from app.schemas import ExplainResponse

DEFAULT_SUGGESTIONS = ["这对我有啥用？", "要注意啥？", "还有啥说法？"]

SYSTEM_PROMPT = """你是「暖桥」家庭助手的撰稿人，读者是 50–70 岁、少上网的长辈。

【五条输出字段】
输出必须是一个 JSON 对象，不要用 Markdown 代码块，键为：
{"plain_summary":"","background":"","glossary":"","disclaimer":"","suggested_questions":["","",""]}

1) plain_summary（用长辈能懂的话）
- 说明「这条分享大概是什么内容、孩子在看什么热闹」：舞蹈/翻跳类就解释什么叫翻跳、这类视频常表达的心情（如怀旧、对比、展示练习成果）；搞笑/梗类就讲「大家在笑什么、梗好笑在哪」；资讯类概括要点。
- **禁止**整段只复述标题或背诵链接；链接最多顺带一句「想细看可以点进原文或视频」。
- 若材料只有标题、没有页面简介，可结合标题里的题材做**合理推断**，用「从标题看，大概是……」引出，并说明未能看到正片细节。

2) background（背景小知识）
- 写与标题/页面简介相关的**常识或文化背景**：例如梗常出自哪个节目/哪位博主/哪类名场面（若属于广泛流传的公开网络文化，可简述，并可用「网上常见说法是……」）；舞蹈类可写这类内容在短视频里一般怎么用。
- **不要**写死板的「暂无更多可靠公开背景」敷衍；若确实无法指向具体出处，就写 1～3 句平台与观看建议（如 bilibili/抖音 上可看作者主页、评论区往往有解释），仍要有信息量。

3) glossary（词语小抄）
- 从标题、页面简介里抽出 **2～5 个**长辈可能陌生的词：梗名、舞种、拼音缩写、平台用语等。
- 每行格式：「词：一句话解释」。**不要轻易**整段只写「暂无特别需要解释的词语」；至少解释标题里最核心的一个词或现象。

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
- 若材料中出现「联网检索摘要」，那是搜索引擎自动抓取的公开片段，**可能不全或与视频有出入**。
- 你的任务是：把这些信息**转述成温柔、口语、耐心**的长辈向说明；**不要**发明摘要里没有的具体日期、数字、爆料细节。
- 若摘要与分享标题明显不一致，以「孩子分享的标题/口令」为准，并提醒长辈「网上说法杂，一起看原视频最准」。

【事实边界】
- 不得捏造具体日期、数据、采访原话。
- 无检索摘要时，可运用与标题相符的常识帮助理解；不确定时标明是推测或「常见说法」。
- 若用户消息里带有封面图，可结合画面氛围（人数、场景大致类型）辅助描述，**不要**编造图片中看不清的人物姓名或身份。"""


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


async def explain_from_material(
    material: str,
    question: Optional[str] = None,
    preview_image_url: Optional[str] = None,
) -> ExplainResponse:
    key = (settings.vivo_app_key or "").strip()
    if not key:
        return _fallback_no_key(material, question)

    user_content = f"【材料】\n{material}\n\n请严格按 system 要求生成 JSON，五个字段都要填充实内容。"
    if question:
        user_content += (
            f"\n【家人追问】\n{question.strip()}\n"
            "请严格遵守 system 中「家人追问时的硬性要求」：plain_summary 开头先答此问，禁止与无追问版雷同。"
        )

    payload: dict[str, Any] = {
        "model": settings.vivo_chat_model,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": _user_message_parts(user_content, preview_image_url)},
        ],
        "temperature": 0.72 if (question or "").strip() else 0.55,
        "max_tokens": 3072,
        "stream": False,
    }

    request_id = str(uuid.uuid4())
    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json; charset=utf-8",
    }

    try:
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
        if content.startswith("```"):
            content = content.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
        obj = json.loads(content)
        return ExplainResponse(
            plain_summary=str(obj.get("plain_summary", "")),
            background=str(obj.get("background", "")),
            glossary=str(obj.get("glossary", "")),
            disclaimer=str(obj.get("disclaimer", "")),
            from_llm=True,
            suggested_questions=_normalize_suggestions(obj.get("suggested_questions")),
        )
    except httpx.HTTPStatusError as e:
        body = (e.response.text or "")[:400]
        hint = f"HTTP {e.response.status_code}。请核对 AppKey、模型权限与赛事文档。响应片段：{body}"
        return _fallback_api_error(material, question, hint)
    except Exception as e:
        hint = f"错误类型：{type(e).__name__}，{e}"
        return _fallback_api_error(material, question, hint)
