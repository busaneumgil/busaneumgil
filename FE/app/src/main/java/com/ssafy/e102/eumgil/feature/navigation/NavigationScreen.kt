package com.ssafy.e102.eumgil.feature.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.BusanEumgilTheme
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.feature.navigation.component.NavigationStepCard

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
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(EumSpacing.medium),
            shape = RoundedCornerShape(EumRadius.large),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(EumSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
            ) {
                NavigationShellHeader()
                NavigationProgressOverview(uiState = uiState)
                NavigationMapShell(
                    title = uiState.mapPlaceholderTitle,
                    description = uiState.mapPlaceholderDescription,
                    mapOverlay = uiState.mapOverlay,
                )
                NavigationStepCard(uiState = uiState.stepCard)
            }
        }
    }
}

@Composable
private fun NavigationShellHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
    ) {
        Text(
            text = stringResource(id = R.string.navigation_shell_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(id = R.string.navigation_shell_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NavigationProgressOverview(uiState: NavigationUiState) {
    val overview =
        when (uiState.screenState) {
            NavigationScreenState.Loading ->
                NavigationProgressOverviewUiState(
                    statusLabel = "안내 준비",
                    progressTitle = "경로 안내를 준비하고 있습니다",
                    progressDescription = "첫 안내 메시지와 남은 거리 정보를 불러오는 중입니다.",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                )
            NavigationScreenState.Ready -> {
                val remainingDistance = uiState.stepCard.metrics.getOrNull(0)?.value ?: uiState.stepCard.distanceLabel
                val estimatedTime = uiState.stepCard.metrics.getOrNull(1)?.value ?: "확인 중"

                NavigationProgressOverviewUiState(
                    statusLabel = "진행 중",
                    progressTitle = uiState.stepCard.instruction,
                    progressDescription = "$remainingDistance 남음 · 예상 $estimatedTime · 종료는 하단 버튼에서 바로 처리할 수 있습니다.",
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                )
            }
            NavigationScreenState.Empty -> {
                val remainingDistance = uiState.stepCard.metrics.getOrNull(0)?.value ?: "확인 중"
                val estimatedTime = uiState.stepCard.metrics.getOrNull(1)?.value ?: "확인 중"

                NavigationProgressOverviewUiState(
                    statusLabel = "요약 안내",
                    progressTitle = "안내 메시지가 아직 없습니다",
                    progressDescription = "$remainingDistance · $estimatedTime 요약만 먼저 표시합니다.",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
                )
            }
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = overview.containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            NavigationOverviewBadge(label = overview.statusLabel)
            Text(
                text = overview.progressTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = overview.progressDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class NavigationProgressOverviewUiState(
    val statusLabel: String,
    val progressTitle: String,
    val progressDescription: String,
    val containerColor: androidx.compose.ui.graphics.Color,
)

@Composable
private fun NavigationOverviewBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(EumRadius.full),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = EumSpacing.small, vertical = EumSpacing.xSmall),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun NavigationTopBar(onBackClick: () -> Unit) {
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
                Text(text = stringResource(id = R.string.navigation_back))
            }
            Text(
                text = stringResource(id = R.string.navigation_screen_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NavigationMapShell(
    title: String,
    description: String,
    mapOverlay: NavigationMapOverlayUiState,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = stringResource(id = R.string.navigation_map_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (mapOverlay.isDisplayable && mapOverlay.selectedRoutePolyline.size >= MIN_ROUTE_POINT_COUNT) {
            NavigationRouteMapViewport(mapOverlay = mapOverlay)
        } else {
            NavigationMapPlaceholder(
                title = title,
                description = description,
            )
        }
    }
}

@Composable
private fun NavigationMapPlaceholder(
    title: String,
    description: String,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(NavigationMapHeight)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title. $description"
                },
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(EumSpacing.medium),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NavigationRouteMapViewport(mapOverlay: NavigationMapOverlayUiState) {
    val surfaceTint = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val routeColor = MaterialTheme.colorScheme.primary
    val currentColor = MaterialTheme.colorScheme.secondary
    val destinationColor = MaterialTheme.colorScheme.error
    val projectionBounds = navigationProjectionBounds(mapOverlay)
    val accessibilityLabel = navigationMapAccessibilityLabel(mapOverlay)
    val backgroundBrush =
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    surfaceTint,
                ),
        )

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(NavigationMapHeight)
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityLabel
                },
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(backgroundBrush),
        ) {
            val horizontalPadding = 28.dp
            val verticalPadding = 28.dp
            val markerAreaWidth = (maxWidth - (horizontalPadding * 2)).coerceAtLeast(0.dp)
            val markerAreaHeight = (maxHeight - (verticalPadding * 2)).coerceAtLeast(0.dp)

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawNavigationMapGrid(outline = outline)

                val routePath =
                    mapOverlay.selectedRoutePolyline.toNavigationPath(
                        bounds = projectionBounds,
                        canvasSize = size,
                    )
                drawPath(
                    path = routePath,
                    color = routeColor.copy(alpha = 0.28f),
                    style =
                        Stroke(
                            width = 12.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                )
                drawPath(
                    path = routePath,
                    color = routeColor,
                    style =
                        Stroke(
                            width = 5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                )

                mapOverlay.currentLocation?.coordinate?.let { coordinate ->
                    drawCircle(
                        color = currentColor.copy(alpha = 0.18f),
                        radius = 18.dp.toPx(),
                        center = projectionBounds.project(coordinate).toCanvasOffset(size),
                    )
                }
                mapOverlay.destination?.coordinate?.let { coordinate ->
                    drawCircle(
                        color = destinationColor.copy(alpha = 0.14f),
                        radius = 20.dp.toPx(),
                        center = projectionBounds.project(coordinate).toCanvasOffset(size),
                    )
                }
            }

            mapOverlay.currentLocation?.let { current ->
                NavigationMapMarker(
                    label = "현",
                    containerColor = currentColor,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .offsetWithinNavigationMap(
                                point = projectionBounds.project(current.coordinate),
                                areaWidth = markerAreaWidth,
                                areaHeight = markerAreaHeight,
                                horizontalPadding = horizontalPadding,
                                verticalPadding = verticalPadding,
                                elementSize = NavigationMarkerSize,
                            ),
                )
            }

            mapOverlay.destination?.let { destination ->
                NavigationMapMarker(
                    label = "도",
                    containerColor = destinationColor,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .offsetWithinNavigationMap(
                                point = projectionBounds.project(destination.coordinate),
                                areaWidth = markerAreaWidth,
                                areaHeight = markerAreaHeight,
                                horizontalPadding = horizontalPadding,
                                verticalPadding = verticalPadding,
                                elementSize = NavigationMarkerSize,
                            ),
                )
            }

            NavigationRouteMapLegend(
                originLabel = mapOverlay.origin?.label ?: "출발지",
                destinationLabel = mapOverlay.destination?.label ?: "목적지",
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(EumSpacing.small),
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
        modifier = modifier.size(NavigationMarkerSize),
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
private fun NavigationRouteMapLegend(
    originLabel: String,
    destinationLabel: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.small),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
        ) {
            Text(
                text = "선택 경로",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "출발 $originLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = "도착 $destinationLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private fun DrawScope.drawNavigationMapGrid(outline: Color) {
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

private fun List<GeoCoordinate>.toNavigationPath(
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
            mapOverlay.currentLocation?.coordinate?.let(::add)
            mapOverlay.origin?.coordinate?.let(::add)
            mapOverlay.destination?.coordinate?.let(::add)
            mapOverlay.routeSegments.forEach { segment -> addAll(segment.polyline) }
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
                .coerceIn(0.10f, 0.90f)

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

private fun navigationMapAccessibilityLabel(mapOverlay: NavigationMapOverlayUiState): String {
    val originLabel = mapOverlay.origin?.label ?: "출발지"
    val destinationLabel = mapOverlay.destination?.label ?: "목적지"
    val pointCount = mapOverlay.selectedRoutePolyline.size

    return "지도 영역, $originLabel 출발, $destinationLabel 도착, 선택 경로 ${pointCount}개 지점을 표시합니다."
}

private val NavigationMapHeight = 240.dp
private val NavigationMarkerSize = 38.dp
private const val MIN_ROUTE_POINT_COUNT = 2
private const val MIN_NAVIGATION_LATITUDE_SPAN = 0.0035
private const val MIN_NAVIGATION_LONGITUDE_SPAN = 0.0045

@Composable
private fun NavigationBottomBar(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
) {
    val headline =
        when (uiState.screenState) {
            NavigationScreenState.Loading -> "진행 화면을 준비하고 있습니다"
            NavigationScreenState.Ready -> "${uiState.stepCard.distanceLabel} 후 ${uiState.stepCard.emphasisLabel}"
            NavigationScreenState.Empty -> "거리 요약만 먼저 표시하고 있습니다"
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            NavigationVoiceGuidanceControls(
                uiState = uiState.tts,
                onAction = onAction,
            )
            Button(
                onClick = { onAction(NavigationUiAction.ExitNavigationClicked) },
                enabled = uiState.isExitEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = uiState.exitCta.label)
            }
            Text(
                text = uiState.exitCta.supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NavigationVoiceGuidanceControls(
    uiState: NavigationTtsUiState,
    onAction: (NavigationUiAction) -> Unit,
) {
    val voiceStateLabel = if (uiState.isEnabled) "켜짐" else "꺼짐"
    val statusText =
        when {
            !uiState.isEnabled -> NAVIGATION_TTS_DISABLED_MESSAGE
            uiState.status == NavigationTtsStatus.Unavailable -> NAVIGATION_TTS_UNAVAILABLE_MESSAGE
            uiState.status == NavigationTtsStatus.Ready -> "음성 안내를 사용할 수 있습니다."
            else -> NAVIGATION_TTS_PREPARING_MESSAGE
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = uiState.isEnabled,
                        role = Role.Switch,
                        onValueChange = { enabled ->
                            onAction(NavigationUiAction.VoiceGuidanceToggled(enabled))
                        },
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = "음성 안내"
                        stateDescription = voiceStateLabel
                    },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = EumSpacing.small),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
            ) {
                Text(
                    text = "음성 안내",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = uiState.isEnabled,
                onCheckedChange = null,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            OutlinedButton(
                onClick = { onAction(NavigationUiAction.BriefingReplayClicked) },
                enabled = uiState.canRequestBriefing,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "다시 듣기")
            }
            TextButton(
                onClick = { onAction(NavigationUiAction.StopBriefingClicked) },
                enabled = uiState.isEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "음성 중지")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Navigation Loading")
@Composable
private fun NavigationLoadingPreview() {
    BusanEumgilTheme {
        NavigationScreen(
            uiState = NavigationUiState(),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Navigation Ready")
@Composable
private fun NavigationReadyPreview() {
    BusanEumgilTheme {
        NavigationScreen(
            uiState =
                NavigationUiState(
                    screenState = NavigationScreenState.Ready,
                    mapPlaceholderDescription = "부산역 방향 경로 오버레이가 이 영역에 연결될 예정입니다.",
                    stepCard =
                        NavigationStepCardUiState(
                            statusLabel = "SAFE 우선",
                            emphasisLabel = "위험도 낮음",
                            distanceLabel = "350m",
                            instruction = "350m 앞에서 좌회전 후 횡단보도를 건너세요",
                            supportingText = "부산역 방향으로 Safe Route 경로를 따라 이동합니다.",
                            metrics =
                                listOf(
                                    NavigationStepMetricUiState(label = "남은 거리", value = "980m"),
                                    NavigationStepMetricUiState(label = "예상 시간", value = "16분"),
                                    NavigationStepMetricUiState(label = "진행 단계", value = "1 / 2"),
                                ),
                        ),
                    exitCta =
                        NavigationCtaUiState(
                            label = "내비게이션 종료",
                            supportingText = "안내를 종료하고 지도로 돌아갑니다.",
                            isEnabled = true,
                        ),
                ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Navigation Empty")
@Composable
private fun NavigationEmptyPreview() {
    BusanEumgilTheme {
        NavigationScreen(
            uiState =
                NavigationUiState(
                    screenState = NavigationScreenState.Empty,
                    mapPlaceholderDescription = "부산역 방향 거리 요약을 먼저 표시하고 있습니다.",
                    stepCard =
                        NavigationStepCardUiState(
                            statusLabel = "최단 거리",
                            emphasisLabel = "위험도 보통",
                            distanceLabel = "840m",
                            instruction = "현재 안내 메시지를 준비하지 못했습니다",
                            supportingText = "부산역 방향으로 거리와 예상 시간 요약만 먼저 표시합니다.",
                            metrics =
                                listOf(
                                    NavigationStepMetricUiState(label = "남은 거리", value = "840m"),
                                    NavigationStepMetricUiState(label = "예상 시간", value = "14분"),
                                    NavigationStepMetricUiState(label = "진행 단계", value = "안내 없음"),
                                ),
                        ),
                    exitCta =
                        NavigationCtaUiState(
                            label = "내비게이션 종료",
                            supportingText = "안내를 종료하고 지도로 돌아갑니다.",
                            isEnabled = true,
                        ),
                ),
            onAction = {},
        )
    }
}
