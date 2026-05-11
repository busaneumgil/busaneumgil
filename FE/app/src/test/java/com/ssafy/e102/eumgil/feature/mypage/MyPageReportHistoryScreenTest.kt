package com.ssafy.e102.eumgil.feature.mypage

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPageReportHistoryScreenTest {
    @Test
    fun `report history layout spec follows list card and button convention`() {
        val spec = reportHistoryLayoutSpec()

        assertEquals(12, spec.cardCornerRadiusDp)
        assertEquals(12, spec.thumbnailCornerRadiusDp)
        assertEquals(12, spec.buttonCornerRadiusDp)
        assertEquals(48, spec.buttonMinHeightDp)
        assertEquals(0, spec.cardShadowElevationDp)
    }

    @Test
    fun `content state keeps create report cta visible`() {
        assertTrue(shouldShowReportHistoryCreateCta(MyPageReportHistoryScreenState.CONTENT))
    }

    @Test
    fun `non content states do not use the content create report cta`() {
        assertFalse(shouldShowReportHistoryCreateCta(MyPageReportHistoryScreenState.LOADING))
        assertFalse(shouldShowReportHistoryCreateCta(MyPageReportHistoryScreenState.EMPTY))
        assertFalse(shouldShowReportHistoryCreateCta(MyPageReportHistoryScreenState.ERROR))
    }

    @Test
    fun `report history create report ctas suppress ripple when they open the report screen`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageReportHistoryScreen.kt")
                .readText()
        val stateCardSection =
            source
                .substringAfter("private fun ReportHistoryStateCard(")
                .substringBefore("@Composable\nprivate fun reportHistoryButtonColors")

        assertTrue(
            "Report-history state cards should be able to mark report CTAs as no-ripple transition actions.",
            source.contains("primaryActionSuppressRipple = true"),
        )
        assertTrue(
            "Report-history state cards should use a dedicated no-ripple navigation button for report CTAs.",
            stateCardSection.contains("NoRippleMyPageReportHistoryNavigationButton("),
        )
        assertTrue(
            "Report-history no-ripple CTA helper should disable ripple indication explicitly.",
            source.contains("indication = null"),
        )
    }
}
