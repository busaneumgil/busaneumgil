package com.ssafy.e102.eumgil.feature.voiceassistant

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumBorderInfo
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.designsystem.theme.EumStatusDanger
import com.ssafy.e102.eumgil.core.designsystem.theme.EumStatusWarning
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSurfaceInfo

enum class VoiceAssistantOverlayVisualState {
    Idle,
    Listening,
    Processing,
    ResultReady,
    ConfirmationRequired,
    Error,
}

internal data class VoiceAssistantOverlayActionCopy(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val supportingText: String? = null,
)

private data class VoiceAssistantOverlayStateCopy(
    @StringRes val badgeRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
)

internal fun resolveVoiceAssistantOverlayVisualState(uiState: UiState): VoiceAssistantOverlayVisualState =
    when {
        uiState.status == VoiceAssistantStatus.Error || uiState.errorMessage.isNullOrBlank().not() ->
            VoiceAssistantOverlayVisualState.Error

        uiState.status == VoiceAssistantStatus.AwaitingConfirmation ->
            VoiceAssistantOverlayVisualState.ConfirmationRequired

        uiState.status == VoiceAssistantStatus.Listening ->
            VoiceAssistantOverlayVisualState.Listening

        uiState.status == VoiceAssistantStatus.Processing ->
            VoiceAssistantOverlayVisualState.Processing

        uiState.lastResolvedAction != null ->
            VoiceAssistantOverlayVisualState.ResultReady

        else -> VoiceAssistantOverlayVisualState.Idle
    }

internal fun resolveVoiceAssistantOverlayActionCopy(
    action: VoiceAssistantAction,
): VoiceAssistantOverlayActionCopy =
    when (action) {
        is VoiceAssistantAction.SearchPlace ->
            VoiceAssistantOverlayActionCopy(
                titleRes = R.string.voice_assistant_overlay_action_search_place_title,
                descriptionRes = R.string.voice_assistant_overlay_action_search_place_description,
                supportingText = action.query,
            )

        is VoiceAssistantAction.OpenReport ->
            VoiceAssistantOverlayActionCopy(
                titleRes = R.string.voice_assistant_overlay_action_open_report_title,
                descriptionRes = R.string.voice_assistant_overlay_action_open_report_description,
            )

        is VoiceAssistantAction.OpenSavedRoutes ->
            VoiceAssistantOverlayActionCopy(
                titleRes = R.string.voice_assistant_overlay_action_open_saved_routes_title,
                descriptionRes = R.string.voice_assistant_overlay_action_open_saved_routes_description,
            )

        is VoiceAssistantAction.OpenMyPage ->
            VoiceAssistantOverlayActionCopy(
                titleRes = R.string.voice_assistant_overlay_action_open_my_page_title,
                descriptionRes = R.string.voice_assistant_overlay_action_open_my_page_description,
            )

        is VoiceAssistantAction.OpenMap ->
            VoiceAssistantOverlayActionCopy(
                titleRes = R.string.voice_assistant_overlay_action_open_map_title,
                descriptionRes = R.string.voice_assistant_overlay_action_open_map_description,
            )

        is VoiceAssistantAction.StopNavigation ->
            VoiceAssistantOverlayActionCopy(
                titleRes = R.string.voice_assistant_overlay_action_stop_navigation_title,
                descriptionRes = R.string.voice_assistant_overlay_action_stop_navigation_description,
            )

        is VoiceAssistantAction.ResumeNavigationGuidance ->
            VoiceAssistantOverlayActionCopy(
                titleRes = R.string.voice_assistant_overlay_action_resume_navigation_title,
                descriptionRes = R.string.voice_assistant_overlay_action_resume_navigation_description,
            )

        is VoiceAssistantAction.UnknownCommand ->
            VoiceAssistantOverlayActionCopy(
                titleRes = R.string.voice_assistant_overlay_action_unknown_title,
                descriptionRes = R.string.voice_assistant_overlay_action_unknown_description,
                supportingText = action.rawCommand,
            )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantOverlay(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
    assistantMessage: String? = null,
    pendingAction: VoiceAssistantAction? = uiState.pendingConfirmationAction,
    bottomSheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    if (!visible) return

    val visualState = resolveVoiceAssistantOverlayVisualState(uiState)
    val displayedAction = pendingAction ?: uiState.pendingConfirmationAction ?: uiState.lastResolvedAction

    ModalBottomSheet(
        onDismissRequest = { onAction(UiAction.Dismissed) },
        sheetState = bottomSheetState,
        dragHandle = null,
        shape =
            RoundedCornerShape(
                topStart = EumRadius.scaleL,
                topEnd = EumRadius.scaleL,
                bottomEnd = 0.dp,
                bottomStart = 0.dp,
            ),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.38f),
        windowInsets = VoiceAssistantOverlayWindowInsets,
    ) {
        VoiceAssistantOverlayContent(
            uiState = uiState,
            visualState = visualState,
            displayedAction = displayedAction,
            assistantMessage = assistantMessage,
            onAction = onAction,
            modifier =
                modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = EumSpacing.large, vertical = EumSpacing.small),
        )
    }
}

