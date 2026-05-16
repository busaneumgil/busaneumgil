package com.ssafy.e102.eumgil.app.navigation

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.location.LocationPermissionState
import com.ssafy.e102.eumgil.core.location.locationPermissions
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.permission.MICROPHONE_PERMISSION
import com.ssafy.e102.eumgil.core.permission.MicrophonePermissionState
import com.ssafy.e102.eumgil.core.permission.resolveMicrophonePermissionState
import com.ssafy.e102.eumgil.feature.arrival.ArrivalRoute as ArrivalScreenRoute
import com.ssafy.e102.eumgil.feature.map.MapRoute
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
            viewModelStoreOwner = backStackEntry,
            onNavigateToSavedRoutes = {
                navController.navigateToTopLevel(TopLevelDestination.SavedRoute)
            },
            onNavigateToMyPage = {
                navController.navigateToTopLevel(TopLevelDestination.MyPage)
            },
            onNavigateToRouteSetting = {
                navController.navigateToRouteSettingPermissionGate()
            },
            onNavigateToSearch = { editingTarget ->
                navController.navigate(SearchRoute.Entry.createRoute(editingTarget))
            },
            shouldResetForHomeEntry = shouldResetForHomeEntry,
            onHomeReentryResetConsumed = {
                backStackEntry.savedStateHandle.consumeMapHomeReentryReset()
            },
            onFacilityDetailVisibilityChanged = { isVisible ->
                backStackEntry.savedStateHandle[MAP_FACILITY_DETAIL_VISIBLE_KEY] = isVisible
            },
        )
    }

    composable(route = TopLevelRoute.SavedRoute.route) {
        val navigationViewModel = rememberNavigationGuidanceViewModel()
        SavedRouteRoute(
            onNavigateToMap = {
                navController.navigateToTopLevel(TopLevelDestination.Map)
            },
            onNavigateToNavigation = { request ->
                navigationViewModel.bindNavigationRequest(request)
                navController.navigate(NavigationRoute.Guidance.route)
            },
            onNavigateToRouteDetail = { request ->
                navigationViewModel.bindNavigationRequest(request)
                navController.navigate(
                    RouteSettingRoute.Detail.createRoute(
                        routeOption = request.selectedRoute.routeOption,
                        fromNavigation = true,
                    ),
                )
            },
            onNavigateToRouteSetting = { routeOption ->
                navController.navigateToRouteSettingPermissionGate(initialRouteOption = routeOption)
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
            onNavigateToGuide = {
                navController.navigate(resolveMyPageGuideRoute())
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
                navController.navigateToRouteSettingPermissionGate {
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
                navController.navigateToRouteSettingPermissionGate {
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
            when (context.resolveMicrophonePermissionState()) {
                MicrophonePermissionState.GRANTED -> Unit
                MicrophonePermissionState.DENIED -> micPermissionLauncher.launch(MICROPHONE_PERMISSION)
                MicrophonePermissionState.UNAVAILABLE -> navController.popBackStack()
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
        route = RouteSettingRoute.PermissionGate.route,
        arguments =
            listOf(
                navArgument(RouteSettingRoute.PermissionGate.ARG_AUTO_START_NAVIGATION) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(RouteSettingRoute.PermissionGate.ARG_INITIAL_ROUTE_OPTION) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val context = LocalContext.current
        val appContainer =
            remember(context.applicationContext) {
                (context.applicationContext as BusanEumgilApp).appContainer
            }
        val locationPermissionManager = remember(appContainer) { appContainer.locationPermissionManager }
        val autoStartNavigation =
            backStackEntry.arguments?.getBoolean(RouteSettingRoute.PermissionGate.ARG_AUTO_START_NAVIGATION) ?: false
        val initialRouteOption =
            backStackEntry.arguments
                ?.getString(RouteSettingRoute.PermissionGate.ARG_INITIAL_ROUTE_OPTION)
                ?.let(RouteOption::fromValue)
        val permissionLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
            ) {
                locationPermissionManager.refreshPermissionState()
                navController.navigateToRouteSettingFromPermissionGate(
                    gateDestinationId = backStackEntry.destination.id,
                    autoStartNavigation = autoStartNavigation,
                    initialRouteOption = initialRouteOption,
                )
            }

        LaunchedEffect(
            locationPermissionManager,
            autoStartNavigation,
            initialRouteOption,
        ) {
            locationPermissionManager.refreshPermissionState()
            when (locationPermissionManager.permissionState.value) {
                is LocationPermissionState.Granted,
                is LocationPermissionState.Unavailable,
                    ->
                    navController.navigateToRouteSettingFromPermissionGate(
                        gateDestinationId = backStackEntry.destination.id,
                        autoStartNavigation = autoStartNavigation,
                        initialRouteOption = initialRouteOption,
                    )

                LocationPermissionState.Denied -> permissionLauncher.launch(locationPermissions)
            }
        }
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
                navArgument(RouteSettingRoute.Setting.ARG_LOCATION_PERMISSION_PRECHECKED) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) { backStackEntry ->
        val autoStartNavigation =
            backStackEntry.arguments?.getBoolean(RouteSettingRoute.Setting.ARG_AUTO_START_NAVIGATION) ?: false
        val initialRouteOption =
            backStackEntry.arguments
                ?.getString(RouteSettingRoute.Setting.ARG_INITIAL_ROUTE_OPTION)
                ?.let(RouteOption::fromValue)
        val locationPermissionPrechecked =
            backStackEntry.arguments
                ?.getBoolean(RouteSettingRoute.Setting.ARG_LOCATION_PERMISSION_PRECHECKED)
                ?: false
        val navigationViewModel = rememberNavigationGuidanceViewModel()

        RouteSettingEntryRoute(
            autoStartNavigation = autoStartNavigation,
            initialRouteOption = initialRouteOption,
            requestLocationPermissionIfNeeded = !locationPermissionPrechecked,
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToMap = {
                navController.navigateToTopLevelMapForHomeEntry()
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
                navArgument(RouteSettingRoute.Detail.ARG_FROM_NAVIGATION) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) { backStackEntry ->
        val routeOption =
            backStackEntry.arguments
                ?.getString(RouteSettingRoute.Detail.ARG_ROUTE_OPTION)
                ?.toRouteOptionOrDefault()
                ?: RouteOption.SAFE
        val fromNavigation =
            backStackEntry.arguments?.getBoolean(RouteSettingRoute.Detail.ARG_FROM_NAVIGATION) ?: false
        val navigationViewModel = rememberNavigationGuidanceViewModel()

        RouteDetailEntryRoute(
            routeOption = routeOption,
            hydrateFromNavigation = fromNavigation,
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToMap = {
                navController.navigateToTopLevelMapForHomeEntry()
            },
            onStartNavigation = { request ->
                navigationViewModel.bindNavigationRequest(request)
                navController.navigate(NavigationRoute.Guidance.route) {
                    popUpTo(RouteSettingRoute.Detail.createRoute(routeOption, fromNavigation = fromNavigation)) {
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
                // ReportHistory는 마이페이지의 하위 경로이므로, 제보 탭 stack에 push하지 않고
                // 먼저 마이페이지 탭으로 switch한 다음 그 위에 push한다.
                // 이렇게 해야 사용자가 다시 제보 탭을 눌렀을 때 ReportHistory가 따라오지 않고
                // 깨끗한 제보 폼(6 grid)으로 돌아간다.
                navController.navigateToTopLevel(TopLevelDestination.MyPage)
                navController.navigate(MyPageSubRoute.ReportHistory.route)
            },
            onNavigateToMap = {
                navController.navigateToTopLevelMapForHomeEntry()
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
                // 제보 작성은 제보 탭의 top-level destination이므로 마이페이지 stack에 push하지 말고
                // 정상적으로 제보 탭으로 switch한다.
                navController.navigateToTopLevel(TopLevelDestination.Report)
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
                navController.navigate(RouteSettingRoute.Detail.createRoute(routeOption, fromNavigation = true))
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

internal fun resolveMyPageGuideRoute(): String = TutorialRoute.Guide.route

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
    val didPopToMap =
        popBackStack(
            route = TopLevelRoute.Map.route,
            inclusive = false,
        )
    if (!didPopToMap) {
        navigateToTopLevel(TopLevelDestination.Map)
    }
    getBackStackEntry(TopLevelRoute.Map.route).savedStateHandle.requestMapHomeReentryReset()
}

private fun NavController.navigateToRouteSettingPermissionGate(
    autoStartNavigation: Boolean = false,
    initialRouteOption: RouteOption? = null,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(
        RouteSettingRoute.PermissionGate.createRoute(
            autoStartNavigation = autoStartNavigation,
            initialRouteOption = initialRouteOption,
        ),
        builder,
    )
}

private fun NavController.navigateToRouteSettingFromPermissionGate(
    gateDestinationId: Int,
    autoStartNavigation: Boolean,
    initialRouteOption: RouteOption?,
) {
    navigate(
        RouteSettingRoute.Setting.createRoute(
            autoStartNavigation = autoStartNavigation,
            initialRouteOption = initialRouteOption,
            locationPermissionPrechecked = true,
        ),
    ) {
        launchSingleTop = true
        popUpTo(gateDestinationId) {
            inclusive = true
        }
    }
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
internal fun rememberNavigationGuidanceViewModel(): NavigationGuidanceViewModel {
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
