package com.ssafy.e102.eumgil.feature.arrival

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.feature.map.component.MapBottomSheetSurface

private val ArrivalSuccessColor = Color(0xFF16A34A)
private const val ArrivalRatingCount = 5

@Composable
fun ArrivalScreen(
    uiState: ArrivalUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (ArrivalUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        ArrivalCompletionContent(
            onHomeClicked = { onAction(ArrivalUiAction.HomeClicked) },
            onExploreNewRouteClicked = { onAction(ArrivalUiAction.ExploreNewRouteClicked) },
        )

        AnimatedVisibility(
            visible = uiState.isEvaluationSheetVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.22f)),
            )
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = EumSpacing.medium)
                    .padding(bottom = EumSpacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedVisibility(
                visible = uiState.isEvaluationSheetVisible,
                enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut(),
            ) {
                ArrivalEvaluationBottomSheet(
                    uiState = uiState,
                    onAction = onAction,
                    modifier = Modifier.widthIn(max = 520.dp),
                )
            }
        }

        if (uiState.isRouteSaveDialogVisible) {
            uiState.routeSaveDraft?.let { draft ->
                ArrivalRouteSaveDialog(
                    draft = draft,
                    routeName = uiState.routeNameInput,
                    isSaving = uiState.isRouteSaveUpdating,
                    isConfirmEnabled = uiState.isRouteSaveConfirmEnabled,
                    onRouteNameChanged = { value -> onAction(ArrivalUiAction.RouteNameChanged(value)) },
                    onDismiss = { onAction(ArrivalUiAction.RouteSaveDialogDismissed) },
                    onConfirm = { onAction(ArrivalUiAction.ConfirmRouteSaveClicked) },
                )
            }
        }
    }
}

