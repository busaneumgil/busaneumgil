package com.ssafy.e102.eumgil.feature.arrival

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumPrimary600
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.feature.map.component.MapBottomSheetHandleHeight
import com.ssafy.e102.eumgil.feature.map.component.MapBottomSheetSurface
import kotlin.math.roundToInt

private val ArrivalSuccessColor = Color(0xFF16A34A)
private val ArrivalRatingSelectedColor = Color(0xFFFACC15)
private val ArrivalEvaluationSectionSpacing = EumSpacing.small
private val ArrivalEvaluationRatingFeedbackPlaceholderHeight = 8.dp
private val ArrivalHeroBandHeight = 332.dp
private val ArrivalHeroBandTopSpacing = 36.dp
private val ArrivalHeroArtworkBottomSpacing = 28.dp
private val ArrivalHeroBackgroundFadeHeight = 40.dp
private val ArrivalHeroLogoTopPadding = 58.dp
private val ArrivalHeroLogoWidth = 108.dp
private val ArrivalHeroLogoHeight = 60.dp
private val ArrivalRouteSaveIconSize = 24.dp
private const val ArrivalHeroArtworkAspectRatio = 1440f / 900f
private const val ArrivalRatingCount = 5

@Composable
fun ArrivalScreen(
    uiState: ArrivalUiState,
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
            modifier = Modifier.fillMaxSize(),
        )

        if (!uiState.isEvaluationSheetVisible) {
            ArrivalCompletionActions(
                onHomeClicked = { onAction(ArrivalUiAction.HomeClicked) },
                onExploreNewRouteClicked = { onAction(ArrivalUiAction.ExploreNewRouteClicked) },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(1f),
            )
        }

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

        ArrivalEvaluationBottomSheet(
            uiState = uiState,
            onAction = onAction,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ArrivalCompletionContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(bottom = EumSpacing.medium),
    ) {
        Spacer(modifier = Modifier.height(ArrivalHeroBandTopSpacing))

        ArrivalHeroBand()

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ArrivalCompletionActions(
    onHomeClicked: () -> Unit,
    onExploreNewRouteClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = EumSpacing.large)
                .padding(bottom = EumSpacing.medium),
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

@Composable
private fun ArrivalHeroBand(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ArrivalHeroBandHeight)
                .background(MaterialTheme.colorScheme.background),
    ) {
        Image(
            painter = painterResource(id = R.drawable.arrival_completion_background),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(ArrivalHeroArtworkAspectRatio)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ArrivalHeroArtworkBottomSpacing),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(ArrivalHeroBackgroundFadeHeight)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                                        MaterialTheme.colorScheme.background,
                                    ),
                            ),
                    ),
        )

        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = ArrivalHeroLogoTopPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = null,
                modifier =
                    Modifier
                        .width(ArrivalHeroLogoWidth)
                        .height(ArrivalHeroLogoHeight),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(EumSpacing.xxSmall))
            Text(
                text = stringResource(id = R.string.auth_login_service_name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ArrivalEvaluationBottomSheet(
    uiState: ArrivalUiState,
    onAction: (ArrivalUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val dragSettleVelocityThresholdPx = with(density) { 320.dp.toPx() }
    val dismissThresholdMinPx = with(density) { 72.dp.toPx() }
    val handleInteractionSource = remember { MutableInteractionSource() }
    val closeSheetLabel = stringResource(id = R.string.arrival_evaluation_close)
    var sheetHeightPx by remember(uiState.isEvaluationSheetVisible) { mutableIntStateOf(0) }
    var sheetOffsetPx by remember(uiState.isEvaluationSheetVisible) { mutableFloatStateOf(0f) }
    var isDragging by remember(uiState.isEvaluationSheetVisible) { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val sheetMaxHeight = maxHeight * 0.82f
        val maxSheetOffsetPx = sheetHeightPx.toFloat().coerceAtLeast(0f)
        val dismissThresholdPx = (sheetHeightPx * 0.35f).coerceAtLeast(dismissThresholdMinPx)
        val dragState =
            rememberDraggableState { delta ->
                isDragging = true
                sheetOffsetPx = (sheetOffsetPx + delta).coerceIn(0f, maxSheetOffsetPx)
            }

        LaunchedEffect(uiState.isEvaluationSheetVisible, maxSheetOffsetPx) {
            if (!uiState.isEvaluationSheetVisible) {
                isDragging = false
                sheetOffsetPx = 0f
            } else {
                sheetOffsetPx = sheetOffsetPx.coerceIn(0f, maxSheetOffsetPx)
            }
        }

        AnimatedVisibility(
            visible = uiState.isEvaluationSheetVisible,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
        ) {
            MapBottomSheetSurface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = sheetMaxHeight)
                        .onSizeChanged { size ->
                            sheetHeightPx = size.height
                            sheetOffsetPx = sheetOffsetPx.coerceIn(0f, maxSheetOffsetPx)
                        }
                        .offset { IntOffset(x = 0, y = sheetOffsetPx.roundToInt()) },
                handleModifier =
                    Modifier
                        .height(MapBottomSheetHandleHeight)
                        .semantics {
                            role = Role.Button
                            contentDescription = closeSheetLabel
                        }
                        .clickable(
                            interactionSource = handleInteractionSource,
                            indication = null,
                            onClick = {
                                isDragging = false
                                sheetOffsetPx = 0f
                                onAction(ArrivalUiAction.EvaluationSheetDismissed)
                            },
                        )
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity ->
                                isDragging = false
                                if (
                                    velocity >= dragSettleVelocityThresholdPx ||
                                    sheetOffsetPx >= dismissThresholdPx
                                ) {
                                    sheetOffsetPx = 0f
                                    onAction(ArrivalUiAction.EvaluationSheetDismissed)
                                } else {
                                    sheetOffsetPx = 0f
                                }
                            },
                        ),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ArrivalEvaluationSectionSpacing),
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
                                contentDescription = closeSheetLabel,
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
                                            ArrivalRatingSelectedColor
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
                    } ?: Spacer(modifier = Modifier.height(ArrivalEvaluationRatingFeedbackPlaceholderHeight))

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                    ) {
                        val routeSaveAccentColor =
                            if (uiState.isRouteSaveEnabled) {
                                EumPrimary600
                            } else {
                                EumPrimary600.copy(alpha = 0.38f)
                            }
                        OutlinedButton(
                            onClick = { onAction(ArrivalUiAction.SaveRouteClicked) },
                            enabled = uiState.isRouteSaveEnabled,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            shape = RoundedCornerShape(EumRadius.medium),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = routeSaveAccentColor,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContentColor = routeSaveAccentColor,
                                ),
                            border = BorderStroke(1.dp, routeSaveAccentColor),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterEnd,
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
                                        tint = routeSaveAccentColor,
                                        modifier = Modifier.size(ArrivalRouteSaveIconSize),
                                    )
                                }
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text =
                                            stringResource(
                                                id =
                                                    if (uiState.isRouteSaveSelected) {
                                                        R.string.arrival_evaluation_route_save_cancel
                                                    } else {
                                                        R.string.arrival_evaluation_save_route
                                                    },
                                            ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = routeSaveAccentColor,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
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
    }
}