@Composable
private fun VoiceAssistantOverlayContent(
    uiState: UiState,
    visualState: VoiceAssistantOverlayVisualState,
    displayedAction: VoiceAssistantAction?,
    assistantMessage: String?,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateCopy = voiceAssistantOverlayStateCopy(visualState)
    val resolvedAssistantMessage =
        assistantMessage?.takeIf { message -> message.isNotBlank() }
            ?: uiState.errorMessage?.takeIf { message -> message.isNotBlank() }
            ?: stringResource(id = stateCopy.descriptionRes)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
    ) {
        VoiceAssistantOverlayHeader(
            onDismiss = { onAction(UiAction.Dismissed) },
        )
        VoiceAssistantOverlayStatusHero(
            stateCopy = stateCopy,
            visualState = visualState,
        )
        VoiceAssistantOverlayInfoCard(
            label = stringResource(id = R.string.voice_assistant_overlay_assistant_message_label),
            body = resolvedAssistantMessage,
        )
        VoiceAssistantOverlayInfoCard(
            label = stringResource(id = R.string.voice_assistant_overlay_transcript_label),
            body =
                uiState.transcript
                    .takeIf { transcript -> transcript.isNotBlank() }
                    ?: stringResource(id = R.string.voice_assistant_overlay_transcript_empty),
        )
        displayedAction?.let { action ->
            VoiceAssistantOverlayActionCard(
                visualState = visualState,
                action = action,
            )
        } ?: VoiceAssistantOverlayInfoCard(
            label = stringResource(id = R.string.voice_assistant_overlay_pending_action_label),
            body = stringResource(id = R.string.voice_assistant_overlay_pending_action_empty),
        )
        VoiceAssistantOverlayActions(
            visualState = visualState,
            onAction = onAction,
        )
    }
}

