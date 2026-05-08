package com.ssafy.e102.eumgil.data.remote.mapper

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.core.model.SearchVoiceAnalysis
import com.ssafy.e102.eumgil.core.model.SearchVoiceIntent
import com.ssafy.e102.eumgil.data.remote.dto.PlaceAccessibilityFeatureDto
import com.ssafy.e102.eumgil.data.remote.dto.PlacePointDto
import com.ssafy.e102.eumgil.data.remote.dto.PlacesSearchDto
import com.ssafy.e102.eumgil.data.remote.dto.SearchPlaceDto
import com.ssafy.e102.eumgil.data.remote.dto.VoiceSearchAnalysisDto
import org.json.JSONArray
import org.json.JSONObject

internal object SearchDtoMapper {
    fun parsePlacesSearchDto(body: String): PlacesSearchDto {
        val responseJson = JSONObject(body)
        val dataJson = responseJson.optJSONObject("data") ?: error("places search response missing data object")
        val placesJson = dataJson.optJSONArray("places") ?: JSONArray()

        return PlacesSearchDto(
            places =
                List(placesJson.length()) { index ->
                    placesJson.getJSONObject(index).toSearchPlaceDto()
                },
        )
    }

    fun toSearchResults(dto: PlacesSearchDto): List<SearchResult> =
        dto.places.map { placeDto ->
            val serverPlaceId = placeDto.placeId?.toString()
            val providerPlaceId = placeDto.providerPlaceId?.takeIf { providerPlaceId -> providerPlaceId.isNotBlank() }
            SearchResult(
                placeId =
                    serverPlaceId
                        ?: synthesizeExternalPlaceId(
                            provider = placeDto.provider,
                            providerPlaceId = providerPlaceId,
                            name = placeDto.name,
                            point = placeDto.point,
                        ),
                title = placeDto.name,
                subtitle = placeDto.address.orEmpty(),
                latitude = placeDto.point.lat,
                longitude = placeDto.point.lng,
                category = placeDto.category.toPlaceCategoryOrNull(),
                serverPlaceId = serverPlaceId,
                providerPlaceId = providerPlaceId,
                accessibilityTagKeys = placeDto.accessibilityFeatures.toAccessibilityTagKeys(),
                matched = placeDto.matched,
            )
        }

    fun parseVoiceSearchAnalysisDto(body: String): VoiceSearchAnalysisDto {
        val responseJson = JSONObject(body)
        val dataJson = responseJson.optJSONObject("data") ?: error("voice analysis response missing data object")

        return VoiceSearchAnalysisDto(
            intent = dataJson.optString("intent"),
            placeName = dataJson.optNullableString("placeName"),
            confirmed = dataJson.optNullableBoolean("confirmed"),
            confirmationMessage = dataJson.optNullableString("confirmationMessage"),
        )
    }

    fun toSearchVoiceAnalysis(dto: VoiceSearchAnalysisDto): SearchVoiceAnalysis =
        SearchVoiceAnalysis(
            intent = dto.intent.toSearchVoiceIntent(),
            placeName = dto.placeName?.takeIf { placeName -> placeName.isNotBlank() },
            confirmed = dto.confirmed,
            confirmationMessage = dto.confirmationMessage?.takeIf { message -> message.isNotBlank() },
        )

    private fun JSONObject.toSearchPlaceDto(): SearchPlaceDto {
        val pointJson = optJSONObject("point") ?: error("places search response missing point object")

        return SearchPlaceDto(
            placeId = optNullableLong("placeId"),
            provider = optString("provider"),
            providerPlaceId = optNullableString("providerPlaceId"),
            name = optString("name"),
            category = optNullableString("category"),
            address = optNullableString("address"),
            distanceMeter = optNullableInt("distanceMeter"),
            point =
                PlacePointDto(
                    lat = pointJson.optDouble("lat"),
                    lng = pointJson.optDouble("lng"),
                ),
            accessibilityFeatures =
                optJSONArray("accessibilityFeatures")
                    ?.let(::toAccessibilityFeatureDtos)
                    .orEmpty(),
            matched = optBoolean("matched"),
        )
    }

    private fun toAccessibilityFeatureDtos(featuresJson: JSONArray): List<PlaceAccessibilityFeatureDto> =
        List(featuresJson.length()) { index ->
            featuresJson.getJSONObject(index).let { featureJson ->
                PlaceAccessibilityFeatureDto(
                    featureType = featureJson.optString("featureType"),
                    isAvailable = featureJson.optBoolean("isAvailable"),
                )
            }
        }

    private fun synthesizeExternalPlaceId(
        provider: String,
        providerPlaceId: String?,
        name: String,
        point: PlacePointDto,
    ): String =
        if (!providerPlaceId.isNullOrBlank()) {
            "provider:${provider.trim().lowercase()}:$providerPlaceId"
        } else {
            listOf(
                "external",
                provider.trim().lowercase().ifBlank { "unknown" },
                name.trim(),
                point.lat.toString(),
                point.lng.toString(),
            ).joinToString(separator = ":")
        }

    private fun String?.toPlaceCategoryOrNull(): PlaceCategory? =
        when (this?.trim()?.uppercase()) {
            null,
            "",
            -> null
            "TOILET" -> PlaceCategory.TOILET
            "ELEVATOR" -> PlaceCategory.ELEVATOR
            "CHARGING_STATION" -> PlaceCategory.CHARGING_STATION
            "FOOD_CAFE" -> PlaceCategory.FOOD_CAFE
            "TOURIST_SPOT" -> PlaceCategory.TOURIST_SPOT
            "ACCOMMODATION" -> PlaceCategory.ACCOMMODATION
            "HEALTHCARE" -> PlaceCategory.HEALTHCARE
            "WELFARE" -> PlaceCategory.WELFARE
            "PUBLIC_OFFICE" -> PlaceCategory.PUBLIC_OFFICE
            "BRAILLE_BLOCK" -> PlaceCategory.BRAILLE_BLOCK
            "RESTAURANT" -> PlaceCategory.RESTAURANT
            "TOURIST_ATTRACTION" -> PlaceCategory.TOURIST_ATTRACTION
            "ETC",
            "OTHER",
            -> PlaceCategory.OTHER
            else -> PlaceCategory.OTHER
        }

    private fun List<PlaceAccessibilityFeatureDto>.toAccessibilityTagKeys(): List<String> =
        mapNotNull { feature ->
            if (!feature.isAvailable) {
                null
            } else {
                when (feature.featureType.trim()) {
                    "accessibleEntrance" -> "step-free-entrance"
                    "elevator" -> "elevator"
                    "accessibleToilet" -> "accessible-toilet"
                    "accessibleParking" -> "accessible-parking"
                    "chargingStation" -> "charging-station"
                    "accessibleRoom" -> "accessible-room"
                    "guidanceFacility" -> "guidance-facility"
                    else -> null
                }
            }
        }.distinct()

    private fun String.toSearchVoiceIntent(): SearchVoiceIntent =
        when (trim().uppercase()) {
            "PLACE_SEARCH" -> SearchVoiceIntent.PLACE_SEARCH
            else -> SearchVoiceIntent.UNKNOWN
        }

    private fun JSONObject.optNullableBoolean(name: String): Boolean? =
        if (isNull(name)) {
            null
        } else {
            optBoolean(name)
        }

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (isNull(name)) {
            null
        } else {
            optInt(name)
        }

    private fun JSONObject.optNullableLong(name: String): Long? =
        if (isNull(name)) {
            null
        } else {
            optLong(name)
        }

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name)) {
            null
        } else {
            optString(name).takeIf { value -> value.isNotBlank() }
        }
}
