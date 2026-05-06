package com.ssafy.e102.eumgil.feature.map.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerFilterUiState

@Composable
fun MapCategoryFilterBar(
    state: MapMarkerFilterUiState,
    onReset: () -> Unit,
    onCategoryToggle: (FacilityCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> {
            FilterStatusCard(
                message = stringResource(id = R.string.map_filter_summary_loading),
                modifier = modifier,
            )
            return
        }

        state.isLoadFailed -> {
            FilterStatusCard(
                message = stringResource(id = R.string.map_filter_summary_error),
                modifier = modifier,
            )
            return
        }

        state.isEmptyData -> {
            FilterStatusCard(
                message = stringResource(id = R.string.map_filter_summary_empty_data),
                modifier = modifier,
            )
            return
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = EumSpacing.small),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = EumSpacing.medium),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                ) {
                    Text(
                        text = stringResource(id = R.string.map_filter_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = selectionSummaryText(state = state),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = EumSpacing.medium),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                item {
                    FilterChip(
                        selected = state.selection.isShowingAllCategories,
                        onClick = onReset,
                        leadingIcon = {
                            FilterChipIcon(iconRes = R.drawable.ic_nav_facility)
                        },
                        label = {
                            Text(text = stringResource(id = R.string.map_filter_chip_all))
                        },
                    )
                }

                items(
                    items = state.categoryOptions,
                    key = { option -> option.category.name },
                ) { option ->
                    val categoryLabel = "${categoryFilterLabel(option.category)} ${option.totalMarkerCount}"

                    FilterChip(
                        selected = option.isSelected,
                        onClick = { onCategoryToggle(option.category) },
                        leadingIcon = {
                            FilterChipIcon(
                                iconRes = categoryFilterIcon(option.category),
                                iconSizeDp = categoryFilterIconSizeDp(option.category),
                            )
                        },
                        label = {
                            Text(text = categoryLabel)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun selectionSummaryText(state: MapMarkerFilterUiState): String {
    if (state.isEmptyResult) {
        return buildString {
            append(selectedCategorySummaryLabel(state = state))
            append(" / ")
            append(stringResource(id = R.string.map_filter_summary_empty_result))
        }
    }

    return buildString {
        append(selectedCategorySummaryLabel(state = state))
        append(" / ")
        append(
            stringResource(
                id = R.string.map_filter_summary,
                state.visibleMarkerCount,
                state.totalMarkerCount,
            ),
        )
    }
}

@Composable
private fun selectedCategorySummaryLabel(state: MapMarkerFilterUiState): String =
    when {
        state.selection.isShowingAllCategories -> stringResource(id = R.string.map_filter_chip_all)
        else -> {
            val selectedLabels =
                state.categoryOptions
                    .filter { option -> option.isSelected }
                    .map { option -> categoryFilterLabel(option.category) }

            when {
                selectedLabels.isEmpty() -> stringResource(id = R.string.map_filter_chip_all)
                selectedLabels.size <= 2 -> selectedLabels.joinToString(separator = " / ")
                else -> selectedLabels.take(2).joinToString(separator = " / ") + " +${selectedLabels.size - 2}"
            }
        }
    }

@Composable
private fun FilterStatusCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
        shadowElevation = 4.dp,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(EumSpacing.medium),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilterChipIcon(
    @DrawableRes iconRes: Int,
    iconSizeDp: Int = 18,
) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        modifier = Modifier.size(iconSizeDp.dp),
    )
}

@Composable
private fun categoryFilterLabel(category: FacilityCategory): String =
    when (category) {
        FacilityCategory.TOILET -> stringResource(id = R.string.map_filter_category_toilet)
        FacilityCategory.ELEVATOR -> stringResource(id = R.string.map_filter_category_elevator)
        FacilityCategory.CHARGING_STATION -> stringResource(id = R.string.map_filter_category_charging_station)
        FacilityCategory.FOOD_CAFE -> "식당·카페"
        FacilityCategory.TOURIST_SPOT -> "무장애 관광지"
        FacilityCategory.ACCOMMODATION -> "숙박"
        FacilityCategory.HEALTHCARE -> "병원"
        FacilityCategory.WELFARE -> "복지관"
        FacilityCategory.PUBLIC_OFFICE -> "관공서"
        FacilityCategory.BRAILLE_BLOCK -> stringResource(id = R.string.map_filter_category_braille_block)
        FacilityCategory.RESTAURANT -> stringResource(id = R.string.map_filter_category_restaurant)
        FacilityCategory.TOURIST_ATTRACTION -> stringResource(id = R.string.map_filter_category_tourist_attraction)
        FacilityCategory.OTHER -> stringResource(id = R.string.map_filter_category_other)
    }

@DrawableRes
private fun categoryFilterIcon(category: FacilityCategory): Int =
    when (category) {
        FacilityCategory.TOILET -> R.drawable.ic_place_restroom
        FacilityCategory.ELEVATOR -> R.drawable.ic_route_elevator
        FacilityCategory.CHARGING_STATION -> R.drawable.ic_place_charging
        FacilityCategory.FOOD_CAFE -> R.drawable.ic_place_food_cafe
        FacilityCategory.TOURIST_SPOT -> R.drawable.ic_place_tourist_spot
        FacilityCategory.ACCOMMODATION -> R.drawable.ic_place_accommodation
        FacilityCategory.HEALTHCARE -> R.drawable.ic_place_healthcare
        FacilityCategory.WELFARE -> R.drawable.ic_place_welfare
        FacilityCategory.PUBLIC_OFFICE -> R.drawable.ic_place_public_office
        FacilityCategory.BRAILLE_BLOCK -> R.drawable.ic_route_tactile_blocks
        FacilityCategory.RESTAURANT -> R.drawable.ic_place_food_cafe
        FacilityCategory.TOURIST_ATTRACTION -> R.drawable.ic_place_tourist_spot
        FacilityCategory.OTHER -> R.drawable.ic_nav_facility
    }

internal fun categoryFilterIconSizeDp(category: FacilityCategory): Int =
    when (category) {
        FacilityCategory.ELEVATOR -> 20
        else -> 18
    }
