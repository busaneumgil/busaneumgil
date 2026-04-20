package com.ssafy.e102.eumgil.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssafy.e102.eumgil.feature.map.MapRoute
import com.ssafy.e102.eumgil.feature.mypage.MyPageRoute
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
            onNavigateToMyPage = {
                navController.navigateToTopLevel(TopLevelDestination.MyPage)
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
            onNavigateToMap = {
                navController.popBackStack()
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
