package com.ssafy.e102.eumgil.feature.route

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import java.util.Locale

@Composable
fun RouteSettingScreen(
    uiState: RouteSettingUiState,
    onAction: (RouteSettingUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isWalkMode = uiState.selectedTravelMode == RouteTravelMode.WALK
    val supportingMessage =
        when (uiState.destinationHandoffState) {
            RouteDestinationHandoffState.DIRECT ->
                uiState.destinationFallbackMessage?.takeIf(String::isNotBlank)

            RouteDestinationHandoffState.EMPTY ->
                stringResource(id = R.string.route_setting_screen_description_empty)

            RouteDestinationHandoffState.INVALID_COORDINATE ->
                stringResource(
                    id = R.string.route_setting_screen_description_invalid_handoff,
                    uiState.destination.name,
                )
        }
    val ctaSupportingText =
        when {
            !isWalkMode -> stringResource(id = R.string.route_setting_transit_cta_supporting)
            uiState.cta.isEnabled -> null
            else -> uiState.cta.supportingText
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            RouteSettingTopBar(
                onBackClick = { onAction(RouteSettingUiAction.BackClicked) },
            )
        },
        bottomBar = {
            RouteSettingBottomBar(
                buttonLabel = uiState.cta.label,
                enabled = uiState.isStartEnabled,
                supportingText = ctaSupportingText,
                onStartClick = { onAction(RouteSettingUiAction.StartNavigationClicked) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            RouteWaypointCard(
                origin = uiState.origin,
                destination = uiState.destination,
                supportingMessage = supportingMessage,
            )
            RouteTravelModeTabs(
                selectedMode = uiState.selectedTravelMode,
                onModeSelected = { mode ->
                    onAction(RouteSettingUiAction.TravelModeSelected(mode))
                },
            )
            RouteMapStage(uiState = uiState)
            if (isWalkMode) {
                RouteWalkOptionSection(
                    uiState = uiState,
                    onOptionClick = { routeOption ->
                        onAction(RouteSettingUiAction.RouteOptionSelected(routeOption))
                    },
                )
            } else {
                RouteTransitOptionSection()
            }
        }
    }
}

@Composable
private fun RouteSettingTopBar(
    onBackClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
        tonalElevation = 1.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = EumSpacing.xSmall, vertical = EumSpacing.xxSmall),
        ) {
            TextButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text(text = stringResource(id = R.string.route_setting_back))
            }
            Text(
                text = stringResource(id = R.string.route_setting_screen_title),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RouteWaypointCard(
    origin: RouteLocationUiState,
    destination: RouteLocationUiState,
    supportingMessage: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            RouteWaypointRow(
                label = stringResource(id = R.string.route_setting_origin_label),
                name = origin.name,
                supportingText = origin.supportingText,
                markerColor = MaterialTheme.colorScheme.primary,
            )
            Spacer(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            )
            RouteWaypointRow(
                label = stringResource(id = R.string.route_setting_destination_label),
                name = destination.name,
                supportingText = destination.supportingText,
                markerColor = MaterialTheme.colorScheme.error,
            )
            supportingMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RouteWaypointRow(
    label: String,
    name: String,
    supportingText: String?,
    markerColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(12.dp)
                    .background(color = markerColor, shape = CircleShape),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = markerColor,
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            supportingText?.takeIf(String::isNotBlank)?.let { value ->
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RouteTravelModeTabs(
    selectedMode: RouteTravelMode,
    onModeSelected: (RouteTravelMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
    ) {
        RouteTravelModeTab(
            label = stringResource(id = R.string.route_setting_travel_mode_walk),
            isSelected = selectedMode == RouteTravelMode.WALK,
            modifier = Modifier.weight(1f),
            onClick = { onModeSelected(RouteTravelMode.WALK) },
        )
        RouteTravelModeTab(
            label = stringResource(id = R.string.route_setting_travel_mode_transit),
            isSelected = selectedMode == RouteTravelMode.TRANSIT,
            modifier = Modifier.weight(1f),
            onClick = { onModeSelected(RouteTravelMode.TRANSIT) },
        )
    }
}

@Composable
private fun RouteTravelModeTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier.clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(EumRadius.medium),
        color =
            if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        border =
            BorderStroke(
                1.dp,
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                },
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

@Composable
private fun RouteMapStage(
    uiState: RouteSettingUiState,
) {
    val isWalkMode = uiState.selectedTravelMode == RouteTravelMode.WALK
    val selectedRoute = uiState.selectedRoute
    val previewMap = uiState.routePreviewMap
    val routeColor = optionAccentColor(selectedRoute?.routeOption ?: RouteOption.SAFE)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(RoutePreviewMapHeight),
        ) {
            RouteMapBackdrop(
                previewMap = previewMap,
                routePath =
                    if (isWalkMode && previewMap.isDisplayable) {
                        previewMap.polyline
                    } else {
                        emptyList()
                    },
                routeColor = routeColor,
                modifier = Modifier.fillMaxSize(),
            )

            when {
                !isWalkMode ->
                    RouteMapMessageCard(
                        title = stringResource(id = R.string.route_setting_transit_placeholder_title),
                        description = stringResource(id = R.string.route_setting_transit_placeholder_description),
                        modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(EumSpacing.medium),
                    )

                selectedRoute == null || !previewMap.isDisplayable ->
                    RouteMapMessageCard(
                        title = routePreviewFallbackTitle(previewMap.status),
                        description = routePreviewFallbackDescription(previewMap),
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(EumSpacing.medium),
                    )
            }

            if (isWalkMode && selectedRoute != null && previewMap.isDisplayable) {
                RouteMapStatusBadge(
                    label = selectedRoute.optionTitle,
                    supportingText = selectedRoute.summaryLabel,
                    accentColor = routeColor,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(EumSpacing.medium),
                )
            }

            RouteMapControls(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = EumSpacing.small),
            )
        }
    }
}

@Composable
private fun RouteMapStatusBadge(
    label: String,
    supportingText: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EumRadius.medium),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = EumSpacing.small, vertical = EumSpacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteMapMessageCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.wrapContentWidth(),
        shape = RoundedCornerShape(EumRadius.medium),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.small),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteMapControls(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
        horizontalAlignment = Alignment.End,
    ) {
        RouteMapControlButton(label = "+")
        RouteMapControlButton(label = "-")
        RouteMapControlButton(
            label = stringResource(id = R.string.route_setting_map_control_recenter),
            isWide = true,
        )
    }
}

