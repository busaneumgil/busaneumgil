package com.ssafy.e102.eumgil.feature.route

import androidx.compose.ui.unit.dp
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
        assertEquals(RouteSettingMapHeightPolicy.FlexibleConstraintClamp, policy.mapHeightPolicy)
        assertTrue(policy.mapMinHeight >= 180.dp)
        assertTrue(policy.mapMaxHeight <= 280.dp)
        assertEquals(2, policy.maxVisibleOptionCards)
        assertFalse(policy.showsOptionSectionSupportingText)
        assertEquals("출발", policy.originLabel)
        assertEquals("도착", policy.destinationLabel)
        assertTrue(policy.showsWaypointSwapButton)
        assertEquals(RouteSettingOptionContainer.BottomSheet, policy.optionContainer)
        assertEquals(RouteSettingTravelModeTabShape.SegmentedPill, policy.travelModeTabShape)
        assertEquals(2, policy.visibleAccessibilityChipCount)
    }
}
