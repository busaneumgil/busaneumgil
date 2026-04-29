package com.ssafy.e102.eumgil.feature.map.component

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.ssafy.e102.eumgil.feature.map.model.MapShortcutFilterKey

class MapShortcutFilterRowConfigurationTest {
    @Test
    fun `map shortcut filter row uses dedicated elevator asset for top chip`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapShortcutFilterRow.kt")
                .readText()

        assertTrue(
            "Top shortcut elevator chip should use its dedicated drawable resource.",
            source.contains("MapShortcutFilterKey.ELEVATOR -> R.drawable.ic_map_shortcut_elevator"),
        )
        assertTrue(
            "Dedicated elevator shortcut icon resource should exist.",
            File("src/main/res/drawable/ic_map_shortcut_elevator.png").exists(),
        )
    }

    @Test
    fun `map shortcut filter row enlarges only the elevator icon`() {
        assertEquals(18, shortcutFilterIconSizeDp(MapShortcutFilterKey.ELEVATOR))
        assertEquals(16, shortcutFilterIconSizeDp(MapShortcutFilterKey.TOILET))
        assertEquals(16, shortcutFilterIconSizeDp(MapShortcutFilterKey.ACCESSIBLE_PARKING))
    }

    @Test
    fun `map top search bar and shortcut filters share medium radius token`() {
        val searchBarSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapTopSearchBar.kt")
                .readText()
        val filterRowSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapShortcutFilterRow.kt")
                .readText()

        assertTrue(
            "MAP top search bar should reuse the shared medium radius token.",
            searchBarSource.contains("shape = RoundedCornerShape(EumRadius.medium)"),
        )
        assertTrue(
            "MAP top shortcut filters should match the search bar corner radius token.",
            filterRowSource.contains("shape = RoundedCornerShape(EumRadius.medium)"),
        )
    }
}
