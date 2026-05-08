package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class LowVisionNavigationRouteTest {
    @Test
    fun `low vision navigation treats exit events as home navigation`() {
        assertEquals(true, shouldNavigateLowVisionHome(NavigationUiEvent.NavigateToMap))
        assertEquals(true, shouldNavigateLowVisionHome(NavigationUiEvent.NavigateToArrival))
    }

    @Test
    fun `low vision navigation ignores non exit navigation events for home`() {
        assertEquals(false, shouldNavigateLowVisionHome(NavigationUiEvent.NavigateBack))
        assertEquals(false, shouldNavigateLowVisionHome(NavigationUiEvent.NavigateToSavedRoute))
        assertEquals(false, shouldNavigateLowVisionHome(NavigationUiEvent.NavigateToRouteDetail(RouteOption.SAFE)))
    }
}
