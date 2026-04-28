package com.ssafy.e102.eumgil.feature.map.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Immutable
data class FacilityDetailBottomSheetShellState(
    val isVisible: Boolean = false,
    val categoryLabel: String = "",
    val distanceLabel: String = "",
    val title: String = "",
    val address: String = "",
)

@Composable
fun FacilityDetailBottomSheetShell(
    state: FacilityDetailBottomSheetShellState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    detailContent: @Composable ColumnScope.() -> Unit,
    bookmarkContent: (@Composable ColumnScope.() -> Unit)? = null,
    actionContent: @Composable ColumnScope.() -> Unit,
) {
    val scrimInteractionSource = remember { MutableInteractionSource() }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val sheetScrollState = rememberScrollState()
        val sheetMaxHeight = maxHeight * 0.78f

        AnimatedVisibility(
            visible = state.isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.26f))
                        .clickable(
                            interactionSource = scrimInteractionSource,
                            indication = null,
                            onClick = onDismiss,
                        ),
            )
        }

        AnimatedVisibility(
            visible = state.isVisible,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.medium)
                    .navigationBarsPadding()
                    .fillMaxWidth(),
        ) {
            MapBottomSheetSurface(
                modifier =
                    Modifier
                        .heightIn(max = sheetMaxHeight)
                        .verticalScroll(sheetScrollState),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                            ) {
                                FacilityDetailHeaderBadge(
                                    text = state.categoryLabel,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                if (state.distanceLabel.isNotBlank()) {
                                    FacilityDetailHeaderBadge(
                                        text = state.distanceLabel,
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                            }
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = state.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        TextButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_action_close),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Box(modifier = Modifier.width(EumSpacing.xSmall))
                            Text(text = stringResource(id = R.string.map_facility_detail_close))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                        content = detailContent,
                    )

                    bookmarkContent?.let { slotContent ->
                        // 119 reserves a dedicated bookmark slot so 212 can wire toggle/persistence
                        // without reshaping the bottom-sheet section order.
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                            content = slotContent,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                        content = actionContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun FacilityDetailHeaderBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EumRadius.full),
        color = containerColor,
    ) {
        Text(
            text = text,
            modifier =
                Modifier.padding(
                    horizontal = EumSpacing.small,
                    vertical = EumSpacing.xSmall,
                ),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}
