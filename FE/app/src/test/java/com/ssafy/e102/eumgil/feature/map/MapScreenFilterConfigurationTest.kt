package com.ssafy.e102.eumgil.feature.map

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MapScreenFilterConfigurationTest {
    @Test
    fun `map screen mounts category filter bar with marker filter actions`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt")
                .readText()

        assertTrue(
            "MapScreen should render the shared category filter bar in the map top overlay.",
            source.contains("MapCategoryFilterBar("),
        )
        assertTrue(
            "MapScreen should bind the category filter bar to markerFilterState.",
            source.contains("state = uiState.markerFilterState"),
        )
        assertTrue(
            "MapScreen should wire the reset action for the category filter bar.",
            source.contains("onReset = { onAction(MapUiAction.MarkerCategoryFilterReset) }"),
        )
        assertTrue(
            "MapScreen should wire category toggles back into the view model action contract.",
            source.contains("onAction(MapUiAction.MarkerCategoryFilterToggled(category))"),
        )
    }
}
