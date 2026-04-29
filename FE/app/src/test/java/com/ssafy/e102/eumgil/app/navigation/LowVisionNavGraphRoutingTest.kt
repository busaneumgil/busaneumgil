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
    }

    @Test
    fun `low vision search result auto starts navigation guidance`() {
        assertEquals(
            RouteSettingRoute.Setting.createRoute(autoStartNavigation = true),
            resolveLowVisionSearchResultRoute(),
        )
    }
}
