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
        assertTrue(shouldSuppressMyPageMenuRipple(MyPageMenuItem.TEXT_SIZE))
        assertTrue(shouldSuppressMyPageMenuRipple(MyPageMenuItem.APP_HELP))
        assertTrue(shouldSuppressMyPageMenuRipple(MyPageMenuItem.PRIVACY_POLICY))
        assertTrue(shouldSuppressMyPageMenuRipple(MyPageMenuItem.SERVICE_TERMS))
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
    fun `my page report history menu is owned by report tab`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt")
                .readText()

        assertFalse(
            "My page should not expose report history as a menu row because report tab owns that flow.",
            source.contains("MyPageMenuItem.REPORT_HISTORY"),
        )
    }

    @Test
    fun `my page body exposes target profile quick actions menu and footer`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt")
                .readText()

        assertTrue(
            "My page should expose the target profile overview card.",
            source.contains("ProfileOverviewCard(") &&
                source.contains("ProfileStatsRow("),
        )
        assertTrue(
            "My page should expose the target quick action cards for Duribal and guide.",
            source.contains("QuickActionGrid(") &&
                source.contains("R.string.my_page_duribal_title") &&
                source.contains("R.string.my_page_guide_title"),
        )
        assertTrue(
            "The regular policy/report menu rows and footer actions should be visible above the bottom tab.",
            source.contains("MyPageMenuItem.TEXT_SIZE") &&
                source.contains("R.string.my_page_menu_text_size") &&
                source.contains("MyPageMenuItem.PRIVACY_POLICY") &&
                source.contains("MyPageMenuItem.SERVICE_TERMS") &&
                source.contains("MyPageFooter("),
        )
    }

    @Test
    fun `my page content scrolls above footer for small screens and large text`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt")
                .readText()

        assertTrue(
            "My page should keep the account footer outside a scrollable weighted content area so expanded menu text cannot overlap it.",
            source.contains(".weight(1f)") &&
                source.contains(".verticalScroll(rememberScrollState())") &&
                source.indexOf("MainMenuCard(") < source.indexOf("MyPageFooter("),
        )
    }

    @Test
    fun `my page display name falls back to generic user label`() {
        assertEquals("사용자", resolveDisplayName(MyPageUiState(displayName = null)))
        assertEquals("사용자", resolveDisplayName(MyPageUiState(displayName = " ")))
        assertEquals("민들레", resolveDisplayName(MyPageUiState(displayName = " 민들레 ")))
    }

    @Test
    fun `my page route owns duribal call confirmation flow`() {
        val myPageSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt")
                .readText()
        val routeSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageRoute.kt")
                .readText()

        assertTrue(
            "MyPageScreen should render the duribal confirmation dialog for the restored CTA.",
            myPageSource.contains("isDuribalConfirmDialogVisible") ||
                myPageSource.contains("EumDuribalCallConfirmDialog("),
        )
        assertTrue(
            "MyPageRoute should create the duribal dial intent after confirmation.",
            routeSource.contains("createDuribalDialIntent") ||
                routeSource.contains("onDuribalCallClick"),
        )
    }

    @Test
    fun `my page screen renders duribal call confirmation dialog with yes and no actions`() {
        val myPageSource =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt")
                .readText()
        val dialogSource =
            File("src/main/java/com/ssafy/e102/eumgil/core/designsystem/component/dialog/EumDuribalCallConfirmDialog.kt")
                .readText()

        assertTrue(
            "MyPageScreen should render the duribal confirmation dialog instead of dialing immediately from the button tap.",
            myPageSource.contains("isDuribalConfirmDialogVisible") &&
                myPageSource.contains("EumDuribalCallConfirmDialog("),
        )
        assertTrue(
            "Duribal confirmation dialog should expose explicit yes and no actions for the restored CTA flow.",
            dialogSource.contains("onConfirm") &&
                dialogSource.contains("onDismiss") &&
                dialogSource.contains("my_page_duribal_call_dialog_confirm") &&
                dialogSource.contains("my_page_duribal_call_dialog_dismiss"),
        )
    }

    @Test
    fun `duribal confirm dialog follows app custom dialog shell`() {
        val dialogSource =
            File("src/main/java/com/ssafy/e102/eumgil/core/designsystem/component/dialog/EumDuribalCallConfirmDialog.kt")
                .readText()

        assertFalse(
            "Duribal dialog should not use the platform AlertDialog shell because it looks inconsistent with app dialogs.",
            dialogSource.contains("AlertDialog("),
        )
        assertTrue(
            "Duribal dialog should use the same custom dialog pattern as the app confirmation dialogs.",
            dialogSource.contains("Dialog(") &&
                dialogSource.contains("Surface(") &&
                dialogSource.contains(".fillMaxWidth()") &&
                dialogSource.contains("ButtonDefaults.buttonElevation("),
        )
    }
}
