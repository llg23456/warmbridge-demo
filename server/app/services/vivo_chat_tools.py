"""vivo Chat Function Calling：web_search 工具编排（OpenAI tools + <APIs> 双协议）。"""

from __future__ import annotations

import json
import logging
import re
import uuid
from dataclasses import dataclass, field
from typing import Any

import httpx

from app.config import settings
from app.services.vivo_llm import apply_thinking_params

_log = logging.getLogger(__name__)

WEB_SEARCH_TOOL: dict[str, Any] = {
    "type": "function",
    "function": {
        "name": "web_search",
        "description": (
            "联网搜索公开信息。当用户问最新热点、游戏版本、角色强度、学校招生、"
            "实时事件，或材料不足以直接回答时调用。"
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "搜索关键词，中文为主，可含年份/游戏名/角色名",
                }
            },
            "required": ["query"],
        },
    },
}

_APIS_TAG_RE = re.compile(r"<APIs>\s*(\[.*?\])\s*</APIs>", re.DOTALL)


@dataclass
class ChatToolResult:
    content: str = ""
    tool_rounds: int = 0
    searched: bool = False
    search_queries: list[str] = field(default_factory=list)
    model_used: str = ""
    tools_supported: bool = True


async def run_web_search_tool(query: str) -> str:
    """执行 web_search 工具：Bing + DDG + 百科（web_lookup）。"""
    from app.services import web_lookup

    q = (query or "").strip()
    if not q:
        return "（搜索关键词为空）"
    text, status = await web_lookup.lookup_fresh_blurb(q)
    if text and len(text) > 30:
        return f"【搜索词：{q}】（{status}）\n{text[:2800]}"
    blob = await web_lookup.duckduckgo_html_search_snippets(q, timeout=12.0)
    if blob and len(blob) > 40:
        return f"【搜索词：{q}】\n{blob[:2800]}"
    return f"【搜索词：{q}】未检索到有效摘要。"


def _parse_tool_calls(message: dict[str, Any]) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    for tc in message.get("tool_calls") or []:
        if not isinstance(tc, dict):
            continue
        fn = tc.get("function") if isinstance(tc.get("function"), dict) else {}
        name = str(fn.get("name") or "").strip()
        raw_args = fn.get("arguments") or "{}"
        try:
            args = json.loads(raw_args) if isinstance(raw_args, str) else dict(raw_args)
        except json.JSONDecodeError:
            args = {"query": str(raw_args)[:200]}
        out.append({"id": tc.get("id") or str(uuid.uuid4()), "name": name, "arguments": args})
    return out


def _parse_apis_tag(content: str) -> list[dict[str, Any]]:
    m = _APIS_TAG_RE.search(content or "")
    if not m:
        return []
    try:
        arr = json.loads(m.group(1))
    except json.JSONDecodeError:
        return []
    out: list[dict[str, Any]] = []
    if not isinstance(arr, list):
        return out
    for item in arr:
        if not isinstance(item, dict):
            continue
        name = str(item.get("name") or "").strip()
        args = item.get("arguments") if isinstance(item.get("arguments"), dict) else {}
        if not args and isinstance(item.get("arguments"), str):
            try:
                args = json.loads(item["arguments"])
            except json.JSONDecodeError:
                args = {"query": item["arguments"]}
        out.append({"id": str(uuid.uuid4()), "name": name, "arguments": args or {}})
    return out


def _tools_permission_denied(data: dict[str, Any]) -> bool:
    code = data.get("code")
    msg = str(data.get("message") or data.get("msg") or "")
    if code in (1002, 30001):
        return True
    return "not having this ability" in msg.lower()


async def _post_chat(
    client: httpx.AsyncClient,
    *,
    payload: dict[str, Any],
    request_id: str,
) -> dict[str, Any]:
    key = (settings.vivo_app_key or "").strip()
    r = await client.post(
        settings.vivo_chat_url,
        params={"request_id": request_id},
        headers={
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json; charset=utf-8",
        },
        json=payload,
        timeout=120.0,
    )
    r.raise_for_status()
    return r.json()


