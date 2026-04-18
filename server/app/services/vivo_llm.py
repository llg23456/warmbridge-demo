"""调用 vivo Chat Completions；无密钥时返回 None。"""

from __future__ import annotations

import json
import uuid
from typing import Any, Optional

import httpx

from app.config import settings
from app.schemas import ExplainResponse


SYSTEM_PROMPT = """你是「暖桥」家庭助手的撰稿人，读者是 50–70 岁、少上网的长辈。
你必须严格只根据用户提供的「材料」写作，不得编造新闻来源、不得捏造事实。
若材料不足，请在 plain_summary 中明确说「公开信息有限」，background 写「暂无更多可靠公开背景」。
输出必须是**一个 JSON 对象**，不要 Markdown 代码块，键为：
{"plain_summary":"","background":"","glossary":"","disclaimer":""}
其中 glossary 用一段中文，把可能出现的网络用语用「词：说明」分行简要解释；没有则写「暂无特别需要解释的词语」。
disclaimer 固定包含：内容仅供家庭交流参考，不构成专业建议；请以原报道为准。"""


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
        disclaimer="内容仅供家庭交流参考，不构成专业建议；请以原报道为准。",
    )


def _fallback_api_error(material: str, question: Optional[str], hint: str) -> ExplainResponse:
    q = (question or "").strip()
    extra = f"\n（家人追问：{q}）" if q else ""
    return ExplainResponse(
        plain_summary=f"当前为离线演示模式：根据已有摘要向长辈说明如下。{material[:300]}…{extra}",
        background=f"已配置密钥，但调用蓝心 Chat 失败。{hint}",
        glossary="暂无特别需要解释的词语（离线模式）。",
        disclaimer="内容仅供家庭交流参考，不构成专业建议；请以原报道为准。",
    )


async def explain_from_material(material: str, question: Optional[str] = None) -> ExplainResponse:
    key = (settings.vivo_app_key or "").strip()
    if not key:
        return _fallback_no_key(material, question)

    user_content = f"【材料】\n{material}\n\n请生成 JSON。"
    if question:
        user_content += f"\n【追问】\n{question}\n请在 JSON 中适度体现在 plain_summary 或 background 中。"

    payload: dict[str, Any] = {
        "model": settings.vivo_chat_model,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_content},
        ],
        "temperature": 0.5,
        "max_tokens": 2048,
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
        )
    except httpx.HTTPStatusError as e:
        body = (e.response.text or "")[:400]
        hint = f"HTTP {e.response.status_code}。请核对 AppKey、模型权限与赛事文档。响应片段：{body}"
        return _fallback_api_error(material, question, hint)
    except Exception as e:
        hint = f"错误类型：{type(e).__name__}，{e}"
        return _fallback_api_error(material, question, hint)
