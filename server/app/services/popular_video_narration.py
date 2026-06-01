"""根据文字解读撰写口播稿（不改详情页解读缓存）。"""

from __future__ import annotations

import json
import uuid
from typing import Any

import httpx

from app.config import settings


NARRATION_SYSTEM = """你是「暖桥」家庭助手的口播撰稿人，读者是 50–70 岁长辈。

任务：根据【文字解读材料】写一段**仅用于合成讲解视频旁白**的口播稿。

硬性要求：
1) 只输出 JSON：{"narration": "..."}，不要 Markdown 代码块。
2) narration 为**纯口语文本**，温柔、耐心，像家人当面讲，可分 3～5 句。
3) 朗读时长 **45～75 秒**（约 **280～450 个汉字**），内容要**充实**：
   - 先概括「这条视频/分享大概在讲什么」；
   - 再补充 1～2 点「背景小知识」或「对孩子/家庭有啥用」；
   - 若有生僻词/梗，用一两句话通俗解释；
   - 结尾提醒「想细看可以点开原视频」。
4) 可融合 plain_summary、background、glossary 的信息，但**不要**像念课文一样逐段照读。
5) 不得捏造具体日期、数据、人物身份；不确定用「大概」「常见说法是」。"""


def _fallback_narration(
    title: str,
    plain_summary: str,
    background: str = "",
    glossary: str = "",
) -> str:
    parts: list[str] = []
    base = (plain_summary or title or "孩子分享了一条视频").strip()
    if not base.startswith("孩子"):
        base = f"孩子分享的这个视频呀，{base}"
    parts.append(base)
    bg = (background or "").strip()
    if bg and "离线" not in bg[:20]:
        parts.append(bg[:180])
    gl = (glossary or "").strip()
    if gl and "暂无" not in gl[:8]:
        parts.append(f"顺便解释几个词：{gl[:160]}")
    parts.append("您要是感兴趣，可以点开原视频慢慢看。")
    text = " ".join(parts)
    if len(text) > 480:
        text = text[:478].rstrip() + "……"
    return text


async def build_narration_script(
    *,
    title: str,
    plain_summary: str,
    background: str = "",
    glossary: str = "",
    page_hint: str = "",
) -> tuple[str, bool]:
    """返回 (口播稿, from_llm)。"""
    summary = (plain_summary or "").strip()
    if not summary:
        summary = (page_hint or title or "一条家庭分享的视频")[:500]

    key = (settings.vivo_app_key or "").strip()
    if not key:
        return _fallback_narration(title, summary, background, glossary), False

    user = (
        f"【视频标题】{title}\n"
        f"【用长辈能懂的话】\n{summary}\n"
        f"【背景小知识】\n{(background or '（无）')[:800]}\n"
        f"【词语小抄】\n{(glossary or '（无）')[:600]}\n"
        "请生成 JSON，narration 为 45～75 秒、信息充实的口播稿。"
    )
    payload: dict[str, Any] = {
        "model": settings.vivo_chat_model,
        "messages": [
            {"role": "system", "content": NARRATION_SYSTEM},
            {"role": "user", "content": user},
        ],
        "temperature": 0.55,
        "max_tokens": 1536,
        "stream": False,
    }
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
        narration = str(obj.get("narration", "")).strip()
        if len(narration) < 80:
            return _fallback_narration(title, summary, background, glossary), False
        if len(narration) > 520:
            narration = narration[:518].rstrip() + "……"
        return narration, True
    except Exception:
        return _fallback_narration(title, summary, background, glossary), False
