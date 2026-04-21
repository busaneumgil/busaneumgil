package com.ssafy.e102.eumgil.feature.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Composable
fun ReportScreen(
    uiState: ReportUiState,
    onAction: (ReportUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ReportTopBar(
                onBackClick = { onAction(ReportUiAction.BackClicked) },
            )
        },
        bottomBar = {
            ReportSubmitBar(
                enabled = uiState.isSubmitEnabled,
                completed = uiState.screenState == ReportScreenState.Completed,
                onSubmitClick = { onAction(ReportUiAction.SubmitClicked) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            ReportIntroSection(uiState = uiState)
            ReportStatePreviewSection(uiState = uiState)
            ReportTypeSection(
                input = uiState.reportType,
                onAction = onAction,
            )
            ReportLocationSection(
                input = uiState.location,
                onAction = onAction,
            )
            ReportPhotoSection(
                input = uiState.photo,
                onAction = onAction,
            )
            ReportDescriptionSection(
                input = uiState.description,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun ReportTopBar(
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
                text = "도로 상태 제보",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ReportIntroSection(uiState: ReportUiState) {
    val isCompleted = uiState.screenState == ReportScreenState.Completed
    val title =
        if (isCompleted) {
            "제보 입력 흐름 확인 완료"
        } else {
            "현장 상황을 알려주세요"
        }
    val description =
        if (isCompleted) {
            "205 shell 단계에서는 제출 완료 피드백만 표시합니다. 실제 저장과 전송은 207에서 연결합니다."
        } else {
            "제보 유형, 위치, 사진, 설명 순서로 입력합니다. 사진과 설명은 선택 사항입니다."
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color =
            if (isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportStatePreviewSection(uiState: ReportUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = "상태 시안",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ReportStateRow(label = "화면", value = reportScreenStateLabel(uiState.screenState))
            ReportStateRow(label = "임시저장", value = reportDraftStateLabel(uiState))
            ReportStateRow(label = "outbox", value = reportOutboxStateLabel(uiState.outboxState))
            ReportStateRow(label = "제출", value = reportSubmitStateLabel(uiState))
        }
    }
}

@Composable
private fun ReportStateRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReportTypeSection(
    input: ReportTypeInput,
    onAction: (ReportUiAction) -> Unit,
) {
    ReportFormSection(
        title = "1. 제보 유형",
        helperText = reportTypeErrorText(input.error) ?: "도로 공사, 장애물, 점자블록 손상 중 하나를 선택하세요.",
        isError = input.error != null,
    ) {
        ReportType.values().forEach { type ->
            val selected = input.value == type
            if (selected) {
                Button(
                    onClick = { onAction(ReportUiAction.ReportTypeSelected(type)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = type.label)
                }
            } else {
                OutlinedButton(
                    onClick = { onAction(ReportUiAction.ReportTypeSelected(type)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = type.label)
                }
            }
        }
    }
}

@Composable
private fun ReportLocationSection(
    input: ReportLocationInput,
    onAction: (ReportUiAction) -> Unit,
) {
    val location = input.value
    val locationText =
        if (location == null) {
            "아직 위치가 선택되지 않았습니다."
        } else {
            "위도 ${"%.4f".format(location.latitude)}, 경도 ${"%.4f".format(location.longitude)}"
        }

    ReportFormSection(
        title = "2. 위치",
        helperText = reportLocationErrorText(input.error) ?: "위치 선택 후 주소를 확인하거나 현재 위치로 다시 설정할 수 있습니다.",
        isError = input.error != null,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(EumRadius.medium),
        ) {
            Column(
                modifier = Modifier.padding(EumSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = if (location == null) "위치 placeholder" else "선택된 위치",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = input.addressText,
            onValueChange = { onAction(ReportUiAction.AddressTextChanged(it)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState: FocusState ->
                        if (!focusState.isFocused) {
                            onAction(ReportUiAction.LocationBlurred)
                        }
                    },
            label = { Text(text = "주소") },
            placeholder = { Text(text = "예: 부산광역시 부산진구 중앙대로 인근") },
            supportingText = {
                Text(text = "주소만 입력한 상태에서는 제출할 수 없습니다. 위치 선택 액션이 필요합니다.")
            },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            minLines = 1,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            OutlinedButton(
                onClick = { onAction(ReportUiAction.CurrentLocationResetClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "현재 위치로 설정")
            }
            OutlinedButton(
                onClick = { onAction(ReportUiAction.LocationPickerClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "지도에서 위치 선택")
            }
        }
    }
}

@Composable
private fun ReportPhotoSection(
    input: ReportPhotoInput,
    onAction: (ReportUiAction) -> Unit,
) {
    val photo = input.value

    ReportFormSection(
        title = "3. 사진",
        helperText = reportPhotoErrorText(input.error) ?: "사진은 선택 사항입니다. shell 단계에서는 mock 사진 상태만 확인합니다.",
        isError = input.error != null,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(EumRadius.medium),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        ) {
            Column(
                modifier = Modifier.padding(EumSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = if (photo == null) "첨부된 사진 없음" else "사진 1장 첨부됨",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = photo?.localUri ?: "현장 사진을 첨부할 영역입니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (photo == null) {
            OutlinedButton(
                onClick = { onAction(ReportUiAction.PhotoAddClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "사진 첨부")
            }
        } else {
            OutlinedButton(
                onClick = { onAction(ReportUiAction.PhotoRemoved) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "사진 제거")
            }
        }
    }
}

@Composable
private fun ReportDescriptionSection(
    input: ReportDescriptionInput,
    onAction: (ReportUiAction) -> Unit,
) {
    val isError = input.error != null
    val supportingText =
        reportDescriptionErrorText(input.error)
            ?: "상황을 짧게 적어주세요. ${input.value.length}/${ReportFormLimits.DESCRIPTION_MAX_LENGTH}"

    ReportFormSection(
        title = "4. 설명",
        helperText = "설명은 선택 사항입니다.",
        isError = false,
    ) {
        OutlinedTextField(
            value = input.value,
            onValueChange = { onAction(ReportUiAction.DescriptionChanged(it)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState: FocusState ->
                        if (!focusState.isFocused) {
                            onAction(ReportUiAction.DescriptionBlurred)
                        }
                    },
            label = { Text(text = "설명") },
            placeholder = { Text(text = "예: 보도 중앙에 이동을 막는 장애물이 있어요.") },
            supportingText = { Text(text = supportingText) },
            isError = isError,
            minLines = 4,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
        )
    }
}

@Composable
private fun ReportSubmitBar(
    enabled: Boolean,
    completed: Boolean,
    onSubmitClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .imePadding(),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Button(
            onClick = onSubmitClick,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            contentPadding = PaddingValues(vertical = EumSpacing.small),
        ) {
            Text(text = if (completed) "제출 완료" else "제보 제출")
        }
    }
}

@Composable
private fun ReportFormSection(
    title: String,
    helperText: String,
    isError: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(EumRadius.large),
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
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

private val ReportType.label: String
    get() =
        when (this) {
            ReportType.ROAD_CONSTRUCTION -> "도로 공사"
            ReportType.OBSTACLE -> "장애물"
            ReportType.TACTILE_BLOCK_DAMAGE -> "점자블록 손상"
            ReportType.OTHER -> "기타"
        }

private fun reportTypeErrorText(error: ReportTypeError?): String? =
    when (error) {
        ReportTypeError.Required -> "제보 유형을 선택해주세요."
        null -> null
    }

private fun reportLocationErrorText(error: ReportLocationError?): String? =
    when (error) {
        ReportLocationError.Required -> "제보 위치를 선택해주세요."
        ReportLocationError.InvalidCoordinate -> "위치 좌표를 다시 확인해주세요."
        ReportLocationError.AddressTooLong -> "주소가 너무 깁니다."
        ReportLocationError.PermissionDenied -> "위치 권한이 필요합니다."
        ReportLocationError.CurrentLocationUnavailable -> "현재 위치를 불러올 수 없습니다."
        null -> null
    }

private fun reportPhotoErrorText(error: ReportPhotoError?): String? =
    when (error) {
        ReportPhotoError.UnsupportedFormat -> "지원하지 않는 사진 형식입니다."
        ReportPhotoError.TooLarge -> "사진 용량이 너무 큽니다."
        ReportPhotoError.Unreadable -> "사진을 읽을 수 없습니다."
        null -> null
    }

private fun reportDescriptionErrorText(error: ReportDescriptionError?): String? =
    when (error) {
        ReportDescriptionError.TooLong -> "설명은 ${ReportFormLimits.DESCRIPTION_MAX_LENGTH}자까지 입력할 수 있습니다."
        null -> null
    }

private fun reportScreenStateLabel(state: ReportScreenState): String =
    when (state) {
        ReportScreenState.InitialLoading -> "초기 로딩"
        ReportScreenState.Editing -> "입력 중"
        ReportScreenState.Submitting -> "제출 진행 중"
        ReportScreenState.Completed -> "제출 완료 피드백"
        is ReportScreenState.Failure -> "실패 후 재시도 가능"
    }

private fun reportDraftStateLabel(uiState: ReportUiState): String =
    when (uiState.draftSaveState) {
        ReportDraftSaveState.Idle ->
            if (uiState.isDraftSavable) {
                "저장 가능"
            } else {
                "입력 전"
            }
        ReportDraftSaveState.Saving -> "저장 중"
        is ReportDraftSaveState.Saved -> "저장 완료"
        is ReportDraftSaveState.Failed -> "저장 실패"
    }

private fun reportOutboxStateLabel(state: ReportOutboxState): String =
    when (state) {
        ReportOutboxState.NotSaved -> "저장 전"
        ReportOutboxState.Saving -> "outbox 저장 중"
        is ReportOutboxState.Saved -> "outbox 저장 완료"
        is ReportOutboxState.Failed -> "outbox 저장 실패"
    }

private fun reportSubmitStateLabel(uiState: ReportUiState): String {
    val submitStateLabel =
        when (uiState.submitState) {
            ReportSubmitState.Idle -> "대기"
            ReportSubmitState.Submitting -> "제출 중"
            is ReportSubmitState.Success -> "성공"
            is ReportSubmitState.Failed -> "실패"
        }

    return if (uiState.isSubmitEnabled) {
        "$submitStateLabel / 제출 가능"
    } else {
        "$submitStateLabel / 제출 불가"
    }
}
