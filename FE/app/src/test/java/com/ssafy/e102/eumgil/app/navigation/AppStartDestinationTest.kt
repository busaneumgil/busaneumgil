package com.ssafy.e102.eumgil.app.navigation

import com.ssafy.e102.eumgil.core.model.AuthSessionSnapshot
import com.ssafy.e102.eumgil.core.model.InitSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppStartDestinationTest {
    @Test
    fun `unauthenticated session starts at login route`() {
        val destination =
            resolveAppStartDestination(
                authSessionSnapshot = AuthSessionSnapshot(
                    isAuthenticated = false,
                    isProfileCompleted = false,
                ),
                initSettings = completedInitSettings,
            )

        assertSame(AppStartDestination.Login, destination)
        assertEquals(AuthRoute.Login.route, destination.route)
    }

    @Test
    fun `authenticated profile incomplete session starts at AUTH-002 route`() {
        val destination =
            resolveAppStartDestination(
                authSessionSnapshot = AuthSessionSnapshot(
                    isAuthenticated = true,
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
                authSessionSnapshot = AuthSessionSnapshot.LocalMockReady,
                initSettings = InitSettings(),
            )

        assertSame(AppStartDestination.DisabilityTypeStep, destination)
    }

    private companion object {
        val completedInitSettings =
            InitSettings(
                disabilityType = "visual",
                disabilityLevel = "mild",
                isLocationTermsAgreed = true,
            )
    }
}
