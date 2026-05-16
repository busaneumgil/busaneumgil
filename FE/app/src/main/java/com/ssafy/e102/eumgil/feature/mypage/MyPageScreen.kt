package com.ssafy.e102.eumgil.feature.mypage

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.component.dialog.EumDuribalCallConfirmDialog
import com.ssafy.e102.eumgil.core.designsystem.component.dialog.EumDuribalCallConfirmDismissStyle
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumCenteredTopBar
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Composable
fun MyPageScreen(
    uiState: MyPageUiState,
    onAction: (MyPageUiAction) -> Unit,
    isDuribalConfirmDialogVisible: Boolean,
    onDuribalCallClick: () -> Unit,
    onDuribalConfirmDismiss: () -> Unit,
    onDuribalConfirm: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MyPageTopBar()
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = EumSpacing.medium,
                        end = EumSpacing.medium,
                        top = EumSpacing.medium,
                        bottom = EumSpacing.large,
                    ),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            ProfileCard(
                uiState = uiState,
                onUserTypeChangeClick = {
                    onAction(MyPageUiAction.UserTypeChangeClicked)
                },
            )

            MainMenuSection(
                onMenuClick = { menuItem ->
                    onAction(MyPageUiAction.MainMenuClicked(menuItem = menuItem))
                },
            )

            DuribalCallButton(onClick = onDuribalCallClick)

            Button(
                onClick = { onAction(MyPageUiAction.LogoutClicked) },
                enabled = !uiState.isLogoutLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                shape = RoundedCornerShape(EumRadius.small),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text(
                    text =
                        stringResource(
                            id =
                                if (uiState.isLogoutLoading) {
                                    R.string.my_page_logout_loading
                                } else {
                                    R.string.my_page_logout
                                },
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    if (isDuribalConfirmDialogVisible) {
        EumDuribalCallConfirmDialog(
            onDismiss = onDuribalConfirmDismiss,
            onConfirm = onDuribalConfirm,
            dismissStyle = EumDuribalCallConfirmDismissStyle.SecondaryButton,
        )
    }
}

@Composable
private fun MyPageTopBar() {
    EumCenteredTopBar(
        title = stringResource(id = R.string.my_page_screen_title),
        titleFontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ProfileCard(
    uiState: MyPageUiState,
    onUserTypeChangeClick: () -> Unit,
) {
    val avatarDescription = stringResource(id = R.string.my_page_profile_avatar_description)
    val avatarRes = resolveProfileAvatarRes(uiState)
    val headlineTextRes = resolveHeadlineTextRes(uiState)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.medium),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.large, vertical = EumSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f))
                        .semantics { contentDescription = avatarDescription },
                contentAlignment = Alignment.Center,
            ) {
                if (avatarRes == R.drawable.ic_nav_mypage) {
                    Icon(
                        painter = painterResource(id = avatarRes),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Image(
                        painter = painterResource(id = avatarRes),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
            ) {
                Text(
                    text = stringResource(id = headlineTextRes),
                    style = MaterialTheme.typography.titleMedium,
                )
                uiState.mobilitySubtype?.let { subtype ->
                    Text(
                        text = stringResource(id = subtype.labelRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f),
                    )
                }
                Button(
                    onClick = onUserTypeChangeClick,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                    shape = RoundedCornerShape(EumRadius.small),
                    border =
                        BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f),
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Text(
                        text = stringResource(id = R.string.my_page_change_user_type),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainMenuSection(
    onMenuClick: (MyPageMenuItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(EumSpacing.small)) {
        Text(
            text = stringResource(id = R.string.my_page_main_menu_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(EumRadius.medium),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)),
        ) {
            MyPageMenuRow(
                menuItem = MyPageMenuItem.NOTICE,
                titleRes = R.string.my_page_menu_notice,
                iconRes = R.drawable.ic_mypage_notice_bell_vector,
                onClick = onMenuClick,
            )
            MyPageMenuRow(
                menuItem = MyPageMenuItem.REPORT_HISTORY,
                titleRes = R.string.my_page_menu_report_history,
                iconRes = R.drawable.ic_mypage_report_history,
                onClick = onMenuClick,
            )
            MyPageMenuRow(
                menuItem = MyPageMenuItem.APP_HELP,
                titleRes = R.string.my_page_menu_app_help,
                iconRes = R.drawable.ic_status_help_circle,
                onClick = onMenuClick,
            )
        }
    }
}

@Composable
private fun DuribalCallButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        shape = RoundedCornerShape(EumRadius.medium),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_mypage_duribal_call),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            text = stringResource(id = R.string.my_page_duribal_call_button),
            modifier = Modifier.padding(start = EumSpacing.small),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun MyPageMenuRow(
    menuItem: MyPageMenuItem,
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    onClick: (MyPageMenuItem) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val title = stringResource(id = titleRes)
    val suppressRipple = shouldSuppressMyPageMenuRipple(menuItem)
    val clickableModifier =
        if (suppressRipple) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = title,
                onClick = { onClick(menuItem) },
            )
        } else {
            Modifier.clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = { onClick(menuItem) },
            )
        }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(clickableModifier),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_action_dropdown),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(20.dp)
                        .rotate(-90f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun shouldSuppressMyPageMenuRipple(menuItem: MyPageMenuItem): Boolean =
    menuItem == MyPageMenuItem.REPORT_HISTORY || menuItem == MyPageMenuItem.APP_HELP

private val MyPageUserMode.labelRes: Int
    get() =
        when (this) {
            MyPageUserMode.LOW_VISION -> R.string.my_page_mode_low_vision
            MyPageUserMode.MOBILITY_IMPAIRED -> R.string.my_page_mode_mobility
            MyPageUserMode.UNKNOWN -> R.string.my_page_mode_unknown
        }

private val MyPageMobilitySubtype.labelRes: Int
    get() =
        when (this) {
            MyPageMobilitySubtype.ELECTRIC_WHEELCHAIR -> R.string.my_page_mobility_subtype_electric
            MyPageMobilitySubtype.MANUAL_WHEELCHAIR -> R.string.my_page_mobility_subtype_manual
            MyPageMobilitySubtype.OTHER -> R.string.my_page_mobility_subtype_other
        }

@StringRes
internal fun resolveHeadlineTextRes(uiState: MyPageUiState): Int = uiState.userMode.labelRes

@DrawableRes
internal fun resolveProfileAvatarRes(uiState: MyPageUiState): Int =
    if (uiState.userMode != MyPageUserMode.MOBILITY_IMPAIRED) {
        R.drawable.ic_nav_mypage
    } else {
        when (uiState.mobilitySubtype) {
            MyPageMobilitySubtype.MANUAL_WHEELCHAIR -> R.drawable.manual_galmaegi
            MyPageMobilitySubtype.ELECTRIC_WHEELCHAIR -> R.drawable.auto_galmaegi
            MyPageMobilitySubtype.OTHER -> R.drawable.crutch_galmaegi
            null -> R.drawable.ic_nav_mypage
        }
    }
