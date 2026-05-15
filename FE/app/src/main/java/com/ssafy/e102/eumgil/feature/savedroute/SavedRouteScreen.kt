package com.ssafy.e102.eumgil.feature.savedroute

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.RouteOption

@Composable
fun SavedRouteScreen(
    uiState: SavedRouteUiState,
    onAction: (SavedRouteUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedRemovalCount =
        uiState.pendingPlaceRemovalIds.size + uiState.pendingRouteRemovalIds.size
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
        bottomBar = {
            if (uiState.isEditMode) {
                SavedBookmarkEditBottomBar(
                    selectedCount = selectedRemovalCount,
                    isActionEnabled = !uiState.isApplyingEditChanges && selectedRemovalCount > 0,
                    onDeleteClick = { onAction(SavedRouteUiAction.DeleteSelectedClicked) },
                )
            }
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
            val editModeDescription =
                stringResource(
                    id =
                        if (isEditMode) {
                            R.string.saved_route_edit_mode_active_a11y
                        } else {
                            R.string.saved_route_edit_mode_inactive_a11y
                        },
                )
            TextButton(
                onClick = onActionClick,
                enabled = isActionEnabled,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .semantics { contentDescription = editModeDescription },
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
    val tabSelectedStateDescription = stringResource(id = R.string.a11y_tab_selected)
    val tabUnselectedStateDescription = stringResource(id = R.string.a11y_tab_unselected)

    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(EumRadius.full))
                .semantics {
                    role = Role.Tab
                    this.selected = selected
                    stateDescription =
                        if (selected) {
                            tabSelectedStateDescription
                        } else {
                            tabUnselectedStateDescription
                        }
                }
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
                iconRes = R.drawable.ic_status_help_circle,
                primaryActionLabel = stringResource(id = R.string.saved_route_explore_map),
                onPrimaryActionClick = { onAction(SavedRouteUiAction.ExploreMapClicked) },
                modifier = modifier.fillMaxWidth(),
            )
        SavedBookmarkContentState.ERROR ->
            SavedBookmarkStateCard(
                title = stringResource(id = R.string.saved_route_place_error_title),
                description = content.errorMessage ?: stringResource(id = R.string.saved_route_error_description),
                iconRes = R.drawable.ic_status_warning,
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
                                {
                                    onAction(SavedRouteUiAction.PlaceDeleteClicked(placeId = place.placeId))
                                }
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
                iconRes = R.drawable.ic_status_help_circle,
                primaryActionLabel = stringResource(id = R.string.saved_route_route_setting_action),
                onPrimaryActionClick = { onAction(SavedRouteUiAction.RouteSettingClicked) },
                modifier = modifier.fillMaxWidth(),
            )
        SavedBookmarkContentState.ERROR ->
            SavedBookmarkStateCard(
                title = stringResource(id = R.string.saved_route_route_error_title),
                description = content.errorMessage ?: stringResource(id = R.string.saved_route_route_error_description),
                iconRes = R.drawable.ic_status_warning,
                primaryActionLabel = stringResource(id = R.string.saved_route_retry),
                onPrimaryActionClick = { onAction(SavedRouteUiAction.RetryClicked) },
                secondaryActionLabel = stringResource(id = R.string.saved_route_route_setting_action),
                onSecondaryActionClick = { onAction(SavedRouteUiAction.RouteSettingClicked) },
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
                        onRouteClick =
                            if (isEditMode) {
                                {
                                    onAction(SavedRouteUiAction.RouteDeleteClicked(bookmarkId = routeBookmark.bookmarkId))
                                }
                            } else if (!isActionEnabled) {
                                null
                            } else {
                                {
                                    onAction(SavedRouteUiAction.RouteClicked(bookmarkId = routeBookmark.bookmarkId))
                                }
                            },
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
    @DrawableRes iconRes: Int? = null,
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
    val iconTint =
        if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }

    Surface(
        modifier =
            modifier.semantics {
                liveRegion =
                    when {
                        isError -> LiveRegionMode.Assertive
                        isLoading -> LiveRegionMode.Polite
                        else -> LiveRegionMode.Polite
                    }
            },
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                iconRes != null ->
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = iconTint,
                    )
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
        NoRippleSavedRouteNavigationButton(
            onClick = onPrimaryActionClick,
            modifier = Modifier.weight(1f),
            fullWidthContent = true,
        ) {
            Text(text = primaryActionLabel)
        }
        if (secondaryActionLabel != null && onSecondaryActionClick != null) {
            NoRippleSavedRouteNavigationButton(
                onClick = onSecondaryActionClick,
                modifier = Modifier.weight(1f),
                isOutlined = true,
                fullWidthContent = true,
            ) {
                Text(text = secondaryActionLabel)
            }
        }
    }
}

