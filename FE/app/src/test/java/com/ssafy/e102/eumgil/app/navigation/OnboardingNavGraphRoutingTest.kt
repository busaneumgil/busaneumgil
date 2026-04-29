package com.ssafy.e102.eumgil.app.navigation

import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType
import com.ssafy.e102.eumgil.feature.terms.TermsGuideStep
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingNavGraphRoutingTest {
    @Test
    fun `low vision primary user type moves to terms guide first step`() {
        // 시각장애 흐름은 약관 안내 5단계 walkthrough의 첫 단계(agree)에서 시작.
        assertEquals(
            OnboardingRoute.TermsGuide.createRoute(TermsGuideStep.AGREE.routeValue),
            resolvePrimaryUserTypeNextRoute(PrimaryUserType.LOW_VISION),
        )
    }

    @Test
    fun `terms guide default route equals first step route`() {
        // createRoute()의 기본값(DEFAULT_STEP=agree)이 TermsGuideStep.AGREE.routeValue와 일치.
        assertEquals(
            OnboardingRoute.TermsGuide.createRoute(TermsGuideStep.AGREE.routeValue),
            OnboardingRoute.TermsGuide.createRoute(),
        )
    }

    @Test
    fun `terms guide completion moves to low vision home`() {
        assertEquals(
            LowVisionRoute.Home.route,
            resolveTermsGuideCompletedRoute(),
        )
    }

    @Test
    fun `mobility impaired primary user type moves to mobility subtype route`() {
        assertEquals(
            OnboardingRoute.MobilityTypeSecondary.route,
            resolvePrimaryUserTypeNextRoute(PrimaryUserType.MOBILITY_IMPAIRED),
        )
    }

    @Test
    fun `profile edit low vision primary user type opens low vision home without terms`() {
        assertEquals(
            LowVisionRoute.Home.route,
            resolvePrimaryUserTypeNextRoute(
                primaryUserType = PrimaryUserType.LOW_VISION,
                entryPoint = OnboardingEntryPoint.PROFILE_EDIT,
            ),
        )
    }

    @Test
    fun `profile edit mobility impaired primary user type moves to profile edit subtype route`() {
        assertEquals(
            OnboardingRoute.ProfileMobilityTypeSecondary.route,
            resolvePrimaryUserTypeNextRoute(
                primaryUserType = PrimaryUserType.MOBILITY_IMPAIRED,
                entryPoint = OnboardingEntryPoint.PROFILE_EDIT,
            ),
        )
    }
}
