# 暖桥 UI 改版 · 全量回归记录

> 阶段 14 产出 · 2026-06-11  
> 基线分支：当前工作区（阶段 0～13 已完成）

## 1. 构建与静态检查

| 命令 | 结果 | 说明 |
|---|---|---|
| `gradle :app:assembleDebug`（`android/`） | ✅ **BUILD SUCCESSFUL** | 各阶段末均通过 |
| `gradle :app:testDebugUnitTest` | ✅ NO-SOURCE | 工程无 `src/test` 单测 |
| `gradle :app:lintDebug` | ❌ **失败** | 3 个 Error（改版前即存在，见 [known-issues](./ui-redesign-known-issues.md)） |

### 后端联调（需本地启动）

```bash
cd server
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

| 检查项 | Agent 环境 | 说明 |
|---|---|---|
| `GET /health` | ⏳ 未运行 | 交付前请在联调机验证 |
| `GET /api/tags` | ⏳ 未运行 | 家长首页兴趣标签依赖 |

Android：`local.properties` → `warmbridge.api.baseUrl`（模拟器常用 `http://10.0.2.2:8000/`）。

---

## 2. 改版目标验收（14.5）

| 标准 | 状态 | 依据 |
|---|---|---|
| 所有现有功能仍可访问 | ✅ 代码 | 路由 `WbRoutes` / `WarmBridgeRoot` 未删减能力路径 |
| 家长首页不再是功能墙 | ✅ | `ParentHomeScreen` 任务驱动：重点卡 + 工具 + 精选 |
| 孩子首页分享主任务明确 | ✅ | 主 CTA「分享给父母」+ 最近分享 |
| 今日关注内容更精选 | ✅ | 首条 `WarmPriorityCard` + 列表 `WarmFeedCard` |
| ExplainPanel 理解顺序清晰 | ✅ | 可折叠区块 + 离线 Info 横幅 |
| 我的页以任务与历史为主 | ✅ | 资料 → 任务与提醒 → 历史 → 设置 |
| 二级工具页统一 | ✅ | `WarmToolScreenScaffold` 四页一致 |
| 通俗视频流程无回归 | ✅ 代码 | 流水线 / 轮询 / Worker / release 逻辑未改 |
| Reminder 不误导跨设备 | ✅ | `reminder_demo_local_only` 横幅 |
| 大字体可用 | ✅ 代码 | 阶段 13 + [a11y-checklist](./ui-redesign-a11y-checklist.md) |
| 构建成功 | ✅ | `assembleDebug` |
| 无阻塞崩溃 | ⏳ 真机 | 需安装 APK 走查 |
| 文档完整 | ✅ | 见 §4 |

---

## 3. 功能回归矩阵

图例：**✅** 代码/构建已验收 · **⏳** 需真机+后端 · **—** 不适用

### 3.1 角色与导航

| 用例 | 状态 | 备注 |
|---|---|---|
| 选家长 | ⏳ | `RoleSelectScreen` → `parent_home` |
| 选孩子 | ⏳ | → `child_home` |
| 切换身份 | ⏳ | 我的页 → 清栈回选角 |
| 三 Tab | ✅ | `ParentMainShell` / `ChildMainShell` |
| Tab 状态恢复 | ⏳ | `rememberSaveable` 频道索引 |
| 系统返回 | ⏳ | 二级页 `WarmTopAppBar` |
| 二级页无底栏 | ✅ | 详情/分享/工具/视频无 `bottomBar` |

### 3.2 家长

| 用例 | 状态 | 备注 |
|---|---|---|
| 首页重点内容 | ✅ | `ParentHomeViewModel` + `WarmPriorityCard` |
| 孩子推荐优先 | ⏳ | 需 feed 数据 |
| 常用工具 | ✅ | 识图 + 快解析 `WarmCompactToolRow` |
| 查看更多 | ✅ | 跳转今日关注 Tab |
| 今日关注三频道 | ✅ | 兴趣 / 孩子推荐 / 年轻人话题 |
| 兴趣同步 | ⏳ | `InterestTagsRepository` + Chip |

### 3.3 孩子

| 用例 | 状态 | 备注 |
|---|---|---|
| 分享主按钮 | ✅ | `WarmPrimaryButton` 居中 |
| 分享成功落库 | ⏳ | `ChildShareLocalStore` |
| 最近分享展示 | ⏳ | DataStore |
| 今日关注两频道 | ✅ | 无「孩子推荐」Segment |
| 不出现孩子推荐频道 | ✅ | `HotTopicsTabScreen(showChildChannel=false)` 两标签 |

