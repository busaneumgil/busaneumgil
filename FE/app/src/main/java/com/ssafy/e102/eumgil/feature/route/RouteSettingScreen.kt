package com.ssafy.e102.eumgil.feature.route

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.component.map.EumMapFloatingActionButtonState
import com.ssafy.e102.eumgil.core.designsystem.component.map.EumMapFloatingControls
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumCenteredTopBar
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import com.ssafy.e102.eumgil.feature.map.component.MapOverlayViewport
import com.ssafy.e102.eumgil.feature.map.component.createRoutePreviewViewportOverlayState
import java.util.Locale

@Composable
fun RouteSettingScreen(
    uiState: RouteSettingUiState,
    onAction: (RouteSettingUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        if (uiState.cta.isEnabled) {
            null
        } else {
            uiState.cta.supportingText
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            RouteScreenTopBar(
                title = stringResource(id = R.string.route_setting_screen_title),
                onBackClick = { onAction(RouteSettingUiAction.BackClicked) },
            )
        },
        bottomBar = {
            RouteSettingBottomBar(
                buttonLabel = uiState.cta.label,
                enabled = uiState.isStartEnabled,
                supportingText = ctaSupportingText,
                selectedRoute = uiState.selectedRoute,
                onStartClick = { onAction(RouteSettingUiAction.StartNavigationClicked) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(top = RouteSettingScreenVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(RouteSettingContentGap),
        ) {
            RouteWaypointCard(
                origin = uiState.origin,
                originStatus = uiState.originStatus,
                destination = uiState.destination,
                supportingMessage = supportingMessage,
                onOriginClick = {
                    onAction(RouteSettingUiAction.WaypointClicked(RouteEditingTarget.ORIGIN))
                },
                onDestinationClick = {
                    onAction(RouteSettingUiAction.WaypointClicked(RouteEditingTarget.DESTINATION))
                },
                onSwapClick = { onAction(RouteSettingUiAction.WaypointsSwapClicked) },
                modifier = Modifier.padding(horizontal = EumSpacing.medium),
            )
            RouteTravelModeTabs(
                selectedMode = uiState.selectedTravelMode,
                onModeSelected = { mode ->
                    onAction(RouteSettingUiAction.TravelModeSelected(mode))
                },
                modifier = Modifier.padding(horizontal = EumSpacing.medium),
            )
            RouteMapStage(
                uiState = uiState,
                modifier = Modifier.weight(1f),
            )
            RouteSettingRouteSheet(
                uiState = uiState,
                onOptionClick = { routeOption ->
                    onAction(RouteSettingUiAction.RouteOptionSelected(routeOption))
                },
                onOptionDetailClick = { routeOption ->
                    onAction(RouteSettingUiAction.RouteOptionDetailClicked(routeOption))
                },
            )
        }
    }
}

@Composable
fun RouteDetailScreen(
    uiState: RouteSettingUiState,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedRoute = uiState.selectedRoute
    val returnToRoutesLabel = stringResource(id = R.string.route_setting_detail_return_action)
    val ctaSupportingText =
        if (uiState.cta.isEnabled) {
            null
        } else {
            uiState.cta.supportingText
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            RouteScreenTopBar(
                title = stringResource(id = R.string.route_setting_detail_screen_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            RouteSettingBottomBar(
                buttonLabel = uiState.cta.label,
                enabled = uiState.isStartEnabled,
                supportingText = ctaSupportingText,
                selectedRoute = selectedRoute,
                onStartClick = onStartClick,
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
            verticalArrangement = Arrangement.spacedBy(EumSpacing.large),
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
            )
            when {
                uiState.isLoading ->
                    RouteStateCard(
                        title = stringResource(id = R.string.route_setting_detail_loading_title),
                        description = stringResource(id = R.string.route_setting_detail_loading_description),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.24f),
                        borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                    )

                uiState.loadErrorMessage != null ->
                    RouteStateCard(
                        title = stringResource(id = R.string.route_setting_detail_error_title),
                        description = uiState.loadErrorMessage,
                        actionLabel = returnToRoutesLabel,
                        onActionClick = onBackClick,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.24f),
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                    )

                selectedRoute == null ->
                    RouteStateCard(
                        title = stringResource(id = R.string.route_setting_detail_empty_title),
                        description = stringResource(id = R.string.route_setting_detail_empty_description),
                        actionLabel = returnToRoutesLabel,
                        onActionClick = onBackClick,
                    )

                else -> {
                    RouteDetailSummaryCard(selectedRoute = selectedRoute)
                    RouteDetailStepsSection(
                        origin = uiState.origin,
                        steps = selectedRoute.detailSteps,
                        fallbackMessage = selectedRoute.detailFallbackMessage,
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteDetailSummaryCard(
    selectedRoute: RouteSelectedRouteUiState,
) {
    val accessibilitySummary =
        if (selectedRoute.detailAccessibilityChips.isEmpty()) {
            stringResource(
                id = R.string.route_setting_detail_summary_a11y,
                selectedRoute.optionTitle,
                selectedRoute.estimatedTimeLabel,
                selectedRoute.distanceLabel,
                selectedRoute.riskLabel,
            )
        } else {
            stringResource(
                id = R.string.route_setting_detail_summary_a11y_with_accessibility,
                selectedRoute.optionTitle,
                selectedRoute.estimatedTimeLabel,
                selectedRoute.distanceLabel,
                selectedRoute.riskLabel,
                selectedRoute.detailAccessibilityChips.joinToString(separator = ", ") { chip -> chip.label },
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilitySummary
                    stateDescription = selectedRoute.summaryLabel
                },
        verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
    ) {
        RouteDetailMetricRow(selectedRoute = selectedRoute)
        RouteDetailChipSection(chips = selectedRoute.detailAccessibilityChips)
    }
}

@Composable
private fun RouteDetailMetricRow(
    selectedRoute: RouteSelectedRouteUiState,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RouteDetailMetricCard(
            title = stringResource(id = R.string.route_setting_detail_metric_time),
            value = selectedRoute.estimatedTimeLabel,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        )
        RouteDetailMetricCard(
            title = stringResource(id = R.string.route_setting_detail_metric_distance),
            value = selectedRoute.distanceLabel,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RouteDetailMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = EumSpacing.small)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title $value"
                },
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RouteDetailChipSection(
    chips: List<RouteDetailChipUiState>,
) {
    if (chips.isEmpty()) {
        return
    }

    val accessibilityDescription =
        stringResource(
            id = R.string.route_setting_detail_accessibility_a11y,
            chips.joinToString(separator = ", ") { chip -> chip.label },
        )

    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityDescription
                },
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
    ) {
        chips.forEach { chip ->
            RouteDetailSummaryChip(chip = chip)
        }
    }
}

@Composable
private fun RouteDetailSummaryChip(
    chip: RouteDetailChipUiState,
) {
    val (containerColor, contentColor) = routeDetailToneColors(tone = chip.tone)
    val stateDescription =
        when (chip.tone) {
            RouteDetailTone.WARNING -> stringResource(id = R.string.route_setting_detail_step_state_warning)
            RouteDetailTone.INFO -> stringResource(id = R.string.route_setting_detail_step_state_info)
            RouteDetailTone.NEUTRAL -> stringResource(id = R.string.route_setting_detail_step_state_default)
        }

    Surface(
        modifier =
            Modifier.semantics {
                contentDescription = chip.label
                this.stateDescription = stateDescription
            },
        shape = RoundedCornerShape(RouteCompactChipCornerRadius),
        color = containerColor.copy(alpha = if (chip.tone == RouteDetailTone.INFO) 0.72f else 0.92f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = EumSpacing.small,
                    vertical = 6.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = routeDetailChipIconRes(kind = chip.kind)),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor,
            )
            Text(
                text = chip.label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun RouteDetailHighlightSection(
    highlights: List<RouteDetailHighlightUiState>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = stringResource(id = R.string.route_setting_detail_highlight_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (highlights.isEmpty()) {
            RouteStateCard(
                title = stringResource(id = R.string.route_setting_detail_highlight_empty_title),
                description = stringResource(id = R.string.route_setting_detail_highlight_empty_description),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(EumSpacing.small)) {
                highlights.forEach { highlight ->
                    RouteDetailHighlightCard(highlight = highlight)
                }
            }
        }
    }
}

@Composable
private fun RouteDetailHighlightCard(
    highlight: RouteDetailHighlightUiState,
) {
    val (containerColor, contentColor) = routeDetailToneColors(tone = highlight.tone)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RouteStandardCardCornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            RouteBadgeChip(
                label = highlight.badgeLabel,
                containerColor = contentColor.copy(alpha = 0.14f),
                contentColor = contentColor,
            )
            Text(
                text = highlight.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = highlight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteDetailStepsSection(
    origin: RouteLocationUiState,
    steps: List<RouteDetailStepUiState>,
    fallbackMessage: String?,
) {
    val startStep = steps.firstOrNull()
    val renderedSteps =
        if (steps.size > 1) {
            steps.filterIndexed { index, _ -> index > 0 }
        } else {
            emptyList()
        }
    val stepsAccessibilityDescription =
        stringResource(
            id = R.string.route_setting_detail_steps_a11y,
            steps.size,
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = stepsAccessibilityDescription
                },
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = stringResource(id = R.string.route_setting_detail_steps_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            shape = RoundedCornerShape(RouteSectionCardCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
            shadowElevation = 0.dp,
        ) {
            Column {
                if (startStep == null) {
                    RouteDetailFallbackRow(
                        title = stringResource(id = R.string.route_setting_detail_steps_fallback_title),
                        description = stringResource(id = R.string.route_setting_detail_steps_supporting),
                    )
                } else {
                    RouteDetailOriginStepRow(
                        origin = origin,
                        step = startStep,
                    )
                    fallbackMessage?.takeIf(String::isNotBlank)?.let { message ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                        RouteDetailFallbackRow(
                            title = stringResource(id = R.string.route_setting_detail_steps_fallback_title),
                            description = message,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                        )
                    }
                    if (renderedSteps.isEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                        RouteDetailFallbackRow(
                            title = stringResource(id = R.string.route_setting_detail_steps_fallback_title),
                            description = stringResource(id = R.string.route_setting_detail_steps_supporting),
                        )
                    } else {
                        renderedSteps.forEach { step ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                            RouteDetailStepRow(step = step)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteDetailOriginStepRow(
    origin: RouteLocationUiState,
    step: RouteDetailStepUiState,
) {
    val markerColor = Color(0xFF16A34A)
    val defaultStateDescription = stringResource(id = R.string.route_setting_detail_step_state_default)
    val accessibilityDescription =
        buildString {
            append(step.indexLabel)
            append(' ')
            append(step.title)
            append(". ")
            append(origin.name)
            origin.supportingText?.takeIf(String::isNotBlank)?.let { supportingText ->
                append(". ")
                append(supportingText)
            }
            append(". ")
            append(step.description)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(EumSpacing.medium)
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityDescription
                    stateDescription = defaultStateDescription
                },
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        RouteDetailStepLeadingIcon(
            kind = step.kind,
            tone = RouteDetailTone.INFO,
            contentColor = markerColor,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = step.indexLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = origin.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            origin.supportingText?.takeIf(String::isNotBlank)?.let { supportingText ->
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteDetailFallbackRow(
    title: String,
    description: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(containerColor)
                .padding(EumSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RouteDetailStepRow(
    step: RouteDetailStepUiState,
) {
    val (containerColor, contentColor) = routeDetailToneColors(tone = step.tone)
    val (badgeContainerColor, badgeContentColor) =
        routeDetailToneColors(tone = step.badgeTone ?: step.tone)
    val cardColor =
        if (step.tone == RouteDetailTone.NEUTRAL) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            containerColor
        }
    val metaColor =
        if (step.tone == RouteDetailTone.NEUTRAL) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            contentColor
        }
    val stepStateDescription =
        when (step.tone) {
            RouteDetailTone.WARNING -> stringResource(id = R.string.route_setting_detail_step_state_warning)
            RouteDetailTone.INFO -> stringResource(id = R.string.route_setting_detail_step_state_info)
            RouteDetailTone.NEUTRAL -> stringResource(id = R.string.route_setting_detail_step_state_default)
        }
    val stepAccessibilityDescription =
        buildString {
            append(step.indexLabel)
            append(' ')
            append(step.title)
            append(". ")
            append(step.description)
            step.metaLabel?.let { metaLabel ->
                append(' ')
                append(metaLabel)
            }
            step.badgeLabel?.let { badgeLabel ->
                append(". ")
                append(badgeLabel)
            }
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(cardColor)
                .padding(EumSpacing.medium)
                .semantics(mergeDescendants = true) {
                    contentDescription = stepAccessibilityDescription
                    stateDescription = stepStateDescription
                },
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        RouteDetailStepLeadingIcon(
            kind = step.kind,
            tone = step.tone,
            contentColor = contentColor,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = step.indexLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            step.badgeLabel?.let { badgeLabel ->
                RouteBadgeChip(
                    label = badgeLabel,
                    containerColor = badgeContainerColor,
                    contentColor = badgeContentColor,
                )
            }
        }
        step.metaLabel?.let { metaLabel ->
            Text(
                text = metaLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = metaColor,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun RouteDetailStepLeadingIcon(
    kind: RouteDetailStepKind,
    tone: RouteDetailTone,
    contentColor: Color,
) {
    val (containerColor, iconTint) =
        when (kind) {
            RouteDetailStepKind.ARRIVAL ->
                MaterialTheme.colorScheme.error.copy(alpha = 0.12f) to MaterialTheme.colorScheme.error

            else -> contentColor.copy(alpha = 0.14f) to contentColor
        }

    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(RouteStandardCardCornerRadius),
        color = if (tone == RouteDetailTone.NEUTRAL) MaterialTheme.colorScheme.surface else containerColor,
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.16f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (kind) {
                RouteDetailStepKind.ARRIVAL -> {
                    Box(
                        modifier =
                            Modifier
                                .size(12.dp)
                                .background(color = iconTint, shape = CircleShape),
                    )
                }

                else -> {
                    Icon(
                        painter = painterResource(id = routeDetailStepIconRes(kind = kind)),
                        contentDescription = null,
                        tint =
                            if (kind.usesDirectionalStepIcon()) {
                                Color.Unspecified
                            } else {
                                iconTint
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteScreenTopBar(
    title: String,
    onBackClick: () -> Unit,
) {
    val policy = routeScreenTopBarPolicy()
    EumCenteredTopBar(
        title = title,
        onBackClick = if (policy.showBackButton) onBackClick else null,
        backContentDescription =
            if (policy.showBackButton) {
                stringResource(id = R.string.route_setting_back)
            } else {
                null
            },
        titleFontWeight = policy.titleFontWeight,
    )
}

internal data class RouteScreenTopBarPolicy(
    val showBackButton: Boolean,
    val titleFontWeight: FontWeight,
)

internal fun routeScreenTopBarPolicy(): RouteScreenTopBarPolicy =
    RouteScreenTopBarPolicy(
        showBackButton = true,
        titleFontWeight = FontWeight.SemiBold,
    )

internal data class RouteSettingLayoutPolicy(
    val allowsDefaultVerticalScroll: Boolean,
    val ctaPlacement: RouteSettingCtaPlacement,
    val mapHeightPolicy: RouteSettingMapHeightPolicy,
    val maxVisibleOptionCards: Int,
    val showsOptionSectionSupportingText: Boolean,
    val originLabel: String,
    val destinationLabel: String,
    val showsWaypointSwapButton: Boolean,
    val waypointMarkerStyle: RouteWaypointMarkerStyle,
    val waypointConnectorStyle: RouteWaypointConnectorStyle,
    val showsWaypointDivider: Boolean,
    val waypointSwapButtonStyle: RouteWaypointSwapButtonStyle,
    val optionContainer: RouteSettingOptionContainer,
    val travelModeTabShape: RouteSettingTravelModeTabShape,
    val visibleAccessibilityChipCount: Int,
    val mapFillsRemainingCenterSpace: Boolean,
    val bottomSheetEdgeToEdge: Boolean,
    val sheetContainerColor: RouteSettingSheetContainerColor,
    val showsRecommendedBadge: Boolean,
    val startCtaIcon: RouteSettingStartCtaIcon,
    val startCtaIconTint: RouteSettingCtaIconTint,
    val bottomSheetFlushToWindowBottom: Boolean,
    val sheetElevation: RouteSettingSheetElevation,
    val sheetBorder: RouteSettingSheetBorder,
    val optionCardContainerColor: RouteSettingOptionCardContainerColor,
    val optionDetailButtonChrome: RouteSettingOptionDetailButtonChrome,
    val optionDetailButtonAlignment: RouteSettingOptionDetailButtonAlignment,
    val walkTabIcon: RouteSettingTravelModeIcon,
    val transitTabIcon: RouteSettingTravelModeIcon,
    val travelModeActiveColor: RouteSettingTravelModeActiveColor,
    val travelModeInactiveColor: RouteSettingTravelModeInactiveColor,
    val travelModeIconSize: RouteSettingTravelModeIconSize,
)

internal enum class RouteSettingCtaPlacement {
    BottomBar,
}

internal enum class RouteSettingMapHeightPolicy {
    FillRemainingCenterSpace,
}

internal enum class RouteWaypointMarkerStyle {
    LinkedPin,
}

internal enum class RouteWaypointConnectorStyle {
    VerticalLine,
}

internal enum class RouteWaypointSwapButtonStyle {
    Borderless,
}

internal enum class RouteSettingOptionContainer {
    BottomSheet,
}

internal enum class RouteSettingTravelModeTabShape {
    SegmentedPill,
}

internal enum class RouteSettingTravelModeIcon {
    WalkImage,
    TransitImage,
}

internal enum class RouteSettingTravelModeActiveColor {
    PrimaryBlue,
}

internal enum class RouteSettingTravelModeInactiveColor {
    Grey700,
}

internal enum class RouteSettingTravelModeIconSize {
    Emphasized,
}

internal enum class RouteSettingSheetContainerColor {
    White,
}

internal enum class RouteSettingStartCtaIcon {
    NavigationPointer,
}

internal enum class RouteSettingCtaIconTint {
    OnPrimary,
}

internal enum class RouteSettingSheetElevation {
    None,
}

internal enum class RouteSettingSheetBorder {
    None,
}

internal enum class RouteSettingOptionCardContainerColor {
    White,
}

internal enum class RouteSettingOptionDetailButtonChrome {
    Borderless,
}

internal enum class RouteSettingOptionDetailButtonAlignment {
    RightCenter,
}

internal fun routeSettingLayoutPolicy(): RouteSettingLayoutPolicy =
    RouteSettingLayoutPolicy(
        allowsDefaultVerticalScroll = false,
        ctaPlacement = RouteSettingCtaPlacement.BottomBar,
        mapHeightPolicy = RouteSettingMapHeightPolicy.FillRemainingCenterSpace,
        maxVisibleOptionCards = MAX_VISIBLE_OPTION_CARD_COUNT,
        showsOptionSectionSupportingText = false,
        originLabel = "출발",
        destinationLabel = "도착",
        showsWaypointSwapButton = true,
        waypointMarkerStyle = RouteWaypointMarkerStyle.LinkedPin,
        waypointConnectorStyle = RouteWaypointConnectorStyle.VerticalLine,
        showsWaypointDivider = false,
        waypointSwapButtonStyle = RouteWaypointSwapButtonStyle.Borderless,
        optionContainer = RouteSettingOptionContainer.BottomSheet,
        travelModeTabShape = RouteSettingTravelModeTabShape.SegmentedPill,
        visibleAccessibilityChipCount = MAX_VISIBLE_ROUTE_CHIP_COUNT,
        mapFillsRemainingCenterSpace = true,
        bottomSheetEdgeToEdge = true,
        sheetContainerColor = RouteSettingSheetContainerColor.White,
        showsRecommendedBadge = false,
        startCtaIcon = RouteSettingStartCtaIcon.NavigationPointer,
        startCtaIconTint = RouteSettingCtaIconTint.OnPrimary,
        bottomSheetFlushToWindowBottom = false,
        sheetElevation = RouteSettingSheetElevation.None,
        sheetBorder = RouteSettingSheetBorder.None,
        optionCardContainerColor = RouteSettingOptionCardContainerColor.White,
        optionDetailButtonChrome = RouteSettingOptionDetailButtonChrome.Borderless,
        optionDetailButtonAlignment = RouteSettingOptionDetailButtonAlignment.RightCenter,
        walkTabIcon = RouteSettingTravelModeIcon.WalkImage,
        transitTabIcon = RouteSettingTravelModeIcon.TransitImage,
        travelModeActiveColor = RouteSettingTravelModeActiveColor.PrimaryBlue,
        travelModeInactiveColor = RouteSettingTravelModeInactiveColor.Grey700,
        travelModeIconSize = RouteSettingTravelModeIconSize.Emphasized,
    )

@Composable
private fun RouteWaypointCard(
    origin: RouteLocationUiState,
    originStatus: RouteOriginStatusUiState?,
    destination: RouteLocationUiState,
    supportingMessage: String?,
    onOriginClick: () -> Unit,
    onDestinationClick: () -> Unit,
    onSwapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val policy = routeSettingLayoutPolicy()
    val originPresentation = resolveOriginWaypointPresentation(
        name = origin.name,
        status = originStatus,
        supportingText = origin.supportingText,
    )
    val originLabel = stringResource(id = R.string.route_setting_origin_label)
    val destinationLabel = stringResource(id = R.string.route_setting_destination_label)
    val labelTextStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelColumnWidth =
        with(density) {
            maxOf(
                textMeasurer.measure(text = AnnotatedString(originLabel), style = labelTextStyle).size.width,
                textMeasurer.measure(text = AnnotatedString(destinationLabel), style = labelTextStyle).size.width,
            ).toDp()
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RouteSectionCardCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = EumSpacing.small,
                    vertical = RouteWaypointCardVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(RouteWaypointGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                    Modifier
                            .weight(1f)
                            .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(RouteWaypointIndicatorGap),
                    verticalAlignment = Alignment.Top,
                ) {
                    RouteWaypointLinkedMarkers(
                        originColor = RouteWaypointOriginColor,
                        destinationColor = RouteWaypointDestinationColor,
                        markerStyle = policy.waypointMarkerStyle,
                        connectorStyle = policy.waypointConnectorStyle,
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .padding(vertical = RouteWaypointMarkerColumnVerticalInset),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(RouteWaypointGap),
                    ) {
                        RouteWaypointRow(
                            label = originLabel,
                            name = originPresentation.name,
                            status = originPresentation.status,
                            supportingText = originPresentation.supportingText,
                            markerColor = RouteWaypointOriginColor,
                            labelWidth = labelColumnWidth,
                            onClick = onOriginClick,
                        )
                        if (policy.showsWaypointDivider) {
                            Spacer(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            )
                        }
                        RouteWaypointRow(
                            label = destinationLabel,
                            name = destination.name,
                            status = null,
                            supportingText = destination.supportingText,
                            markerColor = RouteWaypointDestinationColor,
                            labelWidth = labelColumnWidth,
                            onClick = onDestinationClick,
                        )
                    }
                }
                if (policy.showsWaypointSwapButton) {
                    RouteWaypointSwapButton(
                        onClick = onSwapClick,
                        style = policy.waypointSwapButtonStyle,
                    )
                }
            }
            supportingMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun resolveOriginWaypointPresentation(
    name: String,
    status: RouteOriginStatusUiState?,
    supportingText: String?,
): RouteWaypointPresentation {
    if (status?.label == CURRENT_LOCATION_WAYPOINT_NAME) {
        val resolvedSupportingText =
            (supportingText?.takeIf(String::isNotBlank) ?: name)
                .takeUnless { it == CURRENT_LOCATION_WAYPOINT_NAME }
        return RouteWaypointPresentation(
            name = CURRENT_LOCATION_WAYPOINT_NAME,
            supportingText = resolvedSupportingText,
            status = null,
        )
    }
    return RouteWaypointPresentation(
        name = name,
        supportingText = supportingText,
        status = status,
    )
}

private data class RouteWaypointPresentation(
    val name: String,
    val supportingText: String?,
    val status: RouteOriginStatusUiState?,
)

@Composable
private fun RouteWaypointLinkedMarkers(
    originColor: Color,
    destinationColor: Color,
    markerStyle: RouteWaypointMarkerStyle,
    connectorStyle: RouteWaypointConnectorStyle,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(RouteWaypointMarkerColumnWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RouteWaypointMarker(
            color = originColor,
            style = markerStyle,
        )
        Spacer(
            modifier =
                Modifier
                    .weight(1f)
                    .width(RouteWaypointConnectorWidth)
                    .background(
                        color =
                            when (connectorStyle) {
                                RouteWaypointConnectorStyle.VerticalLine -> RouteWaypointConnectorColor
                            },
                        shape = RoundedCornerShape(RouteWaypointConnectorWidth),
                    ),
        )
        RouteWaypointMarker(
            color = destinationColor,
            style = markerStyle,
        )
    }
}

@Composable
private fun RouteWaypointMarker(
    color: Color,
    style: RouteWaypointMarkerStyle,
    modifier: Modifier = Modifier,
) {
    when (style) {
        RouteWaypointMarkerStyle.LinkedPin -> RouteWaypointPinMarker(color = color, modifier = modifier)
    }
}

@Composable
private fun RouteWaypointPinMarker(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id = R.drawable.ic_route_waypoint_pin),
        contentDescription = null,
        modifier = modifier.size(width = RouteWaypointPinWidth, height = RouteWaypointPinHeight),
        contentScale = ContentScale.FillBounds,
        colorFilter = ColorFilter.tint(color),
    )
}

@Composable
private fun RouteWaypointRow(
    label: String,
    name: String,
    status: RouteOriginStatusUiState?,
    supportingText: String?,
    markerColor: Color,
    labelWidth: Dp,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(RouteWaypointSupportingGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RouteWaypointTextGap),
        ) {
            Text(
                text = label,
                modifier =
                    Modifier
                        .width(labelWidth)
                        .alignByBaseline(),
                style = MaterialTheme.typography.labelMedium,
                color = markerColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = name,
                modifier =
                    Modifier
                        .weight(1f)
                        .alignByBaseline(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        supportingText?.takeIf(String::isNotBlank)?.let { value ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RouteWaypointTextGap),
            ) {
                Spacer(modifier = Modifier.width(labelWidth))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(RouteWaypointSupportingGap),
                ) {
                    status?.let { uiState ->
                        RouteOriginStatusText(uiState = uiState)
                    }
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelSmall,
                        color = RouteWaypointSupportingTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (status != null && supportingText.isNullOrBlank() && shouldShowStandaloneOriginStatus(status = status, name = name)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RouteWaypointTextGap),
            ) {
                Spacer(modifier = Modifier.width(labelWidth))
                RouteOriginStatusText(uiState = status)
            }
        }
    }
}

@Composable
private fun RouteOriginStatusText(
    uiState: RouteOriginStatusUiState,
) {
    val contentColor =
        when (uiState.tone) {
            RouteOriginStatusTone.INFO -> MaterialTheme.colorScheme.primary
            RouteOriginStatusTone.WARNING -> MaterialTheme.colorScheme.tertiary
            RouteOriginStatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Text(
        text = uiState.label,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        fontWeight = FontWeight.Medium,
    )
}

private fun shouldShowStandaloneOriginStatus(
    status: RouteOriginStatusUiState,
    name: String,
): Boolean = !name.contains(status.label)

@Composable
private fun RouteWaypointSwapButton(
    onClick: () -> Unit,
    style: RouteWaypointSwapButtonStyle,
) {
    val a11yLabel = stringResource(id = R.string.route_setting_waypoint_swap)

    Box(
        modifier =
            Modifier
                .size(width = RouteWaypointSwapButtonWidth, height = RouteWaypointSwapButtonHeight)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = a11yLabel
                },
        contentAlignment = Alignment.Center,
    ) {
        RouteWaypointSwapIcon(
            color =
                when (style) {
                    RouteWaypointSwapButtonStyle.Borderless -> MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun RouteWaypointSwapIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = RouteWaypointSwapIconWidth, height = RouteWaypointSwapIconHeight)) {
        val strokeWidth = RouteWaypointSwapIconStrokeWidth.toPx()
        routeWaypointSwapIconSegments(size).forEach { segment ->
            drawLine(
                color = color,
                start = segment.start,
                end = segment.end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

internal data class RouteWaypointSwapSegment(
    val start: Offset,
    val end: Offset,
)

internal fun routeWaypointSwapIconSegments(size: Size): List<RouteWaypointSwapSegment> {
    val arrowHeadHalfWidth = size.width * 0.2f
    val upperStemX = size.width * 0.23f
    val lowerStemX = size.width * 0.77f
    val upperStemTopY = size.height * 0.12f
    val upperStemBottomY = size.height * 0.36f
    val upperHeadBaseY = size.height * 0.26f
    val lowerStemTopY = size.height * 0.68f
    val lowerStemBottomY = size.height * 0.92f
    val lowerHeadBaseY = size.height * 0.78f

    return listOf(
        RouteWaypointSwapSegment(
            start = Offset(x = upperStemX, y = upperStemBottomY),
            end = Offset(x = upperStemX, y = upperStemTopY),
        ),
        RouteWaypointSwapSegment(
            start = Offset(x = upperStemX, y = upperStemTopY),
            end = Offset(x = upperStemX - arrowHeadHalfWidth, y = upperHeadBaseY),
        ),
        RouteWaypointSwapSegment(
            start = Offset(x = upperStemX, y = upperStemTopY),
            end = Offset(x = upperStemX + arrowHeadHalfWidth, y = upperHeadBaseY),
        ),
        RouteWaypointSwapSegment(
            start = Offset(x = lowerStemX, y = lowerStemTopY),
            end = Offset(x = lowerStemX, y = lowerStemBottomY),
        ),
        RouteWaypointSwapSegment(
            start = Offset(x = lowerStemX, y = lowerStemBottomY),
            end = Offset(x = lowerStemX - arrowHeadHalfWidth, y = lowerHeadBaseY),
        ),
        RouteWaypointSwapSegment(
            start = Offset(x = lowerStemX, y = lowerStemBottomY),
            end = Offset(x = lowerStemX + arrowHeadHalfWidth, y = lowerHeadBaseY),
        ),
    )
}

@Composable
private fun RouteTravelModeTabs(
    selectedMode: RouteTravelMode,
    onModeSelected: (RouteTravelMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
    ) {
        RouteTravelModeTab(
            label = stringResource(id = R.string.route_setting_travel_mode_walk),
            iconResId = R.drawable.ic_route_mode_walk,
            iconSize = RouteTravelModeWalkTabIconSize,
            isSelected = selectedMode == RouteTravelMode.WALK,
            modifier = Modifier.weight(1f),
            onClick = { onModeSelected(RouteTravelMode.WALK) },
        )
        RouteTravelModeTab(
            label = stringResource(id = R.string.route_setting_travel_mode_transit),
            iconResId = R.drawable.ic_route_mode_transit,
            iconSize = RouteTravelModeTransitTabIconSize,
            isSelected = selectedMode == RouteTravelMode.TRANSIT,
            modifier = Modifier.weight(1f),
            onClick = { onModeSelected(RouteTravelMode.TRANSIT) },
        )
    }
}

@Composable
private fun RouteTravelModeTab(
    label: String,
    iconResId: Int,
    iconSize: Dp,
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
        shape = RoundedCornerShape(RouteButtonCornerRadius),
        color =
            if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
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
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = RouteTravelModeTabVerticalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = routeTravelModeTabContentColor(isSelected = isSelected),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = routeTravelModeTabContentColor(isSelected = isSelected),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun routeTravelModeTabContentColor(isSelected: Boolean): Color =
    if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        RouteTravelModeInactiveContentColor
    }

@Composable
private fun RouteMapStage(
    uiState: RouteSettingUiState,
    modifier: Modifier = Modifier,
) {
    val selectedRoute = uiState.selectedRoute
    val previewMap = uiState.routePreviewMap
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RouteSectionCardCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxSize(),
        ) {
            RouteMapBackdrop(
                previewMap = previewMap,
                routePath =
                    if (previewMap.isDisplayable) {
                        previewMap.polyline
                    } else {
                        emptyList()
                    },
                modifier = Modifier.fillMaxSize(),
            )

            when {
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
private fun RouteMapMessageCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.wrapContentWidth(),
        shape = RoundedCornerShape(RouteSectionCardCornerRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shadowElevation = RouteOverlayCardElevation,
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
    EumMapFloatingControls(
        actionButtonState =
            EumMapFloatingActionButtonState(
                iconRes = R.drawable.ic_route_start_navigation_button,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(id = R.string.route_setting_map_control_recenter),
                enabled = true,
            ),
        onActionClick = {},
        modifier = modifier,
        onZoomInClick = {},
        onZoomOutClick = {},
    )
}

@Composable
private fun RouteSettingRouteSheet(
    uiState: RouteSettingUiState,
    onOptionClick: (RouteOption) -> Unit,
    onOptionDetailClick: (RouteOption) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                topStart = RouteBottomSheetTopCornerRadius,
                topEnd = RouteBottomSheetTopCornerRadius,
                bottomStart = 0.dp,
                bottomEnd = 0.dp,
            ),
        color = Color.White,
        shadowElevation = RouteBottomSheetElevation,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = EumSpacing.small,
                    vertical = RouteSettingSheetVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(RouteSettingSheetGap),
        ) {
            RouteOptionSection(
                uiState = uiState,
                onOptionClick = onOptionClick,
                onOptionDetailClick = onOptionDetailClick,
            )
        }
    }
}

@Composable
private fun RouteOptionSection(
    uiState: RouteSettingUiState,
    onOptionClick: (RouteOption) -> Unit,
    onOptionDetailClick: (RouteOption) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(RouteOptionCardGap),
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
                uiState.optionCards.take(MAX_VISIBLE_OPTION_CARD_COUNT).forEach { optionCard ->
                    RouteCompactOptionCard(
                        card = optionCard,
                        onClick = { onOptionClick(optionCard.routeOption) },
                        onDetailClick = { onOptionDetailClick(optionCard.routeOption) },
                    )
                }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteCompactOptionCard(
    card: RouteOptionCardUiState,
    onClick: () -> Unit,
    onDetailClick: () -> Unit,
) {
    val accentColor = optionAccentColor(card.routeOption)
    val titleColor = if (card.isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val containerColor = Color.White
    val borderColor = if (card.isSelected) accentColor.copy(alpha = 0.72f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
    val detailContentDescription =
        stringResource(
            id = R.string.route_setting_card_detail_a11y,
            card.title,
        )
    val cardContentDescription =
        stringResource(
            id =
                if (card.isSelected) {
                    R.string.route_setting_card_a11y_selected
                } else {
                    R.string.route_setting_card_a11y_available
                },
            card.title,
            card.summaryLabel,
        )

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    selected = card.isSelected
                    stateDescription = card.selectionLabel
                    contentDescription = cardContentDescription
                }
                .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(RouteStandardCardCornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = EumSpacing.small,
                        vertical = RouteOptionCardVerticalPadding,
                    ),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RouteOptionSelectionIndicator(
                isSelected = card.isSelected,
                accentColor = accentColor,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RouteOptionPrefixBadge(
                            label = routeOptionCompactPrefix(card.routeOption),
                            accentColor = accentColor,
                        )
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = titleColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = compactEstimatedTimeLabel(card.estimatedTimeMinutes),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                        Text(
                            text = compactDistanceLabel(card.distanceMeters),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                    ) {
                        RouteRiskChip(riskLevel = card.riskLevel)
                        card.badges
                            .take(MAX_COMPACT_ACCESSIBILITY_BADGE_COUNT)
                            .forEach { badge ->
                                RouteBadgeChip(label = routeBadgeText(badge))
                            }
                        val overflowCount = card.badges.size - MAX_COMPACT_ACCESSIBILITY_BADGE_COUNT
                        if (overflowCount > 0) {
                            RouteBadgeChip(
                                label = stringResource(id = R.string.route_setting_card_badge_overflow, overflowCount),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
            }
            RouteOptionDetailArrowButton(
                a11yLabel = detailContentDescription,
                accentColor = accentColor,
                onClick = onDetailClick,
            )
        }
    }
}

@Composable
private fun RouteOptionPrefixBadge(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = accentColor,
    )
}

@Composable
private fun RouteOptionDetailArrowButton(
    a11yLabel: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            Modifier
                .size(RouteOptionDetailButtonTouchTargetSize)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics {
                    contentDescription = a11yLabel
                },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_control_next),
            contentDescription = null,
            modifier = Modifier.size(RouteOptionDetailButtonIconSize),
            tint = accentColor,
        )
    }
}

/*
@Composable
private fun RouteOptionDetailButton(
    a11yLabel: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(RouteOptionDetailButtonTouchTargetSize)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = a11yLabel
                },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "›",
                color = accentColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
*/

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
private fun RouteMetricTile(
    label: String,
    value: String,
    accentColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(RouteStandardCardCornerRadius),
        color =
            if (isSelected) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        border =
            BorderStroke(
                1.dp,
                if (isSelected) accentColor.copy(alpha = 0.32f) else MaterialTheme.colorScheme.outlineVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = EumSpacing.small, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RouteSettingBottomBar(
    buttonLabel: String,
    enabled: Boolean,
    supportingText: String?,
    selectedRoute: RouteSelectedRouteUiState?,
    onStartClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        RouteSettingCtaContent(
            buttonLabel = buttonLabel,
            enabled = enabled,
            supportingText = supportingText,
            selectedRoute = selectedRoute,
            buttonHeight = RouteSettingBottomBarButtonHeight,
            verticalGap = EumSpacing.small,
            compactSupportingText = false,
            onStartClick = onStartClick,
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(EumSpacing.medium),
        )
    }
}

@Composable
private fun RouteSettingCtaContent(
    buttonLabel: String,
    enabled: Boolean,
    supportingText: String?,
    selectedRoute: RouteSelectedRouteUiState?,
    buttonHeight: Dp,
    verticalGap: Dp,
    compactSupportingText: Boolean,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctaContentDescription =
        when {
            enabled && selectedRoute != null ->
                stringResource(
                    id = R.string.route_setting_cta_a11y_enabled,
                    buttonLabel,
                    selectedRoute.optionTitle,
                    selectedRoute.summaryLabel,
                )

            supportingText != null ->
                stringResource(
                    id = R.string.route_setting_cta_a11y_disabled,
                    buttonLabel,
                    supportingText,
                )

            else -> buttonLabel
        }
    val ctaStateDescription =
        if (enabled) {
            stringResource(id = R.string.route_setting_cta_state_enabled)
        } else {
            supportingText ?: stringResource(id = R.string.route_setting_cta_state_disabled)
        }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(verticalGap),
    ) {
        supportingText?.let { text ->
            Text(
                text = text,
                style =
                    if (compactSupportingText) {
                        MaterialTheme.typography.labelSmall
                    } else {
                        MaterialTheme.typography.bodySmall
                    },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (compactSupportingText) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onStartClick,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
                    .semantics {
                        contentDescription = ctaContentDescription
                        stateDescription = ctaStateDescription
                    },
            shape = RoundedCornerShape(RouteButtonCornerRadius),
            elevation =
                ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    disabledElevation = 0.dp,
                ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_route_start_navigation_button),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(EumSpacing.xSmall))
            Text(text = buttonLabel)
        }
    }
}

@Composable
private fun RouteMapBackdrop(
    previewMap: RoutePreviewMapUiState,
    routePath: List<GeoCoordinate>,
    modifier: Modifier = Modifier,
) {
    val mapDescription = stringResource(id = R.string.route_setting_preview_title)
    MapOverlayViewport(
        overlayState =
            createRoutePreviewViewportOverlayState(
                previewMap =
                    previewMap.copy(
                        polyline =
                            if (routePath.isNotEmpty()) {
                                routePath
                            } else {
                                previewMap.polyline
                            },
                    ),
            ),
        modifier = modifier,
        contentDescription = mapDescription,
    )
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
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) to MaterialTheme.colorScheme.primary

            RouteRiskLevel.MEDIUM ->
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.tertiary

            RouteRiskLevel.HIGH ->
                MaterialTheme.colorScheme.error.copy(alpha = 0.10f) to MaterialTheme.colorScheme.error
        }

    RouteBadgeChip(
        label = riskLevelText(riskLevel = riskLevel),
        containerColor = containerColor,
        contentColor = contentColor,
        borderColor = contentColor.copy(alpha = 0.22f),
    )
}

@Composable
private fun RouteBadgeChip(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.26f),
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(RouteCompactChipCornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
    ) {
        Text(
            text = label,
            modifier =
                Modifier.padding(
                    horizontal = EumSpacing.small,
                    vertical = EumSpacing.xSmall,
                ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
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
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RouteStandardCardCornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
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
            if (actionLabel != null && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    Text(text = actionLabel)
                }
            }
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
private fun routeOptionBadgeColors(badge: RouteOptionBadge): Pair<Color, Color> =
    when (badge) {
        RouteOptionBadge.CURB_GAP,
        RouteOptionBadge.UNSIGNALIZED_CROSSWALK,
            ->
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.34f) to MaterialTheme.colorScheme.tertiary

        RouteOptionBadge.SAFE_PRIORITY,
        RouteOptionBadge.STEP_FREE,
        RouteOptionBadge.AUDIO_SIGNAL,
        RouteOptionBadge.BRAILLE_BLOCK,
        RouteOptionBadge.SIGNAL_CROSSWALK,
            ->
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f) to MaterialTheme.colorScheme.primary
    }

@Composable
private fun optionAccentColor(routeOption: RouteOption): Color =
    when (routeOption) {
        RouteOption.SAFE -> MaterialTheme.colorScheme.primary
        RouteOption.SHORTEST -> MaterialTheme.colorScheme.tertiary
        RouteOption.RECOMMENDED -> MaterialTheme.colorScheme.primary
        RouteOption.MIN_TRANSFER -> MaterialTheme.colorScheme.secondary
        RouteOption.MIN_WALK -> MaterialTheme.colorScheme.tertiary
    }

@Composable
private fun routeDetailToneColors(tone: RouteDetailTone): Pair<Color, Color> =
    when (tone) {
        RouteDetailTone.NEUTRAL ->
            MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurfaceVariant

        RouteDetailTone.INFO ->
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f) to MaterialTheme.colorScheme.primary

        RouteDetailTone.WARNING ->
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.36f) to MaterialTheme.colorScheme.tertiary
    }

private fun routeDetailStepIconRes(kind: RouteDetailStepKind): Int =
    when (kind) {
        RouteDetailStepKind.START,
        RouteDetailStepKind.STRAIGHT,
        RouteDetailStepKind.FALLBACK,
            -> R.drawable.ic_direction_straight

        RouteDetailStepKind.TURN_LEFT -> R.drawable.ic_direction_turn_left
        RouteDetailStepKind.TURN_RIGHT -> R.drawable.ic_direction_turn_right

        RouteDetailStepKind.TACTILE_GUIDE -> R.drawable.ic_route_tactile_blocks
        RouteDetailStepKind.CROSSWALK -> R.drawable.ic_direction_crosswalk
        RouteDetailStepKind.ELEVATOR -> R.drawable.ic_route_elevator
        RouteDetailStepKind.CONSTRUCTION -> R.drawable.ic_route_construction
        RouteDetailStepKind.CURB_GAP -> R.drawable.ic_status_warning
        RouteDetailStepKind.STAIRS -> R.drawable.ic_route_stairs
        RouteDetailStepKind.ARRIVAL -> R.drawable.ic_status_check
    }

private fun RouteDetailStepKind.usesDirectionalStepIcon(): Boolean =
    this == RouteDetailStepKind.START ||
        this == RouteDetailStepKind.STRAIGHT ||
        this == RouteDetailStepKind.TURN_LEFT ||
        this == RouteDetailStepKind.CROSSWALK ||
        this == RouteDetailStepKind.TURN_RIGHT ||
        this == RouteDetailStepKind.FALLBACK

private fun routeDetailChipIconRes(kind: RouteDetailChipKind): Int =
    when (kind) {
        RouteDetailChipKind.STEP_FREE -> R.drawable.ic_status_safe_info
        RouteDetailChipKind.ELEVATOR -> R.drawable.ic_route_elevator
        RouteDetailChipKind.AUDIO_SIGNAL -> R.drawable.ic_status_safe_info
        RouteDetailChipKind.BRAILLE_BLOCK -> R.drawable.ic_route_tactile_blocks
        RouteDetailChipKind.CONSTRUCTION -> R.drawable.ic_route_construction
        RouteDetailChipKind.SIGNAL_CROSSWALK -> R.drawable.ic_route_crosswalk
        RouteDetailChipKind.UNSIGNALIZED_CROSSWALK -> R.drawable.ic_status_warning
        RouteDetailChipKind.CURB_GAP -> R.drawable.ic_status_warning
        RouteDetailChipKind.STAIRS -> R.drawable.ic_route_stairs
        RouteDetailChipKind.PENDING -> R.drawable.ic_status_neutral
    }

@Composable
private fun RouteOption.routeOptionTitle(): String =
    when (this) {
        RouteOption.SAFE -> stringResource(id = R.string.route_setting_option_safe_title)
        RouteOption.SHORTEST -> stringResource(id = R.string.route_setting_option_shortest_title)
        RouteOption.RECOMMENDED -> "추천 경로"
        RouteOption.MIN_TRANSFER -> "최소 환승"
        RouteOption.MIN_WALK -> "최소 도보"
    }

@Composable
private fun routeOptionCompactPrefix(routeOption: RouteOption): String =
    when (routeOption) {
        RouteOption.SAFE -> stringResource(id = R.string.route_setting_card_recommended_route)
        RouteOption.SHORTEST -> stringResource(id = R.string.route_setting_card_alternative_route)
        RouteOption.RECOMMENDED -> "추천 경로"
        RouteOption.MIN_TRANSFER -> "최소 환승 경로"
        RouteOption.MIN_WALK -> "최소 도보 경로"
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
private const val MAX_VISIBLE_OPTION_CARD_COUNT = 3
private const val MAX_VISIBLE_ROUTE_CHIP_COUNT = 2
private const val MAX_COMPACT_ACCESSIBILITY_BADGE_COUNT = 1
private const val CURRENT_LOCATION_WAYPOINT_NAME = "현재 위치"
private const val DEFAULT_PREVIEW_CENTER_LATITUDE = 35.1796
private const val DEFAULT_PREVIEW_CENTER_LONGITUDE = 129.0756
private val RouteStandardCardCornerRadius = 12.dp
private val RouteSectionCardCornerRadius = 16.dp
private val RouteButtonCornerRadius = 12.dp
private val RouteCompactChipCornerRadius = 8.dp
private val RouteBottomSheetTopCornerRadius = 16.dp
private val RouteFloatingControlCornerRadius = 24.dp
private val RouteOverlayCardElevation = 6.dp
private val RouteFloatingControlElevation = 6.dp
private val RouteBottomSheetElevation = 6.dp
private val RouteTravelModeInactiveContentColor = Color(0xFF374151)
private val RouteSettingScreenVerticalPadding = 8.dp
private val RouteSettingContentGap = 6.dp
private val RouteWaypointCardVerticalPadding = 8.dp
private val RouteWaypointGap = 4.dp
private val RouteWaypointIndicatorGap = 12.dp
private val RouteWaypointTextGap = 8.dp
private val RouteWaypointSupportingGap = 3.dp
private val RouteWaypointMarkerColumnWidth = 24.dp
private val RouteWaypointMarkerColumnVerticalInset = 5.dp
private val RouteWaypointConnectorWidth = 2.dp
private val RouteWaypointPinWidth = 22.dp
private val RouteWaypointPinHeight = 24.dp
private val RouteWaypointSwapButtonWidth = 36.dp
private val RouteWaypointSwapButtonHeight = 56.dp
private val RouteWaypointSwapIconWidth = 18.dp
private val RouteWaypointSwapIconHeight = 24.dp
private val RouteWaypointSwapIconStrokeWidth = 2.25.dp
private val RouteWaypointOriginColor = Color(0xFF16A34A)
private val RouteWaypointDestinationColor = Color(0xFFEF4444)
private val RouteWaypointConnectorColor = Color(0xFFD9E2EF)
private val RouteWaypointSupportingTextColor = Color(0xFF6B7280)
private val RouteTravelModeTabVerticalPadding = 7.dp
private val RouteTravelModeWalkTabIconSize = 30.dp
private val RouteTravelModeTransitTabIconSize = 28.dp
private val RouteMapControlButtonSize = 36.dp
private val RouteSettingSheetVerticalPadding = 8.dp
private val RouteSettingSheetGap = 6.dp
private val RouteOptionCardGap = 6.dp
private val RouteOptionCardVerticalPadding = 8.dp
private val RouteOptionDetailButtonTouchTargetSize = 48.dp
private val RouteOptionDetailButtonIconSize = 24.dp
private val RouteSettingBottomBarButtonHeight = 56.dp
private val RoutePreviewMarkerSize = 38.dp
private const val MIN_ROUTE_PREVIEW_LATITUDE_SPAN = 0.0035
private const val MIN_ROUTE_PREVIEW_LONGITUDE_SPAN = 0.0045
