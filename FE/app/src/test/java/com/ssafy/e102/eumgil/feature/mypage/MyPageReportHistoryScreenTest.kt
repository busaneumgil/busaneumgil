package com.ssafy.e102.eumgil.feature.mypage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPageReportHistoryScreenTest {
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
}
