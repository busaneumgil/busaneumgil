package com.ssafy.e102.eumgil.feature.map.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Immutable
data class FacilityDetailBottomSheetShellState(
    val isVisible: Boolean = false,
    @DrawableRes val placeIconRes: Int = 0,
    val metaLabel: String = "",
    val title: String = "",
    val address: String = "",
)

@Composable
fun FacilityDetailBottomSheetShell(
    state: FacilityDetailBottomSheetShellState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    detailContent: @Composable ColumnScope.() -> Unit,
    headerActionContent: (@Composable () -> Unit)? = null,
    actionContent: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val detailScrollState = rememberScrollState()
        val sheetMaxHeight = maxHeight * 0.9f

        AnimatedVisibility(
            visible = state.isVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)),
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
            enter =
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                ),
            exit =
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                ),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
        ) {
            MapBottomSheetSurface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = sheetMaxHeight),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Image(
                            painter = painterResource(id = state.placeIconRes),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
                        ) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (state.metaLabel.isNotBlank()) {
                                Text(
                                    text = state.metaLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = state.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        headerActionContent?.let { content ->
                            content()
                        }
                    }

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(detailScrollState),
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                        content = detailContent,
                    )

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                        content = actionContent,
                    )
                }
            }
        }
    }
}

