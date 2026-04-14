package com.ssafy.e102.eumgil.app.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityLevel
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityLevelRoute
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityType
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityTypeRoute
import com.ssafy.e102.eumgil.feature.onboarding.LocationTermsRoute

fun NavGraphBuilder.onboardingNavGraph(navController: NavHostController) {
    composable(route = OnboardingRoute.DisabilityType.route) {
        DisabilityTypeRoute(
            onNavigateNext = { disabilityType ->
                navController.navigate(
                    OnboardingRoute.DisabilityLevel.createRoute(
                        disabilityType = disabilityType.routeValue,
                    ),
                )
            },
        )
    }

    composable(
        route = OnboardingRoute.DisabilityLevel.route,
        arguments = listOf(
            navArgument(OnboardingRoute.DisabilityLevel.ARG_DISABILITY_TYPE) {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        val disabilityType =
            DisabilityType.fromRouteValue(
                backStackEntry.arguments?.getString(
                    OnboardingRoute.DisabilityLevel.ARG_DISABILITY_TYPE,
                ),
            ) ?: DisabilityType.VISUAL_IMPAIRMENT

        DisabilityLevelRoute(
            disabilityType = disabilityType,
            onNavigateNext = { disabilityLevel ->
                navController.navigate(
                    OnboardingRoute.LocationTerms.createRoute(
                        disabilityType = disabilityType.routeValue,
                        disabilityLevel = disabilityLevel.routeValue,
                    ),
                )
            },
        )
    }

    composable(
        route = OnboardingRoute.LocationTerms.route,
        arguments = listOf(
            navArgument(OnboardingRoute.LocationTerms.ARG_DISABILITY_TYPE) {
                type = NavType.StringType
            },
            navArgument(OnboardingRoute.LocationTerms.ARG_DISABILITY_LEVEL) {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        val disabilityType =
            DisabilityType.fromRouteValue(
                backStackEntry.arguments?.getString(
                    OnboardingRoute.LocationTerms.ARG_DISABILITY_TYPE,
                ),
            ) ?: DisabilityType.VISUAL_IMPAIRMENT
        val disabilityLevel =
            DisabilityLevel.fromRouteValue(
                backStackEntry.arguments?.getString(
                    OnboardingRoute.LocationTerms.ARG_DISABILITY_LEVEL,
                ),
            ) ?: DisabilityLevel.NONE

        LocationTermsRoute(
            disabilityType = disabilityType,
            disabilityLevel = disabilityLevel,
            onConsentCompleted = {
                navController.navigate(TopLevelRoute.Map.route) {
                    launchSingleTop = true
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                }
            },
            onConsentDeferred = {},
        )
    }
}
