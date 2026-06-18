暖桥 Demo — 自定义插画 / 背景图（可选）
=====================================

请将 PNG 或 JPG 放在与本 README 同一目录：
android/app/src/main/assets/photos/

命名：小写 + 下划线。宽图勿放入 res/drawable，避免 APK 膨胀。

代码常量：com.warmbridge.demo.ui.components.WbAssetPhotos
缺图不崩溃；装饰类图片默认不显示灰色占位块。

推荐文件：

0) home_header_background.png
   - 双端首页上半屏背景插画（WarmHomePageShell）

1) role_select_hero.png
   - 选角页中部插画

2) reminder_dialog_header.png
   - 温情提醒弹窗顶图

3) parent_header_decoration.png
   - 家长首页问候区头图装饰（低透明度叠加，不抢主卡）

4) parent_home_watermark.png
   - 家长首页底部线稿（代码 α≈0.1）

5) child_home_header.png
   - 孩子首页头区装饰

6) hot_topics_header.png
   - 今日关注 Tab 头区装饰

7) mine_header.png
   - 我的页个人区背景（可选）

8) ill_empty_feed.png  或  empty_feed_illustration.png
   - 列表空状态，约 120dp 显示区域

9) error_network_illustration.png
   - 网络错误重试态插画（WarmRetryState）
