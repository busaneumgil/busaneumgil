package com.ssafy.e102.eumgil.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.data.repository.ReportDraftData
import com.ssafy.e102.eumgil.data.repository.ReportOutboxData
import com.ssafy.e102.eumgil.data.repository.ReportRepository
import com.ssafy.e102.eumgil.data.repository.ReportSubmitFailureReason
import com.ssafy.e102.eumgil.data.repository.ReportSubmitResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReportViewModel(
    private val reportRepository: ReportRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<ReportUiEvent>()
    val uiEvent: SharedFlow<ReportUiEvent> = mutableUiEvent.asSharedFlow()

    private var latestDraft: ReportDraftData? = null

    init {
        loadLatestDraft()
    }

    fun onAction(action: ReportUiAction) {
        when (action) {
            ReportUiAction.BackClicked -> handleBackClicked()
            ReportUiAction.DraftDiscardClicked -> discardDraft()
            ReportUiAction.DraftResumeClicked -> resumeDraft()
            ReportUiAction.SaveDraftClicked -> saveDraft()
            is ReportUiAction.DiscardDraftAndStartNew -> handleDiscardDraftAndStartNew(action.type)
            ReportUiAction.ResumeDraftFromDialog -> resumeDraft()

            is ReportUiAction.ReportTypeSelected -> selectReportType(action.type)
            ReportUiAction.ReportTypeBlurred -> touchReportType()
            ReportUiAction.CurrentLocationResetClicked -> setCurrentLocationShell()
            ReportUiAction.LocationPickerClicked -> setPickedLocationShell()
            is ReportUiAction.LocationSelected -> selectLocation(action.location, action.source)
            is ReportUiAction.AddressTextChanged -> updateAddressText(action.address)
            ReportUiAction.LocationBlurred -> touchLocation()
            ReportUiAction.PhotoAddClicked -> addPhotoShell()
            is ReportUiAction.PhotoSelected -> selectPhoto(action.photo)
            is ReportUiAction.PhotoRemovedAt -> removePhotoAt(action.index)
            ReportUiAction.PhotoBlurred -> touchPhoto()
            is ReportUiAction.DescriptionChanged -> updateDescription(action.description)
            ReportUiAction.DescriptionBlurred -> touchDescription()
            ReportUiAction.NextStepClicked -> advanceStep()
            ReportUiAction.ReportHistoryClicked -> {
                if (mutableUiState.value.screenState is ReportScreenState.Completed) {
                    resetForm()
                }
                emitUiEvent(ReportUiEvent.NavigateToReportHistory)
            }
            ReportUiAction.StartNewReportClicked -> {
                if (mutableUiState.value.screenState is ReportScreenState.Completed) {
                    resetForm()
                }
            }
            ReportUiAction.BackToMapClicked -> {
                if (mutableUiState.value.screenState is ReportScreenState.Completed) {
                    resetForm()
                }
                emitUiEvent(ReportUiEvent.NavigateToMap)
            }
            ReportUiAction.TabReentered -> handleTabReentered()
            ReportUiAction.SubmitClicked,
            ReportUiAction.RetrySubmitClicked -> submitReport()
        }
    }

    private fun handleTabReentered() {
        // 완료 화면에서 머무르지 않고 다른 탭으로 떠난 뒤 다시 진입한 경우에만 새 제보로 초기화.
        // Editing/Submitting/Failure 상태는 사용자가 작성·재시도 중이므로 보존한다.
        if (mutableUiState.value.screenState is ReportScreenState.Completed) {
            resetForm()
        }
    }

    private fun handleBackClicked() {
        val currentStep = mutableUiState.value.currentStep
        val previousStep = currentStep.previousOrNull()
        if (previousStep == null) {
            emitUiEvent(ReportUiEvent.NavigateBack)
        } else {
            mutableUiState.update { state -> state.copy(currentStep = previousStep) }
        }
    }

    private fun advanceStep() {
        val state = mutableUiState.value
        val nextStep =
            when (state.currentStep) {
                ReportStep.TypeSelection ->
                    if (state.reportType.value != null) ReportStep.LocationConfirm else null
                ReportStep.LocationConfirm ->
                    if (state.isLocationStepConfirmable) ReportStep.DetailInput else null
                ReportStep.DetailInput, ReportStep.Complete -> null
            }
        if (nextStep != null) {
            mutableUiState.update { it.copy(currentStep = nextStep) }
        }
    }

    private fun loadLatestDraft() {
        viewModelScope.launch {
            runCatching { reportRepository.getLatestDraft() }
                .onSuccess { draft ->
                    latestDraft = draft
                    if (draft != null) {
                        mutableUiState.update { state ->
                            state.copy(
                                draftId = draft.draftId,
                                hasExistingDraft = true,
                            )
                        }
                    }
                }
        }
    }

    private fun saveDraft() {
        val currentState = mutableUiState.value
        if (!currentState.isDraftSavable) return

        val savedSnapshot = currentState.toDraftSnapshot()

        mutableUiState.update { state ->
            state.copy(draftSaveState = ReportDraftSaveState.Saving)
        }

        viewModelScope.launch {
            runCatching { reportRepository.saveDraft(currentState.toDraftData(latestDraft)) }
                .onSuccess { draft ->
                    latestDraft = draft
                    var isCurrentSnapshot = false
                    mutableUiState.update { state ->
                        isCurrentSnapshot = state.toDraftSnapshot() == savedSnapshot
                        state.copy(
                            draftId = draft.draftId,
                            hasExistingDraft = true,
                            draftSaveState =
                                if (isCurrentSnapshot) {
                                    ReportDraftSaveState.Saved(
                                        draftId = draft.draftId,
                                        savedAtMillis = draft.updatedAtMillis,
                                    )
                                } else {
                                    ReportDraftSaveState.Idle
                                },
                        )
                    }
                    if (isCurrentSnapshot) {
                        emitUiEvent(ReportUiEvent.ShowSnackbar("임시저장했습니다."))
                        emitUiEvent(ReportUiEvent.AnnounceForAccessibility("제보 draft를 임시저장했습니다."))
                    }
                }.onFailure {
                    mutableUiState.update { state ->
                        state.copy(
                            draftSaveState =
                                ReportDraftSaveState.Failed(
                                    reason = ReportFailureReason.LocalSaveFailed,
                                ),
                        )
                    }
                    emitUiEvent(ReportUiEvent.ShowSnackbar("임시저장에 실패했습니다."))
                }
        }
    }

    private fun resumeDraft() {
        val draft = latestDraft ?: return
        mutableUiState.value = draft.toUiState()
    }

    private fun discardDraft() {
        val draftId = mutableUiState.value.draftId ?: latestDraft?.draftId
        if (draftId == null) {
            resetForm()
            return
        }

        viewModelScope.launch {
            runCatching { reportRepository.deleteDraft(draftId) }
                .onSuccess {
                    latestDraft = null
                    resetForm()
                    emitUiEvent(ReportUiEvent.ShowSnackbar("임시저장을 삭제했습니다."))
                }.onFailure {
                    mutableUiState.update { state ->
                        state.copy(
                            draftSaveState =
                                ReportDraftSaveState.Failed(
                                    reason = ReportFailureReason.LocalSaveFailed,
                                ),
                        )
                    }
                    emitUiEvent(ReportUiEvent.ShowSnackbar("임시저장 삭제에 실패했습니다."))
                }
        }
    }

    private fun selectReportType(type: ReportType) {
        val state = mutableUiState.value

        // 같은 type을 다시 누른 경우는 idempotent. TypeSelection 단계라면 다음 단계로만 진행한다.
        if (state.reportType.value == type) {
            if (state.currentStep == ReportStep.TypeSelection) {
                applyReportType(type)
            }
            return
        }

        // 저장된 draft가 DB에 있고 사용자가 "다른" type을 선택했다면,
        // 현재 in-memory state(=draft와 동일하든, 사용자가 뒤로가기로 복귀했든)와 무관하게
        // 그대로 진행 시 저장/제출 시점에 기존 draft가 덮어써지거나 삭제되어 데이터가 손실된다.
        // 명시적 사용자 동의를 위해 다이얼로그를 띄운다.
        val hasSavedDraft = state.hasExistingDraft && state.draftId != null
        if (hasSavedDraft) {
            emitUiEvent(ReportUiEvent.ShowDraftDiscardDialog(pendingType = type))
            return
        }

        applyReportType(type)
    }

    private fun applyReportType(type: ReportType) {
        mutableUiState.update { state ->
            val nextStep =
                if (state.currentStep == ReportStep.TypeSelection) {
                    ReportStep.LocationConfirm
                } else {
                    state.currentStep
                }
            state.copy(
                screenState = ReportScreenState.Editing,
                currentStep = nextStep,
                reportType = state.reportType.withValue(type),
                draftSaveState = ReportDraftSaveState.Idle,
                outboxState = ReportOutboxState.NotSaved,
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun handleDiscardDraftAndStartNew(type: ReportType) {
        val draftId = mutableUiState.value.draftId ?: latestDraft?.draftId
        if (draftId == null) {
            // Draft가 이미 없으면 폼 초기화 후 새 유형을 그대로 적용한다.
            resetForm()
            applyReportType(type)
            return
        }

        viewModelScope.launch {
            runCatching { reportRepository.deleteDraft(draftId) }
                .onSuccess {
                    latestDraft = null
                    resetForm()
                    applyReportType(type)
                    emitUiEvent(ReportUiEvent.ShowSnackbar("임시저장을 삭제했습니다."))
                }.onFailure {
                    mutableUiState.update { state ->
                        state.copy(
                            draftSaveState =
                                ReportDraftSaveState.Failed(
                                    reason = ReportFailureReason.LocalSaveFailed,
                                ),
                        )
                    }
                    emitUiEvent(ReportUiEvent.ShowSnackbar("임시저장 삭제에 실패했습니다."))
                }
        }
    }

    private fun touchReportType() {
        mutableUiState.update { state ->
            state.copy(
                reportType = state.reportType.validated(touched = true),
            )
        }
    }

    private fun setCurrentLocationShell() {
        val shellLocation =
            ReportLocation(
                latitude = 35.1796,
                longitude = 129.0756,
                address = "부산광역시 부산진구 중앙대로 인근",
            )

        selectLocation(
            location = shellLocation,
            source = ReportLocationSource.CurrentLocation,
        )
    }

    private fun setPickedLocationShell() {
        val shellLocation =
            ReportLocation(
                latitude = 35.1578,
                longitude = 129.0592,
                address = "부산광역시 부산진구 서면역 인근",
            )

        selectLocation(
            location = shellLocation,
            source = ReportLocationSource.MapPin,
        )
    }

    private fun selectLocation(
        location: ReportLocation,
        source: ReportLocationSource,
    ) {
        mutableUiState.update { state ->
            state.copy(
                screenState = ReportScreenState.Editing,
                location = state.location.withValue(location, source),
                draftSaveState = ReportDraftSaveState.Idle,
                outboxState = ReportOutboxState.NotSaved,
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun updateAddressText(address: String) {
        mutableUiState.update { state ->
            val currentLocation = state.location.value
            val updatedLocation =
                currentLocation?.copy(address = address.trim().ifEmpty { null })

            state.copy(
                screenState = ReportScreenState.Editing,
                location = state.location.withAddress(address, updatedLocation),
                draftSaveState = ReportDraftSaveState.Idle,
                outboxState = ReportOutboxState.NotSaved,
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun touchLocation() {
        mutableUiState.update { state ->
            state.copy(location = state.location.validated(touched = true))
        }
    }

    private fun addPhotoShell() {
        val nextIndex = mutableUiState.value.photo.values.size
        selectPhoto(
            ReportPhoto(
                localUri = "content://mock/report-photo-$nextIndex.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 1_024_000,
            ),
        )
    }

    private fun selectPhoto(photo: ReportPhoto) {
        mutableUiState.update { state ->
            state.copy(
                screenState = ReportScreenState.Editing,
                photo = state.photo.withAdded(photo),
                draftSaveState = ReportDraftSaveState.Idle,
                outboxState = ReportOutboxState.NotSaved,
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun removePhotoAt(index: Int) {
        mutableUiState.update { state ->
            state.copy(
                screenState = ReportScreenState.Editing,
                photo = state.photo.withRemovedAt(index),
                draftSaveState = ReportDraftSaveState.Idle,
                outboxState = ReportOutboxState.NotSaved,
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun touchPhoto() {
        mutableUiState.update { state ->
            state.copy(photo = state.photo.validated(touched = true))
        }
    }

    private fun updateDescription(description: String) {
        mutableUiState.update { state ->
            state.copy(
                screenState = ReportScreenState.Editing,
                description = state.description.withValue(description),
                draftSaveState = ReportDraftSaveState.Idle,
                outboxState = ReportOutboxState.NotSaved,
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun touchDescription() {
        mutableUiState.update { state ->
            state.copy(description = state.description.validated(touched = true))
        }
    }

    private fun submitReport() {
        val validatedState = mutableUiState.value.validatedForSubmit()
        if (!validatedState.isSubmitEnabled) {
            markSubmitValidationFailed(validatedState)
            return
        }

        mutableUiState.value =
            validatedState.copy(
                screenState = ReportScreenState.Submitting,
                submitState = ReportSubmitState.Submitting,
                outboxState =
                    when (val current = validatedState.outboxState) {
                        is ReportOutboxState.Saved -> current
                        else -> ReportOutboxState.Saving
                    },
            )

        viewModelScope.launch {
            val savedOutbox =
                when (val current = validatedState.outboxState) {
                    is ReportOutboxState.Saved -> {
                        validatedState.toOutboxData().copy(outboxId = current.outboxId)
                    }
                    else -> {
                        runCatching { reportRepository.saveOutbox(validatedState.toOutboxData()) }
                            .getOrElse {
                                handleLocalSaveFailure(validatedState)
                                return@launch
                            }
                    }
                }

            val submitResult =
                runCatching { reportRepository.submitOutboxToServer(savedOutbox.outboxId) }
                    .getOrElse {
                        ReportSubmitResult.Failure(
                            outboxId = savedOutbox.outboxId,
                            reason = ReportSubmitFailureReason.Unknown,
                        )
                    }

            when (submitResult) {
                is ReportSubmitResult.Success ->
                    handleServerSubmitSuccess(
                        validatedState = validatedState,
                        outboxId = savedOutbox.outboxId,
                        serverReportId = submitResult.serverReportId,
                    )
                is ReportSubmitResult.Skipped ->
                    handleServerSubmitSkipped(
                        validatedState = validatedState,
                        outboxId = savedOutbox.outboxId,
                    )
                is ReportSubmitResult.Failure ->
                    handleServerSubmitFailure(
                        validatedState = validatedState,
                        outboxId = savedOutbox.outboxId,
                        reason = submitResult.reason,
                    )
            }
        }
    }

    private fun handleLocalSaveFailure(validatedState: ReportUiState) {
        mutableUiState.value =
            validatedState.copy(
                screenState =
                    ReportScreenState.Failure(reason = ReportFailureReason.LocalSaveFailed),
                submitState =
                    ReportSubmitState.Failed(reason = ReportFailureReason.LocalSaveFailed),
                outboxState =
                    ReportOutboxState.Failed(reason = ReportFailureReason.LocalSaveFailed),
            )
        emitUiEvent(ReportUiEvent.ShowSnackbar("제보 저장에 실패했습니다. 다시 시도해 주세요."))
    }

    private suspend fun handleServerSubmitSuccess(
        validatedState: ReportUiState,
        outboxId: String,
        serverReportId: Long,
    ) {
        val isDraftDeleted = deleteDraftIfPresent(validatedState.draftId)
        mutableUiState.value =
            validatedState.copy(
                screenState = ReportScreenState.Completed,
                currentStep = ReportStep.Complete,
                draftId = if (isDraftDeleted) null else validatedState.draftId,
                hasExistingDraft = !isDraftDeleted && validatedState.hasExistingDraft,
                draftSaveState =
                    if (isDraftDeleted) {
                        ReportDraftSaveState.Idle
                    } else {
                        ReportDraftSaveState.Failed(reason = ReportFailureReason.LocalSaveFailed)
                    },
                outboxState = ReportOutboxState.Saved(outboxId = outboxId),
                submitState = ReportSubmitState.Success(reportId = serverReportId),
                submittedAtMillis = System.currentTimeMillis(),
            )
        emitUiEvent(ReportUiEvent.ShowSnackbar("제보를 등록했습니다."))
        emitUiEvent(ReportUiEvent.AnnounceForAccessibility("제보가 서버에 등록되었습니다."))
        emitUiEvent(
            ReportUiEvent.NavigateToReportComplete(
                reportId = serverReportId,
                outboxId = outboxId,
            ),
        )
    }

    private suspend fun handleServerSubmitSkipped(
        validatedState: ReportUiState,
        outboxId: String,
    ) {
        val isDraftDeleted = deleteDraftIfPresent(validatedState.draftId)
        mutableUiState.value =
            validatedState.copy(
                screenState = ReportScreenState.Completed,
                currentStep = ReportStep.Complete,
                draftId = if (isDraftDeleted) null else validatedState.draftId,
                hasExistingDraft = !isDraftDeleted && validatedState.hasExistingDraft,
                draftSaveState =
                    if (isDraftDeleted) {
                        ReportDraftSaveState.Idle
                    } else {
                        ReportDraftSaveState.Failed(reason = ReportFailureReason.LocalSaveFailed)
                    },
                outboxState = ReportOutboxState.Saved(outboxId = outboxId),
                submitState = ReportSubmitState.Success(reportId = null),
                submittedAtMillis = System.currentTimeMillis(),
            )
        emitUiEvent(ReportUiEvent.ShowSnackbar("제보를 outbox에 저장했습니다."))
        emitUiEvent(ReportUiEvent.AnnounceForAccessibility("제보가 로컬 outbox에 저장되었습니다."))
        emitUiEvent(
            ReportUiEvent.NavigateToReportComplete(
                reportId = null,
                outboxId = outboxId,
            ),
        )
    }

    private fun handleServerSubmitFailure(
        validatedState: ReportUiState,
        outboxId: String,
        reason: ReportSubmitFailureReason,
    ) {
        val mappedReason = reason.toFailureReason()
        mutableUiState.value =
            validatedState.copy(
                screenState = ReportScreenState.Failure(reason = mappedReason),
                outboxState = ReportOutboxState.Saved(outboxId = outboxId),
                submitState = ReportSubmitState.Failed(reason = mappedReason),
            )
        emitUiEvent(ReportUiEvent.ShowSnackbar(mappedReason.toSubmitFailureMessage()))
    }

    private suspend fun deleteDraftIfPresent(draftId: String?): Boolean {
        if (draftId.isNullOrBlank()) return true
        val deleteResult = runCatching { reportRepository.deleteDraft(draftId) }
        if (deleteResult.isSuccess) {
            latestDraft = null
        }
        return deleteResult.isSuccess
    }

    private fun markSubmitValidationFailed(validatedState: ReportUiState) {
        mutableUiState.value =
            validatedState.copy(
                screenState = ReportScreenState.Editing,
                submitState = ReportSubmitState.Idle,
            )
        emitUiEvent(ReportUiEvent.ScrollToFirstError)
    }

    private fun resetForm() {
        mutableUiState.value = ReportUiState()
    }

    private fun emitUiEvent(event: ReportUiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }

    companion object {
        fun provideFactory(reportRepository: ReportRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
                        return ReportViewModel(reportRepository = reportRepository) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}

private fun ReportStep.previousOrNull(): ReportStep? =
    when (this) {
        ReportStep.TypeSelection -> null
        ReportStep.LocationConfirm -> ReportStep.TypeSelection
        ReportStep.DetailInput -> ReportStep.LocationConfirm
        ReportStep.Complete -> null
    }

private fun ReportTypeInput.withValue(type: ReportType): ReportTypeInput =
    copy(
        value = type,
        isTouched = true,
        isDirty = true,
        error = validateReportType(type),
    )

private data class ReportDraftSnapshot(
    val reportType: ReportType?,
    val location: ReportLocation?,
    val addressText: String,
    val photos: List<ReportPhoto>,
    val description: String,
)

private fun ReportUiState.toDraftSnapshot(): ReportDraftSnapshot =
    ReportDraftSnapshot(
        reportType = reportType.value,
        location = location.value,
        addressText = location.addressText,
        photos = photo.values,
        description = description.value,
    )

private fun ReportUiState.toDraftData(existingDraft: ReportDraftData?): ReportDraftData {
    val now = System.currentTimeMillis()
    val locationValue = location.value
    val firstPhoto = photo.firstOrNull
    val draftId = draftId ?: existingDraft?.draftId.orEmpty()

    return ReportDraftData(
        draftId = draftId,
        reportCategory = reportType.value?.apiValue,
        description = description.trimmedValue,
        address = locationValue?.address ?: location.addressText.trim().ifEmpty { null },
        latitude = locationValue?.latitude,
        longitude = locationValue?.longitude,
        locationSource = location.source.name,
        photoUri = firstPhoto?.localUri,
        photoMimeType = firstPhoto?.mimeType,
        photoSizeBytes = firstPhoto?.sizeBytes,
        createdAtMillis = existingDraft?.createdAtMillis ?: 0L,
        updatedAtMillis = now,
    )
}

private fun ReportUiState.toOutboxData(): ReportOutboxData {
    val now = System.currentTimeMillis()
    val reportTypeValue = requireNotNull(reportType.value)
    val locationValue = requireNotNull(location.value)
    val firstPhoto = photo.firstOrNull

    return ReportOutboxData(
        outboxId = "",
        reportCategory = reportTypeValue.apiValue,
        description = description.trimmedValue,
        address = locationValue.address ?: location.addressText.trim().ifEmpty { null },
        latitude = locationValue.latitude,
        longitude = locationValue.longitude,
        photoUri = firstPhoto?.localUri,
        photoMimeType = firstPhoto?.mimeType,
        photoSizeBytes = firstPhoto?.sizeBytes,
        createdAtMillis = now,
        updatedAtMillis = now,
    )
}

private fun ReportDraftData.toUiState(): ReportUiState {
    val reportType = reportCategory.toReportType()
    val location =
        if (latitude != null && longitude != null) {
            ReportLocation(
                latitude = latitude,
                longitude = longitude,
                address = address,
            )
        } else {
            null
        }
    val locationSource = locationSource.toReportLocationSource()
    val photos =
        photoUri?.takeIf(String::isNotBlank)?.let { uri ->
            listOf(
                ReportPhoto(
                    localUri = uri,
                    mimeType = photoMimeType,
                    sizeBytes = photoSizeBytes,
                ),
            )
        } ?: emptyList()

    val resumedStep =
        when {
            location != null -> ReportStep.DetailInput
            reportType != null -> ReportStep.LocationConfirm
            else -> ReportStep.TypeSelection
        }

    return ReportUiState(
        currentStep = resumedStep,
        draftId = draftId,
        hasExistingDraft = true,
        reportType =
            ReportTypeInput(
                value = reportType,
                isDirty = reportType != null,
                error = null,
            ),
        location =
            ReportLocationInput(
                value = location,
                addressText = address.orEmpty(),
                source = locationSource,
                isDirty = location != null || !address.isNullOrBlank(),
                error = if (location == null) null else validateLocation(location, address.orEmpty()),
            ),
        photo =
            ReportPhotoInput(
                values = photos,
                isDirty = photos.isNotEmpty(),
                error = validatePhotos(photos),
            ),
        description =
            ReportDescriptionInput(
                value = description,
                isDirty = description.isNotBlank(),
                error = validateDescription(description),
            ),
        draftSaveState =
            ReportDraftSaveState.Saved(
                draftId = draftId,
                savedAtMillis = updatedAtMillis,
            ),
    )
}

private fun String?.toReportType(): ReportType? =
    ReportType.values().firstOrNull { type -> type.apiValue == this }

private fun String?.toReportLocationSource(): ReportLocationSource =
    this
        ?.let { value ->
            ReportLocationSource.values().firstOrNull { source -> source.name == value }
        }
        ?: ReportLocationSource.Draft

private fun ReportTypeInput.validated(touched: Boolean = isTouched): ReportTypeInput =
    copy(
        isTouched = touched,
        error = validateReportType(value),
    )

private fun ReportLocationInput.withValue(
    location: ReportLocation,
    source: ReportLocationSource,
): ReportLocationInput =
    copy(
        value = location,
        addressText = location.address.orEmpty(),
        source = source,
        isTouched = true,
        isDirty = true,
        isResolvingCurrentLocation = false,
        error = validateLocation(location, location.address.orEmpty()),
    )

private fun ReportLocationInput.withAddress(
    address: String,
    updatedLocation: ReportLocation?,
): ReportLocationInput {
    val nextSource =
        if (updatedLocation == null) {
            source
        } else {
            ReportLocationSource.AddressText
        }
    val shouldValidate =
        isTouched ||
            error != null ||
            address.length > ReportFormLimits.ADDRESS_MAX_LENGTH

    return copy(
        value = updatedLocation,
        addressText = address,
        source = nextSource,
        isDirty = true,
        error = if (shouldValidate) validateLocation(updatedLocation, address) else null,
    )
}

private fun ReportLocationInput.validated(touched: Boolean = isTouched): ReportLocationInput =
    copy(
        isTouched = touched,
        error = validateLocation(value, addressText),
    )

private fun ReportPhotoInput.withAdded(photo: ReportPhoto): ReportPhotoInput {
    val nextValues =
        if (values.size >= ReportFormLimits.PHOTO_MAX_COUNT) {
            values
        } else {
            values + photo
        }
    return copy(
        values = nextValues,
        isTouched = true,
        isDirty = true,
        error = validatePhotos(nextValues),
    )
}

private fun ReportPhotoInput.withRemovedAt(index: Int): ReportPhotoInput {
    val nextValues =
        if (index in values.indices) {
            values.toMutableList().also { it.removeAt(index) }
        } else {
            values
        }
    return copy(
        values = nextValues,
        isTouched = true,
        isDirty = true,
        error = validatePhotos(nextValues),
    )
}

private fun ReportPhotoInput.validated(touched: Boolean = isTouched): ReportPhotoInput =
    copy(
        isTouched = touched,
        error = validatePhotos(values),
    )

private fun ReportDescriptionInput.withValue(description: String): ReportDescriptionInput =
    copy(
        value = description,
        isDirty = true,
        error = validateDescription(description),
    )

private fun ReportDescriptionInput.validated(
    touched: Boolean = isTouched,
): ReportDescriptionInput =
    copy(
        isTouched = touched,
        error = validateDescription(value),
    )

private fun ReportUiState.validatedForSubmit(): ReportUiState =
    copy(
        screenState = ReportScreenState.Editing,
        reportType = reportType.validated(touched = true),
        location = location.validated(touched = true),
        photo = photo.validated(touched = true),
        description = description.validated(touched = true),
        submitState = ReportSubmitState.Idle,
    )

private fun validateReportType(type: ReportType?): ReportTypeError? =
    if (type == null) ReportTypeError.Required else null

private fun validateLocation(
    location: ReportLocation?,
    addressText: String,
): ReportLocationError? {
    if (location == null) return ReportLocationError.Required
    if (!location.hasValidCoordinate()) return ReportLocationError.InvalidCoordinate
    if (addressText.length > ReportFormLimits.ADDRESS_MAX_LENGTH) {
        return ReportLocationError.AddressTooLong
    }

    return null
}

private fun ReportLocation.hasValidCoordinate(): Boolean =
    latitude in -90.0..90.0 && longitude in -180.0..180.0

private fun validatePhotos(photos: List<ReportPhoto>): ReportPhotoError? {
    if (photos.isEmpty()) return null
    if (photos.size > ReportFormLimits.PHOTO_MAX_COUNT) return ReportPhotoError.TooMany
    photos.forEach { photo ->
        if (photo.localUri.isBlank()) return ReportPhotoError.Unreadable
        if (photo.mimeType != null && !photo.mimeType.startsWith("image/")) {
            return ReportPhotoError.UnsupportedFormat
        }
        if (photo.sizeBytes != null && photo.sizeBytes > ReportFormLimits.PHOTO_MAX_BYTES) {
            return ReportPhotoError.TooLarge
        }
    }
    return null
}

private fun validateDescription(description: String): ReportDescriptionError? =
    if (description.length > ReportFormLimits.DESCRIPTION_MAX_LENGTH) {
        ReportDescriptionError.TooLong
    } else {
        null
    }

private fun ReportSubmitFailureReason.toFailureReason(): ReportFailureReason =
    when (this) {
        ReportSubmitFailureReason.Unauthorized -> ReportFailureReason.Unauthorized
        ReportSubmitFailureReason.InvalidInput -> ReportFailureReason.InvalidInput
        ReportSubmitFailureReason.Network -> ReportFailureReason.NetworkUnavailable
        ReportSubmitFailureReason.Unknown -> ReportFailureReason.ServerSubmitFailed
    }

private fun ReportFailureReason.toSubmitFailureMessage(): String =
    when (this) {
        ReportFailureReason.Unauthorized -> "로그인이 만료되었습니다. 다시 로그인 후 시도해 주세요."
        ReportFailureReason.InvalidInput -> "입력값을 확인해 주세요."
        ReportFailureReason.NetworkUnavailable -> "네트워크 연결을 확인하고 다시 시도해 주세요."
        ReportFailureReason.ServerSubmitFailed -> "제보 등록에 실패했습니다. 잠시 후 다시 시도해 주세요."
        ReportFailureReason.LocalSaveFailed -> "제보 저장에 실패했습니다. 다시 시도해 주세요."
        else -> "제보 등록에 실패했습니다. 다시 시도해 주세요."
    }
