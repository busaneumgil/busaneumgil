package com.ssafy.e102.eumgil.feature.route

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel

@Composable
fun RouteSettingScreen(
    uiState: RouteSettingUiState,
    onAction: (RouteSettingUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description =
        if (selectedDestination == null) {
            stringResource(id = R.string.route_setting_screen_description_empty)
        } else {
            stringResource(
                id = R.string.route_setting_screen_description_with_destination,
                selectedDestination.name,
            )
        }

    EumPlaceholderScaffold(
        title = stringResource(id = R.string.route_setting_screen_title),
        description = description,
        featurePath = stringResource(id = R.string.feature_path_route_setting),
        actions =
            listOf(
                PlaceholderAction(
                    label = stringResource(id = R.string.action_go_map),
                    onClick = onNavigateBack,
                    isPrimary = true,
                ),
            ),
        modifier = modifier,
        content = {
            RouteSettingDestinationCard(selectedDestination = selectedDestination)
        },
        bottomBar = {
            RouteSettingBottomBar(
                uiState = uiState,
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
            RouteWaypointSection(uiState = uiState)
            RouteOptionSection(
                optionCards = uiState.optionCards,
                onOptionClick = { routeOption ->
                    onAction(RouteSettingUiAction.RouteOptionSelected(routeOption))
                },
            )
            RouteSummarySection(uiState = uiState)
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
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.small, vertical = EumSpacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            TextButton(onClick = onBackClick) {
                Text(text = stringResource(id = R.string.route_setting_back))
            }

            RouteSettingDestinationField(
                label = stringResource(id = R.string.route_setting_destination_name_label),
                value = selectedDestination.name,
            )
            val address =
                selectedDestination.address
                    ?: stringResource(id = R.string.route_setting_destination_address_empty)
            val coordinate =
                stringResource(
                    id = R.string.route_setting_destination_coordinate_value,
                    selectedDestination.latitude,
                    selectedDestination.longitude,
                )

            RouteSettingDestinationField(
                label = stringResource(id = R.string.route_setting_destination_address_label),
                value = address,
            )
            RouteSettingDestinationField(
                label = stringResource(id = R.string.route_setting_destination_coordinate_label),
                value = coordinate,
            )
        }
    }
}

@Composable
private fun RouteWaypointSection(
    uiState: RouteSettingUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Text(
                text = stringResource(id = R.string.route_setting_origin_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            RouteLocationRow(
                label = stringResource(id = R.string.route_setting_origin_label),
                location = uiState.origin,
                accentColor = MaterialTheme.colorScheme.primary,
            )
            RouteLocationRow(
                label = stringResource(id = R.string.route_setting_destination_label),
                location = uiState.destination,
                accentColor = MaterialTheme.colorScheme.error,
            )
            if (uiState.isUsingFallbackDestination) {
                Text(
                    text = stringResource(id = R.string.route_setting_destination_supporting_fallback),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RouteLocationRow(
    label: String,
    location: RouteLocationUiState,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = EumSpacing.xSmall)
                    .size(10.dp)
                    .background(color = accentColor, shape = CircleShape),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
            )
            Text(
                text = location.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            location.supportingText?.takeIf(String::isNotBlank)?.let { supportingText ->
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            location.coordinate?.let { coordinate ->
                Text(
                    text =
                        stringResource(
                            id = R.string.route_setting_destination_coordinate_value,
                            coordinate.latitude,
                            coordinate.longitude,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RouteOptionSection(
    optionCards: List<RouteOptionCardUiState>,
    onOptionClick: (com.ssafy.e102.eumgil.core.model.RouteOption) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = stringResource(id = R.string.route_setting_option_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(id = R.string.route_setting_option_section_supporting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        optionCards.forEach { optionCard ->
            RouteOptionCard(
                card = optionCard,
                onClick = { onOptionClick(optionCard.routeOption) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteOptionCard(
    card: RouteOptionCardUiState,
    onClick: () -> Unit,
) {
    val accentColor = optionAccentColor(routeOption = card.routeOption)
    val containerColor =
        when {
            card.isSelected && card.routeOption == com.ssafy.e102.eumgil.core.model.RouteOption.SAFE ->
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f)

            card.isSelected ->
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)

            card.routeOption == com.ssafy.e102.eumgil.core.model.RouteOption.SAFE ->
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)

            else -> MaterialTheme.colorScheme.surface
        }
    val borderColor =
        when {
            card.isSelected -> accentColor.copy(alpha = 0.52f)
            card.routeOption == com.ssafy.e102.eumgil.core.model.RouteOption.SAFE -> accentColor.copy(alpha = 0.28f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                    verticalAlignment = Alignment.Top,
                ) {
                    RouteOptionSelectionIndicator(
                        isSelected = card.isSelected,
                        accentColor = accentColor,
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                    ) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = card.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                RouteOptionStateChip(
                    label = card.selectionLabel,
                    isSelected = card.isSelected,
                    accentColor = accentColor,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                card.highlightLabel?.let { highlightLabel ->
                    RouteBadgeChip(
                        label = highlightLabel,
                        containerColor = accentColor.copy(alpha = 0.12f),
                        contentColor = accentColor,
                    )
                }
                card.metrics.forEach { metric ->
                    RouteOptionMetricChip(metric = metric)
                }
            }

            Text(
                text = card.summaryLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                RouteRiskChip(riskLevel = card.riskLevel)
                card.badges.forEach { badge ->
                    RouteBadgeChip(
                        label = routeBadgeText(badge),
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteSummarySection(
    uiState: RouteSettingUiState,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = stringResource(id = R.string.route_setting_summary_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(id = R.string.route_setting_summary_section_supporting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when {
            uiState.isLoading ->
                RouteStateCard(
                    title = stringResource(id = R.string.route_setting_summary_loading_title),
                    description = stringResource(id = R.string.route_setting_summary_loading_description),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f),
                    borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
                )

            uiState.loadErrorMessage != null ->
                RouteStateCard(
                    title = stringResource(id = R.string.route_setting_summary_error_title),
                    description = uiState.loadErrorMessage,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.48f),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.26f),
                )

            uiState.selectedRoute == null ->
                RouteStateCard(
                    title = stringResource(id = R.string.route_setting_summary_empty_title),
                    description = stringResource(id = R.string.route_setting_summary_empty_description),
                )

            else -> RouteSummaryCard(route = uiState.selectedRoute, sourceLabel = uiState.sourceLabel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteSummaryCard(
    route: RouteSelectedRouteUiState,
    sourceLabel: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            RouteBadgeChip(
                label = route.optionTitle,
                containerColor = optionAccentColor(route.routeOption).copy(alpha = 0.12f),
                contentColor = optionAccentColor(route.routeOption),
            )
            Text(
                text = route.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = route.summaryLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                RouteRiskChip(riskLevel = route.riskLevel)
                route.badges.forEach { badge ->
                    RouteBadgeChip(label = routeBadgeText(badge))
                }
            }

            RouteSummaryMetrics(metrics = route.summaryMetrics)

            Column(
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = stringResource(id = R.string.route_setting_summary_guidance_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = route.guidanceMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            RoutePreviewPanel(route = route)

            sourceLabel?.takeIf(String::isNotBlank)?.let { label ->
                Text(
                    text = stringResource(id = R.string.route_setting_summary_source_value, label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        modifier = Modifier.padding(top = EumSpacing.xSmall),
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
private fun RouteOptionStateChip(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
) {
    RouteBadgeChip(
        label = label,
        containerColor =
            if (isSelected) {
                accentColor.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        contentColor =
            if (isSelected) {
                accentColor
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
}

@Composable
private fun RouteOptionMetricChip(
    metric: RouteOptionCardMetricUiState,
) {
    Surface(
        shape = RoundedCornerShape(EumRadius.medium),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = EumSpacing.small, vertical = EumSpacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = metric.value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RouteSummaryMetrics(
    metrics: List<RouteSummaryMetricUiState>,
) {
    if (metrics.isEmpty()) {
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                rowMetrics.forEach { metric ->
                    RouteSummaryMetricCard(
                        metric = metric,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowMetrics.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RouteSummaryMetricCard(
    metric: RouteSummaryMetricUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EumRadius.medium),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.small),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = metric.value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RoutePreviewPanel(
    route: RouteSelectedRouteUiState,
) {
    val previewLineColor = MaterialTheme.colorScheme.primary
    val previewMutedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val previewBackground = MaterialTheme.colorScheme.surfaceContainerLowest
    val previewStartColor = MaterialTheme.colorScheme.secondary
    val previewEndColor = MaterialTheme.colorScheme.error

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.medium),
        color = previewBackground,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Text(
                text = stringResource(id = R.string.route_setting_preview_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (route.previewPoints.size < 2) {
                RouteStateCard(
                    title = stringResource(id = R.string.route_setting_preview_placeholder_title),
                    description = stringResource(id = R.string.route_setting_preview_placeholder_description),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(164.dp),
                ) {
                    val mappedPoints =
                        route.previewPoints.toPreviewOffsets(
                            width = size.width,
                            height = size.height,
                            padding = size.minDimension * 0.14f,
                        )
                    val path =
                        Path().apply {
                            moveTo(mappedPoints.first().x, mappedPoints.first().y)
                            mappedPoints.drop(1).forEach { point ->
                                lineTo(point.x, point.y)
                            }
                        }

                    drawPath(
                        path = path,
                        color = previewLineColor,
                        style =
                            Stroke(
                                width = 8f,
                                cap = StrokeCap.Round,
                            ),
                    )
                    drawCircle(
                        color = previewStartColor,
                        radius = 14f,
                        center = mappedPoints.first(),
                    )
                    drawCircle(
                        color = previewEndColor,
                        radius = 14f,
                        center = mappedPoints.last(),
                    )
                    drawCircle(
                        color = previewBackground,
                        radius = 6f,
                        center = mappedPoints.first(),
                    )
                    drawCircle(
                        color = previewBackground,
                        radius = 6f,
                        center = mappedPoints.last(),
                    )
                }
            }

            Text(
                text =
                    if (route.fallbackSegmentCount > 0) {
                        stringResource(
                            id = R.string.route_setting_preview_meta_with_fallback,
                            route.renderableSegmentCount,
                            route.segmentCount,
                            route.fallbackSegmentCount,
                        )
                    } else {
                        stringResource(
                            id = R.string.route_setting_preview_meta,
                            route.renderableSegmentCount,
                            route.segmentCount,
                        )
                    },
                style = MaterialTheme.typography.bodySmall,
                color = previewMutedColor,
            )

            route.previewFallbackNotice?.let { previewFallbackNotice ->
                Text(
                    text = previewFallbackNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RouteSettingBottomBar(
    uiState: RouteSettingUiState,
    onStartClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Button(
                onClick = onStartClick,
                enabled = uiState.isStartEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = uiState.cta.label)
            }
            Text(
                text = uiState.cta.supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.64f),
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondaryContainer,
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
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    borderColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
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
private fun optionAccentColor(routeOption: com.ssafy.e102.eumgil.core.model.RouteOption): Color =
    when (routeOption) {
        com.ssafy.e102.eumgil.core.model.RouteOption.SAFE -> MaterialTheme.colorScheme.primary
        com.ssafy.e102.eumgil.core.model.RouteOption.SHORTEST -> MaterialTheme.colorScheme.tertiary
    }

private fun List<GeoCoordinate>.toPreviewOffsets(
    width: Float,
    height: Float,
    padding: Float,
): List<Offset> {
    val minLongitude = minOf(GeoCoordinate::longitude)
    val maxLongitude = maxOf(GeoCoordinate::longitude)
    val minLatitude = minOf(GeoCoordinate::latitude)
    val maxLatitude = maxOf(GeoCoordinate::latitude)
    val longitudeRange = (maxLongitude - minLongitude).takeIf { it > 0.0 } ?: 0.001
    val latitudeRange = (maxLatitude - minLatitude).takeIf { it > 0.0 } ?: 0.001
    val usableWidth = (width - (padding * 2f)).coerceAtLeast(1f)
    val usableHeight = (height - (padding * 2f)).coerceAtLeast(1f)

    return map { coordinate ->
        val normalizedX = ((coordinate.longitude - minLongitude) / longitudeRange).toFloat()
        val normalizedY = ((coordinate.latitude - minLatitude) / latitudeRange).toFloat()
        Offset(
            x = padding + (normalizedX * usableWidth),
            y = height - padding - (normalizedY * usableHeight),
        )
    }
}
