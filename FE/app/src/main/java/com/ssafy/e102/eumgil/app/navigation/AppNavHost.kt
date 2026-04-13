package com.ssafy.e102.eumgil.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ssafy.e102.eumgil.feature.bootstrap.BootstrapScreen

private const val BOOTSTRAP_ROUTE = "bootstrap"

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = BOOTSTRAP_ROUTE,
        modifier = modifier,
    ) {
        composable(route = BOOTSTRAP_ROUTE) {
            BootstrapScreen()
        }
    }
}
