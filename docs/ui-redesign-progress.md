# 暖桥 UI 改版进度

> 按《02-暖桥-页面结构调整实施规划书-Agent详细版.md》串行实施。

## 总览

| 阶段 | 名称 | 状态 | 完成日期 |
|---|---|---|---|
| 0 | 仓库侦察与实施清单 | ✅ 完成 | 2026-06-11 |
| 1 | 建立改版基础设施 | ✅ 完成 | 2026-06-11 |
| 2 | 导航文案与系统栏统一 | ✅ 完成 | 2026-06-11 |
| 3 | 新增可复用页面结构组件 | ✅ 完成 | 2026-06-11 |
| 4 | 重构家长首页 | ✅ 完成 | 2026-06-11 |
| 5 | 重构孩子首页 | ✅ 完成 | 2026-06-11 |
| 6 | 调整今日关注页 | ✅ 完成 | 2026-06-11 |
| 7 | 重构详情页与 ExplainPanel | ✅ 完成 | 2026-06-11 |
| 8 | 重构我的页 | ✅ 完成 | 2026-06-11 |
| 9 | 统一分享、提醒、识图和快解析 | ✅ 完成 | 2026-06-11 |
| 10 | 通俗视频页面轻量统一 | ✅ 完成 | 2026-06-11 |
| 11 | 统一状态组件与错误处理 | ✅ 完成 | 2026-06-11 |
| 12 | 资源、LOGO 与视觉收口 | ✅ 完成 | 2026-06-11 |
| 13 | 无障碍与大字体验证 | ✅ 完成 | 2026-06-11 |
| 14 | 全量回归与最终交付 | ✅ 完成 | 2026-06-11 |

---

## 阶段 0：仓库侦察与实施清单

### 目标

建立真实工程地图，确认构建基线，不修改业务代码。

### 产出

- [x] `docs/ui-redesign-code-map.md`
- [x] `docs/ui-redesign-before/README.md`（截图清单）
- [x] 构建基线记录（见下）
- [x] 文档与代码差异记录（见 code-map）
- [x] 未修改业务代码

### 构建基线

| 项 | 值 |
|---|---|
| 命令 | `gradle :app:assembleDebug`（在 `android/` 目录；仓库无 `gradlew.bat`） |
| 耗时 | ~3m 10s |
| 结果 | **BUILD SUCCESSFUL** |
| 警告 | `WarmBridgeSystemBars.kt`：Accompanist SystemUiController 已 deprecated |
| 警告 | `android.overridePathCheck=true` experimental |
| 警告 | stripDebugDebugSymbols 跳过部分 native lib |

### 验收结论

- [x] 通过，进入阶段 1

---

## 阶段 1：建立改版基础设施

### 修改文件

- `android/app/src/main/res/values/strings.xml` — 新增改版文案
- `android/app/src/main/java/.../ui/theme/WbDimens.kt` — `compactCardRadius`、`priorityCardMinHeight`
- `android/app/src/main/java/.../ui/preview/WarmBridgePreviewData.kt` — Preview 静态数据

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL** (~7s)

### 验收结论

- [x] 通过，进入阶段 2

---

## 阶段 2：导航文案与系统栏统一

### 主要改动

- 底栏 / 热点标题文案：**今日热点 → 今日关注**
- `ParentMainShell` / `ChildMainShell` 接入 `warmNavBarItemColors()`
- `WarmBridgeRoot` 接入 `WarmBridgeSystemBars`
- 二级页 TopBar 统一为 `WarmTopAppBar`（Share / Reminder / ImageExplain / VideoQuick / Detail / VideoPopular）

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL**

### 验收结论

- [x] 通过，进入阶段 3

---

## 阶段 3：新增可复用页面结构组件

### 新增文件

- `WarmSectionHeader.kt`
- `WarmPriorityCard.kt`
- `WarmContinueCard.kt`
- `WarmCompactToolRow.kt`
- `WarmCuratedContentList.kt`
- `WarmFamilyStatusCard.kt`
- `WarmHistoryEntry.kt`
- `WarmStatusBanner.kt`

### 验收结论

- [x] 各组件含 @Preview；无网络 / 无 NavController
- [x] 构建成功；尚未替换业务页面（阶段 4 起使用）

---

## 阶段 4：重构家长首页

### 新增 / 修改

