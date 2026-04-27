package com.ssafy.e102.eumgil.app.navigation

import com.ssafy.e102.eumgil.core.model.AuthSessionSnapshot
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityLevel
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityType

sealed interface AppStartDestination {
    val route: String

    data object Login : AppStartDestination {
        override val route: String = AuthRoute.Login.route
    }

    data object ProfileSetup : AppStartDestination {
        override val route: String = AuthRoute.ProfileSetup.route
    }

    data object DisabilityTypeStep : AppStartDestination {
        override val route: String = OnboardingRoute.DisabilityType.route
    }

    data class DisabilityLevelStep(
        val disabilityType: DisabilityType,
    ) : AppStartDestination {
        override val route: String =
            OnboardingRoute.DisabilityLevel.createRoute(disabilityType = disabilityType.routeValue)
    }

    data class LocationTermsStep(
        val disabilityType: DisabilityType,
        val disabilityLevel: DisabilityLevel,
    ) : AppStartDestination {
        override val route: String =
            OnboardingRoute.LocationTerms.createRoute(
                disabilityType = disabilityType.routeValue,
                disabilityLevel = disabilityLevel.routeValue,
            )
    }

    data object Map : AppStartDestination {
        override val route: String = TopLevelRoute.Map.route
    }
}

fun resolveAppStartDestination(
    authSessionSnapshot: AuthSessionSnapshot,
    initSettings: InitSettings,
): AppStartDestination =
    when {
        !authSessionSnapshot.isAuthenticated -> AppStartDestination.Login
        !authSessionSnapshot.isProfileCompleted -> AppStartDestination.ProfileSetup
        else -> resolveOnboardingStartDestination(initSettings)
    }

fun resolveAppStartDestination(initSettings: InitSettings): AppStartDestination {
    return resolveOnboardingStartDestination(initSettings)
}

private fun resolveOnboardingStartDestination(initSettings: InitSettings): AppStartDestination {
    val disabilityType = DisabilityType.fromRouteValue(initSettings.disabilityType)
    val disabilityLevel = DisabilityLevel.fromRouteValue(initSettings.disabilityLevel)

    return when {
        initSettings.isOnboardingCompleted &&
            disabilityType != null &&
            disabilityLevel != null -> {
            AppStartDestination.Map
        }
        disabilityType == null -> AppStartDestination.DisabilityTypeStep
        disabilityLevel == null -> AppStartDestination.DisabilityLevelStep(disabilityType)
        else -> AppStartDestination.LocationTermsStep(
            disabilityType = disabilityType,
            disabilityLevel = disabilityLevel,
        )
    }
}
