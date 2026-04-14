package com.ssafy.e102.eumgil.app.navigation

import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityLevel
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityType

sealed interface AppStartDestination {
    val route: String

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

fun resolveAppStartDestination(initSettings: InitSettings): AppStartDestination {
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
