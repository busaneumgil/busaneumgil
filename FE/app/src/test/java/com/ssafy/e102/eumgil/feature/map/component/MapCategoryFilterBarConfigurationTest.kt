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

    @Test
    fun `map category filter bar uses dedicated tourist icon asset for tourist categories`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "Tourist spot category chip should use the dedicated tourist drawable resource.",
            source.contains("FacilityCategory.TOURIST_SPOT -> R.drawable.ic_place_tourist_spot"),
        )
        assertTrue(
            "Tourist attraction category chip should use the dedicated tourist drawable resource.",
            source.contains("FacilityCategory.TOURIST_ATTRACTION -> R.drawable.ic_place_tourist_spot"),
        )
    }

    @Test
    fun `map category filter bar uses dedicated accommodation icon asset for accommodation category`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "Accommodation category chip should use the dedicated accommodation drawable resource.",
            source.contains("FacilityCategory.ACCOMMODATION -> R.drawable.ic_place_accommodation"),
        )
    }

    @Test
    fun `map category filter bar uses dedicated healthcare icon asset for healthcare category`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "Healthcare category chip should use the dedicated healthcare drawable resource.",
            source.contains("FacilityCategory.HEALTHCARE -> R.drawable.ic_place_healthcare"),
        )
    }

    @Test
    fun `map category filter bar uses dedicated welfare icon asset for welfare category`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "Welfare category chip should use the dedicated welfare drawable resource.",
            source.contains("FacilityCategory.WELFARE -> R.drawable.ic_place_welfare"),
        )
        assertTrue(
            "Dedicated welfare place icon resource should exist.",
            File("src/main/res/drawable/ic_place_welfare.png").exists(),
        )
    }

    @Test
    fun `map category filter bar uses dedicated public office icon asset for public office category`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "Public office category chip should use the dedicated public office drawable resource.",
            source.contains("FacilityCategory.PUBLIC_OFFICE -> R.drawable.ic_place_public_office"),
        )
        assertTrue(
            "Dedicated public office place icon resource should exist.",
            File("src/main/res/drawable/ic_place_public_office.png").exists(),
        )
    }

    @Test
    fun `map category filter bar uses dedicated food cafe icon asset for food categories`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "Food cafe category chip should use the dedicated food cafe drawable resource.",
            source.contains("FacilityCategory.FOOD_CAFE -> R.drawable.ic_place_food_cafe"),
        )
        assertTrue(
            "Restaurant category chip should use the dedicated food cafe drawable resource.",
            source.contains("FacilityCategory.RESTAURANT -> R.drawable.ic_place_food_cafe"),
        )
    }
}
