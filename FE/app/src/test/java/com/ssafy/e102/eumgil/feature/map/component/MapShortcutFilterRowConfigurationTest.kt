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
    fun `map shortcut filter row enlarges accessibility heavy icons`() {
        assertEquals(20, shortcutFilterIconSizeDp(MapShortcutFilterKey.ELEVATOR))
        assertEquals(20, shortcutFilterIconSizeDp(MapShortcutFilterKey.TOURIST_SPOT))
        assertEquals(20, shortcutFilterIconSizeDp(MapShortcutFilterKey.ACCOMMODATION))
        assertEquals(20, shortcutFilterIconSizeDp(MapShortcutFilterKey.WELFARE))
        assertEquals(20, shortcutFilterIconSizeDp(MapShortcutFilterKey.PUBLIC_OFFICE))
        assertEquals(16, shortcutFilterIconSizeDp(MapShortcutFilterKey.TOILET))
        assertEquals(16, shortcutFilterIconSizeDp(MapShortcutFilterKey.FOOD_CAFE))
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

    @Test
    fun `map shortcut filter row uses dedicated tourist asset for tourist chip`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapShortcutFilterRow.kt")
                .readText()

        assertTrue(
            "Top shortcut tourist chip should use the dedicated tourist drawable resource.",
            source.contains("MapShortcutFilterKey.TOURIST_SPOT -> R.drawable.ic_place_tourist_spot"),
        )
        assertTrue(
            "Dedicated tourist place icon resource should exist.",
            File("src/main/res/drawable/ic_place_tourist_spot.png").exists(),
        )
    }

    @Test
    fun `map shortcut filter row uses dedicated accommodation asset for accommodation chip`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapShortcutFilterRow.kt")
                .readText()

        assertTrue(
            "Top shortcut accommodation chip should use the dedicated accommodation drawable resource.",
            source.contains("MapShortcutFilterKey.ACCOMMODATION -> R.drawable.ic_place_accommodation"),
        )
        assertTrue(
            "Dedicated accommodation place icon resource should exist.",
            File("src/main/res/drawable/ic_place_accommodation.png").exists(),
        )
    }

    @Test
    fun `map shortcut filter row uses dedicated healthcare asset for healthcare chip`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapShortcutFilterRow.kt")
                .readText()

        assertTrue(
            "Top shortcut healthcare chip should use the dedicated healthcare drawable resource.",
            source.contains("MapShortcutFilterKey.HEALTHCARE -> R.drawable.ic_place_healthcare"),
        )
        assertTrue(
            "Dedicated healthcare place icon resource should exist.",
            File("src/main/res/drawable/ic_place_healthcare.png").exists(),
        )
    }

    @Test
    fun `map shortcut filter row uses dedicated welfare asset for welfare chip`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapShortcutFilterRow.kt")
                .readText()

        assertTrue(
            "Top shortcut welfare chip should use the dedicated welfare drawable resource.",
            source.contains("MapShortcutFilterKey.WELFARE -> R.drawable.ic_place_welfare"),
        )
        assertTrue(
            "Dedicated welfare place icon resource should exist.",
            File("src/main/res/drawable/ic_place_welfare.png").exists(),
        )
    }

    @Test
    fun `map shortcut filter row uses dedicated public office asset for public office chip`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapShortcutFilterRow.kt")
                .readText()

        assertTrue(
            "Top shortcut public office chip should use the dedicated public office drawable resource.",
            source.contains("MapShortcutFilterKey.PUBLIC_OFFICE -> R.drawable.ic_place_public_office"),
        )
        assertTrue(
            "Dedicated public office place icon resource should exist.",
            File("src/main/res/drawable/ic_place_public_office.png").exists(),
        )
    }

    @Test
    fun `map shortcut filter row uses dedicated food cafe asset for food cafe chip`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapShortcutFilterRow.kt")
                .readText()

        assertTrue(
            "Top shortcut food cafe chip should use the dedicated food cafe drawable resource.",
            source.contains("MapShortcutFilterKey.FOOD_CAFE -> R.drawable.ic_place_food_cafe"),
        )
        assertTrue(
            "Dedicated food cafe place icon resource should exist.",
            File("src/main/res/drawable/ic_place_food_cafe.png").exists(),
        )
    }
}
