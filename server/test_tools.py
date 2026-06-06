"""探测 vivo Chat 是否支持 tools / <APIs> Function Calling。用法：cd server && python test_tools.py"""

from __future__ import annotations

import json
import os
import sys
import uuid

import httpx

# 优先 server/.env（与 app.config 一致）
try:
    from app.config import settings

    APP_KEY = (settings.vivo_app_key or "").strip()
except Exception:
    APP_KEY = (os.environ.get("VIVO_APP_KEY") or "").strip()

MODEL = os.environ.get("VIVO_CHAT_MODEL_TEST", "Doubao-Seed-2.0-pro")
URL = "https://api-ai.vivo.com.cn/v1/chat/completions"
REQUEST_ID = str(uuid.uuid4())
QUESTION = "2026年6月6日成都天气怎么样？"

if not APP_KEY:
    print("[FAIL] 未读到 VIVO_APP_KEY：请配置 server/.env 或环境变量")
    sys.exit(1)

payload = {
    "model": MODEL,
    "messages": [
        {"role": "system", "content": "你是 helpful assistant。"},
        {"role": "user", "content": QUESTION},
    ],
    "tools": [
        {
            "type": "function",
            "function": {
                "name": "get_weather",
                "description": "查询指定城市的天气",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "city": {"type": "string", "description": "城市名，如成都"},
                        "date": {"type": "string", "description": "日期，如2026-06-06"},
                    },
                    "required": ["city"],
                },
            },
        }
    ],
    "stream": False,
}

headers = {
    "Authorization": f"Bearer {APP_KEY}",
    "Content-Type": "application/json",
}
params = {"request_id": REQUEST_ID}

print(f"[TEST] vivo Chat tools\nmodel={MODEL}\nquestion={QUESTION}\n")
try:
    r = httpx.post(URL, headers=headers, params=params, json=payload, timeout=60.0)
    print(f"HTTP Status: {r.status_code}")
    try:
        data = r.json()
    except Exception:
        print(r.text[:800])
        sys.exit(1)
    print("\n--- 完整响应 ---")
    print(json.dumps(data, ensure_ascii=False, indent=2))

    choice = data.get("choices", [{}])[0]
    msg = choice.get("message", {})
    finish = choice.get("finish_reason", "")

    print("\n=== 判断结果 ===")
    body_code = data.get("code")
    body_msg = str(data.get("message", ""))
    if body_code in (1002, 30001) or "not having this ability" in body_msg:
        print("[FAIL] 权限未开通：AppKey 不支持 tools/Function Calling")
    elif msg.get("tool_calls"):
        print("[OK] 支持标准 OpenAI tools：返回了 tool_calls")
        tc = msg["tool_calls"][0]
        fn = tc.get("function", {})
        print(f"   调用函数：{fn.get('name')}")
        print(f"   参数：{fn.get('arguments', '')[:200]}")
    elif finish == "tool_calls":
        print("[OK] 支持标准 OpenAI tools：finish_reason=tool_calls")
    elif "<APIs>" in str(msg.get("content", "")):
        print("[OK] 支持 <APIs> 标签模式：模型输出了结构化标签")
    else:
        print("[WARN] 请求未报错，但模型未触发工具（可能问题不需要调用，或模型不支持）")
        print(f"   finish_reason={finish}，content 前80字：{str(msg.get('content', ''))[:80]}")
except Exception as e:
    print(f"[FAIL] 请求异常：{e}")
    sys.exit(1)
