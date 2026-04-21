package com.ssafy.e102.eumgil.feature.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.ssafy.e102.eumgil.core.model.AccessibilityTag
import com.ssafy.e102.eumgil.core.model.BrailleBlockType
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.feature.map.component.FacilityDetailBottomSheetShell
import com.ssafy.e102.eumgil.feature.map.component.FacilityDetailBottomSheetShellState
import com.ssafy.e102.eumgil.feature.map.component.MapCategoryFilterBar
import com.ssafy.e102.eumgil.feature.map.component.MapIntegrationState
import com.ssafy.e102.eumgil.feature.map.component.MapShellScaffold
import com.ssafy.e102.eumgil.feature.map.component.MapTopSearchBar
import com.ssafy.e102.eumgil.feature.map.component.MapViewport
import com.ssafy.e102.eumgil.feature.map.component.MapViewportUiState
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
    onAction: (MapUiAction) -> Unit,
    onNavigateToSavedRoutes: () -> Unit,
    onNavigateToMyPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewportState = mapViewportState(uiState = uiState)
    val locationPanelState = mapLocationPanelState(uiState = uiState)
    val searchBarState = mapSearchBarState(uiState = uiState)
    val facilityDetailSheetUiState = mapFacilityDetailBottomSheetState(uiState = uiState)

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
        bottomOverlay = {
            FacilityDetailBottomSheetShell(
                state = facilityDetailSheetUiState.toShellState(),
                onDismiss = { onAction(MapUiAction.FacilityDetailDismissed) },
                modifier = Modifier.fillMaxSize(),
                detailContent = {
                    FacilityDetailAccessibilityTagSection(
                        title = stringResource(id = R.string.map_facility_detail_accessibility_section_title),
                        tags = facilityDetailSheetUiState.accessibilityTags,
                    )

                    FacilityDetailSlotCard(
                        title = stringResource(id = R.string.map_facility_detail_info_section_title),
                        description = facilityDetailSheetUiState.guideMessage,
                    )
                },
                actionContent = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                    ) {
                        Text(
                            text = stringResource(id = R.string.map_facility_detail_action_section_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Button(
                            onClick = { onAction(MapUiAction.FacilityRouteEntryClicked) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(id = R.string.map_facility_detail_route_entry_action))
                        }
                        Text(
                            text = stringResource(id = R.string.map_facility_detail_action_supporting_route_setting),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
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

@Immutable
private data class MapFacilityDetailSheetUiState(
    val isVisible: Boolean,
    val categoryLabel: String,
    val distanceLabel: String,
    val title: String,
    val address: String,
    val accessibilityTags: List<String>,
    val guideMessage: String,
) {
    fun toShellState(): FacilityDetailBottomSheetShellState =
        FacilityDetailBottomSheetShellState(
            isVisible = isVisible,
            categoryLabel = categoryLabel,
            distanceLabel = distanceLabel,
            title = title,
            address = address,
        )
}

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
private fun FacilityDetailSlotCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.medium),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacilityDetailAccessibilityTagSection(
    title: String,
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.medium),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (tags.isEmpty()) {
                FacilityDetailTagChip(
                    label = stringResource(id = R.string.map_facility_detail_accessibility_empty),
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                    verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                ) {
                    tags.forEach { label ->
                        FacilityDetailTagChip(label = label)
                    }
                }
            }
        }
    }
}

@Composable
private fun FacilityDetailTagChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EumRadius.full),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = label,
            modifier =
                Modifier.padding(
                    horizontal = EumSpacing.small,
                    vertical = EumSpacing.xSmall,
                ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
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
private fun mapFacilityDetailBottomSheetState(uiState: MapUiState): MapFacilityDetailSheetUiState {
    val detail = uiState.facilityDetailSheetState.detail
    return if (detail == null) {
        MapFacilityDetailSheetUiState(
            isVisible = false,
            categoryLabel = "",
            distanceLabel = "",
            title = "",
            address = "",
            accessibilityTags = emptyList(),
            guideMessage = "",
        )
    } else {
        MapFacilityDetailSheetUiState(
            isVisible = uiState.facilityDetailSheetState.isVisible,
            categoryLabel = facilityDetailCategoryLabel(detail.category),
            distanceLabel = facilityDetailDistanceBadgeLabel(detail = detail, locationStatus = uiState.locationStatus),
            title = detail.name,
            address = facilityDetailAddressLabel(detail),
            accessibilityTags = facilityDetailAccessibilityLabels(detail),
            guideMessage = facilityDetailGuideMessage(detail),
        )
    }
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

@Composable
private fun facilityDetailCategoryLabel(category: FacilityCategory): String =
    when (category) {
        FacilityCategory.RESTAURANT -> stringResource(id = R.string.map_filter_category_restaurant)
        FacilityCategory.TOURIST_ATTRACTION -> stringResource(id = R.string.map_filter_category_tourist_attraction)
        FacilityCategory.TOILET -> stringResource(id = R.string.map_filter_category_toilet)
        FacilityCategory.ELEVATOR -> stringResource(id = R.string.map_filter_category_elevator)
        FacilityCategory.CHARGING_STATION -> stringResource(id = R.string.map_filter_category_charging_station)
        FacilityCategory.BRAILLE_BLOCK -> stringResource(id = R.string.map_filter_category_braille_block)
        FacilityCategory.OTHER -> stringResource(id = R.string.map_filter_category_other)
    }

@Composable
private fun facilityDetailDistanceBadgeLabel(
    detail: FacilityDetailSeed,
    locationStatus: MapLocationStatus,
): String {
    val distanceMeters = facilityDistanceMeters(detail.coordinate, locationStatus)
    if (distanceMeters == null) {
        return stringResource(id = R.string.map_facility_detail_distance_badge_unknown)
    }

    return stringResource(
        id = R.string.map_facility_detail_distance_badge,
        facilityDistanceValueLabel(distanceMeters),
    )
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
private fun facilityDetailAccessibilityLabels(detail: FacilityDetailSeed): List<String> =
    buildList {
        detail.brailleBlockType?.let { brailleBlockType ->
            add(brailleBlockTypeLabel(brailleBlockType))
        }
        addAll(
            detail.accessibilityTags.map { tag ->
                accessibilityTagLabel(tag)
            },
        )
    }.distinct()

@Composable
private fun facilityDetailGuideMessage(detail: FacilityDetailSeed): String =
    detail.description
        ?.trim()
        ?.takeIf { description -> description.isNotEmpty() }
        ?: defaultFacilityGuideMessage(detail)

@Composable
private fun defaultFacilityGuideMessage(detail: FacilityDetailSeed): String =
    when (detail.category) {
        FacilityCategory.RESTAURANT -> stringResource(id = R.string.map_facility_detail_guide_fallback_restaurant)
        FacilityCategory.TOURIST_ATTRACTION ->
            stringResource(id = R.string.map_facility_detail_guide_fallback_tourist_attraction)

        FacilityCategory.TOILET -> stringResource(id = R.string.map_facility_detail_guide_fallback_toilet)
        FacilityCategory.ELEVATOR -> stringResource(id = R.string.map_facility_detail_guide_fallback_elevator)
        FacilityCategory.CHARGING_STATION ->
            stringResource(id = R.string.map_facility_detail_guide_fallback_charging_station)

        FacilityCategory.BRAILLE_BLOCK ->
            when (detail.brailleBlockType) {
                BrailleBlockType.GUIDING_LINE ->
                    stringResource(id = R.string.map_facility_detail_guide_fallback_braille_guiding_line)

                BrailleBlockType.WARNING_SURFACE ->
                    stringResource(id = R.string.map_facility_detail_guide_fallback_braille_warning_surface)

                BrailleBlockType.CROSSWALK_APPROACH ->
                    stringResource(id = R.string.map_facility_detail_guide_fallback_braille_crosswalk_approach)

                null -> stringResource(id = R.string.map_facility_detail_guide_fallback_braille_generic)
            }

        FacilityCategory.OTHER -> stringResource(id = R.string.map_facility_detail_guide_fallback_other)
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

        AccessibilityTag.LOW_HEIGHT_BUTTON ->
            stringResource(id = R.string.map_facility_detail_tag_low_height_button)

        AccessibilityTag.REST_AREA -> stringResource(id = R.string.map_facility_detail_tag_rest_area)
        AccessibilityTag.OPEN_24_HOURS -> stringResource(id = R.string.map_facility_detail_tag_open_24_hours)
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

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val DEGREES_TO_RADIANS = PI / 180.0
