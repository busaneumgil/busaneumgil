package com.ssafy.e102.eumgil.feature.mypage

import com.ssafy.e102.eumgil.R
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPageScreenTest {
    @Test
    fun `profile card headline uses user mode label instead of default name`() {
        val headlineRes =
            resolveHeadlineTextRes(
                MyPageUiState(
                    userMode = MyPageUserMode.MOBILITY_IMPAIRED,
                    mobilitySubtype = MyPageMobilitySubtype.MANUAL_WHEELCHAIR,
                ),
            )

        assertEquals(R.string.my_page_mode_mobility, headlineRes)
    }

    @Test
    fun `profile card avatar uses mobility subtype specific galmaegi image`() {
        assertEquals(
            R.drawable.manual_galmaegi,
            resolveProfileAvatarRes(
                MyPageUiState(
                    userMode = MyPageUserMode.MOBILITY_IMPAIRED,
                    mobilitySubtype = MyPageMobilitySubtype.MANUAL_WHEELCHAIR,
                ),
            ),
        )
        assertEquals(
            R.drawable.auto_galmaegi,
            resolveProfileAvatarRes(
                MyPageUiState(
                    userMode = MyPageUserMode.MOBILITY_IMPAIRED,
                    mobilitySubtype = MyPageMobilitySubtype.ELECTRIC_WHEELCHAIR,
                ),
            ),
        )
        assertEquals(
            R.drawable.crutch_galmaegi,
            resolveProfileAvatarRes(
                MyPageUiState(
                    userMode = MyPageUserMode.MOBILITY_IMPAIRED,
                    mobilitySubtype = MyPageMobilitySubtype.OTHER,
                ),
            ),
        )
    }

    @Test
    fun `profile card avatar falls back to my page icon when mobility subtype is unavailable`() {
        assertEquals(
            R.drawable.ic_nav_mypage,
            resolveProfileAvatarRes(
                MyPageUiState(
                    userMode = MyPageUserMode.LOW_VISION,
                    mobilitySubtype = null,
                ),
            ),
        )
    }

    @Test
    fun `main menu rows suppress ripple only for entries that open another screen`() {
        assertTrue(shouldSuppressMyPageMenuRipple(MyPageMenuItem.REPORT_HISTORY))
        assertTrue(shouldSuppressMyPageMenuRipple(MyPageMenuItem.APP_HELP))
        assertFalse(shouldSuppressMyPageMenuRipple(MyPageMenuItem.NOTICE))
    }

    @Test
    fun `my page menu row keeps no ripple modifier available for navigation rows`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt")
                .readText()

        assertTrue(
            "My page menu rows should branch ripple suppression by menu item so only navigation rows lose the transition flash.",
            source.contains("shouldSuppressMyPageMenuRipple(menuItem)"),
        )
        assertTrue(
            "My page navigation rows should be able to suppress ripple with an explicit null indication.",
            source.contains("indication = null"),
        )
    }

    @Test
    fun `my page report history menu uses dedicated icon resource`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt")
                .readText()

        assertTrue(
            "My page report history should use its own dedicated drawable resource instead of reusing a generic report asset.",
            source.contains("iconRes = R.drawable.ic_mypage_report_history"),
        )
    }

    @Test
    fun `my page main menu exposes duribal call button before the regular menu rows`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt")
                .readText()

        assertTrue(
            "My page should surface the duribal call CTA at the top of the main menu section.",
            source.contains("DuribalCallButton(onClick = onDuribalCallClick)"),
        )
        assertTrue(
            "The duribal CTA should use the provided dedicated icon resource.",
            source.contains("R.drawable.ic_mypage_duribal_call"),
        )
    }

    @Test
    fun `my page screen renders duribal call confirmation dialog with yes and no actions`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt")
                .readText()

        assertTrue(
            "MyPageScreen should render the duribal confirmation dialog when the route marks it visible.",
            source.contains("if (isDuribalConfirmDialogVisible)"),
        )
        assertTrue(
            "The duribal confirmation dialog should use the dedicated alert dialog composable.",
            source.contains("private fun DuribalCallConfirmDialog"),
        )
        assertTrue(
            "The dialog should expose a positive confirmation action.",
            source.contains("my_page_duribal_call_dialog_confirm"),
        )
        assertTrue(
            "The dialog should expose a negative dismiss action.",
            source.contains("my_page_duribal_call_dialog_dismiss"),
        )
    }
}
