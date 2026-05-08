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
            "Shared map floating controls should use the FE floating-control rounding token 24dp.",
            sharedSource.contains("RoundedCornerShape(EumRadius.scaleXl)"),
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
            "MAP current-location button should use the new dedicated navigation-style icon.",
            mapSource.contains("R.drawable.ic_map_current_location"),
        )
        assertTrue(
            "The new current-location icon asset should exist.",
            File("src/main/res/drawable/ic_map_current_location.xml").exists(),
        )
    }
}
