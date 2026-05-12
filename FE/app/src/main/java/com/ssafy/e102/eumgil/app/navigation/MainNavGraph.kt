package com.ssafy.e102.eumgil.app.navigation

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.feature.arrival.ArrivalRoute as ArrivalScreenRoute
import com.ssafy.e102.eumgil.feature.map.MapRoute
import com.ssafy.e102.eumgil.feature.mypage.MyPageAppInfoRoute
import com.ssafy.e102.eumgil.feature.mypage.MyPageReportHistoryRoute
import com.ssafy.e102.eumgil.feature.mypage.MyPageRoute
import com.ssafy.e102.eumgil.feature.navigation.NavigationRoute as NavigationScreenRoute
import com.ssafy.e102.eumgil.feature.navigation.NavigationViewModel as NavigationGuidanceViewModel
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType
import com.ssafy.e102.eumgil.feature.report.ReportRoute as ReportScreenRoute
import com.ssafy.e102.eumgil.feature.route.RouteDetailEntryRoute
import com.ssafy.e102.eumgil.feature.route.RouteSettingEntryRoute
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteRoute
import com.ssafy.e102.eumgil.feature.search.SearchEntryRoute
import com.ssafy.e102.eumgil.feature.search.SearchResultsRoute
import com.ssafy.e102.eumgil.feature.search.SearchVoiceInputRoute
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import com.ssafy.e102.eumgil.feature.tutorial.MobilityTutorialRoute
import com.ssafy.e102.eumgil.feature.tutorial.TutorialEntryPoint
import kotlinx.coroutines.flow.map

fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    composable(route = TopLevelRoute.Map.route) { backStackEntry ->
        val shouldResetForHomeEntry by
            backStackEntry.savedStateHandle
                .getStateFlow(MAP_HOME_REENTRY_RESET_KEY, false)
                .collectAsStateWithLifecycle()
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
                navController.navigate(SearchRoute.Entry.createRoute())
            },
            shouldResetForHomeEntry = shouldResetForHomeEntry,
            onHomeReentryResetConsumed = {
                backStackEntry.savedStateHandle.consumeMapHomeReentryReset()
            },
        )
    }

    composable(route = TopLevelRoute.SavedRoute.route) {
        SavedRouteRoute(
            onNavigateToMap = {
                navController.navigateToTopLevel(TopLevelDestination.Map)
            },
            onNavigateToRouteSetting = { routeOption ->
                navController.navigate(
                    RouteSettingRoute.Setting.createRoute(initialRouteOption = routeOption),
                )
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
            onNavigateToAppInfo = {
                navController.navigate(MyPageSubRoute.AppInfo.route)
            },
        )
    }

    composable(
        route = SearchRoute.Entry.route,
        arguments =
            listOf(
                navArgument(SearchRoute.Entry.ARG_EDITING_TARGET) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val initialEditingTarget =
            backStackEntry.arguments
                ?.getString(SearchRoute.Entry.ARG_EDITING_TARGET)
                .toRouteEditingTargetOrDefault()
        val preserveEntryStateOnReentry =
            backStackEntry.savedStateHandle.get<Boolean>(SEARCH_PRESERVE_ENTRY_STATE_KEY) == true
        backStackEntry.savedStateHandle.set(SEARCH_PRESERVE_ENTRY_STATE_KEY, false)
        SearchEntryRoute(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToResults = { query, editingTarget ->
                navController.navigate(SearchRoute.Results.createRoute(query, editingTarget))
            },
            onNavigateToVoiceInput = {
                navController.navigate(SearchRoute.VoiceInput.createRoute(initialEditingTarget)) {
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
            onNavigateToMapPreview = {
                val didReturnToMap = navController.popBackStack(
                    route = TopLevelRoute.Map.route,
                    inclusive = false,
                )
                if (!didReturnToMap) {
                    navController.navigateToTopLevel(TopLevelDestination.Map)
                }
            },
            onNavigateToRouteBriefing = {
                navController.navigate(resolveSearchResultBriefingRoute()) {
                    popUpTo(SearchRoute.Entry.route) {
                        inclusive = true
                    }
                }
            },
            initialEditingTarget = initialEditingTarget,
            preserveEntryStateOnReentry = preserveEntryStateOnReentry,
        )
    }

    composable(
        route = SearchRoute.Results.route,
        arguments =
            listOf(
                navArgument(SearchRoute.Results.ARG_QUERY) {
                    type = NavType.StringType
                },
                navArgument(SearchRoute.Results.ARG_EDITING_TARGET) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val initialEditingTarget =
            backStackEntry.arguments
                ?.getString(SearchRoute.Results.ARG_EDITING_TARGET)
                .toRouteEditingTargetOrDefault()
        SearchResultsRoute(
            initialQuery = backStackEntry.arguments?.getString(SearchRoute.Results.ARG_QUERY).orEmpty(),
            onNavigateBack = {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(SEARCH_PRESERVE_ENTRY_STATE_KEY, true)
                navController.popBackStack()
            },
            onNavigateToResults = { query, editingTarget ->
                navController.navigate(SearchRoute.Results.createRoute(query, editingTarget)) {
                    launchSingleTop = true
                }
            },
            onNavigateToVoiceInput = {
                navController.navigate(SearchRoute.VoiceInput.createRoute(initialEditingTarget)) {
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
            onNavigateToMapPreview = {
                val didReturnToMap = navController.popBackStack(
                    route = TopLevelRoute.Map.route,
                    inclusive = false,
                )
                if (!didReturnToMap) {
                    navController.navigateToTopLevel(TopLevelDestination.Map)
                }
            },
            onNavigateToRouteBriefing = {
                navController.navigate(resolveSearchResultBriefingRoute()) {
                    popUpTo(SearchRoute.Entry.route) {
                        inclusive = true
                    }
                }
            },
            initialEditingTarget = initialEditingTarget,
        )
    }

    composable(
        route = SearchRoute.VoiceInput.route,
        arguments =
            listOf(
                navArgument(SearchRoute.VoiceInput.ARG_EDITING_TARGET) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val initialEditingTarget =
            backStackEntry.arguments
                ?.getString(SearchRoute.VoiceInput.ARG_EDITING_TARGET)
                .toRouteEditingTargetOrDefault()
        val context = LocalContext.current
        val micPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (!isGranted) navController.popBackStack()
        }
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        SearchVoiceInputRoute(
            onNavigateBack = {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(SEARCH_PRESERVE_ENTRY_STATE_KEY, true)
                navController.popBackStack()
            },
            onNavigateToResults = { query, editingTarget ->
                navController.navigate(SearchRoute.Results.createRoute(query, editingTarget)) {
                    launchSingleTop = true
                    popUpTo(SearchRoute.Entry.route) {
                        inclusive = false
                    }
                }
            },
            initialEditingTarget = initialEditingTarget,
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
                navArgument(RouteSettingRoute.Setting.ARG_INITIAL_ROUTE_OPTION) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val autoStartNavigation =
            backStackEntry.arguments?.getBoolean(RouteSettingRoute.Setting.ARG_AUTO_START_NAVIGATION) ?: false
        val initialRouteOption =
            backStackEntry.arguments
                ?.getString(RouteSettingRoute.Setting.ARG_INITIAL_ROUTE_OPTION)
                ?.let(RouteOption::fromValue)
        val navigationViewModel = rememberNavigationGuidanceViewModel()

        RouteSettingEntryRoute(
            autoStartNavigation = autoStartNavigation,
            initialRouteOption = initialRouteOption,
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToSearch = { editingTarget ->
                navController.navigate(SearchRoute.Entry.createRoute(editingTarget)) {
                    launchSingleTop = true
                }
            },
            onNavigateToRouteDetail = { routeOption ->
                navController.navigate(RouteSettingRoute.Detail.createRoute(routeOption))
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

    composable(
        route = RouteSettingRoute.Detail.route,
        arguments =
            listOf(
                navArgument(RouteSettingRoute.Detail.ARG_ROUTE_OPTION) {
                    type = NavType.StringType
                },
            ),
    ) { backStackEntry ->
        val routeOption =
            backStackEntry.arguments
                ?.getString(RouteSettingRoute.Detail.ARG_ROUTE_OPTION)
                ?.toRouteOptionOrDefault()
                ?: RouteOption.SAFE
        val navigationViewModel = rememberNavigationGuidanceViewModel()

        RouteDetailEntryRoute(
            routeOption = routeOption,
            onNavigateBack = {
                navController.popBackStack()
            },
            onStartNavigation = { request ->
                navigationViewModel.bindNavigationRequest(request)
                navController.navigate(NavigationRoute.Guidance.route) {
                    popUpTo(RouteSettingRoute.Detail.createRoute(routeOption)) {
                        inclusive = true
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
            onNavigateToMap = {
                navController.navigateToTopLevel(TopLevelDestination.Map)
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

    composable(route = MyPageSubRoute.AppInfo.route) {
        MyPageAppInfoRoute(
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
            onNavigateToLogin = {
                navController.navigate(AuthRoute.Login.route) {
                    launchSingleTop = true
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                }
            },
            onNavigateToGuide = {
                navController.navigate(resolveAppInfoGuideRoute())
            },
        )
    }

    composable(route = TutorialRoute.Guide.route) {
        MobilityTutorialRoute(
            entryPoint = TutorialEntryPoint.GUIDE,
            onCompleted = {
                val didPopToAppInfo =
                    navController.popBackStack(
                        route = resolveTutorialGuideCompletedRoute(),
                        inclusive = false,
                    )
                if (!didPopToAppInfo) {
                    navController.navigate(resolveTutorialGuideCompletedRoute()) {
                        launchSingleTop = true
                    }
                }
            },
        )
    }

    composable(route = ArrivalRoute.Entry.route) {
        val context = LocalContext.current
        val settingsRepository =
            remember(context) {
                (context.applicationContext as BusanEumgilApp).appContainer.settingsRepository
            }
        val selectedPrimaryUserType by
            remember(settingsRepository) {
                settingsRepository
                    .observeInitSettings()
                    .map { initSettings -> initSettings.selectedPrimaryUserType }
            }.collectAsStateWithLifecycle(initialValue = null)
        ArrivalScreenRoute(
            onNavigateToMap = {
                navController.navigateToArrivalHome(selectedPrimaryUserType)
            },
            onNavigateToSearch = {
                navController.navigate(SearchRoute.Entry.createRoute()) {
                    launchSingleTop = true
                    popUpTo(ArrivalRoute.Entry.route) {
                        inclusive = true
                    }
                }
            },
        )
    }

    composable(route = NavigationRoute.Guidance.route) {
        val context = LocalContext.current
        val settingsRepository =
            remember(context) {
                (context.applicationContext as BusanEumgilApp).appContainer.settingsRepository
            }
        val selectedPrimaryUserType by
            remember(settingsRepository) {
                settingsRepository
                    .observeInitSettings()
                    .map { initSettings -> initSettings.selectedPrimaryUserType }
            }.collectAsStateWithLifecycle(initialValue = null)
        val useLowVisionUi = shouldUseLowVisionNavigationUi(selectedPrimaryUserType)

        NavigationScreenRoute(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToRouteDetail = { routeOption ->
                navController.navigate(RouteSettingRoute.Detail.createRoute(routeOption))
            },
            onNavigateToMap = {
                navController.navigateToTopLevelMapForHomeEntry()
            },
            onNavigateToSavedRoute = {
                if (useLowVisionUi) {
                    navController.navigate(resolveNavigationSavedRoute(selectedPrimaryUserType)) {
                        launchSingleTop = true
                        popUpTo(NavigationRoute.Guidance.route) {
                            inclusive = true
                        }
                    }
                } else {
                    navController.navigateToTopLevel(TopLevelDestination.SavedRoute)
                }
            },
            onNavigateToArrival = {
                navController.navigate(resolveNavigationCompletionRoute(selectedPrimaryUserType)) {
                    launchSingleTop = true
                    popUpTo(NavigationRoute.Guidance.route) {
                        inclusive = true
                    }
                }
            },
            useLowVisionUi = useLowVisionUi,
        )
    }
}

internal fun resolveNavigationSavedRoute(selectedPrimaryUserType: String?): String =
    if (shouldUseLowVisionNavigationUi(selectedPrimaryUserType)) {
        LowVisionRoute.Bookmark.route
    } else {
        TopLevelRoute.SavedRoute.route
    }

internal fun resolveArrivalHomeRoute(selectedPrimaryUserType: String?): String =
    if (shouldUseLowVisionNavigationUi(selectedPrimaryUserType)) {
        LowVisionRoute.Home.route
    } else {
        TopLevelRoute.Map.route
    }

internal fun resolveSearchResultBriefingRoute(): String = LowVisionRoute.RouteBriefing.route

internal fun resolveAppInfoGuideRoute(): String = TutorialRoute.Guide.route

internal fun shouldUseLowVisionNavigationUi(selectedPrimaryUserType: String?): Boolean =
    selectedPrimaryUserType == PrimaryUserType.LOW_VISION.routeValue

private const val SEARCH_PRESERVE_ENTRY_STATE_KEY: String = "searchPreserveEntryState"
private const val MAP_HOME_REENTRY_RESET_KEY: String = "mapHomeReentryReset"

internal data class TopLevelNavigationPolicy(
    val launchSingleTop: Boolean,
    val restoreState: Boolean,
    val saveState: Boolean,
)

internal val DefaultTopLevelNavigationPolicy: TopLevelNavigationPolicy =
    TopLevelNavigationPolicy(
        launchSingleTop = true,
        restoreState = true,
        saveState = true,
    )

fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route.route) {
        launchSingleTop = DefaultTopLevelNavigationPolicy.launchSingleTop
        restoreState = DefaultTopLevelNavigationPolicy.restoreState
        popUpTo(graph.findStartDestination().id) {
            saveState = DefaultTopLevelNavigationPolicy.saveState
        }
    }
}

internal fun NavController.navigateToTopLevelMapForHomeEntry() {
    navigateToTopLevel(TopLevelDestination.Map)
    getBackStackEntry(TopLevelRoute.Map.route).savedStateHandle.requestMapHomeReentryReset()
}

internal fun NavHostController.navigateToArrivalHome(selectedPrimaryUserType: String?) {
    if (!shouldUseLowVisionNavigationUi(selectedPrimaryUserType)) {
        navigateToTopLevelMapForHomeEntry()
        return
    }

    val didPopToLowVisionHome =
        popBackStack(
            route = LowVisionRoute.Home.route,
            inclusive = false,
        )
    if (!didPopToLowVisionHome) {
        navigate(resolveArrivalHomeRoute(selectedPrimaryUserType)) {
            launchSingleTop = true
            popUpTo(ArrivalRoute.Entry.route) {
                inclusive = true
            }
        }
    }
}

internal fun SavedStateHandle.requestMapHomeReentryReset() {
    set(MAP_HOME_REENTRY_RESET_KEY, true)
}

internal fun SavedStateHandle.consumeMapHomeReentryReset(): Boolean {
    val shouldReset = get<Boolean>(MAP_HOME_REENTRY_RESET_KEY) == true
    if (shouldReset) {
        set(MAP_HOME_REENTRY_RESET_KEY, false)
    }
    return shouldReset
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }

private fun String.toRouteOptionOrDefault(): RouteOption =
    runCatching { RouteOption.valueOf(this) }.getOrDefault(RouteOption.SAFE)

private fun String?.toRouteEditingTargetOrDefault(): RouteEditingTarget =
    this
        ?.let { value -> runCatching { RouteEditingTarget.valueOf(value) }.getOrNull() }
        ?: RouteEditingTarget.DESTINATION

@androidx.compose.runtime.Composable
private fun rememberNavigationGuidanceViewModel(): NavigationGuidanceViewModel {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    val currentLocationManager = remember(context) {
        (context.applicationContext as BusanEumgilApp).appContainer.currentLocationManager
    }
    val bookmarkRepository = remember(context) {
        (context.applicationContext as BusanEumgilApp).appContainer.bookmarkRepository
    }
    val routeRepository = remember(context) {
        (context.applicationContext as BusanEumgilApp).appContainer.routeRepository
    }
    val navigationViewModelFactory = remember(currentLocationManager, bookmarkRepository, routeRepository) {
        NavigationGuidanceViewModel.provideFactory(
            currentLocationManager = currentLocationManager,
            bookmarkRepository = bookmarkRepository,
            routeRepository = routeRepository,
        )
    }

    return remember(activity, navigationViewModelFactory) {
        val owner = checkNotNull(activity) { "RouteSettingRoute requires a ComponentActivity host." }
        ViewModelProvider(owner, navigationViewModelFactory)[NavigationGuidanceViewModel::class.java]
    }
}
