package com.ssafy.e102.eumgil.app.navigation

import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.data.repository.SettingsRepository
import com.ssafy.e102.eumgil.feature.onboarding.LocationTermsRoute
import com.ssafy.e102.eumgil.feature.onboarding.LowVisionFollowUpRoute
import com.ssafy.e102.eumgil.feature.onboarding.MobilitySubtype
import com.ssafy.e102.eumgil.feature.onboarding.MobilityTypeSecondaryRoute
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserTypeRoute
import com.ssafy.e102.eumgil.feature.terms.TermsGuideRoute
import kotlinx.coroutines.launch

fun NavGraphBuilder.onboardingNavGraph(
    navController: NavHostController,
    settingsRepository: SettingsRepository,
    initialSettings: InitSettings,
) {
    composable(route = OnboardingRoute.UserTypePrimary.route) {
        val coroutineScope = rememberCoroutineScope()

        PrimaryUserTypeRoute(
            onTypeSelected = { primaryUserType ->
                coroutineScope.launch {
                    settingsRepository.savePrimaryUserType(primaryUserType.routeValue)
                    navController.navigate(resolvePrimaryUserTypeNextRoute(primaryUserType))
                }
            },
        )
    }

    composable(route = OnboardingRoute.LowVisionFollowUp.route) {
        val coroutineScope = rememberCoroutineScope()

        LowVisionFollowUpRoute(
            onNavigateNext = {
                coroutineScope.launch {
                    settingsRepository.saveLowVisionFollowUpCompleted(isCompleted = true)
                    navController.navigate(OnboardingRoute.Terms.route)
                }
            },
        )
    }

    composable(route = OnboardingRoute.MobilityTypeSecondary.route) {
        val coroutineScope = rememberCoroutineScope()

        MobilityTypeSecondaryRoute(
            onNavigateNext = { mobilitySubtype ->
                coroutineScope.launch {
                    settingsRepository.saveMobilitySubtype(mobilitySubtype.routeValue)
                    navController.navigate(OnboardingRoute.Terms.route)
                }
            },
        )
    }

    composable(route = OnboardingRoute.Terms.route) {
        val coroutineScope = rememberCoroutineScope()

        LocationTermsRoute(
            initialLocationTermsChecked = initialSettings.isLocationTermsAgreed,
            initialPrivacyPolicyChecked = initialSettings.isPrivacyPolicyAgreed,
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
        )
    }

    composable(route = OnboardingRoute.TermsGuide.route) {
        val coroutineScope = rememberCoroutineScope()

        TermsGuideRoute(
            onAgreed = {
                coroutineScope.launch {
                    settingsRepository.saveLowVisionFollowUpCompleted(isCompleted = true)
                    settingsRepository.saveLocationTermsAgreement(
                        isLocationTermsAgreed = true,
                        isPrivacyPolicyAgreed = false,
                    )

                    navController.navigate(TopLevelRoute.Map.route) {
                        launchSingleTop = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            },
            onRequestDetails = {
                navController.navigate(OnboardingRoute.Terms.route)
            },
            onTabSelected = { /* Selection only highlights; tab routing is owned by AppNavHost. */ },
        )
    }
}

internal fun resolvePrimaryUserTypeNextRoute(primaryUserType: PrimaryUserType): String =
    when (primaryUserType) {
        PrimaryUserType.LOW_VISION -> OnboardingRoute.TermsGuide.route
        PrimaryUserType.MOBILITY_IMPAIRED -> OnboardingRoute.MobilityTypeSecondary.route
    }
