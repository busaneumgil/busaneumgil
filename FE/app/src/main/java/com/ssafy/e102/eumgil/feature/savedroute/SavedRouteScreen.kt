package com.ssafy.e102.eumgil.feature.savedroute

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
            SavedRouteTopBar()
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
                        onAction = onAction,
                        modifier = Modifier.weight(1f),
                    )
                SavedBookmarkTab.ROUTE ->
                    SavedRouteBookmarkContent(
                        content = uiState.routeContent,
                        onAction = onAction,
                        modifier = Modifier.weight(1f),
                    )
            }
        }
    }
}

@Composable
private fun SavedRouteTopBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
        tonalElevation = 2.dp,
    ) {
        Text(
            text = stringResource(id = R.string.saved_route_screen_title),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.small),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
            modifier = Modifier.fillMaxWidth().padding(4.dp),
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
                        onPlaceClick = {
                            onAction(SavedRouteUiAction.PlaceClicked(placeId = place.placeId))
                        },
                        onGuideClick = {
                            onAction(SavedRouteUiAction.PlaceRouteGuideClicked(placeId = place.placeId))
                        },
                        onRemoveClick = {
                            onAction(SavedRouteUiAction.PlaceRemoveClicked(placeId = place.placeId))
                        },
                    )
                }
            }
    }
}

@Composable
private fun SavedRouteBookmarkContent(
    content: SavedRouteBookmarkContentUiState,
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
                        onGuideClick = {
                            onAction(SavedRouteUiAction.RouteGuideClicked(bookmarkId = routeBookmark.bookmarkId))
                        },
                        onRemoveClick = {
                            onAction(SavedRouteUiAction.RouteRemoveClicked(bookmarkId = routeBookmark.bookmarkId))
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
    onPlaceClick: () -> Unit,
    onGuideClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    val accessibilityDescription =
        stringResource(
            id = R.string.saved_route_place_a11y_description,
            place.name,
            place.address ?: stringResource(id = R.string.saved_route_address_empty),
        )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        .clickable(
                            role = Role.Button,
                            onClick = onPlaceClick,
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
            SavedBookmarkActionColumn(
                onGuideClick = onGuideClick,
                onRemoveClick = onRemoveClick,
            )
        }
    }
}

@Composable
private fun SavedRouteBookmarkListItem(
    routeBookmark: SavedRouteBookmarkUiModel,
    onGuideClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            verticalAlignment = Alignment.Top,
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
            SavedBookmarkActionColumn(
                onGuideClick = onGuideClick,
                onRemoveClick = onRemoveClick,
            )
        }
    }
}

@Composable
private fun SavedBookmarkActionColumn(
    onGuideClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
    ) {
        OutlinedButton(onClick = onGuideClick) {
            Text(text = stringResource(id = R.string.saved_route_start_route))
        }
        TextButton(onClick = onRemoveClick) {
            Text(text = stringResource(id = R.string.saved_route_remove_bookmark))
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