@Composable
private fun NoRippleSavedRouteNavigationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isOutlined: Boolean = false,
    fullWidthContent: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(EumRadius.full),
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor =
        when {
            !enabled && isOutlined -> MaterialTheme.colorScheme.surface
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            isOutlined -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.primary
        }
    val contentColor =
        when {
            !enabled && isOutlined -> MaterialTheme.colorScheme.primary.copy(alpha = 0.56f)
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            isOutlined -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onPrimary
        }
    val border =
        if (isOutlined) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.36f))
        } else {
            null
        }

    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border,
    ) {
        Row(
            modifier =
                (if (fullWidthContent) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                })
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    )
                    .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
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
    val interactionSource = remember { MutableInteractionSource() }
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
            if (isEditMode) {
                SavedBookmarkSelectionButton(
                    selected = isPendingRemoval,
                    enabled = isActionEnabled,
                    onClick = onPrimaryActionClick,
                    contentDescription =
                        stringResource(
                            id = R.string.saved_route_select_item_a11y,
                            place.name,
                        ),
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .then(
                            if (onPlaceClick != null) {
                                Modifier.clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
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
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = savedPlaceCategoryIconRes(place.category)),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .align(Alignment.CenterVertically)
                                .size(SavedBookmarkCategoryIconSize),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                    ) {
                        Text(
                            text = savedPlaceCategoryLabel(category = place.category),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = place.name,
                            style = MaterialTheme.typography.titleMedium.copy(lineHeight = SavedBookmarkPlaceNameLineHeight),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = SavedBookmarkPrimaryTextMaxLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = place.address ?: stringResource(id = R.string.saved_route_address_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (!isEditMode) {
                SavedBookmarkPrimaryActionButton(
                    enabled = isActionEnabled,
                    onClick = onPrimaryActionClick,
                    accessibilityContext = place.name,
                )
            }
        }
    }
}

@Composable
private fun SavedRouteBookmarkListItem(
    routeBookmark: SavedRouteBookmarkUiModel,
    isEditMode: Boolean,
    isPendingRemoval: Boolean,
    isActionEnabled: Boolean,
    onRouteClick: (() -> Unit)?,
    onPrimaryActionClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val accessibilityDescription =
        stringResource(
            id = R.string.saved_route_route_a11y_description,
            routeBookmark.routeName,
            routeBookmark.startLabel,
            routeBookmark.endLabel,
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
            verticalAlignment = Alignment.Top,
        ) {
            if (isEditMode) {
                SavedBookmarkSelectionButton(
                    selected = isPendingRemoval,
                    enabled = isActionEnabled,
                    onClick = onPrimaryActionClick,
                    contentDescription =
                        stringResource(
                            id = R.string.saved_route_select_item_a11y,
                            routeBookmark.routeName,
                        ),
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .then(
                            if (onRouteClick != null) {
                                Modifier.clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onRouteClick,
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
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                ) {
                    SavedRoutePathDecoration(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .padding(vertical = 2.dp),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                    ) {
                        SavedRouteWaypointInfoRow(
                            label = stringResource(id = R.string.route_setting_origin_label),
                            value = routeBookmark.startLabel,
                            accentColor = MaterialTheme.colorScheme.primary,
                        )
                        SavedRouteWaypointInfoRow(
                            label = stringResource(id = R.string.route_setting_destination_label),
                            value = routeBookmark.endLabel,
                            accentColor = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                ) {
                    transportModeLabel(routeBookmark.transportMode)?.let { label ->
                        SavedRouteTagChip(label = label)
                    }
                    routeOptionCompactLabel(
                        rawLabel = routeBookmark.routeOptionLabel,
                        fallback = routeBookmark.routeOption,
                    )?.let { label ->
                        SavedRouteTagChip(label = label)
                    }
                }
            }
            if (!isEditMode) {
                SavedBookmarkPrimaryActionButton(
                    enabled = isActionEnabled,
                    onClick = onPrimaryActionClick,
                    accessibilityContext = routeBookmark.routeName,
                )
            }
        }
    }
}

@Composable
private fun SavedBookmarkSelectionButton(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        }
    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        }

    Box(
        modifier =
            Modifier
                .size(SavedBookmarkSelectionTouchSize)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Checkbox
                    stateDescription =
                        if (selected) {
                            "선택됨"
                        } else {
                            "선택 안 됨"
                        }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(SavedBookmarkSelectionVisualSize),
            shape = CircleShape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = if (selected) 1.dp else 0.dp,
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_confirm),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(5.dp),
                )
            }
        }
    }
}

