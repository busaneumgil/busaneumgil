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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.component.map.EumMapFloatingActionButtonState
import com.ssafy.e102.eumgil.core.designsystem.component.map.EumMapFloatingControls
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumCenteredTopBar
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.feature.map.component.MapViewportOverlayBackdrop
import com.ssafy.e102.eumgil.feature.map.component.createNavigationViewportOverlayState
import com.ssafy.e102.eumgil.feature.navigation.component.NavigationSegmentRail

@Composable
fun NavigationScreen(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenPolicy = navigationScreenPolicy(uiState)
    val railWidth = (LocalConfiguration.current.screenWidthDp.dp / 7).coerceIn(48.dp, 60.dp)

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                NavigationTopBar(
                    onBackClick = { onAction(NavigationUiAction.BackClicked) },
                )
            },
            bottomBar = {
                val bottomBarLayoutPolicy =
                    navigationBottomBarLayoutPolicy(
                        showSegmentRail = screenPolicy.showSegmentRail,
                        railWidth = railWidth,
                    )
                NavigationBottomBar(
                    uiState = uiState,
                    onAction = onAction,
                    layoutPolicy = bottomBarLayoutPolicy,
                )
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            ) {
                NavigationHeroCard(
                    uiState = uiState,
                    onAction = onAction,
                )
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                ) {
                    if (screenPolicy.showSegmentRail) {
                        NavigationSegmentRail(
                            uiState = uiState.segmentSync,
                            onSegmentTapped = { index ->
                                onAction(NavigationUiAction.SegmentTapped(index = index))
                            },
                            onReturnToActiveSegmentClick = {
                                onAction(NavigationUiAction.ReturnToActiveSegmentClicked)
                            },
                            onRouteDetailClick = { onAction(NavigationUiAction.RouteDetailClicked) },
                            isRouteDetailEnabled = uiState.canOpenRouteDetail,
                            modifier =
                                Modifier
                                    .width(railWidth)
                                    .fillMaxHeight(),
                        )
                    }
                    NavigationMapStage(
                        uiState = uiState,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (uiState.isExitConfirmDialogVisible) {
            NavigationExitConfirmDialog(
                onDismiss = { onAction(NavigationUiAction.ExitNavigationDismissed) },
                onConfirm = { onAction(NavigationUiAction.ConfirmExitNavigationClicked) },
                modifier = Modifier.align(Alignment.Center),
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

internal data class NavigationScreenPolicy(
    val showSegmentRail: Boolean,
    val showFocusedSegmentCard: Boolean,
    val showReturnToActiveAction: Boolean,
)

internal data class NavigationHeroLayoutPolicy(
    val minHeight: Dp,
    val maxHeight: Dp,
    val showBottomDivider: Boolean,
)

internal data class NavigationBottomBarLayoutPolicy(
    val topDividerStartInset: Dp,
)

internal fun navigationScreenPolicy(uiState: NavigationUiState): NavigationScreenPolicy =
    NavigationScreenPolicy(
        showSegmentRail = uiState.segmentSync.railItems.isNotEmpty() || uiState.canOpenRouteDetail,
        showFocusedSegmentCard = false,
        showReturnToActiveAction = uiState.segmentSync.isInspectingSegments,
    )

internal fun navigationHeroLayoutPolicy(screenHeight: Dp): NavigationHeroLayoutPolicy =
    NavigationHeroLayoutPolicy(
        minHeight = 116.dp,
        maxHeight = (screenHeight * 0.24f).coerceAtLeast(132.dp),
        showBottomDivider = false,
    )

internal fun navigationBottomBarLayoutPolicy(
    showSegmentRail: Boolean,
    railWidth: Dp,
): NavigationBottomBarLayoutPolicy =
    NavigationBottomBarLayoutPolicy(
        topDividerStartInset = if (showSegmentRail) railWidth else 0.dp,
    )

@Composable
private fun NavigationHeroCard(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
) {
    val heroGuidanceAction = uiState.focusedSegmentCard?.guidanceAction ?: uiState.stepCard.guidanceAction
    val heroDistanceLabel = uiState.focusedSegmentCard?.distanceLabel ?: uiState.stepCard.distanceLabel
    val layoutPolicy = navigationHeroLayoutPolicy(LocalConfiguration.current.screenHeightDp.dp)

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = layoutPolicy.minHeight, max = layoutPolicy.maxHeight),
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = EumSpacing.medium,
                            vertical = EumSpacing.medium,
                        ),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavigationHeroDirectionIcon(
                        guidanceAction = heroGuidanceAction,
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = heroGuidanceAction.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = heroDistanceLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        )
                    }
                }
                NavigationVoiceControl(
                    uiState = uiState,
                    onAction = onAction,
                )
            }
            if (layoutPolicy.showBottomDivider) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.82f),
                )
            }
        }
    }
}