@Composable
private fun RouteMapControlButton(
    label: String,
    isWide: Boolean = false,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .then(if (isWide) Modifier.widthIn(min = 56.dp) else Modifier)
                    .padding(horizontal = if (isWide) 12.dp else 0.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RouteWalkOptionSection(
    uiState: RouteSettingUiState,
    onOptionClick: (RouteOption) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        when {
            uiState.isLoading ->
                RouteStateCard(
                    title = stringResource(id = R.string.route_setting_summary_loading_title),
                    description = stringResource(id = R.string.route_setting_summary_loading_description),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.32f),
                    borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                )

            uiState.loadErrorMessage != null ->
                RouteStateCard(
                    title = stringResource(id = R.string.route_setting_summary_error_title),
                    description = uiState.loadErrorMessage,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.24f),
                )

            uiState.optionCards.isEmpty() ->
                RouteStateCard(
                    title = stringResource(id = R.string.route_setting_summary_empty_title),
                    description = stringResource(id = R.string.route_setting_summary_empty_description),
                )

            else ->
                uiState.optionCards.forEach { optionCard ->
                    RouteCompactOptionCard(
                        card = optionCard,
                        onClick = { onOptionClick(optionCard.routeOption) },
                    )
                }
        }

        uiState.sourceLabel?.takeIf(String::isNotBlank)?.let { label ->
            Text(
                text = stringResource(id = R.string.route_setting_summary_source_value, label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteTransitOptionSection() {
    RouteStateCard(
        title = stringResource(id = R.string.route_setting_transit_placeholder_title),
        description = stringResource(id = R.string.route_setting_transit_placeholder_description),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteCompactOptionCard(
    card: RouteOptionCardUiState,
    onClick: () -> Unit,
) {
    val accentColor = optionAccentColor(card.routeOption)
    val containerColor =
        when {
            card.isSelected -> accentColor.copy(alpha = 0.1f)
            card.routeOption == RouteOption.SAFE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.surface
        }
    val borderColor =
        if (card.isSelected) {
            accentColor.copy(alpha = 0.48f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(EumRadius.large),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RouteOptionSelectionIndicator(
                    isSelected = card.isSelected,
                    accentColor = accentColor,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        card.highlightLabel?.let { highlightLabel ->
                            RouteBadgeChip(
                                label = highlightLabel,
                                containerColor = accentColor.copy(alpha = 0.12f),
                                contentColor = accentColor,
                            )
                        }
                    }
                    Text(
                        text = card.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = compactEstimatedTimeLabel(card.estimatedTimeMinutes),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = compactDistanceLabel(card.distanceMeters),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                RouteRiskChip(riskLevel = card.riskLevel)
                card.badges.take(2).forEach { badge ->
                    RouteBadgeChip(label = routeBadgeText(badge))
                }
            }
        }
    }
}

@Composable
private fun RouteOptionSelectionIndicator(
    isSelected: Boolean,
    accentColor: Color,
) {
    Surface(
        modifier = Modifier.padding(top = 2.dp),
        shape = CircleShape,
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(4.dp)
                    .size(8.dp)
                    .background(
                        color = if (isSelected) accentColor else Color.Transparent,
                        shape = CircleShape,
                    ),
        )
    }
}

@Composable
private fun RouteSettingBottomBar(
    buttonLabel: String,
    enabled: Boolean,
    supportingText: String?,
    onStartClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            supportingText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onStartClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(text = buttonLabel)
            }
        }
    }
}

@Composable
private fun RouteMapBackdrop(
    previewMap: RoutePreviewMapUiState,
    routePath: List<GeoCoordinate>,
    routeColor: Color,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    val originColor = MaterialTheme.colorScheme.secondary
    val destinationColor = MaterialTheme.colorScheme.error
    val projectionMap =
        previewMap.copy(
            polyline =
                if (routePath.isNotEmpty()) {
                    routePath
                } else {
                    listOfNotNull(previewMap.originCoordinate, previewMap.destinationCoordinate)
                },
        )
    val backgroundBrush =
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
        )
    val mapDescription = stringResource(id = R.string.route_setting_preview_title)

    BoxWithConstraints(
        modifier =
            modifier
                .background(backgroundBrush)
                .semantics(mergeDescendants = true) {
                    contentDescription = mapDescription
                },
    ) {
        val projectionBounds = routePreviewProjectionBounds(projectionMap)
        val horizontalPadding = 28.dp
        val verticalPadding = 24.dp
        val markerAreaWidth = (maxWidth - (horizontalPadding * 2)).coerceAtLeast(0.dp)
        val markerAreaHeight = (maxHeight - (verticalPadding * 2)).coerceAtLeast(0.dp)

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoutePreviewMapGrid(outline = outline)

            if (routePath.size >= 2) {
                val routePreviewPath =
                    routePath.toRoutePreviewPath(
                        bounds = projectionBounds,
                        canvasSize = size,
                    )
                drawPath(
                    path = routePreviewPath,
                    color = routeColor.copy(alpha = 0.26f),
                    style =
                        Stroke(
                            width = 12.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                )
                drawPath(
                    path = routePreviewPath,
                    color = routeColor,
                    style =
                        Stroke(
                            width = 5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                )
            }

            previewMap.originCoordinate?.let { coordinate ->
                drawCircle(
                    color = originColor.copy(alpha = 0.18f),
                    radius = 18.dp.toPx(),
                    center = projectionBounds.project(coordinate).toCanvasOffset(size),
                )
            }
            previewMap.destinationCoordinate?.let { coordinate ->
                drawCircle(
                    color = destinationColor.copy(alpha = 0.14f),
                    radius = 20.dp.toPx(),
                    center = projectionBounds.project(coordinate).toCanvasOffset(size),
                )
            }
        }

        previewMap.originCoordinate?.let { coordinate ->
            RoutePreviewMapMarker(
                label = stringResource(id = R.string.route_setting_preview_marker_origin),
                containerColor = originColor,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .offsetWithinRoutePreviewMap(
                            point = projectionBounds.project(coordinate),
                            areaWidth = markerAreaWidth,
                            areaHeight = markerAreaHeight,
                            horizontalPadding = horizontalPadding,
                            verticalPadding = verticalPadding,
                            elementSize = RoutePreviewMarkerSize,
                        ),
            )
        }

        previewMap.destinationCoordinate?.let { coordinate ->
            RoutePreviewMapMarker(
                label = stringResource(id = R.string.route_setting_preview_marker_destination),
                containerColor = destinationColor,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .offsetWithinRoutePreviewMap(
                            point = projectionBounds.project(coordinate),
                            areaWidth = markerAreaWidth,
                            areaHeight = markerAreaHeight,
                            horizontalPadding = horizontalPadding,
                            verticalPadding = verticalPadding,
                            elementSize = RoutePreviewMarkerSize,
                        ),
            )
        }
    }
}

@Composable
private fun RoutePreviewMapMarker(
    label: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(RoutePreviewMarkerSize),
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 6.dp,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun routePreviewFallbackTitle(status: RoutePreviewMapStatus): String =
    when (status) {
        RoutePreviewMapStatus.LOADING -> stringResource(id = R.string.route_setting_preview_loading_title)
        RoutePreviewMapStatus.NO_DESTINATION -> stringResource(id = R.string.route_setting_preview_no_destination_title)
        RoutePreviewMapStatus.INVALID_DESTINATION -> stringResource(id = R.string.route_setting_preview_invalid_destination_title)
        RoutePreviewMapStatus.NO_ROUTE -> stringResource(id = R.string.route_setting_preview_no_route_title)
        RoutePreviewMapStatus.POLYLINE_UNAVAILABLE -> stringResource(id = R.string.route_setting_preview_placeholder_title)
        RoutePreviewMapStatus.ERROR -> stringResource(id = R.string.route_setting_preview_error_title)
        RoutePreviewMapStatus.READY -> stringResource(id = R.string.route_setting_preview_title)
    }

@Composable
private fun routePreviewFallbackDescription(previewMap: RoutePreviewMapUiState): String =
    when (previewMap.status) {
        RoutePreviewMapStatus.LOADING -> stringResource(id = R.string.route_setting_preview_loading_description)
        RoutePreviewMapStatus.NO_DESTINATION -> stringResource(id = R.string.route_setting_preview_no_destination_description)
        RoutePreviewMapStatus.INVALID_DESTINATION -> stringResource(id = R.string.route_setting_preview_invalid_destination_description)
        RoutePreviewMapStatus.NO_ROUTE -> stringResource(id = R.string.route_setting_preview_no_route_description)
        RoutePreviewMapStatus.POLYLINE_UNAVAILABLE ->
            previewMap.fallbackMessage ?: stringResource(id = R.string.route_setting_preview_placeholder_description)
        RoutePreviewMapStatus.ERROR ->
            previewMap.fallbackMessage ?: stringResource(id = R.string.route_setting_preview_error_description)
        RoutePreviewMapStatus.READY -> stringResource(id = R.string.route_setting_preview_placeholder_description)
    }

@Composable
private fun RouteRiskChip(
    riskLevel: RouteRiskLevel,
) {
    val (containerColor, contentColor) =
        when (riskLevel) {
            RouteRiskLevel.LOW ->
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.primary

            RouteRiskLevel.MEDIUM ->
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f) to MaterialTheme.colorScheme.tertiary

            RouteRiskLevel.HIGH ->
                MaterialTheme.colorScheme.error.copy(alpha = 0.12f) to MaterialTheme.colorScheme.error
        }

    RouteBadgeChip(
        label = riskLevelText(riskLevel = riskLevel),
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

@Composable
private fun RouteBadgeChip(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.64f),
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EumRadius.full),
        color = containerColor,
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
            color = contentColor,
        )
    }
}