@Composable
private fun VoiceAssistantOverlayHeader(
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.voice_assistant_overlay_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onDismiss) {
            Icon(
                painter = painterResource(id = R.drawable.ic_action_close),
                contentDescription = stringResource(id = R.string.voice_assistant_overlay_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VoiceAssistantOverlayStatusHero(
    stateCopy: VoiceAssistantOverlayStateCopy,
    visualState: VoiceAssistantOverlayVisualState,
) {
    val badge = stringResource(id = stateCopy.badgeRes)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    stateDescription = badge
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Surface(
            shape = CircleShape,
            color = voiceAssistantOverlayAccentContainerColor(visualState),
        ) {
            Box(
                modifier = Modifier.size(VoiceAssistantOverlayStatusIconContainerSize),
                contentAlignment = Alignment.Center,
            ) {
                if (visualState == VoiceAssistantOverlayVisualState.Processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(VoiceAssistantOverlayProgressSize),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        painter = painterResource(id = stateCopy.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(VoiceAssistantOverlayStatusIconSize),
                        tint = voiceAssistantOverlayAccentColor(visualState),
                    )
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(EumRadius.full),
            color = voiceAssistantOverlayAccentContainerColor(visualState),
            border = BorderStroke(1.dp, voiceAssistantOverlayAccentColor(visualState).copy(alpha = 0.18f)),
        ) {
            Text(
                text = badge,
                modifier = Modifier.padding(horizontal = EumSpacing.medium, vertical = EumSpacing.xSmall),
                style = MaterialTheme.typography.labelLarge,
                color = voiceAssistantOverlayAccentColor(visualState),
            )
        }
        Text(
            text = stringResource(id = stateCopy.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VoiceAssistantOverlayInfoCard(
    label: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.scaleM),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.64f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun VoiceAssistantOverlayActionCard(
    visualState: VoiceAssistantOverlayVisualState,
    action: VoiceAssistantAction,
) {
    val actionCopy = resolveVoiceAssistantOverlayActionCopy(action)
    val labelRes =
        if (visualState == VoiceAssistantOverlayVisualState.ConfirmationRequired) {
            R.string.voice_assistant_overlay_pending_confirmation_action_label
        } else {
            R.string.voice_assistant_overlay_pending_action_label
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.scaleM),
        color = EumSurfaceInfo,
        border = BorderStroke(1.dp, EumBorderInfo),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(
                    modifier = Modifier.size(VoiceAssistantOverlayActionIconContainerSize),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_voice_speaker_wave),
                        contentDescription = null,
                        modifier = Modifier.size(VoiceAssistantOverlayActionIconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = stringResource(id = labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(id = actionCopy.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(id = actionCopy.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                actionCopy.supportingText?.takeIf { text -> text.isNotBlank() }?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceAssistantOverlayActions(
    visualState: VoiceAssistantOverlayVisualState,
    onAction: (UiAction) -> Unit,
) {
    when (visualState) {
        VoiceAssistantOverlayVisualState.ConfirmationRequired -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                OutlinedButton(
                    onClick = { onAction(UiAction.ConfirmationRejected) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = VoiceAssistantOverlayButtonMinHeight),
                ) {
                    Text(text = stringResource(id = R.string.voice_assistant_overlay_action_cancel))
                }
                Button(
                    onClick = { onAction(UiAction.ConfirmationAccepted) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = VoiceAssistantOverlayButtonMinHeight),
                ) {
                    Text(text = stringResource(id = R.string.voice_assistant_overlay_action_confirm))
                }
            }
        }

        VoiceAssistantOverlayVisualState.Error -> {
            Button(
                onClick = { onAction(UiAction.AssistantClicked) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = VoiceAssistantOverlayButtonMinHeight),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(text = stringResource(id = R.string.voice_assistant_overlay_action_retry))
            }
        }

        VoiceAssistantOverlayVisualState.Processing -> {
            Button(
                onClick = {},
                enabled = false,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = VoiceAssistantOverlayButtonMinHeight),
            ) {
                Text(text = stringResource(id = R.string.voice_assistant_overlay_action_processing))
            }
        }

        VoiceAssistantOverlayVisualState.Listening -> {
            OutlinedButton(
                onClick = { onAction(UiAction.Dismissed) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = VoiceAssistantOverlayButtonMinHeight),
            ) {
                Text(text = stringResource(id = R.string.voice_assistant_overlay_action_listening_cancel))
            }
        }

        VoiceAssistantOverlayVisualState.Idle,
        VoiceAssistantOverlayVisualState.ResultReady,
        -> {
            Button(
                onClick = { onAction(UiAction.AssistantClicked) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = VoiceAssistantOverlayButtonMinHeight),
            ) {
                Text(
                    text =
                        stringResource(
                            id =
                                if (visualState == VoiceAssistantOverlayVisualState.ResultReady) {
                                    R.string.voice_assistant_overlay_action_retry
                                } else {
                                    R.string.voice_assistant_overlay_action_start
                                },
                        ),
                )
            }
        }
    }
}

private fun voiceAssistantOverlayStateCopy(
    visualState: VoiceAssistantOverlayVisualState,
): VoiceAssistantOverlayStateCopy =
    when (visualState) {
        VoiceAssistantOverlayVisualState.Idle ->
            VoiceAssistantOverlayStateCopy(
                badgeRes = R.string.voice_assistant_overlay_status_idle_badge,
                titleRes = R.string.voice_assistant_overlay_status_idle_title,
                descriptionRes = R.string.voice_assistant_overlay_status_idle_description,
                iconRes = R.drawable.ic_voice_mic,
            )

        VoiceAssistantOverlayVisualState.Listening ->
            VoiceAssistantOverlayStateCopy(
                badgeRes = R.string.voice_assistant_overlay_status_listening_badge,
                titleRes = R.string.voice_assistant_overlay_status_listening_title,
                descriptionRes = R.string.voice_assistant_overlay_status_listening_description,
                iconRes = R.drawable.ic_voice_mic,
            )

        VoiceAssistantOverlayVisualState.Processing ->
            VoiceAssistantOverlayStateCopy(
                badgeRes = R.string.voice_assistant_overlay_status_processing_badge,
                titleRes = R.string.voice_assistant_overlay_status_processing_title,
                descriptionRes = R.string.voice_assistant_overlay_status_processing_description,
                iconRes = R.drawable.ic_status_processing,
            )

        VoiceAssistantOverlayVisualState.ResultReady ->
            VoiceAssistantOverlayStateCopy(
                badgeRes = R.string.voice_assistant_overlay_status_result_ready_badge,
                titleRes = R.string.voice_assistant_overlay_status_result_ready_title,
                descriptionRes = R.string.voice_assistant_overlay_status_result_ready_description,
                iconRes = R.drawable.ic_status_check,
            )

        VoiceAssistantOverlayVisualState.ConfirmationRequired ->
            VoiceAssistantOverlayStateCopy(
                badgeRes = R.string.voice_assistant_overlay_status_confirmation_required_badge,
                titleRes = R.string.voice_assistant_overlay_status_confirmation_required_title,
                descriptionRes = R.string.voice_assistant_overlay_status_confirmation_required_description,
                iconRes = R.drawable.ic_status_warning,
            )

        VoiceAssistantOverlayVisualState.Error ->
            VoiceAssistantOverlayStateCopy(
                badgeRes = R.string.voice_assistant_overlay_status_error_badge,
                titleRes = R.string.voice_assistant_overlay_status_error_title,
                descriptionRes = R.string.voice_assistant_overlay_status_error_description,
                iconRes = R.drawable.ic_status_danger,
            )
    }

@Composable
private fun voiceAssistantOverlayAccentColor(
    visualState: VoiceAssistantOverlayVisualState,
): Color =
    when (visualState) {
        VoiceAssistantOverlayVisualState.ConfirmationRequired -> EumStatusWarning
        VoiceAssistantOverlayVisualState.Error -> EumStatusDanger
        else -> MaterialTheme.colorScheme.primary
    }

@Composable
private fun voiceAssistantOverlayAccentContainerColor(
    visualState: VoiceAssistantOverlayVisualState,
): Color =
    when (visualState) {
        VoiceAssistantOverlayVisualState.ConfirmationRequired -> EumStatusWarning.copy(alpha = 0.14f)
        VoiceAssistantOverlayVisualState.Error -> EumStatusDanger.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.54f)
    }

private val VoiceAssistantOverlayWindowInsets: WindowInsets = WindowInsets(0, 0, 0, 0)
private val VoiceAssistantOverlayButtonMinHeight = 48.dp
private val VoiceAssistantOverlayStatusIconContainerSize = 88.dp
private val VoiceAssistantOverlayStatusIconSize = 36.dp
private val VoiceAssistantOverlayProgressSize = 40.dp
private val VoiceAssistantOverlayActionIconContainerSize = 44.dp
private val VoiceAssistantOverlayActionIconSize = 24.dp