@Composable
private fun SavedBookmarkEditBottomBar(
    selectedCount: Int,
    isActionEnabled: Boolean,
    onDeleteClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 3.dp,
    ) {
        Button(
            onClick = onDeleteClick,
            enabled = isActionEnabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = EumSpacing.medium,
                        end = EumSpacing.medium,
                        top = EumSpacing.small,
                        bottom = EumSpacing.small,
                    )
                    .heightIn(min = 56.dp),
            shape = RoundedCornerShape(EumRadius.full),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.32f),
                    disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.70f),
                ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_action_delete),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(EumSpacing.xSmall))
            Text(
                text = stringResource(id = R.string.saved_route_delete_selected, selectedCount),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SavedBookmarkPrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accessibilityContext: String? = null,
) {
    val navigationButtonShape = RoundedCornerShape(EumRadius.small)
    val accessibilityLabel =
        accessibilityContext?.let { context ->
            stringResource(id = R.string.saved_route_action_start_a11y, context)
        }
    val sharedModifier =
        if (accessibilityLabel != null) {
            modifier.semantics { contentDescription = accessibilityLabel }
        } else {
            modifier
        }
    NoRippleSavedRouteNavigationButton(
        onClick = onClick,
        modifier =
            sharedModifier.heightIn(min = 38.dp),
        enabled = enabled,
        isOutlined = true,
        shape = navigationButtonShape,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_route_start_navigation_button),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(id = R.string.saved_route_start_route),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SavedRoutePathDecoration(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
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
                    .weight(1f)
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
private fun SavedRouteWaypointInfoRow(
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = SavedBookmarkWaypointValueLineHeight),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = SavedBookmarkWaypointValueMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun routeOptionCompactLabel(
    rawLabel: String?,
    fallback: RouteOption,
): String? =
    when (rawLabel?.uppercase()) {
        "SAFE" -> stringResource(id = R.string.saved_route_route_option_safe_compact)
        "SHORTEST" -> stringResource(id = R.string.saved_route_route_option_fast_compact)
        null, "" ->
            when (fallback) {
                RouteOption.SAFE -> stringResource(id = R.string.saved_route_route_option_safe_compact)
                RouteOption.SHORTEST -> stringResource(id = R.string.saved_route_route_option_fast_compact)
                else -> null
            }
        else -> null
    }

@Composable
private fun transportModeLabel(transportMode: String?): String? =
    when (transportMode?.uppercase()) {
        "WALK" -> stringResource(id = R.string.saved_route_transport_mode_walk)
        "PUBLIC_TRANSIT" -> stringResource(id = R.string.saved_route_transport_mode_transit)
        null, "" -> null
        else -> transportMode
    }

private const val SavedBookmarkPrimaryTextMaxLines = 2
private const val SavedBookmarkWaypointValueMaxLines = 1
private val SavedBookmarkPlaceNameLineHeight = 20.sp
private val SavedBookmarkWaypointValueLineHeight = 18.sp
private val SavedBookmarkCategoryIconSize = 40.dp
private val SavedBookmarkSelectionTouchSize = 48.dp
private val SavedBookmarkSelectionVisualSize = 30.dp
