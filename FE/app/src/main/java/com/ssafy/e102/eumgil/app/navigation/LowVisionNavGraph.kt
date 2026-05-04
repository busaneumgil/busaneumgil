package com.ssafy.e102.eumgil.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionBottomTab
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionAppInfoRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionBookmarkRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionCategoryRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionHomeRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionMyPageRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionNavigationCompleteRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionNavigationRoute
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionRouteBriefingRoute
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
            onCancelRecording = {
                navController.navigate(resolveLowVisionVoiceInputCancelRoute()) {
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
                navController.navigate(LowVisionRoute.Guidance.route)
            },
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
    }

    composable(route = LowVisionRoute.Search.route) {
        LowVisionSearchResultShell(
            navController = navController,
            selectedTab = LowVisionBottomTab.HOME,
            initialQuery = "",
        )
    }

    composable(route = LowVisionRoute.CategorySearch.route) {
        LowVisionCategoryRoute(
            onBackClick = {
                navController.navigateToLowVisionBottomTab(LowVisionBottomTab.HOME)
            },
            onCategorySelected = { category ->
                navController.navigate(LowVisionRoute.CategoryResult.createRoute(category))
            },
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
    }

    composable(
        route = LowVisionRoute.CategoryResult.route,
        arguments =
            listOf(
                navArgument(LowVisionRoute.CategoryResult.ARG_CATEGORY) {
                    type = NavType.StringType
                },
            ),
    ) { backStackEntry ->
        val category = backStackEntry.arguments?.getString(LowVisionRoute.CategoryResult.ARG_CATEGORY).orEmpty()
        LowVisionSearchResultShell(
            navController = navController,
            selectedTab = LowVisionBottomTab.CATEGORY,
            initialQuery = category,
            categoryLabel = category,
        )
    }

    composable(route = LowVisionRoute.RouteBriefing.route) {
        LowVisionRouteBriefingRoute(
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
    }

    composable(route = LowVisionRoute.Guidance.route) {
        LowVisionNavigationRoute(
            onNavigateToComplete = {
                navController.navigate(resolveLowVisionNavigationExitRoute()) {
                    launchSingleTop = true
                    popUpTo(LowVisionRoute.Guidance.route) {
                        inclusive = true
                    }
                }
            },
            onNavigateToBookmark = {
                navController.navigate(LowVisionRoute.Bookmark.route) {
                    launchSingleTop = true
                    popUpTo(LowVisionRoute.Guidance.route) {
                        inclusive = true
                    }
                }
            },
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
    }

    composable(route = LowVisionRoute.NavigationComplete.route) {
        LowVisionNavigationCompleteRoute(
            onNavigateToBookmark = {
                navController.navigate(LowVisionRoute.Bookmark.route) {
                    launchSingleTop = true
                    popUpTo(LowVisionRoute.NavigationComplete.route) {
                        inclusive = true
                    }
                }
            },
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
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

@Composable
private fun LowVisionSearchResultShell(
    navController: NavHostController,
    selectedTab: LowVisionBottomTab,
    initialQuery: String,
    categoryLabel: String? = null,
) {
    val searchPopUpRoute = resolveLowVisionSearchPopUpRoute(selectedTab)
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        LowVisionSearchRoute(
            initialQuery = initialQuery,
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToRouteSetting = {
                navController.navigate(LowVisionRoute.Guidance.route) {
                    launchSingleTop = true
                    popUpTo(searchPopUpRoute) {
                        inclusive = true
                    }
                }
            },
            onNavigateToRouteBriefing = {
                navController.navigate(LowVisionRoute.RouteBriefing.route) {
                    launchSingleTop = true
                }
            },
            onNavigateToBookmark = {
                navController.navigate(LowVisionRoute.Bookmark.route) {
                    launchSingleTop = true
                    popUpTo(searchPopUpRoute) {
                        inclusive = true
                    }
                }
            },
            modifier = Modifier.weight(1f),
            categoryLabel = categoryLabel,
        )

        LowVisionBottomNav(
            selectedTab = selectedTab,
            onTabSelected = { tab -> navController.navigateToLowVisionBottomTab(tab) },
        )
    }
}

internal fun resolveLowVisionRecordingCompletedRoute(): String = LowVisionRoute.Search.route

internal fun resolveLowVisionVoiceInputCancelRoute(): String = LowVisionRoute.Home.route

internal fun resolveLowVisionRecordingPopUpRoute(): String = LowVisionRoute.VoiceInput.route

internal fun resolveLowVisionSearchResultRoute(): String =
    LowVisionRoute.Guidance.route

internal fun resolveLowVisionSearchPopUpRoute(selectedTab: LowVisionBottomTab = LowVisionBottomTab.HOME): String =
    when (selectedTab) {
        LowVisionBottomTab.CATEGORY -> LowVisionRoute.CategorySearch.route
        else -> LowVisionRoute.Search.route
    }

internal fun resolveNavigationCompletionRoute(): String = ArrivalRoute.Entry.route

internal fun resolveLowVisionNavigationExitRoute(): String = LowVisionRoute.Home.route

internal fun resolveLowVisionCurrentLocationRoute(): String? = null

internal fun resolveLowVisionModeChangeRoute(): String = OnboardingRoute.ProfileUserTypePrimary.route

internal fun resolveLowVisionAppInfoRoute(): String = LowVisionRoute.AppInfo.route

internal fun resolveLowVisionLogoutRoute(): String = AuthRoute.Login.route

internal fun resolveLowVisionBottomTabRoute(tab: LowVisionBottomTab): String =
    when (tab) {
        LowVisionBottomTab.HOME -> LowVisionRoute.Home.route
        LowVisionBottomTab.BOOKMARK -> LowVisionRoute.Bookmark.route
        LowVisionBottomTab.CATEGORY -> LowVisionRoute.CategorySearch.route
        LowVisionBottomTab.MY_PAGE -> LowVisionRoute.MyPage.route
    }

internal fun resolveLowVisionSelectedBottomTab(currentRoute: String?): LowVisionBottomTab? =
    when (currentRoute) {
        LowVisionRoute.Home.route,
        LowVisionRoute.VoiceInput.route,
        LowVisionRoute.Search.route,
        LowVisionRoute.RouteBriefing.route,
        LowVisionRoute.Guidance.route,
        LowVisionRoute.NavigationComplete.route -> LowVisionBottomTab.HOME
        LowVisionRoute.Bookmark.route -> LowVisionBottomTab.BOOKMARK
        LowVisionRoute.CategorySearch.route,
        LowVisionRoute.CategoryResult.route -> LowVisionBottomTab.CATEGORY
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
            navigate(LowVisionRoute.CategorySearch.route) {
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