- **新增** `ParentHomeViewModel.kt` — 并行加载 child / trend / tag feed + 视频任务
- **重写** `ParentHomeScreen.kt` — 紧凑问候、今日重点、继续上次/提醒、两工具、精选、查看更多
- **修改** `ParentMainShell.kt` — 注入 ViewModel、`onOpenDetail` / `onOpenVideoJob`

### 页面结构（已实现）

1. 紧凑问候（120dp，太阳静止）
2. 兴趣摘要一行 → 跳转今日关注 Tab
3. 今日重点 `WarmPriorityCard`（孩子推荐 > 年轻人话题 > 暖桥日报）
4. 继续上次（视频任务）或提醒入口
5. 常用工具（识图 + 快解析）
6. 精选 2～3 条 + 查看更多

### 状态

- Loading / Content / Empty / Error + 重试

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL**

### 验收结论

- [x] 通过，可进入阶段 5

---

## 阶段 5：重构孩子首页

### 新增 / 修改

- **新增** `DemoShareStatus.kt` — 明确标注的 Demo 家庭状态
- **新增** `ChildShareLocalStore.kt` — DataStore 保存最近一次成功分享
- **新增** `SharePrefillHolder.kt` — 推荐内容跳转分享页预填
- **新增** `ChildHomeViewModel.kt` — 加载年轻人话题推荐 + 监听本机最近分享
- **重写** `ChildHomeScreen.kt` — 分享中心布局
- **修改** `ChildMainShell.kt`、`ShareScreen.kt` — ViewModel 注入、预填与落库

### 页面结构（已实现）

1. 问候「嗨，子女账号」+ 副标题
2. `WarmFamilyStatusCard`（演示状态，非真实已读）
3. 唯一主 CTA「分享给父母」
4. 最近分享（本机 DataStore；无记录时空态文案）
5. 推荐给父母 1～2 条 + 「分享」预填跳转
6. 轻量工具：温情提醒 + 图片识梗（视频快解析仍可通过 `WbRoutes.VideoQuick` 路由访问）

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL**

### 验收结论

- [x] 通过，可进入阶段 6

---

## 阶段 6：调整今日关注页

### 主要改动

- **HotTopicsTabScreen** — 头图 220dp → 112dp；标题改为「今日关注」+ 副标题
- **FeedListContent** — 首条 `WarmPriorityCard` + 其余 `WarmFeedCard`；错误/空态统一 `WarmEmptyState` + 重试
- **WarmFeedCard** — 增加类型标签、来源、阅读时间、孩子推荐标记；降低 elevation
- **util/FeedDisplay.kt** — 抽取 `estimateReadingTime`（家长首页复用）

### 保留能力

- 家长三频道 / 孩子两频道 + SegmentedControl
- 孩子端「按兴趣」标签编辑区
- 加载骨架 `FeedLoadingShimmer`
- 空态插画 `ILL_EMPTY_FEED`

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL**

### 验收结论

- [x] 通过，可进入阶段 7

---

## 阶段 7：重构详情页与 ExplainPanel

### 主要改动

- **ExplainPanel** 按理解顺序重组为可折叠区块：
  1. 这件事是什么（`plain_summary`）
  2. 与您有关吗（通用提示，不虚构结论）
  3. 建议怎么做（查看原文或询问家人）
  4. 听这段摘要（TTS，失败软提示）
  5. 词语解释（过长默认折叠）
  6. 您可能还想问（Chip 追问）
  7. 背景小知识（默认折叠）
  8. 说明与来源（默认折叠）
  9. 自由追问 + 追问记录（超过 3 条默认折叠）
  10. 通俗视频入口（移至追问区之后）

- **离线解读**：`from_llm = false` 时显示 Info 横幅，非错误红屏
- **错误态**：`WarmStatusBanner` 替代纯红字
- **DetailScreen**：文案迁入 `strings.xml`，内边距对齐 `WbDimens`

### 未改动的高风险逻辑

- explain / tts / 追问 API 与缓存行为
- 识图/快解析自动解读条件
- 追问不替换顶部摘要，历史堆叠不变

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL**

### 验收结论

- [x] 通过，可进入阶段 8

---

## 阶段 8：重构我的页

### 页面顺序（已实现）

