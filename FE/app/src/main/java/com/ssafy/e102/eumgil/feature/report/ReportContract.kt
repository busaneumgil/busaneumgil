package com.ssafy.e102.eumgil.feature.report

object ReportFormLimits {
    const val DESCRIPTION_MAX_LENGTH = 300
    const val ADDRESS_MAX_LENGTH = 120
    const val PHOTO_MAX_BYTES = 10L * 1024L * 1024L
    const val PHOTO_MAX_COUNT = 5
}

data class ReportUiState(
    val screenState: ReportScreenState = ReportScreenState.Editing,
    val currentStep: ReportStep = ReportStep.TypeSelection,
    val draftId: String? = null,
    val hasExistingDraft: Boolean = false,
    val reportType: ReportTypeInput = ReportTypeInput(),
    val location: ReportLocationInput = ReportLocationInput(),
    val photo: ReportPhotoInput = ReportPhotoInput(),
    val description: ReportDescriptionInput = ReportDescriptionInput(),
    val draftSaveState: ReportDraftSaveState = ReportDraftSaveState.Idle,
    val outboxState: ReportOutboxState = ReportOutboxState.NotSaved,
    val submitState: ReportSubmitState = ReportSubmitState.Idle,
    val submittedAtMillis: Long? = null,
) {
    val isDraftSavable: Boolean
        get() = reportType.value != null ||
            location.value != null ||
            photo.values.isNotEmpty() ||
            description.value.isNotBlank()

    val isLocationStepConfirmable: Boolean
        get() = location.value != null && location.error == null

    val isSubmitEnabled: Boolean
        get() = screenState == ReportScreenState.Editing &&
            submitState !is ReportSubmitState.Submitting &&
            reportType.value != null &&
            location.value != null &&
            reportType.error == null &&
            location.error == null &&
            photo.error == null &&
            description.error == null
}

enum class ReportStep {
    TypeSelection,
    LocationConfirm,
    DetailInput,
    Complete,
}

data class ReportTypeInput(
    val value: ReportType? = null,
    val isTouched: Boolean = false,
    val isDirty: Boolean = false,
    val error: ReportTypeError? = null,
)

data class ReportLocationInput(
    val value: ReportLocation? = null,
    val addressText: String = "",
    val source: ReportLocationSource = ReportLocationSource.None,
    val isTouched: Boolean = false,
    val isDirty: Boolean = false,
    val isResolvingCurrentLocation: Boolean = false,
    val error: ReportLocationError? = null,
)

data class ReportPhotoInput(
    val values: List<ReportPhoto> = emptyList(),
    val isTouched: Boolean = false,
    val isDirty: Boolean = false,
    val error: ReportPhotoError? = null,
) {
    val count: Int get() = values.size
    val canAddMore: Boolean get() = values.size < ReportFormLimits.PHOTO_MAX_COUNT
    val firstOrNull: ReportPhoto? get() = values.firstOrNull()
}

data class ReportDescriptionInput(
    val value: String = "",
    val isTouched: Boolean = false,
    val isDirty: Boolean = false,
    val error: ReportDescriptionError? = null,
) {
    val trimmedValue: String
        get() = value.trim()

    val remainingLength: Int
        get() = ReportFormLimits.DESCRIPTION_MAX_LENGTH - value.length
}

data class ReportLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
)

data class ReportPhoto(
    val localUri: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
)

// apiValue codes match server `ReportType` enum defined in 제보 API 명세 (2026-04-29).
enum class ReportType(
    val apiValue: String,
) {
    STAIRS_STEP("STAIRS_STEP"),
    BRAILLE_BLOCK("BRAILLE_BLOCK"),
    SIDEWALK_MISSING("SIDEWALK_MISSING"),
    RAMP("RAMP"),
    SIDEWALK_WIDTH("SIDEWALK_WIDTH"),
    OTHER_OBSTACLE("OTHER_OBSTACLE"),
}

enum class ReportLocationSource {
    None,
    CurrentLocation,
    MapPin,
    AddressText,
    Draft,
}

sealed interface ReportScreenState {
    data object InitialLoading : ReportScreenState

    data object Editing : ReportScreenState

