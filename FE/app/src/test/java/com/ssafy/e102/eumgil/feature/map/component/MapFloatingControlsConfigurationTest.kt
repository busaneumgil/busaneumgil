package com.ssafy.e102.eumgil.feature.map.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MapFloatingControlsConfigurationTest {
    @Test
    fun `shared map floating controls use compact grouped layout`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/core/designsystem/component/map/EumMapFloatingControls.kt")
                .readText()

        assertTrue(
            "Shared map floating controls should keep the compact 48dp zoom stack width.",
            source.contains("modifier = Modifier.width(48.dp)"),
        )
        assertTrue(
            "Shared map floating controls should keep each zoom action at 48dp height.",
            source.contains(".height(48.dp)"),
        )
        assertTrue(
            "Shared map floating controls should keep the current-location action at a 48dp square size.",
            source.contains("modifier = Modifier.size(48.dp)"),
        )
        assertTrue(
            "Shared map floating controls should group zoom in and zoom out inside one stacked card.",
            source.contains("HorizontalDivider"),
        )
    }

    @Test
    fun `shared map floating controls use fe rounding token and updated location icon`() {
        val sharedSource =
            File("src/main/java/com/ssafy/e102/eumgil/core/designsystem/component/map/EumMapFloatingControls.kt")
                .readText()
        val mapSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapFloatingControls.kt")
                .readText()

        assertTrue(
            "Shared map floating controls should keep the zoom stack with a subtle small corner radius.",
            sharedSource.contains("RoundedCornerShape(EumRadius.scaleS)"),
        )
        assertTrue(
            "Shared map floating controls should give the current-location action the same corner radius as the zoom stack.",
            sharedSource.split("RoundedCornerShape(EumRadius.scaleS)").size - 1 >= 2,
        )
        assertTrue(
            "Shared map floating controls should preserve the floating overlay elevation.",
            sharedSource.contains("shadowElevation = 6.dp"),
        )
        assertTrue(
            "MAP screen map controls should delegate to the shared component.",
            mapSource.contains("EumMapFloatingControls("),
        )
        assertTrue(
            "MAP screen should distinguish between an available current-location action and an actively selected one.",
            mapSource.contains("isRecenterButtonActive: Boolean"),
        )
        assertTrue(
            "MAP request-permission state should reuse the disabled-look PNG so the default inactive appearance matches the design.",
            mapSource.split("iconRes = R.drawable.ic_map_current_location_disabled").size - 1 >= 2,
        )
        assertTrue(
            "MAP enabled current-location button should reuse the route-start button icon.",
            mapSource.contains("R.drawable.ic_route_start_navigation_button"),
        )
        assertTrue(
            "MAP enabled current-location button should only show the filled route-start icon after the user explicitly activates current-location recentering.",
            mapSource.contains("if (isRecenterButtonActive)"),
        )
        assertTrue(
            "MAP enabled current-location button should tint the route-start icon with the primary color.",
            mapSource.contains("tint = MaterialTheme.colorScheme.primary"),
        )
        assertTrue(
            "MAP disabled current-location button should use the replacement PNG asset.",
            mapSource.contains("R.drawable.ic_map_current_location_disabled"),
        )
        assertTrue(
            "MAP disabled current-location button should render the provided image as-is without tinting it.",
            mapSource.contains("tint = Color.Unspecified"),
        )
        assertTrue(
            "The disabled current-location icon asset should exist in drawable.",
            File("src/main/res/drawable/ic_map_current_location_disabled.png").exists(),
        )
    }
}
