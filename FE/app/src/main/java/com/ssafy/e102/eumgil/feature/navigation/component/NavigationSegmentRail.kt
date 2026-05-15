package com.ssafy.e102.eumgil.feature.navigation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.guidance.component.GuideCollapsedRailItem
import com.ssafy.e102.eumgil.feature.guidance.component.resolveGuideRailPromotedItemIndex
import com.ssafy.e102.eumgil.feature.guidance.component.shouldHideGuideRailItemForTopCard
import com.ssafy.e102.eumgil.feature.navigation.NavigationGuidanceAction
import com.ssafy.e102.eumgil.feature.navigation.NavigationSegmentRailItemUiState
import com.ssafy.e102.eumgil.feature.navigation.NavigationSegmentSyncUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun NavigationSegmentRail(
    uiState: NavigationSegmentSyncUiState,
    onSegmentTapped: (Int) -> Unit,
    onTopVisibleSegmentChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
    val railSlots = createNavigationSegmentRailSlots(uiState)
    val railFocusItems = railSlots.focusItems()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var hiddenRailItemPosition by remember { mutableStateOf<Int?>(null) }
    val fallbackRailItemSizePx = with(LocalDensity.current) { NavigationSegmentRailItemHeight.roundToPx() }
    val hiddenSegmentIndex = hiddenRailItemPosition?.let { position -> railFocusItems.getOrNull(position)?.index }
    val currentFocusedSegmentIndex by rememberUpdatedState(uiState.focusedSegmentIndex)
    val currentOnTopVisibleSegmentChanged by rememberUpdatedState(onTopVisibleSegmentChanged)

    LaunchedEffect(listState, railFocusItems, fallbackRailItemSizePx) {
        var hasObservedInitialPosition = false
        snapshotFlow {
            val firstVisibleItemSizePx =
                listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { item -> item.index == listState.firstVisibleItemIndex }
                    ?.size
                    ?: fallbackRailItemSizePx
                NavigationRailPromotionSnapshot(
                    firstVisibleItemIndex = listState.firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                    firstVisibleItemSizePx = firstVisibleItemSizePx,
                    isScrollInProgress = listState.isScrollInProgress,
                    promotedItemPosition =
                        resolveGuideRailPromotedItemIndex(
                            firstVisibleItemIndex = listState.firstVisibleItemIndex,
                        firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                        firstVisibleItemSizePx = firstVisibleItemSizePx,
                        itemCount = railFocusItems.size,
                    ),
            )
        }
            .distinctUntilChanged()
            .collect { snapshot ->
                if (!hasObservedInitialPosition) {
                    hasObservedInitialPosition = true
                    return@collect
                }
                if (snapshot.shouldSnapToPromotedItem()) {
                    snapshot.promotedItemPosition?.let { position ->
                        hiddenRailItemPosition = position
                        listState.animateScrollToItem(position, scrollOffset = 0)
                        listState.scrollToItem(position, scrollOffset = 0)
                    }
                }
                val isSettlingAfterCollapsedTopCard =
                    !snapshot.isScrollInProgress &&
                        snapshot.firstVisibleItemScrollOffset == 0 &&
                        hiddenRailItemPosition != null &&
                        snapshot.firstVisibleItemIndex == hiddenRailItemPosition?.plus(1)
                if (!snapshot.isScrollInProgress && !isSettlingAfterCollapsedTopCard) {
                    val promotedItemPosition = snapshot.promotedItemPosition
                    val promotedSegmentIndex =
                        promotedItemPosition?.let { position -> railFocusItems.getOrNull(position)?.index }
                    if (promotedItemPosition != null && promotedSegmentIndex != null) {
                        hiddenRailItemPosition = promotedItemPosition
                        promotedSegmentIndex
                            .takeIf { index -> index != currentFocusedSegmentIndex }
                            ?.let(currentOnTopVisibleSegmentChanged)
                    }
                }
            }
    }

    LaunchedEffect(uiState.focusedSegmentIndex, railFocusItems.size) {
        if (railFocusItems.isEmpty()) return@LaunchedEffect
        if (uiState.isInspectingSegments || listState.isScrollInProgress) return@LaunchedEffect
        val targetItemPosition =
            railFocusItems.indexOfFirst { item -> item.index == uiState.focusedSegmentIndex }
                .takeIf { position -> position >= 0 }
                ?: return@LaunchedEffect
        val isTargetVisible = listState.layoutInfo.visibleItemsInfo.any { item -> item.index == targetItemPosition }
        if (!isTargetVisible) {
            listState.animateScrollToItem(targetItemPosition)
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            ) {
                val navigationRailEndSnapPadding =
                    (maxHeight - NavigationSegmentRailItemHeight).coerceAtLeast(0.dp)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = navigationRailEndSnapPadding),
                ) {
                    items(items = listOf("navigation-rail-start"), key = { it }) {
                        NavigationSegmentRailWaypoint(
                            label = stringResource(id = R.string.navigation_rail_origin_label),
                            iconRes = R.drawable.ic_navigation_rail_origin_pin,
                            segmentItem = railSlots.originItem,
                            dividerColor = dividerColor,
                            isContentHidden =
                                railSlots.originItem?.index?.let { index ->
                                    shouldHideGuideRailItemForTopCard(index, hiddenSegmentIndex)
                                } == true,
                            onClick = {
                                hiddenRailItemPosition = railFocusItems.indexOfFirst { item -> item == railSlots.originItem }
                                    .takeIf { position -> position >= 0 }
                                railSlots.originItem?.index?.let(onSegmentTapped)
                            },
                        )
                    }
                    items(items = railSlots.intermediateItems, key = { item -> item.index }) { item ->
                        NavigationSegmentRailItem(
                            item = item,
                            dividerColor = dividerColor,
                            isContentHidden = shouldHideGuideRailItemForTopCard(item.index, hiddenSegmentIndex),
                            onClick = {
                                hiddenRailItemPosition = railFocusItems.indexOfFirst { railItem -> railItem.index == item.index }
                                    .takeIf { position -> position >= 0 }
                                onSegmentTapped(item.index)
                            },
                        )
                    }
                    items(items = listOf("navigation-rail-destination"), key = { it }) {
                        NavigationSegmentRailWaypoint(
                            label = stringResource(id = R.string.navigation_rail_destination_label),
                            iconRes = R.drawable.ic_navigation_rail_destination_pin,
                            segmentItem = railSlots.destinationItem,
                            dividerColor = dividerColor,
                            isContentHidden =
                                railSlots.destinationItem?.index?.let { index ->
                                    shouldHideGuideRailItemForTopCard(index, hiddenSegmentIndex)
                                } == true,
                            onClick = {
                                hiddenRailItemPosition = railFocusItems.indexOfFirst { item -> item == railSlots.destinationItem }
                                    .takeIf { position -> position >= 0 }
                                railSlots.destinationItem?.index?.let(onSegmentTapped)
                            },
                        )
                    }
                    items(items = listOf("navigation-rail-return"), key = { it }) {
                        NavigationSegmentRailTopAction(
                            enabled = railSlots.canScrollToTop,
                            dividerColor = dividerColor,
                            onClick = {
                                coroutineScope.launch {
                                    hiddenRailItemPosition = 0
                                    listState.animateScrollToItem(0, scrollOffset = 0)
                                    listState.scrollToItem(0, scrollOffset = 0)
                                    railFocusItems.firstOrNull()?.index?.let(onSegmentTapped)
                                }
                            },
                        )
                    }
                }
            }
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
    isContentHidden: Boolean,
    onClick: () -> Unit,
) {
    val enabled = segmentItem != null
    val item = segmentItem
    val contentLabel =
        item?.let {
            "$label ${item.guidanceAction.label} ${item.distanceLabel}"
        } ?: label

    GuideCollapsedRailItem(
        action = item?.guidanceAction ?: NavigationGuidanceAction.STRAIGHT,
        isOrigin = iconRes == R.drawable.ic_navigation_rail_origin_pin,
        isDestination = iconRes == R.drawable.ic_navigation_rail_destination_pin,
        isActive = item?.isActive == true,
        isFocused = item?.isFocused == true,
        isSelected = item?.isSelected == true,
        enabled = enabled,
        contentDescription = contentLabel,
        stateDescription = item?.stateLabel ?: label,
        dividerColor = dividerColor,
        height = NavigationSegmentRailItemHeight,
        isContentHidden = isContentHidden,
        onClick = onClick,
    )
}

