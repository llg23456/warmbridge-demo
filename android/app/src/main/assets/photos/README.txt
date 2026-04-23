暖桥 Demo — 自定义插画 / 背景图（可选）
=====================================

请将 PNG 或 JPG 放在与本 README 同一目录：`android/app/src/main/assets/photos/`

已有 / 推荐文件名：

1) role_select_hero.png
   - 选角页中部插画

2) reminder_dialog_header.png
   - 温情提醒弹窗顶图

3) parent_home_watermark.png  （首页优化新增）
   - 家长首页底部装饰线稿/插画，代码中以约 10% 透明度平铺；可不提供则该区域为透明占位

4) ill_empty_feed.png  （列表空状态）
   - 「今日热点」等列表无数据时中央展示，建议约 240×240 px 以上，显示区域约 120dp

代码常量见：`com.warmbridge.demo.ui.components.WbAssetPhotos`
