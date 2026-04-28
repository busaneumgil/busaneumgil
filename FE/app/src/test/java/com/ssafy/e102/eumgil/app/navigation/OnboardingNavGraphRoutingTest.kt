package com.ssafy.e102.eumgil.app.navigation

import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingNavGraphRoutingTest {
    @Test
    fun `low vision primary user type moves to terms guide route`() {
        assertEquals(
            OnboardingRoute.TermsGuide.route,
            resolvePrimaryUserTypeNextRoute(PrimaryUserType.LOW_VISION),
        )
    }

    @Test
    fun `mobility impaired primary user type moves to mobility subtype route`() {
        assertEquals(
            OnboardingRoute.MobilityTypeSecondary.route,
            resolvePrimaryUserTypeNextRoute(PrimaryUserType.MOBILITY_IMPAIRED),
        )
    }
}
