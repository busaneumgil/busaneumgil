package com.ssafy.e102.eumgil.app.navigation

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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssafy.e102.eumgil.BuildConfig
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.config.AppEnvironment
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumTopLevelTabBar
import com.ssafy.e102.eumgil.data.repository.provideProfileUserTypeUpdateRepository
import com.ssafy.e102.eumgil.feature.map.MapKwsEvent
import com.ssafy.e102.eumgil.feature.map.MapKwsViewModel
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType
import kotlinx.coroutines.flow.map

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

    LaunchedEffect(authSessionRepository, settingsRepository) {
        val authGateState = authSessionRepository.getAuthGateState()
        val savedSettings = settingsRepository.getInitSettings()
        appStartDestination =
            resolveAppStartDestination(
                authGateState = authGateState,
                initSettings = savedSettings,
                forceLowVisionTermsGuide = BuildConfig.FORCE_LOW_VISION_TERMS_GUIDE,
            )
    }

    if (appStartDestination == null) {
        AppEntryLoadingScreen(modifier = modifier)
        return
    }

    val startDestination = appStartDestination ?: return
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val currentTopLevelRoute = currentRoute.toCurrentTopLevelRoute()
    val showTopLevelBar = currentTopLevelRoute != null

    val selectedPrimaryUserType by remember(settingsRepository) {
        settingsRepository.observeInitSettings().map { it.selectedPrimaryUserType }
    }.collectAsStateWithLifecycle(initialValue = null)

    if (selectedPrimaryUserType == PrimaryUserType.MOBILITY_IMPAIRED.routeValue) {
        MobilityKwsEffect(
            navController = navController,
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
                        navController.navigateToTopLevel(destination)
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination.route,
            modifier = modifier.padding(innerPadding),
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
        this == ArrivalRoute.Entry.route -> TopLevelRoute.Map.route
        this?.startsWith("route_setting") == true -> null
        else -> null
    }

@Composable
private fun MobilityKwsEffect(
    navController: NavController,
    onNavigateToVoiceInput: () -> Unit,
) {
    val kwsViewModel: MapKwsViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnNavigateToVoiceInput by rememberUpdatedState(onNavigateToVoiceInput)

    // 앱 백그라운드 전환 시 마이크 해제 / 복귀 시 재시작
    DisposableEffect(lifecycleOwner, kwsViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> kwsViewModel.resumeSpotting()
                Lifecycle.Event.ON_PAUSE -> kwsViewModel.pauseSpotting()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // VoiceInput 바텀시트 닫힐 때 KWS 재시작
    // (Activity ON_RESUME은 같은 앱 내 화면 전환 시 발생하지 않으므로 별도 처리)
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            if (entry.destination.route != SearchRoute.VoiceInput.route) {
                kwsViewModel.resumeSpotting()
            } else {
                kwsViewModel.pauseSpotting()
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
        contentDescription = stringResource(id = R.string.app_name),
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}
