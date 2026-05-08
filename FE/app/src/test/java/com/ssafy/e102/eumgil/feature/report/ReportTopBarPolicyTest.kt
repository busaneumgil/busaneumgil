package com.ssafy.e102.eumgil.feature.report

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTopBarPolicyTest {
    @Test
    fun `report root step hides back button`() {
        assertFalse(reportTopBarShowsBackButton(ReportStep.TypeSelection))
    }

    @Test
    fun `report intermediate steps keep back button`() {
        assertTrue(reportTopBarShowsBackButton(ReportStep.LocationConfirm))
        assertTrue(reportTopBarShowsBackButton(ReportStep.DetailInput))
    }

    @Test
    fun `report completion hides back button`() {
        assertFalse(reportTopBarShowsBackButton(ReportStep.Complete))
    }
}