    data object Submitting : ReportScreenState

    data object Completed : ReportScreenState

    data class Failure(
        val reason: ReportFailureReason,
    ) : ReportScreenState
}

sealed interface ReportDraftSaveState {
    data object Idle : ReportDraftSaveState

    data object Saving : ReportDraftSaveState

    data class Saved(
        val draftId: String,
        val savedAtMillis: Long,
    ) : ReportDraftSaveState

    data class Failed(
        val reason: ReportFailureReason,
    ) : ReportDraftSaveState
}

sealed interface ReportOutboxState {
    data object NotSaved : ReportOutboxState

    data object Saving : ReportOutboxState

    data class Saved(
        val outboxId: String,
    ) : ReportOutboxState

    data class Failed(
        val reason: ReportFailureReason,
    ) : ReportOutboxState
}

sealed interface ReportSubmitState {
    data object Idle : ReportSubmitState

    data object Submitting : ReportSubmitState

    data class Success(
        val reportId: Long? = null,
    ) : ReportSubmitState

    data class Failed(
        val reason: ReportFailureReason,
    ) : ReportSubmitState
}

sealed interface ReportUiAction {
    data object BackClicked : ReportUiAction

    data object DraftResumeClicked : ReportUiAction

    data object DraftDiscardClicked : ReportUiAction

    data class ReportTypeSelected(
        val type: ReportType,
    ) : ReportUiAction

    data object ReportTypeBlurred : ReportUiAction

    data object CurrentLocationResetClicked : ReportUiAction

    data object LocationPickerClicked : ReportUiAction

    data class LocationSelected(
        val location: ReportLocation,
        val source: ReportLocationSource,
    ) : ReportUiAction

    data class AddressTextChanged(
        val address: String,
    ) : ReportUiAction

    data object LocationBlurred : ReportUiAction

    data object PhotoAddClicked : ReportUiAction

    data class PhotoSelected(
        val photo: ReportPhoto,
    ) : ReportUiAction

    data class PhotoRemovedAt(
        val index: Int,
    ) : ReportUiAction

    data object PhotoBlurred : ReportUiAction

    data class DescriptionChanged(
        val description: String,
    ) : ReportUiAction

    data object DescriptionBlurred : ReportUiAction

    data object SaveDraftClicked : ReportUiAction

    data object SubmitClicked : ReportUiAction

    data object RetrySubmitClicked : ReportUiAction

    data object NextStepClicked : ReportUiAction

    data object ReportHistoryClicked : ReportUiAction
}

sealed interface ReportUiEvent {
    data object NavigateBack : ReportUiEvent

    data object ShowDraftDiscardDialog : ReportUiEvent

    data object RequestLocationPermission : ReportUiEvent

    data object OpenLocationPicker : ReportUiEvent

    data object OpenPhotoPicker : ReportUiEvent

    data object ScrollToFirstError : ReportUiEvent

    data class AnnounceForAccessibility(
        val message: String,
    ) : ReportUiEvent

    data class ShowSnackbar(
        val message: String,
    ) : ReportUiEvent

    data class NavigateToReportComplete(
        val reportId: Long? = null,
        val outboxId: String? = null,
    ) : ReportUiEvent

    data object NavigateToReportHistory : ReportUiEvent
}

sealed interface ReportFailureReason {
    data object InvalidInput : ReportFailureReason

    data object LocationPermissionDenied : ReportFailureReason

    data object CurrentLocationUnavailable : ReportFailureReason

    data object NetworkUnavailable : ReportFailureReason

    data object LocalSaveFailed : ReportFailureReason

    data object ServerSubmitFailed : ReportFailureReason

    data object Unauthorized : ReportFailureReason

    data object Unknown : ReportFailureReason
}

enum class ReportTypeError {
    Required,
}

enum class ReportLocationError {
    Required,
    InvalidCoordinate,
    AddressTooLong,
    PermissionDenied,
    CurrentLocationUnavailable,
}

enum class ReportPhotoError {
    UnsupportedFormat,
    TooLarge,
    Unreadable,
    TooMany,
}

enum class ReportDescriptionError {
    TooLong,
}
