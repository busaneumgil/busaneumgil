package com.ssafy.e102.eumgil.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssafy.e102.eumgil.feature.auth.LoginRoute
import com.ssafy.e102.eumgil.feature.auth.ProfileSetupRoute

fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    composable(route = AuthRoute.Login.route) {
        LoginRoute(
            onLoginCompleted = {
                navController.navigate(AuthRoute.ProfileSetup.route) {
                    launchSingleTop = true
                    popUpTo(AuthRoute.Login.route) {
                        inclusive = true
                    }
                }
            },
        )
    }

    composable(route = AuthRoute.ProfileSetup.route) {
        ProfileSetupRoute()
    }
}
