package com.ssafy.e102.eumgil.feature.map

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapFacilityDetailSheetConfigurationTest {
    @Test
    fun `facility detail sheet removes legacy section labels and guide copy`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertFalse(
            "Accessibility tag title should be removed from the place detail sheet body.",
            source.contains("map_facility_detail_accessibility_section_title"),
        )
        assertFalse(
            "Guide section title should be removed from the place detail sheet body.",
            source.contains("map_facility_detail_info_section_title"),
        )
        assertFalse(
            "CTA supporting copy should be removed from the place detail action area.",
            source.contains("map_facility_detail_action_supporting_route_setting"),
        )
        assertFalse(
            "Legacy guide card should no longer be rendered in the place detail sheet.",
            source.contains("FacilityDetailSlotCard("),
        )
    }

    @Test
    fun `facility detail bottom sheet shell removes divider chrome`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/FacilityDetailBottomSheetShell.kt").readText()

        assertFalse(
            "Detail sheet header/body/action sections should be separated by spacing rather than dividers.",
            source.contains("HorizontalDivider"),
        )
    }

    @Test
    fun `facility detail uses dedicated public office icon asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Public office category should map to a dedicated place icon asset.",
            source.contains("FacilityCategory.PUBLIC_OFFICE -> R.drawable.ic_place_public_office"),
        )
        assertTrue(
            "Dedicated public office drawable should exist for the detail header.",
            File("src/main/res/drawable/ic_place_public_office.xml").exists(),
        )
    }
}
