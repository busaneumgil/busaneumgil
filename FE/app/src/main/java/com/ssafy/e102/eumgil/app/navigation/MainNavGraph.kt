package com.ssafy.e102.eumgil.app.navigation

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.feature.map.MapRoute
import com.ssafy.e102.eumgil.feature.mypage.MyPageReportHistoryRoute
import com.ssafy.e102.eumgil.feature.mypage.MyPageRoute
import com.ssafy.e102.eumgil.feature.navigation.NavigationRoute as NavigationScreenRoute
import com.ssafy.e102.eumgil.feature.navigation.NavigationViewModel as NavigationGuidanceViewModel
import com.ssafy.e102.eumgil.feature.report.ReportRoute as ReportScreenRoute
import com.ssafy.e102.eumgil.feature.route.RouteSettingEntryRoute
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteRoute
import com.ssafy.e102.eumgil.feature.search.SearchEntryRoute
import com.ssafy.e102.eumgil.feature.search.SearchResultsRoute

fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    composable(route = TopLevelRoute.Map.route) {
        MapRoute(
            onNavigateToSavedRoutes = {
                navController.navigateToTopLevel(TopLevelDestination.SavedRoute)
            },
            onNavigateToMyPage = {
                navController.navigateToTopLevel(TopLevelDestination.MyPage)
            },
            onNavigateToRouteSetting = {
                navController.navigate(RouteSettingRoute.Setting.createRoute())
            },
            onNavigateToSearch = {
                navController.navigate(SearchRoute.Entry.route)
            },
        )
    }

    composable(route = TopLevelRoute.SavedRoute.route) {
        SavedRouteRoute(
            onNavigateToMap = {
                navController.navigateToTopLevel(TopLevelDestination.Map)
            },
            onNavigateToRouteSetting = {
                navController.navigate(RouteSettingRoute.Setting.createRoute())
            },
        )
    }

    composable(route = TopLevelRoute.MyPage.route) {
        MyPageRoute(
            onNavigateToUserTypePrimary = {
                navController.navigate(OnboardingRoute.ProfileUserTypePrimary.route)
            },
            onNavigateToLogin = {
                navController.navigate(AuthRoute.Login.route) {
                    launchSingleTop = true
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                }
            },
            onNavigateToReportHistory = {
                navController.navigate(MyPageSubRoute.ReportHistory.route)
            },
        )
    }

    composable(route = SearchRoute.Entry.route) {
        SearchEntryRoute(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToResults = { query ->
                navController.navigate(SearchRoute.Results.createRoute(query))
            },
            onNavigateToRouteSetting = {
                navController.navigate(RouteSettingRoute.Setting.createRoute()) {
                    popUpTo(SearchRoute.Entry.route) {
                        inclusive = true
                    }
                }
            },
        )
    }

    composable(
        route = SearchRoute.Results.route,
        arguments =
            listOf(
                navArgument(SearchRoute.Results.ARG_QUERY) {
                    type = NavType.StringType
                },
            ),
    ) { backStackEntry ->
        SearchResultsRoute(
            initialQuery = backStackEntry.arguments?.getString(SearchRoute.Results.ARG_QUERY).orEmpty(),
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToResults = { query ->
                navController.navigate(SearchRoute.Results.createRoute(query)) {
                    launchSingleTop = true
                }
            },
            onNavigateToRouteSetting = {
                navController.navigate(RouteSettingRoute.Setting.createRoute()) {
                    popUpTo(SearchRoute.Entry.route) {
                        inclusive = true
                    }
                }
            },
        )
    }

    composable(
        route = RouteSettingRoute.Setting.route,
        arguments =
            listOf(
                navArgument(RouteSettingRoute.Setting.ARG_AUTO_START_NAVIGATION) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) { backStackEntry ->
        val autoStartNavigation =
            backStackEntry.arguments?.getBoolean(RouteSettingRoute.Setting.ARG_AUTO_START_NAVIGATION) ?: false
        val context = LocalContext.current
        val activity = remember(context) { context.findComponentActivity() }
        val currentLocationManager = remember(context) {
            (context.applicationContext as BusanEumgilApp).appContainer.currentLocationManager
        }
        val bookmarkRepository = remember(context) {
            (context.applicationContext as BusanEumgilApp).appContainer.bookmarkRepository
        }
        val navigationViewModelFactory = remember(currentLocationManager, bookmarkRepository) {
            NavigationGuidanceViewModel.provideFactory(
                currentLocationManager = currentLocationManager,
                bookmarkRepository = bookmarkRepository,
            )
        }
        val navigationViewModel =
            remember(activity, navigationViewModelFactory) {
                val owner = checkNotNull(activity) { "RouteSettingRoute requires a ComponentActivity host." }
                ViewModelProvider(owner, navigationViewModelFactory)[NavigationGuidanceViewModel::class.java]
            }

        RouteSettingEntryRoute(
            autoStartNavigation = autoStartNavigation,
            onNavigateBack = {
                navController.popBackStack()
            },
            onStartNavigation = { request ->
                navigationViewModel.bindNavigationRequest(request)
                navController.navigate(NavigationRoute.Guidance.route) {
                    if (autoStartNavigation) {
                        popUpTo(RouteSettingRoute.Setting.route) {
                            inclusive = true
                        }
                    }
                }
            },
        )
    }

    composable(route = ReportRoute.Report.route) {
        ReportScreenRoute(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToReportHistory = {
                navController.navigate(MyPageSubRoute.ReportHistory.route)
            },
        )
    }

    composable(route = MyPageSubRoute.ReportHistory.route) {
        MyPageReportHistoryRoute(
            onNavigateBack = {
                val didPopToMyPage =
                    navController.popBackStack(
                        route = TopLevelRoute.MyPage.route,
                        inclusive = false,
                    )
                if (!didPopToMyPage) {
                    navController.navigateToTopLevel(TopLevelDestination.MyPage)
                }
            },
            onNavigateToReport = {
                navController.navigate(ReportRoute.Report.route)
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
            onNavigateToSavedRoute = {
                navController.navigateToTopLevel(TopLevelDestination.SavedRoute)
            },
            onNavigateToLowVisionHome = {
                navController.navigate(resolveNavigationCompletionRoute()) {
                    launchSingleTop = true
                    popUpTo(NavigationRoute.Guidance.route) {
                        inclusive = true
                    }
                }
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

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
