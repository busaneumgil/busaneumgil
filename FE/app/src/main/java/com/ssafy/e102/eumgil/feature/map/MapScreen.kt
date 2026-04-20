package com.ssafy.e102.eumgil.feature.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.feature.map.component.MapCategoryFilterBar
import com.ssafy.e102.eumgil.feature.map.component.MapIntegrationState
import com.ssafy.e102.eumgil.feature.map.component.MapShellScaffold
import com.ssafy.e102.eumgil.feature.map.component.MapTopSearchBar
import com.ssafy.e102.eumgil.feature.map.component.MapViewport
import com.ssafy.e102.eumgil.feature.map.component.MapViewportUiState
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapDefaults

@Suppress("UNUSED_PARAMETER")
@Composable
fun MapScreen(
    uiState: MapUiState,
    onAction: (MapUiAction) -> Unit,
    onNavigateToSavedRoutes: () -> Unit,
    onNavigateToMyPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewportState = mapViewportState(uiState = uiState)
    val locationPanelState = mapLocationPanelState(uiState = uiState)
    val searchBarState = mapSearchBarState(uiState = uiState)

    MapShellScaffold(
        modifier = modifier,
        mapContent = {
            MapViewport(
                state = viewportState,
                onMarkerClick = { markerId ->
                    onAction(MapUiAction.MarkerTapped(markerId))
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
                    hint = searchBarState.hint,
                    actionLabel = searchBarState.actionLabel,
                    accessibilityLabel = searchBarState.accessibilityLabel,
                    onClick = { onAction(MapUiAction.SearchEntryClicked) },
                )

                MapCategoryFilterBar(
                    state = uiState.markerFilterState,
                    onReset = { onAction(MapUiAction.MarkerCategoryFilterReset) },
                    onCategoryToggle = { category ->
                        onAction(MapUiAction.MarkerCategoryFilterToggled(category))
                    },
                )

                MapLocationStatusCard(
                    state = locationPanelState,
                    onActionClick = { onAction(MapUiAction.LocationActionClicked) },
                )
            }
        },
    )
}

@Immutable
private data class MapSearchBarState(
    val title: String,
    val hint: String,
    val actionLabel: String,
    val accessibilityLabel: String,
)

@Immutable
private data class MapLocationPanelState(
    val badgeLabel: String,
    val title: String,
    val description: String,
    val supportingText: String,
    val actionLabel: String,
    val isActionEnabled: Boolean,
    val isPrimaryAction: Boolean,
    val isCriticalState: Boolean,
)

@Composable
private fun MapLocationStatusCard(
    state: MapLocationPanelState,
    onActionClick: () -> Unit,
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
        modifier = Modifier.fillMaxWidth(),
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
                    Text(text = state.actionLabel)
                }
            } else {
                OutlinedButton(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isActionEnabled,
                ) {
                    Text(text = state.actionLabel)
                }
            }
        }
    }
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

            MapLocationPanelState(
                badgeLabel = stringResource(id = R.string.map_location_status_unavailable_badge),
                title = stringResource(id = titleRes),
                description = stringResource(id = descriptionRes),
                supportingText = stringResource(id = R.string.map_location_status_unavailable_supporting),
                actionLabel =
                    stringResource(
                        id =
                            if (isRetryEnabled) {
                                R.string.map_location_action_retry
                            } else {
                                R.string.map_location_action_disabled
                            },
                    ),
                isActionEnabled = isRetryEnabled,
                isPrimaryAction = false,
                isCriticalState = true,
            )
        }
    }
}

@Composable
private fun mapSearchBarState(uiState: MapUiState): MapSearchBarState {
    val selectedDestination = uiState.selectedDestination

    if (selectedDestination == null) {
        return MapSearchBarState(
            title = stringResource(id = R.string.map_shell_search_title),
            hint = stringResource(id = R.string.map_shell_search_hint),
            actionLabel = stringResource(id = R.string.map_shell_search_action),
            accessibilityLabel = stringResource(id = R.string.map_shell_search_a11y_label),
        )
    }

    return MapSearchBarState(
        title = selectedDestination.name,
        hint =
            selectedDestination.address
                ?: stringResource(id = R.string.map_shell_search_hint_selected_fallback),
        actionLabel = stringResource(id = R.string.map_shell_search_action_selected),
        accessibilityLabel =
            if (selectedDestination.address.isNullOrBlank()) {
                stringResource(
                    id = R.string.map_shell_search_a11y_label_selected_without_address,
                    selectedDestination.name,
                )
            } else {
                stringResource(
                    id = R.string.map_shell_search_a11y_label_selected_with_address,
                    selectedDestination.name,
                    selectedDestination.address.orEmpty(),
                )
            },
    )
}

@Composable
private fun mapViewportState(uiState: MapUiState): MapViewportUiState {
    val cameraTarget = uiState.cameraTarget
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
                    uiState.selectedDestination?.name
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
                selectedDestinationSummaryText(destination = uiState.selectedDestination)

            MapCameraSource.DEFAULT_BUSAN ->
                stringResource(
                    id = R.string.map_viewport_supporting_default,
                    coordinateText(MapDefaults.BUSAN_CENTER),
                )
        }

    return MapViewportUiState(
        integrationState = MapIntegrationState.Unbound,
        cameraTarget = cameraTarget,
        markerOverlayState = uiState.markerOverlayState,
        selectedMarkerId = uiState.selectedMarkerId,
        regionLabel = regionLabel,
        statusLabel = statusLabel,
        title = title,
        description = description,
        supportingText = supportingText,
    )
}

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
