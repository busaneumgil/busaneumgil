package com.ssafy.e102.eumgil.app.navigation

import com.ssafy.e102.eumgil.core.model.AuthGateState
import com.ssafy.e102.eumgil.core.model.AuthSession
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppStartDestinationRoutingTest {
    @Test
    fun `unauthenticated session starts at login route`() {
        val destination =
            resolveAppStartDestination(
                authGateState = AuthGateState(),
                initSettings = completedInitSettings,
            )

        assertSame(AppStartDestination.Login, destination)
        assertEquals(AuthRoute.Login.route, destination.route)
    }

    @Test
    fun `authenticated profile incomplete session starts at AUTH-002 route`() {
        val destination =
            resolveAppStartDestination(
                authGateState = AuthGateState(
                    authSession = AuthSession(accessToken = "test-token"),
                    isProfileCompleted = false,
                ),
                initSettings = completedInitSettings,
            )

        assertSame(AppStartDestination.ProfileSetup, destination)
        assertEquals("auth/profile_setup", destination.route)
    }

    @Test
    fun `profile complete session keeps existing onboarding gate`() {
        val destination =
            resolveAppStartDestination(
                authGateState = AuthGateState(
                    authSession = AuthSession(accessToken = "test-token"),
                    isProfileCompleted = true,
                ),
                initSettings = InitSettings(),
            )

        assertSame(AppStartDestination.UserTypePrimaryStep, destination)
    }

    @Test
    fun `profile complete session still starts ONB-001 when only primary user type is stored`() {
        val destination =
            resolveAppStartDestination(
                authGateState = AuthGateState(
                    authSession = AuthSession(accessToken = "test-token"),
                    isProfileCompleted = true,
                ),
                initSettings =
                    InitSettings(
                        selectedPrimaryUserType = PrimaryUserType.LOW_VISION.routeValue,
                    ),
            )

        assertSame(AppStartDestination.UserTypePrimaryStep, destination)
    }

    private companion object {
        val completedInitSettings =
            InitSettings(
                selectedPrimaryUserType = PrimaryUserType.MOBILITY_IMPAIRED.routeValue,
                selectedMobilitySubtype = "manual_wheelchair",
                isLocationTermsAgreed = true,
            )
    }
}
