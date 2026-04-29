package com.ssafy.e102.eumgil.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionBottomTab
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionHomeRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionVoiceInputRoute
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionVoiceInputBottomNav
import com.ssafy.e102.eumgil.feature.search.SearchResultsRoute

fun NavGraphBuilder.lowVisionNavGraph(navController: NavHostController) {
    composable(route = LowVisionRoute.Home.route) {
        LowVisionHomeRoute(
            onVoiceInputClick = {
                navController.navigate(LowVisionRoute.VoiceInput.route)
            },
            onCurrentLocationClick = {
                navController.navigate(TopLevelRoute.Map.route)
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

    composable(route = LowVisionRoute.Search.route) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            SearchResultsRoute(
                initialQuery = "",
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToResults = { _ -> },
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

            LowVisionVoiceInputBottomNav(
                selectedTab = LowVisionBottomTab.CATEGORY,
                onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
            )
        }
    }
}

internal fun resolveLowVisionRecordingCompletedRoute(): String = LowVisionRoute.Search.route

internal fun resolveLowVisionRecordingPopUpRoute(): String = LowVisionRoute.VoiceInput.route

internal fun resolveLowVisionSearchResultRoute(): String =
    RouteSettingRoute.Setting.createRoute(autoStartNavigation = true)

internal fun resolveLowVisionSearchPopUpRoute(): String = LowVisionRoute.Search.route

internal fun resolveNavigationCompletionRoute(): String = LowVisionRoute.Home.route

internal fun resolveLowVisionBottomTabRoute(tab: LowVisionBottomTab): String =
    when (tab) {
        LowVisionBottomTab.HOME -> LowVisionRoute.Home.route
        LowVisionBottomTab.BOOKMARK -> TopLevelRoute.SavedRoute.route
        LowVisionBottomTab.CATEGORY -> LowVisionRoute.Search.route
        LowVisionBottomTab.MY_PAGE -> TopLevelRoute.MyPage.route
    }

private fun NavHostController.navigateToLowVisionBottomTab(tab: LowVisionBottomTab) {
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
        LowVisionBottomTab.BOOKMARK -> navigateToTopLevel(TopLevelDestination.SavedRoute)
        LowVisionBottomTab.CATEGORY -> {
            navigate(LowVisionRoute.Search.route) {
                launchSingleTop = true
                popUpTo(LowVisionRoute.Home.route) {
                    inclusive = false
                }
            }
        }
        LowVisionBottomTab.MY_PAGE -> navigateToTopLevel(TopLevelDestination.MyPage)
    }
}
