package com.ssafy.e102.eumgil.feature.map

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.BuildConfig
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.AccessibilityTag
import com.ssafy.e102.eumgil.core.model.BrailleBlockType
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.MapPlaceDetailType
import com.ssafy.e102.eumgil.core.model.MapTappedPlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import com.ssafy.e102.eumgil.feature.map.component.FacilityDetailBottomSheetShell
import com.ssafy.e102.eumgil.feature.map.component.FacilityDetailBottomSheetShellState
import com.ssafy.e102.eumgil.feature.map.component.MapFloatingControls
import com.ssafy.e102.eumgil.feature.map.component.MapIntegrationState
import com.ssafy.e102.eumgil.feature.map.component.MapShortcutFilterRow
import com.ssafy.e102.eumgil.feature.map.component.MapShellScaffold
import com.ssafy.e102.eumgil.feature.map.component.MapTopSearchBar
import com.ssafy.e102.eumgil.feature.map.component.MapViewport
import com.ssafy.e102.eumgil.feature.map.component.MapViewportUiState
import com.ssafy.e102.eumgil.feature.map.component.createMapMarkerViewportOverlayState
import com.ssafy.e102.eumgil.feature.map.component.RecentDestinationBottomSheetShell
import com.ssafy.e102.eumgil.feature.map.component.RecentDestinationBottomSheetState
import com.ssafy.e102.eumgil.feature.map.component.RecentDestinationRowState
import com.ssafy.e102.eumgil.feature.map.component.resolveMapIntegrationState
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapDefaults
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("UNUSED_PARAMETER")
@Composable
fun MapScreen(
    uiState: MapUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (MapUiAction) -> Unit,
    onNavigateToSavedRoutes: () -> Unit,
    onNavigateToMyPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewportState = mapViewportState(uiState = uiState)
    val searchBarState = mapSearchBarState(uiState = uiState)
    val facilityDetailSheetUiState = mapFacilityDetailBottomSheetState(uiState = uiState)
    val recentDestinationSheetState = mapRecentDestinationBottomSheetState(uiState = uiState)
    val routeSelectionStatusState = mapRouteSelectionStatusState(uiState = uiState)

    Box(modifier = modifier.fillMaxSize()) {
        MapShellScaffold(
            mapContent = {
                MapViewport(
                    state = viewportState,
                    onMarkerClick = { markerId ->
                        onAction(MapUiAction.MarkerTapped(markerId))
                    },
                    onCameraMoveEnd = { center, zoomLevel, isUserGesture, isSelectedMapPinVisibleInViewport ->
                        onAction(
                            MapUiAction.ViewportCameraChanged(
                                center = center,
                                zoomLevel = zoomLevel,
                                isUserGesture = isUserGesture,
                                isSelectedMapPinVisibleInViewport = isSelectedMapPinVisibleInViewport,
                            ),
                        )
                    },
                    onMapClick = { payload ->
                        onAction(MapUiAction.MapTapped(payload))
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            },
            topOverlay = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                ) {
                    MapTopSearchBar(
                        title = searchBarState.title,
                        subtitle = searchBarState.subtitle,
                        accessibilityLabel = searchBarState.accessibilityLabel,
                        onClick = { onAction(MapUiAction.SearchEntryClicked) },
                    )

                    MapShortcutFilterRow(
                        state = uiState.shortcutFilterState,
                        onChipClick = { key ->
                            onAction(MapUiAction.ShortcutFilterClicked(key))
                        },
                    )

                    routeSelectionStatusState?.let { state ->
                        MapRouteSelectionStatusBar(
                            state = state,
                            onOriginClick = {
                                onAction(MapUiAction.RouteEndpointStatusClicked(RouteEditingTarget.ORIGIN))
                            },
                            onDestinationClick = {
                                onAction(MapUiAction.RouteEndpointStatusClicked(RouteEditingTarget.DESTINATION))
                            },
                        )
                    }

                    if (uiState.isSearchHereVisible) {
                        MapSearchHereButton(
                            onClick = { onAction(MapUiAction.SearchHereClicked) },
                            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally),
                        )
                    }
                }
            },
            controlOverlay = {
                MapFloatingControls(
                    recenterButtonState = uiState.recenterButtonState,
                    isRecenterButtonActive = uiState.isRecenterButtonActive,
                    onRecenterClick = { onAction(MapUiAction.LocationActionClicked) },
                    onZoomInClick = { onAction(MapUiAction.ZoomInClicked) },
                    onZoomOutClick = { onAction(MapUiAction.ZoomOutClicked) },
                )
            },
            bottomOverlay = {
                RecentDestinationBottomSheetShell(
                    state =
                        recentDestinationSheetState.copy(
                            isVisible =
                                recentDestinationSheetState.isVisible &&
                                    facilityDetailSheetUiState.isVisible.not(),
                        ),
                    onViewAllClick = onNavigateToSavedRoutes,
                    onRouteClick = { placeId ->
                        onAction(MapUiAction.RecentDestinationRouteClicked(placeId))
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                FacilityDetailBottomSheetShell(
                    state = facilityDetailSheetUiState.toShellState(),
                    onDismiss = { onAction(MapUiAction.FacilityDetailDismissed) },
                    modifier = Modifier.fillMaxSize(),
                    detailContent = {
                        FacilityDetailAccessibilityTagSection(
                            tags = facilityDetailSheetUiState.accessibilityTags,
                        )
                    },
                    actionContent = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onAction(
                                            MapUiAction.FacilitySetRouteEndpointClicked(
                                                RouteEditingTarget.ORIGIN,
                                            ),
                                        )
                                    },
                                    enabled = facilityDetailSheetUiState.isRouteActionEnabled,
                                    modifier =
                                        Modifier
                                            .weight(1.15f)
                                            .height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    IconTextButtonContent(
                                        iconRes = R.drawable.ic_route_start_navigation_button,
                                        label = stringResource(id = R.string.map_facility_detail_set_origin_action),
                                    )
                                }
                                NoRippleMapPrimaryActionButton(
                                    onClick = {
                                        onAction(
                                            MapUiAction.FacilitySetRouteEndpointClicked(
                                                RouteEditingTarget.DESTINATION,
                                            ),
                                        )
                                    },
                                    enabled = facilityDetailSheetUiState.isRouteActionEnabled,
                                    modifier =
                                        Modifier
                                            .weight(1.15f)
                                            .height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    IconTextButtonContent(
                                        iconRes = R.drawable.ic_route_start_navigation_button,
                                        label = stringResource(
                                            id = R.string.map_facility_detail_set_destination_action,
                                        ),
                                    )
                                }
                                FacilityDetailBookmarkActionButton(
                                    state = facilityDetailSheetUiState,
                                    onToggle = { onAction(MapUiAction.FacilityBookmarkClicked) },
                                )
                            }
                            facilityDetailSheetUiState.bookmarkErrorMessage?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                )
            },
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.medium),
        )
    }
}

