package com.ssafy.e102.eumgil.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ssafy.e102.eumgil.feature.auth.LoginRoute
import com.ssafy.e102.eumgil.feature.auth.ProfileSetupRoute

fun NavGraphBuilder.authNavGraph() {
    composable(route = AuthRoute.Login.route) {
        LoginRoute()
    }

    composable(route = AuthRoute.ProfileSetup.route) {
        ProfileSetupRoute()
    }
}
