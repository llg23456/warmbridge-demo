# 暖桥 UI 改版 · 工程地图



> 最后更新：2026-06-11 · 阶段 14 最终交付  

> 基线：`warmbridge-demo/android` Kotlin + Jetpack Compose



## 模块索引



| 模块 | 符号 | 实际文件路径 | 依赖 / 状态来源 | 改版 |

|---|---|---|---|---|

| 根导航 | `WarmBridgeRoot` | `ui/WarmBridgeRoot.kt` | `NavController` | ✅ |

| 路由 | `WbRoutes` | `navigation/WbRoutes.kt` | — | 轻改 |

| 家长 Tab 壳 | `ParentMainShell` | `ui/shell/ParentMainShell.kt` | `ParentHomeViewModel`、`InterestTagsRepository` | ✅ |

| 孩子 Tab 壳 | `ChildMainShell` | `ui/shell/ChildMainShell.kt` | `ChildHomeViewModel` | ✅ |

| 家长首页 | `ParentHomeScreen` | `ui/screens/ParentHomeScreen.kt` | `ParentHomeViewModel` | ✅ |

| 孩子首页 | `ChildHomeScreen` | `ui/screens/ChildHomeScreen.kt` | `ChildHomeViewModel`、`ChildShareLocalStore` | ✅ |

| 今日关注 | `HotTopicsTabScreen` | `ui/screens/HotTopicsTabScreen.kt` | `FeedListContent` | ✅ |

| Feed 列表 | `FeedListContent` | `ui/screens/FeedListContent.kt` | `NetworkModule.api.feed()` | ✅ |

| 我的 | `MineScreen` | `ui/screens/MineScreen.kt` | `popularVideoJobs` | ✅ |

| 详情 | `DetailScreen` | `ui/screens/DetailScreen.kt` | item + `ExplainPanel` | ✅ |

| 解读面板 | `ExplainPanel` | `ui/screens/ExplainPanel.kt` | explain / tts | ✅ UI |

| 分享 | `ShareScreen` | `ui/screens/ShareScreen.kt` | `SharePrefillHolder`、share API | ✅ |

| 提醒 | `ReminderScreen` | `ui/screens/ReminderScreen.kt` | `ReminderWorker` | ✅ |

| 识图 | `ImageExplainScreen` | `ui/screens/ImageExplainScreen.kt` | OCR multipart | ✅ |

| 快解析 | `VideoQuickScreen` | `ui/screens/VideoQuickScreen.kt` | quickparse API | ✅ |

| 通俗视频 | `VideoPopularScreen` | `ui/screens/VideoPopularScreen.kt` | Job 轮询、Worker | ✅ UI |

| 选角 | `RoleSelectScreen` | `ui/screens/RoleSelectScreen.kt` | — | 文案 |

| 系统栏 | `WarmBridgeSystemBars` | `ui/theme/WarmBridgeSystemBars.kt` | 路由 | ✅ 接入 |



## ViewModel / 本地状态（改版新增）



| 符号 | 路径 | 说明 |

|---|---|---|

| `ParentHomeViewModel` | `ui/screens/ParentHomeViewModel.kt` | 家长首页并行加载 |

| `ChildHomeViewModel` | `ui/screens/ChildHomeViewModel.kt` | 孩子推荐 + 最近分享 |

| `ChildShareLocalStore` | `data/local/ChildShareLocalStore.kt` | 分享成功落库 |

| `SharePrefillHolder` | `data/local/SharePrefillHolder.kt` | 跳转分享预填 |

| `DemoShareStatus` | `data/local/DemoShareStatus.kt` | 演示家庭状态文案 |

| `InterestTagsRepository` | `data/local/InterestTagsRepository.kt` | 兴趣标签持久化 |



## 组件索引（改版新增 / 重点）



| 符号 | 路径 | 说明 |

|---|---|---|

| `WarmToolScreenScaffold` | `ui/components/WarmToolScreenScaffold.kt` | 二级工具页骨架 |

