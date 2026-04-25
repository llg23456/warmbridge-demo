"""从整段口令/分享文案中提取首个 http(s) 链接。"""

from __future__ import annotations

import re

_URL_RE = re.compile(r"https?://[^\s\"'<>()\[\]］】]+", re.IGNORECASE)

_TRAIL = set(")）.,，。;；:：」』'\"］】]}")

def extract_first_url(blob: str) -> str:
    m = _URL_RE.search(blob or "")
    if not m:
        return ""
    u = m.group(0)
    while u and u[-1] in _TRAIL:
        u = u[:-1]
    return u.strip()
