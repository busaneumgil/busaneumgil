package com.ssafy.e102.eumgil.feature.search

import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchResult

data class SearchUiState(
    val query: String = "",
    val hasEditedQuery: Boolean = false,
    val recentSearches: List<RecentSearch> = emptyList(),
    val resultState: SearchResultUiState = SearchResultUiState.Initial,
)

sealed interface SearchUiAction {
    data object BackClicked : SearchUiAction

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
}

sealed interface SearchUiEvent {
    data object NavigateBack : SearchUiEvent

    data class NavigateToResults(
        val query: String,
    ) : SearchUiEvent

    data object NavigateToRouteSetting : SearchUiEvent
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
