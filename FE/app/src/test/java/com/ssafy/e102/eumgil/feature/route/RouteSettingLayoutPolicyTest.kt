package com.ssafy.e102.eumgil.feature.route

import androidx.compose.ui.geometry.Size
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteSettingLayoutPolicyTest {
    @Test
    fun `route setting keeps default content within one non scrolling screen`() {
        val policy = routeSettingLayoutPolicy()

        assertFalse(policy.allowsDefaultVerticalScroll)
        assertEquals(RouteSettingCtaPlacement.BottomBar, policy.ctaPlacement)
        assertEquals(RouteSettingMapHeightPolicy.FillRemainingCenterSpace, policy.mapHeightPolicy)
        assertEquals(3, policy.maxVisibleOptionCards)
        assertFalse(policy.showsOptionSectionSupportingText)
        assertEquals("출발", policy.originLabel)
        assertEquals("도착", policy.destinationLabel)
        assertTrue(policy.showsWaypointSwapButton)
        assertEquals(RouteWaypointMarkerStyle.LinkedPin, policy.waypointMarkerStyle)
        assertEquals(RouteWaypointConnectorStyle.VerticalLine, policy.waypointConnectorStyle)
        assertFalse(policy.showsWaypointDivider)
        assertEquals(RouteWaypointSwapButtonStyle.Borderless, policy.waypointSwapButtonStyle)
        assertEquals(RouteSettingOptionContainer.BottomSheet, policy.optionContainer)
        assertEquals(RouteSettingTravelModeTabShape.SegmentedPill, policy.travelModeTabShape)
        assertEquals(2, policy.visibleAccessibilityChipCount)
        assertTrue(policy.mapFillsRemainingCenterSpace)
        assertTrue(policy.bottomSheetEdgeToEdge)
        assertEquals(RouteSettingSheetContainerColor.White, policy.sheetContainerColor)
        assertFalse(policy.showsRecommendedBadge)
        assertEquals(RouteSettingStartCtaIcon.NavigationPointer, policy.startCtaIcon)
        assertEquals(RouteSettingCtaIconTint.OnPrimary, policy.startCtaIconTint)
        assertFalse(policy.bottomSheetFlushToWindowBottom)
        assertEquals(RouteSettingSheetElevation.None, policy.sheetElevation)
        assertEquals(RouteSettingSheetBorder.None, policy.sheetBorder)
        assertEquals(RouteSettingOptionCardContainerColor.White, policy.optionCardContainerColor)
        assertEquals(RouteSettingTravelModeIcon.WalkImage, policy.walkTabIcon)
        assertEquals(RouteSettingTravelModeIcon.TransitImage, policy.transitTabIcon)
        assertEquals(RouteSettingTravelModeActiveColor.PrimaryBlue, policy.travelModeActiveColor)
        assertEquals(RouteSettingTravelModeInactiveColor.Grey700, policy.travelModeInactiveColor)
        assertEquals(RouteSettingTravelModeIconSize.Emphasized, policy.travelModeIconSize)
    }

    @Test
    fun `route setting screen uses shared bottom bar instead of inline sheet cta`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val screenSection =
            source
                .substringAfter("fun RouteSettingScreen(")
                .substringBefore("@Composable\nfun RouteDetailScreen")
        val sheetSection =
            source
                .substringAfter("private fun RouteSettingRouteSheet(")
                .substringBefore("@Composable\nprivate fun RouteWalkOptionSection")

        assertTrue(
            "Route selection should attach the start CTA with Scaffold.bottomBar to match route detail.",
            screenSection.contains("bottomBar = {"),
        )
        assertTrue(
            "Route selection should reuse the shared bottom bar component.",
            screenSection.contains("RouteSettingBottomBar("),
        )
        assertFalse(
            "The route sheet should no longer render the inline CTA content inside the sheet.",
            sheetSection.contains("RouteSettingCtaContent("),
        )
    }

    @Test
    fun `route waypoint pin marker uses shared png asset`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val asset = File("src/main/res/drawable-nodpi/ic_route_waypoint_pin.png")

        assertTrue(
            "Waypoint pin marker should render the shared PNG asset instead of a reconstructed Canvas path.",
            source.contains("painterResource(id = R.drawable.ic_route_waypoint_pin)"),
        )
        assertTrue("Waypoint pin asset should exist in drawable-nodpi.", asset.exists())
    }

    @Test
    fun `route waypoint swap icon uses separate up and down arrows`() {
        val segments = routeWaypointSwapIconSegments(Size(width = 18f, height = 24f))

        assertEquals(6, segments.size)

        val upperStem = segments[0]
        val upperLeftHead = segments[1]
        val upperRightHead = segments[2]
        val lowerStem = segments[3]
        val lowerLeftHead = segments[4]
        val lowerRightHead = segments[5]

        assertTrue(upperStem.end.y < upperStem.start.y)
        assertEquals(upperStem.end, upperLeftHead.start)
        assertEquals(upperStem.end, upperRightHead.start)
        assertTrue(upperLeftHead.end.x < upperStem.end.x)
        assertTrue(upperRightHead.end.x > upperStem.end.x)
        assertTrue(upperLeftHead.end.y > upperStem.end.y)
        assertTrue(upperRightHead.end.y > upperStem.end.y)

        assertTrue(lowerStem.end.y > lowerStem.start.y)
        assertEquals(lowerStem.end, lowerLeftHead.start)
        assertEquals(lowerStem.end, lowerRightHead.start)
        assertTrue(lowerLeftHead.end.x < lowerStem.end.x)
        assertTrue(lowerRightHead.end.x > lowerStem.end.x)
        assertTrue(lowerLeftHead.end.y < lowerStem.end.y)
        assertTrue(lowerRightHead.end.y < lowerStem.end.y)

        assertTrue(upperStem.start.x < lowerStem.start.x)
        assertTrue(lowerStem.start.x - upperStem.start.x >= 9f)
        assertTrue(lowerStem.start.y - upperStem.start.y >= 6f)
    }

    @Test
    fun `route waypoint supporting text aligns under the place name column and uses lighter meta color`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()

        assertTrue(
            "Waypoint rows should measure the widest label instead of relying on a hardcoded width.",
            source.contains("rememberTextMeasurer()"),
        )
        assertTrue(
            "Waypoint rows should baseline-align the status label with the place name.",
            source.contains("alignByBaseline()"),
        )
        assertTrue(
            "Waypoint address text should start under the place name column using the measured label width.",
            source.contains("Spacer(modifier = Modifier.width(labelWidth))"),
        )
        assertTrue(
            "Waypoint supporting text should use the lighter neutral meta color from the design convention.",
            source.contains("RouteWaypointSupportingTextColor = Color(0xFF6B7280)"),
        )
    }

    @Test
    fun `route surfaces use documented corner and shadow tokens`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()

        assertTrue(
            "Route cards should use the section container radius token.",
            source.contains("shape = RoundedCornerShape(RouteSectionCardCornerRadius)"),
        )
        assertTrue(
            "Route CTA should use the standard button corner radius token.",
            source.contains("shape = RoundedCornerShape(RouteButtonCornerRadius)"),
        )
        assertTrue(
            "Route map floating controls should use the floating control radius token.",
            source.contains("shape = RoundedCornerShape(RouteFloatingControlCornerRadius)"),
        )
        assertTrue(
            "Route map floating controls should keep the documented floating control elevation.",
            source.contains("shadowElevation = RouteFloatingControlElevation"),
        )
        assertTrue(
            "The sticky bottom CTA bar should remain flat and avoid a separating shadow seam.",
            source.contains("shadowElevation = 0.dp"),
        )
        assertTrue(
            "Route badges should use the compact chip radius token instead of pill rounding.",
            source.contains("shape = RoundedCornerShape(RouteCompactChipCornerRadius)"),
        )
    }

    @Test
    fun `route start cta uses provided png icon without tint override`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val asset = File("src/main/res/drawable/ic_route_start_navigation_button.png")

        assertTrue(
            "Route start CTA should use the provided PNG icon asset for the button.",
            source.contains("R.drawable.ic_route_start_navigation_button"),
        )
        assertTrue(
            "Route start CTA should tint the button icon white on the primary background.",
            source.contains("tint = MaterialTheme.colorScheme.onPrimary"),
        )
        assertTrue("Route start CTA PNG icon should exist in drawable.", asset.exists())
    }

    @Test
    fun `route option prefix stays plain text while labels keep compact chip styling`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val prefixSection =
            source
                .substringAfter("private fun RouteOptionPrefixBadge(")
                .substringBefore("@Composable\nprivate fun RouteOptionDetailArrowButton")

        assertFalse(
            "The recommended prefix text should not render as a filled chip background.",
            prefixSection.contains("Surface("),
        )
        assertTrue(
            "The recommended prefix should stay as colored text only.",
            prefixSection.contains("Text("),
        )
    }

    @Test
    fun `route detail summary removes headline copy and border for flat metrics layout`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val summarySection =
            source
                .substringAfter("private fun RouteDetailSummaryCard(")
                .substringBefore("@Composable\nprivate fun RouteDetailMetricRow")

        assertFalse(
            "Detail summary should not keep the route risk badge when the header copy is removed.",
            summarySection.contains("RouteRiskChip("),
        )
        assertFalse(
            "Detail summary should not render the destination headline copy under the title divider.",
            summarySection.contains("route_setting_detail_summary_destination"),
        )
        assertFalse(
            "Detail summary should not render the destination supporting address in the flat layout.",
            summarySection.contains("selectedRoute.destination.supportingText"),
        )
        assertFalse(
            "Detail summary should not keep a card border in the refactored flat layout.",
            summarySection.contains("border = BorderStroke"),
        )
    }

    @Test
    fun `route setting hides walk fallback notice and debug card from the screen`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()

        assertFalse(
            "The route screen should not render the walk-first fallback notice card while transit recovery is in progress.",
            source.contains("route_setting_fallback_notice_title"),
        )
        assertFalse(
            "The route screen should not expose the route debug card in the FE UI.",
            source.contains("RouteDebugStateCard(debugMessage = uiState.loadDebugMessage)"),
        )
    }

    @Test
    fun `route preview map does not show selected route badge overlay`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val previewMapSection =
            source
                .substringAfter("if (previewMap.coordinates.isNotEmpty()) {")
                .substringBefore("RouteMapControls(")

        assertFalse(
            "Preview map should not render the selected route status badge over the map.",
            previewMapSection.contains("RouteMapStatusBadge("),
        )
    }

    @Test
    fun `route detail screen adds title divider and uses attached guide rows`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val detailScreenSection =
            source
                .substringAfter("fun RouteDetailScreen(")
                .substringBefore("@Composable\nprivate fun RouteDetailSummaryCard")
        val stepsSection =
            source
                .substringAfter("private fun RouteDetailStepsSection(")
                .substringBefore("@Composable\nprivate fun RouteDetailOriginHeader")

        assertTrue(
            "Detail screen should add a divider directly under the title area.",
            detailScreenSection.contains("HorizontalDivider("),
        )
        assertFalse(
            "Guide rows should start from the departure row instead of dropping the first step.",
            stepsSection.contains("steps.drop(1)"),
        )
        assertFalse(
            "The refactored guide list should not keep a separate origin header card.",
            stepsSection.contains("RouteDetailOriginHeader("),
        )
        assertFalse(
            "Guide rows should no longer nest each step inside its own card.",
            stepsSection.contains("RouteDetailStepCard(step = step)"),
        )
        assertTrue(
            "The first attached row should render the departure cell explicitly.",
            stepsSection.contains("RouteDetailOriginStepRow("),
        )
        assertTrue(
            "Subsequent rows should render as attached guide rows.",
            stepsSection.contains("RouteDetailStepRow("),
        )
        assertTrue(
            "Attached guide rows should be visually separated with dividers, not nested cards.",
            stepsSection.contains("HorizontalDivider("),
        )
    }

    @Test
    fun `route detail departure and arrival reuse labeled rail pins with detail icon sizing`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val leadingIconSection =
            source
                .substringAfter("private fun RouteDetailStepLeadingIcon(")
                .substringBefore("@Composable\nprivate fun RouteScreenTopBar")

        assertTrue(
            "Route detail start should reuse the in-use labeled origin pin asset.",
            source.contains("RouteDetailStepKind.START -> R.drawable.ic_navigation_rail_origin_pin"),
        )
        assertTrue(
            "Route detail arrival should reuse the in-use labeled destination pin asset.",
            source.contains("RouteDetailStepKind.ARRIVAL -> R.drawable.ic_navigation_rail_destination_pin"),
        )
        assertTrue(
            "Transit detail rows should reuse the shared bus icon from the route map surface.",
            source.contains("RouteDetailStepKind.BUS -> R.drawable.ic_place_bus"),
        )
        assertTrue(
            "Transit detail rows should map subway boarding to the dedicated subway asset.",
            source.contains("RouteDetailStepKind.SUBWAY -> R.drawable.ic_route_subway"),
        )
        assertTrue(
            "Route detail leading icons should branch explicitly for the labeled rail pin treatment.",
            leadingIconSection.contains("if (usesLabeledWaypointPinIcon)"),
        )
        assertTrue(
            "Labeled rail pins in route detail should keep the shared 22x24 aspect ratio while matching the other detail icon height.",
            leadingIconSection.contains("modifier = Modifier.size(width = RouteDetailWaypointPinWidth, height = RouteDetailWaypointPinHeight)"),
        )
        assertTrue(
            "Route detail leading glyph icons should route through a dedicated size helper so transit icons can be slightly reduced.",
            leadingIconSection.contains("modifier = Modifier.size(kind.leadingIconSize())"),
        )
        assertTrue(
            "Route detail should preserve a dedicated helper for labeled start/destination pin steps.",
            source.contains("private fun RouteDetailStepKind.usesLabeledWaypointPinIcon(): Boolean ="),
        )
        assertTrue(
            "Route detail should keep a dedicated helper for smaller transit icon sizing.",
            source.contains("private fun RouteDetailStepKind.leadingIconSize(): Dp ="),
        )
        assertTrue(
            "Route detail should document the reduced transit icon size token.",
            source.contains("private val RouteDetailTransitLeadingIconSize = 20.dp"),
        )
        assertTrue(
            "Route detail should document the waypoint pin width token.",
            source.contains("private val RouteDetailWaypointPinWidth = 22.dp"),
        )
        assertTrue(
            "Route detail should document the waypoint pin height token.",
            source.contains("private val RouteDetailWaypointPinHeight = 24.dp"),
        )
    }

    @Test
    fun `route setting suppresses ripple only on taps that open other route screens`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val waypointSection =
            source
                .substringAfter("private fun RouteWaypointRow(")
                .substringBefore("@Composable\nprivate fun RouteOriginStatusText")
        val detailArrowSection =
            source
                .substringAfter("private fun RouteOptionDetailArrowButton(")
                .substringBefore("/*")

        assertTrue(
            "Waypoint rows should suppress ripple because they open the search screen for origin/destination editing.",
            waypointSection.contains("indication = null"),
        )
        assertTrue(
            "Waypoint rows should keep a dedicated interaction source when ripple is suppressed.",
            waypointSection.contains("MutableInteractionSource()"),
        )
        assertTrue(
            "Route option detail arrows should suppress ripple because they open the route detail screen.",
            detailArrowSection.contains("indication = null"),
        )
        assertTrue(
            "Route option detail arrows should keep a dedicated interaction source when ripple is suppressed.",
            detailArrowSection.contains("MutableInteractionSource()"),
        )
    }

    @Test
    fun `resolved current location origin promotes current location label and moves address below`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val waypointCardSection =
            source
                .substringAfter("private fun RouteWaypointCard(")
                .substringBefore("@Composable\nprivate fun RouteWaypointLinkedMarkers")
        val originPresentationSection =
            source
                .substringAfter("private fun resolveOriginWaypointPresentation(")
                .substringBefore("@Composable\nprivate fun RouteWaypointLinkedMarkers")

        assertTrue(
            "Origin rows should resolve a dedicated current-location presentation before rendering the waypoint text.",
            waypointCardSection.contains("val originPresentation = resolveOriginWaypointPresentation("),
        )
        assertTrue(
            "The origin row should render the resolved current-location title instead of the raw place name.",
            waypointCardSection.contains("name = originPresentation.name"),
        )
        assertTrue(
            "The origin row should render the resolved supporting address under the current-location title.",
            waypointCardSection.contains("supportingText = originPresentation.supportingText"),
        )
        assertTrue(
            "The origin row should be able to suppress the blue current-location status once it becomes the primary title.",
            waypointCardSection.contains("status = originPresentation.status"),
        )
        assertTrue(
            "Resolved current-location rows should detect the current-location status label explicitly.",
            originPresentationSection.contains("status?.label == CURRENT_LOCATION_WAYPOINT_NAME"),
        )
        assertTrue(
            "Resolved current-location rows should keep the address on the supporting line and fall back to the original name when needed.",
            originPresentationSection.contains("supportingText?.takeIf(String::isNotBlank) ?: name"),
        )
    }
}
