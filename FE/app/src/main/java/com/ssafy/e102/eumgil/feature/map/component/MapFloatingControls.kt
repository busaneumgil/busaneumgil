package com.ssafy.e102.eumgil.feature.map.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.map.MapRecenterButtonState

@Composable
fun MapFloatingControls(
    recenterButtonState: MapRecenterButtonState,
    onRecenterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
            shadowElevation = 8.dp,
        ) {
            Column {
                ZoomButton(label = "+")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                ZoomButton(label = "−")
            }
        }

        val buttonStyle = recenterButtonStyle(recenterButtonState)
        Surface(
            onClick = onRecenterClick,
            enabled = buttonStyle.isEnabled,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier =
                    Modifier
                        .defaultMinSize(minWidth = 46.dp, minHeight = 46.dp)
                        .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = buttonStyle.iconRes),
                    contentDescription = buttonStyle.contentDescription,
                    modifier = Modifier.size(20.dp),
                    tint = buttonStyle.tint,
                )
            }
        }
    }
}

@Composable
private fun ZoomButton(
    label: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minWidth = 46.dp, minHeight = 46.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun recenterButtonStyle(state: MapRecenterButtonState): RecenterButtonStyle =
    when (state) {
        MapRecenterButtonState.REQUEST_PERMISSION ->
            RecenterButtonStyle(
                iconRes = R.drawable.ic_permission_location,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(id = R.string.map_location_action_request_permission),
                isEnabled = true,
            )

        MapRecenterButtonState.LOADING ->
            RecenterButtonStyle(
                iconRes = R.drawable.ic_status_hourglass,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = stringResource(id = R.string.map_location_action_loading),
                isEnabled = false,
            )

        MapRecenterButtonState.RETRY ->
            RecenterButtonStyle(
                iconRes = R.drawable.ic_status_refresh,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(id = R.string.map_location_action_retry),
                isEnabled = true,
            )

        MapRecenterButtonState.DISABLED ->
            RecenterButtonStyle(
                iconRes = R.drawable.ic_status_cancel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = stringResource(id = R.string.map_location_action_disabled),
                isEnabled = false,
            )

        MapRecenterButtonState.ENABLED ->
            RecenterButtonStyle(
                iconRes = R.drawable.ic_permission_location,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(id = R.string.map_location_action_recenter),
                isEnabled = true,
            )
    }

private data class RecenterButtonStyle(
    @DrawableRes val iconRes: Int,
    val tint: Color,
    val contentDescription: String,
    val isEnabled: Boolean,
)
