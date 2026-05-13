package com.ssafy.e102.eumgil.feature.mypage

import com.ssafy.e102.eumgil.core.external.duribalDialUriString
import org.junit.Assert.assertEquals
import org.junit.Test

class MyPageRouteDialIntentTest {
    @Test
    fun `duribal dial uri keeps the call center number prefilled`() {
        assertEquals("tel:15551114", duribalDialUriString())
    }
}
