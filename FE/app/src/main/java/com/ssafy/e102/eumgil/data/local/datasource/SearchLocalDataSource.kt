package com.ssafy.e102.eumgil.data.local.datasource

import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

class SearchLocalDataSource {
    private val cachedResultsByQuery = ConcurrentHashMap<String, List<SearchResult>>()
    private val recentSearchesByKeyword = LinkedHashMap<String, RecentSearch>()

    suspend fun getCachedResults(query: SearchQuery): List<SearchResult> =
        cachedResultsByQuery[query.normalizedKey()].orEmpty()

    suspend fun updateCachedResults(
        query: SearchQuery,
        results: List<SearchResult>,
    ) {
        cachedResultsByQuery[query.normalizedKey()] = results
    }

    suspend fun getRecentSearches(): List<RecentSearch> =
        synchronized(recentSearchesByKeyword) {
            recentSearchesByKeyword.values.sortedByDescending(RecentSearch::searchedAtMillis)
        }

    suspend fun saveRecentSearch(keyword: String) {
        val normalizedKeyword = keyword.normalizedKeyword()
        if (normalizedKeyword.isEmpty()) return

        synchronized(recentSearchesByKeyword) {
            recentSearchesByKeyword.remove(normalizedKeyword)
            recentSearchesByKeyword[normalizedKeyword] = RecentSearch(keyword = keyword.trim())

            while (recentSearchesByKeyword.size > MAX_RECENT_SEARCHES) {
                val oldestKey =
                    recentSearchesByKeyword
                        .entries
                        .minByOrNull { entry -> entry.value.searchedAtMillis }
                        ?.key
                        ?: break
                recentSearchesByKeyword.remove(oldestKey)
            }
        }
    }

    private fun SearchQuery.normalizedKey(): String = keyword.normalizedKeyword()

    private fun String.normalizedKeyword(): String = trim().lowercase()

    companion object {
        private const val MAX_RECENT_SEARCHES: Int = 10
    }
}