@Composable
private fun RouteStateCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun riskLevelText(riskLevel: RouteRiskLevel): String =
    when (riskLevel) {
        RouteRiskLevel.LOW -> stringResource(id = R.string.route_setting_risk_low)
        RouteRiskLevel.MEDIUM -> stringResource(id = R.string.route_setting_risk_medium)
        RouteRiskLevel.HIGH -> stringResource(id = R.string.route_setting_risk_high)
    }

@Composable
private fun routeBadgeText(badge: RouteOptionBadge): String =
    when (badge) {
        RouteOptionBadge.SAFE_PRIORITY -> stringResource(id = R.string.route_setting_badge_safe_priority)
        RouteOptionBadge.STEP_FREE -> stringResource(id = R.string.route_setting_badge_step_free)
        RouteOptionBadge.AUDIO_SIGNAL -> stringResource(id = R.string.route_setting_badge_audio_signal)
        RouteOptionBadge.BRAILLE_BLOCK -> stringResource(id = R.string.route_setting_badge_braille_block)
        RouteOptionBadge.SIGNAL_CROSSWALK -> stringResource(id = R.string.route_setting_badge_signal_crosswalk)
        RouteOptionBadge.CURB_GAP -> stringResource(id = R.string.route_setting_badge_curb_gap)
        RouteOptionBadge.UNSIGNALIZED_CROSSWALK ->
            stringResource(id = R.string.route_setting_badge_unsignalized_crosswalk)
    }

