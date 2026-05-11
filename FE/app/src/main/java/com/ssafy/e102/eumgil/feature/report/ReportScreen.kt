package com.ssafy.e102.eumgil.feature.report

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumCenteredTopBar
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
                showBackButton = reportTopBarShowsBackButton(uiState.currentStep),
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
                    ReportCompleteStep(uiState = uiState)
            }
        }
    }
}

@Composable
private fun ReportTopBar(
    title: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
) {
    EumCenteredTopBar(
        title = title,
        onBackClick = if (showBackButton) onBackClick else null,
        backContentDescription =
            if (showBackButton) {
                stringResource(id = R.string.action_go_back_previous_step)
            } else {
                null
            },
        titleFontWeight = FontWeight.SemiBold,
    )
}

internal fun reportTopBarShowsBackButton(step: ReportStep): Boolean =
    when (step) {
        ReportStep.LocationConfirm, ReportStep.DetailInput -> true
        ReportStep.TypeSelection, ReportStep.Complete -> false
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
        ReportStep.DetailInput -> {
            val submitting = uiState.submitState is ReportSubmitState.Submitting
            ReportPrimaryActionBar(
                label = if (submitting) "제출 중" else "다음",
                enabled = uiState.isSubmitEnabled,
                onClick = { onAction(ReportUiAction.SubmitClicked) },
            )
        }
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
        shape = RoundedCornerShape(EumRadius.medium),
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
                        selected = false,
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
        border = BorderStroke(1.dp, borderColor),
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
                tint = Color.Unspecified,
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
    ReportDescriptionSection(
        input = uiState.description,
        onAction = onAction,
    )
    ReportPhotoSection(
        input = uiState.photo,
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
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        if (isSubmitRecoverable) {
            ReportSubmitFailureBanner(
                reason = (uiState.submitState as? ReportSubmitState.Failed)?.reason
                    ?: (uiState.screenState as? ReportScreenState.Failure)?.reason,
                onRetryClick = { onAction(ReportUiAction.RetrySubmitClicked) },
            )
        }
        if (canSaveDraft) {
            OutlinedButton(
                onClick = { onAction(ReportUiAction.SaveDraftClicked) },
                enabled = !isDraftSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (isDraftSaving) "임시저장 중" else "임시저장")
            }
        }
    }
}

@Composable
private fun ReportSubmitFailureBanner(
    reason: ReportFailureReason?,
    onRetryClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.36f)),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_status_warning),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(id = R.string.report_submit_failure_banner_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(id = reason.toBannerDescriptionRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Button(
                onClick = onRetryClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.report_submit_failure_retry))
            }
        }
    }
}

private fun ReportFailureReason?.toBannerDescriptionRes(): Int =
    when (this) {
        ReportFailureReason.Unauthorized -> R.string.report_submit_failure_unauthorized
        ReportFailureReason.InvalidInput -> R.string.report_submit_failure_invalid_input
        ReportFailureReason.NetworkUnavailable -> R.string.report_submit_failure_network
        ReportFailureReason.LocalSaveFailed -> R.string.report_submit_failure_local_save
        ReportFailureReason.ServerSubmitFailed -> R.string.report_submit_failure_server
        else -> R.string.report_submit_failure_unknown
    }

@Composable
private fun ReportCompleteStep(uiState: ReportUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ReportCompleteHero()
        ReportCompleteSummaryCard(uiState = uiState)
    }
}

@Composable
private fun ReportCompleteHero() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_status_check),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = "제보가 완료되었습니다!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "소중한 제보 감사합니다.\n검토 후 서비스에 반영하겠습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReportCompleteSummaryCard(uiState: ReportUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            ReportCompleteSummaryRow(
                label = "일시",
                value = formatSubmittedAt(uiState.submittedAtMillis),
            )
            ReportCompleteSummaryRow(
                label = "유형",
                value = uiState.reportType.value?.label ?: "-",
            )
            ReportCompleteSummaryRow(
                label = "위치",
                value =
                    uiState.location.value?.address?.takeIf { it.isNotBlank() }
                        ?: uiState.location.addressText.ifBlank { "위치 정보 없음" },
            )
            ReportCompleteSummaryRow(
                label = "설명",
                value = uiState.description.value.trim().ifBlank { "설명 없음" },
            )
        }
    }
}

@Composable
private fun ReportCompleteSummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatSubmittedAt(submittedAtMillis: Long?): String {
    val millis = submittedAtMillis ?: return "-"
    val formatter =
        java.text.SimpleDateFormat(
            "yyyy.MM.dd (E) HH:mm",
            java.util.Locale.KOREA,
        )
    return formatter.format(java.util.Date(millis))
}

