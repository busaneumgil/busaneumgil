package com.ssafy.e102.eumgil.feature.navigation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.navigation.NavigationGuidanceAction
import com.ssafy.e102.eumgil.feature.navigation.NavigationSegmentRailItemUiState
import com.ssafy.e102.eumgil.feature.navigation.NavigationSegmentSyncUiState
import com.ssafy.e102.eumgil.feature.navigation.iconRes

@Composable
fun NavigationSegmentRail(
    uiState: NavigationSegmentSyncUiState,
    onSegmentTapped: (Int) -> Unit,
    onReturnToActiveSegmentClick: () -> Unit,
    onRouteDetailClick: () -> Unit,
    isRouteDetailEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val railColor = MaterialTheme.colorScheme.surface
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
    val railSlots = createNavigationSegmentRailSlots(uiState)
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.focusedSegmentIndex, uiState.railItems.size) {
        if (uiState.railItems.isEmpty()) return@LaunchedEffect
        val targetItemIndex = uiState.focusedSegmentIndex.coerceIn(0, uiState.railItems.lastIndex)
        val isTargetVisible = listState.layoutInfo.visibleItemsInfo.any { item -> item.index == targetItemIndex }
        if (!isTargetVisible) {
            listState.animateScrollToItem(targetItemIndex)
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .background(color = railColor),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                state = listState,
            ) {
                items(items = listOf("navigation-rail-start"), key = { it }) {
                    NavigationSegmentRailWaypoint(
                        label = stringResource(id = R.string.navigation_rail_origin_label),
                        iconRes = R.drawable.ic_navigation_rail_origin_pin,
                        segmentItem = railSlots.originItem,
                        dividerColor = dividerColor,
                        onClick = {
                            railSlots.originItem?.index?.let(onSegmentTapped)
                        },
                    )
                }
                items(items = railSlots.intermediateItems, key = { item -> item.index }) { item ->
                    NavigationSegmentRailItem(
                        item = item,
                        dividerColor = dividerColor,
                        onClick = { onSegmentTapped(item.index) },
                    )
                }
                items(items = listOf("navigation-rail-destination"), key = { it }) {
                    NavigationSegmentRailWaypoint(
                        label = stringResource(id = R.string.navigation_rail_destination_label),
                        iconRes = R.drawable.ic_navigation_rail_destination_pin,
                        segmentItem = railSlots.destinationItem,
                        dividerColor = dividerColor,
                        onClick = {
                            railSlots.destinationItem?.index?.let(onSegmentTapped)
                        },
                    )
                }
                items(items = listOf("navigation-rail-return"), key = { it }) {
                    NavigationSegmentRailReturnAction(
                        enabled = railSlots.canReturnToActiveSegment,
                        dividerColor = dividerColor,
                        onClick = onReturnToActiveSegmentClick,
                    )
                }
            }
            NavigationSegmentRailDetailAction(
                enabled = isRouteDetailEnabled,
                dividerColor = dividerColor,
                onClick = onRouteDetailClick,
            )
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(dividerColor),
        )
    }
}

@Composable
private fun NavigationSegmentRailWaypoint(
    label: String,
    iconRes: Int,
    segmentItem: NavigationSegmentRailItemUiState?,
    dividerColor: Color,
    onClick: () -> Unit,
) {
    val enabled = segmentItem != null
    val tone = segmentItem?.let { item -> navigationSegmentRailTone(item) } ?: navigationDisabledRailTone()
    val isSelected = segmentItem?.isSelected == true
    val contentLabel =
        segmentItem?.let { item ->
            "$label ${item.guidanceAction.label} ${item.distanceLabel}"
        } ?: label

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(tone.containerColor)
                    .semantics {
                        contentDescription = contentLabel
                        selected = isSelected
                        stateDescription = segmentItem?.stateLabel ?: label
                        if (!enabled) {
                            disabled()
                        }
                    }
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (tone.indicatorColor != Color.Transparent) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(tone.indicatorColor),
                )
            }
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier =
                    Modifier
                        .width(42.dp)
                        .height(50.dp)
                        .alpha(tone.iconAlpha),
            )
        }
        HorizontalDivider(color = dividerColor)
    }
}

@Composable
private fun NavigationSegmentRailItem(
    item: NavigationSegmentRailItemUiState,
    dividerColor: Color,
    onClick: () -> Unit,
) {
    val tone = navigationSegmentRailTone(item)
    val isSelected = item.isSelected

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(tone.containerColor)
                    .semantics {
                        contentDescription = "${item.guidanceAction.label} ${item.distanceLabel}"
                        selected = isSelected
                        stateDescription = item.stateLabel
                    }
                    .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (tone.indicatorColor != Color.Transparent) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(tone.indicatorColor),
                )
            }
            Icon(
                painter = painterResource(id = item.guidanceAction.iconRes()),
                contentDescription = null,
                tint = tone.iconTint,
                modifier =
                    Modifier
                        .size(item.guidanceAction.railIconSize())
                        .alpha(tone.iconAlpha),
            )
        }
        HorizontalDivider(color = dividerColor)
    }
}

