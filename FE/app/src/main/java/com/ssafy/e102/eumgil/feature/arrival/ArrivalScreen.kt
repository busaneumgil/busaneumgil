package com.ssafy.e102.eumgil.feature.arrival

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.feature.map.component.MapBottomSheetSurface

private val ArrivalSuccessColor = Color(0xFF16A34A)

@Composable
fun ArrivalScreen(
    uiState: ArrivalUiState,
    onAction: (ArrivalUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = EumSpacing.large, vertical = EumSpacing.medium),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(212.dp)
                            .clip(RoundedCornerShape(EumRadius.large))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.splash_illustration),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(top = 24.dp),
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Surface(
                    shape = RoundedCornerShape(EumRadius.full),
                    color = ArrivalSuccessColor.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = EumSpacing.small,
                                vertical = EumSpacing.xSmall,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_status_check),
                            contentDescription = null,
                            tint = ArrivalSuccessColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(EumSpacing.xxSmall))
                        Text(
                            text = stringResource(id = R.string.arrival_screen_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = ArrivalSuccessColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(EumSpacing.large))

                Text(
                    text = stringResource(id = R.string.arrival_screen_headline),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(EumSpacing.small))
                Text(
                    text = stringResource(id = R.string.arrival_screen_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp),
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = EumSpacing.xxSmall),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                Button(
                    onClick = { onAction(ArrivalUiAction.HomeClicked) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    shape = RoundedCornerShape(EumRadius.medium),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_home_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(EumSpacing.xxSmall))
                    Text(
                        text = stringResource(id = R.string.arrival_action_go_home),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                OutlinedButton(
                    onClick = { onAction(ArrivalUiAction.ExploreNewRouteClicked) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    shape = RoundedCornerShape(EumRadius.medium),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(EumSpacing.xxSmall))
                    Text(
                        text = stringResource(id = R.string.arrival_action_explore_new_route),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.isEvaluationSheetVisible,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 3 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight / 3 }) + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth(),
        ) {
            MapBottomSheetSurface(
                modifier = Modifier.widthIn(max = 520.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
                ) {
                    Text(
                        text = stringResource(id = R.string.arrival_evaluation_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(id = R.string.arrival_evaluation_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(id = R.string.arrival_evaluation_pending_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.arrival_evaluation_save_route))
                    }
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.arrival_evaluation_submit))
                    }
                    TextButton(
                        onClick = { onAction(ArrivalUiAction.EvaluationSheetDismissed) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(text = stringResource(id = R.string.arrival_evaluation_close))
                    }
                }
            }
        }
    }
}