@Immutable
private data class MapSearchBarState(
    val title: String,
    val subtitle: String?,
    val accessibilityLabel: String,
)

@Immutable
private data class MapLocationPanelState(
    val badgeLabel: String,
    val title: String,
    val description: String,
    val supportingText: String,
    @DrawableRes val actionIconRes: Int,
    val actionLabel: String,
    val isActionEnabled: Boolean,
    val isPrimaryAction: Boolean,
    val isCriticalState: Boolean,
)

@Immutable
private data class MapFacilityDetailSheetUiState(
    val isVisible: Boolean,
    @DrawableRes val placeIconRes: Int,
    val metaLabel: String,
    val title: String,
    val address: String,
    val accessibilityTags: List<String>,
    val isBookmarked: Boolean,
    val isBookmarkUpdating: Boolean,
    val isBookmarkEnabled: Boolean,
    val isRouteActionEnabled: Boolean,
    val bookmarkErrorMessage: String?,
) {
    fun toShellState(): FacilityDetailBottomSheetShellState =
        FacilityDetailBottomSheetShellState(
            isVisible = isVisible,
            placeIconRes = placeIconRes,
            metaLabel = metaLabel,
            title = title,
            address = address,
            hasDetailContent = accessibilityTags.isNotEmpty(),
        )
}

@Immutable
private data class MapRecentDestinationBottomSheetUiState(
    val isVisible: Boolean,
    val items: List<RecentDestinationRowState>,
) {
    fun toShellState(): RecentDestinationBottomSheetState =
        RecentDestinationBottomSheetState(
            isVisible = isVisible,
            items = items,
        )
}

@Immutable
private data class MapRouteSelectionStatusUiState(
    val originLabel: String?,
    val destinationLabel: String?,
    val accessibilityLabel: String,
)

