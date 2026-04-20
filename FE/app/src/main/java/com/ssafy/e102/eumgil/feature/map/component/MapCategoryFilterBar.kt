package com.ssafy.e102.eumgil.feature.map.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    if (state.isLoading) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(EumRadius.large),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
            shadowElevation = 4.dp,
        ) {
            Text(
                text = stringResource(id = R.string.map_filter_summary_loading),
                modifier = Modifier.padding(EumSpacing.medium),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
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
            Text(
                text = selectionSummaryText(state = state),
                modifier = Modifier.padding(horizontal = EumSpacing.medium),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = EumSpacing.medium),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                item {
                    FilterChip(
                        selected = state.selection.isShowingAllCategories,
                        onClick = onReset,
                        label = {
                            Text(text = stringResource(id = R.string.map_filter_chip_all))
                        },
                    )
                }

                items(
                    items = state.categoryOptions,
                    key = { option -> option.category.name },
                ) { option ->
                    FilterChip(
                        selected = option.isSelected,
                        onClick = { onCategoryToggle(option.category) },
                        label = {
                            Text(
                                text =
                                    buildString {
                                        append(categoryFilterLabel(option.category))
                                        append(' ')
                                        append(option.totalMarkerCount)
                                    },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun selectionSummaryText(state: MapMarkerFilterUiState): String {
    val selectionLabel =
        when {
            state.selection.isShowingAllCategories -> stringResource(id = R.string.map_filter_chip_all)
            else -> {
                val selectedLabels =
                    state.categoryOptions
                        .filter { option -> option.isSelected }
                        .map { option -> categoryFilterLabel(option.category) }

                when {
                    selectedLabels.isEmpty() -> stringResource(id = R.string.map_filter_chip_all)
                    selectedLabels.size <= 2 -> selectedLabels.joinToString(separator = " · ")
                    else -> selectedLabels.take(2).joinToString(separator = " · ") + " +${selectedLabels.size - 2}"
                }
            }
        }

    return buildString {
        append(selectionLabel)
        append(" · ")
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
private fun categoryFilterLabel(category: FacilityCategory): String =
    when (category) {
        FacilityCategory.RESTAURANT -> stringResource(id = R.string.map_filter_category_restaurant)
        FacilityCategory.TOURIST_ATTRACTION -> stringResource(id = R.string.map_filter_category_tourist_attraction)
        FacilityCategory.TOILET -> stringResource(id = R.string.map_filter_category_toilet)
        FacilityCategory.ELEVATOR -> stringResource(id = R.string.map_filter_category_elevator)
        FacilityCategory.CHARGING_STATION -> stringResource(id = R.string.map_filter_category_charging_station)
        FacilityCategory.BRAILLE_BLOCK -> stringResource(id = R.string.map_filter_category_braille_block)
        FacilityCategory.OTHER -> stringResource(id = R.string.map_filter_category_other)
    }
