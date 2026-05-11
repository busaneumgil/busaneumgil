package com.ssafy.e102.eumgil.feature.map.component

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapOverlayViewportUsageTest {
    @Test
    fun `route and navigation screens use kakao backed overlay viewport`() {
        val routeScreen =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val navigationScreen =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt")
                .readText()

        assertTrue(routeScreen.contains("MapOverlayViewport("))
        assertTrue(navigationScreen.contains("MapOverlayViewport("))
        assertFalse(routeScreen.contains("MapViewportOverlayBackdrop("))
        assertFalse(navigationScreen.contains("MapViewportOverlayBackdrop("))
    }
}
