package com.ssafy.e102.eumgil.feature.map.component

import com.ssafy.e102.eumgil.core.model.FacilityCategory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapCategoryFilterBarConfigurationTest {
    @Test
    fun `map category filter bar uses elevator icon asset for elevator chip`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "Elevator category chip should use the elevator drawable resource.",
            source.contains("FacilityCategory.ELEVATOR -> R.drawable.ic_route_elevator"),
        )
    }

    @Test
    fun `map category filter bar enlarges only the elevator icon`() {
        assertEquals(20, categoryFilterIconSizeDp(FacilityCategory.ELEVATOR))
        assertEquals(18, categoryFilterIconSizeDp(FacilityCategory.TOILET))
        assertEquals(18, categoryFilterIconSizeDp(FacilityCategory.CHARGING_STATION))
    }
}
