package com.ssafy.e102.eumgil.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<SearchUiEvent>()
    val uiEvent: SharedFlow<SearchUiEvent> = mutableUiEvent.asSharedFlow()

    private var searchJob: Job? = null

    init {
        refreshRecentSearches()
    }

    fun onAction(action: SearchUiAction) {
        when (action) {
            SearchUiAction.BackClicked -> emitUiEvent(SearchUiEvent.NavigateBack)
            SearchUiAction.ClearQueryClicked -> clearQuery()
            SearchUiAction.SearchSubmitted -> submitSearch()
            is SearchUiAction.QueryChanged -> updateQuery(action.query)
            is SearchUiAction.RecentSearchClicked -> submitSearch(keyword = action.keyword)
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

    private fun submitSearch(keyword: String? = null) {
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
        searchJob =
            viewModelScope.launch {
                mutableUiState.update { state ->
                    state.copy(
                        query = normalizedQuery,
                        hasEditedQuery = true,
                        resultState = SearchResultUiState.Loading(query = normalizedQuery),
                    )
                }

                val results =
                    try {
                        searchRepository.search(SearchQuery(keyword = normalizedQuery))
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
                                )
                            },
                    )
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

    private fun emitUiEvent(event: SearchUiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }

    companion object {
        fun provideFactory(searchRepository: SearchRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                        return SearchViewModel(searchRepository = searchRepository) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
