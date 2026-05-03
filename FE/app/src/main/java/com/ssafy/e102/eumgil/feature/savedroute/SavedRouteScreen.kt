package com.ssafy.e102.eumgil.feature.savedroute

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
            BookmarkTopBar(
                isEditMode = uiState.isEditMode,
                editEnabled = uiState.canEnterEditMode || uiState.isEditMode,
                onEditToggleClick = { onAction(SavedRouteUiAction.EditModeToggled) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            BookmarkTabRow(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab -> onAction(SavedRouteUiAction.TabSelected(tab)) },
            )
            when (uiState.selectedTab) {
                BookmarkTab.PLACE ->
                    BookmarkPlaceTabContent(
                        uiState = uiState,
                        onAction = onAction,
                    )
                BookmarkTab.ROUTE ->
                    BookmarkRouteTabPlaceholder()
            }
        }
    }
}

@Composable
private fun BookmarkTopBar(
    isEditMode: Boolean,
    editEnabled: Boolean,
    onEditToggleClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.bookmark_screen_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onEditToggleClick,
                enabled = editEnabled,
            ) {
                Text(
                    text =
                        stringResource(
                            id =
                                if (isEditMode) {
                                    R.string.bookmark_edit_done
                                } else {
                                    R.string.bookmark_edit_enter
                                },
                        ),
                )
            }
        }
    }
}

@Composable
private fun BookmarkTabRow(
    selectedTab: BookmarkTab,
    onTabSelected: (BookmarkTab) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
    ) {
        BookmarkTabItem(
            label = stringResource(id = R.string.bookmark_tab_place),
            selected = selectedTab == BookmarkTab.PLACE,
            onClick = { onTabSelected(BookmarkTab.PLACE) },
            modifier = Modifier.weight(1f),
        )
        BookmarkTabItem(
            label = stringResource(id = R.string.bookmark_tab_route),
            selected = selectedTab == BookmarkTab.ROUTE,
            onClick = { onTabSelected(BookmarkTab.ROUTE) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BookmarkTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        }
    val textColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        }
    Surface(
        modifier =
            modifier
                .height(40.dp)
                .clickable(role = Role.Tab, onClick = onClick)
                .semantics {
                    this.selected = selected
                },
        shape = RoundedCornerShape(EumRadius.medium),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
            )
        }
    }
}

@Composable
private fun BookmarkPlaceTabContent(
    uiState: SavedRouteUiState,
    onAction: (SavedRouteUiAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = EumSpacing.medium,
                vertical = EumSpacing.medium,
            ),
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        when (uiState.screenState) {
            SavedRouteScreenState.LOADING ->
                item {
                    BookmarkStateCard(
                        title = stringResource(id = R.string.saved_route_loading_title),
                        description = stringResource(id = R.string.saved_route_loading_description),
                        isLoading = true,
                    )
                }

            SavedRouteScreenState.EMPTY ->
                item {
                    BookmarkStateCard(
                        title = stringResource(id = R.string.bookmark_place_empty_title),
                        description = stringResource(id = R.string.bookmark_place_empty_description),
                        primaryActionLabel = stringResource(id = R.string.bookmark_place_empty_search_cta),
                        onPrimaryActionClick = { onAction(SavedRouteUiAction.ExploreMapClicked) },
                    )
                }

            SavedRouteScreenState.ERROR ->
                item {
                    BookmarkStateCard(
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
                        BookmarkInlineMessage(message = message)
                    }
                }
                items(
                    items = uiState.places,
                    key = SavedPlaceUiModel::placeId,
                ) { place ->
                    BookmarkPlaceCard(
                        place = place,
                        isEditMode = uiState.isEditMode,
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

@Composable
private fun BookmarkRouteTabPlaceholder() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.medium),
        contentAlignment = Alignment.TopCenter,
    ) {
        BookmarkStateCard(
            title = stringResource(id = R.string.bookmark_route_placeholder_title),
            description = stringResource(id = R.string.bookmark_route_placeholder_description),
        )
    }
}

@Composable
private fun BookmarkStateCard(
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
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
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
            BookmarkStateActions(
                primaryActionLabel = primaryActionLabel,
                onPrimaryActionClick = onPrimaryActionClick,
                secondaryActionLabel = secondaryActionLabel,
                onSecondaryActionClick = onSecondaryActionClick,
            )
        }
    }
}

@Composable
private fun BookmarkStateActions(
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
private fun BookmarkInlineMessage(message: String) {
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
private fun BookmarkPlaceCard(
    place: SavedPlaceUiModel,
    isEditMode: Boolean,
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
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Button,
                            enabled = !isEditMode,
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
                    fontWeight = FontWeight.SemiBold,
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
            Spacer(modifier = Modifier.height(EumSpacing.xSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                if (isEditMode) {
                    OutlinedButton(
                        onClick = onRemoveClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.saved_route_remove_bookmark))
                    }
                } else {
                    Button(
                        onClick = onRouteGuideClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.bookmark_route_guide_cta))
                    }
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