async def chat_with_tools(
    messages: list[dict[str, Any]],
    *,
    model: str | None = None,
    temperature: float = 0.55,
    max_tokens: int = 2048,
    max_tool_rounds: int = 2,
    use_tools: bool = True,
) -> ChatToolResult:
    """
    带 web_search 工具的 Chat；先 OpenAI tools，若模型回 <APIs> 则解析执行。
    tools 权限不可用或无调用时返回模型正文。
    """
    key = (settings.vivo_app_key or "").strip()
    if not key:
        return ChatToolResult(content="", tools_supported=False)

    primary = (model or settings.vivo_explain_model or settings.vivo_chat_model).strip()
    fallback = (settings.vivo_explain_model_fallback or "Doubao-Seed-2.0-lite").strip()
    models_to_try = [primary] if primary == fallback else [primary, fallback]

    last_err = ""
    for model_name in models_to_try:
        msgs = list(messages)
        searched = False
        queries: list[str] = []
        tool_rounds = 0
        tools_ok = True

        try:
            async with httpx.AsyncClient() as client:
                for _ in range(max_tool_rounds + 1):
                    payload: dict[str, Any] = {
                        "model": model_name,
                        "messages": msgs,
                        "temperature": temperature,
                        "max_tokens": max_tokens,
                        "stream": False,
                    }
                    apply_thinking_params(payload, model_name, enabled=False)
                    if use_tools and tool_rounds < max_tool_rounds:
                        payload["tools"] = [WEB_SEARCH_TOOL]

                    data = await _post_chat(client, payload=payload, request_id=str(uuid.uuid4()))
                    if _tools_permission_denied(data):
                        tools_ok = False
                        _log.warning("vivo chat tools denied model=%s code=%s", model_name, data.get("code"))
                        break

                    choices = data.get("choices") or []
                    if not choices:
                        last_err = f"empty choices: {str(data)[:200]}"
                        break
                    choice = choices[0]
                    msg = choice.get("message") if isinstance(choice.get("message"), dict) else {}
                    finish = str(choice.get("finish_reason") or "")

                    calls = _parse_tool_calls(msg)
                    if not calls and finish != "tool_calls":
                        content = str(msg.get("content") or "").strip()
                        apis_calls = _parse_apis_tag(content)
                        if apis_calls:
                            calls = apis_calls
                            msg = {**msg, "content": _APIS_TAG_RE.sub("", content).strip()}

                    if not calls:
                        content = str(msg.get("content") or "").strip()
                        return ChatToolResult(
                            content=content,
                            tool_rounds=tool_rounds,
                            searched=searched,
                            search_queries=queries,
                            model_used=model_name,
                            tools_supported=tools_ok,
                        )

                    tool_rounds += 1
                    msgs.append(msg)
                    for call in calls:
                        name = call.get("name") or ""
                        args = call.get("arguments") if isinstance(call.get("arguments"), dict) else {}
                        if name != "web_search":
                            result_text = f"未知工具 {name}"
                        else:
                            q = str(args.get("query") or "").strip()
                            queries.append(q)
                            result_text = await run_web_search_tool(q)
                            searched = True
                        msgs.append(
                            {
                                "role": "tool",
                                "tool_call_id": call.get("id") or str(uuid.uuid4()),
                                "content": result_text,
                            }
                        )

                if tool_rounds >= max_tool_rounds:
                    payload = {
                        "model": model_name,
                        "messages": msgs,
                        "temperature": temperature,
                        "max_tokens": max_tokens,
                        "stream": False,
                    }
                    apply_thinking_params(payload, model_name, enabled=False)
                    data = await _post_chat(client, payload=payload, request_id=str(uuid.uuid4()))
                    content = str((data.get("choices") or [{}])[0].get("message", {}).get("content") or "")
                    return ChatToolResult(
                        content=content.strip(),
                        tool_rounds=tool_rounds,
                        searched=searched,
                        search_queries=queries,
                        model_used=model_name,
                        tools_supported=tools_ok,
                    )
        except Exception as e:
            last_err = str(e)
            _log.warning("chat_with_tools model=%s err=%s", model_name, e)
            continue

        if not tools_ok and use_tools:
            plain = await chat_with_tools(
                messages,
                model=model_name,
                temperature=temperature,
                max_tokens=max_tokens,
                max_tool_rounds=0,
                use_tools=False,
            )
            plain.tools_supported = False
            return plain

    _log.warning("chat_with_tools all models failed: %s", last_err)
    return ChatToolResult(content="", tools_supported=False, model_used=primary)