@Composable
private fun NavigationHeroDirectionIcon(
    guidanceAction: NavigationGuidanceAction,
) {
    Icon(
        painter = painterResource(id = guidanceAction.iconRes()),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(34.dp),
    )
}

@Composable
private fun NavigationVoiceControl(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
) {
    val isEnabled = uiState.tts.isEnabled
    val containerColor =
        if (isEnabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        }
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
    val iconRes =
        if (isEnabled) {
            R.drawable.ic_control_voice
        } else {
            R.drawable.ic_navigation_tts_off
        }
    val iconTint =
        if (isEnabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    val contentDescription =
        if (isEnabled) {
            stringResource(id = R.string.navigation_tts_toggle_content_description_on)
        } else {
            stringResource(id = R.string.navigation_tts_toggle_content_description_off)
        }

    Surface(
        modifier =
            Modifier
                .size(44.dp)
                .semantics {
                    this.contentDescription = contentDescription
                }
                .clickable(
                    role = Role.Button,
                    onClick = {
                        onAction(
                            NavigationUiAction.VoiceGuidanceToggled(
                                enabled = !isEnabled,
                            ),
                        )
                    },
                ),
        shape = RoundedCornerShape(EumRadius.scaleM),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(if (isEnabled) 26.dp else 28.dp),
            )
        }
    }
}

@Composable
private fun NavigationMapStage(
    uiState: NavigationUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
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
                        .padding(EumSpacing.small),
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

@Composable
private fun NavigationMapBackdrop(
    mapOverlay: NavigationMapOverlayUiState,
    modifier: Modifier = Modifier,
) {
    val mapDescription = stringResource(id = R.string.navigation_map_section_title)
    MapViewportOverlayBackdrop(
        overlayState = createNavigationViewportOverlayState(mapOverlay),
        modifier = modifier,
        horizontalPadding = 28.dp,
        verticalPadding = 24.dp,
        contentDescription = mapDescription,
    )
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
        shape = RoundedCornerShape(EumRadius.scaleM),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)),
        shadowElevation = 0.dp,
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
    EumMapFloatingControls(
        actionButtonState =
            EumMapFloatingActionButtonState(
                iconRes = R.drawable.ic_route_start_navigation_button,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(id = R.string.navigation_return_to_active_segment_label),
                enabled = true,
            ),
        onActionClick = {},
        modifier = modifier,
        onZoomInClick = {},
        onZoomOutClick = {},
        zoomInLabel = stringResource(id = R.string.navigation_map_control_zoom_in),
        zoomOutLabel = stringResource(id = R.string.navigation_map_control_zoom_out),
    )
}

@Composable
private fun NavigationBottomBar(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
    layoutPolicy: NavigationBottomBarLayoutPolicy,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
        ) {
            HorizontalDivider(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = layoutPolicy.topDividerStartInset),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.82f),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = EumSpacing.medium,
                            vertical = EumSpacing.small,
                        ),
            ) {
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
                    elevation =
                        ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            disabledElevation = 0.dp,
                        ),
                    shape = RoundedCornerShape(EumRadius.scaleM),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_control_stop),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = uiState.exitCta.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = EumSpacing.xSmall),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationExitConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = R.string.navigation_exit_confirm_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.navigation_exit_confirm_dialog_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(EumRadius.scaleM),
            ) {
                Text(text = stringResource(id = R.string.navigation_exit_confirm_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.navigation_exit_confirm_dialog_cancel))
            }
        },
    )
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
            when (mapOverlay.mapFocusMode) {
                NavigationMapFocusMode.ACTIVE -> {
                    addAll(mapOverlay.selectedRoutePolyline)
                    mapOverlay.currentLocation?.let { point -> add(point.coordinate) }
                    mapOverlay.origin?.let { point -> add(point.coordinate) }
                    mapOverlay.destination?.let { point -> add(point.coordinate) }
                }

                NavigationMapFocusMode.FOCUSED -> {
                    addAll(mapOverlay.focusedSegmentPolyline)
                    if (mapOverlay.activeSegmentPolyline != mapOverlay.focusedSegmentPolyline) {
                        addAll(mapOverlay.activeSegmentPolyline)
                    }
                    mapOverlay.focusCoordinate?.let { coordinate -> add(coordinate) }
                    mapOverlay.currentLocation?.let { point -> add(point.coordinate) }
                }
            }
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
