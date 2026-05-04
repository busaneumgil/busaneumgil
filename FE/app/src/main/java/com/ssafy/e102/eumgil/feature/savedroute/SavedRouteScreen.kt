package com.ssafy.e102.eumgil.feature.savedroute

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.RouteOption
import java.util.Locale

@Composable
fun SavedRouteScreen(
    uiState: SavedRouteUiState,
    onAction: (SavedRouteUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SavedRouteTopBar(
                isEditMode = uiState.isEditMode,
                isActionEnabled =
                    if (uiState.isEditMode) {
                        !uiState.isApplyingEditChanges
                    } else {
                        uiState.placeContent.places.isNotEmpty() ||
                            uiState.routeContent.routes.isNotEmpty()
                    },
                onActionClick = {
                    onAction(
                        if (uiState.isEditMode) {
                            SavedRouteUiAction.EditDoneClicked
                        } else {
                            SavedRouteUiAction.EditClicked
                        },
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            SavedBookmarkTabRow(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab -> onAction(SavedRouteUiAction.TabSelected(tab)) },
            )

            when (uiState.selectedTab) {
                SavedBookmarkTab.PLACE ->
                    SavedPlaceContent(
                        content = uiState.placeContent,
                        isEditMode = uiState.isEditMode,
                        isActionEnabled = !uiState.isApplyingEditChanges,
                        pendingRemovalIds = uiState.pendingPlaceRemovalIds,
                        onAction = onAction,
                        modifier = Modifier.weight(1f),
                    )
                SavedBookmarkTab.ROUTE ->
                    SavedRouteBookmarkContent(
                        content = uiState.routeContent,
                        isEditMode = uiState.isEditMode,
                        isActionEnabled = !uiState.isApplyingEditChanges,
                        pendingRemovalIds = uiState.pendingRouteRemovalIds,
                        onAction = onAction,
                        modifier = Modifier.weight(1f),
                    )
            }
        }
    }
}

@Composable
private fun SavedRouteTopBar(
    isEditMode: Boolean,
    isActionEnabled: Boolean,
    onActionClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.route_saved_route),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(
                onClick = onActionClick,
                enabled = isActionEnabled,
                modifier = Modifier.align(Alignment.CenterEnd),
                contentPadding = PaddingValues(horizontal = EumSpacing.small, vertical = 6.dp),
            ) {
                Text(
                    text =
                        stringResource(
                            id =
                                if (isEditMode) {
                                    R.string.saved_route_done
                                } else {
                                    R.string.saved_route_edit
                                },
                        ),
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (isActionEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                        },
                )
            }
        }
    }
}

@Composable
private fun SavedBookmarkTabRow(
    selectedTab: SavedBookmarkTab,
    onTabSelected: (SavedBookmarkTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.full),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SavedBookmarkTabButton(
                label = stringResource(id = R.string.saved_route_tab_place),
                selected = selectedTab == SavedBookmarkTab.PLACE,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(SavedBookmarkTab.PLACE) },
            )
            SavedBookmarkTabButton(
                label = stringResource(id = R.string.saved_route_tab_route),
                selected = selectedTab == SavedBookmarkTab.ROUTE,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(SavedBookmarkTab.ROUTE) },
            )
        }
    }
}

@Composable
private fun SavedBookmarkTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(EumRadius.full))
                .clickable(
                    role = Role.Tab,
                    onClick = onClick,
                ),
        shape = RoundedCornerShape(EumRadius.full),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = EumSpacing.small),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

@Composable
private fun SavedPlaceContent(
    content: SavedPlaceContentUiState,
    isEditMode: Boolean,
    isActionEnabled: Boolean,
    pendingRemovalIds: Set<String>,
    onAction: (SavedRouteUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (content.screenState) {
        SavedBookmarkContentState.LOADING ->
            SavedBookmarkStateCard(
                title = stringResource(id = R.string.saved_route_place_loading_title),
                description = stringResource(id = R.string.saved_route_place_loading_description),
                isLoading = true,
                modifier = modifier.fillMaxWidth(),
            )
        SavedBookmarkContentState.EMPTY ->
            SavedBookmarkStateCard(
                title = stringResource(id = R.string.saved_route_place_empty_title),
                description = stringResource(id = R.string.saved_route_place_empty_description),
                primaryActionLabel = stringResource(id = R.string.saved_route_explore_map),
                onPrimaryActionClick = { onAction(SavedRouteUiAction.ExploreMapClicked) },
                modifier = modifier.fillMaxWidth(),
            )
        SavedBookmarkContentState.ERROR ->
            SavedBookmarkStateCard(
                title = stringResource(id = R.string.saved_route_place_error_title),
                description = content.errorMessage ?: stringResource(id = R.string.saved_route_error_description),
                primaryActionLabel = stringResource(id = R.string.saved_route_retry),
                onPrimaryActionClick = { onAction(SavedRouteUiAction.RetryClicked) },
                secondaryActionLabel = stringResource(id = R.string.saved_route_explore_map),
                onSecondaryActionClick = { onAction(SavedRouteUiAction.ExploreMapClicked) },
                isError = true,
                modifier = modifier.fillMaxWidth(),
            )
        SavedBookmarkContentState.CONTENT ->
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                content.errorMessage?.let { message ->
                    item {
                        SavedRouteInlineMessage(message = message)
                    }
                }
                items(
                    items = content.places,
                    key = SavedPlaceUiModel::placeId,
                ) { place ->
                    SavedPlaceListItem(
                        place = place,
                        isEditMode = isEditMode,
                        isPendingRemoval = place.placeId in pendingRemovalIds,
                        isActionEnabled = isActionEnabled,
                        onPlaceClick =
                            if (isEditMode) {
                                null
                            } else {
                                {
                                    onAction(SavedRouteUiAction.PlaceClicked(placeId = place.placeId))
                                }
                            },
                        onPrimaryActionClick = {
                            onAction(
                                if (isEditMode) {
                                    SavedRouteUiAction.PlaceDeleteClicked(placeId = place.placeId)
                                } else {
                                    SavedRouteUiAction.PlaceRouteGuideClicked(placeId = place.placeId)
                                },
                            )
                        },
                    )
                }
            }
    }
}

