# UI 改版后截图清单

> 阶段 14 交付。请在 **改版完成并安装最新 Debug APK** 后，于模拟器或真机截取放入本目录。  
> 与 `docs/ui-redesign-before/` 同名文件一一对比。

## 必截页面

| 文件名建议 | 页面 | 角色 | 改版后预期特征 |
|---|---|---|---|
| `01-role-select.png` | 选角页 | — | 文案在 `strings.xml`；可选 `role_select_hero` |
| `02-parent-home.png` | 家长首页 | 家长 | 120dp 头区；今日重点卡；工具行；精选 |
| `03-child-home.png` | 孩子首页 | 孩子 | 家庭状态卡 + 主 CTA 分享 + 最近分享 |
| `04-parent-hot-interest.png` | 今日关注 | 家长 | 标题「今日关注」；112dp 头图 |
| `05-parent-hot-child.png` | 今日关注 | 家长 | 孩子推荐频道 |
| `06-child-hot.png` | 今日关注 | 孩子 | **两频道**（无孩子推荐） |
| `07-mine-parent.png` | 我的 | 家长 | 任务优先；`WarmPopularVideoStatusChip` |
| `08-mine-child.png` | 我的 | 孩子 | |
| `09-detail-feed.png` | 详情 | 任意 | `WarmTopAppBar` |
| `10-explain-panel.png` | 解读区 | 任意 | 可折叠理解顺序 |
| `11-share.png` | 分享 | 孩子 | `WarmToolScreenScaffold` + 底栏发送 |
| `12-reminder.png` | 温情提醒 | 任意 | Demo Info 横幅 |
| `13-image-explain.png` | 图片识梗 | 任意 | 底栏「上传并讲解」 |
| `14-video-quick.png` | 视频快解析 | 任意 | 底栏「解析并生成解读」 |
| `15-video-popular-running.png` | 通俗视频 | 任意 | `WarmSectionCard` 进度遮罩 |
| `16-video-popular-done.png` | 通俗视频 | 任意 | 播放器 + 解读区块 |

## 状态变体

| 文件名 | 场景 |
|---|---|
| `parent-home-loading.png` | 家长首页 Loading |
| `parent-home-error.png` | `WarmRetryState` + 可选网络插画 |
| `feed-empty.png` | 今日关注空列表 |
| `font-scale-1.5x.png` | 系统字体 1.5× 家长首页或详情 |

## 品牌

| 文件名 | 说明 |
|---|---|
| `launcher-icon.png` | 桌面图标（桥为主视觉） |

## 当前状态

- [ ] 截图尚未入库（需人工截取）
- [x] 改版代码与 `assembleDebug` 已通过（2026-06-11）
