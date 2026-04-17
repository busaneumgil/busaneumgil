package com.ssafy.e102.eumgil.core.model

data class SearchQuery(
    val keyword: String,
    val limit: Int = DEFAULT_LIMIT,
) {
    val normalizedKeyword: String
        get() = keyword.trim()

    companion object {
        const val DEFAULT_LIMIT: Int = 20
    }
}

data class SearchResult(
    val placeId: String,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory? = null,
)

data class RecentSearch(
    val keyword: String,
    val searchedAtMillis: Long = System.currentTimeMillis(),
)
