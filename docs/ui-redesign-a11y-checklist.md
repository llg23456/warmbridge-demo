# 暖桥 UI 改版 · 无障碍与大字体验证清单

> 阶段 13 产出。指标见 `WbAccessibility` / `WbDimens` / `WbTypography`。

## 字号与触控

| 项 | 目标 | 实现 |
|---|---|---|
| 正文最小 | ≥16sp | `bodyMedium` = 16sp；`labelMedium` 提升至 16sp |
| 推荐正文 | 18sp | `bodyLarge` = 18sp |
| 主按钮高度 | ≥56dp | `WarmPrimaryButton` 默认 `heightIn(min = touchMin)` |
| 普通点击区 | ≥48dp | `WbDimens.minTouchTarget`；分段条 / 兴趣 Chip 已对齐 |

## 语义与 TalkBack

| 项 | 说明 |
|---|---|
| 导航图标 | `cd_nav_*`、`cd_back`、`cd_close`、`cd_forward` 在 `strings.xml` |
| 状态横幅 | `WarmStatusBanner` 合并语义为完整文案（图标装饰性 null） |
| 装饰插画 | `AssetPhoto` / 空态图 `contentDescription = null` |
| 折叠区块 | `detail_section_expand` / `detail_section_collapse` |

## 大字倍率（系统 Font Scale）

- 标题改用 `MaterialTheme.typography`，避免硬编码 `sp` 绕过缩放
- `WarmPriorityCard` / `WarmFeedCard` 移除摘要 `maxLines` 裁切，卡片 `heightIn(min=…)` 随内容增高
- 分段条标签允许 `maxLines = 2` 换行

## 对比度

- 次要文案优先 `onSurfaceVariant`，替代 `#666` 硬编码 `WbTextMuted`（在关键阅读区）

## 手动验证（设备 / 模拟器）

1. **设置 → 显示 → 字体大小**：1.3×、1.5× 浏览选角、家长首页、今日关注、详情、分享、我的
2. **TalkBack**：底栏三 Tab、TopBar 返回、主按钮、任务状态 Chip（文字+颜色双通道）
3. **横屏**：各重点页不崩溃（布局可滚动即可）
4. **键盘**：分享/提醒/追问输入框 `adjustResize`（`MainActivity` 已配置）

## 重点页面覆盖

- [x] 选角 `RoleSelectScreen`
- [x] 家长首页 `ParentHomeScreen`
- [x] 孩子首页 `ChildHomeScreen`
- [x] 今日关注 `HotTopicsTabScreen` + `FeedListContent`
- [x] 详情 `DetailScreen` + `ExplainPanel`
- [x] 分享 / 提醒 / 识图 / 快解析（工具页 scaffold）
- [x] 我的 `MineScreen`
