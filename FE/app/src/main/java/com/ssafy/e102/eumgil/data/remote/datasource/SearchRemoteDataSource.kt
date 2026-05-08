package com.ssafy.e102.eumgil.data.remote.datasource

import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult

class SearchRemoteDataSource(
    private val baseUrl: String,
) {
    suspend fun search(query: SearchQuery): List<SearchResult> {
        error("TODO: connect live search API using $baseUrl for query=$query")
    }
}
