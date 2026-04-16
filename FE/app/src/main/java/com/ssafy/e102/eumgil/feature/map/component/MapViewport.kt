package com.ssafy.e102.eumgil.feature.map.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Immutable
data class MapViewportUiState(
    val integrationState: MapIntegrationState,
    val regionLabel: String,
    val statusLabel: String,
    val title: String,
    val description: String,
    val supportingText: String,
)

@Immutable
sealed interface MapIntegrationState {
    @Immutable
    data object Unbound : MapIntegrationState

    @Immutable
    data class Bound(val providerName: String) : MapIntegrationState
}

@Composable
fun MapViewport(
    state: MapViewportUiState,
    modifier: Modifier = Modifier,
) {
    when (val integrationState = state.integrationState) {
        MapIntegrationState.Unbound -> {
            MapFallbackSurface(
                regionLabel = state.regionLabel,
                statusLabel = state.statusLabel,
                title = state.title,
                description = state.description,
                supportingText = state.supportingText,
                modifier = modifier,
            )
        }

        is MapIntegrationState.Bound -> {
            MapContainer(
                integrationState = integrationState,
                state = state,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun MapContainer(
    integrationState: MapIntegrationState.Bound,
    state: MapViewportUiState,
    modifier: Modifier = Modifier,
) {
    // SDK 벤더가 실제로 확정되고 의존성이 연결되면 이 지점에 컨테이너를 붙인다.
    MapFallbackSurface(
        regionLabel = state.regionLabel,
        statusLabel = integrationState.providerName,
        title = state.title,
        description = state.description,
        supportingText = state.supportingText,
        modifier = modifier,
    )
}

@Composable
private fun MapFallbackSurface(
    regionLabel: String,
    statusLabel: String,
    title: String,
    description: String,
    supportingText: String,
    modifier: Modifier = Modifier,
) {
    val surfaceTint = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        surfaceTint,
                    ),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
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

            drawCircle(
                color = primary.copy(alpha = 0.10f),
                radius = size.minDimension * 0.20f,
                center = Offset(size.width * 0.72f, size.height * 0.28f),
            )
            drawCircle(
                color = secondary.copy(alpha = 0.12f),
                radius = size.minDimension * 0.13f,
                center = Offset(size.width * 0.22f, size.height * 0.68f),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .widthIn(max = 340.dp)
                .padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Surface(
                color = primary.copy(alpha = 0.10f),
                shape = RoundedCornerShape(EumRadius.full),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.22f)),
            ) {
                Text(
                    text = regionLabel,
                    modifier = Modifier.padding(
                        horizontal = EumSpacing.small,
                        vertical = EumSpacing.xSmall,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Surface(
                color = surfaceTint.copy(alpha = 0.98f),
                shape = RoundedCornerShape(EumRadius.large),
                tonalElevation = 2.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, outline.copy(alpha = 0.8f)),
            ) {
                Column(
                    modifier = Modifier.padding(EumSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