1. **资料** — 头像、名称、角色、演示标识、切换身份
2. **任务与提醒** — 通俗视频任务（加载/列表/空态/失败摘要 + 重试）、温情提醒入口
3. **历史与收藏** — 浏览历史 / 我的收藏 / 我的提问（`WarmHistoryEntry` + 空态对话框）
4. **更多设置** — 提醒、帮助、关于、隐私、切换身份

### 主要改动

- 任务区块优先于设置区块
- 失败任务展示 `error_message` 摘要
- 任务加载失败使用 `WarmStatusBanner` + 重试
- 复用 `WarmSectionHeader`、`WarmHistoryEntry`

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL**

### 验收结论

- [x] 通过，可进入阶段 9

---

## 阶段 9：统一分享、提醒、识图和快解析

### 新增 / 修改

- **新增** `WarmToolScreenScaffold.kt` — TopBar → 说明 → 状态 → 输入区 → 底栏主按钮 + 页脚提示
- **重写** `ShareScreen.kt` — 接入 scaffold；`WarmStatusBanner` 成功/失败反馈；文案迁入 `strings.xml`
- **重写** `ReminderScreen.kt` — 接入 scaffold；`reminder_demo_local_only` Info 横幅 + 定时结果横幅
- **重写** `ImageExplainScreen.kt` — 底栏「上传并讲解」；统一 loading / 错误态
- **重写** `VideoQuickScreen.kt` — 底栏「解析并生成解读」；`video_quick_result_hint` 页脚；统一 loading / 错误态
- **修改** `strings.xml` — `share_*`、`reminder_*`、`image_*`、`video_quick_*` 等工具页文案

### 统一结构（四页一致）

1. `WarmTopAppBar` 标题 + 返回/关闭
2. 页面说明（intro）
3. 状态区（loading / success / error / demo 提示）
4. 输入区（文本框 / 选图 / 外链按钮）
5. 底栏固定主按钮（`WarmPrimaryButton`）
6. 页脚辅助说明（legal / demo / 结果提示）

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL** (~6s)

### 验收结论

- [x] 通过，可进入阶段 10

---

## 阶段 10：通俗视频页面轻量统一

### 主要改动

- **VideoPopularScreen** 文案迁入 `strings.xml`（`video_popular_*`）
- 生成遮罩改用 `WarmSectionCard` + `WarmStatusBanner` 统一进度/错误/中断样式
- 状态映射：`running` 步骤+进度条；`failed` 错误摘要+重试；`interrupted` 警告+重新生成
- 完成页：`WarmPrimaryButton` 分享视频 + `OutlinedButton` 分享链接/保存相册
- `ExplainPanel` 嵌入 `WarmSectionCard`（「文字解读与追问」）
- 轮询终止条件补充 `interrupted`（不改 2s 间隔、Worker、release 等行为）

### 未改动

- `prepare → analyze → media → merge → done` 流水线
- WorkManager 通知、下载、保存相册、VideoView 播放、离开 release

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL** (~3s)

### 验收结论

- [x] 通过，可进入阶段 11

---

## 阶段 11：统一状态组件与错误处理

### 新增组件

- **WarmLoadingContent** — 统一 loading 转圈 + 文案（compact / centered / minHeight）
- **WarmRetryState** — `WarmStatusBanner` 错误说明 + 重试按钮
- **WarmTaskStatusChip** / **WarmPopularVideoStatusChip** — 任务状态色块（running / done / failed / interrupted）

### 页面迁移

| 页面 | 改动 |
|---|---|
| ParentHomeScreen / ChildHomeScreen | Loading → `WarmLoadingContent`；Error → `WarmRetryState` |
| FeedListContent | 网络错误 → `WarmRetryState`（空态仍用 `WarmEmptyState`） |
| DetailScreen | 加载失败 → `WarmRetryState` + reload；其余错误 → `WarmStatusBanner` |
| MineScreen | 任务加载 → `WarmLoadingContent`；任务列表错误 → `WarmRetryState`；状态 → `WarmPopularVideoStatusChip` |
| ImageExplainScreen / VideoQuickScreen | loading 行 → `WarmLoadingContent` |
| VideoPopularScreen | 视频加载 / 生成进度 → `WarmLoadingContent` |
| ExplainPanel | 解读/追问 loading → `WarmLoadingContent`；TTS 软提示 → Info `WarmStatusBanner`；离线解读保持 Info 横幅 |

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL** (~7s)

