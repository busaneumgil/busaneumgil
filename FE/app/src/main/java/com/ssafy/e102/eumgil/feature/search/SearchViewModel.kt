package com.ssafy.e102.eumgil.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.location.ANDROID_GEOCODER_PROVIDER
import com.ssafy.e102.eumgil.core.model.MapPlaceDetailType
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.core.model.SearchVoiceMode
import com.ssafy.e102.eumgil.core.model.toPlaceDestinationOrNull
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DestinationPreviewRepository
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.NoOpDestinationPreviewRepository
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val VOICE_INPUT_RESULT_PREVIEW_DELAY_MILLIS = 2_000L

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val destinationSelectionRepository: DestinationSelectionRepository,
    private val destinationPreviewRepository: DestinationPreviewRepository = NoOpDestinationPreviewRepository,
    private val placesRepository: PlacesRepository? = null,
) : ViewModel() {
    private val mutableUiState =
        MutableStateFlow(
            SearchUiState(
                editingTarget = destinationSelectionRepository.editingTarget.value,
            ),
        )
    val uiState: StateFlow<SearchUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<SearchUiEvent>()
    val uiEvent: SharedFlow<SearchUiEvent> = mutableUiEvent.asSharedFlow()

    private var searchJob: Job? = null
    private var voiceSearchNavigationJob: Job? = null

    init {
        refreshRecentSearches()
    }

    fun onAction(action: SearchUiAction) {
        when (action) {
            SearchUiAction.BackClicked -> emitUiEvent(SearchUiEvent.NavigateBack)
            is SearchUiAction.EditingTargetConfigured -> configureEditingTarget(action.editingTarget)
            is SearchUiAction.EntryRouteEntered -> enterEntryRoute(preserveState = action.preserveState)
            SearchUiAction.VoiceInputClicked -> emitUiEvent(SearchUiEvent.NavigateToVoiceInput)
            SearchUiAction.VoiceRouteEntered -> enterVoiceRoute()
            SearchUiAction.VoiceCaptureButtonClicked -> startVoiceCapture()
            SearchUiAction.VoiceCaptureEmpty -> handleVoiceCaptureEmpty()
            SearchUiAction.VoiceInputDismissed -> dismissVoiceInput()
            SearchUiAction.ClearQueryClicked -> clearQuery()
            SearchUiAction.SearchSubmitted -> submitSearch()
            is SearchUiAction.VoiceTranscriptReceived ->
                handleVoiceTranscript(
                    transcript = action.transcript,
                    searchQuery = action.searchQuery,
                )
            is SearchUiAction.QueryChanged -> updateQuery(action.query)
            is SearchUiAction.ResultsRouteEntered -> enterResultsRoute(action.query)
            is SearchUiAction.RecentSearchClicked -> submitSearch(keyword = action.keyword)
            is SearchUiAction.RecentSearchDeleteClicked -> deleteRecentSearch(action.keyword)
            SearchUiAction.RecentSearchClearAllClicked -> clearRecentSearches()
            is SearchUiAction.SearchResultClicked -> selectSearchResult(action.result)
            is SearchUiAction.SearchResultPreviewClicked -> previewSearchResult(action.result)
            is SearchUiAction.SearchResultBriefingClicked -> briefSearchResult(action.result)
            is SearchUiAction.BookmarkToggleClicked -> toggleBookmark(action.result)
            is SearchUiAction.LowVisionBookmarkSaveClicked -> saveLowVisionBookmark(action.result)
            SearchUiAction.LoadNextPageClicked -> loadNextSearchPage()
        }
    }

    private fun selectSearchResult(result: SearchResult) {
        if (!handoffSearchResult(result)) return

        emitUiEvent(SearchUiEvent.NavigateToRouteSetting)
    }

    private fun previewSearchResult(result: SearchResult) {
        val destination = result.toPlaceDestinationOrNull()
        if (destination == null) {
            showResultActionError(message = blockedResultMessage(result))
            return
        }

        destinationPreviewRepository.requestPreview(
            destination = destination,
            editingTarget = destinationSelectionRepository.editingTarget.value,
            accessibilityTagKeys = result.accessibilityTagKeys,
            detailType =
                if (result.isVerifiedPlace) {
                    MapPlaceDetailType.INTERNAL_PLACE
                } else if (result.isAddressSearchFallback()) {
                    MapPlaceDetailType.EXTERNAL_ADDRESS
                } else {
                    MapPlaceDetailType.EXTERNAL_POI
                },
            bookmarkTargetId = result.serverPlaceId,
            provider = result.bookmarkProvider(),
            providerPlaceId = result.bookmarkProviderPlaceId(),
        )
        persistRecentDestination(result = result, destination = destination)
        emitUiEvent(SearchUiEvent.NavigateToMapPreview)
    }

    private fun briefSearchResult(result: SearchResult) {
        if (!handoffSearchResult(result)) return

        emitUiEvent(SearchUiEvent.NavigateToRouteBriefing)
    }

    private fun configureEditingTarget(editingTarget: com.ssafy.e102.eumgil.data.repository.RouteEditingTarget) {
        destinationSelectionRepository.setEditingTarget(editingTarget)
        mutableUiState.update { state ->
            state.copy(editingTarget = editingTarget)
        }
    }

    private fun handoffSearchResult(result: SearchResult): Boolean {
        val destination = result.toPlaceDestinationOrNull()
        if (destination == null) {
            showResultActionError(message = blockedResultMessage(result))
            return false
        }

        destinationSelectionRepository.updateSelectionForEditingTarget(destination)
        persistRecentDestination(result = result, destination = destination)
        return true
    }

    private fun persistRecentDestination(
        result: SearchResult,
        destination: com.ssafy.e102.eumgil.core.model.PlaceDestination,
    ) {
        if (!result.isVerifiedPlace) return

        viewModelScope.launch {
            val accessibilityTagKeys =
                runCatching {
                    placesRepository?.getPlaceDetail(checkNotNull(result.serverPlaceId))?.accessibilityTags
                        ?: result.accessibilityTagKeys
                }.getOrDefault(result.accessibilityTagKeys)

            runCatching {
                searchRepository.saveRecentDestination(
                    RecentDestination(
                        placeId = destination.placeId,
                        name = destination.name,
                        address = destination.address,
                        latitude = destination.latitude,
                        longitude = destination.longitude,
                        category = destination.category,
                        accessibilityTagKeys = accessibilityTagKeys,
                    ),
                )
            }
        }
    }

    private fun toggleBookmark(result: SearchResult) {
        val destination = result.toPlaceDestinationOrNull()
        if (destination == null) {
            showResultActionError(message = blockedResultMessage(result))
            return
        }

        viewModelScope.launch {
            runCatching {
                if (bookmarkRepository.isBookmarked(destination.placeId)) {
                    bookmarkRepository.deleteBookmark(destination.placeId)
                } else {
                    bookmarkRepository.saveBookmark(
                        BookmarkData(
                            placeId = destination.placeId,
                            placeName = destination.name,
                            address = destination.address,
                            latitude = destination.latitude,
                            longitude = destination.longitude,
                            category = destination.category?.name,
                            serverPlaceId = result.serverPlaceId?.toLongOrNull(),
                            provider = result.bookmarkProvider(),
                            providerPlaceId = result.bookmarkProviderPlaceId(),
                            providerCategory = destination.category?.name,
                        ),
                    )
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                mutableUiState.update { state ->
                    state.copy(
                        resultState =
                            SearchResultUiState.Error(
                                query = state.query.trim(),
                                message = BOOKMARK_TOGGLE_FAILURE_MESSAGE,
                            ),
                    )
                }
            }
        }
    }

    private fun saveLowVisionBookmark(result: SearchResult) {
        val destination = result.toPlaceDestinationOrNull()
        if (destination == null) {
            showResultActionError(message = blockedResultMessage(result))
            return
        }

        viewModelScope.launch {
            runCatching {
                bookmarkRepository.saveBookmark(
                    BookmarkData(
                        placeId = destination.placeId,
                        placeName = destination.name,
                        address = destination.address,
                        latitude = destination.latitude,
                        longitude = destination.longitude,
                        category = destination.category?.name,
                        serverPlaceId = result.serverPlaceId?.toLongOrNull(),
                        provider = result.bookmarkProvider(),
                        providerPlaceId = result.bookmarkProviderPlaceId(),
                        providerCategory = destination.category?.name,
                    ),
                )
            }.onSuccess {
                emitUiEvent(SearchUiEvent.NavigateToLowVisionBookmark)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                mutableUiState.update { state ->
                    state.copy(
                        resultState =
                            SearchResultUiState.Error(
                                query = state.query.trim(),
                                message = BOOKMARK_TOGGLE_FAILURE_MESSAGE,
                            ),
                    )
                }
            }
        }
    }

    private fun updateQuery(query: String) {
        searchJob?.cancel()
        mutableUiState.update { state ->
            state.copy(
                query = query,
                hasEditedQuery = true,
            )
        }
        renderInputState()
    }

    private fun clearQuery() {
        searchJob?.cancel()
        mutableUiState.update { state ->
            state.copy(
                query = "",
                hasEditedQuery = true,
            )
        }
        renderInputState()
    }

    private fun enterEntryRoute(preserveState: Boolean) {
        if (preserveState) return

        cancelPendingVoiceSearchNavigation()
        searchJob?.cancel()
        mutableUiState.update { state ->
            state.copy(
                query = "",
                hasEditedQuery = false,
                resultState = SearchResultUiState.Initial,
                voiceInputState = SearchVoiceInputUiState(),
            )
        }
    }

    private fun enterVoiceRoute() {
        cancelPendingVoiceSearchNavigation()
        showListeningVoiceInputState()
    }

    private fun startVoiceCapture() {
        cancelPendingVoiceSearchNavigation()
        showListeningVoiceInputState()
        emitUiEvent(SearchUiEvent.StartVoiceCapture)
    }

    private fun showListeningVoiceInputState() {
        mutableUiState.update { state ->
            state.copy(
                voiceInputState =
                    state.voiceInputState.copy(
                        isActive = true,
                        transcript = "",
                        status = SearchVoiceInputStatus.Listening,
                        guidance = SearchVoiceInputGuidance.None,
                    ),
            )
        }
    }

    private fun handleVoiceCaptureEmpty() {
        cancelPendingVoiceSearchNavigation()
        mutableUiState.update { state ->
            state.copy(
                voiceInputState =
                    SearchVoiceInputUiState(
                        isActive = true,
                        transcript = "",
                        status = SearchVoiceInputStatus.Idle,
                        guidance = SearchVoiceInputGuidance.RetryRequired,
                    ),
            )
        }
    }

    private fun dismissVoiceInput() {
        cancelPendingVoiceSearchNavigation()
        val shouldStopCapture = mutableUiState.value.voiceInputState.status == SearchVoiceInputStatus.Listening
        if (shouldStopCapture) {
            emitUiEvent(SearchUiEvent.StopVoiceCapture)
        }
        emitUiEvent(SearchUiEvent.NavigateBack)
    }

    private fun handleVoiceTranscript(
        transcript: String,
        searchQuery: String?,
    ) {
        val normalizedTranscript = transcript.trim()
        if (normalizedTranscript.isEmpty()) return
        val shouldStopCapture = mutableUiState.value.voiceInputState.status == SearchVoiceInputStatus.Listening
        val normalizedSearchQuery = searchQuery?.trim()?.takeIf(String::isNotEmpty)

        cancelPendingVoiceSearchNavigation()
        mutableUiState.update { state ->
            state.copy(
                voiceInputState =
                    SearchVoiceInputUiState(
                        isActive = true,
                        transcript = normalizedTranscript,
                        status = SearchVoiceInputStatus.Recognized,
                        guidance = SearchVoiceInputGuidance.None,
                    ),
            )
        }
        if (shouldStopCapture) {
            emitUiEvent(SearchUiEvent.StopVoiceCapture)
        }
        voiceSearchNavigationJob =
            viewModelScope.launch {
                delay(VOICE_INPUT_RESULT_PREVIEW_DELAY_MILLIS)
                val resolvedKeyword =
                    normalizedSearchQuery
                        ?: runCatching {
                            searchRepository.analyzeVoiceSearch(
                                text = normalizedTranscript,
                                mode = SearchVoiceMode.MOBILITY_IMPAIRED,
                            ).placeName
                        }.getOrNull()
                            ?.trim()
                            ?.takeIf(String::isNotEmpty)
                        ?: normalizedTranscript
                submitSearch(keyword = resolvedKeyword)
            }
    }

    private fun cancelPendingVoiceSearchNavigation() {
        voiceSearchNavigationJob?.cancel()
        voiceSearchNavigationJob = null
    }

    private fun renderInputState() {
        val currentState = mutableUiState.value
        val normalizedQuery = currentState.query.trim()
        val resultState =
            when {
                !currentState.hasEditedQuery && currentState.query.isEmpty() ->
                    SearchResultUiState.Initial

                normalizedQuery.isEmpty() -> SearchResultUiState.EmptyQuery

                else -> SearchResultUiState.Typing(query = normalizedQuery)
            }

        mutableUiState.update { state ->
            state.copy(resultState = resultState)
        }
    }

    private fun enterResultsRoute(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return

        val resultState = mutableUiState.value.resultState
        if (resultState.hasResultQuery(normalizedQuery)) {
            mutableUiState.update { state ->
                state.copy(
                    query = normalizedQuery,
                    hasEditedQuery = true,
                )
            }
            return
        }

        submitSearch(keyword = normalizedQuery, navigateToResults = false)
    }

    private fun submitSearch(
        keyword: String? = null,
        navigateToResults: Boolean = true,
    ) {
        val normalizedQuery = (keyword ?: mutableUiState.value.query).trim()

        if (normalizedQuery.isEmpty()) {
            mutableUiState.update { state ->
                state.copy(
                    query = keyword ?: state.query,
                    hasEditedQuery = true,
                    resultState = SearchResultUiState.EmptyQuery,
                )
            }
            return
        }

        searchJob?.cancel()
        mutableUiState.update { state ->
            state.copy(
                query = normalizedQuery,
                hasEditedQuery = true,
                resultState = SearchResultUiState.Loading(query = normalizedQuery),
            )
        }
        if (navigateToResults) {
            emitUiEvent(
                SearchUiEvent.NavigateToResults(
                    query = normalizedQuery,
                    editingTarget = destinationSelectionRepository.editingTarget.value,
                ),
            )
        }
        searchJob =
            viewModelScope.launch {
                val searchPage =
                    try {
                        searchRepository.searchPage(SearchQuery(keyword = normalizedQuery))
                    } catch (throwable: Throwable) {
                        if (throwable is CancellationException) throw throwable

                        mutableUiState.update { state ->
                            state.copy(
                                resultState =
                                    SearchResultUiState.Error(
                                        query = normalizedQuery,
                                        message = throwable.message,
                                    ),
                            )
                        }
                        return@launch
                    }
                val results = searchPage.results

                val recentSearches =
                    try {
                        searchRepository.saveRecentSearch(normalizedQuery)
                        searchRepository.getRecentSearches()
                    } catch (throwable: Throwable) {
                        if (throwable is CancellationException) throw throwable
                        mutableUiState.value.recentSearches
                    }

                mutableUiState.update { state ->
                    state.copy(
                        recentSearches = recentSearches,
                        resultState =
                            if (results.isEmpty()) {
                                SearchResultUiState.Empty(query = normalizedQuery)
                            } else {
                                SearchResultUiState.Success(
                                    query = normalizedQuery,
                                    results = results,
                                    nextCursor = searchPage.nextCursor,
                                    hasNext = searchPage.hasNext,
                                )
                            },
                    )
                }
            }
    }

    private fun loadNextSearchPage() {
        val currentResultState = mutableUiState.value.resultState as? SearchResultUiState.Success ?: return
        val nextCursor = currentResultState.nextCursor?.trim()?.takeIf(String::isNotEmpty) ?: return
        if (!currentResultState.hasNext || currentResultState.isLoadingNextPage) return

        mutableUiState.update { state ->
            state.copy(resultState = currentResultState.copy(isLoadingNextPage = true))
        }

        searchJob =
            viewModelScope.launch {
                val nextPage =
                    try {
                        searchRepository.searchPage(
                            SearchQuery(
                                keyword = currentResultState.query,
                                cursor = nextCursor,
                            ),
                        )
                    } catch (throwable: Throwable) {
                        if (throwable is CancellationException) throw throwable

                        mutableUiState.update { state ->
                            val latestSuccess = state.resultState as? SearchResultUiState.Success
                            if (latestSuccess == null || latestSuccess.query != currentResultState.query) {
                                state
                            } else {
                                state.copy(
                                    resultState = latestSuccess.copy(isLoadingNextPage = false),
                                )
                            }
                        }
                        return@launch
                    }

                mutableUiState.update { state ->
                    val latestSuccess = state.resultState as? SearchResultUiState.Success
                    if (latestSuccess == null || latestSuccess.query != currentResultState.query) {
                        state
                    } else {
                        state.copy(
                            resultState =
                                latestSuccess.copy(
                                    results = latestSuccess.results + nextPage.results,
                                    nextCursor = nextPage.nextCursor,
                                    hasNext = nextPage.hasNext,
                                    isLoadingNextPage = false,
                                ),
                        )
                    }
                }
            }
    }

    private fun refreshRecentSearches() {
        viewModelScope.launch {
            val recentSearches =
                try {
                    searchRepository.getRecentSearches()
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    emptyList()
                }

            mutableUiState.update { state ->
                state.copy(recentSearches = recentSearches)
            }
        }
    }

    private fun deleteRecentSearch(keyword: String) {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isEmpty()) return

        viewModelScope.launch {
            val recentSearches =
                try {
                    searchRepository.deleteRecentSearch(normalizedKeyword)
                    searchRepository.getRecentSearches()
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    mutableUiState.value.recentSearches
                }

            mutableUiState.update { state ->
                state.copy(recentSearches = recentSearches)
            }
        }
    }

    private fun clearRecentSearches() {
        viewModelScope.launch {
            val recentSearches =
                try {
                    searchRepository.clearRecentSearches()
                    searchRepository.getRecentSearches()
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    mutableUiState.value.recentSearches
                }

            mutableUiState.update { state ->
                state.copy(recentSearches = recentSearches)
            }
        }
    }

    private fun emitUiEvent(event: SearchUiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }

    private fun showResultActionError(message: String) {
        mutableUiState.update { state ->
            state.copy(
                resultState =
                    SearchResultUiState.Error(
                        query = state.query.trim(),
                        message = message,
                    ),
            )
        }
    }

    private fun blockedResultMessage(result: SearchResult): String =
        if (result.toPlaceDestinationOrNull() == null) {
            INVALID_DESTINATION_HANDOFF_MESSAGE
        } else {
            BOOKMARK_TOGGLE_FAILURE_MESSAGE
        }

    override fun onCleared() {
        cancelPendingVoiceSearchNavigation()
        searchJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val INVALID_DESTINATION_HANDOFF_MESSAGE = "좌표 정보가 올바르지 않아 경로 설정으로 넘길 수 없습니다."
        private const val UNVERIFIED_PLACE_HANDOFF_MESSAGE = "접근성 정보가 확인된 장소만 길찾기를 시작할 수 있습니다."
        private const val BOOKMARK_TOGGLE_FAILURE_MESSAGE = "북마크 상태를 변경하지 못했습니다. 다시 시도해 주세요."

        fun provideFactory(
            searchRepository: SearchRepository,
            bookmarkRepository: BookmarkRepository,
            destinationSelectionRepository: DestinationSelectionRepository,
            destinationPreviewRepository: DestinationPreviewRepository,
            placesRepository: PlacesRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                        return SearchViewModel(
                            searchRepository = searchRepository,
                            bookmarkRepository = bookmarkRepository,
                            destinationSelectionRepository = destinationSelectionRepository,
                            destinationPreviewRepository = destinationPreviewRepository,
                            placesRepository = placesRepository,
                        ) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}

private fun SearchResultUiState.hasResultQuery(query: String): Boolean =
    when (this) {
        is SearchResultUiState.Loading -> this.query == query
        is SearchResultUiState.Success -> this.query == query
        is SearchResultUiState.Empty -> this.query == query
        is SearchResultUiState.Error -> this.query == query
        else -> false
    }

private fun SearchResult.bookmarkProvider(): String? =
    when {
        isAddressSearchFallback() -> "KAKAO"
        !provider.isNullOrBlank() -> provider
        !providerPlaceId.isNullOrBlank() -> "KAKAO"
        else -> null
    }

private fun SearchResult.bookmarkProviderPlaceId(): String? =
    providerPlaceId?.takeIf { !isAddressSearchFallback() && it.isNotBlank() }

private fun SearchResult.isAddressSearchFallback(): Boolean =
    provider?.equals(ANDROID_GEOCODER_PROVIDER, ignoreCase = true) == true
