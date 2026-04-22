package com.ssafy.e102.eumgil.feature.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

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
            NavigationActionBar(
                uiState = uiState,
                onAction = onAction,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            NavigationStatusCard(uiState = uiState)
            NavigationMapPlaceholder(uiState = uiState)
            NavigationGuidanceCard(uiState = uiState)
            NavigationControlCard(uiState = uiState, onAction = onAction)
            NavigationStateReferenceCard()
        }
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
                Text(text = "뒤로")
            }
            Text(
                text = "내비게이션 mock",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NavigationActionBar(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            OutlinedButton(
                onClick = { onAction(NavigationUiAction.EndNavigationClicked) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "안내 종료")
            }
            Button(
                onClick = {
                    if (uiState.screenState == NavigationScreenState.Failure) {
                        onAction(NavigationUiAction.RetryClicked)
                    } else {
                        onAction(NavigationUiAction.CompleteMockNavigationClicked)
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text =
                        if (uiState.screenState == NavigationScreenState.Failure) {
                            "재시도"
                        } else {
                            "도착 처리"
                        },
                )
            }
        }
    }
}

@Composable
private fun NavigationStatusCard(uiState: NavigationUiState) {
    val (title, description) =
        when (uiState.screenState) {
            NavigationScreenState.Preparing ->
                "안내 준비 중" to "경로 정보를 화면에 배치하는 mock 상태입니다."
            NavigationScreenState.Guiding ->
                "안내 중" to "현재 단계, 남은 거리, 예상 시간을 표시합니다."
            NavigationScreenState.Rerouting ->
                "재탐색 필요" to "경로 이탈 또는 조건 변경 시 노출할 상태 시안입니다."
            NavigationScreenState.Completed ->
                "안내 완료" to "목적지 도착 후 지도 복귀 또는 저장 액션으로 확장합니다."
            NavigationScreenState.Failure ->
                "안내 실패" to (uiState.errorMessage ?: "경로 정보를 다시 확인해주세요.")
        }

    NavigationInfoCard(
        title = title,
        description = description,
        isError = uiState.screenState == NavigationScreenState.Failure,
    )
}

@Composable
private fun NavigationMapPlaceholder(uiState: NavigationUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Text(
                text = "지도/경로 영역",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(EumRadius.medium),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(EumSpacing.large),
                    verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "경로 polyline placeholder",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "상태: ${navigationStateLabel(uiState.screenState)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationGuidanceCard(uiState: NavigationUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Text(
                text = "다음 안내",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = uiState.currentInstruction,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            NavigationMetricRow(
                remainingDistanceText = uiState.remainingDistanceText,
                estimatedTimeText = uiState.estimatedTimeText,
                progressText = uiState.progressText,
            )
        }
    }
}

@Composable
private fun NavigationMetricRow(
    remainingDistanceText: String,
    estimatedTimeText: String,
    progressText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        NavigationMetricCard(
            label = "남은 거리",
            value = remainingDistanceText,
            modifier = Modifier.weight(1f),
        )
        NavigationMetricCard(
            label = "예상 시간",
            value = estimatedTimeText,
            modifier = Modifier.weight(1f),
        )
        NavigationMetricCard(
            label = "단계",
            value = progressText,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NavigationMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EumRadius.medium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.small),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NavigationControlCard(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall)) {
                    Text(
                        text = "음성 안내",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (uiState.isVoiceGuidanceEnabled) "켜짐" else "꺼짐",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.isVoiceGuidanceEnabled,
                    onCheckedChange = { onAction(NavigationUiAction.VoiceGuidanceToggled) },
                )
            }
            OutlinedButton(
                onClick = { onAction(NavigationUiAction.RerouteClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (uiState.isRerouting) "재탐색 중" else "재탐색 mock")
            }
        }
    }
}

@Composable
private fun NavigationStateReferenceCard() {
    NavigationInfoCard(
        title = "상태 시안 기준",
        description = "준비 중, 안내 중, 재탐색 필요, 안내 완료, 실패/재시도 가능 상태를 같은 화면 구조에서 표현합니다.",
        isError = false,
    )
}

@Composable
private fun NavigationInfoCard(
    title: String,
    description: String,
    isError: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color =
            if (isError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.36f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun navigationStateLabel(state: NavigationScreenState): String =
    when (state) {
        NavigationScreenState.Preparing -> "준비 중"
        NavigationScreenState.Guiding -> "안내 중"
        NavigationScreenState.Rerouting -> "재탐색 필요"
        NavigationScreenState.Completed -> "안내 완료"
        NavigationScreenState.Failure -> "실패"
    }
