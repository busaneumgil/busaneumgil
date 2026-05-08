package com.ssafy.e102.eumgil.feature.map.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MapTopSearchBarConfigurationTest {
    @Test
    fun `map top search bar disables ripple indication for entry tap`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapTopSearchBar.kt")
                .readText()

        assertTrue(
            "Map top search bar should keep search navigation tap behavior without showing a pressed ripple.",
            source.contains("indication = null"),
        )
        assertTrue(
            "Map top search bar should provide its own interaction source when ripple indication is disabled.",
            source.contains("MutableInteractionSource"),
        )
    }
}