### 3.4 详情与解读

| 用例 | 状态 | 备注 |
|---|---|---|
| item 加载 | ⏳ | `DetailScreen` + 重试 |
| 打开原文 | ⏳ | Custom Tabs |
| explain | ⏳ | API 未改 |
| TTS | ⏳ | 失败软提示 Info 横幅 |
| 建议追问 Chip | ⏳ | |
| 自由追问 | ⏳ | |
| 追问历史 | ⏳ | 折叠规则保留 |
| 识图原图 | ⏳ | `SessionCover` |
| 快解析自动 explain | ✅ | `autoExplainOnLoad` 条件未改 |

### 3.5 提醒

| 用例 | 状态 | 备注 |
|---|---|---|
| 通知权限 | ⏳ | Tiramisu `POST_NOTIFICATIONS` |
| WorkManager 定时 | ⏳ | `ReminderWorker` |
| 前台弹窗 | ⏳ | `ReminderInAppDialog` |
| Demo 限制文案 | ✅ | Info 横幅 + 页脚 |

### 3.6 工具页（分享 / 识图 / 快解析）

| 用例 | 状态 | 备注 |
|---|---|---|
| 统一 TopBar + 底栏主按钮 | ✅ | `WarmToolScreenScaffold` |
| 网络错误 + 重试 | ✅ | `WarmRetryState` / `WarmStatusBanner` |
| 分享预填 | ⏳ | `SharePrefillHolder` |

### 3.7 通俗视频

| 用例 | 状态 | 备注 |
|---|---|---|
| start / 2s 轮询 | ✅ 代码 | `LaunchedEffect` + `delay(2000)` |
| WorkManager 通知 | ⏳ | `PopularVideoPollWorker` |
| done 播放 / 保存 / 分享 | ⏳ | |
| 离开 release | ✅ 代码 | `DisposableEffect` |
| failed / interrupted + 重试 | ✅ UI | `PopularVideoProgressOverlay` |

### 3.8 无障碍（抽样）

| 用例 | 状态 | 文档 |
|---|---|---|
| Font Scale 1.3 / 1.5 | ⏳ 真机 | [a11y-checklist](./ui-redesign-a11y-checklist.md) |
| TalkBack 底栏/TopBar | ⏳ 真机 | `cd_*` 字符串已集中 |

---

## 4. 交付物清单

| 文件 | 状态 |
|---|---|
| `docs/ui-redesign-code-map.md` | ✅ 已更新 |
| `docs/ui-redesign-progress.md` | ✅ 阶段 0～14 完成 |
| `docs/ui-redesign-regression.md` | ✅ 本文 |
| `docs/ui-redesign-known-issues.md` | ✅ |
| `docs/ui-redesign-a11y-checklist.md` | ✅ 阶段 13 |
| `docs/ui-redesign-before/` | 📋 截图清单（改版前，需人工补图） |
| `docs/ui-redesign-after/` | 📋 截图清单（改版后，需人工补图） |

### 新组件 Preview（`@Preview`）

`WarmPriorityCard`、`WarmFeedCard`、`WarmStatusBanner`、`WarmRetryState`、`WarmLoadingContent`、`WarmTaskStatusChip`、`WarmSectionHeader`、`WarmFamilyStatusCard`、`WarmContinueCard`、`WarmCuratedContentList`、`WarmHistoryEntry`、`WarmCompactToolRow` 等。

---

## 5. 回滚说明

1. **Git**：在改版前打 tag 或记下 commit；回滚 `git checkout <commit> -- android/` 或整仓 `git revert` 系列提交。
2. **范围**：本次改版集中在 `android/app/src/main/java/.../ui/`、`res/values/strings.xml`、Launcher drawable、`assets/photos/README.txt`；后端与 `server/` **未改**。
3. **验证回滚**：`gradle :app:assembleDebug` + 对比 `docs/ui-redesign-before/` 截图。
4. **数据**：`InterestTagsRepository` / `ChildShareLocalStore` DataStore 键未变；卸载 App 可清本地状态。

---

## 6. 建议真机走查顺序（约 30 分钟）

1. 启动后端 → 安装 Debug APK  
2. 选角 → 家长首页 → 今日关注三频道 → 详情解读  
3. 切换孩子 → 分享 → 今日关注两频道  
4. 提醒 / 识图 / 快解析 各进一页  
5. 通俗视频（可选长流程）→ 我的任务列表  
6. 设置字体 1.5× → 重复 2～3 核心路径
