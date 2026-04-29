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
import com.ssafy.e102.eumgil.feature.onboarding.LocationTermsRoute
import com.ssafy.e102.eumgil.feature.onboarding.LowVisionFollowUpRoute
import com.ssafy.e102.eumgil.feature.onboarding.MobilitySubtype
import com.ssafy.e102.eumgil.feature.onboarding.MobilityTypeSecondaryRoute
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserTypeRoute
import com.ssafy.e102.eumgil.feature.terms.TermsGuideRoute
import com.ssafy.e102.eumgil.feature.terms.TermsGuideStep
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

    composable(
        route = OnboardingRoute.TermsGuide.route,
        arguments = listOf(
            navArgument(OnboardingRoute.TermsGuide.ARG_STEP) {
                type = NavType.StringType
                defaultValue = OnboardingRoute.TermsGuide.DEFAULT_STEP
            },
        ),
    ) { backStackEntry ->
        val coroutineScope = rememberCoroutineScope()

        val initialStep =
            TermsGuideStep.fromRouteValue(
                backStackEntry.arguments?.getString(OnboardingRoute.TermsGuide.ARG_STEP),
            ) ?: TermsGuideStep.AGREE

        TermsGuideRoute(
            initialStep = initialStep,
            onCompleted = {
                // 5단계(처리방침)까지 통과 = 위치/개인정보 항목 모두 확인 완료.
                coroutineScope.launch {
                    settingsRepository.saveLowVisionFollowUpCompleted(isCompleted = true)
                    settingsRepository.saveLocationTermsAgreement(
                        isLocationTermsAgreed = true,
                        isPrivacyPolicyAgreed = true,
                    )

                    navController.navigate(resolveTermsGuideCompletedRoute()) {
                        launchSingleTop = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            },
            onRequestDetails = { _ ->
                // 모든 단계의 "자세히 보기"는 기존 정식 약관 화면을 재사용한다.
                // 항목별 상세 화면이 별도로 생기면 step 분기로 라우팅을 갈라주면 됨.
                navController.navigate(OnboardingRoute.Terms.route)
            },
            onTabSelected = { /* Selection only highlights; tab routing is owned by AppNavHost. */ },
        )
    }
}

internal fun resolvePrimaryUserTypeNextRoute(primaryUserType: PrimaryUserType): String =
    when (primaryUserType) {
        // 시각장애 흐름은 약관 안내 5단계의 1번(agree)부터 시작.
        PrimaryUserType.LOW_VISION -> OnboardingRoute.TermsGuide.createRoute()
        PrimaryUserType.MOBILITY_IMPAIRED -> OnboardingRoute.MobilityTypeSecondary.route
    }

internal fun resolveTermsGuideCompletedRoute(): String = LowVisionRoute.Home.route
