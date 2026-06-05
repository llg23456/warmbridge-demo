"""文生图 / 图生视频提示词：敏感内容净化与安全降级。"""

from __future__ import annotations

import re

# 英文：可识别公众人物 / 敏感身份（小写匹配后剔除或泛化）
_SENSITIVE_EN = (
    "celebrity",
    "celebrities",
    "politician",
    "president",
    "prime minister",
    "dictator",
    "protest",
    "riot",
    "war zone",
    "blood",
    "gore",
    "nude",
    "naked",
    "nsfw",
    "weapon",
    "gun",
    "knife fight",
)

# 替换为安全泛化描述
_PERSON_REPLACEMENTS = (
    (re.compile(r"\b(close[- ]?up of (a )?)(person|people|man|woman|child|boy|girl)\b", re.I), "distant soft silhouette"),
    (re.compile(r"\b(portrait of|face of)\s+[\w\s]{2,30}\b", re.I), "abstract soft background"),
    (re.compile(r"\b(real person|recognizable face|famous actor|pop star)\b", re.I), "generic scenery"),
)


def sanitize_visual_prompt(prompt: str, *, topic: str = "") -> str:
    """
    去掉高风险词、把人物特写泛化为远景/剪影，供内容策略拦截后的二次尝试。
    不保证通过审核，但比直接丢弃原 prompt 更贴近主题。
    """
    p = (prompt or "").strip()
    if not p:
        return p

    for pat, repl in _PERSON_REPLACEMENTS:
        p = pat.sub(repl, p)

    low = p.lower()
    for word in _SENSITIVE_EN:
        if word in low:
            p = re.sub(re.escape(word), "", p, flags=re.I)

    p = re.sub(r"\s{2,}", " ", p).strip(" ,;")
    t = (topic or "").strip()[:40]
    if t and t.lower() not in p.lower():
        p = f"{p}, family-friendly documentary mood inspired by {t}"
    suffix = (
        ", no text, no logos, no recognizable celebrities, no violence, "
        "widescreen 16:9 landscape, cinematic realism"
    )
    if "16:9" not in p.lower():
        p = f"{p}{suffix}"
    return p[:780]


def safe_video_intro_prompt(topic: str = "") -> str:
    t = (topic or "daily life").strip()[:40] or "daily life"
    return (
        f"Gentle slow cinematic pan over a cozy home interior inspired by {t}, "
        f"warm natural light, soft focus, family-friendly documentary style "
        f"--dur 5 --ratio 16:9"
    )
