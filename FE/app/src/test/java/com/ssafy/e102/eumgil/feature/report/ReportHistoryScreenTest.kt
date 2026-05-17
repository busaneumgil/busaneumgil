package com.ssafy.e102.eumgil.feature.report

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportHistoryScreenTest {
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
        assertTrue(shouldShowReportHistoryCreateCta(ReportHistoryScreenState.CONTENT))
    }

    @Test
    fun `non content states do not use the content create report cta`() {
        assertFalse(shouldShowReportHistoryCreateCta(ReportHistoryScreenState.LOADING))
        assertFalse(shouldShowReportHistoryCreateCta(ReportHistoryScreenState.EMPTY))
        assertFalse(shouldShowReportHistoryCreateCta(ReportHistoryScreenState.ERROR))
    }

    @Test
    fun `report history create report ctas suppress ripple when they open the report screen`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/report/ReportHistoryScreen.kt")
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
            stateCardSection.contains("NoRippleReportHistoryNavigationButton("),
        )
        assertTrue(
            "Report-history no-ripple CTA helper should disable ripple indication explicitly.",
            source.contains("indication = null"),
        )
    }

    @Test
    fun `report history placeholder icon uses report specific drawable without changing tab icon`() {
        val reportHistorySource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/report/ReportHistoryScreen.kt")
                .readText()
        val topLevelDestinationSource =
            File("src/main/java/com/ssafy/e102/eumgil/app/navigation/TopLevelDestination.kt")
                .readText()

        assertTrue(
            "Report history placeholder should use the dedicated My page drawable.",
            reportHistorySource.contains("R.drawable.ic_mypage_report_history"),
        )
        assertTrue(
            "The top-level report tab should keep the existing report navigation icon.",
            topLevelDestinationSource.contains("iconRes = R.drawable.ic_nav_report"),
        )
    }
}
