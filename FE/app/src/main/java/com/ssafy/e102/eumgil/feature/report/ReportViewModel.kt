package com.ssafy.e102.eumgil.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<ReportUiEvent>()
    val uiEvent: SharedFlow<ReportUiEvent> = mutableUiEvent.asSharedFlow()

    fun onAction(action: ReportUiAction) {
        when (action) {
            ReportUiAction.BackClicked -> emitUiEvent(ReportUiEvent.NavigateBack)
            ReportUiAction.DraftDiscardClicked -> resetForm()
            ReportUiAction.DraftResumeClicked,
            ReportUiAction.SaveDraftClicked -> Unit

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
            ReportUiAction.RetrySubmitClicked -> submitShell()
        }
    }

    private fun selectReportType(type: ReportType) {
        mutableUiState.update { state ->
            state.copy(
                screenState = ReportScreenState.Editing,
                reportType =
                    state.reportType.copy(
                        value = type,
                        isTouched = true,
                        isDirty = true,
                        error = null,
                    ),
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun touchReportType() {
        mutableUiState.update { state ->
            state.copy(
                reportType = state.reportType.copy(isTouched = true),
            )
        }
    }

    private fun setCurrentLocationShell() {
        selectLocation(
            location =
                ReportLocation(
                    latitude = 35.1796,
                    longitude = 129.0756,
                    address = "부산광역시 부산진구 중앙대로 인근",
                ),
            source = ReportLocationSource.CurrentLocation,
        )
    }

    private fun setPickedLocationShell() {
        selectLocation(
            location =
                ReportLocation(
                    latitude = 35.1578,
                    longitude = 129.0592,
                    address = "부산광역시 부산진구 서면역 인근",
                ),
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
                location =
                    state.location.copy(
                        value = location,
                        addressText = location.address.orEmpty(),
                        source = source,
                        isTouched = true,
                        isDirty = true,
                        isResolvingCurrentLocation = false,
                        error = null,
                    ),
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
                location =
                    state.location.copy(
                        value = updatedLocation,
                        addressText = address,
                        source =
                            if (updatedLocation == null) {
                                state.location.source
                            } else {
                                ReportLocationSource.AddressText
                            },
                        isDirty = true,
                    ),
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun touchLocation() {
        mutableUiState.update { state ->
            state.copy(location = state.location.copy(isTouched = true))
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
                photo =
                    state.photo.copy(
                        value = photo,
                        isTouched = true,
                        isDirty = true,
                        error = null,
                    ),
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun removePhoto() {
        mutableUiState.update { state ->
            state.copy(
                photo =
                    ReportPhotoInput(
                        isTouched = true,
                        isDirty = true,
                    ),
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun touchPhoto() {
        mutableUiState.update { state ->
            state.copy(photo = state.photo.copy(isTouched = true))
        }
    }

    private fun updateDescription(description: String) {
        mutableUiState.update { state ->
            state.copy(
                screenState = ReportScreenState.Editing,
                description =
                    state.description.copy(
                        value = description,
                        isDirty = true,
                        error =
                            if (description.length > ReportFormLimits.DESCRIPTION_MAX_LENGTH) {
                                ReportDescriptionError.TooLong
                            } else {
                                null
                            },
                    ),
                submitState = ReportSubmitState.Idle,
            )
        }
    }

    private fun touchDescription() {
        mutableUiState.update { state ->
            state.copy(description = state.description.copy(isTouched = true))
        }
    }

    private fun submitShell() {
        val currentState = mutableUiState.value
        if (!currentState.isSubmitEnabled) return

        mutableUiState.update { state ->
            state.copy(
                screenState = ReportScreenState.Completed,
                submitState = ReportSubmitState.Success(),
            )
        }
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
        fun provideFactory(): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
                        return ReportViewModel() as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
