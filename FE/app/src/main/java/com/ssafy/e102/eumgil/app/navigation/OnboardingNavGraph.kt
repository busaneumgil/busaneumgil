package com.ssafy.e102.eumgil.app.navigation

import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.data.repository.SettingsRepository
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityLevel
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityLevelRoute
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityType
import com.ssafy.e102.eumgil.feature.onboarding.DisabilityTypeRoute
import com.ssafy.e102.eumgil.feature.onboarding.LocationTermsRoute
import kotlinx.coroutines.launch

fun NavGraphBuilder.onboardingNavGraph(
    navController: NavHostController,
    settingsRepository: SettingsRepository,
    initialSettings: InitSettings,
) {
    composable(route = OnboardingRoute.DisabilityType.route) {
        val coroutineScope = rememberCoroutineScope()

        DisabilityTypeRoute(
            onNavigateNext = { disabilityType ->
                coroutineScope.launch {
                    settingsRepository.saveDisabilityType(disabilityType.routeValue)
                    navController.navigate(
                        OnboardingRoute.DisabilityLevel.createRoute(
                            disabilityType = disabilityType.routeValue,
                        ),
                    )
                }
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

        val coroutineScope = rememberCoroutineScope()
        DisabilityLevelRoute(
            disabilityType = disabilityType,
            onNavigateNext = { disabilityLevel ->
                coroutineScope.launch {
                    settingsRepository.saveDisabilityLevel(disabilityLevel.routeValue)
                    navController.navigate(
                        OnboardingRoute.LocationTerms.createRoute(
                            disabilityType = disabilityType.routeValue,
                            disabilityLevel = disabilityLevel.routeValue,
                        ),
                    )
                }
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

        val coroutineScope = rememberCoroutineScope()
        val shouldRestoreAgreement =
            initialSettings.disabilityType == disabilityType.routeValue &&
                initialSettings.disabilityLevel == disabilityLevel.routeValue

        LocationTermsRoute(
            disabilityType = disabilityType,
            disabilityLevel = disabilityLevel,
            initialLocationTermsChecked =
                shouldRestoreAgreement && initialSettings.isLocationTermsAgreed,
            initialPrivacyPolicyChecked =
                shouldRestoreAgreement && initialSettings.isPrivacyPolicyAgreed,
            onConsentCompleted = { agreement ->
                coroutineScope.launch {
                    settingsRepository.saveLocationTermsAgreement(
                        isLocationTermsAgreed = agreement.isLocationTermsAgreed,
                        isPrivacyPolicyAgreed = agreement.isPrivacyPolicyAgreed,
                    )

                    navController.navigate(TopLevelRoute.Map.route) {
                        launchSingleTop = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            },
            onConsentDeferred = { agreement ->
                coroutineScope.launch {
                    settingsRepository.saveLocationTermsAgreement(
                        isLocationTermsAgreed = agreement.isLocationTermsAgreed,
                        isPrivacyPolicyAgreed = agreement.isPrivacyPolicyAgreed,
                    )
                }
            },
        )
    }
}