@Composable
private fun NavigationSegmentRailItem(
    item: NavigationSegmentRailItemUiState,
    dividerColor: Color,
    isContentHidden: Boolean,
    onClick: () -> Unit,
) {
    GuideCollapsedRailItem(
        action = item.guidanceAction,
        isActive = item.isActive,
        isFocused = item.isFocused,
        isSelected = item.isSelected,
        contentDescription = "${item.guidanceAction.label} ${item.distanceLabel}",
        stateDescription = item.stateLabel,
        dividerColor = dividerColor,
        height = NavigationSegmentRailItemHeight,
        isContentHidden = isContentHidden,
        onClick = onClick,
    )
}

internal fun NavigationGuidanceAction.railIconSize(): Dp =
    if (this == NavigationGuidanceAction.BUS || this == NavigationGuidanceAction.SUBWAY) {
        NavigationSegmentRailTransitIconSize
    } else {
        NavigationSegmentRailIconSize
    }

@Composable
private fun NavigationSegmentRailTopAction(
    enabled: Boolean,
    dividerColor: Color,
    onClick: () -> Unit,
) {
    val label = stringResource(id = R.string.navigation_rail_scroll_to_top_label)
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

internal data class NavigationSegmentRailSlots(
    val originItem: NavigationSegmentRailItemUiState? = null,
    val intermediateItems: List<NavigationSegmentRailItemUiState> = emptyList(),
    val destinationItem: NavigationSegmentRailItemUiState? = null,
    val canScrollToTop: Boolean = false,
)

internal fun createNavigationSegmentRailSlots(uiState: NavigationSegmentSyncUiState): NavigationSegmentRailSlots {
    val railItems = uiState.railItems

    return NavigationSegmentRailSlots(
        originItem = railItems.firstOrNull(),
        intermediateItems =
            if (railItems.size <= 2) {
                emptyList()
            } else {
                railItems.subList(1, railItems.lastIndex)
            },
        destinationItem = railItems.lastOrNull(),
        canScrollToTop = railItems.isNotEmpty(),
    )
}

private fun NavigationSegmentRailSlots.focusItems(): List<NavigationSegmentRailItemUiState> =
    buildList {
        originItem?.let(::add)
        addAll(intermediateItems)
        destinationItem?.let(::add)
    }

private val NavigationSegmentRailItemUiState.isSelected: Boolean
    get() = isFocused || isActive

private val NavigationSegmentRailIconSize = 34.dp
internal val NavigationSegmentRailTransitIconSize = 30.dp

private val NavigationSegmentRailItemUiState.stateLabel: String
    get() =
        when {
            isFocused -> "Selected segment"
            isActive -> "Current segment"
            isCompleted -> "Completed segment"
            else -> "Guidance segment"
        }

private data class NavigationRailPromotionSnapshot(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val firstVisibleItemSizePx: Int,
    val isScrollInProgress: Boolean,
    val promotedItemPosition: Int?,
) {
    fun shouldSnapToPromotedItem(): Boolean =
        promotedItemPosition != null &&
            !isScrollInProgress &&
            (firstVisibleItemScrollOffset > 0 || firstVisibleItemIndex != promotedItemPosition)
}

private val NavigationSegmentRailItemHeight = 96.dp
