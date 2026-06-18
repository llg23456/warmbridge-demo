package com.warmbridge.demo.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.warmbridge.demo.R
import com.warmbridge.demo.data.local.ChildShareLocalStore
import com.warmbridge.demo.data.local.InterestTagsRepository
import com.warmbridge.demo.data.local.SharePrefillHolder
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.navigation.WbRoutes
import com.warmbridge.demo.ui.components.WarmBottomNavBar
import com.warmbridge.demo.ui.components.WarmNavTab
import com.warmbridge.demo.ui.screens.ChildHomeScreen
import com.warmbridge.demo.ui.screens.ChildHomeViewModel
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
    val shareStore = remember { ChildShareLocalStore(context) }
    val scope = rememberCoroutineScope()
    var selectedInterestTag by remember { mutableStateOf<String?>(null) }
    var serverTags by remember { mutableStateOf(DefaultInterestTags) }

    LaunchedEffect(interestRepo) {
        interestRepo.selectedTag.collect { selectedInterestTag = it }
    }
    LaunchedEffect(Unit) {
        runCatching { NetworkModule.api.tags().tags }
            .onSuccess { remote -> if (remote.isNotEmpty()) serverTags = remote }
    }

    fun updateSelectedTag(tag: String?) {
        selectedInterestTag = tag
        scope.launch { interestRepo.setSelectedTag(tag) }
    }

    val navBackStackEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: WbRoutes.ChildHome

    val childTabs = listOf(
        WarmNavTab(
            route = WbRoutes.ChildHome,
            label = stringResource(R.string.nav_home),
            icon = Icons.Filled.Home,
            contentDescription = stringResource(R.string.cd_nav_home),
        ),
        WarmNavTab(
            route = WbRoutes.ChildHot,
            label = stringResource(R.string.nav_hot),
            icon = Icons.Filled.Star,
            contentDescription = stringResource(R.string.cd_nav_hot),
        ),
        WarmNavTab(
            route = WbRoutes.ChildMine,
            label = stringResource(R.string.nav_mine),
            icon = Icons.Filled.Person,
            contentDescription = stringResource(R.string.cd_nav_mine),
        ),
    )

    fun navigateTab(route: String) {
        innerNav.navigate(route) {
            popUpTo(innerNav.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            WarmBottomNavBar(
                selectedRoute = currentRoute,
                tabs = childTabs,
                onTabSelected = ::navigateTab,
            )
        },
    ) { padding ->
        NavHost(
            navController = innerNav,
            startDestination = WbRoutes.ChildHome,
            modifier = Modifier.padding(padding),
        ) {
            composable(WbRoutes.ChildHome) {
                val homeViewModel: ChildHomeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ChildHomeViewModel(shareStore) as T
                        }
                    },
                )
                ChildHomeScreen(
                    onShare = { outerNav.navigate(WbRoutes.Share) },
                    onShareRecommend = { url, note ->
                        SharePrefillHolder.set(url, note)
                        outerNav.navigate(WbRoutes.Share)
                    },
                    onReminder = { outerNav.navigate(WbRoutes.Reminder) },
                    onImageExplain = { outerNav.navigate(WbRoutes.ImageExplain) },
                    onVideoQuick = { outerNav.navigate(WbRoutes.VideoQuick) },
                    onGoToHotTab = { navigateTab(WbRoutes.ChildHot) },
                    viewModel = homeViewModel,
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
                    selectedInterestTag = selectedInterestTag,
                    onOpenDetail = { id -> outerNav.navigate(WbRoutes.detail(id)) },
                    showTagFilterEditor = true,
                    serverTags = serverTags,
                    onSelectedInterestTagChange = { updateSelectedTag(it) },
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
                    onOpenPopularVideoJob = { itemId, jobId ->
                        outerNav.navigate(WbRoutes.videoPopular(itemId, jobId))
                    },
                )
            }
        }
    }
}
