"""通俗视频生成报告：口播稿 + 提示词 + 检索材料 + 诊断 → Markdown。"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

from app.config import settings
from app.services.popular_video_analyze import ANALYZE_SYSTEM, VideoAnalyzeResult, build_analyze_user_content

_log = logging.getLogger(__name__)

_DATA_DIR = Path(__file__).resolve().parent.parent.parent / "data" / "popular_videos"
_REPORTS_DIR = _DATA_DIR / "reports"


@dataclass
class PrepareDiag:
    """prepare 阶段材料采集情况（写入报告 §0）。"""

    cover_url: str = ""
    cover_download_ok: bool = False
    used_placeholder_cover: bool = False
    cover_source: str = ""  # og | bing_image | placeholder
    bing_image_keyword: str = ""
    skip_page_material: bool = False
    page_description: str = ""
    page_text: str = ""
    cover_ocr: str = ""
    stored_web_context_len: int = 0

    def cover_status(self) -> tuple[str, str]:
        if self.cover_download_ok:
            if self.cover_source == "bing_image":
                kw = self.bing_image_keyword or "（关键词）"
                return "✅ Bing 图", f"关键词「{kw}」从 Bing 图片搜索抓取（免费，不耗 AI 额度）"
            return "✅ 已下载", "已从 og/预览 URL 拉取封面图"
        if self.used_placeholder_cover:
            if self.bing_image_keyword:
                return "❌ 占位图", f"Bing 图片搜索「{self.bing_image_keyword}」未命中，已用橙色占位图"
            url = (self.cover_url or "").strip()
            if not url:
                return "❌ 占位图", "og 封面为空且 Bing 图未命中，已用橙色占位图"
            return "❌ 占位图", f"封面 URL 下载失败，已用橙色占位图；URL={url[:80]}"
        return "❌ 无", "封面未就绪"

    def og_status(self) -> tuple[str, str]:
        desc = (self.page_description or "").strip()
        if desc:
            return "✅ 有", f"{len(desc)} 字"
        if self.skip_page_material:
            return "⏭ 不适用", "口令为 APP 深度链接，无 og 简介（正常，以联网检索为准）"
        return "⏭ 无", "未解析到 og:description（可忽略，以联网检索为准）"

    def page_text_status(self) -> tuple[str, str]:
        t = (self.page_text or "").strip()
        if t:
            return "✅ 有", f"{len(t)} 字"
        if self.skip_page_material:
            return "⏭ 不适用", "口令页无 HTML 正文，不抓取（正常，以联网检索为准）"
        return "⏭ 无", "HTML 正文为空（可忽略，以联网检索为准）"

    def ocr_status(self) -> tuple[str, str]:
        t = (self.cover_ocr or "").strip()
        if self.cover_source == "bing_image":
            return "⏭ 跳过", "Bing 配图无字幕，不做 OCR（正常）"
        if t:
            return "✅ 有", f"{len(t)} 字"
        if self.used_placeholder_cover:
            return "⏭ 跳过", "占位图无文字，未做 OCR"
        if not self.cover_download_ok:
            return "⏭ 跳过", "封面未下载成功"
        return "❌ 无", "封面 OCR 已调用但未识别到有效文字"


@dataclass
class WebSearchDiag:
    """联网检索诊断。"""

    enabled: bool = True
    keywords_tried: list[str] = field(default_factory=list)
    attempt_log: list[str] = field(default_factory=list)
    fresh_len: int = 0
    stored_len: int = 0
    merged_len: int = 0
    empty_reason: str = ""

    def status(self) -> tuple[str, str]:
        if not self.enabled:
            return "⏭ 关闭", "WEB_SEARCH_ENABLED=false，未发起检索"
        if self.fresh_len > 0:
            return "✅ 有", f"刷新 {self.fresh_len} 字；关键词 {self.keywords_tried[:3]}"
        if self.stored_len > 0 and self.merged_len > 0:
            return "⚠ 仅入库旧摘要", f"本次 DDG HTML/Bing 等无新结果；沿用分享时摘要 {self.stored_len} 字"
        reason = self.empty_reason or "未知"
        kws = "、".join(self.keywords_tried[:5]) or "（未能提取有效词）"
        return "❌ 无", f"关键词 [{kws}]；{reason}"


def report_path(job_id: str) -> Path:
    _REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    return _REPORTS_DIR / f"{job_id}.md"


def _md_table_row(name: str, status: str, note: str) -> str:
    note = (note or "").replace("|", "｜").replace("\n", " ")
    return f"| {name} | {status} | {note} |"


def write_generation_report(
    job_id: str,
    *,
    item_title: str = "",
    item_url: str = "",
    item_tag: str = "",
    analyze: VideoAnalyzeResult | None,
    fresh_web: str = "",
    user_materials: dict[str, str] | None = None,
    prepare: PrepareDiag | None = None,
    web_diag: WebSearchDiag | None = None,
    slides_count: int = 0,
    intro_generated: bool = False,
    merge_mode: str = "",
    job_status: str = "done",
    video_url: str = "",
) -> Path:
    path = report_path(job_id)
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    mats = user_materials or {}
    a = analyze or VideoAnalyzeResult()
    prep = prepare or PrepareDiag()
    wd = web_diag or WebSearchDiag()

    user_block = build_analyze_user_content(
        title=mats.get("title", item_title),
        source=mats.get("source", ""),
        summary=mats.get("summary", ""),
        share_keywords=mats.get("share_keywords", ""),
        page_description=mats.get("page_description", ""),
        page_text=mats.get("page_text", ""),
        cover_ocr_text=mats.get("cover_ocr", ""),
        web_context=fresh_web,
        tag=item_tag,
        material_diag=prep,
        web_diag=wd,
    )

    img_lines = "\n".join(f"{i + 1}. `{p}`" for i, p in enumerate(a.image_prompts[:3])) or "（无）"

    c_st, c_note = prep.cover_status()
    og_st, og_note = prep.og_status()
    pt_st, pt_note = prep.page_text_status()
    ocr_st, ocr_note = prep.ocr_status()
    web_st, web_note = wd.status()

    attempt_lines = "\n".join(f"- {line}" for line in wd.attempt_log[:12]) or "- （无逐条记录）"

    body = f"""# 通俗视频生成报告 · `{job_id}`