@Composable
private fun optionAccentColor(routeOption: RouteOption): Color =
    when (routeOption) {
        RouteOption.SAFE -> MaterialTheme.colorScheme.primary
        RouteOption.SHORTEST -> MaterialTheme.colorScheme.tertiary
    }

private fun compactEstimatedTimeLabel(minutes: Int): String =
    if (minutes > 0) {
        "${minutes}분"
    } else {
        "--"
    }

private fun compactDistanceLabel(distanceMeters: Int): String =
    when {
        distanceMeters <= 0 -> "--"
        distanceMeters < METERS_PER_KILOMETER -> "${distanceMeters}m"
        else -> String.format(Locale.US, "%.1fkm", distanceMeters / METERS_PER_KILOMETER.toFloat())
    }

private fun DrawScope.drawRoutePreviewMapGrid(outline: Color) {
    val verticalStep = size.width / 5f
    val horizontalStep = size.height / 6f
    val strokeWidth = 1.dp.toPx()

    for (index in 0..5) {
        val x = index * verticalStep
        drawLine(
            color = outline.copy(alpha = 0.18f),
            start = Offset(x, 0f),
            end = Offset(x - (size.height * 0.16f), size.height),
            strokeWidth = strokeWidth,
        )
    }

    for (index in 0..6) {
        val y = index * horizontalStep
        drawLine(
            color = outline.copy(alpha = 0.12f),
            start = Offset(0f, y),
            end = Offset(size.width, y + (size.width * 0.08f)),
            strokeWidth = strokeWidth,
        )
    }
}

