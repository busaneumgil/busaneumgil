package com.ssafy.e102.eumgil.feature.savedroute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.model.RouteBookmark
import com.ssafy.e102.eumgil.core.model.hasValidCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteBookmarkRepository
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
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
    private val routeBookmarkRepository: RouteBookmarkRepository,
    private val destinationSelectionRepository: DestinationSelectionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SavedRouteUiState())
    val uiState: StateFlow<SavedRouteUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<SavedRouteUiEvent>()
    val uiEvent: SharedFlow<SavedRouteUiEvent> = mutableUiEvent.asSharedFlow()

    private var latestPlaces: List<SavedPlaceUiModel> = emptyList()
    private var latestRoutes: List<SavedRouteBookmarkUiModel> = emptyList()
    private var observePlacesJob: Job? = null
    private var observeRoutesJob: Job? = null

    init {
        observePlaceBookmarks()
        observeRouteBookmarks()
    }

    fun onAction(action: SavedRouteUiAction) {
        when (action) {
            is SavedRouteUiAction.TabSelected -> {
                mutableUiState.update { state ->
                    state.copy(selectedTab = action.tab)
                }
            }
            SavedRouteUiAction.EditClicked -> enterEditMode()
            SavedRouteUiAction.EditDoneClicked -> applyPendingRemovals()
            SavedRouteUiAction.ExploreMapClicked -> emitUiEvent(SavedRouteUiEvent.NavigateToMap)
            SavedRouteUiAction.RetryClicked -> retryCurrentTab()
            is SavedRouteUiAction.PlaceClicked -> handoffPlace(action.placeId, SavedRouteUiEvent.NavigateToMap)
            is SavedRouteUiAction.PlaceRouteGuideClicked ->
                handoffPlace(
                    placeId = action.placeId,
                    event = SavedRouteUiEvent.NavigateToRouteSetting(),
                )
            is SavedRouteUiAction.PlaceDeleteClicked -> togglePlaceRemoval(action.placeId)
            is SavedRouteUiAction.PlaceRemoveClicked -> removePlaceBookmark(action.placeId)
            is SavedRouteUiAction.RouteGuideClicked -> handoffRouteBookmark(action.bookmarkId)
            is SavedRouteUiAction.RouteDeleteClicked -> toggleRouteRemoval(action.bookmarkId)
            is SavedRouteUiAction.RouteRemoveClicked -> removeRouteBookmark(action.bookmarkId)
        }
    }

    private fun enterEditMode() {
        mutableUiState.update { state ->
            if (state.isApplyingEditChanges) {
                state
            } else {
                state.copy(isEditMode = true)
            }
        }
    }

    private fun togglePlaceRemoval(placeId: String) {
        mutableUiState.update { state ->
            if (!state.isEditMode || state.isApplyingEditChanges) {
                state
            } else {
                state.copy(
                    pendingPlaceRemovalIds = state.pendingPlaceRemovalIds.toggled(placeId),
                    placeContent = state.placeContent.copy(errorMessage = null),
                )
            }
        }
    }

    private fun toggleRouteRemoval(bookmarkId: String) {
        mutableUiState.update { state ->
            if (!state.isEditMode || state.isApplyingEditChanges) {
                state
            } else {
                state.copy(
                    pendingRouteRemovalIds = state.pendingRouteRemovalIds.toggled(bookmarkId),
                    routeContent = state.routeContent.copy(errorMessage = null),
                )
            }
        }
    }

    private fun applyPendingRemovals() {
        val currentState = uiState.value
        if (!currentState.isEditMode || currentState.isApplyingEditChanges) return

        val pendingPlaceRemovalIds = currentState.pendingPlaceRemovalIds
        val pendingRouteRemovalIds = currentState.pendingRouteRemovalIds
        if (pendingPlaceRemovalIds.isEmpty() && pendingRouteRemovalIds.isEmpty()) {
            mutableUiState.update { state ->
                state.copy(isEditMode = false)
            }
            return
        }

        mutableUiState.update { state ->
            state.copy(isApplyingEditChanges = true)
        }

        viewModelScope.launch {
            val failedPlaceRemovalIds =
                pendingPlaceRemovalIds.filterTo(mutableSetOf()) { placeId ->
                    runCatching { bookmarkRepository.deleteBookmark(placeId) }.isFailure
                }
            val failedRouteRemovalIds =
                pendingRouteRemovalIds.filterTo(mutableSetOf()) { bookmarkId ->
                    runCatching { routeBookmarkRepository.deleteRouteBookmark(bookmarkId) }.isFailure
                }

            mutableUiState.update { state ->
                state.copy(
                    isEditMode = failedPlaceRemovalIds.isNotEmpty() || failedRouteRemovalIds.isNotEmpty(),
                    isApplyingEditChanges = false,
                    pendingPlaceRemovalIds = failedPlaceRemovalIds,
                    pendingRouteRemovalIds = failedRouteRemovalIds,
                    placeContent =
                        state.placeContent.copy(
                            errorMessage =
                                when {
                                    failedPlaceRemovalIds.isNotEmpty() -> PLACE_BOOKMARK_REMOVE_FAILURE_MESSAGE
                                    pendingPlaceRemovalIds.isNotEmpty() -> null
                                    else -> state.placeContent.errorMessage
                                },
                        ),
                    routeContent =
                        state.routeContent.copy(
                            errorMessage =
                                when {
                                    failedRouteRemovalIds.isNotEmpty() -> ROUTE_BOOKMARK_REMOVE_FAILURE_MESSAGE
                                    pendingRouteRemovalIds.isNotEmpty() -> null
                                    else -> state.routeContent.errorMessage
                                },
                        ),
                )
            }

            when {
                failedPlaceRemovalIds.isEmpty() && failedRouteRemovalIds.isEmpty() ->
                    emitUiEvent(SavedRouteUiEvent.ShowSnackbar(BOOKMARK_REMOVE_SUCCESS_MESSAGE))
                else ->
                    emitUiEvent(SavedRouteUiEvent.ShowSnackbar(BOOKMARK_REMOVE_FAILURE_MESSAGE))
            }
        }
    }

    private fun retryCurrentTab() {
        when (uiState.value.selectedTab) {
            SavedBookmarkTab.PLACE -> observePlaceBookmarks()
            SavedBookmarkTab.ROUTE -> observeRouteBookmarks()
        }
    }

    private fun observePlaceBookmarks() {
        observePlacesJob?.cancel()
        mutableUiState.update { state ->
            state.copy(
                placeContent =
                    state.placeContent.copy(
                        screenState = SavedBookmarkContentState.LOADING,
                        errorMessage = null,
                    ),
            )
        }
        observePlacesJob =
            viewModelScope.launch {
                bookmarkRepository.observeBookmarks()
                    .catch {
                        mutableUiState.update { state ->
                            state.copy(
                                placeContent =
                                    state.placeContent.copy(
                                        screenState =
                                            if (latestPlaces.isEmpty()) {
                                                SavedBookmarkContentState.ERROR
                                            } else {
                                                SavedBookmarkContentState.CONTENT
                                            },
                                        places = latestPlaces,
                                        errorMessage = PLACE_BOOKMARK_LOAD_FAILURE_MESSAGE,
                                    ),
                            )
                        }
                    }
                    .collectLatest { bookmarks ->
                        latestPlaces = bookmarks.map(BookmarkData::toSavedPlaceUiModel)
                        mutableUiState.update { state ->
                            state.copy(
                                placeContent =
                                    state.placeContent.copy(
                                        screenState =
                                            if (latestPlaces.isEmpty()) {
                                                SavedBookmarkContentState.EMPTY
                                            } else {
                                                SavedBookmarkContentState.CONTENT
                                            },
                                        places = latestPlaces,
                                        errorMessage = null,
                                    ),
                            )
                        }
                    }
            }
    }

    private fun observeRouteBookmarks() {
        observeRoutesJob?.cancel()
        mutableUiState.update { state ->
            state.copy(
                routeContent =
                    state.routeContent.copy(
                        screenState = SavedBookmarkContentState.LOADING,
                        errorMessage = null,
                    ),
            )
        }
        observeRoutesJob =
            viewModelScope.launch {
                routeBookmarkRepository.observeRouteBookmarks()
                    .catch {
                        mutableUiState.update { state ->
                            state.copy(
                                routeContent =
                                    state.routeContent.copy(
                                        screenState =
                                            if (latestRoutes.isEmpty()) {
                                                SavedBookmarkContentState.ERROR
                                            } else {
                                                SavedBookmarkContentState.CONTENT
                                            },
                                        routes = latestRoutes,
                                        errorMessage = ROUTE_BOOKMARK_LOAD_FAILURE_MESSAGE,
                                    ),
                            )
                        }
                    }
                    .collectLatest { routeBookmarks ->
                        latestRoutes = routeBookmarks.map(RouteBookmark::toSavedRouteBookmarkUiModel)
                        mutableUiState.update { state ->
                            state.copy(
                                routeContent =
                                    state.routeContent.copy(
                                        screenState =
                                            if (latestRoutes.isEmpty()) {
                                                SavedBookmarkContentState.EMPTY
                                            } else {
                                                SavedBookmarkContentState.CONTENT
                                            },
                                        routes = latestRoutes,
                                        errorMessage = null,
                                    ),
                            )
                        }
                    }
            }
    }

    private fun removePlaceBookmark(placeId: String) {
        viewModelScope.launch {
            runCatching {
                bookmarkRepository.deleteBookmark(placeId)
            }.onSuccess {
                emitUiEvent(SavedRouteUiEvent.ShowSnackbar(PLACE_BOOKMARK_REMOVE_SUCCESS_MESSAGE))
            }.onFailure {
                mutableUiState.update { state ->
                    state.copy(
                        placeContent =
                            state.placeContent.copy(
                                errorMessage = PLACE_BOOKMARK_REMOVE_FAILURE_MESSAGE,
                            ),
                    )
                }
                emitUiEvent(SavedRouteUiEvent.ShowSnackbar(PLACE_BOOKMARK_REMOVE_FAILURE_MESSAGE))
            }
        }
    }

    private fun removeRouteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            runCatching {
                routeBookmarkRepository.deleteRouteBookmark(bookmarkId)
            }.onSuccess {
                emitUiEvent(SavedRouteUiEvent.ShowSnackbar(ROUTE_BOOKMARK_REMOVE_SUCCESS_MESSAGE))
            }.onFailure {
                mutableUiState.update { state ->
                    state.copy(
                        routeContent =
                            state.routeContent.copy(
                                errorMessage = ROUTE_BOOKMARK_REMOVE_FAILURE_MESSAGE,
                            ),
                    )
                }
                emitUiEvent(SavedRouteUiEvent.ShowSnackbar(ROUTE_BOOKMARK_REMOVE_FAILURE_MESSAGE))
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
                state.copy(
                    placeContent =
                        state.placeContent.copy(
                            errorMessage = INVALID_PLACE_COORDINATE_MESSAGE,
                        ),
                )
            }
            emitUiEvent(SavedRouteUiEvent.ShowSnackbar(INVALID_PLACE_COORDINATE_MESSAGE))
            return
        }

        destinationSelectionRepository.setEditingTarget(RouteEditingTarget.DESTINATION)
        destinationSelectionRepository.updateSelectedDestination(destination)
        emitUiEvent(event)
    }

    private fun handoffRouteBookmark(bookmarkId: String) {
        val routeBookmark =
            latestRoutes.firstOrNull { savedRouteBookmark -> savedRouteBookmark.bookmarkId == bookmarkId } ?: return
        val origin = routeBookmark.toOriginPlaceDestination()
        val destination = routeBookmark.toDestinationPlaceDestination()
        if (!origin.hasValidCoordinate() || !destination.hasValidCoordinate()) {
            mutableUiState.update { state ->
                state.copy(
                    routeContent =
                        state.routeContent.copy(
                            errorMessage = INVALID_ROUTE_COORDINATE_MESSAGE,
                        ),
                )
            }
            emitUiEvent(SavedRouteUiEvent.ShowSnackbar(INVALID_ROUTE_COORDINATE_MESSAGE))
            return
        }

        destinationSelectionRepository.setEditingTarget(RouteEditingTarget.DESTINATION)
        destinationSelectionRepository.swapSelections(origin = origin, destination = destination)
        emitUiEvent(
            SavedRouteUiEvent.NavigateToRouteSetting(
                initialRouteOption = routeBookmark.routeOption,
            ),
        )
    }

    private fun emitUiEvent(event: SavedRouteUiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }

    companion object {
        private const val BOOKMARK_REMOVE_SUCCESS_MESSAGE = "선택한 북마크를 삭제했습니다."
        private const val BOOKMARK_REMOVE_FAILURE_MESSAGE = "일부 북마크를 삭제하지 못했습니다. 다시 시도해 주세요."
        private const val PLACE_BOOKMARK_LOAD_FAILURE_MESSAGE = "북마크한 장소를 불러오지 못했습니다."
        private const val ROUTE_BOOKMARK_LOAD_FAILURE_MESSAGE = "북마크한 경로를 불러오지 못했습니다."
        private const val PLACE_BOOKMARK_REMOVE_SUCCESS_MESSAGE = "북마크한 장소를 삭제했습니다."
        private const val PLACE_BOOKMARK_REMOVE_FAILURE_MESSAGE = "북마크한 장소를 삭제하지 못했습니다. 다시 시도해 주세요."
        private const val ROUTE_BOOKMARK_REMOVE_SUCCESS_MESSAGE = "북마크한 경로를 삭제했습니다."
        private const val ROUTE_BOOKMARK_REMOVE_FAILURE_MESSAGE = "북마크한 경로를 삭제하지 못했습니다. 다시 시도해 주세요."
        private const val INVALID_PLACE_COORDINATE_MESSAGE = "북마크한 장소의 좌표가 올바르지 않습니다."
        private const val INVALID_ROUTE_COORDINATE_MESSAGE = "저장한 경로의 좌표가 올바르지 않습니다."

        fun provideFactory(
            bookmarkRepository: BookmarkRepository,
            routeBookmarkRepository: RouteBookmarkRepository,
            destinationSelectionRepository: DestinationSelectionRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SavedRouteViewModel::class.java)) {
                        return SavedRouteViewModel(
                            bookmarkRepository = bookmarkRepository,
                            routeBookmarkRepository = routeBookmarkRepository,
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

private fun RouteBookmark.toSavedRouteBookmarkUiModel(): SavedRouteBookmarkUiModel =
    SavedRouteBookmarkUiModel(
        bookmarkId = bookmarkId,
        routeName = routeName,
        startLabel = startLabel,
        endLabel = endLabel,
        startPoint = startPoint,
        endPoint = endPoint,
        routeOption = routeOption,
        transportMode = transportMode,
        routeOptionLabel = routeOptionLabel,
        distanceMeters = distanceMeters,
        durationMinutes = durationMinutes,
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

private fun SavedRouteBookmarkUiModel.toOriginPlaceDestination(): PlaceDestination =
    PlaceDestination(
        placeId = "route-bookmark-origin:$bookmarkId",
        name = startLabel,
        latitude = startPoint.latitude,
        longitude = startPoint.longitude,
    )

private fun SavedRouteBookmarkUiModel.toDestinationPlaceDestination(): PlaceDestination =
    PlaceDestination(
        placeId = "route-bookmark:$bookmarkId",
        name = endLabel,
        latitude = endPoint.latitude,
        longitude = endPoint.longitude,
    )

private fun String?.toPlaceCategoryOrNull(): PlaceCategory? =
    this?.let { value ->
        runCatching { PlaceCategory.valueOf(value) }.getOrNull()
    }

private fun Set<String>.toggled(value: String): Set<String> =
    if (value in this) {
        this - value
    } else {
        this + value
    }
