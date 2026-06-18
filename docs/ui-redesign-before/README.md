# UI 改版前截图清单

> 阶段 0 建立清单。请在 Android Studio 模拟器或真机上手动截取并放入本目录。  
> 建议设备：约 360dp 逻辑宽，系统字体默认。

## 必截页面

| 文件名建议 | 页面 | 角色 | 备注 |
|---|---|---|---|
| `01-role-select.png` | 选角页 | — | 启动后首屏 |
| `02-parent-home.png` | 家长首页 | 家长 | 含兴趣标签、工具入口、热点 CTA |
| `03-child-home.png` | 孩子首页 | 孩子 | 分享/提醒/识图/快解析入口 |
| `04-parent-hot-interest.png` | 今日热点 Tab | 家长 | 「按兴趣」频道 |
| `05-parent-hot-child.png` | 今日热点 Tab | 家长 | 「孩子推荐」频道 |
| `06-child-hot.png` | 今日热点 Tab | 孩子 | 两频道（无孩子推荐） |
| `07-mine-parent.png` | 我的 | 家长 | 含通俗视频任务 |
| `08-mine-child.png` | 我的 | 孩子 | |
| `09-detail-feed.png` | 详情页 | 任意 | 普通 Feed 条目 |
| `10-explain-panel.png` | 详情 · 解读区 | 任意 | 展开 ExplainPanel |
| `11-share.png` | 分享链接 | 孩子 | |
| `12-reminder.png` | 温情提醒 | 任意 | |
| `13-image-explain.png` | 图片识梗 | 任意 | |
| `14-video-quick.png` | 视频快解析 | 任意 | |
| `15-video-popular-running.png` | 通俗视频 | 任意 | 生成中 |
| `16-video-popular-done.png` | 通俗视频 | 任意 | 已完成可播放 |

## 状态变体（阶段 4+ 对比用）

- `parent-home-loading.png`
- `parent-home-empty.png`
- `parent-home-error.png`
- `parent-home-font-1.5x.png`

## 当前基线 UI 特征（文字记录）

- 底部 Tab 文案：**首页 / 今日热点 / 我的**
- 家长首页：220dp 渐变头图 + 兴趣 Chip + 识图/快解析卡片 + 热点 CTA + 提醒
- 孩子首页：分享/提醒/识图/快解析四入口
- 热点页：220dp 头图 + SegmentedControl 三/两频道
- 详情：ExplainPanel 模块平铺，非理解顺序分组
