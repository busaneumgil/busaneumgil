package com.ssafy.e102.eumgil.feature.map

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapBookmarkActionButtonConfigurationTest {
    @Test
    fun `facility detail bookmark action button uses transparent container`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Facility detail bookmark button should keep a transparent container behind the icon.",
            source.contains("color = Color.Transparent"),
        )
        assertFalse(
            "Facility detail bookmark button should not render a selected-state tint background.",
            source.contains("primaryContainer.copy(alpha = 0.5f)"),
        )
    }
}
