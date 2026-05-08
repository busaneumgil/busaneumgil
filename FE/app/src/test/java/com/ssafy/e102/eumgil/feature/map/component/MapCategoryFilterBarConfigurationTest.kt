package com.ssafy.e102.eumgil.feature.map.component

import com.ssafy.e102.eumgil.core.model.FacilityCategory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapCategoryFilterBarConfigurationTest {
    @Test
    fun `map category filter bar uses compact wheelchair icon asset for toilet chip`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "Toilet category chip should use the compact wheelchair drawable derived from the onboarding source.",
            source.contains("FacilityCategory.TOILET -> R.drawable.ic_user_wheelchair_compact"),
        )
    }

    @Test
    fun `map category filter bar uses provided elevator icon asset for elevator chip`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "Elevator category chip should use the provided elevator drawable resource.",
            source.contains("FacilityCategory.ELEVATOR -> R.drawable.ic_place_elevator"),
        )
    }

    @Test
    fun `map category filter bar enlarges only the elevator icon after compact toilet asset swap`() {
        assertEquals(20, categoryFilterIconSizeDp(FacilityCategory.ELEVATOR))
        assertEquals(18, categoryFilterIconSizeDp(FacilityCategory.TOILET))
        assertEquals(18, categoryFilterIconSizeDp(FacilityCategory.CHARGING_STATION))
    }

    @Test
    fun `map category filter bar uses design token radius for container and chips`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "MAP top category filter overlay should use the 16dp container radius token.",
            source.contains("shape = RoundedCornerShape(EumRadius.scaleL)"),
        )
        assertTrue(
            "MAP top category filter chips should use the 8dp chip radius token.",
            source.contains("shape = RoundedCornerShape(EumRadius.scaleS)"),
        )
    }

    @Test
    fun `map category filter bar keeps leading icons in service main blue`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "MAP top category filter icons should always tint to Primary 600.",
            source.contains("tint = EumPrimary600"),
        )
    }

    @Test
    fun `map category filter bar uses the new charging station asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt")
                .readText()

        assertTrue(
            "Charging station category chip should use the provided charging station drawable resource.",
            source.contains("FacilityCategory.CHARGING_STATION -> R.drawable.ic_place_charging_station"),
        )
        assertTrue(
            "Provided charging station icon resource should exist.",
            File("src/main/res/drawable/ic_place_charging_station.png").exists(),
        )
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