private fun NavigationGuidanceAction.railIconSize(): Dp =
    if (this == NavigationGuidanceAction.BUS || this == NavigationGuidanceAction.SUBWAY) {
        NavigationSegmentRailTransitIconSize
    } else {
        NavigationSegmentRailIconSize
    }

@Composable
private fun NavigationSegmentRailReturnAction(
    enabled: Boolean,
    dividerColor: Color,
    onClick: () -> Unit,
) {
    val label = stringResource(id = R.string.navigation_return_to_active_segment_label)
    val outlineColor =
        if (enabled) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        }
    val iconAlpha = if (enabled) 0.86f else 0.34f

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics {
                        contentDescription = label
                        if (!enabled) {
                            disabled()
                        }
                    }
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .border(
                            width = 1.dp,
                            color = outlineColor,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_control_previous),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .size(22.dp)
                            .rotate(90f)
                            .alpha(iconAlpha),
                )
            }
        }
        HorizontalDivider(color = dividerColor)
    }
}

@Composable
private fun NavigationSegmentRailDetailAction(
    enabled: Boolean,
    dividerColor: Color,
    onClick: () -> Unit,
) {
    val detailLabel = stringResource(id = R.string.navigation_detail_button_label)
    val iconAlpha = if (enabled) 0.86f else 0.34f

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics {
                        contentDescription = detailLabel
                        if (!enabled) {
                            disabled()
                        }
                    }
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_navigation_detail_more),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .size(30.dp)
                        .alpha(iconAlpha),
            )
        }
        HorizontalDivider(color = dividerColor)
    }
}

private data class NavigationSegmentRailTone(
    val containerColor: Color,
    val indicatorColor: Color,
    val iconTint: Color,
    val iconAlpha: Float,
)

internal data class NavigationSegmentRailSlots(
    val originItem: NavigationSegmentRailItemUiState? = null,
    val intermediateItems: List<NavigationSegmentRailItemUiState> = emptyList(),
    val destinationItem: NavigationSegmentRailItemUiState? = null,
    val canReturnToActiveSegment: Boolean = false,
)

internal fun createNavigationSegmentRailSlots(uiState: NavigationSegmentSyncUiState): NavigationSegmentRailSlots {
    val railItems = uiState.railItems
    val canReturnToActiveSegment =
        railItems.isNotEmpty() &&
            (uiState.isInspectingSegments || uiState.focusedSegmentIndex != uiState.activeSegmentIndex)

    return NavigationSegmentRailSlots(
        originItem = railItems.firstOrNull(),
        intermediateItems =
            if (railItems.size <= 2) {
                emptyList()
            } else {
                railItems.subList(1, railItems.lastIndex)
            },
        destinationItem = railItems.lastOrNull(),
        canReturnToActiveSegment = canReturnToActiveSegment,
    )
}

@Composable
private fun navigationSegmentRailTone(item: NavigationSegmentRailItemUiState): NavigationSegmentRailTone =
    when {
        item.isFocused ->
            NavigationSegmentRailTone(
                containerColor = MaterialTheme.colorScheme.primary,
                indicatorColor = Color.Transparent,
                iconTint = MaterialTheme.colorScheme.onPrimary,
                iconAlpha = 1f,
            )

        item.isActive ->
            NavigationSegmentRailTone(
                containerColor = Color.Transparent,
                indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                iconTint = MaterialTheme.colorScheme.onSurface,
                iconAlpha = 1f,
            )

        item.isCompleted ->
            NavigationSegmentRailTone(
                containerColor = Color.Transparent,
                indicatorColor = Color.Transparent,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                iconAlpha = 0.42f,
            )

        else ->
            NavigationSegmentRailTone(
                containerColor = Color.Transparent,
                indicatorColor = Color.Transparent,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                iconAlpha = 0.72f,
            )
    }

private fun navigationDisabledRailTone(): NavigationSegmentRailTone =
    NavigationSegmentRailTone(
        containerColor = Color.Transparent,
        indicatorColor = Color.Transparent,
        iconTint = Color.Unspecified,
        iconAlpha = 0.38f,
    )

private val NavigationSegmentRailItemUiState.isSelected: Boolean
    get() = isFocused || isActive

private val NavigationSegmentRailIconSize = 34.dp
private val NavigationSegmentRailTransitIconSize = 30.dp

private val NavigationSegmentRailItemUiState.stateLabel: String
    get() =
        when {
            isFocused -> "Selected segment"
            isActive -> "Current segment"
            isCompleted -> "Completed segment"
            else -> "Guidance segment"
        }
