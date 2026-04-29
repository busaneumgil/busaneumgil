package com.ssafy.e102.eumgil.feature.report

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
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
                title = reportStepTitle(uiState.currentStep),
                onBackClick = { onAction(ReportUiAction.BackClicked) },
            )
        },
        bottomBar = {
            ReportBottomBar(
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
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            if (uiState.currentStep == ReportStep.TypeSelection && uiState.hasExistingDraft) {
                ReportDraftBanner(onAction = onAction)
            }

            when (uiState.currentStep) {
                ReportStep.TypeSelection ->
                    ReportTypeStep(
                        input = uiState.reportType,
                        onAction = onAction,
                    )
                ReportStep.LocationConfirm ->
                    ReportLocationStep(
                        input = uiState.location,
                        onAction = onAction,
                    )
                ReportStep.DetailInput ->
                    ReportDetailStep(
                        uiState = uiState,
                        onAction = onAction,
                    )
                ReportStep.Complete ->
                    ReportCompleteStep()
            }
        }
    }
}

@Composable
private fun ReportTopBar(
    title: String,
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
                    .statusBarsPadding()
                    .padding(horizontal = EumSpacing.small, vertical = EumSpacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            TextButton(onClick = onBackClick) {
                Text(text = "뒤로")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ReportBottomBar(
    uiState: ReportUiState,
    onAction: (ReportUiAction) -> Unit,
) {
    when (uiState.currentStep) {
        ReportStep.TypeSelection -> Unit
        ReportStep.LocationConfirm ->
            ReportPrimaryActionBar(
                label = "다음",
                enabled = uiState.isLocationStepConfirmable,
                onClick = { onAction(ReportUiAction.NextStepClicked) },
            )
        ReportStep.DetailInput ->
            ReportPrimaryActionBar(
                label =
                    reportSubmitButtonText(
                        completed = false,
                        submitting = uiState.submitState is ReportSubmitState.Submitting,
                    ),
                enabled = uiState.isSubmitEnabled,
                onClick = { onAction(ReportUiAction.SubmitClicked) },
            )
        ReportStep.Complete ->
            ReportPrimaryActionBar(
                label = "제보 내역 확인하기",
                enabled = true,
                onClick = { onAction(ReportUiAction.ReportHistoryClicked) },
            )
    }
}

@Composable
private fun ReportPrimaryActionBar(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
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
            onClick = onClick,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            contentPadding = PaddingValues(vertical = EumSpacing.small),
        ) {
            Text(text = label)
        }
    }
}

@Composable
private fun ReportDraftBanner(onAction: (ReportUiAction) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = "임시저장된 제보가 있습니다",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                Button(
                    onClick = { onAction(ReportUiAction.DraftResumeClicked) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "불러오기")
                }
                OutlinedButton(
                    onClick = { onAction(ReportUiAction.DraftDiscardClicked) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "삭제")
                }
            }
        }
    }
}