@Composable
private fun ReportPhotoSection(
    input: ReportPhotoInput,
    onAction: (ReportUiAction) -> Unit,
) {
    val isError = input.error != null
    val helperText =
        reportPhotoErrorText(input.error)
            ?: "사진은 선택 사항입니다. 최대 ${ReportFormLimits.PHOTO_MAX_COUNT}장까지 첨부할 수 있어요."

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(EumRadius.medium),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "사진 첨부 (선택)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${input.count}/${ReportFormLimits.PHOTO_MAX_COUNT}장",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ReportPhotoGrid(
                photos = input.values,
                canAddMore = input.canAddMore,
                onAddClick = { onAction(ReportUiAction.PhotoAddClicked) },
                onRemoveClick = { index -> onAction(ReportUiAction.PhotoRemovedAt(index)) },
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
}

@Composable
private fun ReportPhotoGrid(
    photos: List<ReportPhoto>,
    canAddMore: Boolean,
    onAddClick: () -> Unit,
    onRemoveClick: (Int) -> Unit,
) {
    val itemsPerRow = 3
    val cells: List<PhotoCell> =
        buildList {
            photos.forEachIndexed { index, photo ->
                add(PhotoCell.Item(index = index, photo = photo))
            }
            if (canAddMore) {
                add(PhotoCell.Add)
            }
        }
    if (cells.isEmpty()) {
        // canAddMore=false 이면서 photos가 비어있을 수 없지만, 안전하게 처리.
        return
    }
    cells.chunked(itemsPerRow).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            row.forEach { cell ->
                when (cell) {
                    is PhotoCell.Item ->
                        ReportPhotoThumb(
                            photo = cell.photo,
                            onRemoveClick = { onRemoveClick(cell.index) },
                            modifier = Modifier.weight(1f),
                        )
                    PhotoCell.Add ->
                        ReportPhotoAddTile(
                            onClick = onAddClick,
                            modifier = Modifier.weight(1f),
                        )
                }
            }
            repeat(itemsPerRow - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private sealed interface PhotoCell {
    data class Item(val index: Int, val photo: ReportPhoto) : PhotoCell
    data object Add : PhotoCell
}

@Composable
private fun ReportPhotoThumb(
    photo: ReportPhoto,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .heightIn(min = 96.dp)
                .semantics {
                    contentDescription = "첨부된 사진"
                },
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(EumRadius.small),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "사진",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(EumSpacing.xSmall)
                    .size(24.dp)
                    .clickable(onClick = onRemoveClick)
                    .semantics {
                        contentDescription = "사진 제거"
                    },
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_close),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ReportPhotoAddTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .heightIn(min = 96.dp)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = "사진을 추가합니다."
                },
        shape = RoundedCornerShape(EumRadius.small),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(EumSpacing.xSmall),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_permission_camera),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(EumSpacing.xSmall))
            Text(
                text = "사진 추가",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportDescriptionSection(
    input: ReportDescriptionInput,
    onAction: (ReportUiAction) -> Unit,
) {
    val isError = input.error != null
    val charCountText = "${input.value.length}/${ReportFormLimits.DESCRIPTION_MAX_LENGTH}"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(EumRadius.medium),
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
                text = "상세 설명",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "아래 문제 상황을 자세히 알려주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                placeholder = { Text(text = "예: 보도 중앙에 이동을 막는 장애물이 있어요.") },
                isError = isError,
                minLines = 4,
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text =
                        reportDescriptionErrorText(input.error) ?: charCountText,
                    style = MaterialTheme.typography.labelMedium,
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
}

private fun reportStepTitle(step: ReportStep): String =
    when (step) {
        ReportStep.TypeSelection -> "제보"
        ReportStep.LocationConfirm -> "위치 확인"
        ReportStep.DetailInput -> "상세 정보 입력"
        ReportStep.Complete -> "제보 완료"
    }

private val ReportType.label: String
    get() =
        when (this) {
            ReportType.STAIRS_STEP -> "계단·단차 있음"
            ReportType.BRAILLE_BLOCK -> "점자블록 문제"
            ReportType.SIDEWALK_MISSING -> "인도 없음"
            ReportType.RAMP -> "경사로 문제"
            ReportType.SIDEWALK_WIDTH -> "인도폭 문제"
            ReportType.OTHER_OBSTACLE -> "기타 장애물"
        }

private val ReportType.description: String
    get() =
        when (this) {
            ReportType.STAIRS_STEP -> "안내와 달리 계단이나 단차가 있어요"
            ReportType.BRAILLE_BLOCK -> "손상, 미설치, 잘못된 설치"
            ReportType.SIDEWALK_MISSING -> "안내와 달리 보행 가능한 인도가 없어요"
            ReportType.RAMP -> "경사로 이용이 어렵거나 위험해요"
            ReportType.SIDEWALK_WIDTH -> "인도가 좁아 통행이 어려워요"
            ReportType.OTHER_OBSTACLE -> "위 항목에 없는 보행 불편 상황"
        }

@get:DrawableRes
private val ReportType.iconRes: Int
    get() =
        when (this) {
            ReportType.STAIRS_STEP -> R.drawable.ic_report_stairs
            ReportType.BRAILLE_BLOCK -> R.drawable.ic_report_tactile_damage
            ReportType.SIDEWALK_MISSING -> R.drawable.ic_report_sidewalk
            ReportType.RAMP -> R.drawable.ic_report_ramp
            ReportType.SIDEWALK_WIDTH -> R.drawable.ic_report_roadway
            ReportType.OTHER_OBSTACLE -> R.drawable.ic_report_other
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
        ReportPhotoError.TooMany ->
            "사진은 최대 ${ReportFormLimits.PHOTO_MAX_COUNT}장까지 첨부할 수 있습니다."
        null -> null
    }

private fun reportDescriptionErrorText(error: ReportDescriptionError?): String? =
    when (error) {
        ReportDescriptionError.TooLong -> "설명은 ${ReportFormLimits.DESCRIPTION_MAX_LENGTH}자까지 입력할 수 있습니다."
        null -> null
    }
