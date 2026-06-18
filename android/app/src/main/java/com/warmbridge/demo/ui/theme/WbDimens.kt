package com.warmbridge.demo.ui.theme

import androidx.compose.ui.unit.dp

/** 全局间距与尺寸常量，与 res/values/dimens.xml 语义对齐。 */
object WbDimens {
    val screenPadding = 20.dp
    val contentPadding = 16.dp
    val sectionGap = 16.dp
    val cardRadius = 16.dp
    val compactCardRadius = 12.dp
    val priorityCardMinHeight = 160.dp
    val chipRadius = 24.dp
    val chipInnerRadius = 8.dp
    val sourceChipRadius = 4.dp
    val touchMin = 56.dp
    /** WCAG 取向：普通可点击区域最小边长 */
    val minTouchTarget = 48.dp
    val actionCardHeight = 88.dp
    /** @deprecated 首页渐变高度改由屏幕高度动态计算，见 [WarmHomePageShell] */
    val homeGradientHeight = 280.dp
    val homeShortcutIconContainer = 52.dp
    val homeShortcutIconSize = 32.dp
    /** Tab 页透明问候区高度 */
    val pageHeaderHeightParent = 108.dp
    val pageHeaderHeightChild = 96.dp
    val pageHeaderHeightHot = 88.dp
    val pageHeaderHeightMine = 88.dp
    /** 首卡相对 header 的视觉 overlap，消除顶栏条带感 */
    val pageContentOverlap = 12.dp
    val navBarHeight = 56.dp
    val navBarIconSize = 21.dp
    val navBarIconLabelGap = 3.dp
    val hotTabRowHeight = 45.dp
    val hotSearchFieldHeight = 42.dp
    val interestChipHeight = 32.dp
    val interestChipHorizontalPadding = 14.dp
    val interestChipRadius = 16.dp
    val interestChipIconSize = 16.dp
    val iconContainerSize = 40.dp
    val iconSize = 24.dp
}
