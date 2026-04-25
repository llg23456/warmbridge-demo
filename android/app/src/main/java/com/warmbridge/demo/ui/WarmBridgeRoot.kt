package com.warmbridge.demo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.warmbridge.demo.navigation.WbRoutes
import com.warmbridge.demo.ui.components.ReminderInAppDialog
import com.warmbridge.demo.ui.screens.DetailScreen
import com.warmbridge.demo.ui.screens.ImageExplainScreen
import com.warmbridge.demo.ui.screens.ReminderScreen
import com.warmbridge.demo.ui.screens.RoleSelectScreen
import com.warmbridge.demo.ui.screens.ShareScreen
import com.warmbridge.demo.ui.screens.VideoQuickScreen
import com.warmbridge.demo.ui.shell.ChildMainShell
import com.warmbridge.demo.ui.shell.ParentMainShell

@Composable
fun WarmBridgeRoot(
    modifier: Modifier = Modifier,
    reminderDialogText: String? = null,
    onDismissReminderDialog: () -> Unit = {},
) {
    val nav = rememberNavController()
    Box(modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = WbRoutes.Role, modifier = Modifier.fillMaxSize()) {
            composable(WbRoutes.Role) {
                RoleSelectScreen(
                    onParent = {
                        nav.navigate(WbRoutes.Parent) {
                            popUpTo(WbRoutes.Role) { inclusive = true }
                        }
                    },
                    onChild = {
                        nav.navigate(WbRoutes.Child) {
                            popUpTo(WbRoutes.Role) { inclusive = true }
                        }
                    },
                )
            }
            composable(WbRoutes.Parent) {
                ParentMainShell(outerNav = nav)
            }
            composable(WbRoutes.Child) {
                ChildMainShell(outerNav = nav)
            }
            composable(
                route = "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments!!.getString("id")!!
                DetailScreen(
                    itemId = id,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(WbRoutes.Share) {
                ShareScreen(
                    onDone = { nav.popBackStack() },
                )
            }
            composable(WbRoutes.Reminder) {
                ReminderScreen(
                    onBack = { nav.popBackStack() },
                )
            }
            composable(WbRoutes.ImageExplain) {
                ImageExplainScreen(
                    onBack = { nav.popBackStack() },
                    onDoneToDetail = { id ->
                        nav.navigate(WbRoutes.detail(id)) {
                            popUpTo(WbRoutes.ImageExplain) { inclusive = true }
                        }
                    },
                )
            }
            composable(WbRoutes.VideoQuick) {
                VideoQuickScreen(
                    onBack = { nav.popBackStack() },
                    onDoneToDetail = { id ->
                        nav.navigate(WbRoutes.detail(id)) {
                            popUpTo(WbRoutes.VideoQuick) { inclusive = true }
                        }
                    },
                )
            }
        }

        if (!reminderDialogText.isNullOrBlank()) {
            ReminderInAppDialog(
                message = reminderDialogText,
                onAck = onDismissReminderDialog,
                onReply = {
                    onDismissReminderDialog()
                    nav.navigate(WbRoutes.Reminder)
                },
            )
        }
    }
}
