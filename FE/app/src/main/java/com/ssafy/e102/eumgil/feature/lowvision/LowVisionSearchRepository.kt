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
import kotlinx.coroutines.CancellationException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal class LowVisionSearchRepository(
    private val delegate: SearchRepository,
    private val placesRepository: PlacesRepository? = null,
    private val currentLocationProvider: () -> LocationSnapshot? = { null },
) : SearchRepository {
    override suspend fun search(query: SearchQuery): List<SearchResult> = searchPage(query).results

    override suspend fun searchPage(query: SearchQuery): SearchPage {
        val knownCurrentLocation = currentLocationProvider()
        val freshCurrentLocation = knownCurrentLocation?.takeIf(LocationSnapshot::isFreshCurrentLocation)
        val categorySearchSpec = query.normalizedKeyword.toLowVisionCategorySearchSpec()
        val currentLocation =
            if (categorySearchSpec != null) {
                knownCurrentLocation
            } else {
                freshCurrentLocation
            }
        val anchor = currentLocation?.toCategorySearchAnchor()

        if (categorySearchSpec != null) {
            if (anchor == null) {
                throw LowVisionCurrentLocationRequiredException()
            }

            if (query.cursor.isNullOrBlank() && placesRepository != null) {
                val places =
                    placesRepository.getCategoryPlacesOrEmpty(
                        anchor = anchor,
                        categories = categorySearchSpec.categories,
                    )

                if (places.isNotEmpty()) {
                    return SearchPage(
                        results =
                            places.map(PlaceSummary::toSearchResult)
                                .sortedByCurrentLocation(currentLocation),
                    )
                }
            }

            return delegate.searchCategoryPage(
                query = query,
                searchSpec = categorySearchSpec,
                anchor = anchor,
            ).sortedByCurrentLocation(currentLocation)
        }

        return delegate.searchPage(query.withSearchAnchor(anchor)).sortedByCurrentLocation(currentLocation)
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
}

private data class CategorySearchAnchor(
    val latitude: Double,
    val longitude: Double,
)

private fun LocationSnapshot.toCategorySearchAnchor(): CategorySearchAnchor =
    CategorySearchAnchor(latitude = latitude, longitude = longitude)

private data class LowVisionCategorySearchSpec(
    val categories: Set<PlaceCategory>,
    val liveSearchKeywords: List<String>,
)

private fun String.toLowVisionCategorySearchSpec(): LowVisionCategorySearchSpec? =
    when (trim()) {
        "\uC74C\uC2DD\uC810" ->
            LowVisionCategorySearchSpec(
                categories = setOf(PlaceCategory.FOOD_CAFE, PlaceCategory.RESTAURANT),
                liveSearchKeywords = listOf("\uC74C\uC2DD\uC810", "\uC2DD\uB2F9", "\uCE74\uD398"),
            )
        "\uAD00\uAD11\uC9C0" ->
            LowVisionCategorySearchSpec(
                categories = setOf(PlaceCategory.TOURIST_SPOT, PlaceCategory.TOURIST_ATTRACTION),
                liveSearchKeywords = listOf("\uAD00\uAD11\uC9C0", "\uBB34\uC7A5\uC560 \uAD00\uAD11\uC9C0"),
            )
        "\uC219\uBC15\uC2DC\uC124" ->
            LowVisionCategorySearchSpec(
                categories = setOf(PlaceCategory.ACCOMMODATION),
                liveSearchKeywords = listOf("\uC219\uBC15\uC2DC\uC124", "\uC219\uBC15", "\uD638\uD154"),
            )
        "\uBCD1\uC6D0" ->
            LowVisionCategorySearchSpec(
                categories = setOf(PlaceCategory.HEALTHCARE),
                liveSearchKeywords = listOf("\uBCD1\uC6D0", "\uC758\uB8CC\uC2DC\uC124", "\uC758\uC6D0"),
            )
        "\uBCF5\uC9C0\uAD00" ->
            LowVisionCategorySearchSpec(
                categories = setOf(PlaceCategory.WELFARE),
                liveSearchKeywords = listOf("\uBCF5\uC9C0\uAD00", "\uBCF5\uC9C0\uC13C\uD130", "\uC0AC\uD68C\uBCF5\uC9C0\uAD00"),
            )
        "\uAD00\uACF5\uC11C" ->
            LowVisionCategorySearchSpec(
                categories = setOf(PlaceCategory.PUBLIC_OFFICE),
                liveSearchKeywords =
                    listOf(
                        "\uAD00\uACF5\uC11C",
                        "\uC8FC\uBBFC\uC13C\uD130",
                        "\uD589\uC815\uBCF5\uC9C0\uC13C\uD130",
                        "\uAD6C\uCCAD",
                    ),
            )
        else -> null
    }

private suspend fun PlacesRepository.getCategoryPlacesOrEmpty(
    anchor: CategorySearchAnchor,
    categories: Set<PlaceCategory>,
): List<PlaceSummary> =
    try {
        getPlaces(
            PlaceQuery(
                latitude = anchor.latitude,
                longitude = anchor.longitude,
                radiusMeters = LOW_VISION_CATEGORY_SEARCH_RADIUS_METERS,
                categories = categories,
            ),
        )
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        emptyList()
    }

private suspend fun SearchRepository.searchCategoryPage(
    query: SearchQuery,
    searchSpec: LowVisionCategorySearchSpec,
    anchor: CategorySearchAnchor,
): SearchPage {
    if (!query.cursor.isNullOrBlank()) {
        return searchPage(query.withSearchAnchor(anchor))
    }

    var lastPage = SearchPage(results = emptyList())
    searchSpec.liveSearchKeywords.forEach { keyword ->
        val page = searchPage(query.copy(keyword = keyword).withSearchAnchor(anchor))
        lastPage = page
        if (page.results.isNotEmpty()) return page
    }
    return lastPage
}

private fun SearchQuery.withSearchAnchor(anchor: CategorySearchAnchor?): SearchQuery =
    if (anchor == null) {
        this
    } else {
        copy(
            latitude = anchor.latitude,
            longitude = anchor.longitude,
            radiusMeters = radiusMeters ?: LOW_VISION_CATEGORY_SEARCH_RADIUS_METERS,
        )
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

internal const val LOW_VISION_CURRENT_LOCATION_REQUIRED_MESSAGE: String =
    "\uD604\uC7AC \uC704\uCE58\uB97C \uD655\uC778\uD560 \uC218 \uC5C6\uC5B4\uC694. \uC704\uCE58 \uAD8C\uD55C\uACFC \uC704\uCE58 \uC11C\uBE44\uC2A4\uB97C \uD655\uC778\uD55C \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."

internal class LowVisionCurrentLocationRequiredException :
    IllegalStateException(LOW_VISION_CURRENT_LOCATION_REQUIRED_MESSAGE)

private const val LOW_VISION_CATEGORY_SEARCH_RADIUS_METERS = 3_000

private fun SearchPage.sortedByCurrentLocation(currentLocation: LocationSnapshot?): SearchPage =
    copy(results = results.sortedByCurrentLocation(currentLocation))

private fun List<SearchResult>.sortedByCurrentLocation(currentLocation: LocationSnapshot?): List<SearchResult> {
    val current = currentLocation ?: return this
    return sortedBy { result ->
        haversineDistanceMeters(
            startLatitude = current.latitude,
            startLongitude = current.longitude,
            endLatitude = result.latitude,
            endLongitude = result.longitude,
        )
    }
}

private fun haversineDistanceMeters(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double,
): Double {
    val earthRadiusMeters = 6_371_000.0
    val dLat = Math.toRadians(endLatitude - startLatitude)
    val dLng = Math.toRadians(endLongitude - startLongitude)
    val startLatitudeRadians = Math.toRadians(startLatitude)
    val endLatitudeRadians = Math.toRadians(endLatitude)
    val haversine =
        sin(dLat / 2).pow(2) +
            cos(startLatitudeRadians) * cos(endLatitudeRadians) * sin(dLng / 2).pow(2)
    return 2 * earthRadiusMeters * atan2(sqrt(haversine), sqrt(1 - haversine))
}
