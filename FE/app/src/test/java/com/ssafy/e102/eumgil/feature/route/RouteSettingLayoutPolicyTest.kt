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
        assertEquals(RouteSettingCtaPlacement.BottomSheetContent, policy.ctaPlacement)
        assertEquals(RouteSettingMapHeightPolicy.FillRemainingCenterSpace, policy.mapHeightPolicy)
        assertEquals(2, policy.maxVisibleOptionCards)
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
        assertTrue(policy.bottomSheetFlushToWindowBottom)
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
}
