# 暖桥 UI 改版 · 已知问题与限制

> 阶段 14 产出 · 不阻塞 `assembleDebug`，但影响 lint / 真机体验时需知晓。

## 构建与工具链

| 问题 | 严重度 | 说明 |
|---|---|---|
| 无 `gradlew.bat` | 低 | 使用全局 `gradle` 或 Android Studio；见 README |
| `lintDebug` 3 Error | 中 | **改版前既有**，非 UI 改版引入：`PopularVideoPollWorker` / `ReminderWorker` `MissingPermission`；`MainActivity` `UnspecifiedRegisterReceiverFlag` |
| Lint 30 Warnings | 低 | 含 `UnusedResources`（预留 strings）、`MonochromeLauncherIcon`、`InsecureBaseConfiguration`（Demo 明文 HTTP）等 |
| 无单元测试 | 低 | `testDebugUnitTest` NO-SOURCE |

## 运行时 / 产品

| 问题 | 严重度 | 说明 |
|---|---|---|
| 后端未启动时全页错误 | 预期 | `humanizeNetworkError` + `WarmRetryState`；非崩溃 |
| 孩子「家庭状态」为 Demo | 低 | `DemoShareStatus` + `WarmFamilyStatusCard` 标注演示，非真实已读 |
| 分享记录仅本机 | 预期 | `ChildShareLocalStore`；不跨设备 |
| 提醒仅本机通知 | 预期 | `reminder_demo_local_only` 已明示 |
| 我的 · 浏览历史/收藏/提问 | 低 | 占位对话框，无 Room 后端 |
| 可选 `assets/photos/*.png` 缺失 | 低 | `AssetPhoto` 静默跳过或占位，不崩溃 |
| 通俗视频播放中返回无确认弹窗 | 低 | 说明书提及；当前直接返回（行为与改版前一致） |

## 无障碍 / 视觉

| 问题 | 严重度 | 说明 |
|---|---|---|
| Adaptive Icon 无 monochrome | 低 | Lint `MonochromeLauncherIcon`；Android 13+ 主题图标可选后续补 |
| `WarmBridgeSystemBars` Accompanist 已弃用 | 低 | 构建 Warning；建议后续迁 `EdgeToEdge` |
| 部分预留 `strings.xml` 未引用 | 低 | 阶段 1 预埋文案，lint `UnusedResources` |

## 联调环境

| 问题 | 说明 |
|---|---|
| `warmbridge.api.baseUrl` 因设备而异 | 模拟器 `10.0.2.2`；真机填电脑局域网 IP |
| Agent 交付时后端未运行 | `/health` 未在本机验证；交付前请自行启动 `uvicorn` |

## 不在本次范围（刻意未改）

- FastAPI BFF 业务接口与通俗视频生成流水线  
- `PopularVideoPollWorker` 轮询间隔与通知逻辑（仅 UI 层调整）  
- Explain / TTS / 追问 API 与缓存 Key  
- 识图 OCR、快解析、share API 契约  
