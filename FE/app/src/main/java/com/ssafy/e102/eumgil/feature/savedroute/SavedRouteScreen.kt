package com.ssafy.e102.eumgil.feature.savedroute

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

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
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            item {
                SavedRouteHeader(uiState = uiState)
            }

            when (uiState.screenState) {
                SavedRouteScreenState.LOADING ->
                    item {
                        SavedRouteStateCard(
                            title = stringResource(id = R.string.saved_route_loading_title),
                            description = stringResource(id = R.string.saved_route_loading_description),
                            isLoading = true,
                        )
                    }

                SavedRouteScreenState.EMPTY ->
                    item {
                        SavedRouteStateCard(
                            title = stringResource(id = R.string.saved_route_empty_title),
                            description = stringResource(id = R.string.saved_route_empty_description),
                            primaryActionLabel = stringResource(id = R.string.saved_route_explore_map),
                            onPrimaryActionClick = { onAction(SavedRouteUiAction.ExploreMapClicked) },
                        )
                    }

                SavedRouteScreenState.ERROR ->
                    item {
                        SavedRouteStateCard(
                            title = stringResource(id = R.string.saved_route_error_title),
                            description =
                                uiState.errorMessage
                                    ?: stringResource(id = R.string.saved_route_error_description),
                            primaryActionLabel = stringResource(id = R.string.saved_route_retry),
                            onPrimaryActionClick = { onAction(SavedRouteUiAction.RetryClicked) },
                            secondaryActionLabel = stringResource(id = R.string.saved_route_explore_map),
                            onSecondaryActionClick = { onAction(SavedRouteUiAction.ExploreMapClicked) },
                            isError = true,
                        )
                    }

                SavedRouteScreenState.CONTENT -> {
                    uiState.errorMessage?.let { message ->
                        item {
                            SavedRouteInlineMessage(message = message)
                        }
                    }
                    items(
                        items = uiState.places,
                        key = { place -> place.placeId },
                    ) { place ->
                        SavedPlaceListItem(
                            place = place,
                            onPlaceClick = {
                                onAction(SavedRouteUiAction.PlaceClicked(placeId = place.placeId))
                            },
                            onRouteGuideClick = {
                                onAction(SavedRouteUiAction.RouteGuideClicked(placeId = place.placeId))
                            },
                            onRemoveClick = {
                                onAction(SavedRouteUiAction.BookmarkRemoveClicked(placeId = place.placeId))
                            },
                        )
                    }
                }
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
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SavedRouteHeader(uiState: SavedRouteUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
    ) {
        Text(
            text = stringResource(id = R.string.saved_route_header_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text =
                stringResource(
                    id = R.string.saved_route_header_description,
                    uiState.places.size,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SavedRouteStateCard(
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            horizontalAlignment = Alignment.Start,
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
            SavedRouteStateActions(
                primaryActionLabel = primaryActionLabel,
                onPrimaryActionClick = onPrimaryActionClick,
                secondaryActionLabel = secondaryActionLabel,
                onSecondaryActionClick = onSecondaryActionClick,
            )
        }
    }
}

@Composable
private fun SavedRouteStateActions(
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
    onRouteGuideClick: () -> Unit,
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
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
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
                    text =
                        place.address
                            ?: stringResource(id = R.string.saved_route_address_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onPlaceClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.saved_route_view_on_map))
                }
                Button(
                    onClick = onRouteGuideClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.saved_route_start_route))
                }
                OutlinedButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.weight(0.8f),
                ) {
                    Text(text = stringResource(id = R.string.saved_route_remove_bookmark))
                }
            }
        }
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
