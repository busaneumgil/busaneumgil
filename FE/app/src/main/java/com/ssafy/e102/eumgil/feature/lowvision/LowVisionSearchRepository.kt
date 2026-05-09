package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.core.model.SearchVoiceAnalysis
import com.ssafy.e102.eumgil.core.model.SearchVoiceMode
import com.ssafy.e102.eumgil.data.mock.datasource.SearchMockDataSource
import com.ssafy.e102.eumgil.data.repository.SearchRepository

internal class LowVisionSearchRepository(
    private val delegate: SearchRepository,
    private val mockDataSource: SearchMockDataSource = SearchMockDataSource(),
) : SearchRepository {
    override suspend fun search(query: SearchQuery): List<SearchResult> = mockDataSource.search(query)

    override suspend fun analyzeVoiceSearch(
        text: String,
        mode: SearchVoiceMode,
    ): SearchVoiceAnalysis = delegate.analyzeVoiceSearch(text = text, mode = mode)

    override suspend fun getRecentSearches(): List<RecentSearch> = delegate.getRecentSearches()

    override suspend fun saveRecentSearch(keyword: String) {
        delegate.saveRecentSearch(keyword)
    }

    override suspend fun getRecentDestinations(): List<RecentDestination> = delegate.getRecentDestinations()

    override suspend fun saveRecentDestination(destination: RecentDestination) {
        delegate.saveRecentDestination(destination)
    }
}
