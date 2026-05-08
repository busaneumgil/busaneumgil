package com.ssafy.e102.eumgil.feature.terms

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TermsGuideScreenTest {
    @Test
    fun `terms guide screen does not reserve or render a bottom tab bar`() {
        assertFalse(TermsGuideLayoutDefaults.showBottomNav)
    }

    @Test
    fun `age step uses text icon instead of the broken age drawable`() {
        assertEquals("14+", TermsGuideStep.AGE.iconText)
        assertNull(TermsGuideStep.AGREE.iconText)
    }

    @Test
    fun `detail buttons open configured notion pages for each agreement step`() {
        assertEquals(
            "https://www.notion.so/ryuwon-project/350a58d49be680ab9931f226486dac58?source=copy_link",
            TermsGuideStep.AGREE.detailUrl,
        )
        assertEquals(
            "https://www.notion.so/ryuwon-project/350a58d49be6804a925ef3e41000c3cd?source=copy_link",
            TermsGuideStep.SENSITIVE.detailUrl,
        )
        assertEquals(
            "https://www.notion.so/ryuwon-project/350a58d49be68063bbd1f633be85badb?source=copy_link",
            TermsGuideStep.LOCATION.detailUrl,
        )
        assertNull(TermsGuideStep.AGE.detailUrl)
        assertEquals(
            "https://www.notion.so/ryuwon-project/350a58d49be68063bbd1f633be85badb?source=copy_link",
            TermsGuideStep.PRIVACY.detailUrl,
        )
    }

    @Test
    fun `bottom guidance and detail button use low vision readable sizing`() {
        assertEquals(2f, TermsGuideLayoutDefaults.mainActionCardWeight)
        assertEquals(1f, TermsGuideLayoutDefaults.detailActionCardWeight)
        assertEquals(35.dp, TermsGuideLayoutDefaults.actionCardCornerRadius)
        assertEquals(TermsGuideLayoutDefaults.actionCardCornerRadius, TermsGuideLayoutDefaults.moreButtonCornerRadius)
        assertEquals(48.sp, TermsGuideLayoutDefaults.cardLabelFontSize)
        assertEquals(88.sp, TermsGuideLayoutDefaults.textIconFontSize)
        assertEquals(28.sp, TermsGuideLayoutDefaults.hintFontSize)
        assertEquals(36.sp, TermsGuideLayoutDefaults.hintLineHeight)
        assertEquals(34.sp, TermsGuideLayoutDefaults.moreButtonFontSize)
        assertEquals(42.sp, TermsGuideLayoutDefaults.moreButtonLineHeight)
        assertEquals(74.dp, TermsGuideLayoutDefaults.moreButtonMinHeight)
        assertEquals(28.dp, TermsGuideLayoutDefaults.bottomContentHorizontalPadding)
        assertEquals(28.dp, TermsGuideLayoutDefaults.bottomContentBottomPadding)
    }
}
