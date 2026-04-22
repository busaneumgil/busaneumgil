package com.ssafy.e102.eumgil.feature.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.designsystem.theme.BusanEumgilTheme
import com.ssafy.e102.eumgil.feature.navigation.component.NavigationStepCard

@Composable
fun NavigationScreen(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            NavigationTopBar(
                onBackClick = { onAction(NavigationUiAction.BackClicked) },
            )
        },
        bottomBar = {
            NavigationBottomBar(
                uiState = uiState,
                onAction = onAction,
            )
        },
    ) { innerPadding ->
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(EumSpacing.medium),
            shape = RoundedCornerShape(EumRadius.large),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(EumSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
            ) {
                NavigationShellHeader()
                NavigationProgressOverview(uiState = uiState)
                NavigationMapShell(
                    title = uiState.mapPlaceholderTitle,
                    description = uiState.mapPlaceholderDescription,
                )
                NavigationStepCard(uiState = uiState.stepCard)
            }
        }
    }
}

@Composable
private fun NavigationShellHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
    ) {
        Text(
            text = stringResource(id = R.string.navigation_shell_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(id = R.string.navigation_shell_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NavigationProgressOverview(uiState: NavigationUiState) {
    val overview =
        when (uiState.screenState) {
            NavigationScreenState.Loading ->
                NavigationProgressOverviewUiState(
                    statusLabel = "안내 준비",
                    progressTitle = "경로 안내를 준비하고 있습니다",
                    progressDescription = "첫 안내 메시지와 남은 거리 정보를 불러오는 중입니다.",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                )
            NavigationScreenState.Ready -> {
                val remainingDistance = uiState.stepCard.metrics.getOrNull(0)?.value ?: uiState.stepCard.distanceLabel
                val estimatedTime = uiState.stepCard.metrics.getOrNull(1)?.value ?: "확인 중"

                NavigationProgressOverviewUiState(
                    statusLabel = "진행 중",
                    progressTitle = uiState.stepCard.instruction,
                    progressDescription = "$remainingDistance 남음 · 예상 $estimatedTime · 종료는 하단 버튼에서 바로 처리할 수 있습니다.",
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                )
            }
            NavigationScreenState.Empty -> {
                val remainingDistance = uiState.stepCard.metrics.getOrNull(0)?.value ?: "확인 중"
                val estimatedTime = uiState.stepCard.metrics.getOrNull(1)?.value ?: "확인 중"

                NavigationProgressOverviewUiState(
                    statusLabel = "요약 안내",
                    progressTitle = "안내 메시지가 아직 없습니다",
                    progressDescription = "$remainingDistance · $estimatedTime 요약만 먼저 표시합니다.",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
                )
            }
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = overview.containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            NavigationOverviewBadge(label = overview.statusLabel)
            Text(
                text = overview.progressTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = overview.progressDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class NavigationProgressOverviewUiState(
    val statusLabel: String,
    val progressTitle: String,
    val progressDescription: String,
    val containerColor: androidx.compose.ui.graphics.Color,
)

@Composable
private fun NavigationOverviewBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(EumRadius.full),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = EumSpacing.small, vertical = EumSpacing.xSmall),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun NavigationTopBar(onBackClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.small, vertical = EumSpacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            TextButton(onClick = onBackClick) {
                Text(text = stringResource(id = R.string.navigation_back))
            }
            Text(
                text = stringResource(id = R.string.navigation_screen_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NavigationMapShell(
    title: String,
    description: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = stringResource(id = R.string.navigation_map_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            shape = RoundedCornerShape(EumRadius.large),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(EumSpacing.medium),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NavigationBottomBar(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
) {
    val headline =
        when (uiState.screenState) {
            NavigationScreenState.Loading -> "진행 화면을 준비하고 있습니다"
            NavigationScreenState.Ready -> "${uiState.stepCard.distanceLabel} 후 ${uiState.stepCard.emphasisLabel}"
            NavigationScreenState.Empty -> "거리 요약만 먼저 표시하고 있습니다"
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Button(
                onClick = { onAction(NavigationUiAction.ExitNavigationClicked) },
                enabled = uiState.isExitEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = uiState.exitCta.label)
            }
            Text(
                text = uiState.exitCta.supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Navigation Loading")
@Composable
private fun NavigationLoadingPreview() {
    BusanEumgilTheme {
        NavigationScreen(
            uiState = NavigationUiState(),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Navigation Ready")
@Composable
private fun NavigationReadyPreview() {
    BusanEumgilTheme {
        NavigationScreen(
            uiState =
                NavigationUiState(
                    screenState = NavigationScreenState.Ready,
                    mapPlaceholderDescription = "부산역 방향 경로 오버레이가 이 영역에 연결될 예정입니다.",
                    stepCard =
                        NavigationStepCardUiState(
                            statusLabel = "SAFE 우선",
                            emphasisLabel = "위험도 낮음",
                            distanceLabel = "350m",
                            instruction = "350m 앞에서 좌회전 후 횡단보도를 건너세요",
                            supportingText = "부산역 방향으로 Safe Route 경로를 따라 이동합니다.",
                            metrics =
                                listOf(
                                    NavigationStepMetricUiState(label = "남은 거리", value = "980m"),
                                    NavigationStepMetricUiState(label = "예상 시간", value = "16분"),
                                    NavigationStepMetricUiState(label = "진행 단계", value = "1 / 2"),
                                ),
                        ),
                    exitCta =
                        NavigationCtaUiState(
                            label = "내비게이션 종료",
                            supportingText = "안내를 종료하고 지도로 돌아갑니다.",
                            isEnabled = true,
                        ),
                ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Navigation Empty")
@Composable
private fun NavigationEmptyPreview() {
    BusanEumgilTheme {
        NavigationScreen(
            uiState =
                NavigationUiState(
                    screenState = NavigationScreenState.Empty,
                    mapPlaceholderDescription = "부산역 방향 거리 요약을 먼저 표시하고 있습니다.",
                    stepCard =
                        NavigationStepCardUiState(
                            statusLabel = "최단 거리",
                            emphasisLabel = "위험도 보통",
                            distanceLabel = "840m",
                            instruction = "현재 안내 메시지를 준비하지 못했습니다",
                            supportingText = "부산역 방향으로 거리와 예상 시간 요약만 먼저 표시합니다.",
                            metrics =
                                listOf(
                                    NavigationStepMetricUiState(label = "남은 거리", value = "840m"),
                                    NavigationStepMetricUiState(label = "예상 시간", value = "14분"),
                                    NavigationStepMetricUiState(label = "진행 단계", value = "안내 없음"),
                                ),
                        ),
                    exitCta =
                        NavigationCtaUiState(
                            label = "내비게이션 종료",
                            supportingText = "안내를 종료하고 지도로 돌아갑니다.",
                            isEnabled = true,
                        ),
                ),
            onAction = {},
        )
    }
}
