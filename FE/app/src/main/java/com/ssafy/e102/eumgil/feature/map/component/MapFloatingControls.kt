package com.ssafy.e102.eumgil.feature.map.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        horizontalAlignment = Alignment.End,
    ) {
        MapFloatingControlCard(
            modifier = Modifier.width(58.dp),
        ) {
            Column {
                ZoomButton(
                    label = "+",
                    modifier = Modifier.height(58.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                ZoomButton(
                    label = "-",
                    modifier = Modifier.height(58.dp),
                )
            }
        }

        val buttonStyle = recenterButtonStyle(recenterButtonState)
        MapFloatingControlCard(
            modifier = Modifier.size(54.dp),
            onClick = onRecenterClick,
            enabled = buttonStyle.isEnabled,
        ) {
            Column(
                modifier = Modifier.defaultMinSize(minWidth = 54.dp, minHeight = 54.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(id = buttonStyle.iconRes),
                    contentDescription = buttonStyle.contentDescription,
                    modifier = Modifier.size(22.dp),
                    tint = buttonStyle.tint,
                )
            }
        }
    }
}

@Composable
private fun ZoomButton(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(58.dp)
                .defaultMinSize(minWidth = 58.dp, minHeight = 58.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 28.sp,
                ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MapFloatingControlCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = border,
            shadowElevation = 10.dp,
        ) {
            content()
        }
    } else {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = border,
            shadowElevation = 10.dp,
        ) {
            content()
        }
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
