package com.ssafy.e102.eumgil.feature.map

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapFacilityDetailSheetConfigurationTest {
    @Test
    fun `facility detail and recent destinations use compact wheelchair icon asset for toilet`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Toilet category should map to the compact wheelchair place icon asset in the detail sheet.",
            source.contains("FacilityCategory.TOILET -> R.drawable.ic_user_wheelchair_compact"),
        )
        assertTrue(
            "Recent destinations should reuse the compact wheelchair place icon for toilets.",
            source.contains("PlaceCategory.TOILET -> R.drawable.ic_user_wheelchair_compact"),
        )
        assertTrue(
            "Compact wheelchair drawable should exist for detail and recent destination surfaces.",
            File("src/main/res/drawable/ic_user_wheelchair_compact.png").exists(),
        )
    }

    @Test
    fun `facility detail and recent destinations use provided elevator icon asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Elevator category should map to the provided elevator place icon asset in the detail sheet.",
            source.contains("FacilityCategory.ELEVATOR -> R.drawable.ic_place_elevator"),
        )
        assertTrue(
            "Recent destinations should reuse the provided elevator place icon.",
            source.contains("PlaceCategory.ELEVATOR -> R.drawable.ic_place_elevator"),
        )
        assertTrue(
            "Provided elevator drawable should exist for detail and recent destination surfaces.",
            File("src/main/res/drawable/ic_place_elevator.png").exists(),
        )
    }

    @Test
    fun `facility detail and recent destinations use provided charging station icon asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Charging station category should map to the provided charging station place icon asset in the detail sheet.",
            source.contains("FacilityCategory.CHARGING_STATION -> R.drawable.ic_place_charging_station"),
        )
        assertTrue(
            "Recent destinations should reuse the provided charging station place icon.",
            source.contains("PlaceCategory.CHARGING_STATION -> R.drawable.ic_place_charging_station"),
        )
        assertTrue(
            "Provided charging station drawable should exist for detail and recent destination surfaces.",
            File("src/main/res/drawable/ic_place_charging_station.png").exists(),
        )
    }

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
        assertFalse(
            "Guide message section should no longer be rendered in the place detail sheet body.",
            source.contains("FacilityDetailGuideMessageSection("),
        )
        assertFalse(
            "Empty accessibility placeholder chips should not be rendered when no accessibility info exists.",
            source.contains("map_facility_detail_accessibility_empty"),
        )
    }

    @Test
    fun `facility detail bottom sheet shell removes divider chrome and hides empty detail spacing`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/FacilityDetailBottomSheetShell.kt").readText()

        assertFalse(
            "Detail sheet header/body/action sections should be separated by spacing rather than dividers.",
            source.contains("HorizontalDivider"),
        )
        assertTrue(
            "Detail sheet shell should skip the body column when there is no accessibility content or the sheet is collapsed.",
            source.contains("if (state.hasDetailContent && !isCollapsed)"),
        )
    }

    @Test
    fun `facility detail bottom sheet keeps collapse handle but moves bookmark action to the header`() {
        val shellSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/FacilityDetailBottomSheetShell.kt").readText()
        val screenSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "The drag handle should expose a collapse and expand label instead of a close label.",
            shellSource.contains("map_facility_detail_sheet_toggle"),
        )
        assertTrue(
            "The detail sheet header should render injected actions from the screen so bookmark can replace the old close icon.",
            shellSource.contains("headerActionContent?.let"),
        )
        assertTrue(
            "Dragging down should collapse the place sheet instead of dismissing and clearing selection state.",
            shellSource.contains("isCollapsed =") &&
                shellSource.contains("sheetOffsetPx >= collapseThresholdPx") &&
                !shellSource.contains("sheetOffsetPx >= dismissThresholdPx"),
        )
        assertTrue(
            "Collapsed state should keep the fixed action area visible while hiding the detailed body content.",
            shellSource.contains("if (state.hasDetailContent && !isCollapsed)"),
        )
        assertFalse(
            "The explicit close icon should be removed from the place sheet header once bookmark occupies that slot.",
            shellSource.contains("map_facility_detail_close") ||
                shellSource.contains("IconButton(onClick = onDismiss)"),
        )
        assertTrue(
            "MapScreen should provide the bookmark action through the sheet header slot.",
            screenSource.contains("headerActionContent = {") &&
                screenSource.contains("FacilityDetailBookmarkActionButton("),
        )
    }

    @Test
    fun `facility detail collapsed sheet keeps minimum height and bottom action order`() {
        val shellSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/FacilityDetailBottomSheetShell.kt").readText()
        val screenSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()
        val actionContentSection =
            screenSource
                .substringAfter("actionContent = {")
                .substringBefore("facilityDetailSheetUiState.bookmarkErrorMessage")

        assertTrue(
            "Collapsed place sheet should keep enough height for title, summary, and fixed route actions.",
            shellSource.contains("FacilityDetailCollapsedMinHeight") &&
                shellSource.contains("heightIn(min = FacilityDetailCollapsedMinHeight"),
        )
        assertTrue(
            "Collapsed title should be one line while expanded title can use two lines.",
            shellSource.contains("maxLines = if (isCollapsed) 1 else 2"),
        )
        assertTrue(
            "Bottom actions should keep origin before destination in the fixed action row.",
            actionContentSection.indexOf("map_facility_detail_set_origin_action") <
                actionContentSection.indexOf("map_facility_detail_set_destination_action") &&
                !actionContentSection.contains("FacilityDetailBookmarkActionButton("),
        )
        assertTrue(
            "Origin and destination CTAs should split the row evenly after the bookmark action moves to the header.",
            actionContentSection.contains(".weight(1f)") &&
                !actionContentSection.contains("weight(1.15f)"),
        )
        assertTrue(
            "Origin and destination action labels should share the same button text style.",
            screenSource.contains("style = MaterialTheme.typography.labelLarge") &&
                screenSource.contains("fontWeight = FontWeight.SemiBold"),
        )
    }

    @Test
    fun `map viewport state routes preview metadata by editing target before selected endpoints`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Map viewport state should treat an origin preview as the effective origin endpoint.",
            source.contains("preview?.editingTarget == RouteEditingTarget.ORIGIN") &&
                source.contains("uiState.selectedOrigin"),
        )
        assertTrue(
            "Map viewport state should treat a destination preview as the effective destination endpoint.",
            source.contains("preview?.editingTarget == RouteEditingTarget.DESTINATION") &&
                source.contains("uiState.selectedDestination"),
        )
        assertTrue(
            "Selected destination summary should use the effective viewport destination so preview screens announce the searched place name.",
            source.contains("selectedDestinationSummaryText(destination = viewportDestination)"),
        )
        assertTrue(
            "Projected destination metadata should use the effective viewport destination so preview pins reuse the searched place name.",
            source.contains("selectedDestinationName = viewportDestination?.name"),
        )
    }

    @Test
    fun `facility detail and recent destinations use dedicated public office icon asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Public office category should map to a dedicated place icon asset in the detail sheet.",
            source.contains("FacilityCategory.PUBLIC_OFFICE -> R.drawable.ic_place_public_office"),
        )
        assertTrue(
            "Recent destinations should reuse the dedicated public office place icon.",
            source.contains("PlaceCategory.PUBLIC_OFFICE -> R.drawable.ic_place_public_office"),
        )
        assertTrue(
            "Dedicated public office drawable should exist for detail and recent destination surfaces.",
            File("src/main/res/drawable/ic_place_public_office.png").exists(),
        )
    }

    @Test
    fun `facility detail and recent destinations use dedicated tourist icon asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Tourist spot category should map to a dedicated place icon asset in the detail sheet.",
            source.contains("FacilityCategory.TOURIST_SPOT -> R.drawable.ic_place_tourist_spot"),
        )
        assertTrue(
            "Tourist attraction category should map to a dedicated place icon asset in the detail sheet.",
            source.contains("FacilityCategory.TOURIST_ATTRACTION -> R.drawable.ic_place_tourist_spot"),
        )
        assertTrue(
            "Recent destinations should reuse the dedicated tourist place icon for tourist spots.",
            source.contains("PlaceCategory.TOURIST_SPOT -> R.drawable.ic_place_tourist_spot"),
        )
        assertTrue(
            "Recent destinations should reuse the dedicated tourist place icon for tourist attractions.",
            source.contains("PlaceCategory.TOURIST_ATTRACTION -> R.drawable.ic_place_tourist_spot"),
        )
        assertTrue(
            "Dedicated tourist place drawable should exist for detail and recent destination surfaces.",
            File("src/main/res/drawable/ic_place_tourist_spot.png").exists(),
        )
    }

    @Test
    fun `facility detail and recent destinations use dedicated accommodation icon asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Accommodation category should map to a dedicated place icon asset in the detail sheet.",
            source.contains("FacilityCategory.ACCOMMODATION -> R.drawable.ic_place_accommodation"),
        )
        assertTrue(
            "Recent destinations should reuse the dedicated accommodation place icon.",
            source.contains("PlaceCategory.ACCOMMODATION -> R.drawable.ic_place_accommodation"),
        )
        assertTrue(
            "Dedicated accommodation place drawable should exist for detail and recent destination surfaces.",
            File("src/main/res/drawable/ic_place_accommodation.png").exists() ||
                File("src/main/res/drawable/ic_place_accommodation.xml").exists(),
        )
    }

    @Test
    fun `facility detail and recent destinations use dedicated healthcare icon asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Healthcare category should map to a dedicated place icon asset in the detail sheet.",
            source.contains("FacilityCategory.HEALTHCARE -> R.drawable.ic_place_healthcare"),
        )
        assertTrue(
            "Recent destinations should reuse the dedicated healthcare place icon.",
            source.contains("PlaceCategory.HEALTHCARE -> R.drawable.ic_place_healthcare"),
        )
        assertTrue(
            "Dedicated healthcare place drawable should exist for detail and recent destination surfaces.",
            File("src/main/res/drawable/ic_place_healthcare.png").exists() ||
                File("src/main/res/drawable/ic_place_healthcare.xml").exists(),
        )
    }

    @Test
    fun `facility detail and recent destinations use dedicated welfare icon asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Welfare category should map to a dedicated place icon asset in the detail sheet.",
            source.contains("FacilityCategory.WELFARE -> R.drawable.ic_place_welfare"),
        )
        assertTrue(
            "Recent destinations should reuse the dedicated welfare place icon.",
            source.contains("PlaceCategory.WELFARE -> R.drawable.ic_place_welfare"),
        )
        assertTrue(
            "Dedicated welfare place drawable should exist for detail and recent destination surfaces.",
            File("src/main/res/drawable/ic_place_welfare.png").exists(),
        )
    }

    @Test
    fun `facility detail and recent destinations split restaurant icon from food cafe icon asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Food cafe category should map to a dedicated place icon asset in the detail sheet.",
            source.contains("FacilityCategory.FOOD_CAFE -> R.drawable.ic_place_food_cafe"),
        )
        assertTrue(
            "Restaurant category should map to the dedicated restaurant icon asset in the detail sheet.",
            source.contains("FacilityCategory.RESTAURANT -> R.drawable.ic_place_restaurant"),
        )
        assertTrue(
            "Recent destinations should reuse the dedicated food cafe place icon for food cafes.",
            source.contains("PlaceCategory.FOOD_CAFE -> R.drawable.ic_place_food_cafe"),
        )
        assertTrue(
            "Recent destinations should reuse the dedicated restaurant place icon for restaurants.",
            source.contains("PlaceCategory.RESTAURANT -> R.drawable.ic_place_restaurant"),
        )
        assertTrue(
            "Dedicated food cafe place drawable should exist for detail and recent destination surfaces.",
            File("src/main/res/drawable/ic_place_food_cafe.png").exists(),
        )
        assertTrue(
            "Dedicated restaurant place drawable should exist for detail and recent destination surfaces.",
            File("src/main/res/drawable/ic_place_restaurant.xml").exists(),
        )
    }

    @Test
    fun `facility detail and recent destinations use dedicated other icon asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Other facility category should map to a dedicated place icon asset in the detail sheet.",
            source.contains("FacilityCategory.OTHER -> R.drawable.ic_place_other"),
        )
        assertTrue(
            "Recent destinations should reuse the dedicated other place icon.",
            source.contains("PlaceCategory.OTHER -> R.drawable.ic_place_other"),
        )
        assertTrue(
            "Dedicated other place drawable should exist for detail and recent destination surfaces.",
            File("src/main/res/drawable/ic_place_other.png").exists(),
        )
    }

    @Test
    fun `recent destinations treat missing category as dedicated other icon`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()
        val recentDestinationIconSection =
            source
                .substringAfter("private fun recentDestinationIcon(category: PlaceCategory?): Int =")
                .substringBefore("private const val EARTH_RADIUS_METERS")

        assertTrue(
            "Recent destinations should render missing categories with the dedicated other place icon so uncategorized entries stay visually aligned with the other category.",
            recentDestinationIconSection.contains("null -> R.drawable.ic_place_other"),
        )
    }

    @Test
    fun `map tap place detail uses other icon for unclassified categories`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()
        val mapTapIconSection =
            source
                .substringAfter("private fun mapTapDetailPlaceIconRes(detail: MapTappedPlaceDetail): Int =")
                .substringBefore("@Composable")

        assertTrue(
            "Map tap place detail should treat missing category mappings as the dedicated other place icon.",
            mapTapIconSection.contains("when (detail.category)"),
        )
        assertTrue(
            "Map tap place detail should map the explicit other category to the dedicated other place icon.",
            mapTapIconSection.contains("PlaceCategory.OTHER -> R.drawable.ic_place_other"),
        )
        assertTrue(
            "Map tap place detail should map null categories to the dedicated other place icon.",
            mapTapIconSection.contains("null -> R.drawable.ic_place_other"),
        )
        assertTrue(
            "Recognized place categories should continue reusing the recent destination icon mapping.",
            mapTapIconSection.contains("else -> recentDestinationIcon(detail.category)"),
        )
    }

    @Test
    fun `facility detail route entry button reuses the active current location icon`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Facility detail route entry CTA should reuse the active current-location button icon asset so it renders as a filled white icon on the primary CTA.",
            source.contains("iconRes = R.drawable.ic_route_start_navigation_button"),
        )
        assertTrue(
            "The active current-location button asset should exist before the facility detail CTA reuses it.",
            File("src/main/res/drawable/ic_route_start_navigation_button.png").exists(),
        )
    }

    @Test
    fun `facility detail sheet renders phone row below address and wires dial action`() {
        val shellSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/FacilityDetailBottomSheetShell.kt").readText()
        val stringsSource =
            File("src/main/res/values/strings.xml").readText()
        val screenSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "The bottom-sheet shell should expose an optional phone number field so place detail can render the backend phone value under the address.",
            shellSource.contains("val phoneNumber: String? = null"),
        )
        assertTrue(
            "The bottom-sheet shell should render the dedicated phone copy string so the phone number appears as its own tappable line.",
            shellSource.contains("map_facility_detail_phone_value"),
        )
        assertTrue(
            "The phone line should use underlined text so users can recognize it as a tappable call action.",
            shellSource.contains("TextDecoration.Underline"),
        )
        assertTrue(
            "The phone row should render the dedicated contact icon beside the number so the affordance stays clear even without a text glyph prefix.",
            shellSource.contains("R.drawable.ic_permission_contacts"),
        )
        assertTrue(
            "The visible phone label should now keep only the number text because the call affordance comes from the separate icon.",
            stringsSource.contains("<string name=\"map_facility_detail_phone_value\">%1\$s</string>"),
        )
        assertTrue(
            "The map screen should wire the phone row tap back into the feature action so tapping the number can open the dialer.",
            screenSource.contains("onPhoneClick =") &&
                screenSource.contains("MapUiAction.FacilityPhoneClicked"),
        )
    }

    @Test
    fun `recent destination route button reuses the shared route entry icon`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/RecentDestinationBottomSheetShell.kt").readText()

        assertTrue(
            "Recent destination CTA should reuse the shared route-entry icon so the map home sheet matches the facility detail action.",
            source.contains("R.drawable.ic_route_start_navigation_button"),
        )
    }

    @Test
    fun `recent destination accessibility chips use compact labels`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt").readText()

        assertTrue(
            "Recent destination accessible toilet chip should use the compact noun label without the trailing status suffix.",
            source.contains("\"accessible-toilet\" -> \"장애인 화장실\""),
        )
        assertTrue(
            "Recent destination elevator chip should use the compact noun label without the trailing status suffix.",
            source.contains("\"elevator\" -> \"엘리베이터\""),
        )
    }
}
