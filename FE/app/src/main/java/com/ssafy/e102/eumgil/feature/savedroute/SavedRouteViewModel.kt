package com.ssafy.e102.eumgil.feature.savedroute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.model.hasValidCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SavedRouteViewModel(
    private val bookmarkRepository: BookmarkRepository,
    private val destinationSelectionRepository: DestinationSelectionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SavedRouteUiState())
    val uiState: StateFlow<SavedRouteUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<SavedRouteUiEvent>()
    val uiEvent: SharedFlow<SavedRouteUiEvent> = mutableUiEvent.asSharedFlow()

    private var latestPlaces: List<SavedPlaceUiModel> = emptyList()
    private var observeBookmarksJob: Job? = null

    init {
        observeBookmarks()
    }

    fun onAction(action: SavedRouteUiAction) {
        when (action) {
            SavedRouteUiAction.ExploreMapClicked -> emitUiEvent(SavedRouteUiEvent.NavigateToMap)
            is SavedRouteUiAction.BookmarkRemoveClicked -> removeBookmark(action.placeId)
            is SavedRouteUiAction.PlaceClicked -> handoffPlace(action.placeId, SavedRouteUiEvent.NavigateToMap)
            SavedRouteUiAction.RetryClicked -> observeBookmarks()
            is SavedRouteUiAction.RouteGuideClicked ->
                handoffPlace(action.placeId, SavedRouteUiEvent.NavigateToRouteSetting)
            is SavedRouteUiAction.TabSelected -> selectTab(action.tab)
            SavedRouteUiAction.EditModeToggled -> toggleEditMode()
        }
    }

    private fun selectTab(tab: BookmarkTab) {
        mutableUiState.update { state ->
            if (state.selectedTab == tab) {
                state
            } else {
                state.copy(
                    selectedTab = tab,
                    isEditMode = false,
                )
            }
        }
    }

    private fun toggleEditMode() {
        mutableUiState.update { state ->
            if (!state.canEnterEditMode && !state.isEditMode) {
                state
            } else {
                state.copy(isEditMode = !state.isEditMode)
            }
        }
    }

    private fun observeBookmarks() {
        observeBookmarksJob?.cancel()
        mutableUiState.update { state ->
            state.copy(
                screenState = SavedRouteScreenState.LOADING,
                errorMessage = null,
            )
        }
        observeBookmarksJob =
            viewModelScope.launch {
                bookmarkRepository.observeBookmarks()
                    .catch {
                        mutableUiState.update { state ->
                            state.copy(
                                screenState =
                                    if (latestPlaces.isEmpty()) {
                                        SavedRouteScreenState.ERROR
                                    } else {
                                        SavedRouteScreenState.CONTENT
                                    },
                                places = latestPlaces,
                                errorMessage = BOOKMARK_LOAD_FAILURE_MESSAGE,
                            )
                        }
                    }
                    .collectLatest { bookmarks ->
                        latestPlaces = bookmarks.map(BookmarkData::toSavedPlaceUiModel)
                        mutableUiState.update { state ->
                            val nextScreenState =
                                if (latestPlaces.isEmpty()) {
                                    SavedRouteScreenState.EMPTY
                                } else {
                                    SavedRouteScreenState.CONTENT
                                }
                            state.copy(
                                screenState = nextScreenState,
                                places = latestPlaces,
                                errorMessage = null,
                                isEditMode = state.isEditMode && latestPlaces.isNotEmpty(),
                            )
                        }
                    }
            }
    }

    private fun removeBookmark(placeId: String) {
        viewModelScope.launch {
            runCatching {
                bookmarkRepository.deleteBookmark(placeId)
            }.onSuccess {
                emitUiEvent(SavedRouteUiEvent.ShowSnackbar(BOOKMARK_REMOVE_SUCCESS_MESSAGE))
            }.onFailure {
                mutableUiState.update { state ->
                    state.copy(errorMessage = BOOKMARK_REMOVE_FAILURE_MESSAGE)
                }
                emitUiEvent(SavedRouteUiEvent.ShowSnackbar(BOOKMARK_REMOVE_FAILURE_MESSAGE))
            }
        }
    }

    private fun handoffPlace(
        placeId: String,
        event: SavedRouteUiEvent,
    ) {
        val place = latestPlaces.firstOrNull { savedPlace -> savedPlace.placeId == placeId } ?: return
        val destination = place.toPlaceDestination()
        if (!destination.hasValidCoordinate()) {
            mutableUiState.update { state ->
                state.copy(errorMessage = INVALID_PLACE_COORDINATE_MESSAGE)
            }
            emitUiEvent(SavedRouteUiEvent.ShowSnackbar(INVALID_PLACE_COORDINATE_MESSAGE))
            return
        }

        destinationSelectionRepository.updateSelectedDestination(destination)
        emitUiEvent(event)
    }

    private fun emitUiEvent(event: SavedRouteUiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }

    companion object {
        private const val BOOKMARK_LOAD_FAILURE_MESSAGE = "저장한 장소를 불러오지 못했습니다."
        private const val BOOKMARK_REMOVE_SUCCESS_MESSAGE = "저장한 장소에서 삭제했습니다."
        private const val BOOKMARK_REMOVE_FAILURE_MESSAGE = "저장한 장소 삭제에 실패했습니다. 다시 시도해 주세요."
        private const val INVALID_PLACE_COORDINATE_MESSAGE = "저장한 장소의 좌표가 올바르지 않습니다."

        fun provideFactory(
            bookmarkRepository: BookmarkRepository,
            destinationSelectionRepository: DestinationSelectionRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SavedRouteViewModel::class.java)) {
                        return SavedRouteViewModel(
                            bookmarkRepository = bookmarkRepository,
                            destinationSelectionRepository = destinationSelectionRepository,
                        ) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}

private fun BookmarkData.toSavedPlaceUiModel(): SavedPlaceUiModel =
    SavedPlaceUiModel(
        placeId = placeId,
        name = placeName,
        address = address,
        category = category,
        latitude = latitude,
        longitude = longitude,
    )

private fun SavedPlaceUiModel.toPlaceDestination(): PlaceDestination =
    PlaceDestination(
        placeId = placeId,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        category = category.toPlaceCategoryOrNull(),
    )

private fun String?.toPlaceCategoryOrNull(): PlaceCategory? =
    this?.let { value ->
        runCatching { PlaceCategory.valueOf(value) }.getOrNull()
    }
