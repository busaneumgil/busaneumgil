package com.ssafy.e102.eumgil.feature.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideSidePanelPolicyTest {
    @Test
    fun `route detail and navigation side panels share guide primitives`() {
        val shared =
            File("src/main/java/com/ssafy/e102/eumgil/feature/guidance/component/GuideSidePanel.kt")
        val routeDetail =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val navigation =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt")
                .readText()
        val rail =
            File("src/main/java/com/ssafy/e102/eumgil/feature/navigation/component/NavigationSegmentRail.kt")
                .readText()

        assertTrue("Guide side panel primitives should live in a neutral feature package.", shared.exists())
        val sharedSource = shared.readText()

        assertTrue(
            "The shared module should own the expanded panel shell, step row, collapsed rail item, icon, and handle.",
            sharedSource.contains("fun GuideSidePanelShell(") &&
                sharedSource.contains("fun GuideSidePanelStepRow(") &&
                sharedSource.contains("fun GuideCollapsedRailItem(") &&
                sharedSource.contains("fun GuideSidePanelStepIcon(") &&
                sharedSource.contains("fun GuideSidePanelHandle(") &&
                sharedSource.contains("detectHorizontalDragGestures("),
        )
        assertTrue(
            "Shared guide icons should use the navigation icon set for origin, destination, and guidance steps.",
            sharedSource.contains("R.drawable.ic_navigation_rail_origin_pin") &&
                sharedSource.contains("R.drawable.ic_navigation_rail_destination_pin") &&
                sharedSource.contains("NavigationGuidanceAction"),
        )
        assertTrue(
            "Route detail should render its side panel through the shared shell, row, and collapsed rail primitives.",
            routeDetail.contains("GuideSidePanelShell(") &&
                routeDetail.contains("GuideSidePanelStepRow(") &&
                routeDetail.contains("GuideCollapsedRailItem("),
        )
        assertTrue(
            "Navigation guidance should render its side panel through the shared shell and row primitives.",
            navigation.contains("GuideSidePanelShell(") &&
                navigation.contains("GuideSidePanelStepRow("),
        )
        assertTrue(
            "Navigation collapsed rail should use the shared collapsed rail primitive while keeping its return/detail actions local.",
            rail.contains("GuideCollapsedRailItem(") &&
                rail.contains("NavigationSegmentRailReturnAction(") &&
                rail.contains("NavigationSegmentRailDetailAction("),
        )
        assertFalse(
            "Route detail should no longer own a private side-panel icon implementation.",
            routeDetail.contains("private fun RouteDetailSidePanelStepIcon("),
        )
        assertFalse(
            "Navigation should no longer own a private expanded side-panel icon implementation.",
            navigation.contains("private fun NavigationSidePanelStepIcon("),
        )
        assertFalse(
            "Route detail should no longer own a private side-panel handle or swipe threshold.",
            routeDetail.contains("private fun RouteDetailSidePanelToggleHandle(") ||
                routeDetail.contains("RouteDetailSidePanelSwipeThresholdPx"),
        )
        assertFalse(
            "Navigation should no longer own a private side-panel handle or swipe threshold.",
            navigation.contains("private fun NavigationSidePanelExpandHandle(") ||
                navigation.contains("NavigationSidePanelSwipeThresholdPx"),
        )
    }

    @Test
    fun `marker focus remains screen owned while side panel UI is shared`() {
        val shared =
            File("src/main/java/com/ssafy/e102/eumgil/feature/guidance/component/GuideSidePanel.kt")
        val routeDetail =
            File("src/main/java/com/ssafy/e102/eumgil/feature/route/RouteSettingScreen.kt")
                .readText()
        val detailScreenSection =
            routeDetail
                .substringAfter("fun RouteDetailScreen(")
                .substringBefore("@Composable\nprivate fun RouteDetailMapBottomSheet")

        assertTrue(
            "Route detail should keep marker focus state near the map so teammate marker-card work has a stable hook.",
            detailScreenSection.contains("focusedDetailStepIndex") &&
                detailScreenSection.contains("RouteMapBackdrop(") &&
                detailScreenSection.contains("onMarkerClick"),
        )
        assertTrue("Guide side panel primitives should exist before checking their scope.", shared.exists())
        val sharedSource = shared.readText()
        assertFalse("The shared side panel should not own map marker click logic.", sharedSource.contains("onMarkerClick"))
        assertFalse("The shared side panel should not own RouteMapBackdrop.", sharedSource.contains("RouteMapBackdrop"))
        assertFalse("The shared side panel should not own focusedDetailStepIndex.", sharedSource.contains("focusedDetailStepIndex"))
    }

    @Test
    fun `shared waypoint pins use route detail marker color tokens`() {
        val sharedSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/guidance/component/GuideSidePanel.kt")
                .readText()
        val originPin =
            File("src/main/res/drawable-nodpi/ic_navigation_rail_origin_pin.png")
        val destinationPin =
            File("src/main/res/drawable-nodpi/ic_navigation_rail_destination_pin.png")

        assertTrue("Origin pin asset should exist.", originPin.exists())
        assertTrue("Destination pin asset should exist.", destinationPin.exists())
        assertTrue(
            "Shared side panel should document the requested origin and destination pin colors.",
            sharedSource.contains("GuideWaypointOriginColor = Color(0xFF4D8FF9)") &&
                sharedSource.contains("GuideWaypointDestinationColor = Color(0xFFF94D4D)"),
        )
    }
}
