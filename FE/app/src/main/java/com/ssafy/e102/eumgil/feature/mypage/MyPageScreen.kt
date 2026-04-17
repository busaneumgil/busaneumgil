package com.ssafy.e102.eumgil.feature.mypage

import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.common.model.PlaceholderAction
import com.ssafy.e102.eumgil.core.designsystem.component.layout.EumPlaceholderScaffold
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Composable
fun MyPageScreen(
    uiState: MyPageUiState,
    onAction: (MyPageUiAction) -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToSavedRoutes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EumPlaceholderScaffold(
        title = stringResource(id = R.string.my_page_screen_title),
        description = stringResource(id = R.string.my_page_screen_description),
        featurePath = stringResource(id = R.string.feature_path_my_page),
        actions = listOf(
            PlaceholderAction(
                label = stringResource(id = R.string.action_go_map),
                onClick = onNavigateToMap,
                isPrimary = true,
            ),
            PlaceholderAction(
                label = stringResource(id = R.string.action_go_saved_routes),
                onClick = onNavigateToSavedRoutes,
            ),
        ),
        modifier = modifier,
        content = {
            if (uiState.isDebugSectionVisible) {
                RepositoryDebugSection(
                    uiState = uiState,
                    onToggleChanged = { isEnabled ->
                        onAction(MyPageUiAction.ForceMockToggled(isEnabled = isEnabled))
                    },
                )
            }
        },
    )
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