private fun List<GeoCoordinate>.toRoutePreviewPath(
    bounds: RoutePreviewProjectionBounds,
    canvasSize: Size,
): Path =
    Path().also { path ->
        forEachIndexed { index, coordinate ->
            val offset = bounds.project(coordinate).toCanvasOffset(canvasSize)
            if (index == 0) {
                path.moveTo(offset.x, offset.y)
            } else {
                path.lineTo(offset.x, offset.y)
            }
        }
    }

private fun RoutePreviewProjectionPoint.toCanvasOffset(size: Size): Offset =
    Offset(
        x = size.width * xRatio,
        y = size.height * yRatio,
    )

private fun Modifier.offsetWithinRoutePreviewMap(
    point: RoutePreviewProjectionPoint,
    areaWidth: Dp,
    areaHeight: Dp,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    elementSize: Dp,
): Modifier =
    offset(
        x = horizontalPadding + (areaWidth * point.xRatio) - (elementSize / 2),
        y = verticalPadding + (areaHeight * point.yRatio) - (elementSize / 2),
    )

private fun routePreviewProjectionBounds(previewMap: RoutePreviewMapUiState): RoutePreviewProjectionBounds {
    val coordinates =
        buildList {
            addAll(previewMap.polyline)
            previewMap.originCoordinate?.let(::add)
            previewMap.destinationCoordinate?.let(::add)
        }

    if (coordinates.isEmpty()) {
        return RoutePreviewProjectionBounds(
            minLatitude = DEFAULT_PREVIEW_CENTER_LATITUDE - (MIN_ROUTE_PREVIEW_LATITUDE_SPAN / 2.0),
            maxLatitude = DEFAULT_PREVIEW_CENTER_LATITUDE + (MIN_ROUTE_PREVIEW_LATITUDE_SPAN / 2.0),
            minLongitude = DEFAULT_PREVIEW_CENTER_LONGITUDE - (MIN_ROUTE_PREVIEW_LONGITUDE_SPAN / 2.0),
            maxLongitude = DEFAULT_PREVIEW_CENTER_LONGITUDE + (MIN_ROUTE_PREVIEW_LONGITUDE_SPAN / 2.0),
        )
    }

    val latitudeBounds =
        expandedRoutePreviewBounds(
            minValue = coordinates.minOf { coordinate -> coordinate.latitude },
            maxValue = coordinates.maxOf { coordinate -> coordinate.latitude },
            minimumSpan = MIN_ROUTE_PREVIEW_LATITUDE_SPAN,
        )
    val longitudeBounds =
        expandedRoutePreviewBounds(
            minValue = coordinates.minOf { coordinate -> coordinate.longitude },
            maxValue = coordinates.maxOf { coordinate -> coordinate.longitude },
            minimumSpan = MIN_ROUTE_PREVIEW_LONGITUDE_SPAN,
        )

    return RoutePreviewProjectionBounds(
        minLatitude = latitudeBounds.first,
        maxLatitude = latitudeBounds.second,
        minLongitude = longitudeBounds.first,
        maxLongitude = longitudeBounds.second,
    )
}

