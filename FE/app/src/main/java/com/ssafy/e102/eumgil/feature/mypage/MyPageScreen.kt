package com.ssafy.e102.eumgil.feature.mypage

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumCenteredTopBar
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Composable
fun MyPageScreen(
    uiState: MyPageUiState,
    onAction: (MyPageUiAction) -> Unit,
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

            Button(
                onClick = { onAction(MyPageUiAction.LogoutClicked) },
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
                    text = stringResource(id = R.string.my_page_logout),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (uiState.isDebugSectionVisible) {
                RepositoryDebugSection(
                    uiState = uiState,
                    onToggleChanged = { isEnabled ->
                        onAction(MyPageUiAction.ForceMockToggled(isEnabled = isEnabled))
                    },
                )
            }
        }
    }
}

@Composable
private fun MyPageTopBar() {
    EumCenteredTopBar(
        title = stringResource(id = R.string.my_page_screen_title),
        titleFontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ProfileCard(
    uiState: MyPageUiState,
    onUserTypeChangeClick: () -> Unit,
) {
    val avatarDescription = stringResource(id = R.string.my_page_profile_avatar_description)

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
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_mypage),
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
            ) {
                Text(
                    text = uiState.displayName ?: stringResource(id = R.string.my_page_default_user_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(id = uiState.userMode.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
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
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainMenuSection(onMenuClick: (MyPageMenuItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(EumSpacing.small)) {
        Text(
            text = stringResource(id = R.string.my_page_main_menu_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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
                iconRes = R.drawable.ic_report_other,
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
private fun MyPageMenuRow(
    menuItem: MyPageMenuItem,
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    onClick: (MyPageMenuItem) -> Unit,
) {
    val title = stringResource(id = titleRes)

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(
                    role = Role.Button,
                    onClickLabel = title,
                    onClick = { onClick(menuItem) },
                ),
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

@Composable
private fun RepositoryDebugSection(
    uiState: MyPageUiState,
    onToggleChanged: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Text(
                text = stringResource(id = R.string.repository_debug_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    stringResource(
                        id =
                            if (uiState.isForceMockEnabled) {
                                R.string.repository_debug_mode_mock
                            } else {
                                R.string.repository_debug_mode_live
                            },
                    ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text =
                    stringResource(
                        id =
                            if (uiState.isRuntimeToggleEnabled) {
                                R.string.repository_debug_toggle_description
                            } else {
                                R.string.repository_debug_toggle_locked_description
                            },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(EumRadius.medium),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(EumSpacing.medium)
                            .toggleable(
                                value = uiState.isForceMockEnabled,
                                enabled = uiState.isRuntimeToggleEnabled,
                                role = Role.Switch,
                                onValueChange = onToggleChanged,
                            ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.padding(end = EumSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                    ) {
                        Text(
                            text = stringResource(id = R.string.repository_debug_toggle_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(id = R.string.repository_debug_toggle_supporting),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = uiState.isForceMockEnabled,
                        onCheckedChange = null,
                        enabled = uiState.isRuntimeToggleEnabled,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

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
