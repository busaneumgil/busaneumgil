package com.ssafy.e102.eumgil.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionState
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.location.isFreshCurrentLocation
import com.ssafy.e102.eumgil.data.repository.ReportDraftData
import com.ssafy.e102.eumgil.data.repository.ReportDraftPhotoData
import com.ssafy.e102.eumgil.data.repository.ReportOutboxData
import com.ssafy.e102.eumgil.data.repository.ReportRepository
import com.ssafy.e102.eumgil.data.repository.ReportSubmitFailureReason
import com.ssafy.e102.eumgil.data.repository.ReportSubmitResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ReportViewModel(
    private val reportRepository: ReportRepository,
    private val currentLocationManager: CurrentLocationManager,
    private val locationPermissionManager: LocationPermissionManager,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<ReportUiEvent>()
    val uiEvent: SharedFlow<ReportUiEvent> = mutableUiEvent.asSharedFlow()

    private var latestDraft: ReportDraftData? = null

    // ─── 현재 위치 one-shot resolution 상태 ───────────────────────────────
    // 권한 요청 대기 중인지. true인 동안 RefreshLocationPermission 액션으로 흐름 재개·종료.
    private var pendingCurrentLocationRequest = false
    // 진행 중인 위치 fetch job (timeout 포함). 화면 이탈/취소 시 정리 대상.
    private var currentLocationJob: Job? = null
    // 권한 다이얼로그가 응답 없이 너무 오래 대기 시 fallback timeout.
    private var permissionPendingTimeoutJob: Job? = null

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
            ReportUiAction.CurrentLocationResetClicked -> requestCurrentLocation()
            ReportUiAction.RefreshLocationPermission -> handleRefreshLocationPermission()
            // LocationPickerClicked: REPORT-02에 inline 카카오맵이 임베드되어 사용자가
            // 직접 지도를 드래그·줌으로 위치를 선택하므로 별도 picker 액션 불필요. no-op.
            ReportUiAction.LocationPickerClicked -> Unit
            is ReportUiAction.LocationSelected -> selectLocation(action.location, action.source)
            is ReportUiAction.AddressTextChanged -> updateAddressText(action.address)
            ReportUiAction.LocationBlurred -> touchLocation()
            ReportUiAction.PhotoAddClicked -> handlePhotoAddClicked()
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
                }.onFailure {
                    mutableUiState.update { state ->
                        state.copy(
                            draftSaveState =
                                ReportDraftSaveState.Failed(
                                    reason = ReportFailureReason.LocalSaveFailed,
                                ),
                        )
                    }
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
                }.onFailure {
                    mutableUiState.update { state ->
                        state.copy(
                            draftSaveState =
                                ReportDraftSaveState.Failed(
                                    reason = ReportFailureReason.LocalSaveFailed,
                                ),
                        )
                    }
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

    // ─── 현재 위치 one-shot resolution ─────────────────────────────────────
    //
    // 흐름:
    //   1) "현재 위치로 설정" 버튼 → requestCurrentLocation()
    //   2) 권한 state 확인
    //      - Granted → 즉시 fetchAndApplyCurrentLocation() (last known + 필요시 active fix)
    //      - Denied  → RequestLocationPermission emit, pending 플래그 + lifecycle 타임아웃
    //      - Unavailable → 즉시 에러 + Snackbar 안내
    //   3) (Denied 경로) 사용자가 다이얼로그 응답 → Activity ON_RESUME → Route가
    //      RefreshLocationPermission dispatch → handleRefreshLocationPermission()이 새 state로 분기
    //   4) 권한이 Granted로 바뀌면 fetch로 진입. 여전히 Denied/Unavailable이면 적절한 에러 종료.
    //
    // 정리:
    //   - fetch는 active provider를 임시로 켜고 첫 fresh snapshot 받으면 즉시 stop (Map의
    //     continuous tracking과 간섭 최소화).
    //   - onCleared / 새 요청 시작 시 이전 job 취소.

    private fun requestCurrentLocation() {
        // 같은 요청 중복 클릭 방어: 이미 resolving 중이면 무시.
        if (mutableUiState.value.location.isResolvingCurrentLocation) return

        setResolvingCurrentLocation(true, clearError = true)

        locationPermissionManager.refreshPermissionState()
        when (locationPermissionManager.permissionState.value) {
            is LocationPermissionState.Granted -> startCurrentLocationFetch()
            LocationPermissionState.Denied -> {
                pendingCurrentLocationRequest = true
                startPermissionPendingTimeout()
                emitUiEvent(ReportUiEvent.RequestLocationPermission)
            }
            is LocationPermissionState.Unavailable -> {
                finishCurrentLocationWithUnavailable()
            }
        }
    }

    private fun handleRefreshLocationPermission() {
        // ON_RESUME에서 한 번씩 들어옴. pending 중인 요청만 처리.
        if (!pendingCurrentLocationRequest) return

        locationPermissionManager.refreshPermissionState()
        when (locationPermissionManager.permissionState.value) {
            is LocationPermissionState.Granted -> {
                pendingCurrentLocationRequest = false
                cancelPermissionPendingTimeout()
                startCurrentLocationFetch()
            }
            LocationPermissionState.Denied -> {
                // 사용자가 거부했거나 다이얼로그를 닫음. one-shot 흐름 종료.
                pendingCurrentLocationRequest = false
                cancelPermissionPendingTimeout()
                finishCurrentLocationWithError(ReportLocationError.PermissionDenied)
            }
            is LocationPermissionState.Unavailable -> {
                pendingCurrentLocationRequest = false
                cancelPermissionPendingTimeout()
                finishCurrentLocationWithUnavailable()
            }
        }
    }

    private fun startCurrentLocationFetch() {
        currentLocationJob?.cancel()
        currentLocationJob =
            viewModelScope.launch {
                val snapshot = fetchFreshCurrentLocation()
                if (snapshot != null) {
                    applyFetchedLocation(snapshot)
                } else {
                    finishCurrentLocationWithError(ReportLocationError.CurrentLocationUnavailable)
                }
            }
    }

    private suspend fun fetchFreshCurrentLocation(): LocationSnapshot? {
        // 1) Last known이 fresh면 즉시 사용 (active provider 호출 없이).
        currentLocationManager.refreshLatestLocation()
        currentLocationManager.latestLocation.value
            ?.takeIf { it.isFreshCurrentLocation() }
            ?.let { return it }

        // 2) Active provider 임시 시작 → 첫 fresh snapshot 또는 timeout.
        currentLocationManager.startLocationUpdates()
        return try {
            withTimeoutOrNull(CURRENT_LOCATION_FETCH_TIMEOUT_MS) {
                currentLocationManager.latestLocation
                    .filterNotNull()
                    .filter { it.isFreshCurrentLocation() }
                    .first()
            }
        } finally {
            currentLocationManager.stopLocationUpdates()
        }
    }

    private fun applyFetchedLocation(snapshot: LocationSnapshot) {
        val location =
            ReportLocation(
                latitude = snapshot.latitude,
                longitude = snapshot.longitude,
                address = null, // Task 2.3 reverse geocoding에서 채움. 그 전까지 좌표만.
            )
        selectLocation(location = location, source = ReportLocationSource.CurrentLocation)
        // selectLocation이 isResolvingCurrentLocation을 직접 false로 두지 않으니 명시적으로 클리어.
        setResolvingCurrentLocation(false)
    }

    private fun finishCurrentLocationWithError(error: ReportLocationError) {
        currentLocationJob?.cancel()
        currentLocationJob = null
        mutableUiState.update { state ->
            state.copy(
                location =
                    state.location.copy(
                        isResolvingCurrentLocation = false,
                        error = error,
                    ),
            )
        }
    }

    private fun finishCurrentLocationWithUnavailable() {
        finishCurrentLocationWithError(ReportLocationError.CurrentLocationUnavailable)
    }

    private fun setResolvingCurrentLocation(
        resolving: Boolean,
        clearError: Boolean = false,
    ) {
        mutableUiState.update { state ->
            state.copy(
                location =
                    state.location.copy(
                        isResolvingCurrentLocation = resolving,
                        error = if (clearError) null else state.location.error,
                    ),
            )
        }
    }

    private fun startPermissionPendingTimeout() {
        cancelPermissionPendingTimeout()
        permissionPendingTimeoutJob =
            viewModelScope.launch {
                kotlinx.coroutines.delay(PERMISSION_PENDING_TIMEOUT_MS)
                if (pendingCurrentLocationRequest) {
                    pendingCurrentLocationRequest = false
                    finishCurrentLocationWithError(ReportLocationError.PermissionDenied)
                }
            }
    }

    private fun cancelPermissionPendingTimeout() {
        permissionPendingTimeoutJob?.cancel()
        permissionPendingTimeoutJob = null
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

    /**
     * "+" 사진 추가 버튼 클릭 처리.
     *
     * Task 3.2 (S14P31E102-682)부터는 mock URI를 생성하지 않고 Route에 picker 열기 요청을 emit한다.
     * Route가 시스템 Photo Picker를 띄우고, 사용자 선택 결과는 `PhotoSelected` 액션으로 다시 들어온다.
     * 첨부 cap 초과 시에는 picker 호출 자체를 건너뛴다.
     */
    private fun handlePhotoAddClicked() {
        val currentCount = mutableUiState.value.photo.values.size
        if (currentCount >= ReportFormLimits.PHOTO_MAX_COUNT) {
            return
        }
        emitUiEvent(ReportUiEvent.OpenPhotoPicker)
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

    override fun onCleared() {
        super.onCleared()
        // 화면 이탈 시 진행 중인 위치 fetch, 타임아웃, active provider 모두 정리.
        currentLocationJob?.cancel()
        currentLocationJob = null
        cancelPermissionPendingTimeout()
        pendingCurrentLocationRequest = false
        // Map 등 다른 도메인이 다시 startLocationUpdates를 호출하면 재개됨. 일시 stop OK.
        currentLocationManager.stopLocationUpdates()
    }

    companion object {
        // Active provider로 첫 fresh fix를 받는 최대 대기 시간. 실내·신호 약함 케이스에서
        // 사용자를 무한히 기다리지 않게 막는다. 10초는 Map 화면 동작 감각과 일관.
        private const val CURRENT_LOCATION_FETCH_TIMEOUT_MS = 10_000L
        // 권한 다이얼로그가 응답 없이 머무르는 비정상 케이스 fallback. ON_RESUME이 들어오지
        // 않는 환경에서도 일정 시간 후 흐름을 종료한다.
        private const val PERMISSION_PENDING_TIMEOUT_MS = 20_000L

        fun provideFactory(
            reportRepository: ReportRepository,
            currentLocationManager: CurrentLocationManager,
            locationPermissionManager: LocationPermissionManager,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
                        return ReportViewModel(
                            reportRepository = reportRepository,
                            currentLocationManager = currentLocationManager,
                            locationPermissionManager = locationPermissionManager,
                        ) as T
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
    val draftId = draftId ?: existingDraft?.draftId.orEmpty()

    return ReportDraftData(
        draftId = draftId,
        reportCategory = reportType.value?.apiValue,
        description = description.trimmedValue,
        address = locationValue?.address ?: location.addressText.trim().ifEmpty { null },
        latitude = locationValue?.latitude,
        longitude = locationValue?.longitude,
        locationSource = location.source.name,
        // Task 3.2 (S14P31E102-682)부터 첨부 사진 전체(최대 5장)를 draft에 영속화.
        photos = photo.values.map { it.toDraftPhotoData() },
        createdAtMillis = existingDraft?.createdAtMillis ?: 0L,
        updatedAtMillis = now,
    )
}

private fun ReportPhoto.toDraftPhotoData(): ReportDraftPhotoData =
    ReportDraftPhotoData(
        localUri = localUri,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
    )

private fun ReportDraftPhotoData.toReportPhoto(): ReportPhoto =
    ReportPhoto(
        localUri = localUri,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
    )

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
    // Task 3.2 (S14P31E102-682)부터 photos 리스트(최대 5장)를 그대로 복원. v6 이전에 저장된
    // 단일 사진 draft는 Repository.toData()에서 1-item list로 fallback 변환되어 전달됨.
    val photos = this.photos.map { it.toReportPhoto() }

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
    // 같은 사진(localUri 기준)을 두 번 첨부하면 무시한다.
    // Photo Picker가 같은 사진에 대해 동일 content URI를 돌려주므로 문자열 비교로 충분.
    if (values.any { it.localUri == photo.localUri }) return this
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

