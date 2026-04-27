package com.ssafy.e102.eumgil.app.navigation

import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssafy.e102.eumgil.data.repository.AuthSessionRepository
import com.ssafy.e102.eumgil.data.repository.SettingsRepository
import com.ssafy.e102.eumgil.feature.auth.LoginRoute
import com.ssafy.e102.eumgil.feature.auth.ProfileSetupRoute
import kotlinx.coroutines.launch

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    authSessionRepository: AuthSessionRepository,
    settingsRepository: SettingsRepository,
) {
    composable(route = AuthRoute.Login.route) {
        LoginRoute()
    }

    composable(route = AuthRoute.ProfileSetup.route) {
        val coroutineScope = rememberCoroutineScope()

        ProfileSetupRoute(
            onProfileSetupCompleted = {
                coroutineScope.launch {
                    authSessionRepository.markProfileCompleted()
                    val nextDestination =
                        resolveAppStartDestination(
                            authSessionSnapshot = authSessionRepository.getAuthSessionSnapshot(),
                            initSettings = settingsRepository.getInitSettings(),
                        )

                    navController.navigate(nextDestination.route) {
                        launchSingleTop = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            },
        )
    }
}
