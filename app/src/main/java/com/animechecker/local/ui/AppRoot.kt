package com.animechecker.local.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.animechecker.local.di.AppContainer
import com.animechecker.local.ui.calendar.CalendarScreen
import com.animechecker.local.ui.detail.DetailScreen
import com.animechecker.local.ui.explore.ExploreScreen
import com.animechecker.local.ui.home.HomeScreen
import com.animechecker.local.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/** 顶层页签路由。 */
@Serializable
data object HomeRoute

@Serializable
data object CalendarRoute

@Serializable
data object ExploreRoute

/** 详情：参数显式声明在 route 上，取参走 toRoute，不用字符串拼接。 */
@Serializable
data class DetailRoute(val subjectId: Long)

/** 设置：从首页顶栏进入，不占页签。 */
@Serializable
data object SettingsRoute

private data class Tab(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val TABS = listOf(
    Tab(HomeRoute, HomeRoute::class, Strings.TAB_LIST, Icons.Filled.Checklist, Icons.Outlined.Checklist),
    Tab(
        CalendarRoute,
        CalendarRoute::class,
        Strings.TAB_CALENDAR,
        Icons.Filled.CalendarMonth,
        Icons.Outlined.CalendarMonth,
    ),
    Tab(
        ExploreRoute,
        ExploreRoute::class,
        Strings.TAB_EXPLORE,
        Icons.Filled.VideoLibrary,
        Icons.Outlined.VideoLibrary,
    ),
)

/**
 * 导航宿主：底部页签 + NavHost。
 *
 * 约定：
 * - 页面数据一律来自 Room Flow，导航不承担数据传递；
 * - 切页签不触发目录级网络刷新，刷新时机归各页 ViewModel（刷新分层见 data/repository）。
 */
@Composable
fun AppRoot(container: AppContainer) {
    CompositionLocalProvider(LocalAppContainer provides container) {
        val nav = rememberNavController()
        val backStackEntry by nav.currentBackStackEntryAsState()
        val current = backStackEntry?.destination
        val showTabs = current != null && TABS.any { current.isRoute(it.routeClass) }

        // 页签导航栏平滑过渡
        val tabBarInset by animateDpAsState(
            targetValue = if (showTabs) TAB_BAR_HEIGHT else 0.dp,
            animationSpec = tween(TAB_BAR_ANIM_MS),
            label = "tabBarInset",
        )

        Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
            val consumedBottom = with(LocalDensity.current) {
                WindowInsets(bottom = innerPadding.calculateBottomPadding().roundToPx())
            }
            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController = nav,
                    startDestination = HomeRoute,
                    modifier = Modifier
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding() + tabBarInset,
                        )
                        .consumeWindowInsets(consumedBottom),
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            onOpenDetail = { subjectId -> nav.navigate(DetailRoute(subjectId)) },
                            onOpenSettings = { nav.navigate(SettingsRoute) },
                            onGoExplore = { nav.navigateToTab(ExploreRoute) },
                        )
                    }
                    composable<CalendarRoute> {
                        CalendarScreen(onOpenDetail = { subjectId -> nav.navigate(DetailRoute(subjectId)) })
                    }
                    composable<ExploreRoute> {
                        ExploreScreen(onOpenDetail = { subjectId -> nav.navigate(DetailRoute(subjectId)) })
                    }
                    composable<DetailRoute> { entry ->
                        DetailScreen(
                            subjectId = entry.toRoute<DetailRoute>().subjectId,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(onBack = { nav.popBackStack() })
                    }
                }

                AnimatedVisibility(
                    visible = showTabs,
                    enter = fadeIn(tween(TAB_BAR_ANIM_MS)) + slideInVertically(tween(TAB_BAR_ANIM_MS)) { it },
                    exit = fadeOut(tween(TAB_BAR_ANIM_MS)) + slideOutVertically(tween(TAB_BAR_ANIM_MS)) { it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        TABS.forEach { tab ->
                            val selected = current?.isRoute(tab.routeClass) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { nav.navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 页签栏内容高度默认 80dp。 */
private val TAB_BAR_HEIGHT = 80.dp

/** 页签栏让位与自身进出的动画时长。 */
private const val TAB_BAR_ANIM_MS = 300

/** 当前 destination 是否属于该路由（含其父图）。 */
private fun NavDestination.isRoute(routeClass: KClass<*>): Boolean =
    hierarchy.any { it.hasRoute(routeClass) }

/** 页签切换：各页签状态保留，回到页签栈顶而不堆叠。 */
private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