> 生成时间：{now}  
> 任务状态：**{job_status}**  
> 成片：`{video_url or '（未完成）'}`  
> 合成模式：{merge_mode or '—'} · 轮播图 {slides_count} 张 · D3 片头 {'是' if intro_generated else '否'}

---

## 0. 诊断摘要（先看这里）

> 说明：**❌ 无** 不等于程序 bug；抖音口令页 og/正文为 **⏭ 不适用** 属正常，以联网检索为准。  
> 口播视角：旁白面向**长辈**，用「您 / 推送给您」；**禁止**「推给我 / 给我」。

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| 封面图 | {c_st} | {c_note} |
| 页面简介 og | {og_st} | {og_note} |
| 页面正文摘录 | {pt_st} | {pt_note} |
| 封面 OCR | {ocr_st} | {ocr_note} |
| 联网检索 | {web_st} | {web_note} |
| 口播视角 | {'⚠ 请检查' if _narration_needs_perspective_fix(a.narration) else '✅ 正常'} | 面向长辈「您」；见 §2 |

### 联网检索逐条记录

{attempt_lines}

---

## 1. 条目信息

| 字段 | 内容 |
| --- | --- |
| 标题 | {item_title or '—'} |
| 标签 | {item_tag or '—'} |
| 原链接 | {item_url or '—'} |

---

## 2. 口播稿（可直接改这里后重新 TTS/合成）

**核心词**：`{a.core_keyword or '—'}`  
**来源**：{'蓝心 LLM' if a.from_llm else '本地兜底'} · 约 {len(a.narration)} 字  
**视角要求**：说给**长辈听** → 「孩子给您推的…」「分享给您…」；勿用「推给我 / 给我」。

```
{a.narration or '（无）'}
```

---

## 3. 画面提示词（英文）

### 3.1 图生视频片头 `video_prompt`

```
{a.video_prompt or '（无）'}
```

### 3.2 文生图轮播 `image_prompts`

{img_lines}

---

## 4. 发给蓝心的 Prompt（改 SYSTEM / USER 可对照修改）

### 4.1 System（`popular_video_analyze.ANALYZE_SYSTEM`）

```
{ANALYZE_SYSTEM}
```

### 4.2 User（本次任务实际材料 + 字段状态说明）

```
{user_block}
```

---

## 5. 联网检索材料（DuckDuckGo HTML / Bing 等）

```
{fresh_web or '（无有效检索结果，见 §0 诊断）'}
```

---

## 6. 原始输入摘录（带长度）

| 块 | 状态 | 内容 |
| --- | --- | --- |
| 列表摘要/推荐语 | {len(mats.get('summary') or '')} 字 | {(mats.get('summary') or '—')[:500]} |
| 口令关键词 | — | {mats.get('share_keywords') or '—'} |
| 页面简介 | {len(mats.get('page_description') or '')} 字 | {(mats.get('page_description') or '—')[:800]} |
| 页面正文 | {len(mats.get('page_text') or '')} 字 | {(mats.get('page_text') or '—')[:1500]} |
| 封面 OCR | {len(mats.get('cover_ocr') or '')} 字 | {(mats.get('cover_ocr') or '—')[:500]} |
| 分享入库 web | {prep.stored_web_context_len} 字 | {'有' if prep.stored_web_context_len else '无'} |

---

## 7. 修改说明

- 改 **§2 口播稿** 后：需重新跑 TTS + merge（或整条通俗视频任务重生成）。
- 改 **§3 提示词** 后：需重新 media + merge。
- **§0 诊断** 供新对话/排查用：材料缺啥、检索为何空，一目了然。
- 本文件：`{path.resolve()}`

"""

    path.write_text(body, encoding="utf-8")
    _log.info("WbVideoGen report saved %s", path)
    return path


def _narration_needs_perspective_fix(n: str) -> bool:
    bad = ("推给我", "给我推", "孩子给我", "分享给我", "发给我", "告诉我个")
    return any(x in (n or "") for x in bad)
