package com.ssafy.e102.eumgil.data.local.datasource

import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

class SearchLocalDataSource {
    private val cachedResultsByQuery = ConcurrentHashMap<String, List<SearchResult>>()
    private val recentSearchesByKeyword = LinkedHashMap<String, RecentSearch>()
    private val recentDestinationsByKey = LinkedHashMap<String, RecentDestination>()

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

    suspend fun getRecentDestinations(): List<RecentDestination> =
        synchronized(recentDestinationsByKey) {
            recentDestinationsByKey.values.sortedByDescending(RecentDestination::searchedAtMillis)
        }

    suspend fun saveRecentDestination(destination: RecentDestination) {
        val normalizedKey = destination.normalizedKey()
        if (normalizedKey.isEmpty()) return

        synchronized(recentDestinationsByKey) {
            recentDestinationsByKey.remove(normalizedKey)
            recentDestinationsByKey[normalizedKey] =
                destination.copy(
                    name = destination.name.trim(),
                    address = destination.address?.trim()?.takeIf(String::isNotEmpty),
                    accessibilityTagKeys = destination.accessibilityTagKeys.filter(String::isNotBlank).distinct(),
                )

            while (recentDestinationsByKey.size > MAX_RECENT_DESTINATIONS) {
                val oldestKey =
                    recentDestinationsByKey
                        .entries
                        .minByOrNull { entry -> entry.value.searchedAtMillis }
                        ?.key
                        ?: break
                recentDestinationsByKey.remove(oldestKey)
            }
        }
    }

    private fun SearchQuery.normalizedKey(): String =
        buildList {
            add(keyword.normalizedKeyword())
            add("size=$limit")
            latitude?.let { latitude -> add("lat=$latitude") }
            longitude?.let { longitude -> add("lng=$longitude") }
            radiusMeters?.let { radiusMeters -> add("radius=$radiusMeters") }
            cursor?.trim()?.takeIf(String::isNotEmpty)?.let { cursor -> add("cursor=$cursor") }
        }.joinToString(separator = "|")

    private fun String.normalizedKeyword(): String = trim().lowercase()

    private fun RecentDestination.normalizedKey(): String =
        placeId.trim().ifBlank {
            listOf(
                name.trim(),
                address.orEmpty().trim(),
                latitude.toString(),
                longitude.toString(),
            ).joinToString(separator = "|").lowercase()
        }

    companion object {
        private const val MAX_RECENT_SEARCHES: Int = 10
        private const val MAX_RECENT_DESTINATIONS: Int = 10
    }
}
