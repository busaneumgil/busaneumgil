package com.ssafy.e102.eumgil.feature.map

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MapScreenNavigationPolicyTest {
    @Test
    fun `facility detail route action suppresses ripple for map to route-setting navigation`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt")
                .readText()

        assertTrue(
            "Facility detail route CTA should keep using the primary route label from the map screen.",
            source.contains("R.string.map_facility_detail_route_entry_action"),
        )
        assertTrue(
            "Facility detail route CTA should suppress ripple because it leaves the map for route setting.",
            source.contains("NoRippleMapPrimaryActionButton("),
        )
    }
}
