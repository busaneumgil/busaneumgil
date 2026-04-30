package com.ssafy.e102.eumgil.feature.lowvision

import org.junit.Assert.assertEquals
import org.junit.Test

class LowVisionNavigationScreenTest {
    @Test
    fun `navigation screen keeps distance and eta as top summary metrics`() {
        assertEquals(
            listOf("남은 거리", "남은 시간"),
            lowVisionNavigationMetricSections().map(LowVisionNavigationMetricSection::label),
        )
    }

    @Test
    fun `navigation screen exposes current location and exit action cards`() {
        assertEquals(
            listOf("현재 위치", "안내 종료"),
            lowVisionNavigationActionCards().map(LowVisionNavigationActionCard::label),
        )
    }

    @Test
    fun `navigation screen keeps low vision bottom tab order`() {
        assertEquals(
            listOf(
                LowVisionBottomTab.HOME,
                LowVisionBottomTab.BOOKMARK,
                LowVisionBottomTab.CATEGORY,
                LowVisionBottomTab.MY_PAGE,
            ),
            lowVisionNavigationBottomTabs(),
        )
    }

    @Test
    fun `navigation metric talkback text reads label and value as one phrase`() {
        assertEquals(
            "남은 거리 50m",
            LowVisionNavigationMetricSection(label = "남은 거리", metricIndex = 0).talkBackText("50m"),
        )
        assertEquals(
            "남은 시간 2분",
            LowVisionNavigationMetricSection(label = "남은 시간", metricIndex = 1).talkBackText("2분"),
        )
    }
}
