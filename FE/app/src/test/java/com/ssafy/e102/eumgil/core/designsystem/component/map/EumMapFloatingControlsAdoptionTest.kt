package com.ssafy.e102.eumgil.core.designsystem.component.map

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class EumMapFloatingControlsAdoptionTest {
    @Test
    fun `route setting screen uses shared map floating controls`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt").readText()

        assertTrue(
            "Route setting map preview should use the shared floating control component so future map API screens stay aligned.",
            source.contains("EumMapFloatingControls("),
        )
        assertTrue(
            "Route setting should use the new current-location icon for the lower action card.",
            source.contains("R.drawable.ic_map_current_location"),
        )
    }

    @Test
    fun `navigation screen uses shared map floating controls`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt").readText()

        assertTrue(
            "Navigation map preview should use the shared floating control component so all map surfaces match.",
            source.contains("EumMapFloatingControls("),
        )
        assertTrue(
            "Navigation should use the new current-location icon for the lower action card.",
            source.contains("R.drawable.ic_map_current_location"),
        )
    }
}
