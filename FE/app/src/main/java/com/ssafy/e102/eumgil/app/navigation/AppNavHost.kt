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
    val currentTopLevelRoute = currentRoute.toCurrentTopLevelRoute()
    val showTopLevelBar = currentTopLevelRoute != null

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
                initialSettings = restoredSettings,
            )
            lowVisionNavGraph(navController = navController)
            mainNavGraph(navController = navController)
        }
    }
}

private fun String?.toCurrentTopLevelRoute(): String? =
    when {
        this == TopLevelRoute.Map.route -> TopLevelRoute.Map.route
        this == TopLevelRoute.SavedRoute.route -> TopLevelRoute.SavedRoute.route
        this == ReportRoute.Report.route -> ReportRoute.Report.route
        this == TopLevelRoute.MyPage.route -> TopLevelRoute.MyPage.route
        this?.startsWith("${TopLevelRoute.MyPage.route}/") == true -> TopLevelRoute.MyPage.route
        else -> null
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