private fun expandedRoutePreviewBounds(
    minValue: Double,
    maxValue: Double,
    minimumSpan: Double,
): Pair<Double, Double> {
    val center = (minValue + maxValue) / 2.0
    val paddedSpan = (maxValue - minValue) * 1.42
    val finalSpan = maxOf(paddedSpan, minimumSpan)
    val halfSpan = finalSpan / 2.0

    return (center - halfSpan) to (center + halfSpan)
}

private data class RoutePreviewProjectionBounds(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
) {
    private val latitudeSpan: Double
        get() = (maxLatitude - minLatitude).coerceAtLeast(MIN_ROUTE_PREVIEW_LATITUDE_SPAN)

    private val longitudeSpan: Double
        get() = (maxLongitude - minLongitude).coerceAtLeast(MIN_ROUTE_PREVIEW_LONGITUDE_SPAN)

    fun project(coordinate: GeoCoordinate): RoutePreviewProjectionPoint {
        val longitudeRatio =
            ((coordinate.longitude - minLongitude) / longitudeSpan)
                .toFloat()
                .coerceIn(0.08f, 0.92f)
        val latitudeRatio =
            (1f - ((coordinate.latitude - minLatitude) / latitudeSpan).toFloat())
                .coerceIn(0.1f, 0.9f)

        return RoutePreviewProjectionPoint(
            xRatio = longitudeRatio,
            yRatio = latitudeRatio,
        )
    }
}

private data class RoutePreviewProjectionPoint(
    val xRatio: Float,
    val yRatio: Float,
)

private const val METERS_PER_KILOMETER = 1_000
private const val DEFAULT_PREVIEW_CENTER_LATITUDE = 35.1796
private const val DEFAULT_PREVIEW_CENTER_LONGITUDE = 129.0756
private val RoutePreviewMapHeight = 300.dp
private val RoutePreviewMarkerSize = 38.dp
private const val MIN_ROUTE_PREVIEW_LATITUDE_SPAN = 0.0035
private const val MIN_ROUTE_PREVIEW_LONGITUDE_SPAN = 0.0045
