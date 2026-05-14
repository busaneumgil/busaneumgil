package com.ssafy.e102.eumgil.feature.map.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import kotlin.math.roundToInt

@Immutable
data class FacilityDetailBottomSheetShellState(
    val isVisible: Boolean = false,
    @DrawableRes val placeIconRes: Int = 0,
    val metaLabel: String = "",
    val title: String = "",
    val address: String = "",
    val hasDetailContent: Boolean = false,
)

@Composable
fun FacilityDetailBottomSheetShell(
    state: FacilityDetailBottomSheetShellState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    detailContent: @Composable ColumnScope.() -> Unit,
    headerActionContent: (@Composable () -> Unit)? = null,
    actionContent: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val dragSettleVelocityThresholdPx = with(density) { 320.dp.toPx() }
    val collapseThresholdMinPx = with(density) { 72.dp.toPx() }
    val handleInteractionSource = remember { MutableInteractionSource() }
    var sheetHeightPx by remember(state.isVisible) { mutableIntStateOf(0) }
    var sheetOffsetPx by remember(state.isVisible) { mutableFloatStateOf(0f) }
    var isDragging by remember(state.isVisible) { mutableStateOf(false) }
    var isCollapsed by remember(state.isVisible) { mutableStateOf(false) }
    val sheetToggleDescription = stringResource(id = R.string.map_facility_detail_sheet_toggle)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val detailScrollState = rememberScrollState()
        val sheetMaxHeight = maxHeight * 0.9f
        val maxSheetOffsetPx = sheetHeightPx.toFloat().coerceAtLeast(0f)
        val collapseThresholdPx = (sheetHeightPx * 0.25f).coerceAtLeast(collapseThresholdMinPx)
        val animatedSheetOffsetPx by animateFloatAsState(
            targetValue = sheetOffsetPx.coerceIn(0f, maxSheetOffsetPx),
            animationSpec =
                if (isDragging) {
                    snap()
                } else {
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                },
            label = "facilityDetailSheetOffset",
        )
        val dragState =
            rememberDraggableState { delta ->
                isDragging = true
                if (delta < 0f) {
                    isCollapsed = false
                    sheetOffsetPx = 0f
                } else {
                    sheetOffsetPx = (sheetOffsetPx + delta).coerceIn(0f, maxSheetOffsetPx)
                }
            }

        LaunchedEffect(state.isVisible, maxSheetOffsetPx) {
            if (!state.isVisible) {
                isDragging = false
                sheetOffsetPx = 0f
                isCollapsed = false
            } else {
                sheetOffsetPx = sheetOffsetPx.coerceIn(0f, maxSheetOffsetPx)
            }
        }

        AnimatedVisibility(
            visible = state.isVisible,
            enter =
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                ),
            exit =
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                ),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
        ) {
            MapBottomSheetSurface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = FacilityDetailCollapsedMinHeight, max = sheetMaxHeight)
                        .onSizeChanged { size ->
                            sheetHeightPx = size.height
                            sheetOffsetPx = sheetOffsetPx.coerceIn(0f, maxSheetOffsetPx)
                        }
                        .offset { IntOffset(x = 0, y = animatedSheetOffsetPx.roundToInt()) },
                handleModifier =
                    Modifier
                        .height(MapBottomSheetHandleHeight)
                        .semantics {
                            role = Role.Button
                            contentDescription = sheetToggleDescription
                        }
                        .clickable(
                            interactionSource = handleInteractionSource,
                            indication = null,
                            onClick = {
                                isDragging = false
                                isCollapsed = !isCollapsed
                                sheetOffsetPx = 0f
                            },
                        )
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity ->
                                isDragging = false
                                isCollapsed =
                                    velocity >= dragSettleVelocityThresholdPx ||
                                    sheetOffsetPx >= collapseThresholdPx
                                sheetOffsetPx = 0f
                            },
                        ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Image(
                            painter = painterResource(id = state.placeIconRes),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
                        ) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (isCollapsed) 1 else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!isCollapsed && state.metaLabel.isNotBlank()) {
                                Text(
                                    text = state.metaLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (!isCollapsed) {
                                Text(
                                    text = state.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            headerActionContent?.let { content ->
                                content()
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_action_close),
                                    contentDescription = stringResource(id = R.string.map_facility_detail_close),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (state.hasDetailContent && !isCollapsed) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(detailScrollState),
                            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                            content = detailContent,
                        )
                    }

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                        content = actionContent,
                    )
                }
            }
        }
    }
}

private val FacilityDetailCollapsedMinHeight = 188.dp
