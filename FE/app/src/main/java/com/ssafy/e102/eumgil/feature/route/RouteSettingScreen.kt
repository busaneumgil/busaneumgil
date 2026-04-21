package com.ssafy.e102.eumgil.feature.route

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.PlaceDestination

@Composable
fun RouteSettingScreen(
    uiState: RouteSettingUiState,
    onAction: (RouteSettingUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RouteSettingTopBar(
                onBackClick = { onAction(RouteSettingUiAction.BackClicked) },
            )
        },
        bottomBar = {
            RouteSettingBottomBar(
                isRouteFindEnabled = uiState.isRouteFindEnabled,
                isNavigationStartEnabled = uiState.isNavigationStartEnabled,
                onFindRouteClick = { onAction(RouteSettingUiAction.FindRouteClicked) },
                onStartNavigationClick = { onAction(RouteSettingUiAction.StartNavigationClicked) },
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
            RouteSettingStatusCard(uiState = uiState)
            RouteSettingPointCard(
                title = "출발지",
                point = uiState.startPoint,
                actionLabel = "현재 위치 재확인",
                onClick = { onAction(RouteSettingUiAction.StartPointClicked) },
            )
            RouteSettingDestinationCard(
                destination = uiState.destination,
                isSearchEnabled = uiState.isSearchEnabled,
                onClick = { onAction(RouteSettingUiAction.DestinationClicked) },
            )
            RouteSettingOptionSection(
                selectedOptions = uiState.selectedOptions,
                onOptionToggle = { option ->
                    onAction(RouteSettingUiAction.RouteOptionToggled(option))
                },
            )
            RouteSettingPreviewCard(uiState = uiState)
        }
    }
}

@Composable
private fun RouteSettingTopBar(
    onBackClick: () -> Unit,
) {
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
                text = "경로 설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RouteSettingBottomBar(
    isRouteFindEnabled: Boolean,
    isNavigationStartEnabled: Boolean,
    onFindRouteClick: () -> Unit,
    onStartNavigationClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            OutlinedButton(
                onClick = onFindRouteClick,
                enabled = isRouteFindEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "경로 시안 확인")
            }
            Button(
                onClick = onStartNavigationClick,
                enabled = isNavigationStartEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "내비게이션 mock 시작")
            }
        }
    }
}

@Composable
private fun RouteSettingStatusCard(uiState: RouteSettingUiState) {
    val (title, description) =
        when (uiState.screenState) {
            RouteSettingScreenState.Editing ->
                "경로 조건 입력 중" to "출발지와 목적지를 확인하고 접근성 옵션을 선택합니다."
            RouteSettingScreenState.FindingRoute ->
                "경로 확인 중" to "실제 경로 API 연결 전 mock 상태입니다."
            RouteSettingScreenState.PreviewReady ->
                "경로 시안 준비 완료" to "후속 작업에서 실제 경로 미리보기와 내비게이션 시작으로 확장합니다."
            RouteSettingScreenState.Failure ->
                "경로 시안 확인 필요" to (uiState.errorMessage ?: "입력 상태를 다시 확인해주세요.")
        }

    RouteSettingInfoCard(
        title = title,
        description = description,
        isError = uiState.screenState == RouteSettingScreenState.Failure,
    )
}

@Composable
private fun RouteSettingPointCard(
    title: String,
    point: RouteSettingPointUiModel,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            RouteSettingSectionTitle(text = title)
            Text(
                text = point.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = point.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            point.coordinateText?.let { coordinateText ->
                Text(
                    text = coordinateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onClick,
                enabled = point.isAvailable,
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun RouteSettingDestinationCard(
    destination: PlaceDestination?,
    isSearchEnabled: Boolean,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            RouteSettingSectionTitle(text = "목적지")

            if (destination == null) {
                Text(
                    text = "선택된 목적지가 없습니다.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "지도 시설 상세 또는 검색 화면에서 목적지를 선택하면 이 영역에 반영됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RouteSettingDestinationContent(destination = destination)
            }

            OutlinedButton(
                onClick = onClick,
                enabled = isSearchEnabled,
            ) {
                Text(text = if (destination == null) "목적지 검색" else "목적지 변경")
            }
        }
    }
}

@Composable
private fun RouteSettingDestinationContent(destination: PlaceDestination) {
    val address = destination.address ?: "주소 정보 없음"
    val coordinate = "위도 ${"%.5f".format(destination.latitude)}, 경도 ${"%.5f".format(destination.longitude)}"

    Text(
        text = destination.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = address,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = coordinate,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RouteSettingOptionSection(
    selectedOptions: Set<RouteSettingOption>,
    onOptionToggle: (RouteSettingOption) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            RouteSettingSectionTitle(text = "경로 옵션")
            Text(
                text = "실제 탐색 API 연결 전까지 옵션 선택 상태만 고정합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                RouteSettingOption.entries.forEach { option ->
                    FilterChip(
                        selected = option in selectedOptions,
                        onClick = { onOptionToggle(option) },
                        label = { Text(text = option.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteSettingPreviewCard(uiState: RouteSettingUiState) {
    val destinationName = uiState.destination?.name ?: "목적지 미선택"
    val optionSummary =
        if (uiState.selectedOptions.isEmpty()) {
            "선택된 옵션 없음"
        } else {
            uiState.selectedOptions.joinToString { option -> option.label }
        }

    RouteSettingInfoCard(
        title = "경로 미리보기 상태",
        description = "목적지: $destinationName\n옵션: $optionSummary\n남은 거리 1.2km, 예상 18분 mock",
        isError = false,
    )
}

@Composable
private fun RouteSettingInfoCard(
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

@Composable
private fun RouteSettingSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}