| `WarmSectionHeader` | `ui/components/WarmSectionHeader.kt` | 区块标题 |

| `WarmPriorityCard` | `ui/components/WarmPriorityCard.kt` | 今日重点 |

| `WarmContinueCard` | `ui/components/WarmContinueCard.kt` | 继续上次 |

| `WarmCompactToolRow` | `ui/components/WarmCompactToolRow.kt` | 紧凑工具行 |

| `WarmCuratedContentList` | `ui/components/WarmCuratedContentList.kt` | 精选列表 |

| `WarmFamilyStatusCard` | `ui/components/WarmFamilyStatusCard.kt` | 家庭状态 |

| `WarmHistoryEntry` | `ui/components/WarmHistoryEntry.kt` | 历史入口行 |

| `WarmStatusBanner` | `ui/components/WarmStatusBanner.kt` | 统一状态横幅 |

| `WarmLoadingContent` | `ui/components/WarmLoadingContent.kt` | 统一 Loading |

| `WarmRetryState` | `ui/components/WarmRetryState.kt` | 错误 + 重试 |

| `WarmTaskStatusChip` | `ui/components/WarmTaskStatusChip.kt` | 任务状态色块 |

| `WbAssetPhotos` / `AssetPhoto` | `ui/components/AssetImage.kt` | assets 插画路径 |

| `WarmHeaderGradientBackground` | `ui/components/WarmHeaderGradient.kt` | 头区渐变 + 可选装饰 |

| `WarmTopAppBar` | `ui/components/WarmTopAppBar.kt` | 统一 TopBar |

| `WarmPrimaryButton` | `ui/components/WarmPrimaryButton.kt` | 主按钮 ≥56dp |

| `WarmFeedCard` | `ui/components/WarmFeedCard.kt` | Feed 卡片 |

| `WarmEmptyState` | `ui/components/WarmEmptyState.kt` | 空态 |

| `warmNavBarItemColors` | `ui/components/WarmNavBar.kt` | 底栏色 |



## 数据与网络（未改契约）



| 符号 | 路径 |

|---|---|

| `WarmBridgeApi` | `data/remote/WarmBridgeApi.kt` |

| `NetworkModule` | `data/remote/NetworkModule.kt` |

| `PopularVideoPollWorker` | `video/PopularVideoPollWorker.kt` |

| `humanizeNetworkError` | `util/NetworkErrors.kt` |



## 主题与无障碍



| 符号 | 路径 |

|---|---|

| `WbTypography` | `ui/theme/Type.kt`（正文 18/16sp） |

| `WbDimens` | `ui/theme/WbDimens.kt`（`touchMin` 56、`minTouchTarget` 48） |

| `WbAccessibility` | `ui/theme/WbAccessibility.kt` |



## 文档与代码差异（已闭合项）



| 原差异（阶段 0） | 现状 |

|---|---|

| 无 ViewModel | 家长/孩子首页已引入 |

| 系统栏未接入 Root | `WarmBridgeRoot` 已调用 `WarmBridgeSystemBars` |

| 底栏未用 `warmNavBarItemColors` | Shell 已接入 |

| 家长首页无 feed | `ParentHomeViewModel` 加载重点/精选 |

| 今日热点文案 | 已改为「今日关注」 |

| 无 gradlew | 仍用全局 `gradle`；见 known-issues |



## 源码目录概览



```text

android/app/src/main/java/com/warmbridge/demo/

├── MainActivity.kt

├── navigation/WbRoutes.kt

├── data/local/          ViewModel 配套 Store / Repository

├── data/remote/

├── reminder/            ReminderWorker

├── video/               PopularVideoPollWorker

├── util/                FeedDisplay, NetworkErrors, SessionCover, ...

└── ui/

    ├── WarmBridgeRoot.kt

    ├── shell/

    ├── screens/         业务页 + ViewModel + ExplainPanel

    ├── components/      改版组件库（多数含 @Preview）

    ├── preview/         WarmBridgePreviewData

    └── theme/

```


