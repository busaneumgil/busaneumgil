package com.ssafy.e102.eumgil.feature.mypage

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPageRouteDialIntentTest {
    @Test
    fun `duribal dial intent opens the dialer with the call center number prefilled`() {
        val intent = createDuribalDialIntent()

        assertEquals(Intent.ACTION_DIAL, intent.action)
        assertEquals("tel:15551114", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
