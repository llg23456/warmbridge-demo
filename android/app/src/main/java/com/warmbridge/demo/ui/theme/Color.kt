package com.warmbridge.demo.ui.theme

import androidx.compose.ui.graphics.Color

/** 设计系统：暖米白页面底 */
val WbPageBg = Color(0xFFF7F5F2)

/** 卡片 / 弹窗 / 输入框 */
val WbSurface = Color(0xFFFFFFFF)

/** 品牌主色（界面规范 #E07A3D） */
val WbBrandOrange = Color(0xFFE07A3D)

/** 主色（与 Material primary 对齐） */
val WbPrimary = WbBrandOrange

/** 主色按下 */
val WbPrimaryPressed = Color(0xFFC96A32)

/** 顶部渐变起点：主色 10% 透明度 */
val WbHeaderGradientOrange = Color(0x1AE07A3D)

/** Chip 未选背景 / 分段条背景 */
val WbChipUnselectedBg = Color(0xFFF5F5F5)

/** 规范辅助灰字 #666666 */
val WbTextMuted = Color(0xFF666666)

/** 来源标签浅底 */
val WbSourceChipBg = Color(0xFFFFF5ED)

/** 「我的」页背景（iOS 设置灰） */
val WbMinePageBg = Color(0xFFF2F2F7)

/** 卡片标题色 */
val WbCardTitle = Color(0xFF212121)

/** 水波纹 主色 15% */
val WbRippleOrange = Color(0x26E07A3D)

/** 辅色墨绿 */
val WbSecondary = Color(0xFF2E5A4C)

val WbTextPrimary = Color(0xFF1A1A1A)

val WbTextSecondary = Color(0xFF6B6B6B)

val WbDivider = Color(0xFFE8E4DF)

/** 图标浅底：墨绿 10% */
val WbIconTintBg = Color(0x1A2E5A4C)

/** 弹窗遮罩 40% #1A1A1A */
val WbScrim = Color(0x661A1A1A)

// 兼容旧引用（逐步迁移到上方面名）
val WbCream = WbPageBg
val WbWarmOrange = WbPrimary
val WbDeepGreen = WbSecondary
val WbIndigo = Color(0xFF3D4F6F)
val WbCard = WbSurface
val WbSoft = Color(0xFFF0E8E0)
