package com.ssafy.e102.eumgil.feature.mypage

import com.ssafy.e102.eumgil.core.designsystem.theme.EumPrimary600
import com.ssafy.e102.eumgil.core.designsystem.theme.EumWhite
import org.junit.Assert.assertEquals
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
}
