package com.ssafy.e102.eumgil.feature.route

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
}
