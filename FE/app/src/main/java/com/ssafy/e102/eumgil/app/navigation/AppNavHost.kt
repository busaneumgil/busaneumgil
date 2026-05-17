package com.ssafy.e102.eumgil.app.navigation

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssafy.e102.eumgil.BuildConfig
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.config.AppEnvironment
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumTopLevelTabBar
import com.ssafy.e102.eumgil.core.model.AuthGateState
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.data.repository.provideProfileUserTypeUpdateRepository
import com.ssafy.e102.eumgil.feature.map.MapKwsEvent
import com.ssafy.e102.eumgil.feature.map.MapKwsViewModel
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType

internal val AppNavHostContentWindowInsets: WindowInsets = WindowInsets(0, 0, 0, 0)

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val appContainer = remember(context) { (context as BusanEumgilApp).appContainer }
    val settingsRepository = remember(appContainer) { appContainer.settingsRepository }
    val authSessionRepository = remember(appContainer) { appContainer.authSessionRepository }
    val authSignupRepository = remember(appContainer) { appContainer.authSignupRepository }
    val profileUserTypeUpdateRepository =
        remember(authSessionRepository, settingsRepository) {
            provideProfileUserTypeUpdateRepository(
                baseUrl = AppEnvironment.baseUrl,
                authSessionRepository = authSessionRepository,
                settingsRepository = settingsRepository,
                isMockMode = AppEnvironment.isMockMode,
            )
        }
    var appStartDestination by remember { mutableStateOf<AppStartDestination?>(null) }
    var bootstrappedAuthGateState by remember { mutableStateOf<AuthGateState?>(null) }
    var bootstrappedInitSettings by remember { mutableStateOf<InitSettings?>(null) }

    LaunchedEffect(authSessionRepository, settingsRepository) {
        val authGateState = authSessionRepository.getAuthGateState()
        val savedSettings = settingsRepository.getInitSettings()
        bootstrappedAuthGateState = authGateState
        bootstrappedInitSettings = savedSettings
        appStartDestination =
            resolveAppStartDestination(
                authGateState = authGateState,
                initSettings = savedSettings,
                forceLowVisionTermsGuide = BuildConfig.FORCE_LOW_VISION_TERMS_GUIDE,
            )
    }

    if (appStartDestination == null || bootstrappedAuthGateState == null || bootstrappedInitSettings == null) {
        AppEntryLoadingScreen(modifier = modifier)
        return
    }

    val startDestination = appStartDestination ?: return
    val initialAuthGateState = bootstrappedAuthGateState ?: return
    val initialInitSettings = bootstrappedInitSettings ?: return
    val navController = rememberNavController()
    val authGateState by
        remember(authSessionRepository) {
            authSessionRepository.observeAuthGateState()
        }.collectAsStateWithLifecycle(initialValue = initialAuthGateState)
    val initSettings by
        remember(settingsRepository) {
            settingsRepository.observeInitSettings()
        }.collectAsStateWithLifecycle(initialValue = initialInitSettings)
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val currentTopLevelRoute = currentRoute.toCurrentTopLevelRoute()
    val isMapFacilityDetailVisible by
        currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow(MAP_FACILITY_DETAIL_VISIBLE_KEY, false)
            ?.collectAsStateWithLifecycle()
            ?: remember { mutableStateOf(false) }
    val isMapVoiceSearchVisible by
        currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow(MAP_VOICE_SEARCH_VISIBLE_KEY, false)
            ?.collectAsStateWithLifecycle()
            ?: remember { mutableStateOf(false) }
    val showTopLevelBar =
        currentTopLevelRoute != null &&
            !isMapFacilityDetailVisible &&
            !isMapVoiceSearchVisible

    val selectedPrimaryUserType = initSettings.selectedPrimaryUserType

    LaunchedEffect(navController, authGateState, currentRoute) {
        if (authGateState.hasSession || authGateState.hasPendingSignup || currentRoute == null) return@LaunchedEffect
        if (currentRoute == AuthRoute.Login.route) return@LaunchedEffect

        navController.navigate(AuthRoute.Login.route) {
            launchSingleTop = true
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
        }
    }

    if (selectedPrimaryUserType == PrimaryUserType.MOBILITY_IMPAIRED.routeValue) {
        MobilityKwsEffect(
            navController = navController,
            shouldPauseForMapVoiceInput = isMapVoiceSearchVisible,
            onNavigateToVoiceInput = {
                navController.navigate(SearchRoute.VoiceInput.createRoute()) {
                    launchSingleTop = true
                }
            },
        )
    }

    Scaffold(
        contentWindowInsets = AppNavHostContentWindowInsets,
        bottomBar = {
            if (showTopLevelBar) {
                EumTopLevelTabBar(
                    destinations = TopLevelDestination.entries,
                    currentRoute = currentTopLevelRoute,
                    onDestinationSelected = { destination ->
                        if (
                            shouldNavigateToTopLevelMapForHomeEntry(
                                currentRoute = currentRoute,
                                destination = destination,
                            )
                        ) {
                            Log.i(
                                APP_NAV_HOST_LOG_TAG,
                                "Map home tab selected from aliased route=$currentRoute; forcing home reentry reset",
                            )
                            navController.navigateToTopLevelMapForHomeEntry()
                        } else {
                            navController.navigateToTopLevel(destination)
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination.route,
            modifier = modifier.padding(innerPadding),
            enterTransition = { appEnterTransition() },
            exitTransition = { appExitTransition() },
            popEnterTransition = { appEnterTransition() },
            popExitTransition = { appExitTransition() },
        ) {
            authNavGraph(
                navController = navController,
                authSessionRepository = authSessionRepository,
                settingsRepository = settingsRepository,
            )
            onboardingNavGraph(
                navController = navController,
                settingsRepository = settingsRepository,
                authSignupRepository = authSignupRepository,
                profileUserTypeUpdateRepository = profileUserTypeUpdateRepository,
            )
            lowVisionNavGraph(navController = navController)
            mainNavGraph(navController = navController)
        }
    }
}

internal fun String?.toCurrentTopLevelRoute(): String? =
    when {
        this == TopLevelRoute.Map.route -> TopLevelRoute.Map.route
        this == TopLevelRoute.SavedRoute.route -> TopLevelRoute.SavedRoute.route
        this == ReportRoute.Report.route -> ReportRoute.Report.route
        this == TopLevelRoute.MyPage.route -> TopLevelRoute.MyPage.route
        this?.startsWith("${TopLevelRoute.MyPage.route}/") == true -> TopLevelRoute.MyPage.route
        this == SearchRoute.Entry.route -> TopLevelRoute.Map.route
        this?.startsWith("search/") == true -> TopLevelRoute.Map.route
        this == NavigationRoute.Guidance.route -> null
        this == ArrivalRoute.Entry.route -> null
        this?.startsWith("route_setting") == true -> null
        else -> null
    }

internal fun shouldNavigateToTopLevelMapForHomeEntry(
    currentRoute: String?,
    destination: TopLevelDestination,
): Boolean =
    destination == TopLevelDestination.Map &&
        currentRoute != TopLevelRoute.Map.route &&
        currentRoute != TopLevelRoute.SavedRoute.route &&
        currentRoute.toCurrentTopLevelRoute() != null

internal fun shouldUseInstantAppDestinationTransitions(): Boolean = true

private fun appEnterTransition(): EnterTransition = EnterTransition.None

private fun appExitTransition(): ExitTransition = ExitTransition.None

private const val APP_NAV_HOST_LOG_TAG = "AppNavHost"
internal const val MAP_FACILITY_DETAIL_VISIBLE_KEY: String = "mapFacilityDetailVisible"

internal fun shouldPauseMapKws(
    currentRoute: String?,
    shouldPauseForMapVoiceInput: Boolean,
): Boolean = currentRoute == SearchRoute.VoiceInput.route || shouldPauseForMapVoiceInput

@Composable
private fun MobilityKwsEffect(
    navController: NavController,
    shouldPauseForMapVoiceInput: Boolean,
    onNavigateToVoiceInput: () -> Unit,
) {
    val kwsViewModel: MapKwsViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnNavigateToVoiceInput by rememberUpdatedState(onNavigateToVoiceInput)
    val currentShouldPauseForMapVoiceInput by rememberUpdatedState(shouldPauseForMapVoiceInput)

    DisposableEffect(lifecycleOwner, kwsViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    if (currentShouldPauseForMapVoiceInput) {
                        kwsViewModel.pauseSpotting()
                    } else {
                        kwsViewModel.resumeSpotting()
                    }

                Lifecycle.Event.ON_PAUSE -> kwsViewModel.pauseSpotting()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(navController, kwsViewModel, shouldPauseForMapVoiceInput) {
        if (
            shouldPauseMapKws(
                currentRoute = navController.currentBackStackEntry?.destination?.route,
                shouldPauseForMapVoiceInput = shouldPauseForMapVoiceInput,
            )
        ) {
            kwsViewModel.pauseSpotting()
        } else {
            kwsViewModel.resumeSpotting()
        }
    }

    LaunchedEffect(navController, shouldPauseForMapVoiceInput) {
        navController.currentBackStackEntryFlow.collect { entry ->
            if (
                shouldPauseMapKws(
                    currentRoute = entry.destination.route,
                    shouldPauseForMapVoiceInput = shouldPauseForMapVoiceInput,
                )
            ) {
                kwsViewModel.pauseSpotting()
            } else {
                kwsViewModel.resumeSpotting()
            }
        }
    }

    LaunchedEffect(kwsViewModel) {
        kwsViewModel.uiEvent.collect { event ->
            when (event) {
                MapKwsEvent.NavigateToVoiceInput -> currentOnNavigateToVoiceInput()
            }
        }
    }
}

@Composable
private fun AppEntryLoadingScreen(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.splash_illustration),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}
