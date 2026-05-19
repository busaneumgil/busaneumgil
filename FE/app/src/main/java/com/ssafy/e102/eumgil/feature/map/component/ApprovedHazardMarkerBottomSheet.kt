package com.ssafy.e102.eumgil.feature.map.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    BoxWithConstraints(
        modifier = modifier,
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
                            items(
                                items = resolvedMarker.imageUrls,
                                key = { imageUrl -> imageUrl },
                            ) { imageUrl ->
                                HazardMarkerPhotoCard(imageUrl = imageUrl)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HazardMarkerPhotoCard(imageUrl: String) {
    Surface(
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
            contentDescription = null,
            modifier = Modifier.size(width = 180.dp, height = 132.dp),
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

private val ApprovedHazardMarker.reportTypeLabel: String
    get() = reportType.toReportTypeOrNull()?.displayLabel ?: reportType

private val ApprovedHazardMarker.reportTypeIconRes: Int
    get() = reportType.toReportTypeOrNull()?.markerIconRes ?: R.drawable.ic_report_other