@Composable
private fun ReportTypeStep(
    input: ReportTypeInput,
    onAction: (ReportUiAction) -> Unit,
) {
    val helperText = reportTypeErrorText(input.error) ?: "해당하는 유형을 선택해주세요."
    val isError = input.error != null

    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = "어떤 문제인가요?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = helperText,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Spacer(modifier = Modifier.height(EumSpacing.xSmall))
        ReportType.values().toList().chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                rowItems.forEach { type ->
                    ReportTypeCard(
                        type = type,
                        selected = input.value == type,
                        onClick = { onAction(ReportUiAction.ReportTypeSelected(type)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReportTypeCard(
    type: ReportType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        }
    val backgroundColor =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    val iconTint =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val selectionLabel = if (selected) "선택됨" else "선택 안 됨"

    Surface(
        modifier =
            modifier
                .heightIn(min = 132.dp)
                .clickable(onClick = onClick)
                .semantics {
                    this.selected = selected
                    contentDescription = "${type.label}. ${type.description}. $selectionLabel"
                },
        shape = RoundedCornerShape(EumRadius.large),
        color = backgroundColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(EumSpacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Icon(
                painter = painterResource(id = type.iconRes),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = iconTint,
            )
            Text(
                text = type.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = type.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReportLocationStep(
    input: ReportLocationInput,
    onAction: (ReportUiAction) -> Unit,
) {
    val helperText =
        reportLocationErrorText(input.error)
            ?: "지도 위치를 확인하거나 현재 위치 또는 지도에서 직접 선택할 수 있습니다."
    val isError = input.error != null

    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        ReportMapPlaceholder(location = input.value)
        ReportLocationBottomCard(
            location = input.value,
            addressText = input.addressText,
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
            label = { Text(text = "주소 (선택 입력)") },
            placeholder = { Text(text = "예: 부산광역시 부산진구 중앙대로 인근") },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            minLines = 1,
        )
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

@Composable
private fun ReportMapPlaceholder(location: ReportLocation?) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(EumRadius.medium),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(EumSpacing.medium),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (location == null) "지도 (위치 미선택)" else "지도 (선택된 위치)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(EumSpacing.xSmall))
            Text(
                text = "지도 SDK 연결 전 placeholder입니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReportLocationBottomCard(
    location: ReportLocation?,
    addressText: String,
) {
    val mainAddress =
        location?.address
            ?.takeIf { it.isNotBlank() }
            ?: addressText.ifBlank { null }
    val coordinateLine =
        location?.let {
            "위도 ${"%.4f".format(it.latitude)}, 경도 ${"%.4f".format(it.longitude)}"
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = "선택된 위치",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = mainAddress ?: "아직 위치가 선택되지 않았습니다.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (coordinateLine != null) {
                Text(
                    text = coordinateLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReportDetailStep(
    uiState: ReportUiState,
    onAction: (ReportUiAction) -> Unit,
) {
    ReportPhotoSection(
        input = uiState.photo,
        onAction = onAction,
    )
    ReportDescriptionSection(
        input = uiState.description,
        onAction = onAction,
    )
    ReportDetailDraftActions(
        uiState = uiState,
        onAction = onAction,
    )
}

@Composable
private fun ReportDetailDraftActions(
    uiState: ReportUiState,
    onAction: (ReportUiAction) -> Unit,
) {
    val isDraftSaving = uiState.draftSaveState is ReportDraftSaveState.Saving
    val isSubmitRecoverable =
        uiState.screenState is ReportScreenState.Failure ||
            uiState.submitState is ReportSubmitState.Failed ||
            uiState.outboxState is ReportOutboxState.Failed
    val canSaveDraft = uiState.isDraftSavable

    if (!canSaveDraft && !isSubmitRecoverable) return

    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
    ) {
        if (canSaveDraft) {
            OutlinedButton(
                onClick = { onAction(ReportUiAction.SaveDraftClicked) },
                enabled = !isDraftSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (isDraftSaving) "임시저장 중" else "임시저장")
            }
        }
        if (isSubmitRecoverable) {
            Button(
                onClick = { onAction(ReportUiAction.RetrySubmitClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "다시 제출")
            }
        }
    }
}

@Composable
private fun ReportCompleteStep() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f),
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = "제보가 등록되었습니다",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "제보는 로컬 outbox에 저장되었습니다. 검토 후 지도에 반영됩니다.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        title = "사진 첨부",
        helperText = reportPhotoErrorText(input.error) ?: "사진은 선택 사항입니다.",
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
        title = "상세 설명",
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

private fun reportStepTitle(step: ReportStep): String =
    when (step) {
        ReportStep.TypeSelection -> "장애물 유형 선택"
        ReportStep.LocationConfirm -> "위치 확인"
        ReportStep.DetailInput -> "상세 정보 입력"
        ReportStep.Complete -> "제보 완료"
    }

private val ReportType.label: String
    get() =
        when (this) {
            ReportType.CONSTRUCTION -> "공사/통제"
            ReportType.STAIRS -> "계단/단차"
            ReportType.SLOPE -> "경사 문제"
            ReportType.ELEVATOR -> "엘리베이터 고장"
            ReportType.TACTILE_BLOCK -> "점자블록 문제"
            ReportType.GUIDANCE_BLOCK -> "유도블록 문제"
            ReportType.FACILITY_DAMAGE -> "시설 파손/노후"
            ReportType.OTHER_OBSTACLE -> "기타 장애물"
        }

private val ReportType.description: String
    get() =
        when (this) {
            ReportType.CONSTRUCTION -> "공사중이거나 통행 불가"
            ReportType.STAIRS -> "이동에 불편한 단차"
            ReportType.SLOPE -> "경사가 가파르거나 위험"
            ReportType.ELEVATOR -> "사용 불가 또는 고장"
            ReportType.TACTILE_BLOCK -> "손상, 미설치, 잘못된 설치"
            ReportType.GUIDANCE_BLOCK -> "유도 목적에 잘못되었거나 단절"
            ReportType.FACILITY_DAMAGE -> "파손되었거나 노후된 시설"
            ReportType.OTHER_OBSTACLE -> "기타 불편한 상황"
        }

@get:DrawableRes
private val ReportType.iconRes: Int
    get() =
        when (this) {
            ReportType.CONSTRUCTION -> R.drawable.ic_report_construction
            ReportType.STAIRS -> R.drawable.ic_report_stairs
            ReportType.SLOPE -> R.drawable.ic_report_slope
            ReportType.ELEVATOR -> R.drawable.ic_report_elevator
            ReportType.TACTILE_BLOCK -> R.drawable.ic_report_tactile_damage
            ReportType.GUIDANCE_BLOCK -> R.drawable.ic_report_guidance_block
            ReportType.FACILITY_DAMAGE -> R.drawable.ic_report_facility_damage
            ReportType.OTHER_OBSTACLE -> R.drawable.ic_report_obstacle
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

private fun reportSubmitButtonText(
    completed: Boolean,
    submitting: Boolean,
): String =
    when {
        completed -> "제출 완료"
        submitting -> "제출 중"
        else -> "제보 제출"
    }
