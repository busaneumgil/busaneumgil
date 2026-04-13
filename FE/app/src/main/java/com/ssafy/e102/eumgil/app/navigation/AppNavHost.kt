package com.ssafy.e102.eumgil.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumTopLevelTabBar

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            EumTopLevelTabBar(
                destinations = TopLevelDestination.entries,
                currentRoute = currentRoute,
                onDestinationSelected = { destination ->
                    navController.navigateToTopLevel(destination)
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelRoute.Map.route,
            modifier = modifier.padding(innerPadding),
        ) {
            mainNavGraph(navController = navController)
        }
    }
}
