package com.ssafy.e102.eumgil.app.navigation

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.data.repository.AuthSignupRepository
import com.ssafy.e102.eumgil.data.repository.ProfileUserTypeUpdateRepository
import com.ssafy.e102.eumgil.data.repository.ProfileUserTypeUpdateResult
import com.ssafy.e102.eumgil.data.repository.SettingsRepository
import com.ssafy.e102.eumgil.feature.onboarding.LocationTermsRoute
import com.ssafy.e102.eumgil.feature.onboarding.LowVisionFollowUpRoute
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserTypeRoute
import com.ssafy.e102.eumgil.feature.onboarding.MobilityTypeSecondaryRoute
import com.ssafy.e102.eumgil.feature.terms.TermsGuideRoute
import com.ssafy.e102.eumgil.feature.terms.TermsGuideStep
import kotlinx.coroutines.launch

fun NavGraphBuilder.onboardingNavGraph(
    navController: NavHostController,
    settingsRepository: SettingsRepository,
    authSignupRepository: AuthSignupRepository,
    profileUserTypeUpdateRepository: ProfileUserTypeUpdateRepository,
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

    composable(route = OnboardingRoute.ProfileUserTypePrimary.route) {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        PrimaryUserTypeRoute(
            onTypeSelected = { primaryUserType ->
                coroutineScope.launch {
                    when (primaryUserType) {
                        PrimaryUserType.LOW_VISION ->
                            completeProfileEditAndNavigate(
                                navController = navController,
                                context = context,
                                profileUserTypeUpdateRepository = profileUserTypeUpdateRepository,
                                selectedPrimaryUserType = primaryUserType.routeValue,
                                selectedMobilitySubtype = null,
                            )

                        PrimaryUserType.MOBILITY_IMPAIRED ->
                            navController.navigate(
                                resolvePrimaryUserTypeNextRoute(
                                    primaryUserType = primaryUserType,
                                    entryPoint = OnboardingEntryPoint.PROFILE_EDIT,
                                ),
                            )
                    }
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

    composable(route = OnboardingRoute.ProfileMobilityTypeSecondary.route) {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        MobilityTypeSecondaryRoute(
            onNavigateNext = { mobilitySubtype ->
                coroutineScope.launch {
                    completeProfileEditAndNavigate(
                        navController = navController,
                        context = context,
                        profileUserTypeUpdateRepository = profileUserTypeUpdateRepository,
                        selectedPrimaryUserType = PrimaryUserType.MOBILITY_IMPAIRED.routeValue,
                        selectedMobilitySubtype = mobilitySubtype.routeValue,
                    )
                }
            },
        )
    }

    composable(route = OnboardingRoute.Terms.route) {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val initSettings by
            settingsRepository
                .observeInitSettings()
                .collectAsStateWithLifecycle(initialValue = InitSettings())

        LocationTermsRoute(
            initialLocationTermsChecked = initSettings.isLocationTermsAgreed,
            initialPrivacyPolicyChecked = initSettings.isPrivacyPolicyAgreed,
            onConsentCompleted = { agreement ->
                coroutineScope.launch {
                    runCatching {
                        settingsRepository.saveLocationTermsAgreement(
                            isLocationTermsAgreed = agreement.isLocationTermsAgreed,
                            isPrivacyPolicyAgreed = agreement.isPrivacyPolicyAgreed,
                        )
                        authSignupRepository.completePendingSignup(
                            requiredTermsAccepted = agreement.isLocationTermsAgreed,
                        )
                        val completedSettings = settingsRepository.getInitSettings()
                        navController.navigateToCompletedOnboarding(
                            route = resolveOnboardingCompletedRoute(completedSettings.selectedPrimaryUserType),
                        )
                    }.onFailure { throwable ->
                        Toast
                            .makeText(
                                context,
                                throwable.message ?: DEFAULT_ONBOARDING_COMPLETION_ERROR_MESSAGE,
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            },
        )
    }

    composable(
        route = OnboardingRoute.TermsGuide.route,
        arguments =
            listOf(
                navArgument(OnboardingRoute.TermsGuide.ARG_STEP) {
                    type = NavType.StringType
                    defaultValue = OnboardingRoute.TermsGuide.DEFAULT_STEP
                },
            ),
    ) { backStackEntry ->
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        val initialStep =
            TermsGuideStep.fromRouteValue(
                backStackEntry.arguments?.getString(OnboardingRoute.TermsGuide.ARG_STEP),
            ) ?: TermsGuideStep.AGREE

        TermsGuideRoute(
            initialStep = initialStep,
            onCompleted = {
                // 5단계(처리방침)까지 통과 = 위치/개인정보 항목 모두 확인 완료.
                coroutineScope.launch {
                    runCatching {
                        settingsRepository.saveLowVisionFollowUpCompleted(isCompleted = true)
                        settingsRepository.saveLocationTermsAgreement(
                            isLocationTermsAgreed = true,
                            isPrivacyPolicyAgreed = true,
                        )
                        authSignupRepository.completePendingSignup(requiredTermsAccepted = true)
                        val completedSettings = settingsRepository.getInitSettings()
                        navController.navigateToCompletedOnboarding(
                            route = resolveOnboardingCompletedRoute(completedSettings.selectedPrimaryUserType),
                        )
                    }.onFailure { throwable ->
                        Toast
                            .makeText(
                                context,
                                throwable.message ?: DEFAULT_ONBOARDING_COMPLETION_ERROR_MESSAGE,
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            },
            onRequestDetails = { _ ->
                // 모든 단계의 "자세히 보기"는 기존 정식 약관 화면을 재사용한다.
                // 항목별 상세 화면이 별도로 생기면 step 분기로 라우팅을 갈라주면 됨.
                navController.navigate(OnboardingRoute.Terms.route)
            },
        )
    }
}

internal enum class OnboardingEntryPoint {
    SIGN_UP,
    PROFILE_EDIT,
}

internal fun resolvePrimaryUserTypeNextRoute(
    primaryUserType: PrimaryUserType,
    entryPoint: OnboardingEntryPoint = OnboardingEntryPoint.SIGN_UP,
): String =
    when (entryPoint) {
        OnboardingEntryPoint.SIGN_UP ->
            when (primaryUserType) {
                PrimaryUserType.LOW_VISION -> OnboardingRoute.TermsGuide.createRoute()
                PrimaryUserType.MOBILITY_IMPAIRED -> OnboardingRoute.MobilityTypeSecondary.route
            }

        OnboardingEntryPoint.PROFILE_EDIT ->
            when (primaryUserType) {
                PrimaryUserType.LOW_VISION -> LowVisionRoute.Home.route
                PrimaryUserType.MOBILITY_IMPAIRED -> OnboardingRoute.ProfileMobilityTypeSecondary.route
            }
    }

internal fun resolveOnboardingCompletedRoute(selectedPrimaryUserType: String?): String =
    if (selectedPrimaryUserType == PrimaryUserType.LOW_VISION.routeValue) {
        LowVisionRoute.Home.route
    } else {
        TopLevelRoute.Map.route
    }

internal fun resolveProfileEditCompletedRoute(selectedPrimaryUserType: String?): String =
    if (selectedPrimaryUserType == PrimaryUserType.LOW_VISION.routeValue) {
        LowVisionRoute.Home.route
    } else {
        TopLevelRoute.MyPage.route
    }

private fun NavHostController.navigateToCompletedOnboarding(route: String) {
    navigate(route) {
        launchSingleTop = true
        popUpTo(graph.findStartDestination().id) {
            inclusive = true
        }
    }
}

private fun NavHostController.navigateToMyPageAfterProfileEdit() {
    val didPopToMyPage =
        popBackStack(
            route = TopLevelRoute.MyPage.route,
            inclusive = false,
        )

    if (!didPopToMyPage) {
        navigate(TopLevelRoute.MyPage.route) {
            launchSingleTop = true
        }
    }
}

private fun NavHostController.navigateToLowVisionHomeAfterProfileEdit() {
    navigate(LowVisionRoute.Home.route) {
        launchSingleTop = true
        popUpTo(TopLevelRoute.MyPage.route) {
            inclusive = true
        }
    }
}

private suspend fun completeProfileEditAndNavigate(
    navController: NavHostController,
    context: android.content.Context,
    profileUserTypeUpdateRepository: ProfileUserTypeUpdateRepository,
    selectedPrimaryUserType: String,
    selectedMobilitySubtype: String?,
) {
    when (
        val result =
            profileUserTypeUpdateRepository.completeProfileEdit(
                selectedPrimaryUserType = selectedPrimaryUserType,
                selectedMobilitySubtype = selectedMobilitySubtype,
            )
    ) {
        is ProfileUserTypeUpdateResult.Success -> {
            when (resolveProfileEditCompletedRoute(result.selectedPrimaryUserType)) {
                LowVisionRoute.Home.route -> navController.navigateToLowVisionHomeAfterProfileEdit()
                else -> navController.navigateToMyPageAfterProfileEdit()
            }
        }

        ProfileUserTypeUpdateResult.MissingSession,
        ProfileUserTypeUpdateResult.AuthenticationFailed,
        -> {
            Toast
                .makeText(
                    context,
                    PROFILE_EDIT_AUTHENTICATION_ERROR_MESSAGE,
                    Toast.LENGTH_SHORT,
                ).show()
            navController.navigateToLoginAfterAuthenticationFailure()
        }

        is ProfileUserTypeUpdateResult.Failure ->
            Toast
                .makeText(
                    context,
                    result.message.ifBlank { DEFAULT_PROFILE_EDIT_COMPLETION_ERROR_MESSAGE },
                    Toast.LENGTH_SHORT,
                ).show()
    }
}

private fun NavHostController.navigateToLoginAfterAuthenticationFailure() {
    navigate(AuthRoute.Login.route) {
        launchSingleTop = true
        popUpTo(graph.findStartDestination().id) {
            inclusive = true
        }
    }
}

private const val DEFAULT_ONBOARDING_COMPLETION_ERROR_MESSAGE: String =
    "온보딩 완료 처리에 실패했습니다. 다시 시도해주세요."

private const val DEFAULT_PROFILE_EDIT_COMPLETION_ERROR_MESSAGE: String =
    "프로필 변경에 실패했습니다. 다시 시도해주세요."

private const val PROFILE_EDIT_AUTHENTICATION_ERROR_MESSAGE: String =
    "로그인 정보가 만료되었습니다. 다시 로그인해주세요."
