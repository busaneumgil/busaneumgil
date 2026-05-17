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
    fun `my page main menu exposes duribal call button`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt")
                .readText()
        val mainMenuSection =
            source
                .substringAfter("private fun MainMenuSection(")
                .substringBefore("@Composable\nprivate fun MyPageMenuRow")

        assertTrue(
            "My page should expose the duribal CTA again.",
            source.contains("DuribalCallButton(") ||
                source.contains("ic_mypage_duribal_call") ||
                source.contains("my_page_duribal_call_button") ||
                mainMenuSection.contains("onDuribalCallClick"),
        )
        assertTrue(
            "The regular menu rows should remain in the main menu after removing the duribal CTA.",
            mainMenuSection.contains("MyPageMenuRow("),
        )
        assertTrue(
            "The restored duribal CTA should sit below the main menu and directly above logout.",
            source.indexOf("MainMenuSection(") <
                source.indexOf("DuribalCallButton(onClick = onDuribalCallClick)") &&
                source.indexOf("DuribalCallButton(onClick = onDuribalCallClick)") <
                source.indexOf("MyPageUiAction.LogoutClicked"),
        )
    }

    @Test
    fun `duribal call button centers icon and label as a single group`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt")
                .readText()
        val buttonSection =
            source
                .substringAfter("private fun DuribalCallButton(")
                .substringBefore("@Composable\nprivate fun MyPageMenuRow")

        assertTrue(
            "Duribal call CTA should wrap its icon and label in a single row so the combined content stays centered inside the full-width button.",
            buttonSection.contains(
                "Row(\n            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),\n            verticalAlignment = Alignment.CenterVertically,\n        )",
            ),
        )
        assertFalse(
            "Duribal call CTA should not rely on text-only start padding because the icon and label are centered together as one group.",
            buttonSection.contains("modifier = Modifier.padding(start = EumSpacing.small)"),
        )
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