@Composable
private fun SavedRouteBookmarkContent(
    content: SavedRouteBookmarkContentUiState,
    isEditMode: Boolean,
    isActionEnabled: Boolean,
    pendingRemovalIds: Set<String>,
    onAction: (SavedRouteUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (content.screenState) {
        SavedBookmarkContentState.LOADING ->
            SavedBookmarkStateCard(
                title = stringResource(id = R.string.saved_route_route_loading_title),
                description = stringResource(id = R.string.saved_route_route_loading_description),
                isLoading = true,
                modifier = modifier.fillMaxWidth(),
            )
        SavedBookmarkContentState.EMPTY ->
            SavedBookmarkStateCard(
                title = stringResource(id = R.string.saved_route_route_empty_title),
                description = stringResource(id = R.string.saved_route_route_empty_description),
                primaryActionLabel = stringResource(id = R.string.saved_route_explore_map),
                onPrimaryActionClick = { onAction(SavedRouteUiAction.ExploreMapClicked) },
                modifier = modifier.fillMaxWidth(),
            )
        SavedBookmarkContentState.ERROR ->
            SavedBookmarkStateCard(
                title = stringResource(id = R.string.saved_route_route_error_title),
                description = content.errorMessage ?: stringResource(id = R.string.saved_route_route_error_description),
                primaryActionLabel = stringResource(id = R.string.saved_route_retry),
                onPrimaryActionClick = { onAction(SavedRouteUiAction.RetryClicked) },
                secondaryActionLabel = stringResource(id = R.string.saved_route_explore_map),
                onSecondaryActionClick = { onAction(SavedRouteUiAction.ExploreMapClicked) },
                isError = true,
                modifier = modifier.fillMaxWidth(),
            )
        SavedBookmarkContentState.CONTENT ->
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                content.errorMessage?.let { message ->
                    item {
                        SavedRouteInlineMessage(message = message)
                    }
                }
                items(
                    items = content.routes,
                    key = SavedRouteBookmarkUiModel::bookmarkId,
                ) { routeBookmark ->
                    SavedRouteBookmarkListItem(
                        routeBookmark = routeBookmark,
                        isEditMode = isEditMode,
                        isPendingRemoval = routeBookmark.bookmarkId in pendingRemovalIds,
                        isActionEnabled = isActionEnabled,
                        onPrimaryActionClick = {
                            onAction(
                                if (isEditMode) {
                                    SavedRouteUiAction.RouteDeleteClicked(bookmarkId = routeBookmark.bookmarkId)
                                } else {
                                    SavedRouteUiAction.RouteGuideClicked(bookmarkId = routeBookmark.bookmarkId)
                                },
                            )
                        },
                    )
                }
            }
    }
}

@Composable
private fun SavedBookmarkStateCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    onPrimaryActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
    isError: Boolean = false,
) {
    val borderColor =
        if (isError) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.36f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            SavedBookmarkStateActions(
                primaryActionLabel = primaryActionLabel,
                onPrimaryActionClick = onPrimaryActionClick,
                secondaryActionLabel = secondaryActionLabel,
                onSecondaryActionClick = onSecondaryActionClick,
            )
        }
    }
}

@Composable
private fun SavedBookmarkStateActions(
    primaryActionLabel: String?,
    onPrimaryActionClick: (() -> Unit)?,
    secondaryActionLabel: String?,
    onSecondaryActionClick: (() -> Unit)?,
) {
    if (primaryActionLabel == null || onPrimaryActionClick == null) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Button(
            onClick = onPrimaryActionClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(text = primaryActionLabel)
        }
        if (secondaryActionLabel != null && onSecondaryActionClick != null) {
            OutlinedButton(
                onClick = onSecondaryActionClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = secondaryActionLabel)
            }
        }
    }
}

