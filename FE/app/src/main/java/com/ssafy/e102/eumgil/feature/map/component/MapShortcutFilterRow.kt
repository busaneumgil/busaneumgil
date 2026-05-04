package com.ssafy.e102.eumgil.feature.map.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.feature.map.model.MapShortcutFilterChipState
import com.ssafy.e102.eumgil.feature.map.model.MapShortcutFilterKey
import com.ssafy.e102.eumgil.feature.map.model.MapShortcutFilterRowState

@Composable
fun MapShortcutFilterRow(
    state: MapShortcutFilterRowState,
    onChipClick: (MapShortcutFilterKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = true,
    ) {
        items(
            items = state.chips,
            key = { chip -> chip.key.name },
        ) { chip ->
            ShortcutFilterChip(
                chip = chip,
                onClick = { onChipClick(chip.key) },
            )
        }
    }
}

@Composable
private fun ShortcutFilterChip(
    chip: MapShortcutFilterChipState,
    onClick: () -> Unit,
) {
    val selected = chip.isSelected
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
        }

    Surface(
        onClick = onClick,
        modifier = Modifier.alpha(if (chip.isEnabled) 1f else 0.52f),
        enabled = chip.isEnabled,
        shape = RoundedCornerShape(EumRadius.medium),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (selected) 4.dp else 2.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = 38.dp)
                    .padding(horizontal = 13.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = shortcutFilterIcon(chip.key)),
                contentDescription = null,
                modifier = Modifier.size(shortcutFilterIconSizeDp(chip.key).dp),
                tint = contentColor,
            )
            Text(
                text = shortcutFilterLabel(chip.key),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun shortcutFilterLabel(key: MapShortcutFilterKey): String =
    when (key) {
        MapShortcutFilterKey.TOILET -> "장애인 화장실"
        MapShortcutFilterKey.ELEVATOR -> "엘리베이터"
        MapShortcutFilterKey.CHARGING_STATION -> "전동보장구 충전"
        MapShortcutFilterKey.FOOD_CAFE -> "식당·카페"
        MapShortcutFilterKey.TOURIST_SPOT -> "무장애 관광지"
        MapShortcutFilterKey.ACCOMMODATION -> "숙박"
        MapShortcutFilterKey.HEALTHCARE -> "병원"
        MapShortcutFilterKey.WELFARE -> "복지관"
        MapShortcutFilterKey.PUBLIC_OFFICE -> "관공서"
    }

@DrawableRes
private fun shortcutFilterIcon(key: MapShortcutFilterKey): Int =
    when (key) {
        MapShortcutFilterKey.TOILET -> R.drawable.ic_place_restroom
        MapShortcutFilterKey.ELEVATOR -> R.drawable.ic_map_shortcut_elevator
        MapShortcutFilterKey.CHARGING_STATION -> R.drawable.ic_place_charging
        MapShortcutFilterKey.FOOD_CAFE -> R.drawable.ic_place_restaurant
        MapShortcutFilterKey.TOURIST_SPOT -> R.drawable.ic_nav_facility
        MapShortcutFilterKey.ACCOMMODATION -> R.drawable.ic_place_lodging
        MapShortcutFilterKey.HEALTHCARE -> R.drawable.ic_place_hospital
        MapShortcutFilterKey.WELFARE -> R.drawable.ic_place_welfare
        MapShortcutFilterKey.PUBLIC_OFFICE -> R.drawable.ic_nav_facility
    }

internal fun shortcutFilterIconSizeDp(key: MapShortcutFilterKey): Int =
    when (key) {
        MapShortcutFilterKey.ELEVATOR -> 18
        else -> 16
    }
