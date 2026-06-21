package com.warmbridge.demo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.warmbridge.demo.navigation.WbRoutes

private fun isSecondaryRoute(route: String?): Boolean {
    if (route.isNullOrBlank()) return false
    return route.startsWith("detail/") ||
        route.startsWith("video_popular/") ||
        route == WbRoutes.Share ||
        route == WbRoutes.Reminder ||
        route == WbRoutes.ImageExplain ||
        route == WbRoutes.VideoQuick
}

/**
 * Tab 壳状态栏跟首页暖橙顶色衔接；选角页品牌橙；二级页跟 TopBar 走 surface。
 */
@Composable
fun WarmBridgeSystemBars(currentRoute: String?) {
    val systemUiController = rememberSystemUiController()
    val scheme = MaterialTheme.colorScheme

    val isTabShell = currentRoute == WbRoutes.Parent || currentRoute == WbRoutes.Child
    val isRoleSelect = currentRoute == WbRoutes.Role
    val statusBarColor = when {
        isTabShell -> WarmHomeHeaderOrangeTop
        isRoleSelect -> WbBrandOrange
        isSecondaryRoute(currentRoute) -> scheme.surface
        else -> WbPageBg
    }
    val statusBarDarkIcons = !isTabShell && !isRoleSelect

    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = statusBarDarkIcons,
        )
        systemUiController.setNavigationBarColor(
            color = WbPageBg,
            darkIcons = true,
        )
    }
}