@Composable
private fun SavedRouteInlineMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.medium),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.34f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(EumSpacing.medium),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun SavedPlaceListItem(
    place: SavedPlaceUiModel,
    isEditMode: Boolean,
    isPendingRemoval: Boolean,
    isActionEnabled: Boolean,
    onPlaceClick: (() -> Unit)?,
    onPrimaryActionClick: () -> Unit,
) {
    val accessibilityDescription =
        stringResource(
            id = R.string.saved_route_place_a11y_description,
            place.name,
            place.address ?: stringResource(id = R.string.saved_route_address_empty),
        )
    val borderColor =
        if (isPendingRemoval) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.40f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    val containerColor =
        if (isPendingRemoval) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surface
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .then(
                            if (onPlaceClick != null) {
                                Modifier.clickable(
                                    role = Role.Button,
                                    onClick = onPlaceClick,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .semantics {
                            contentDescription = accessibilityDescription
                        },
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = savedPlaceCategoryLabel(category = place.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = place.address ?: stringResource(id = R.string.saved_route_address_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SavedBookmarkPrimaryActionButton(
                isEditMode = isEditMode,
                enabled = isActionEnabled,
                onClick = onPrimaryActionClick,
            )
        }
    }
}

@Composable
private fun SavedRouteBookmarkListItem(
    routeBookmark: SavedRouteBookmarkUiModel,
    isEditMode: Boolean,
    isPendingRemoval: Boolean,
    isActionEnabled: Boolean,
    onPrimaryActionClick: () -> Unit,
) {
    val borderColor =
        if (isPendingRemoval) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.40f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    val containerColor =
        if (isPendingRemoval) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surface
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SavedRoutePathDecoration()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = routeBookmark.routeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        stringResource(
                            id = R.string.saved_route_route_summary,
                            routeBookmark.startLabel,
                            routeBookmark.endLabel,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                savedRouteMetaLabel(routeBookmark)?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                ) {
                    SavedRouteTagChip(label = routeOptionLabel(routeBookmark.routeOption))
                }
            }
            SavedBookmarkPrimaryActionButton(
                isEditMode = isEditMode,
                enabled = isActionEnabled,
                onClick = onPrimaryActionClick,
            )
        }
    }
}

@Composable
private fun SavedBookmarkPrimaryActionButton(
    isEditMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(EumRadius.full)
    if (isEditMode) {
        Button(
            onClick = onClick,
            modifier =
                modifier.heightIn(min = 42.dp),
            enabled = enabled,
            shape = shape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.40f),
                    disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.78f),
                ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(
                text = stringResource(id = R.string.saved_route_remove_bookmark),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier =
                modifier.heightIn(min = 42.dp),
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.56f),
                ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_route_start_navigation),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(id = R.string.saved_route_start_route),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SavedRoutePathDecoration() {
    Column(
        modifier = Modifier.padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            modifier =
                Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
        )
    }
}

@Composable
private fun SavedRouteTagChip(label: String) {
    Surface(
        shape = RoundedCornerShape(EumRadius.full),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = EumSpacing.small, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun savedPlaceCategoryLabel(category: String?): String =
    when (category) {
        "RESTAURANT" -> stringResource(id = R.string.map_filter_category_restaurant)
        "TOURIST_ATTRACTION" -> stringResource(id = R.string.map_filter_category_tourist_attraction)
        "TOILET" -> stringResource(id = R.string.map_filter_category_toilet)
        "ELEVATOR" -> stringResource(id = R.string.map_filter_category_elevator)
        "CHARGING_STATION" -> stringResource(id = R.string.map_filter_category_charging_station)
        "BRAILLE_BLOCK" -> stringResource(id = R.string.map_filter_category_braille_block)
        else -> stringResource(id = R.string.map_filter_category_other)
    }

@Composable
private fun routeOptionLabel(routeOption: RouteOption): String =
    stringResource(
        id =
            when (routeOption) {
                RouteOption.SAFE -> R.string.route_setting_option_safe_title
                RouteOption.SHORTEST -> R.string.route_setting_option_shortest_title
            },
    )

@Composable
private fun savedRouteMetaLabel(routeBookmark: SavedRouteBookmarkUiModel): String? {
    val metaParts =
        buildList {
            routeBookmark.distanceMeters?.let { distanceMeters ->
                add(
                    stringResource(
                        id = R.string.saved_route_meta_distance,
                        distanceMeters.toSavedRouteDistanceLabel(),
                    ),
                )
            }
            routeBookmark.durationMinutes?.let { durationMinutes ->
                add(
                    stringResource(
                        id = R.string.saved_route_meta_duration,
                        durationMinutes.toSavedRouteDurationLabel(),
                    ),
                )
            }
        }
    return metaParts.takeIf(List<String>::isNotEmpty)?.joinToString(separator = " · ")
}

private fun Int.toSavedRouteDistanceLabel(): String =
    if (this < 1_000) {
        "${this}m"
    } else {
        String.format(Locale.US, "%.1fkm", this / 1_000f)
    }

private fun Int.toSavedRouteDurationLabel(): String = "${this}분"
