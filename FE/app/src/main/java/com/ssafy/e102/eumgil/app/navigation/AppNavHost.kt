package com.ssafy.e102.eumgil.app.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumTopLevelTabBar
import com.ssafy.e102.eumgil.core.model.InitSettings

internal val AppNavHostContentWindowInsets: WindowInsets = WindowInsets(0, 0, 0, 0)

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val appContainer = remember(context) { (context as BusanEumgilApp).appContainer }
    val settingsRepository = remember(appContainer) { appContainer.settingsRepository }
    val authSessionRepository = remember(appContainer) { appContainer.authSessionRepository }
    var appStartDestination by remember { mutableStateOf<AppStartDestination?>(null) }
    var initialSettings by remember { mutableStateOf<InitSettings?>(null) }

    LaunchedEffect(authSessionRepository, settingsRepository) {
        var authGateState = authSessionRepository.getAuthGateState()
        if (authGateState.hasSession && !authGateState.isProfileCompleted) {
            authSessionRepository.markProfileCompleted()
            authGateState = authSessionRepository.getAuthGateState()
        }
        val savedSettings = settingsRepository.getInitSettings()
        initialSettings = savedSettings
        appStartDestination =
            resolveAppStartDestination(
                authGateState = authGateState,
                initSettings = savedSettings,
            )
    }

    if (appStartDestination == null || initialSettings == null) {
        AppEntryLoadingScreen(modifier = modifier)
        return
    }
    val startDestination = appStartDestination ?: return
    val restoredSettings = initialSettings ?: return

    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // 인증·온보딩·저시력 전용 플로우에서는 탭 바를 숨깁니다.
    val showTopLevelBar = currentRoute != null &&
        !currentRoute.startsWith("auth/") &&
        !currentRoute.startsWith("onboarding/") &&
        !currentRoute.startsWith("low_vision/")

    // 검색·경로 설정·안내 화면에서도 홈(지도) 탭이 활성 상태로 보입니다.
    val effectiveActiveRoute = when {
        currentRoute == SearchRoute.Search.route -> TopLevelRoute.Map.route
        currentRoute == NavigationRoute.Guidance.route -> TopLevelRoute.Map.route
        currentRoute?.startsWith("route_setting") == true -> TopLevelRoute.Map.route
        else -> currentRoute
    }

    Scaffold(
        contentWindowInsets = AppNavHostContentWindowInsets,
        bottomBar = {
            if (showTopLevelBar) {
                EumTopLevelTabBar(
                    destinations = TopLevelDestination.entries,
                    currentRoute = effectiveActiveRoute,
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
                se