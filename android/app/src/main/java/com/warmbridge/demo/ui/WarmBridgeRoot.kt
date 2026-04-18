package com.warmbridge.demo.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.warmbridge.demo.ui.components.ReminderInAppDialog
import com.warmbridge.demo.ui.screens.ChildHomeScreen
import com.warmbridge.demo.ui.screens.DetailScreen
import com.warmbridge.demo.ui.screens.FeedScreen
import com.warmbridge.demo.ui.screens.ParentHomeScreen
import com.warmbridge.demo.ui.screens.ReminderScreen
import com.warmbridge.demo.ui.screens.RoleSelectScreen
import com.warmbridge.demo.ui.screens.ShareScreen

@Composable
fun WarmBridgeRoot(
    modifier: Modifier = Modifier,
    reminderDialogText: String? = null,
    onDismissReminderDialog: () -> Unit = {},
) {
    val nav = rememberNavController()
    Box(modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = "role", modifier = Modifier.fillMaxSize()) {
            composable("role") {
                RoleSelectScreen(
                    onParent = { nav.navigate("parent") { popUpTo("role") } },
                    onChild = { nav.navigate("child") { popUpTo("role") } },
                )
            }
            composable("parent") {
                ParentHomeScreen(
                    onPickTagFeed = { tag ->
                        val segment = if (tag.isBlank()) "ALL" else Uri.encode(tag)
                        nav.navigate("feed/tag/$segment")
                    },
                    onChildChannel = { nav.navigate("feed/child/ALL") },
                    onTrendChannel = { nav.navigate("feed/trend/ALL") },
                    onReminder = { nav.navigate("reminder") },
                    onSwitchRole = { nav.navigate("role") { popUpTo(0) } },
                )
            }
            composable("child") {
                ChildHomeScreen(
                    onShare = { nav.navigate("share") },
                    onReminder = { nav.navigate("reminder") },
                    onSwitchRole = { nav.navigate("role") { popUpTo(0) } },
                )
            }
            composable(
                route = "feed/{channel}/{tag}",
                arguments = listOf(
                    navArgument("channel") { type = NavType.StringType },
                    navArgument("tag") { type = NavType.StringType },
                ),
            ) { entry ->
                val channel = entry.arguments!!.getString("channel")!!
                val tag = entry.arguments!!.getString("tag")!!
                FeedScreen(
                    channel = channel,
                    tagToken = tag,
                    onOpenDetail = { id -> nav.navigate("detail/$id") },
                    onBack = { nav.popBackStack() },
                )
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
            composable("share") {
                ShareScreen(
                    onDone = { nav.popBackStack() },
                )
            }
            composable("reminder") {
                ReminderScreen(
                    onBack = { nav.popBackStack() },
                )
            }
        }

        if (!reminderDialogText.isNullOrBlank()) {
            ReminderInAppDialog(
                message = reminderDialogText,
                onAck = onDismissReminderDialog,
                onReply = {
                    onDismissReminderDialog()
                    nav.navigate("reminder")
                },
            )
        }
    }
}
