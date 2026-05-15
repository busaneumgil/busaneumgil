package com.ssafy.e102.eumgil.feature.mypage

import com.ssafy.e102.eumgil.core.designsystem.theme.EumPrimary600
import com.ssafy.e102.eumgil.core.designsystem.theme.EumWhite
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPageAppInfoScreenTest {
    @Test
    fun `hero card uses primary background with white text and white logo surface`() {
        val style = myPageAppInfoHeroCardStyle()

        assertEquals(EumPrimary600, style.containerColor)
        assertEquals(EumPrimary600, style.borderColor)
        assertEquals(EumWhite, style.logoSurfaceColor)
        assertEquals(EumWhite, style.titleColor)
        assertEquals(EumWhite, style.versionColor)
        assertEquals(EumWhite, style.descriptionColor)
    }

    @Test
    fun `app info rows suppress ripple only for entries that leave the my page flow`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageAppInfoScreen.kt")
                .readText()
        val rowSection =
            source
                .substringAfter("private fun MyPageAppInfoRow(")
                .substringBefore("private fun appInfoArrowRotationDegrees")

        assertTrue(
            "App-info rows that open guide or external links should opt into transition ripple suppression explicitly.",
            source.contains("suppressRipple = true"),
        )
        assertTrue(
            "App-info navigation rows should disable ripple indication when they leave the current screen.",
            rowSection.contains("indication = null"),
        )
        assertTrue(
            "App-info navigation rows should keep a dedicated interaction source when ripple is suppressed.",
            rowSection.contains("MutableInteractionSource()"),
        )
    }
}