### 验收结论

- [x] 状态样式统一；重试可用；离线/TTS 非红屏错误
- [x] 通过，可进入阶段 12

---

## 阶段 12：资源、LOGO 与视觉收口

### LOGO / Launcher

- **ic_launcher_foreground.xml** 重绘：两人 + 绿色拱桥（主视觉）+ 弱化浅色太阳
- **AndroidManifest** 改为 `@mipmap/ic_launcher` / `ic_launcher_round`（API 26+ adaptive；低版本 layer-list 回退）
- 新增 `res/mipmap/ic_launcher.xml` 与 `ic_launcher_round.xml` 作 legacy 合成

### 资源路径集中（WbAssetPhotos）

扩展常量并文档化：`PARENT_HEADER_DECORATION`、`CHILD_HOME_HEADER`、`HOT_TOPICS_HEADER`、`MINE_HEADER`、`ILL_ERROR_NETWORK`、`EMPTY_FEED_ILLUSTRATION` 等

### AssetPhoto 增强

- `showPlaceholder = false`：缺图时不显示灰块（装饰图静默跳过）
- **AssetPhotoFirstAvailable**：多文件名任一命中即显示

### 页面接入

- 头区渐变支持可选装饰：`WarmHeaderGradientBackground(decorationAsset, decorationAlpha)`
- 家长/孩子/今日关注头区已挂接对应装饰路径（有图才显示）
- 家长首页底部 `PARENT_HOME_WATERMARK`（α=0.1）
- 网络错误 `WarmRetryState` 支持 `error_network_illustration.png`
- 空列表支持 `ill_empty_feed` / `empty_feed_illustration` 双文件名

### 文档

- 更新 `assets/photos/README.txt` 完整清单

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL** (~3s)

### 验收结论

- [x] 桥比太阳更突出；路径集中；缺图不崩溃；无新增 drawable 大图
- [x] 通过，可进入阶段 13

---

## 阶段 13：无障碍与大字体验证

### 字号与触控

- `labelMedium` 14sp → **16sp**；`WarmPrimaryButton` 默认 `heightIn(min = 56dp)`
- `WbDimens.minTouchTarget = 48dp`；分段条 / 兴趣 Chip 触控区对齐
- 问候标题改用 `typography` 层级，移除硬编码 `fontSize`

### 语义（TalkBack）

- 新增 `cd_back` / `cd_close` / `cd_forward` / `cd_pick_image` 等
- `WarmStatusBanner` 合并朗读完整文案；状态图标+文字双通道
- `WarmTopAppBar`、`WarmSectionCard`、我的页箭头等接入 `strings.xml`

### 大字倍率

- `WarmPriorityCard` / `WarmFeedCard` 移除摘要行数裁切，卡片随内容增高
- 分段条标签允许两行换行

### 对比度

- 关键阅读区次要文案改用 `onSurfaceVariant`

### 产出

- `docs/ui-redesign-a11y-checklist.md`（手动验证步骤）

### 编译

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL** (~5s)

### 验收结论

- [x] 代码侧指标达标；设备 1.3×/1.5× 与 TalkBack 见 checklist 手动项
- [x] 通过，可进入阶段 14

---

## 阶段 14：全量回归与最终交付

### 构建

| 命令 | 结果 |
|---|---|
| `gradle :app:assembleDebug` | ✅ BUILD SUCCESSFUL |
| `gradle :app:testDebugUnitTest` | ✅ NO-SOURCE（无单测） |
| `gradle :app:lintDebug` | ❌ 3 Error（改版前既有，见 known-issues） |

### 交付文档

- [x] `docs/ui-redesign-code-map.md`（更新至阶段 14）
- [x] `docs/ui-redesign-progress.md`（本文，阶段 0～14 完成）
- [x] `docs/ui-redesign-regression.md`
- [x] `docs/ui-redesign-known-issues.md`
- [x] `docs/ui-redesign-a11y-checklist.md`
- [x] `docs/ui-redesign-before/README.md`
- [x] `docs/ui-redesign-after/README.md`（截图待人工补全）

### 验收结论

- [x] 改版目标（14.5）代码侧达标；真机走查见 regression §6
- [x] **UI 改版规划（阶段 0～14）全部完成**

---
