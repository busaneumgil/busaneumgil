package com.ssafy.e102.eumgil.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.composable
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionBottomTab
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionAppInfoRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionBookmarkRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionHomeRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionMyPageRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionSearchRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionVoiceInputRoute
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionBottomNav

fun NavGraphBuilder.lowVisionNavGraph(navController: NavHostController) {
    composable(route = LowVisionRoute.Home.route) {
        LowVisionHomeRoute(
            onVoiceInputClick = {
                navController.navigate(LowVisionRoute.VoiceInput.route)
            },
            onCurrentLocationClick = {
                resolveLowVisionCurrentLocationRoute()?.let(navController::navigate)
            },
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
    }

    composable(route = LowVisionRoute.VoiceInput.route) {
        LowVisionVoiceInputRoute(
            onRecordingFinished = {
                navController.navigate(resolveLowVisionRecordingCompletedRoute()) {
                    launchSingleTop = true
                    popUpTo(resolveLowVisionRecordingPopUpRoute()) {
                        inclusive = true
                    }
                }
            },
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
    }

    composable(route = LowVisionRoute.Bookmark.route) {
        LowVisionBookmarkRoute(
            onNavigateToRouteSetting = {
                navController.navigate(RouteSettingRoute.Setting.createRoute(autoStartNavigation = true))
            },
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
    }

    composable(route = LowVisionRoute.Search.route) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            LowVisionSearchRoute(
                initialQuery = "",
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRouteSetting = {
                    navController.navigate(resolveLowVisionSearchResultRoute()) {
                        launchSingleTop = true
                        popUpTo(resolveLowVisionSearchPopUpRoute()) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )

            LowVisionBottomNav(
                selectedTab = LowVisionBottomTab.CATEGORY,
                onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
            )
        }
    }

    composable(route = LowVisionRoute.MyPage.route) {
        LowVisionMyPageRoute(
            onModeChangeClick = {
                navController.navigate(resolveLowVisionModeChangeRoute())
            },
            onAppInfoClick = {
                navController.navigate(resolveLowVisionAppInfoRoute())
            },
            onLogoutClick = {
                navController.navigate(resolveLowVisionLogoutRoute()) {
                    launchSingleTop = true
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                }
            },
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
    }

    composable(route = LowVisionRoute.AppInfo.route) {
        LowVisionAppInfoRoute(
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
    }
}

internal fun resolveLowVisionRecordingCompletedRoute(): String = LowVisionRoute.Search.route

internal fun resolveLowVisionRecordingPopUpRoute(): String = LowVisionRoute.VoiceInput.route

internal fun resolveLowVisionSearchResultRoute(): String =
    RouteSettingRoute.Setting.createRoute(autoStartNavigation = true)

internal fun resolveLowVisionSearchPopUpRoute(): String = LowVisionRoute.Search.route

internal fun resolveNavigationCompletionRoute(): String = ArrivalRoute.Entry.route

internal fun resolveLowVisionCurrentLocationRoute(): String? = null

internal fun resolveLowVisionModeChangeRoute(): String = OnboardingRoute.ProfileUserTypePrimary.route

internal fun resolveLowVisionAppInfoRoute(): String = LowVisionRoute.AppInfo.route

internal fun resolveLowVisionLogoutRoute(): String = AuthRoute.Login.route

internal fun resolveLowVisionBottomTabRoute(tab: LowVisionBottomTab): String =
    when (tab) {
        LowVisionBottomTab.HOME -> LowVisionRoute.Home.route
        LowVisionBottomTab.BOOKMARK -> LowVisionRoute.Bookmark.route
        LowVisionBottomTab.CATEGORY -> LowVisionRoute.Search.route
        LowVisionBottomTab.MY_PAGE -> LowVisionRoute.MyPage.route
    }

internal fun resolveLowVisionSelectedBottomTab(currentRoute: String?): LowVisionBottomTab? =
    when (currentRoute) {
        LowVisionRoute.Home.route,
        LowVisionRoute.VoiceInput.route -> LowVisionBottomTab.HOME
        LowVisionRoute.Bookmark.route -> LowVisionBottomTab.BOOKMARK
        LowVisionRoute.Search.route -> LowVisionBottomTab.CATEGORY
        LowVisionRoute.MyPage.route,
        LowVisionRoute.AppInfo.route -> LowVisionBottomTab.MY_PAGE
        else -> null
    }

internal fun shouldNavigateLowVisionBottomTab(
    currentRoute: String?,
    selectedTab: LowVisionBottomTab,
): Boolean = resolveLowVisionSelectedBottomTab(currentRoute) != selectedTab

private fun NavHostController.navigateToLowVisionBottomTab(tab: LowVisionBottomTab) {
    if (!shouldNavigateLowVisionBottomTab(currentBackStackEntry?.destination?.route, tab)) {
        return
    }

    when (tab) {
        LowVisionBottomTab.HOME -> {
            val didPopHome =
                popBackStack(
                    route = LowVisionRoute.Home.route,
                    inclusive = false,
                )
            if (!didPopHome) {
                navigate(LowVisionRoute.Home.route) {
                    launchSingleTop = true
                }
            }
        }
        LowVisionBottomTab.BOOKMARK -> {
            navigate(LowVisionRoute.Bookmark.route) {
                launchSingleTop = true
                popUpTo(LowVisionRoute.Home.route) {
                    inclusive = false
                }
            }
        }
        LowVisionBottomTab.CATEGORY -> {
            navigate(LowVisionRoute.Search.route) {
                launchSingleTop = true
                popUpTo(LowVisionRoute.Home.route) {
                    inclusive = false
                }
            }
        }
        LowVisionBottomTab.MY_PAGE -> {
            navigate(LowVisionRoute.MyPage.route) {
                launchSingleTop = true
                popUpTo(LowVisionRoute.Home.route) {
                    inclusive = false
                }
            }
        }
    }
}