@Composable
private fun MapRouteSelectionStatusBar(
    state: MapRouteSelectionStatusUiState,
    onOriginClick: () -> Unit,
    onDestinationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = state.accessibilityLabel
                },
        shape = RoundedCornerShape(EumRadius.medium),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = EumSpacing.small,
                    vertical = EumSpacing.xSmall,
                ),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            MapRouteEndpointChip(
                label = stringResource(id = R.string.route_setting_origin_label),
                value = state.originLabel ?: stringResource(id = R.string.map_route_selection_origin_empty),
                containerColor = RouteSelectionOriginChipColor,
                contentColor = RouteSelectionOriginTextColor,
                onClick = onOriginClick,
                modifier = Modifier.weight(1f),
            )
            MapRouteEndpointChip(
                label = stringResource(id = R.string.route_setting_destination_label),
                value = state.destinationLabel ?: stringResource(id = R.string.map_route_selection_destination_empty),
                containerColor = RouteSelectionDestinationChipColor,
                contentColor = RouteSelectionDestinationTextColor,
                onClick = onDestinationClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MapRouteEndpointChip(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier =
            modifier
                .semantics {
                    role = Role.Button
                    contentDescription = "$label $value"
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        shape = RoundedCornerShape(EumRadius.small),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = EumSpacing.small, vertical = EumSpacing.xSmall),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MapSearchHereButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val label = stringResource(id = R.string.map_search_here_action)
    Surface(
        modifier =
            modifier
                .semantics {
                    role = Role.Button
                    contentDescription = label
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = EumSpacing.medium, vertical = EumSpacing.xSmall),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MapLocationStatusCard(
    state: MapLocationPanelState,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        when {
            state.isCriticalState -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
            state.isPrimaryAction -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        }
    val borderColor =
        when {
            state.isCriticalState -> MaterialTheme.colorScheme.error.copy(alpha = 0.32f)
            state.isPrimaryAction -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
        }
    val badgeContainerColor =
        when {
            state.isCriticalState -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            state.isPrimaryAction -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
        }
    val badgeContentColor =
        when {
            state.isCriticalState -> MaterialTheme.colorScheme.error
            state.isPrimaryAction -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondary
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EumRadius.large),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Surface(
                shape = RoundedCornerShape(EumRadius.full),
                color = badgeContainerColor,
            ) {
                Text(
                    text = state.badgeLabel,
                    modifier =
                        Modifier.padding(
                            horizontal = EumSpacing.small,
                            vertical = EumSpacing.xSmall,
                        ),
                    style = MaterialTheme.typography.labelLarge,
                    color = badgeContentColor,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = state.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.isPrimaryAction) {
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isActionEnabled,
                ) {
                    IconTextButtonContent(
                        iconRes = state.actionIconRes,
                        label = state.actionLabel,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isActionEnabled,
                ) {
                    IconTextButtonContent(
                        iconRes = state.actionIconRes,
                        label = state.actionLabel,
                    )
                }
            }
        }
    }
}

@Composable
private fun IconTextButtonContent(
    @DrawableRes iconRes: Int,
    label: String,
) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(EumSpacing.xSmall))
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun NoRippleMapPrimaryActionButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    Surface(
        modifier = modifier,
        shape = shape,
        color = if (enabled) containerColor else disabledContainerColor,
        contentColor = if (enabled) contentColor else disabledContentColor,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    )
                    .padding(horizontal = EumSpacing.medium),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun FacilityDetailBookmarkActionButton(
    state: MapFacilityDetailSheetUiState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookmarkButtonLabel = stringResource(id = R.string.map_facility_detail_bookmark_button_label)
    val bookmarkStateDescription =
        when {
            state.isBookmarkEnabled.not() -> "Bookmark is unavailable for this place."

            state.isBookmarkUpdating ->
                stringResource(id = R.string.map_facility_detail_bookmark_state_updating)

            state.isBookmarked ->
                stringResource(id = R.string.map_facility_detail_bookmark_state_saved)

            else -> stringResource(id = R.string.map_facility_detail_bookmark_state_unsaved)
        }
    val contentColor =
        when {
            state.isBookmarkEnabled.not() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            state.isBookmarked -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.primary
        }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        IconButton(
            onClick = onToggle,
            enabled = state.isBookmarkEnabled && state.isBookmarkUpdating.not(),
            modifier =
                Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = bookmarkButtonLabel
                        stateDescription = bookmarkStateDescription
                    },
        ) {
            Icon(
                painter =
                    painterResource(
                        id =
                            if (state.isBookmarkUpdating) {
                                if (state.isBookmarked) {
                                    R.drawable.ic_nav_bookmark_selected
                                } else {
                                    R.drawable.ic_nav_bookmark_outline
                                }
                            } else if (state.isBookmarked) {
                                R.drawable.ic_nav_bookmark_selected
                            } else {
                                R.drawable.ic_nav_bookmark_outline
                            },
                    ),
                contentDescription = null,
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun FacilityDetailAccessibilityTagSection(
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
    ) {
        tags.chunked(2).forEach { rowLabels ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                rowLabels.forEach { label ->
                    FacilityDetailTagCard(
                        label = label,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (rowLabels.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FacilityDetailTagCard(
    label: String,
    modifier: Modifier = Modifier,
) {
    val iconRes = facilityAccessibilityTagIconRes(label)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = EumSpacing.small),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            iconRes?.let { resId ->
                Icon(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier.size(facilityAccessibilityTagIconSizeDp(resId).dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
@DrawableRes
private fun facilityAccessibilityTagIconRes(label: String): Int? {
    val normalizedLabel = label.trim()
    val bareGuidanceLabel = stringResource(id = R.string.place_accessibility_label_guidance_facility).substringBeforeLast(' ')
    return when (normalizedLabel) {
        stringResource(id = R.string.map_facility_detail_tag_accessible_toilet),
        stringResource(id = R.string.place_accessibility_label_accessible_toilet),
        -> R.drawable.ic_accessibility_tag_accessible_toilet

        stringResource(id = R.string.map_facility_detail_tag_elevator),
        stringResource(id = R.string.place_accessibility_label_elevator),
        -> R.drawable.ic_accessibility_tag_elevator

        stringResource(id = R.string.map_facility_detail_tag_accessible_parking),
        stringResource(id = R.string.place_accessibility_label_accessible_parking),
        -> R.drawable.ic_accessibility_tag_accessible_parking

        stringResource(id = R.string.map_facility_detail_tag_step_free_entrance),
        stringResource(id = R.string.place_accessibility_label_step_free),
        -> R.drawable.ic_accessibility_tag_step_free

        stringResource(id = R.string.map_facility_detail_tag_charging_station),
        -> R.drawable.ic_accessibility_tag_charging_station

        bareGuidanceLabel,
        stringResource(id = R.string.map_facility_detail_tag_guidance_facility),
        stringResource(id = R.string.place_accessibility_label_guidance_facility),
        -> R.drawable.ic_accessibility_tag_guidance_facility

        else -> null
    }
}

private fun facilityAccessibilityTagIconSizeDp(
    @DrawableRes iconRes: Int,
): Int =
    when (iconRes) {
        R.drawable.ic_accessibility_tag_accessible_toilet -> 18
        else -> 16
    }

@Composable
private fun mapLocationPanelState(uiState: MapUiState): MapLocationPanelState {
    return when (val status = uiState.locationStatus) {
        MapLocationStatus.PermissionDenied ->
            MapLocationPanelState(
                badgeLabel = stringResource(id = R.string.map_location_status_permission_badge),
                title = stringResource(id = R.string.map_location_status_permission_title),
                description = stringResource(id = R.string.map_location_status_permission_description),
                supportingText = stringResource(id = R.string.map_location_status_permission_supporting),
                actionIconRes = R.drawable.ic_permission_location,
                actionLabel = stringResource(id = R.string.map_location_action_request_permission),
                isActionEnabled = true,
                isPrimaryAction = false,
                isCriticalState = false,
            )

        MapLocationStatus.Loading ->
            MapLocationPanelState(
                badgeLabel = stringResource(id = R.string.map_location_status_loading_badge),
                title = stringResource(id = R.string.map_location_status_loading_title),
                description = stringResource(id = R.string.map_location_status_loading_description),
                supportingText = stringResource(id = R.string.map_location_status_loading_supporting),
                actionIconRes = R.drawable.ic_status_hourglass,
                actionLabel = stringResource(id = R.string.map_location_action_loading),
                isActionEnabled = false,
                isPrimaryAction = false,
                isCriticalState = false,
            )

        is MapLocationStatus.Ready -> {
            val locationSummary =
                locationSummaryText(
                    location = status.location,
                    accuracyMeters = status.accuracyMeters,
                )

            MapLocationPanelState(
                badgeLabel = stringResource(id = R.string.map_location_status_ready_badge),
                title = stringResource(id = R.string.map_location_status_ready_title),
                description = stringResource(id = R.string.map_location_status_ready_description),
                supportingText =
                    stringResource(
                        id = R.string.map_location_status_ready_supporting,
                        locationSummary,
                    ),
                actionIconRes = R.drawable.ic_status_refresh,
                actionLabel = stringResource(id = R.string.map_location_action_recenter),
                isActionEnabled = true,
                isPrimaryAction = true,
                isCriticalState = false,
            )
        }

        is MapLocationStatus.Unavailable -> {
            val titleRes =
                when (status.reason) {
                    MapLocationUnavailableReason.CURRENT_LOCATION_UNAVAILABLE ->
                        R.string.map_location_status_unavailable_current_title

                    MapLocationUnavailableReason.LOCATION_SERVICES_DISABLED ->
                        R.string.map_location_status_unavailable_services_title

                    MapLocationUnavailableReason.NO_LOCATION_FEATURE ->
                        R.string.map_location_status_unavailable_feature_title
                }
            val descriptionRes =
                when (status.reason) {
                    MapLocationUnavailableReason.CURRENT_LOCATION_UNAVAILABLE ->
                        R.string.map_location_status_unavailable_current_description

                    MapLocationUnavailableReason.LOCATION_SERVICES_DISABLED ->
                        R.string.map_location_status_unavailable_services_description

                    MapLocationUnavailableReason.NO_LOCATION_FEATURE ->
                        R.string.map_location_status_unavailable_feature_description
                }
            val isRetryEnabled = status.reason != MapLocationUnavailableReason.NO_LOCATION_FEATURE
            val actionLabelRes =
                if (isRetryEnabled) {
                    R.string.map_location_action_retry
                } else {
                    R.string.map_location_action_disabled
                }

            MapLocationPanelState(
                badgeLabel = stringResource(id = R.string.map_location_status_unavailable_badge),
                title = stringResource(id = titleRes),
                description = stringResource(id = descriptionRes),
                supportingText = stringResource(id = R.string.map_location_status_unavailable_supporting),
                actionIconRes =
                    if (isRetryEnabled) {
                        R.drawable.ic_status_refresh
                    } else {
                        R.drawable.ic_status_cancel
                    },
                actionLabel = stringResource(id = actionLabelRes),
                isActionEnabled = isRetryEnabled,
                isPrimaryAction = false,
                isCriticalState = true,
            )
        }
    }
}

@Composable
private fun mapSearchBarState(uiState: MapUiState): MapSearchBarState {
    return MapSearchBarState(
        title = stringResource(id = R.string.map_shell_search_title),
        subtitle = null,
        accessibilityLabel = stringResource(id = R.string.map_shell_search_a11y_label),
    )
}

@Composable
private fun mapRouteSelectionStatusState(uiState: MapUiState): MapRouteSelectionStatusUiState? {
    val preview = uiState.facilityDetailSheetState.destinationPreview
    val selectedOriginName =
        when (preview?.editingTarget) {
            RouteEditingTarget.ORIGIN -> preview.destination.name
            else -> uiState.selectedOrigin?.name
        }?.takeIf(String::isNotBlank)
    val selectedDestinationName =
        when (preview?.editingTarget) {
            RouteEditingTarget.DESTINATION -> preview.destination.name
            else -> uiState.selectedDestination?.name
        }?.takeIf(String::isNotBlank)

    if (selectedOriginName == null && selectedDestinationName == null) {
        return null
    }

    return MapRouteSelectionStatusUiState(
        originLabel = selectedOriginName,
        destinationLabel = selectedDestinationName,
        accessibilityLabel =
            stringResource(
                id = R.string.map_route_selection_status_a11y,
                selectedOriginName ?: stringResource(id = R.string.map_route_selection_origin_empty),
                selectedDestinationName ?: stringResource(id = R.string.map_route_selection_destination_empty),
            ),
    )
}

@Composable
private fun mapRecentDestinationBottomSheetState(uiState: MapUiState): RecentDestinationBottomSheetState =
    MapRecentDestinationBottomSheetUiState(
        isVisible =
            uiState.recentDestinations.isNotEmpty() &&
                uiState.selectedMapPinCoordinate == null,
        items =
            uiState.recentDestinations.map { destination ->
                val tagLabels = recentDestinationTagLabels(destination)
                RecentDestinationRowState(
                    placeId = destination.placeId,
                    title = destination.name,
                    address = destination.address.orEmpty(),
                    tags = tagLabels.take(3),
                    overflowTagCount = (tagLabels.size - 3).coerceAtLeast(0),
                    iconRes = recentDestinationIcon(destination.category),
                )
            },
    ).toShellState()

@Composable
private fun mapTapFacilityDetailSheetState(uiState: MapUiState): MapFacilityDetailSheetUiState? {
    val sheetState = uiState.facilityDetailSheetState
    val mapTapDetail = sheetState.mapTapDetail
    val shouldDelayPoiSheetUntilDetail = sheetState.isMapTapDetailLoading && !sheetState.mapTapNameHint.isNullOrBlank()
    val loadingTitle =
        sheetState.mapTapNameHint
            ?.takeIf { it.isNotBlank() }
            ?: stringResource(id = R.string.map_facility_detail_loading_title)
    val errorTitle =
        sheetState.mapTapNameHint
            ?.takeIf { it.isNotBlank() }
            ?: stringResource(id = R.string.map_facility_detail_error_title)
    return when {
        mapTapDetail != null ->
            MapFacilityDetailSheetUiState(
                isVisible = true,
                placeIconRes = mapTapDetailPlaceIconRes(mapTapDetail),
                metaLabel =
                    mapTapDetailMetaLabel(
                        detail = mapTapDetail,
                        locationStatus = uiState.locationStatus,
                ),
                title = mapTapDetail.name,
                address = mapTapDetailAddressLabel(mapTapDetail),
                accessibilityTags =
                    mapTapDetailAccessibilityLabels(
                        detail = mapTapDetail,
                        selectedFilterCategories = uiState.markerFilterState.selection.selectedFacilityCategories,
                    ),
                isBookmarked = sheetState.isBookmarked,
                isBookmarkUpdating = sheetState.isBookmarkUpdating,
                isBookmarkEnabled = true,
                isRouteActionEnabled = mapTapDetail.hasValidCoordinate(),
                bookmarkErrorMessage = sheetState.bookmarkErrorMessage,
            )

        shouldDelayPoiSheetUntilDetail -> null

        sheetState.isMapTapDetailLoading ->
            MapFacilityDetailSheetUiState(
                isVisible = true,
                placeIconRes = R.drawable.ic_nav_facility,
                metaLabel = stringResource(id = R.string.map_facility_detail_location_meta),
                title = loadingTitle,
                address =
                    uiState.selectedMapPinCoordinate
                        ?.let { coordinate -> coordinateText(coordinate) }
                        .orEmpty(),
                accessibilityTags = emptyList(),
                isBookmarked = false,
                isBookmarkUpdating = true,
                isBookmarkEnabled = false,
                isRouteActionEnabled = false,
                bookmarkErrorMessage = null,
            )

        sheetState.mapTapDetailErrorMessage != null ->
            MapFacilityDetailSheetUiState(
                isVisible = true,
                placeIconRes = R.drawable.ic_nav_facility,
                metaLabel = stringResource(id = R.string.map_facility_detail_location_meta),
                title = errorTitle,
                address =
                    uiState.selectedMapPinCoordinate
                        ?.let { coordinate -> coordinateText(coordinate) }
                        .orEmpty(),
                accessibilityTags = emptyList(),
                isBookmarked = false,
                isBookmarkUpdating = false,
                isBookmarkEnabled = false,
                isRouteActionEnabled = false,
                bookmarkErrorMessage = null,
            )

        else -> null
    }
}

@Composable
private fun mapFacilityDetailBottomSheetState(uiState: MapUiState): MapFacilityDetailSheetUiState {
    mapTapFacilityDetailSheetState(uiState)?.let { sheetState ->
        return sheetState
    }

    val detail = uiState.facilityDetailSheetState.detail
    return if (detail == null) {
        MapFacilityDetailSheetUiState(
            isVisible = false,
            placeIconRes = R.drawable.ic_nav_facility,
            metaLabel = "",
            title = "",
            address = "",
            accessibilityTags = emptyList(),
            isBookmarked = false,
            isBookmarkUpdating = false,
            isBookmarkEnabled = false,
            isRouteActionEnabled = false,
            bookmarkErrorMessage = null,
        )
    } else if (uiState.facilityDetailSheetState.mapTapDetail != null) {
        val mapTapDetail = uiState.facilityDetailSheetState.mapTapDetail
        MapFacilityDetailSheetUiState(
            isVisible = true,
            placeIconRes = mapTapDetailPlaceIconRes(mapTapDetail),
            metaLabel =
                mapTapDetailMetaLabel(
                    detail = mapTapDetail,
                    locationStatus = uiState.locationStatus,
            ),
            title = mapTapDetail.name,
            address = mapTapDetailAddressLabel(mapTapDetail),
            accessibilityTags =
                mapTapDetailAccessibilityLabels(
                    detail = mapTapDetail,
                    selectedFilterCategories = uiState.markerFilterState.selection.selectedFacilityCategories,
                ),
            isBookmarked = uiState.facilityDetailSheetState.isBookmarked,
            isBookmarkUpdating = uiState.facilityDetailSheetState.isBookmarkUpdating,
            isBookmarkEnabled = true,
            isRouteActionEnabled = mapTapDetail.hasValidCoordinate(),
            bookmarkErrorMessage = uiState.facilityDetailSheetState.bookmarkErrorMessage,
        )
    } else if (uiState.facilityDetailSheetState.isMapTapDetailLoading) {
        MapFacilityDetailSheetUiState(
            isVisible = true,
            placeIconRes = R.drawable.ic_nav_facility,
            metaLabel = "위치 상세",
            title = "선택한 위치",
            address =
                uiState.selectedMapPinCoordinate
                    ?.let { coordinate -> coordinateText(coordinate) }
                    .orEmpty(),
            accessibilityTags = emptyList(),
            isBookmarked = false,
            isBookmarkUpdating = true,
            isBookmarkEnabled = false,
            isRouteActionEnabled = false,
            bookmarkErrorMessage = null,
        )
    } else if (uiState.facilityDetailSheetState.mapTapDetailErrorMessage != null) {
        MapFacilityDetailSheetUiState(
            isVisible = true,
            placeIconRes = R.drawable.ic_nav_facility,
            metaLabel = "위치 상세",
            title = "상세 정보를 불러오지 못했습니다",
            address =
                uiState.selectedMapPinCoordinate
                    ?.let { coordinate -> coordinateText(coordinate) }
                    .orEmpty(),
            accessibilityTags = emptyList(),
            isBookmarked = false,
            isBookmarkUpdating = false,
            isBookmarkEnabled = false,
            isRouteActionEnabled = false,
            bookmarkErrorMessage = null,
        )
    } else {
        MapFacilityDetailSheetUiState(
            isVisible = uiState.facilityDetailSheetState.isVisible,
            placeIconRes = facilityDetailPlaceIconRes(detail.category),
            metaLabel =
                facilityDetailMetaLabel(
                    detail = detail,
                    locationStatus = uiState.locationStatus,
            ),
            title = detail.name,
            address = facilityDetailAddressLabel(detail),
            accessibilityTags =
                facilityDetailAccessibilityLabels(
                    detail = detail,
                    selectedFilterCategories = uiState.markerFilterState.selection.selectedFacilityCategories,
                ),
            isBookmarked = uiState.facilityDetailSheetState.isBookmarked,
            isBookmarkUpdating = uiState.facilityDetailSheetState.isBookmarkUpdating,
            isBookmarkEnabled = true,
            isRouteActionEnabled = true,
            bookmarkErrorMessage = uiState.facilityDetailSheetState.bookmarkErrorMessage,
        )
    }
}

@Composable
private fun mapViewportState(uiState: MapUiState): MapViewportUiState {
    val cameraTarget = uiState.cameraTarget
    val currentLocationMarker = resolveCurrentLocationMarker(uiState.locationStatus)
    val preview = uiState.facilityDetailSheetState.destinationPreview
    val viewportOrigin =
        if (preview?.editingTarget == RouteEditingTarget.ORIGIN) {
            preview.destination
        } else {
            uiState.selectedOrigin
        }
    val viewportDestination =
        if (preview?.editingTarget == RouteEditingTarget.DESTINATION) {
            preview.destination
        } else {
            uiState.selectedDestination
        }
    val integrationState =
        resolveMapIntegrationState(
            hasNativeAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank(),
            isInspectionMode = LocalInspectionMode.current,
        )
    val statusLabel =
        when (uiState.locationStatus) {
            MapLocationStatus.PermissionDenied ->
                stringResource(id = R.string.map_viewport_status_permission)

            MapLocationStatus.Loading -> stringResource(id = R.string.map_viewport_status_loading)

            is MapLocationStatus.Ready -> stringResource(id = R.string.map_viewport_status_ready)

            is MapLocationStatus.Unavailable ->
                stringResource(id = R.string.map_viewport_status_unavailable)
        }
    val regionLabel =
        when (cameraTarget.source) {
            MapCameraSource.CURRENT_LOCATION ->
                stringResource(id = R.string.map_viewport_region_current)

            MapCameraSource.SEARCH_RESULT ->
                stringResource(id = R.string.map_viewport_region_selected)

            MapCameraSource.DEFAULT_BUSAN ->
                stringResource(id = R.string.map_viewport_region_default)
        }
    val title =
        when (cameraTarget.source) {
            MapCameraSource.CURRENT_LOCATION ->
                stringResource(id = R.string.map_viewport_title_current)

            MapCameraSource.SEARCH_RESULT ->
                stringResource(
                    id = R.string.map_viewport_title_selected,
                    viewportDestination?.name
                        ?: stringResource(id = R.string.map_shell_search_hint_selected_fallback),
                )

            MapCameraSource.DEFAULT_BUSAN ->
                stringResource(id = R.string.map_viewport_title_default)
        }
    val description =
        when (cameraTarget.source) {
            MapCameraSource.CURRENT_LOCATION ->
                stringResource(id = R.string.map_viewport_description_current)

            MapCameraSource.SEARCH_RESULT ->
                stringResource(id = R.string.map_viewport_description_selected)

            MapCameraSource.DEFAULT_BUSAN ->
                stringResource(id = R.string.map_viewport_description_default)
        }
    val supportingText =
        when (cameraTarget.source) {
            MapCameraSource.CURRENT_LOCATION ->
                locationSummaryText(
                    location = cameraTarget.center,
                    accuracyMeters = (uiState.locationStatus as? MapLocationStatus.Ready)?.accuracyMeters,
                )

            MapCameraSource.SEARCH_RESULT ->
                selectedDestinationSummaryText(destination = viewportDestination)

            MapCameraSource.DEFAULT_BUSAN ->
                stringResource(
                    id = R.string.map_viewport_supporting_default,
                    coordinateText(MapDefaults.BUSAN_CENTER),
                )
        }

    return MapViewportUiState(
        integrationState = integrationState,
        cameraTarget = cameraTarget,
        rendererSessionKey = uiState.rendererSessionKey,
        currentLocation = currentLocationMarker,
        selectedOriginCoordinate =
            viewportOrigin?.let { origin ->
                MapCoordinate(
                    latitude = origin.latitude,
                    longitude = origin.longitude,
                )
            },
        selectedOriginName = viewportOrigin?.name,
        selectedDestinationCoordinate =
            viewportDestination?.let { destination ->
                MapCoordinate(
                    latitude = destination.latitude,
                    longitude = destination.longitude,
                )
            },
        selectedDestinationName = viewportDestination?.name,
        markerOverlayState = uiState.markerOverlayState,
        overlayState =
            createMapMarkerViewportOverlayState(
                cameraTarget = cameraTarget,
                markerOverlayState = uiState.markerOverlayState,
                selectedMarkerId = uiState.selectedMarkerId,
                currentLocation = currentLocationMarker,
                currentLocationLabel =
                    currentLocationMarker?.let {
                        stringResource(id = R.string.navigation_map_marker_current)
                    },
            ),
        selectedMarkerId = uiState.selectedMarkerId,
        selectedMapPinCoordinate = uiState.selectedMapPinCoordinate,
        regionLabel = regionLabel,
        statusLabel = statusLabel,
        title = title,
        description = description,
        supportingText = supportingText,
    )
}

internal fun resolveCurrentLocationMarker(locationStatus: MapLocationStatus): MapCoordinate? =
    (locationStatus as? MapLocationStatus.Ready)?.location

@Composable
private fun selectedDestinationSummaryText(destination: PlaceDestination?): String {
    if (destination == null) {
        return stringResource(
            id = R.string.map_viewport_supporting_default,
            coordinateText(MapDefaults.BUSAN_CENTER),
        )
    }

    val coordinate =
        coordinateText(
            MapCoordinate(
                latitude = destination.latitude,
                longitude = destination.longitude,
            ),
        )

    val summary =
        if (destination.address.isNullOrBlank()) {
            stringResource(
                id = R.string.map_destination_summary_without_address,
                coordinate,
            )
        } else {
            stringResource(
                id = R.string.map_destination_summary_with_address,
                destination.address.orEmpty(),
                coordinate,
            )
        }

    return stringResource(
        id = R.string.map_viewport_supporting_selected,
        summary,
    )
}

@Composable
private fun locationSummaryText(
    location: MapCoordinate,
    accuracyMeters: Float?,
): String {
    val coordinate = coordinateText(location)
    val accuracy =
        accuracyMeters?.let { meters ->
            stringResource(
                id = R.string.map_location_accuracy_value,
                meters,
            )
        }

    return if (accuracy == null) {
        coordinate
    } else {
        stringResource(
            id = R.string.map_location_summary_with_accuracy,
            coordinate,
            accuracy,
        )
    }
}

@Composable
private fun coordinateText(location: MapCoordinate): String =
    stringResource(
        id = R.string.map_location_coordinate_value,
        location.latitude,
        location.longitude,
    )

@DrawableRes
private fun mapTapDetailPlaceIconRes(detail: MapTappedPlaceDetail): Int =
    recentDestinationIcon(detail.category)

@Composable
private fun mapTapDetailMetaLabel(
    detail: MapTappedPlaceDetail,
    locationStatus: MapLocationStatus,
): String {
    val categoryLabel =
        detail.providerCategory
            ?.takeIf { providerCategory -> providerCategory.isNotBlank() }
            ?: detail.category?.let { category -> placeCategoryFallbackLabel(category) }
            ?: mapTapDetailTypeLabel(detail.detailType)
    val distanceMeters =
        facilityDistanceMeters(
            coordinate = GeoCoordinate(latitude = detail.latitude, longitude = detail.longitude),
            locationStatus = locationStatus,
        )
    if (distanceMeters == null) {
        return categoryLabel
    }

    return "$categoryLabel / ${facilityDistanceValueLabel(distanceMeters)}"
}

@Composable
private fun mapTapDetailAddressLabel(detail: MapTappedPlaceDetail): String =
    detail.address
        .takeIf { address -> address.isNotBlank() }
        ?: stringResource(id = R.string.map_facility_detail_address_fallback)

private fun mapTapDetailAccessibilityLabels(
    detail: MapTappedPlaceDetail,
    selectedFilterCategories: Set<FacilityCategory>,
): List<String> {
    val selectedRawKeys = selectedFilterCategories.mapNotNull(::selectedFilterAccessibilityRawKey)
    val normalizedSelectedRawKeys = selectedRawKeys.map(String::lowercase).toSet()
    val orderedRawKeys =
        detail.accessibilityTags
            .distinctBy { rawKey -> rawKey.trim().lowercase() }
            .sortedWith(
                compareBy<String> { rawKey ->
                    if (rawKey.trim().lowercase() in normalizedSelectedRawKeys) 0 else 1
                },
            )

    return orderedRawKeys
        .mapNotNull(::recentDestinationTagLabel)
        .distinct()
        .take(MAX_FACILITY_DETAIL_ACCESSIBILITY_TAGS)
}

private fun MapTappedPlaceDetail.hasValidCoordinate(): Boolean =
    latitude.isFinite() &&
        longitude.isFinite() &&
        latitude in -90.0..90.0 &&
        longitude in -180.0..180.0

@Composable
private fun mapTapDetailTypeLabel(detailType: MapPlaceDetailType): String =
    when (detailType) {
        MapPlaceDetailType.INTERNAL_PLACE -> stringResource(id = R.string.map_facility_detail_type_internal_place)
        MapPlaceDetailType.EXTERNAL_POI -> stringResource(id = R.string.map_facility_detail_type_external_poi)
        MapPlaceDetailType.EXTERNAL_ADDRESS -> stringResource(id = R.string.map_facility_detail_type_external_address)
    }

@Composable
private fun placeCategoryFallbackLabel(category: PlaceCategory): String =
    when (category) {
        PlaceCategory.TOILET -> stringResource(id = R.string.map_filter_category_toilet)
        PlaceCategory.ELEVATOR -> stringResource(id = R.string.map_filter_category_elevator)
        PlaceCategory.CHARGING_STATION -> stringResource(id = R.string.map_filter_category_charging_station)
        PlaceCategory.FOOD_CAFE -> stringResource(id = R.string.map_facility_detail_category_food_cafe)
        PlaceCategory.TOURIST_SPOT -> stringResource(id = R.string.map_facility_detail_category_tourist_spot)
        PlaceCategory.ACCOMMODATION -> stringResource(id = R.string.map_facility_detail_category_accommodation)
        PlaceCategory.HEALTHCARE -> stringResource(id = R.string.map_facility_detail_category_healthcare)
        PlaceCategory.WELFARE -> stringResource(id = R.string.map_facility_detail_category_welfare)
        PlaceCategory.PUBLIC_OFFICE -> stringResource(id = R.string.map_facility_detail_category_public_office)
        PlaceCategory.BRAILLE_BLOCK -> stringResource(id = R.string.map_filter_category_braille_block)
        PlaceCategory.RESTAURANT -> stringResource(id = R.string.map_filter_category_restaurant)
        PlaceCategory.TOURIST_ATTRACTION -> stringResource(id = R.string.map_filter_category_tourist_attraction)
        PlaceCategory.OTHER -> stringResource(id = R.string.map_filter_category_other)
    }

@Composable
private fun facilityDetailCategoryLabel(category: FacilityCategory): String =
    when (category) {
        FacilityCategory.TOILET -> stringResource(id = R.string.map_filter_category_toilet)
        FacilityCategory.ELEVATOR -> stringResource(id = R.string.map_filter_category_elevator)
        FacilityCategory.CHARGING_STATION -> stringResource(id = R.string.map_filter_category_charging_station)
        FacilityCategory.FOOD_CAFE -> "식당·카페"
        FacilityCategory.TOURIST_SPOT -> "무장애 관광지"
        FacilityCategory.ACCOMMODATION -> "숙박"
        FacilityCategory.HEALTHCARE -> "병원"
        FacilityCategory.WELFARE -> "복지관"
        FacilityCategory.PUBLIC_OFFICE -> "관공서"
        FacilityCategory.BRAILLE_BLOCK -> stringResource(id = R.string.map_filter_category_braille_block)
        FacilityCategory.RESTAURANT -> stringResource(id = R.string.map_filter_category_restaurant)
        FacilityCategory.TOURIST_ATTRACTION -> stringResource(id = R.string.map_filter_category_tourist_attraction)
        FacilityCategory.OTHER -> stringResource(id = R.string.map_filter_category_other)
    }

@Composable
private fun facilityDetailMetaLabel(
    detail: FacilityDetailSeed,
    locationStatus: MapLocationStatus,
): String {
    val categoryLabel = facilityDetailCategoryLabel(detail.category)
    val distanceMeters = facilityDistanceMeters(detail.coordinate, locationStatus)
    if (distanceMeters == null) {
        return categoryLabel
    }

    return "$categoryLabel / ${facilityDistanceValueLabel(distanceMeters)}"
}

@Composable
private fun facilityDistanceValueLabel(distanceMeters: Int): String =
    if (distanceMeters < 1_000) {
        stringResource(
            id = R.string.map_facility_detail_distance_meters,
            normalizeDistanceMeters(distanceMeters),
        )
    } else {
        stringResource(
            id = R.string.map_facility_detail_distance_kilometers,
            distanceMeters / 1_000f,
        )
    }

@Composable
private fun facilityDetailAddressLabel(detail: FacilityDetailSeed): String =
    detail.address.takeIf { address -> address.isNotBlank() }
        ?: stringResource(id = R.string.map_facility_detail_address_fallback)

@Composable
private fun facilityDetailAccessibilityLabels(
    detail: FacilityDetailSeed,
    selectedFilterCategories: Set<FacilityCategory>,
): List<String> {
    val prioritizedTags =
        selectedFilterCategories
            .mapNotNull(::selectedFilterAccessibilityTag)
            .filter { tag -> tag in detail.accessibilityTags }

    // TODO: Keep the active filter tag visible while this sheet shows only three accessibility tags.
    return buildList {
        detail.brailleBlockType?.let { brailleBlockType ->
            add(brailleBlockTypeLabel(brailleBlockType))
        }
        addAll(
            (prioritizedTags + detail.accessibilityTags.sortedBy(::accessibilityTagDisplayPriority))
                .distinct()
                .map { tag ->
                    accessibilityTagLabel(tag)
                },
        )
    }.distinct().take(MAX_FACILITY_DETAIL_ACCESSIBILITY_TAGS)
}

private fun selectedFilterAccessibilityTag(category: FacilityCategory): AccessibilityTag? =
    when (category) {
        FacilityCategory.TOILET -> AccessibilityTag.ACCESSIBLE_TOILET
        FacilityCategory.ELEVATOR -> AccessibilityTag.ELEVATOR
        FacilityCategory.CHARGING_STATION -> AccessibilityTag.CHARGING_STATION
        else -> null
    }

private fun selectedFilterAccessibilityRawKey(category: FacilityCategory): String? =
    when (category) {
        FacilityCategory.TOILET -> "accessible-toilet"
        FacilityCategory.ELEVATOR -> "elevator"
        FacilityCategory.CHARGING_STATION -> "charging-station"
        else -> null
    }

@Composable
private fun accessibilityTagLabel(tag: AccessibilityTag): String =
    when (tag) {
        AccessibilityTag.RAMP -> stringResource(id = R.string.map_facility_detail_tag_ramp)
        AccessibilityTag.STEP_FREE_ENTRANCE ->
            stringResource(id = R.string.map_facility_detail_tag_step_free_entrance)

        AccessibilityTag.AUTO_DOOR -> stringResource(id = R.string.map_facility_detail_tag_auto_door)
        AccessibilityTag.WIDE_ENTRY -> stringResource(id = R.string.map_facility_detail_tag_wide_entry)
        AccessibilityTag.ACCESSIBLE_TOILET ->
            stringResource(id = R.string.map_facility_detail_tag_accessible_toilet)

        AccessibilityTag.ELEVATOR -> stringResource(id = R.string.map_facility_detail_tag_elevator)
        AccessibilityTag.WHEELCHAIR_TURNING_SPACE ->
            stringResource(id = R.string.map_facility_detail_tag_wheelchair_turning_space)

        AccessibilityTag.TABLE_SPACING -> stringResource(id = R.string.map_facility_detail_tag_table_spacing)
        AccessibilityTag.ACCESSIBLE_PARKING ->
            stringResource(id = R.string.map_facility_detail_tag_accessible_parking)
        AccessibilityTag.CHARGING_STATION ->
            stringResource(id = R.string.map_facility_detail_tag_charging_station)

        AccessibilityTag.GUIDANCE_FACILITY ->
            stringResource(id = R.string.map_facility_detail_tag_guidance_facility)

        AccessibilityTag.ACCESSIBLE_ROOM ->
            stringResource(id = R.string.map_facility_detail_tag_accessible_room)

        AccessibilityTag.LOW_HEIGHT_BUTTON ->
            stringResource(id = R.string.map_facility_detail_tag_low_height_button)

        AccessibilityTag.REST_AREA -> stringResource(id = R.string.map_facility_detail_tag_rest_area)
        AccessibilityTag.OPEN_24_HOURS -> stringResource(id = R.string.map_facility_detail_tag_open_24_hours)
    }

private fun accessibilityTagDisplayPriority(tag: AccessibilityTag): Int =
    when (tag) {
        AccessibilityTag.STEP_FREE_ENTRANCE -> 0
        AccessibilityTag.RAMP -> 1
        AccessibilityTag.AUTO_DOOR -> 2
        AccessibilityTag.WIDE_ENTRY -> 3
        AccessibilityTag.ELEVATOR -> 4
        AccessibilityTag.ACCESSIBLE_PARKING -> 5
        AccessibilityTag.CHARGING_STATION -> 6
        AccessibilityTag.ACCESSIBLE_TOILET -> 7
        AccessibilityTag.GUIDANCE_FACILITY -> 8
        AccessibilityTag.ACCESSIBLE_ROOM -> 9
        AccessibilityTag.WHEELCHAIR_TURNING_SPACE -> 10
        AccessibilityTag.TABLE_SPACING -> 11
        AccessibilityTag.LOW_HEIGHT_BUTTON -> 12
        AccessibilityTag.REST_AREA -> 13
        AccessibilityTag.OPEN_24_HOURS -> 14
    }

@Composable
private fun brailleBlockTypeLabel(type: BrailleBlockType): String =
    when (type) {
        BrailleBlockType.GUIDING_LINE -> stringResource(id = R.string.map_facility_detail_braille_guiding_line)
        BrailleBlockType.WARNING_SURFACE -> stringResource(id = R.string.map_facility_detail_braille_warning_surface)
        BrailleBlockType.CROSSWALK_APPROACH ->
            stringResource(id = R.string.map_facility_detail_braille_crosswalk_approach)
    }

private fun facilityDistanceMeters(
    coordinate: GeoCoordinate,
    locationStatus: MapLocationStatus,
): Int? =
    when (locationStatus) {
        is MapLocationStatus.Ready -> distanceMetersBetween(from = locationStatus.location, to = coordinate)
        MapLocationStatus.PermissionDenied -> null
        MapLocationStatus.Loading -> null
        is MapLocationStatus.Unavailable -> null
    }

private fun distanceMetersBetween(
    from: MapCoordinate,
    to: GeoCoordinate,
): Int {
    val latitudeDeltaRadians = (to.latitude - from.latitude) * DEGREES_TO_RADIANS
    val longitudeDeltaRadians = (to.longitude - from.longitude) * DEGREES_TO_RADIANS
    val fromLatitudeRadians = from.latitude * DEGREES_TO_RADIANS
    val toLatitudeRadians = to.latitude * DEGREES_TO_RADIANS

    val haversine =
        sin(latitudeDeltaRadians / 2).pow(2) +
            cos(fromLatitudeRadians) * cos(toLatitudeRadians) * sin(longitudeDeltaRadians / 2).pow(2)
    val centralAngle = 2 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
    return (EARTH_RADIUS_METERS * centralAngle).roundToInt()
}

private fun normalizeDistanceMeters(distanceMeters: Int): Int =
    when {
        distanceMeters < 100 -> distanceMeters
        distanceMeters < 1_000 -> ((distanceMeters + 5) / 10) * 10
        else -> distanceMeters
    }

private fun recentDestinationTagLabels(destination: RecentDestination): List<String> =
    destination.accessibilityTagKeys
        .mapNotNull(::recentDestinationTagLabel)
        .distinct()

private fun recentDestinationTagLabel(rawKey: String): String? =
    when (rawKey.trim().lowercase()) {
        "accessible-toilet" -> "장애인 화장실"
        "elevator" -> "엘리베이터"
        "accessible-parking" -> "장애인 주차 가능"
        "step-free-entrance" -> "단차 없음"
        "guidance-facility" -> "안내시설"
        "accessible-room" -> "객실 이용 가능"
        "ramp" -> "경사로"
        "auto-door" -> "출입 가능"
        "wide-entry" -> "출입 가능"
        "wheelchair-turning-space" -> "출입 가능"
        "table-spacing" -> "출입 가능"
        "rest-area" -> "안내시설"
        "braille-block" -> "안내시설"
        "crosswalk" -> "안내시설"
        "low-height-button" -> "안내시설"
        else -> null
    }

@DrawableRes
private fun facilityDetailPlaceIconRes(category: FacilityCategory): Int =
    when (category) {
        FacilityCategory.TOILET -> R.drawable.ic_user_wheelchair_compact
        FacilityCategory.ELEVATOR -> R.drawable.ic_place_elevator
        FacilityCategory.CHARGING_STATION -> R.drawable.ic_place_charging_station
        FacilityCategory.FOOD_CAFE -> R.drawable.ic_place_food_cafe
        FacilityCategory.TOURIST_SPOT -> R.drawable.ic_place_tourist_spot
        FacilityCategory.ACCOMMODATION -> R.drawable.ic_place_accommodation
        FacilityCategory.HEALTHCARE -> R.drawable.ic_place_healthcare
        FacilityCategory.WELFARE -> R.drawable.ic_place_welfare
        FacilityCategory.PUBLIC_OFFICE -> R.drawable.ic_place_public_office
        FacilityCategory.BRAILLE_BLOCK -> R.drawable.ic_route_tactile_blocks
        FacilityCategory.RESTAURANT -> R.drawable.ic_place_food_cafe
        FacilityCategory.TOURIST_ATTRACTION -> R.drawable.ic_place_tourist_spot
        FacilityCategory.OTHER -> R.drawable.ic_place_other
    }

@DrawableRes
private fun recentDestinationIcon(category: PlaceCategory?): Int =
    when (category) {
        PlaceCategory.TOILET -> R.drawable.ic_user_wheelchair_compact
        PlaceCategory.ELEVATOR -> R.drawable.ic_place_elevator
        PlaceCategory.CHARGING_STATION -> R.drawable.ic_place_charging_station
        PlaceCategory.FOOD_CAFE -> R.drawable.ic_place_food_cafe
        PlaceCategory.TOURIST_SPOT -> R.drawable.ic_place_tourist_spot
        PlaceCategory.ACCOMMODATION -> R.drawable.ic_place_accommodation
        PlaceCategory.HEALTHCARE -> R.drawable.ic_place_healthcare
        PlaceCategory.WELFARE -> R.drawable.ic_place_welfare
        PlaceCategory.PUBLIC_OFFICE -> R.drawable.ic_place_public_office
        PlaceCategory.BRAILLE_BLOCK -> R.drawable.ic_route_tactile_blocks
        PlaceCategory.RESTAURANT -> R.drawable.ic_place_food_cafe
        PlaceCategory.TOURIST_ATTRACTION -> R.drawable.ic_place_tourist_spot
        PlaceCategory.OTHER -> R.drawable.ic_place_other
        null -> R.drawable.ic_nav_facility
    }

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val DEGREES_TO_RADIANS = PI / 180.0
private const val MAX_FACILITY_DETAIL_ACCESSIBILITY_TAGS = 3
private val RouteSelectionOriginChipColor = Color(0xFFEAF8EF)
private val RouteSelectionOriginTextColor = Color(0xFF166534)
private val RouteSelectionDestinationChipColor = Color(0xFFFFEEF0)
private val RouteSelectionDestinationTextColor = Color(0xFFB4232F)
