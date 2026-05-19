package com.ssafy.e102.eumgil.feature.map.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.data.repository.ApprovedHazardMarker
import com.ssafy.e102.eumgil.feature.report.displayLabel
import com.ssafy.e102.eumgil.feature.report.markerIconRes
import com.ssafy.e102.eumgil.feature.report.toReportTypeOrNull

@Composable
internal fun ApprovedHazardMarkerBottomSheet(
    marker: ApprovedHazardMarker?,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var viewerState by remember { mutableStateOf(ApprovedHazardMarkerImageViewerState()) }
    LaunchedEffect(marker?.reportId) {
        if (marker == null) {
            viewerState = viewerState.close()
        }
    }
    BoxWithConstraints(
        modifier = modifier.zIndex(ApprovedHazardMarkerOverlayZIndex),
    ) {
        val sheetMaxHeight = maxHeight * 0.72f
        AnimatedVisibility(
            visible = marker != null,
            enter = slideInVertically { fullHeight -> fullHeight } + fadeIn(),
            exit = slideOutVertically { fullHeight -> fullHeight } + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
        ) {
            val resolvedMarker = marker ?: return@AnimatedVisibility
            MapBottomSheetSurface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .heightIn(max = sheetMaxHeight),
                handleModifier = Modifier.height(MapBottomSheetHandleHeight),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(EumRadius.scaleM),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(id = resolvedMarker.reportTypeIconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(id = R.string.approved_hazard_marker_sheet_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = resolvedMarker.reportTypeLabel,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_action_close),
                                contentDescription = stringResource(id = R.string.map_facility_detail_close),
                            )
                        }
                    }

                    if (resolvedMarker.imageUrls.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(EumRadius.scaleM),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(
                                text = stringResource(id = R.string.approved_hazard_marker_no_images),
                                modifier = Modifier.padding(EumSpacing.medium),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                        ) {
                            itemsIndexed(
                                items = resolvedMarker.imageUrls,
                                key = { index, imageUrl -> "$index-$imageUrl" },
                            ) { index, imageUrl ->
                                HazardMarkerPhotoCard(
                                    imageUrl = imageUrl,
                                    index = index,
                                    onClick = {
                                        viewerState = viewerState.open(
                                            imageUrls = resolvedMarker.imageUrls,
                                            initialIndex = index,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        ApprovedHazardMarkerImageViewer(
            state = viewerState,
            onDismiss = { viewerState = viewerState.close() },
        )
    }
}

@Composable
private fun HazardMarkerPhotoCard(
    imageUrl: String,
    index: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .size(136.dp)
                .testTag("approvedHazardThumbnail-$index")
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(EumRadius.scaleM),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        SubcomposeAsyncImage(
            model =
                ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
            contentDescription = stringResource(id = R.string.approved_hazard_marker_thumbnail_content_description, index + 1),
            modifier = Modifier.fillMaxSize().aspectRatio(1f),
            contentScale = ContentScale.Crop,
            loading = {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(id = R.string.approved_hazard_marker_image_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            error = {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(id = R.string.approved_hazard_marker_image_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ApprovedHazardMarkerImageViewer(
    state: ApprovedHazardMarkerImageViewerState,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = state.isVisible && state.imageUrls.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        val pagerState = rememberPagerState(initialPage = state.selectedIndex) { state.imageUrls.size }
        LaunchedEffect(state.selectedIndex, state.imageUrls, state.isVisible) {
            if (state.isVisible && state.imageUrls.isNotEmpty()) {
                pagerState.scrollToPage(state.selectedIndex.coerceIn(0, state.imageUrls.lastIndex))
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
        ) {
            val highlightedImageUrl =
                state.imageUrls.getOrNull(
                    pagerState.currentPage.coerceIn(0, state.imageUrls.lastIndex),
                )
            if (highlightedImageUrl != null) {
                SubcomposeAsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(highlightedImageUrl)
                            .crossfade(true)
                            .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(28.dp),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.58f)),
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("approvedHazardViewerBackdrop")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        ),
            )

            IconButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .testTag("approvedHazardViewerClose")
                        .padding(EumSpacing.medium),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_close),
                    contentDescription = stringResource(id = R.string.approved_hazard_marker_viewer_close),
                    tint = ApprovedHazardMarkerViewerForegroundColor,
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = EumSpacing.medium, vertical = 72.dp),
            ) { page ->
                SubcomposeAsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(state.imageUrls[page])
                            .crossfade(true)
                            .build(),
                    contentDescription = stringResource(id = R.string.approved_hazard_marker_thumbnail_content_description, page + 1),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(id = R.string.approved_hazard_marker_image_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ApprovedHazardMarkerViewerForegroundColor,
                            )
                        }
                    },
                    error = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(id = R.string.approved_hazard_marker_image_error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ApprovedHazardMarkerViewerForegroundColor,
                            )
                        }
                    },
                )
            }

            Text(
                text = "${pagerState.currentPage + 1} / ${state.imageUrls.size}",
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .testTag("approvedHazardViewerIndicator")
                        .padding(bottom = 32.dp),
                style = MaterialTheme.typography.labelLarge,
                color = ApprovedHazardMarkerViewerForegroundColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

internal data class ApprovedHazardMarkerImageViewerState(
    val imageUrls: List<String> = emptyList(),
    val selectedIndex: Int = 0,
    val isVisible: Boolean = false,
) {
    fun open(
        imageUrls: List<String>,
        initialIndex: Int,
    ): ApprovedHazardMarkerImageViewerState {
        if (imageUrls.isEmpty()) return copy(imageUrls = emptyList(), selectedIndex = 0, isVisible = false)
        return copy(
            imageUrls = imageUrls,
            selectedIndex = initialIndex.coerceIn(0, imageUrls.lastIndex),
            isVisible = true,
        )
    }

    fun close(): ApprovedHazardMarkerImageViewerState = copy(isVisible = false)
}

private val ApprovedHazardMarkerViewerForegroundColor = Color.White
private const val ApprovedHazardMarkerOverlayZIndex = 10f

private val ApprovedHazardMarker.reportTypeLabel: String
    get() = reportType.toReportTypeOrNull()?.displayLabel ?: reportType

private val ApprovedHazardMarker.reportTypeIconRes: Int
    get() = reportType.toReportTypeOrNull()?.markerIconRes ?: R.drawable.ic_report_other
