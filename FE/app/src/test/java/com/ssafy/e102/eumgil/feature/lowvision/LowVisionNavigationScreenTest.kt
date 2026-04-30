package com.ssafy.e102.eumgil.feature.lowvision

import org.junit.Assert.assertEquals
import org.junit.Test

class LowVisionNavigationScreenTest {
    @Test
    fun `navigation information is split into separate low vision pages`() {
        assertEquals(
            listOf("안내 정보", "남은 거리", "예상 시간", "진행 단계"),
            lowVisionNavigationPages.map(LowVisionNavigationPage::title),
        )
    }

    @Test
    fun `navigation page index stays within page range`() {
        assertEquals(1, nextLowVisionNavigationPageIndex(0))
        assertEquals(lowVisionNavigationPages.lastIndex, nextLowVisionNavigationPageIndex(lowVisionNavigationPages.lastIndex))
        assertEquals(0, previousLowVisionNavigationPageIndex(0))
        assertEquals(0, previousLowVisionNavigationPageIndex(1))
    }
}