@Composable
private fun ArrivalCompletionContent(
    onHomeClicked: () -> Unit,
    onExploreNewRouteClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = EumSpacing.large, vertical = EumSpacing.medium),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(212.dp)
                        .clip(RoundedCornerShape(EumRadius.large))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.splash_illustration),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 24.dp),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                shape = RoundedCornerShape(EumRadius.full),
                color = ArrivalSuccessColor.copy(alpha = 0.12f),
            ) {
                Row(
                    modifier =
                        Modifier.padding(
                            horizontal = EumSpacing.small,
                            vertical = EumSpacing.xSmall,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_status_check),
                        contentDescription = null,
                        tint = ArrivalSuccessColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(EumSpacing.xxSmall))
                    Text(
                        text = stringResource(id = R.string.arrival_screen_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = ArrivalSuccessColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(EumSpacing.large))

            Text(
                text = stringResource(id = R.string.arrival_screen_headline),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(EumSpacing.small))
            Text(
                text = stringResource(id = R.string.arrival_screen_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = EumSpacing.xxSmall),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Button(
                onClick = onHomeClicked,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                shape = RoundedCornerShape(EumRadius.medium),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_home_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(EumSpacing.xxSmall))
                Text(
                    text = stringResource(id = R.string.arrival_action_go_home),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            OutlinedButton(
                onClick = onExploreNewRouteClicked,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                shape = RoundedCornerShape(EumRadius.medium),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(EumSpacing.xxSmall))
                Text(
                    text = stringResource(id = R.string.arrival_action_explore_new_route),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ArrivalEvaluationBottomSheet(
    uiState: ArrivalUiState,
    onAction: (ArrivalUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    MapBottomSheetSurface(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.arrival_evaluation_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = { onAction(ArrivalUiAction.EvaluationSheetDismissed) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_close),
                        contentDescription = stringResource(id = R.string.arrival_evaluation_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.arrival_evaluation_question),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(ArrivalRatingCount) { index ->
                    val rating = index + 1
                    val isSelected = rating <= uiState.selectedRating
                    IconButton(
                        onClick = { onAction(ArrivalUiAction.RatingSelected(rating)) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    id =
                                        if (isSelected) {
                                            R.drawable.ic_rating_star_filled
                                        } else {
                                            R.drawable.ic_action_favorite
                                        },
                                ),
                            contentDescription =
                                stringResource(
                                    id = R.string.arrival_evaluation_star_content_description,
                                    rating,
                                ),
                            tint =
                                if (isSelected) {
                                    ArrivalSuccessColor
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }

            uiState.selectedRatingLabel.labelResId?.let { labelResId ->
                Text(
                    text = stringResource(id = labelResId),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ArrivalSuccessColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } ?: Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                OutlinedButton(
                    onClick = { onAction(ArrivalUiAction.SaveRouteClicked) },
                    enabled = uiState.isRouteSaveEnabled,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    shape = RoundedCornerShape(EumRadius.medium),
                    border =
                        BorderStroke(
                            1.dp,
                            if (uiState.isRouteSaveSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                ) {
                    Icon(
                        painter =
                            painterResource(
                                id =
                                    if (uiState.isRouteSaveSelected) {
                                        R.drawable.ic_nav_bookmark_selected
                                    } else {
                                        R.drawable.ic_nav_bookmark_outline
                                    },
                            ),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(EumSpacing.xxSmall))
                    Text(
                        text =
                            stringResource(
                                id =
                                    if (uiState.isRouteSaveSelected) {
                                        R.string.arrival_evaluation_route_saved
                                    } else {
                                        R.string.arrival_evaluation_save_route
                                    },
                            ),
                        style = MaterialTheme.typography.labelLarge,
                        color =
                            if (uiState.isRouteSaveSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )
                }

                Button(
                    onClick = { onAction(ArrivalUiAction.SubmitEvaluationClicked) },
                    enabled = uiState.isEvaluationSubmitEnabled,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    shape = RoundedCornerShape(EumRadius.medium),
                ) {
                    Text(
                        text = stringResource(id = R.string.arrival_evaluation_submit),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArrivalRouteSaveDialog(
    draft: ArrivalRouteSaveDraftUiState,
    routeName: String,
    isSaving: Boolean,
    isConfirmEnabled: Boolean,
    onRouteNameChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.arrival_route_save_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                Text(
                    text =
                        stringResource(
                            id = R.string.arrival_route_save_dialog_summary,
                            draft.startLabel,
                            draft.endLabel,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = buildRouteSaveMetaLabel(draft = draft),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedTextField(
                    value = routeName,
                    onValueChange = onRouteNameChanged,
                    enabled = !isSaving,
                    singleLine = true,
                    label = {
                        Text(text = stringResource(id = R.string.arrival_route_save_dialog_name_label))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isConfirmEnabled,
            ) {
                Text(text = stringResource(id = R.string.arrival_route_save_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
            ) {
                Text(text = stringResource(id = R.string.arrival_route_save_dialog_cancel))
            }
        },
    )
}

@Composable
private fun buildRouteSaveMetaLabel(draft: ArrivalRouteSaveDraftUiState): String {
    val metaParts =
        buildList {
            add(draft.routeOptionLabel)
            draft.distanceMeters?.let { distanceMeters ->
                add(
                    stringResource(
                        id = R.string.saved_route_meta_distance,
                        distanceMeters.toSavedRouteDistanceLabel(),
                    ),
                )
            }
            draft.durationMinutes?.let { durationMinutes ->
                add(
                    stringResource(
                        id = R.string.saved_route_meta_duration,
                        durationMinutes.toSavedRouteDurationLabel(),
                    ),
                )
            }
        }
    return metaParts.joinToString(separator = " · ")
}

private fun Int.toSavedRouteDistanceLabel(): String =
    if (this < 1_000) {
        "${this}m"
    } else {
        String.format("%.1fkm", this / 1_000f)
    }

private fun Int.toSavedRouteDurationLabel(): String = "${this}분"
