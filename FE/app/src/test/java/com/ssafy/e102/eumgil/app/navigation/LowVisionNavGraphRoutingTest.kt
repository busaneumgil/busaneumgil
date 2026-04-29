package com.ssafy.e102.eumgil.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class LowVisionNavGraphRoutingTest {
    @Test
    fun `recording completion moves to low vision search results`() {
        assertEquals(
            LowVisionRoute.Search.route,
            resolveLowVisionRecordingCompletedRoute(),
        )
        assertEquals(
            LowVisionRoute.VoiceInput.route,
            resolveLowVisionRecordingPopUpRoute(),
        )
    }

    @Test
    fun `low vision search result auto starts navigation guidance`() {
        assertEquals(
            RouteSettingRoute.Setting.createRoute(autoStartNavigation = true),
            resolveLowVisionSearchResultRoute(),
        )
        assertEquals(
            LowVisionRoute.Search.route,
            resolveLowVisionSearchPopUpRoute(),
        )
    }

    @Test
    fun `navigation completion returns to low vision voice input home tab`() {
        assertEquals(
            LowVisionRoute.VoiceInput.route,
            resolveNavigationCompletionRoute(),
        )
    }
}
