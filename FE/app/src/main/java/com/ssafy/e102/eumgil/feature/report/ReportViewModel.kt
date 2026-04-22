package com.ssafy.e102.eumgil.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.data.repository.ReportDraftData
import com.ssafy.e102.eumgil.data.repository.ReportOutboxData
import com.ssafy.e102.eumgil.data.repository.ReportRepository
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
            ReportUiAction.BackClicked -> emitUiEvent(ReportUiEvent.NavigateBack)
            ReportUiAction.DraftDiscardClicked -> discardDraft()
            ReportUiAction.DraftResumeClicked -> resumeDraft()
            ReportUiAction.SaveDraftClicked -> saveDraft()

            is ReportUiAction.ReportTypeSelected -> selectReportType(action.type)
            ReportUiAction.ReportTypeBlurred -> touchReportType()
            ReportUiAction.CurrentLocationResetClicked -> setCurrentLocationShell()
            ReportUiAction.LocationPickerClicked -> setPickedLocationShell()
            is ReportUiAction.LocationSelected -> selectLocation(action.location, action.source)
            is ReportUiAction.AddressTextChanged -> updateAddressText(action.address)
            ReportUiAction.LocationBlurred -> touchLocation()
            ReportUiAction.PhotoAddClicked -> addPhotoShell()
            is ReportUiAction.PhotoSelected -> selectPhoto(action.photo)
            ReportUiAction.PhotoRemoved -> removePhoto()
            ReportUiAction.PhotoBlurred -> touchPhoto()
            is ReportUiAction.DescriptionChanged -> updateDescription(action.description)
            ReportUiAction.DescriptionBlurred -> touchDescription()
            ReportUiAction.SubmitClicked,
            ReportUiAction.RetrySubmitClicked -> submitReport()
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

        mutableUiState.update { state ->
            state.copy(draftSaveState = ReportDraftSaveState.Saving)
        }

        viewModelScope.launch {
            runCatching { reportRepository.saveDraft(currentState.toDraftData(latestDraft)) }
                .onSuccess { draft ->
                    latestDraft = draft
                    mutableUiState.update { state ->
                        state.copy(
                            draftId = draft.draftId,
                            hasExistingDraft = true,
                            draftSaveState =
                                ReportDraftSaveState.Saved(
                                    draftId = draft.draftId,
                                    savedAtMillis = draft.updatedAtMillis,
                                ),
                        )
                    }
                    emitUiEvent(ReportUiEvent.ShowSnackbar("임시저장했습니다."))
                    emitUiEvent(ReportUiEvent.AnnounceForAccessibility("제보 draft를 임시저장했습니다."))
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
        mutableUiState.update { state ->
            state.copy(
                screenState = ReportScreenState.Editing,
                reportType = state.reportType.withValue(type),
                outboxState = ReportOutboxState.NotSaved,
                submitState = ReportSubmitState.Idle,
            )
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
        selectPhoto(
            ReportPhoto(
                localUri = "content://mock/report-photo.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 1_024_000,
            ),
        )
    }

    private fun selectPhoto(photo: ReportPhoto) {
        mutableUiState.update { state ->
            state.copy(
                screenState = ReportScreenState.Editing,
                photo = state.photo.withValue(photo),
                outboxState = ReportOutboxState.NotSaved,
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun removePhoto() {
        val clearedPhoto =
            ReportPhotoInput(
                isTouched = true,
                isDirty = true,
            )

        mutableUiState.update { state ->
            state.copy(
                screenState = ReportScreenState.Editing,
                photo = clearedPhoto,
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
                outboxState = ReportOutboxState.Saving,
            )

        viewModelScope.launch {
            runCatching { reportRepository.saveOutbox(validatedState.toOutboxData()) }
                .onSuccess { outbox ->
                    validatedState.draftId?.let { draftId ->
                        runCatching { reportRepository.deleteDraft(draftId) }
                    }
                    latestDraft = null
                    mutableUiState.value =
                        validatedState.copy(
                            screenState = ReportScreenState.Completed,
                            draftId = null,
                            hasExistingDraft = false,
                            draftSaveState = ReportDraftSaveState.Idle,
                            outboxState = ReportOutboxState.Saved(outboxId = outbox.outboxId),
                            submitState = ReportSubmitState.Success(reportId = null),
                        )
                    emitUiEvent(ReportUiEvent.ShowSnackbar("제보를 outbox에 저장했습니다."))
                    emitUiEvent(ReportUiEvent.AnnounceForAccessibility("제보가 로컬 outbox에 저장되었습니다."))
                    emitUiEvent(
                        ReportUiEvent.NavigateToReportComplete(
                            reportId = null,
                            outboxId = outbox.outboxId,
                        ),
                    )
                }.onFailure {
                    mutableUiState.value =
                        validatedState.copy(
                            screenState =
                                ReportScreenState.Failure(
                                    reason = ReportFailureReason.LocalSaveFailed,
                                ),
                            submitState =
                                ReportSubmitState.Failed(
                                    reason = ReportFailureReason.LocalSaveFailed,
                                ),
                            outboxState =
                                ReportOutboxState.Failed(
                                    reason = ReportFailureReason.LocalSaveFailed,
                                ),
                        )
                    emitUiEvent(ReportUiEvent.ShowSnackbar("제보 저장에 실패했습니다. 다시 시도해 주세요."))
                }
        }
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

private fun ReportTypeInput.withValue(type: ReportType): ReportTypeInput =
    copy(
        value = type,
        isTouched = true,
        isDirty = true,
        error = validateReportType(type),
    )

private fun ReportUiState.toDraftData(existingDraft: ReportDraftData?): ReportDraftData {
    val now = System.currentTimeMillis()
    val locationValue = location.value
    val photoValue = photo.value
    val draftId = draftId ?: existingDraft?.draftId.orEmpty()

    return ReportDraftData(
        draftId = draftId,
        reportCategory = reportType.value?.apiValue,
        description = description.trimmedValue,
        address = locationValue?.address ?: location.addressText.trim().ifEmpty { null },
        latitude = locationValue?.latitude,
        longitude = locationValue?.longitude,
        locationSource = location.source.name,
        photoUri = photoValue?.localUri,
        photoMimeType = photoValue?.mimeType,
        photoSizeBytes = photoValue?.sizeBytes,
        createdAtMillis = existingDraft?.createdAtMillis ?: 0L,
        updatedAtMillis = now,
    )
}

private fun ReportUiState.toOutboxData(): ReportOutboxData {
    val now = System.currentTimeMillis()
    val reportTypeValue = requireNotNull(reportType.value)
    val locationValue = requireNotNull(location.value)
    val photoValue = photo.value

    return ReportOutboxData(
        outboxId = "",
        reportCategory = reportTypeValue.apiValue,
        description = description.trimmedValue,
        address = locationValue.address ?: location.addressText.trim().ifEmpty { null },
        latitude = locationValue.latitude,
        longitude = locationValue.longitude,
        photoUri = photoValue?.localUri,
        photoMimeType = photoValue?.mimeType,
        photoSizeBytes = photoValue?.sizeBytes,
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
    val photo =
        photoUri?.takeIf(String::isNotBlank)?.let { uri ->
            ReportPhoto(
                localUri = uri,
                mimeType = photoMimeType,
                sizeBytes = photoSizeBytes,
            )
        }

    return ReportUiState(
        draftId = draftId,
        hasExistingDraft = true,
        reportType =
            ReportTypeInput(
                value = reportType,
                isDirty = reportType != null,
                error = validateReportType(reportType),
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
                value = photo,
                isDirty = photo != null,
                error = validatePhoto(photo),
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

private fun ReportPhotoInput.withValue(photo: ReportPhoto): ReportPhotoInput =
    copy(
        value = photo,
        isTouched = true,
        isDirty = true,
        error = validatePhoto(photo),
    )

private fun ReportPhotoInput.validated(touched: Boolean = isTouched): ReportPhotoInput =
    copy(
        isTouched = touched,
        error = validatePhoto(value),
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

private fun validatePhoto(photo: ReportPhoto?): ReportPhotoError? {
    if (photo == null) return null
    if (photo.localUri.isBlank()) return ReportPhotoError.Unreadable
    if (photo.mimeType != null && !photo.mimeType.startsWith("image/")) {
        return ReportPhotoError.UnsupportedFormat
    }
    if (photo.sizeBytes != null && photo.sizeBytes > ReportFormLimits.PHOTO_MAX_BYTES) {
        return ReportPhotoError.TooLarge
    }

    return null
}

private fun validateDescription(description: String): ReportDescriptionError? =
    if (description.length > ReportFormLimits.DESCRIPTION_MAX_LENGTH) {
        ReportDescriptionError.TooLong
    } else {
        null
    }
