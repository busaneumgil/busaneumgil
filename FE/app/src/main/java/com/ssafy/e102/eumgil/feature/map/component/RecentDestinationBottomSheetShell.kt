package com.ssafy.e102.eumgil.feature.map.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R

@Immutable
data class RecentDestinationBottomSheetState(
    val isVisible: Boolean = false,
    val title: String = "최근 목적지",
    val items: List<RecentDestinationRowState> = emptyList(),
)

@Immutable
data class RecentDestinationRowState(
    val placeId: String,
    val title: String,
    val address: String,
    val tags: List<String> = emptyList(),
    val overflowTagCount: Int = 0,
    @DrawableRes val iconRes: Int,
)

@Composable
fun RecentDestinationBottomSheetShell(
    state: RecentDestinationBottomSheetState,
    onViewAllClick: () -> Unit,
    onRouteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val sheetMaxHeight = maxHeight * 0.58f

        AnimatedVisibility(
            visible = state.isVisible,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 3 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight / 3 }) + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth(),
        ) {
            MapBottomSheetSurface(
                modifier = Modifier.heightIn(max = sheetMaxHeight),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(onClick = onViewAllClick) {
                        Text(
                            text = "전체보기",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    state.items.forEachIndexed { index, item ->
                        RecentDestinationRow(
                            state = item,
                            onRouteClick = { onRouteClick(item.placeId) },
                        )
                        if (index != state.items.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentDestinationRow(
    state: RecentDestinationRowState,
    onRouteClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f),
        ) {
            Icon(
                painter = painterResource(id = state.iconRes),
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(10.dp)
                        .size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.tags.isNotEmpty() || state.overflowTagCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.tags.forEach { label ->
                        RecentDestinationTagChip(
                            label = label,
                            isOverflow = false,
                        )
                    }
                    if (state.overflowTagCount > 0) {
                        RecentDestinationTagChip(
                            label = "+${state.overflowTagCount}",
                            isOverflow = true,
                        )
                    }
                }
            }
        }

        Button(
            onClick = onRouteClick,
            shape = RoundedCornerShape(12.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_nav_route),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "길찾기",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun RecentDestinationTagChip(
    label: String,
    isOverflow: Boolean,
) {
    val containerColor =
        if (isOverflow) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
        }
    val contentColor =
        if (isOverflow) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (isOverflow) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    },
            ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
