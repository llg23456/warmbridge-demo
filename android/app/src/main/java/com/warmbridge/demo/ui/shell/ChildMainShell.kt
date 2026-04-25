package com.warmbridge.demo.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.warmbridge.demo.R
import com.warmbridge.demo.data.local.InterestTagsRepository
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.navigation.WbRoutes
import com.warmbridge.demo.ui.screens.ChildHomeScreen
import com.warmbridge.demo.ui.screens.HotTopicsTabScreen
import com.warmbridge.demo.ui.screens.MineScreen
import kotlinx.coroutines.launch

private val DefaultInterestTags = listOf("科技", "军事", "人文", "健康", "社会", "数码", "吃瓜", "AI", "生活")

@Composable
fun ChildMainShell(
    outerNav: NavHostController,
    modifier: Modifier = Modifier,
) {
    val innerNav = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val interestRepo = remember { InterestTagsRepository(context) }
    val scope = rememberCoroutineScope()
    var interestTags by remember { mutableStateOf(emptySet<String>()) }
    var serverTags by remember { mutableStateOf(DefaultInterestTags) }

    LaunchedEffect(interestRepo) {
        interestRepo.tags.collect { interestTags = it }
    }
    LaunchedEffect(Unit) {
        runCatching { NetworkModule.api.tags().tags }
            .onSuccess { remote -> if (remote.isNotEmpty()) serverTags = remote }
    }

    fun updateTags(s: Set<String>) {
        interestTags = s
        scope.launch { interestRepo.setTags(s) }
    }

    val navBackStackEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: WbRoutes.ChildHome

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == WbRoutes.ChildHome,
                    onClick = {
                        innerNav.navigate(WbRoutes.ChildHome) {
                            popUpTo(innerNav.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.cd_nav_home))
                    },
                    label = { Text(stringResource(R.string.nav_home)) },
                )
                NavigationBarItem(
                    selected = currentRoute == WbRoutes.ChildHot,
                    onClick = {
                        innerNav.navigate(WbRoutes.ChildHot) {
                            popUpTo(innerNav.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(Icons.Filled.Whatshot, contentDescription = stringResource(R.string.cd_nav_hot))
                    },
                    label = { Text(stringResource(R.string.nav_hot)) },
                )
                NavigationBarItem(
                    selected = currentRoute == WbRoutes.ChildMine,
                    onClick = {
                        innerNav.navigate(WbRoutes.ChildMine) {
                            popUpTo(innerNav.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.cd_nav_mine))
                    },
                    label = { Text(stringResource(R.string.nav_mine)) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = innerNav,
            startDestination = WbRoutes.ChildHome,
            modifier = Modifier.padding(padding),
        ) {
            composable(WbRoutes.ChildHome) {
                ChildHomeScreen(
                    onShare = { outerNav.navigate(WbRoutes.Share) },
                    onReminder = { outerNav.navigate(WbRoutes.Reminder) },
                    onGoToHotTab = {
                        innerNav.navigate(WbRoutes.ChildHot) {
                            popUpTo(innerNav.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onImageExplain = { outerNav.navigate(WbRoutes.ImageExplain) },
                    onVideoQuick = { outerNav.navigate(WbRoutes.VideoQuick) },
                )
            }
            composable(WbRoutes.ChildHot) {
                BackHandler {
                    innerNav.navigate(WbRoutes.ChildHome) {
                        popUpTo(innerNav.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                HotTopicsTabScreen(
                    showChildChannel = false,
                    interestTags = interestTags,
                    onOpenDetail = { id -> outerNav.navigate(WbRoutes.detail(id)) },
                    showTagFilterEditor = true,
                    serverTags = serverTags,
                    onInterestTagsChange = { updateTags(it) },
                )
            }
            composable(WbRoutes.ChildMine) {
                BackHandler {
                    innerNav.navigate(WbRoutes.ChildHome) {
                        popUpTo(innerNav.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                MineScreen(
                    isParent = false,
                    onReminder = { outerNav.navigate(WbRoutes.Reminder) },
                    onSwitchRole = {
                        outerNav.navigate(WbRoutes.Role) {
                            popUpTo(WbRoutes.Role) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
