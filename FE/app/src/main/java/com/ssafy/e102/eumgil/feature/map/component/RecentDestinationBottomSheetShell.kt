package com.ssafy.e102.eumgil.feature.map.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import kotlin.math.roundToInt

@Immutable
data class RecentDestinationBottomSheetState(
    val isVisible: Boolean = false,
    val title: String = "최근 목적지",
    val items: List<RecentDestinationRowState> = emptyList(),
)

@Immutable
data class RecentDestinationRowState(
    val placeId: String,
    val title: String,
    val address: String,
    val tags: List<String> = emptyList(),
    val overflowTagCount: Int = 0,
    @DrawableRes val iconRes: Int,
)

@Composable
fun RecentDestinationBottomSheetShell(
    state: RecentDestinationBottomSheetState,
    onViewAllClick: () -> Unit,
    onRouteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val dragSettleVelocityThresholdPx = with(density) { 320.dp.toPx() }
    val dismissThresholdMinPx = with(density) { 72.dp.toPx() }
    val handleInteractionSource = remember { MutableInteractionSource() }
    var isDismissedByUser by remember(state.items) { mutableStateOf(false) }
    var sheetHeightPx by remember(state.items, state.isVisible) { mutableIntStateOf(0) }
    var sheetOffsetPx by remember(state.items, state.isVisible) { mutableFloatStateOf(0f) }
    var isDragging by remember(state.items, state.isVisible) { mutableStateOf(false) }
    val isSheetVisible = state.isVisible && !isDismissedByUser

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val sheetMaxHeight = maxHeight * 0.58f
        val maxSheetOffsetPx = sheetHeightPx.toFloat().coerceAtLeast(0f)
        val dismissThresholdPx = (sheetHeightPx * 0.35f).coerceAtLeast(dismissThresholdMinPx)
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
            label = "recentDestinationSheetOffset",
        )
        val dragState =
            rememberDraggableState { delta ->
                isDragging = true
                sheetOffsetPx = (sheetOffsetPx + delta).coerceIn(0f, maxSheetOffsetPx)
            }

        LaunchedEffect(isSheetVisible, maxSheetOffsetPx) {
            if (!isSheetVisible) {
                isDragging = false
                sheetOffsetPx = 0f
            } else {
                sheetOffsetPx = sheetOffsetPx.coerceIn(0f, maxSheetOffsetPx)
            }
        }

        AnimatedVisibility(
            visible = isSheetVisible,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 3 }) + fadeIn(),
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
                        .offset { IntOffset(x = 0, y = animatedSheetOffsetPx.roundToInt()) },
                handleModifier =
                    Modifier
                        .height(24.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "최근 목적지 시트 닫기"
                        }
                        .clickable(
                            interactionSource = handleInteractionSource,
                            indication = null,
                            onClick = {
                                isDragging = false
                                isDismissedByUser = true
                                sheetOffsetPx = 0f
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
                                    isDismissedByUser = true
                                }
                                sheetOffsetPx = 0f
                            },
                        ),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(onClick = onViewAllClick) {
                        Text(
                            text = "전체보기",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    state.items.forEachIndexed { index, item ->
                        RecentDestinationRow(
                            state = item,
                            onRouteClick = { onRouteClick(item.placeId) },
                        )
                        if (index != state.items.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentDestinationRow(
    state: RecentDestinationRowState,
    onRouteClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f),
        ) {
            Icon(
                painter = painterResource(id = state.iconRes),
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(10.dp)
                        .size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.tags.isNotEmpty() || state.overflowTagCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.tags.forEach { label ->
                        RecentDestinationTagChip(
                            label = label,
                            isOverflow = false,
                        )
                    }
                    if (state.overflowTagCount > 0) {
                        RecentDestinationTagChip(
                            label = "+${state.overflowTagCount}",
                            isOverflow = true,
                        )
                    }
                }
            }
        }

        Button(
            onClick = onRouteClick,
            shape = RoundedCornerShape(12.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_route_start_navigation_button),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "길찾기",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun RecentDestinationTagChip(
    label: String,
    isOverflow: Boolean,
) {
    val containerColor =
        if (isOverflow) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
        }
    val contentColor =
        if (isOverflow) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (isOverflow) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    },
            ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
