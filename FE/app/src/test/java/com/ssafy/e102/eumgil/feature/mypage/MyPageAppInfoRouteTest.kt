package com.ssafy.e102.eumgil.feature.mypage

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPageAppInfoRouteTest {
    @Test
    fun `privacy policy intent opens the configured notion page`() {
        val intent = createPrivacyPolicyIntent()

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(
            "https://www.notion.so/ryuwon-project/350a58d49be68063bbd1f633be85badb",
            intent.dataString,
        )
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `service terms intent opens the configured notion page`() {
        val intent = createServiceTermsIntent()

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(
            "https://www.notion.so/ryuwon-project/350a58d49be680ab9931f226486dac58",
            intent.dataString,
        )
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
