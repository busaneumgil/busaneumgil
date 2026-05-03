package com.ssafy.e102.eumgil.feature.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumCenteredTopBar
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.GeoCoordinate

@Composable
fun NavigationScreen(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            NavigationTopBar(
                onBackClick = { onAction(NavigationUiAction.BackClicked) },
            )
        },
        bottomBar = {
            NavigationBottomBar(
                uiState = uiState,
                onAction = onAction,
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
            NavigationHeroCard(
                uiState = uiState,
                onAction = onAction,
            )
            NavigationMapStage(
                uiState = uiState,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavigationTopBar(
    onBackClick: () -> Unit,
) {
    val policy = navigationTopBarPolicy()
    EumCenteredTopBar(
        title = stringResource(id = R.string.navigation_screen_title),
        onBackClick = if (policy.showBackButton) onBackClick else null,
        backContentDescription =
            if (policy.showBackButton) {
                stringResource(id = R.string.navigation_back)
            } else {
                null
            },
        titleFontWeight = policy.titleFontWeight,
    )
}

internal data class NavigationTopBarPolicy(
    val showBackButton: Boolean,
    val showBookmarkAction: Boolean,
    val titleFontWeight: FontWeight,
)

internal fun navigationTopBarPolicy(): NavigationTopBarPolicy =
    NavigationTopBarPolicy(
        showBackButton = true,
        showBookmarkAction = false,
        titleFontWeight = FontWeight.SemiBold,
    )

@Composable
private fun NavigationHeroCard(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(EumSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            NavigationHeroMarker()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Surface(
                    shape = RoundedCornerShape(EumRadius.full),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = uiState.stepCard.statusLabel,
                        modifier =
                            Modifier.padding(
                                horizontal = EumSpacing.small,
                                vertical = EumSpacing.xSmall,
                            ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text = uiState.stepCard.instruction,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = "${uiState.stepCard.distanceLabel} · ${uiState.stepCard.emphasisLabel}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                )
                Text(
                    text = uiState.stepCard.supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                )
                if (uiState.tts.fallbackMessage.isNotBlank() && !uiState.tts.canRequestBriefing) {
                    Text(
                        text = uiState.tts.fallbackMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                    )
                }
            }
            NavigationVoiceControl(
                uiState = uiState,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun NavigationHeroMarker() {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(id = R.string.navigation_hero_marker),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun NavigationVoiceControl(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
) {
    val primaryLabel =
        if (uiState.tts.isEnabled) {
            stringResource(id = R.string.navigation_voice_button_label)
        } else {
            stringResource(id = R.string.navigation_voice_enable_button_label)
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
    ) {
        Surface(
            modifier =
                Modifier
                    .size(width = 88.dp, height = 96.dp)
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            when {
                                uiState.tts.canRequestBriefing ->
                                    onAction(NavigationUiAction.BriefingReplayClicked)

                                !uiState.tts.isEnabled ->
                                    onAction(
                                        NavigationUiAction.VoiceGuidanceToggled(
                                            enabled = true,
                                        ),
                                    )

                                else -> Unit
                            }
                        },
                    ),
            shape = RoundedCornerShape(EumRadius.large),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(EumSpacing.small),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.navigation_voice_button_icon),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = primaryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        TextButton(
            onClick = {
                onAction(
                    NavigationUiAction.VoiceGuidanceToggled(
                        enabled = !uiState.tts.isEnabled,
                    ),
                )
            },
        ) {
            Text(
                text =
                    if (uiState.tts.isEnabled) {
                        stringResource(id = R.string.navigation_voice_toggle_disable)
                    } else {
                        stringResource(id = R.string.navigation_voice_toggle_enable)
                    },
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun NavigationMapStage(
    uiState: NavigationUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            NavigationMapBackdrop(
                mapOverlay = uiState.mapOverlay,
                modifier = Modifier.fillMaxSize(),
            )
            if (uiState.mapOverlay.shouldUsePlaceholder) {
                NavigationMapMessageCard(
                    title = uiState.mapPlaceholderTitle,
                    description = uiState.mapPlaceholderDescription,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(EumSpacing.medium),
                )
            }
            NavigationMapControls(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = EumSpacing.small),
            )
        }
    }
}

@Composable
private fun NavigationMapBackdrop(
    mapOverlay: NavigationMapOverlayUiState,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    val currentColor = MaterialTheme.colorScheme.primary
    val originColor = MaterialTheme.colorScheme.secondary
    val destinationColor = MaterialTheme.colorScheme.error
    val projectionBounds = navigationProjectionBounds(mapOverlay)
    val mapDescription = stringResource(id = R.string.navigation_map_section_title)
    val backgroundBrush =
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
        )

    BoxWithConstraints(
        modifier =
            modifier
                .background(backgroundBrush)
                .semantics {
                    contentDescription = mapDescription
                },
    ) {
        val horizontalPadding = 28.dp
        val verticalPadding = 24.dp
        val markerAreaWidth = (maxWidth - (horizontalPadding * 2)).coerceAtLeast(0.dp)
        val markerAreaHeight = (maxHeight - (verticalPadding * 2)).coerceAtLeast(0.dp)

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawNavigationMapGrid(outline = outline)
            if (mapOverlay.selectedRoutePolyline.size >= 2) {
                val routePreviewPath =
                    mapOverlay.selectedRoutePolyline.toNavigationPreviewPath(
                        bounds = projectionBounds,
                        canvasSize = size,
                    )
                drawPath(
                    path = routePreviewPath,
                    color = currentColor.copy(alpha = 0.22f),
                    style =
                        Stroke(
                            width = 12.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                )
                drawPath(
                    path = routePreviewPath,
                    color = currentColor,
                    style =
                        Stroke(
                            width = 5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                )
            }
            mapOverlay.origin?.let { point ->
                drawCircle(
                    color = originColor.copy(alpha = 0.18f),
                    radius = 18.dp.toPx(),
                    center = projectionBounds.project(point.coordinate).toCanvasOffset(size),
                )
            }
            mapOverlay.destination?.let { point ->
                drawCircle(
                    color = destinationColor.copy(alpha = 0.16f),
                    radius = 20.dp.toPx(),
                    center = projectionBounds.project(point.coordinate).toCanvasOffset(size),
                )
            }
            mapOverlay.currentLocation?.let { point ->
                drawCircle(
                    color = currentColor.copy(alpha = 0.16f),
                    radius = 16.dp.toPx(),
                    center = projectionBounds.project(point.coordinate).toCanvasOffset(size),
                )
            }
        }

        mapOverlay.origin?.let { point ->
            NavigationMapMarker(
                label = stringResource(id = R.string.navigation_map_marker_origin),
                containerColor = originColor,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .offsetWithinNavigationMap(
                            point = projectionBounds.project(point.coordinate),
                            areaWidth = markerAreaWidth,
                            areaHeight = markerAreaHeight,
                            horizontalPadding = horizontalPadding,
                            verticalPadding = verticalPadding,
                            elementSize = NavigationMapMarkerSize,
                        ),
            )
        }
        mapOverlay.destination?.let { point ->
            NavigationMapMarker(
                label = stringResource(id = R.string.navigation_map_marker_destination),
                containerColor = destinationColor,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .offsetWithinNavigationMap(
                            point = projectionBounds.project(point.coordinate),
                            areaWidth = markerAreaWidth,
                            areaHeight = markerAreaHeight,
                            horizontalPadding = horizontalPadding,
                            verticalPadding = verticalPadding,
                            elementSize = NavigationMapMarkerSize,
                        ),
            )
        }
        mapOverlay.currentLocation?.let { point ->
            NavigationMapMarker(
                label = stringResource(id = R.string.navigation_map_marker_current),
                containerColor = currentColor,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .offsetWithinNavigationMap(
                            point = projectionBounds.project(point.coordinate),
                            areaWidth = markerAreaWidth,
                            areaHeight = markerAreaHeight,
                            horizontalPadding = horizontalPadding,
                            verticalPadding = verticalPadding,
                            elementSize = NavigationMapMarkerSize,
                        ),
            )
        }
    }
}

@Composable
private fun NavigationMapMarker(
    label: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(NavigationMapMarkerSize),
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
private fun NavigationMapMessageCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EumRadius.medium),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)),
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
private fun NavigationMapControls(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
        horizontalAlignment = Alignment.End,
    ) {
        NavigationMapControlButton(label = stringResource(id = R.string.navigation_map_control_zoom_in))
        NavigationMapControlButton(label = stringResource(id = R.string.navigation_map_control_zoom_out))
        NavigationMapControlButton(
            label = stringResource(id = R.string.navigation_map_control_recenter),
            isWide = true,
        )
    }
}

@Composable
private fun NavigationMapControlButton(
    label: String,
    isWide: Boolean = false,
) {
    Surface(
        modifier =
            Modifier
                .width(if (isWide) 56.dp else 48.dp)
                .height(48.dp)
                .clickable(
                    role = Role.Button,
                    onClick = {},
                ),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun NavigationBottomBar(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 10.dp,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(EumRadius.large),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)),
            ) {
                Row(
                    modifier = Modifier.padding(EumSpacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavigationSummaryMetric(
                        title = stringResource(id = R.string.navigation_summary_eta_title),
                        value = uiState.remainingEtaLabel,
                        modifier = Modifier.weight(1f),
                    )
                    NavigationSummaryMetric(
                        title = stringResource(id = R.string.navigation_summary_distance_title),
                        value = uiState.remainingDistanceLabel,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { onAction(NavigationUiAction.RouteDetailClicked) },
                        enabled = uiState.canOpenRouteDetail,
                        shape = RoundedCornerShape(EumRadius.medium),
                    ) {
                        Text(text = stringResource(id = R.string.navigation_detail_button_label))
                    }
                }
            }
            Button(
                onClick = { onAction(NavigationUiAction.ExitNavigationClicked) },
                enabled = uiState.isExitEnabled,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.36f),
                        disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.7f),
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = uiState.exitCta.label)
            }
            if (!uiState.isExitEnabled) {
                Text(
                    text = uiState.exitCta.supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NavigationSummaryMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun DrawScope.drawNavigationMapGrid(outline: Color) {
    val verticalStep = size.width / 5f
    val horizontalStep = size.height / 6f
    val strokeWidth = 1.dp.toPx()

    for (index in 0..5) {
        val x = index * verticalStep
        drawLine(
            color = outline.copy(alpha = 0.16f),
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

private fun List<GeoCoordinate>.toNavigationPreviewPath(
    bounds: NavigationProjectionBounds,
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

private fun NavigationProjectionPoint.toCanvasOffset(size: Size): Offset =
    Offset(
        x = size.width * xRatio,
        y = size.height * yRatio,
    )

private fun Modifier.offsetWithinNavigationMap(
    point: NavigationProjectionPoint,
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

private fun navigationProjectionBounds(mapOverlay: NavigationMapOverlayUiState): NavigationProjectionBounds {
    val coordinates =
        buildList {
            addAll(mapOverlay.selectedRoutePolyline)
            mapOverlay.currentLocation?.let { point -> add(point.coordinate) }
            mapOverlay.origin?.let { point -> add(point.coordinate) }
            mapOverlay.destination?.let { point -> add(point.coordinate) }
        }

    if (coordinates.isEmpty()) {
        return NavigationProjectionBounds(
            minLatitude = DEFAULT_NAVIGATION_CENTER_LATITUDE - (MIN_NAVIGATION_LATITUDE_SPAN / 2.0),
            maxLatitude = DEFAULT_NAVIGATION_CENTER_LATITUDE + (MIN_NAVIGATION_LATITUDE_SPAN / 2.0),
            minLongitude = DEFAULT_NAVIGATION_CENTER_LONGITUDE - (MIN_NAVIGATION_LONGITUDE_SPAN / 2.0),
            maxLongitude = DEFAULT_NAVIGATION_CENTER_LONGITUDE + (MIN_NAVIGATION_LONGITUDE_SPAN / 2.0),
        )
    }

    val latitudeBounds =
        expandedNavigationBounds(
            minValue = coordinates.minOf { coordinate -> coordinate.latitude },
            maxValue = coordinates.maxOf { coordinate -> coordinate.latitude },
            minimumSpan = MIN_NAVIGATION_LATITUDE_SPAN,
        )
    val longitudeBounds =
        expandedNavigationBounds(
            minValue = coordinates.minOf { coordinate -> coordinate.longitude },
            maxValue = coordinates.maxOf { coordinate -> coordinate.longitude },
            minimumSpan = MIN_NAVIGATION_LONGITUDE_SPAN,
        )

    return NavigationProjectionBounds(
        minLatitude = latitudeBounds.first,
        maxLatitude = latitudeBounds.second,
        minLongitude = longitudeBounds.first,
        maxLongitude = longitudeBounds.second,
    )
}

private fun expandedNavigationBounds(
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

private data class NavigationProjectionBounds(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
) {
    private val latitudeSpan: Double
        get() = (maxLatitude - minLatitude).coerceAtLeast(MIN_NAVIGATION_LATITUDE_SPAN)

    private val longitudeSpan: Double
        get() = (maxLongitude - minLongitude).coerceAtLeast(MIN_NAVIGATION_LONGITUDE_SPAN)

    fun project(coordinate: GeoCoordinate): NavigationProjectionPoint {
        val longitudeRatio =
            ((coordinate.longitude - minLongitude) / longitudeSpan)
                .toFloat()
                .coerceIn(0.08f, 0.92f)
        val latitudeRatio =
            (1f - ((coordinate.latitude - minLatitude) / latitudeSpan).toFloat())
                .coerceIn(0.1f, 0.9f)

        return NavigationProjectionPoint(
            xRatio = longitudeRatio,
            yRatio = latitudeRatio,
        )
    }
}

private data class NavigationProjectionPoint(
    val xRatio: Float,
    val yRatio: Float,
)

private const val DEFAULT_NAVIGATION_CENTER_LATITUDE = 35.1796
private const val DEFAULT_NAVIGATION_CENTER_LONGITUDE = 129.0756
private const val MIN_NAVIGATION_LATITUDE_SPAN = 0.0035
private const val MIN_NAVIGATION_LONGITUDE_SPAN = 0.0045
private val NavigationMapMarkerSize = 38.dp
