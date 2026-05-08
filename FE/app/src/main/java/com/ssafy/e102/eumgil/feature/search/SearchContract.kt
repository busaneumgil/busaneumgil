package com.ssafy.e102.eumgil.feature.search

import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget

data class SearchUiState(
    val query: String = "",
    val hasEditedQuery: Boolean = false,
    val recentSearches: List<RecentSearch> = emptyList(),
    val resultState: SearchResultUiState = SearchResultUiState.Initial,
    val voiceInputState: SearchVoiceInputUiState = SearchVoiceInputUiState(),
)

data class SearchVoiceInputUiState(
    val isActive: Boolean = false,
    val transcript: String = "",
    val status: SearchVoiceInputStatus = SearchVoiceInputStatus.Idle,
)

enum class SearchVoiceInputStatus {
    Idle,
    Listening,
}

sealed interface SearchUiAction {
    data object BackClicked : SearchUiAction

    data class EditingTargetConfigured(
        val editingTarget: RouteEditingTarget,
    ) : SearchUiAction

    data object VoiceInputClicked : SearchUiAction

    data object VoiceRouteEntered : SearchUiAction

    data object VoiceCaptureButtonClicked : SearchUiAction

    data object VoiceInputDismissed : SearchUiAction

    data class VoiceTranscriptReceived(
        val transcript: String,
    ) : SearchUiAction

    data class ResultsRouteEntered(
        val query: String,
    ) : SearchUiAction

    data class QueryChanged(
        val query: String,
    ) : SearchUiAction

    data object ClearQueryClicked : SearchUiAction

    data object SearchSubmitted : SearchUiAction

    data class RecentSearchClicked(
        val keyword: String,
    ) : SearchUiAction

    data class SearchResultClicked(
        val result: SearchResult,
    ) : SearchUiAction

    data class SearchResultBriefingClicked(
        val result: SearchResult,
    ) : SearchUiAction

    data class BookmarkToggleClicked(
        val result: SearchResult,
    ) : SearchUiAction

    data class LowVisionBookmarkSaveClicked(
        val result: SearchResult,
    ) : SearchUiAction
}

sealed interface SearchUiEvent {
    data object NavigateBack : SearchUiEvent

    data object NavigateToVoiceInput : SearchUiEvent

    data class NavigateToResults(
        val query: String,
        val editingTarget: RouteEditingTarget,
    ) : SearchUiEvent

    data object StartVoiceCapture : SearchUiEvent

    data object StopVoiceCapture : SearchUiEvent

    data object NavigateToRouteSetting : SearchUiEvent

    data object NavigateToRouteBriefing : SearchUiEvent

    data object NavigateToLowVisionBookmark : SearchUiEvent
}

sealed interface SearchResultUiState {
    data object Initial : SearchResultUiState

    data object EmptyQuery : SearchResultUiState

    data class Typing(
        val query: String,
    ) : SearchResultUiState

    data class Loading(
        val query: String,
    ) : SearchResultUiState

    data class Success(
        val query: String,
        val results: List<SearchResult>,
    ) : SearchResultUiState

    data class Empty(
        val query: String,
    ) : SearchResultUiState

    data class Error(
        val query: String,
        val message: String? = null,
    ) : SearchResultUiState
}
