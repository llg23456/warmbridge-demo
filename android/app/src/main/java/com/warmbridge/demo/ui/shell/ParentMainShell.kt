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
import com.warmbridge.demo.ui.screens.HotTopicsTabScreen
import com.warmbridge.demo.ui.screens.MineScreen
import com.warmbridge.demo.ui.screens.ParentHomeScreen
import kotlinx.coroutines.launch

private val DefaultInterestTags = listOf("科技", "军事", "人文", "健康", "社会")

@Composable
fun ParentMainShell(
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
    val currentRoute = navBackStackEntry?.destination?.route ?: WbRoutes.ParentHome

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == WbRoutes.ParentHome,
                    onClick = {
                        innerNav.navigate(WbRoutes.ParentHome) {
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
                    selected = currentRoute == WbRoutes.ParentHot,
                    onClick = {
                        innerNav.navigate(WbRoutes.ParentHot) {
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
                    selected = currentRoute == WbRoutes.ParentMine,
                    onClick = {
                        innerNav.navigate(WbRoutes.ParentMine) {
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
            startDestination = WbRoutes.ParentHome,
            modifier = Modifier.padding(padding),
        ) {
            composable(WbRoutes.ParentHome) {
                ParentHomeScreen(
                    serverTags = serverTags,
                    selectedTags = interestTags,
                    onSelectedTagsChange = { updateTags(it) },
                    onGoToHotTab = {
                        innerNav.navigate(WbRoutes.ParentHot) {
                            popUpTo(innerNav.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onReminder = { outerNav.navigate(WbRoutes.Reminder) },
                )
            }
            composable(WbRoutes.ParentHot) {
                BackHandler {
                    innerNav.navigate(WbRoutes.ParentHome) {
                        popUpTo(innerNav.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                HotTopicsTabScreen(
                    showChildChannel = true,
                    interestTags = interestTags,
                    onOpenDetail = { id -> outerNav.navigate(WbRoutes.detail(id)) },
                    showTagFilterEditor = false,
                    serverTags = serverTags,
                    onInterestTagsChange = { updateTags(it) },
                )
            }
            composable(WbRoutes.ParentMine) {
                BackHandler {
                    innerNav.navigate(WbRoutes.ParentHome) {
                        popUpTo(innerNav.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                MineScreen(
                    isParent = true,
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
