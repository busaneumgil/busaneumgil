package com.ssafy.e102.eumgil.feature.report

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportScreenPolicyTest {
    @Test
    fun `report complete cta suppresses ripple because it opens report history`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/report/ReportScreen.kt")
                .readText()

        assertFalse(shouldSuppressReportPrimaryActionRipple(ReportStep.LocationConfirm))
        assertFalse(shouldSuppressReportPrimaryActionRipple(ReportStep.DetailInput))
        assertTrue(shouldSuppressReportPrimaryActionRipple(ReportStep.Complete))
        assertTrue(
            "Report screen should route completion CTA through a no-ripple navigation button helper.",
            source.contains("NoRippleReportPrimaryActionButton("),
        )
    }
}
