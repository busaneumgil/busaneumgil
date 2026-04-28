package com.ssafy.e102.eumgil.app.navigation

import com.ssafy.e102.eumgil.core.model.AuthGateState
import com.ssafy.e102.eumgil.core.model.InitSettings

sealed interface AppStartDestination {
    val route: String

    data object Login : AppStartDestination {
        override val route: String = AuthRoute.Login.route
    }

    data object ProfileSetup : AppStartDestination {
        override val route: String = AuthRoute.ProfileSetup.route
    }

    data object UserTypePrimaryStep : AppStartDestination {
        override val route: String = OnboardingRoute.UserTypePrimary.route
    }

    data object Map : AppStartDestination {
        override val route: String = TopLevelRoute.Map.route
    }
}

fun resolveAppStartDestination(
    authGateState: AuthGateState,
    initSettings: InitSettings,
): AppStartDestination {
    if (!authGateState.hasSession) return AppStartDestination.Login
    if (!authGateState.isProfileCompleted) return AppStartDestination.ProfileSetup

    return if (initSettings.isOnboardingCompleted) {
        AppStartDestination.Map
    } else {
        AppStartDestination.UserTypePrimaryStep
    }
}
