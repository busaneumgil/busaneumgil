package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.core.model.SearchVoiceAnalysis
import com.ssafy.e102.eumgil.core.model.SearchVoiceIntent
import com.ssafy.e102.eumgil.core.model.SearchVoiceMode
import com.ssafy.e102.eumgil.data.local.datasource.SearchLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.SearchMockDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.SearchRemoteDataSource
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryDomain
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySource
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySourcePolicy

interface SearchRepository {
    suspend fun search(query: SearchQuery): List<SearchResult>

    suspend fun analyzeVoiceSearch(
        text: String,
        mode: SearchVoiceMode = SearchVoiceMode.MOBILITY_IMPAIRED,
    ): SearchVoiceAnalysis {
        val normalizedText = text.trim()
        return if (normalizedText.isEmpty()) {
            SearchVoiceAnalysis(intent = SearchVoiceIntent.UNKNOWN)
        } else {
            SearchVoiceAnalysis(
                intent = SearchVoiceIntent.PLACE_SEARCH,
                placeName = normalizedText,
            )
        }
    }

    suspend fun getRecentSearches(): List<RecentSearch>

    suspend fun saveRecentSearch(keyword: String)

    suspend fun deleteRecentSearch(keyword: String) = Unit

    suspend fun clearRecentSearches() = Unit

    suspend fun getRecentDestinations(): List<RecentDestination>

    suspend fun saveRecentDestination(destination: RecentDestination)
}

class DefaultSearchRepository(
    private val remoteDataSource: SearchRemoteDataSource,
    private val localDataSource: SearchLocalDataSource,
    private val mockDataSource: SearchMockDataSource,
    private val sourcePolicy: RepositorySourcePolicy,
) : SearchRepository {
    override suspend fun search(query: SearchQuery): List<SearchResult> {
        val readPlan = sourcePolicy.readPlan(RepositoryDomain.SEARCH)
        val lastSource = readPlan.sources.last()
        var remoteFailure: Throwable? = null

        for (source in readPlan.sources) {
            when (source) {
                RepositorySource.REMOTE -> {
                    val remoteResult = runCatching { remoteDataSource.search(query) }
                    if (remoteResult.isSuccess) {
                        val searchResults = remoteResult.getOrDefault(emptyList())
                        localDataSource.updateCachedResults(query = query, results = searchResults)
                        return searchResults
                    }
                    remoteFailure = remoteResult.exceptionOrNull()
                }

                RepositorySource.LOCAL -> {
                    val cachedResults = localDataSource.getCachedResults(query)
                    if (cachedResults.isNotEmpty()) {
                        return cachedResults
                    }
                    if (source == lastSource && remoteFailure == null) {
                        return cachedResults
                    }
                }

                RepositorySource.MOCK -> return mockDataSource.search(query)
            }
        }

        throw remoteFailure ?: IllegalStateException("No search data source matched the current policy.")
    }

    override suspend fun analyzeVoiceSearch(
        text: String,
        mode: SearchVoiceMode,
    ): SearchVoiceAnalysis {
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) {
            return super<SearchRepository>.analyzeVoiceSearch(text = normalizedText, mode = mode)
        }

        val readPlan = sourcePolicy.readPlan(RepositoryDomain.SEARCH)
        return if (RepositorySource.REMOTE in readPlan.sources) {
            remoteDataSource.analyzeVoiceSearch(
                text = normalizedText,
                mode = mode,
            )
        } else {
            super<SearchRepository>.analyzeVoiceSearch(text = normalizedText, mode = mode)
        }
    }

    override suspend fun getRecentSearches(): List<RecentSearch> = localDataSource.getRecentSearches()

    override suspend fun saveRecentSearch(keyword: String) {
        localDataSource.saveRecentSearch(keyword)
    }

    override suspend fun deleteRecentSearch(keyword: String) {
        localDataSource.deleteRecentSearch(keyword)
    }

    override suspend fun clearRecentSearches() {
        localDataSource.clearRecentSearches()
    }

    override suspend fun getRecentDestinations(): List<RecentDestination> = localDataSource.getRecentDestinations()

    override suspend fun saveRecentDestination(destination: RecentDestination) {
        localDataSource.saveRecentDestination(destination)
    }
}
