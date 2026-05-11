package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.location.isFreshCurrentLocation
import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceQuery
import com.ssafy.e102.eumgil.core.model.PlaceSummary
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchPage
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.core.model.SearchVoiceAnalysis
import com.ssafy.e102.eumgil.core.model.SearchVoiceMode
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.SearchRepository

internal class LowVisionSearchRepository(
    private val delegate: SearchRepository,
    private val placesRepository: PlacesRepository? = null,
    private val currentLocationProvider: () -> LocationSnapshot? = { null },
) : SearchRepository {
    override suspend fun search(query: SearchQuery): List<SearchResult> = searchPage(query).results

    override suspend fun searchPage(query: SearchQuery): SearchPage {
        val categories = query.normalizedKeyword.toLowVisionCategoryFilters()
        if (categories != null && query.cursor.isNullOrBlank() && placesRepository != null) {
            val anchor = currentLocationProvider().toLowVisionSearchAnchor()
            val places =
                placesRepository.getPlaces(
                    PlaceQuery(
                        latitude = anchor.latitude,
                        longitude = anchor.longitude,
                        categories = categories,
                    ),
                )

            return SearchPage(results = places.map(PlaceSummary::toSearchResult).filter(SearchResult::isInLowVisionRouteServiceArea))
        }

        val page = delegate.searchPage(query.withLowVisionSearchAnchor())
        val routableResults = page.results.filter(SearchResult::isInLowVisionRouteServiceArea)
        return page.copy(
            results = routableResults,
            size = routableResults.size,
        )
    }

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

    private fun SearchQuery.withLowVisionSearchAnchor(): SearchQuery {
        val currentAnchor = currentLocationProvider().toLowVisionSearchAnchor()
        val hasRoutableAnchor =
            latitude != null &&
                longitude != null &&
                isInLowVisionRouteServiceArea(latitude = latitude, longitude = longitude)
        return if (hasRoutableAnchor) {
            this
        } else {
            copy(latitude = currentAnchor.latitude, longitude = currentAnchor.longitude)
        }
    }
}

private data class CategorySearchAnchor(
    val latitude: Double,
    val longitude: Double,
)

private fun LocationSnapshot?.toLowVisionSearchAnchor(): CategorySearchAnchor =
    if (this != null && isFreshCurrentLocation() && isInLowVisionRouteServiceArea(latitude = latitude, longitude = longitude)) {
        CategorySearchAnchor(latitude = latitude, longitude = longitude)
    } else {
        DEFAULT_CATEGORY_SEARCH_ANCHOR
    }

private fun SearchResult.isInLowVisionRouteServiceArea(): Boolean =
    isInLowVisionRouteServiceArea(latitude = latitude, longitude = longitude)

private fun isInLowVisionRouteServiceArea(
    latitude: Double,
    longitude: Double,
): Boolean =
    latitude in BUSAN_MIN_LATITUDE..BUSAN_MAX_LATITUDE &&
        longitude in BUSAN_MIN_LONGITUDE..BUSAN_MAX_LONGITUDE

private fun String.toLowVisionCategoryFilters(): Set<PlaceCategory>? =
    when (trim()) {
        "음식점" -> setOf(PlaceCategory.FOOD_CAFE, PlaceCategory.RESTAURANT)
        "관광지" -> setOf(PlaceCategory.TOURIST_SPOT, PlaceCategory.TOURIST_ATTRACTION)
        "숙박시설" -> setOf(PlaceCategory.ACCOMMODATION)
        "병원" -> setOf(PlaceCategory.HEALTHCARE)
        "복지관" -> setOf(PlaceCategory.WELFARE)
        "관공서" -> setOf(PlaceCategory.PUBLIC_OFFICE)
        else -> null
    }

private fun PlaceSummary.toSearchResult(): SearchResult =
    SearchResult(
        placeId = placeId,
        title = name,
        subtitle = address,
        latitude = latitude,
        longitude = longitude,
        category = category,
        serverPlaceId = placeId,
        accessibilityTagKeys = accessibilityTags,
    )

private val DEFAULT_CATEGORY_SEARCH_ANCHOR = CategorySearchAnchor(latitude = 35.1796, longitude = 129.0756)
private const val BUSAN_MIN_LATITUDE = 34.85
private const val BUSAN_MAX_LATITUDE = 35.45
private const val BUSAN_MIN_LONGITUDE = 128.70
private const val BUSAN_MAX_LONGITUDE = 129.40
