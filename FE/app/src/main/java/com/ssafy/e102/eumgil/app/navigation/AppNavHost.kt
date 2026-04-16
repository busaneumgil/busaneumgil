package com.ssafy.e102.eumgil.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumTopLevelTabBar
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val appContainer = remember(context) { (context as BusanEumgilApp).appContainer }
    val settingsRepository = remember(appContainer) { appContainer.settingsRepository }
    var appStartDestination by remember { mutableStateOf<AppStartDestination?>(null) }
    var initialSettings by remember { mutableStateOf<InitSettings?>(null) }

    LaunchedEffect(settingsRepository) {
        val savedSettings = settingsRepository.getInitSettings()
        initialSettings = savedSettings
        appStartDestination = resolveAppStartDestination(savedSettings)
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
    val showTopLevelBar =
        TopLevelDestination.entries.any { destination ->
            destination.route.route == currentRoute
        }

    Scaffold(
        bottomBar = {
            if (showTopLevelBar) {
                EumTopLevelTabBar(
                    destinations = TopLevelDestination.entries,
                    currentRoute = currentRoute,
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
            onboardingNavGraph(
                navController = navController,
                settingsRepository = settingsRepository,
                initialSettings = restoredSettings,
            )
            mainNavGraph(navController = navController)
        }
    }
}

@Composable
private fun AppEntryLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(EumSpacing.xLarge))
            Text(
                text = stringResource(id = R.string.app_entry_loading),
                modifier = Modifier.padding(top = EumSpacing.medium),
            )
        }
    }
}
