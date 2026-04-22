package com.ssafy.e102.eumgil.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssafy.e102.eumgil.feature.map.MapRoute
import com.ssafy.e102.eumgil.feature.mypage.MyPageRoute
import com.ssafy.e102.eumgil.feature.navigation.NavigationRoute as NavigationScreenRoute
import com.ssafy.e102.eumgil.feature.report.ReportRoute as ReportScreenRoute
import com.ssafy.e102.eumgil.feature.route.RouteSettingEntryRoute
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteRoute
import com.ssafy.e102.eumgil.feature.search.SearchRoute as SearchScreenRoute

fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    composable(route = TopLevelRoute.Map.route) {
        MapRoute(
            onNavigateToSavedRoutes = {
                navController.navigateToTopLevel(TopLevelDestination.SavedRoute)
            },
            onNavigateToMyPage = {
                navController.navigateToTopLevel(TopLevelDestination.MyPage)
            },
            onNavigateToFacilityRouteEntry = {
                navController.navigate(RouteSettingRoute.Setting.route)
            },
            onNavigateToSearch = {
                navController.navigate(SearchRoute.Search.route)
            },
        )
    }

    composable(route = TopLevelRoute.SavedRoute.route) {
        SavedRouteRoute(
            onNavigateToMap = {
                navController.navigateToTopLevel(TopLevelDestination.Map)
            },
        )
    }

    composable(route = TopLevelRoute.MyPage.route) {
        MyPageRoute(
            onNavigateToMap = {
                navController.navigateToTopLevel(TopLevelDestination.Map)
            },
            onNavigateToSavedRoutes = {
                navController.navigateToTopLevel(TopLevelDestination.SavedRoute)
            },
        )
    }

    composable(route = SearchRoute.Search.route) {
        SearchScreenRoute(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToRouteSetting = {
                navController.navigate(RouteSettingRoute.Setting.route) {
                    popUpTo(SearchRoute.Search.route) {
                        inclusive = true
                    }
                }
            },
        )
    }

    composable(route = RouteSettingRoute.Setting.route) {
        // 200 can hand off by updating DestinationSelectionRepository, then navigating here.
        RouteSettingEntryRoute(
            onNavigateBack = {
                navController.popBackStack()
            },
            onStartNavigation = {
                navController.navigate(NavigationRoute.Guidance.route)
            },
        )
    }

    composable(route = ReportRoute.Report.route) {
        ReportScreenRoute(
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }

    composable(route = NavigationRoute.Guidance.route) {
        NavigationScreenRoute(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToMap = {
                navController.navigateToTopLevel(TopLevelDestination.Map)
            },
        )
    }
}

fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route.route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
    }
}
