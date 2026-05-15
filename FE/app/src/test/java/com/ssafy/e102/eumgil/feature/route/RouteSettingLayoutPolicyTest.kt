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
    fun `route setting screen keeps shared bottom bar and removes card inline cta`() {
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
                .substringBefore("@Composable\nprivate fun RouteOptionSection")

        assertTrue(
            "Route selection should render the start CTA as a floating overlay aligned to the bottom.",
            screenSection.contains("RouteSettingBottomBar(") &&
                screenSection.contains("modifier = Modifier.align(Alignment.BottomCenter)"),
        )
        assertTrue(
            "Route selection should reuse the shared bottom bar component.",
            screenSection.contains("RouteSettingBottomBar("),
        )
        assertFalse(
            "The route sheet should no longer render the inline CTA content inside the sheet.",
            sheetSection.contains("RouteSettingCtaContent("),
        )
        assertFalse(
            "Transit result cards should not render an inline start button; start remains in the shared bottom bar.",
            source.contains("RouteInlineStartActionButton(onClick = onStartClick)"),
        )
    }

    @Test
    fun `route setting transit result list implements UIUX plan skeleton`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val optionSection =
            source
                .substringAfter("private fun RouteOptionSection(")
                .substringBefore("@OptIn(ExperimentalLayoutApi::class)")

        assertFalse("Transit results should not render filter and sort controls in the sheet.", optionSection.contains("RouteTransitResultControls("))
        assertTrue("Transit results should render the segment ratio bar.", source.contains("RouteTransitSegmentRatioBar("))
        assertTrue("Transit results should render bus or subway option labels.", source.contains("RouteTransitOptionSummary("))
        assertTrue("Initial route loading should use the centered spinner state.", source.contains("RouteSearchLoadingState()"))
        assertFalse("Transit cards should not show inline start on selected routes.", source.contains("card.travelMode == RouteTravelMode.TRANSIT && card.isSelected"))
        assertFalse("Transit cards should not keep the left radio selection indicator.", optionSection.contains("RouteOptionSelectionIndicator("))
        assertTrue("Visible route options should stay capped at three.", source.contains("take(MAX_VISIBLE_OPTION_CARD_COUNT)"))
        assertTrue("The max visible route option count should remain three.", source.contains("private const val MAX_VISIBLE_OPTION_CARD_COUNT = 3"))
    }

    @Test
    fun `route setting implements blue route search header and walk map preview carousel`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val screenSection =
            source
                .substringAfter("fun RouteSettingScreen(")
                .substringBefore("if (isDuribalConfirmDialogVisible)")
        val headerSection =
            source
                .substringAfter("private fun RouteSearchHeaderKakao(")
                .substringBefore("@Composable\nprivate fun RouteSearchHeader(")
        val mapStageSection =
            source
                .substringAfter("private fun RouteMapStage(")
                .substringBefore("@Composable\nprivate fun RouteMapMessageCard")

        assertTrue("Route setting top bar should use the blue route search header.", screenSection.contains("RouteSearchHeaderKakao("))
        assertFalse("The route search header should no longer expose disabled car or bike slots.", headerSection.contains("enabled = false"))
        assertTrue("The route search header should expose transit mode with text.", headerSection.contains("\"대중교통\""))
        assertTrue("The route search header should expose walk mode with text.", headerSection.contains("\"도보\""))
        assertTrue("The selected route mode should render with a white pill background.", source.contains("color = if (selected) Color.White else Color.Transparent"))
        assertTrue("The header should include a close action.", headerSection.contains("R.drawable.ic_action_close"))
        assertTrue("The header should include origin and destination waypoint rows.", headerSection.contains("RouteSearchHeaderWaypointLine("))
        assertTrue("The header should include a waypoint swap control.", headerSection.contains("onSwapClick"))
        assertFalse("The header should not expose a non-functional more menu affordance.", headerSection.contains("R.drawable.ic_action_more"))
        assertTrue("Walk mode should render map-anchored preview cards instead of the transit bottom sheet.", mapStageSection.contains("RouteWalkPreviewCarousel("))
        assertTrue("Walk preview cards should expose the route detail arrow CTA.", source.contains("경로 상세 보기"))
        assertFalse("Walk preview should remove kcal text beside distance.", source.contains("estimatedWalkCaloriesLabel("))
        assertTrue("Walk preview cards should expose the primary backend badge plus overflow count.", source.contains("val visibleBadge = card.badges.firstOrNull()") && source.contains("overflowBadgeCount"))
        assertTrue(
            "Walk preview cards should split the available row width equally like the reference mock.",
            source.contains("private fun RouteWalkPreviewCarousel") &&
                source.contains("optionCards.take(RouteWalkPreviewVisibleCardCount)") &&
                source.contains("modifier = Modifier.weight(1f)"),
        )
        assertTrue("Transit mode should keep the bottom sheet from the previous slice.", screenSection.contains("uiState.selectedTravelMode == RouteTravelMode.TRANSIT"))
    }

    @Test
    fun `route setting walk preview reserves space for full width bottom CTA`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val mapStageSection =
            source
                .substringAfter("private fun RouteMapStage(")
                .substringBefore("@Composable\nprivate fun RouteMapMessageCard")
        val ctaSection =
            source
                .substringAfter("private fun RouteSettingCtaContent(")
                .substringBefore("@Composable\nprivate fun RouteMapBackdrop")
        val carouselSection =
            source
                .substringAfter("private fun RouteWalkPreviewCarousel(")
                .substringBefore("@OptIn(ExperimentalLayoutApi::class)")
        val cardSection =
            source
                .substringAfter("private fun RouteWalkPreviewSummaryCard(")
                .substringBefore("@Composable\nprivate fun RouteMapControls")

        assertTrue(
            "Walk preview cards should sit above the fixed bottom CTA instead of being covered by it.",
            mapStageSection.contains("bottom = RouteWalkPreviewCarouselBottomPadding"),
        )
        assertTrue(
            "Walk preview cards should stay below the recenter control by using a compact fixed minimum card height.",
            cardSection.contains(".heightIn(min = RouteWalkPreviewCardMinHeight)") &&
                source.contains("RouteWalkPreviewCardMinHeight = 116.dp") &&
                source.contains("RouteWalkPreviewCarouselBottomPadding = RouteSettingBottomBarButtonHeight + RouteSettingBottomBarBottomGap + 4.dp"),
        )
        assertTrue(
            "Walk preview should show exactly two equal-width option cards with symmetric horizontal padding.",
            carouselSection.contains("optionCards.take(RouteWalkPreviewVisibleCardCount)") &&
                carouselSection.contains("Modifier.weight(1f)") &&
                !carouselSection.contains(".horizontalScroll(rememberScrollState())") &&
                !source.contains("RouteWalkPreviewCompactCardWidth"),
        )
        assertTrue(
            "Walk preview detail affordance should use the compact chevron, not the long arrow icon.",
            cardSection.contains("R.drawable.ic_route_card_chevron"),
        )
        assertTrue(
            "Route start CTA should match the reference full-width bottom button.",
            ctaSection.contains(".fillMaxWidth()") &&
                !ctaSection.contains(".width(RouteSettingBottomBarButtonWidth)"),
        )
        assertTrue(
            "Route start CTA should keep the requested 30dp bottom gap.",
            source.contains("RouteSettingBottomBarBottomGap = 30.dp"),
        )
        assertFalse(
            "Route start CTA should not add navigation bar inset on top of the explicit 30dp app-bottom gap.",
            ctaSection.contains(".navigationBarsPadding()"),
        )
    }

    @Test
    fun `walk preview tags use response badge colors from the reference mock`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val cardSection =
            source
                .substringAfter("private fun RouteWalkPreviewSummaryCard(")
                .substringBefore("@Composable\nprivate fun RouteMapControls")
        val badgeRowSection =
            source
                .substringAfter("private fun RouteWalkPreviewBadgeRow(")
                .substringBefore("@Composable\nprivate fun RouteMapControls")
        val badgeColorsSection =
            source
                .substringAfter("private fun routeOptionBadgeColors(")
                .substringBefore("@Composable\nprivate fun optionAccentColor")

        assertTrue(
            "Walk preview should apply backend badge colors instead of rendering response badges as neutral chips.",
            cardSection.contains("RouteWalkPreviewBadgeRow(") &&
                badgeRowSection.contains("routeOptionBadgeColors(visibleBadge)") &&
                badgeRowSection.contains("RouteBadgeOverflowColor"),
        )
        assertTrue(
            "The overflow +n badge should sit to the right of the primary badge in one non-wrapping row.",
            badgeRowSection.contains("Row(") &&
                badgeRowSection.contains("verticalAlignment = Alignment.CenterVertically") &&
                !cardSection.contains("FlowRow("),
        )
        assertTrue(
            "Positive/caution/overflow route tags should match the blue, red, and gray reference chips with white text.",
            source.contains("RouteBadgePositiveColor = Color(0xFF4B9EDC)") &&
                source.contains("RouteBadgeCautionColor = Color(0xFFD9534F)") &&
                source.contains("RouteBadgeOverflowColor = Color(0xFFA8A8A8)") &&
                badgeColorsSection.contains("RouteBadgeCautionColor to Color.White") &&
                badgeColorsSection.contains("RouteBadgePositiveColor to Color.White"),
        )
    }

    @Test
    fun `transit route sheet reserves room for the shared bottom CTA`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val routeSheetSection =
            source
                .substringAfter("private fun RouteSettingRouteSheet(")
                .substringBefore("@Composable\nprivate fun RouteOptionSection")

        assertTrue(
            "Transit bottom sheet content should reserve clearance so low-floor reservations are not hidden behind the shared CTA.",
            routeSheetSection.contains("bottom = RouteSettingBottomBarOverlayClearance"),
        )
        assertTrue(
            "Transit CTA clearance should be derived from the actual CTA height and requested bottom gap.",
            source.contains("RouteSettingBottomBarOverlayClearance = RouteSettingBottomBarButtonHeight + RouteSettingBottomBarBottomGap + EumSpacing.medium"),
        )
    }

    @Test
    fun `transit option timeline uses fixed radius transport icons and route option colors`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val segmentBarSection =
            source
                .substringAfter("private fun RouteTransitSegmentRatioBar(")
                .substringBefore("@Composable\nprivate fun RouteTransitOptionSummary")

        assertTrue(
            "Transit segment timeline should clip all four corners to the 5dp token.",
            segmentBarSection.contains(".clip(RoundedCornerShape(RouteTransitSegmentTimelineRadius))"),
        )
        assertTrue(
            "Transit segment timeline should render a start icon for bus and subway segments.",
            segmentBarSection.contains("RouteTransitSegmentStartIcon(segment = segment)"),
        )
        assertTrue(
            "Recommended route color should use the confirmed blue token.",
            source.contains("RouteSafeBlue = Color(0xFF006BE0)"),
        )
        assertTrue(
            "Minimum-walk/fast route color should use the confirmed orange token.",
            source.contains("RouteFastOrange = Color(0xFFF9AB4D)"),
        )
        assertTrue(
            "Subway line colors should include the confirmed Busan line tokens.",
            source.contains("RouteSubwayLine1 = Color(0xFFFF7F00)") &&
                source.contains("RouteSubwayBusanGimhae = Color(0xFF8200FF)"),
        )
        assertTrue(
            "Transit bus tags should use Phase 3 color, radius, and border tokens.",
            source.contains("RouteTransitTagLowFloorColor = Color(0xFF2671A8)") &&
                source.contains("RouteTransitTagNormalColor = Color(0xFF4B9EDC)") &&
                source.contains("RouteTransitTagBorderColor = Color(0xFFD9D9D9)") &&
                source.contains("RouteTransitTagArrivalColor = Color(0xFFF94D4D)") &&
                source.contains("RouteTransitTagCornerRadius = 10.dp"),
        )
        assertTrue(
            "Transit option cards should suppress stop origin-destination text and expose horizontal tag scrolling.",
            source.contains("horizontalScroll(rememberScrollState())") &&
                !source
                    .substringAfter("private fun RouteTransitOptionSummary(")
                    .substringBefore("@Composable\nprivate fun RouteTransitOptionChip")
                    .contains("stopLabel?.let"),
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
            "Route map should delegate floating control shape to the shared map floating controls component.",
            source.contains("EumMapFloatingControls("),
        )
        assertTrue(
            "Route map floating controls should keep the documented floating control elevation token available to the shared component.",
            source.contains("RouteFloatingControlElevation = 6.dp"),
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
    fun `route detail screen uses map backed side panel and attached guide rows`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val guideSidePanelSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/guidance/component/GuideSidePanel.kt")
                .readText()
        val detailScreenSection =
            source
                .substringAfter("fun RouteDetailScreen(")
                .substringBefore("@Composable\nprivate fun RouteDetailMapBottomSheet")
        val stepsSection =
            source
                .substringAfter("private fun RouteDetailStepsSection(")
                .substringBefore("@Composable\nprivate fun RouteDetailOriginHeader")

        assertTrue(
            "Detail screen should keep the map visible behind the detail side panel.",
            detailScreenSection.contains("RouteMapBackdrop("),
        )
        assertTrue(
            "Detail screen should render both walk and transit details through the left side panel.",
            detailScreenSection.contains("RouteDetailSidePanel("),
        )
        assertFalse(
            "Detail screen should not branch transit routes into a bottom sheet.",
            detailScreenSection.contains("RouteDetailMapBottomSheet("),
        )
        assertFalse(
            "Detail screen should remove the origin-destination edit header.",
            detailScreenSection.contains("RouteSearchHeaderKakao("),
        )
        assertTrue(
            "Detail side panel should delegate horizontal swipe collapse and expansion to the shared guide shell.",
            source.contains("GuideSidePanelShell(") &&
                guideSidePanelSource.contains("detectHorizontalDragGestures(") &&
                guideSidePanelSource.contains("GuideSidePanelSwipeThresholdPx"),
        )
        assertTrue(
            "Collapsed detail state should render an icon-only rail.",
            source.contains("private fun RouteDetailIconRail(") &&
                source.contains("RouteDetailCollapsedRailWidth"),
        )
        assertTrue(
            "Collapsed detail rail should drive the focused top card and map marker from the top visible icon.",
            source.contains("onTopVisibleStepChanged = { index ->") &&
                source.contains("snapshotFlow") &&
                source.contains("firstVisibleItemIndex"),
        )
        assertTrue(
            "Collapsed detail rail should expose an up action below the destination item that scrolls and focuses the origin.",
            source.contains("RouteDetailCollapsedRailScrollTopAction(") &&
                source.contains("listState.animateScrollToItem(0)") &&
                source.contains("onStepClick(0)"),
        )
        assertTrue(
            "Detail bottom sheet should expose a segment ratio/timeline bar.",
            source.contains("private fun RouteDetailTimelineBar("),
        )
        assertTrue(
            "Transit detail sheet should reserve arrival info and refresh affordances.",
            source.contains("private fun RouteDetailTransitActionRow("),
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
    fun `route detail side panel keeps arrival visible above the fixed start button`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val timelinePanelSection =
            source
                .substringAfter("private fun RouteDetailTimelinePanelContent(")
                .substringBefore("@Composable\nprivate fun RouteDetailIconRail")
        val collapsedCardSection =
            source
                .substringAfter("private fun RouteDetailCollapsedGuideCard(")
                .substringBefore("@Composable\nprivate fun RouteDetailCollapsedTransitGuideCardContent")

        assertTrue(
            "The open detail side panel must reserve bottom clearance so the arrival row is not hidden by the fixed CTA.",
            timelinePanelSection.contains("contentPadding = PaddingValues(bottom = RouteDetailSidePanelBottomClearance)") &&
                source.contains("RouteDetailSidePanelBottomClearance = RouteSettingBottomBarOverlayClearance") &&
                !source.contains("RouteDetailSidePanelBottomActionSpace"),
        )
        assertTrue(
            "The open detail side panel should draw a divider directly below the arrival row before the scroll-top action.",
            timelinePanelSection.contains("item(key = \"route-detail-arrival-divider\")") &&
                timelinePanelSection.contains("HorizontalDivider(color = RouteDetailGuideDividerColor)"),
        )
        assertTrue(
            "The bottom of the guide list should expose an in-panel scroll-to-top affordance like the reference.",
            timelinePanelSection.contains("RouteDetailScrollTopAction(") &&
                timelinePanelSection.contains("listState.animateScrollToItem(0)") &&
                !timelinePanelSection.contains("if (false)"),
        )
        assertTrue(
            "The open detail side panel should virtualize long guide lists to avoid blocking route-detail entry.",
            timelinePanelSection.contains("LazyColumn(") &&
                timelinePanelSection.contains("rememberLazyListState()") &&
                timelinePanelSection.contains("itemsIndexed("),
        )
        assertFalse(
            "The open detail side panel should not eagerly compose every guide row through a verticalScroll Column.",
            timelinePanelSection.contains(".verticalScroll(listState)") ||
                timelinePanelSection.contains(".verticalScroll(scrollState)"),
        )
        assertTrue(
            "Collapsed top guide card should animate vertical changes when rail scrolling changes the focused step.",
            collapsedCardSection.contains("AnimatedContent(") &&
                collapsedCardSection.contains("slideInVertically") &&
                collapsedCardSection.contains("slideOutVertically"),
        )
    }

    @Test
    fun `route detail defers guidance marker creation until a step is focused`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val detailScreenSection =
            source
                .substringAfter("fun RouteDetailScreen(")
                .substringBefore("@Composable\nprivate fun RouteDetailMapBottomSheet")

        assertTrue(
            "Route detail entry should not build every guidance marker before one is visible on the map.",
            detailScreenSection.contains("if (focusedDetailStepIndex == null)") &&
                detailScreenSection.contains("emptyList()") &&
                detailScreenSection.contains("selectedRoute.detailGuidanceMarkers(focusedDetailStepIndex)"),
        )
    }

    @Test
    fun `route detail guide rows use white Kakao style list rows and neutral icons`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val guideSidePanelSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/guidance/component/GuideSidePanel.kt")
                .readText()
        val originRowSection =
            source
                .substringAfter("private fun RouteDetailOriginStepRow(")
                .substringBefore("@Composable\nprivate fun RouteDetailFallbackRow")
        val stepRowSection =
            source
                .substringAfter("private fun RouteDetailStepRow(")
                .substringBefore("@Composable\nprivate fun RouteDetailArrivalInfoChip")

        assertTrue(
            "Origin and guide rows should render on white surfaces instead of warning-colored cards.",
            originRowSection.contains("GuideSidePanelStepRow(") &&
                stepRowSection.contains("GuideSidePanelStepRow(") &&
                guideSidePanelSource.contains("expandedContainerColor = MaterialTheme.colorScheme.surface"),
        )
        assertFalse(
            "Open detail rows should not show the raw route step sequence numbers from the backend.",
            originRowSection.contains("text = step.indexLabel") ||
                stepRowSection.contains("text = step.indexLabel"),
        )
        assertTrue(
            "Warning badges in the guide list should be neutral chips, not red/pink panels.",
            stepRowSection.contains("RouteDetailGuideBadgeContainerColor") &&
                stepRowSection.contains("RouteDetailGuideBadgeContentColor"),
        )
        assertTrue(
            "Open detail side-panel direction icons should use the neutral reference tint.",
            guideSidePanelSource.contains("tint = contentColor") &&
                !guideSidePanelSource.contains("Color.Unspecified") &&
                stepRowSection.contains("leadingContentColor = RouteDetailGuideIconColor"),
        )
    }

    @Test
    fun `route detail side panel removes trailing map affordances and footer clutter`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val timelinePanelSection =
            source
                .substringAfter("private fun RouteDetailTimelinePanelContent(")
                .substringBefore("@Composable\nprivate fun RouteDetailScrollTopAction")
        val originRowSection =
            source
                .substringAfter("private fun RouteDetailOriginStepRow(")
                .substringBefore("@Composable\nprivate fun RouteDetailFallbackRow")
        val stepRowSection =
            source
                .substringAfter("private fun RouteDetailStepRow(")
                .substringBefore("@Composable\nprivate fun RouteDetailArrivalInfoChip")

        assertTrue(
            "Origin row should use the same compact shared row contract as other guidance rows.",
            originRowSection.contains("title = origin.name") &&
                originRowSection.contains("description = origin.supportingText ?: step.description") &&
                originRowSection.contains("minHeight = RouteDetailGuideRowMinHeight") &&
                !originRowSection.contains("supportingContent ="),
        )
        assertFalse(
            "Guidance rows should not render the right-side map marker affordance in the open side panel.",
            stepRowSection.contains("RouteDetailRowAccessoryIcon("),
        )
        assertFalse(
            "Arrival row should not expose the detail-info chip inside the side panel.",
            stepRowSection.contains("RouteDetailArrivalInfoChip("),
        )
        assertFalse(
            "Side panel footer should not render information-correction suggestions below the scroll-top button.",
            timelinePanelSection.contains("RouteDetailPanelFeedbackFooter("),
        )
        assertTrue(
            "Scroll-top affordance should sit close to the final row instead of leaving an oversized gap.",
            source.contains("RouteDetailPanelBottomActionTopPadding = 24.dp") &&
                source.contains("RouteDetailScrollTopActionVerticalPadding = 0.dp"),
        )
    }

    @Test
    fun `route CTAs use narrower horizontal insets across search and detail`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val bottomBarSection =
            source
                .substringAfter("private fun RouteSettingBottomBar(")
                .substringBefore("@Composable\nprivate fun RouteSettingCtaContent")

        assertTrue(
            "Route start CTA should subtract 50dp from each side compared with the old screen-wide button.",
            source.contains("RouteSettingBottomBarHorizontalPadding = EumSpacing.medium + 50.dp") &&
                bottomBarSection.contains("start = RouteSettingBottomBarHorizontalPadding") &&
                bottomBarSection.contains("end = RouteSettingBottomBarHorizontalPadding"),
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
