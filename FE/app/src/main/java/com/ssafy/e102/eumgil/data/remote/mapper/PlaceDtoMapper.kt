package com.ssafy.e102.eumgil.data.remote.mapper

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceFeatureAvailability
import com.ssafy.e102.eumgil.core.model.PlaceFeatureType
import com.ssafy.e102.eumgil.core.model.PlaceSummary
import com.ssafy.e102.eumgil.data.remote.dto.PlaceAccessibilityFeatureDto
import com.ssafy.e102.eumgil.data.remote.dto.PlaceDetailDto
import com.ssafy.e102.eumgil.data.remote.dto.PlacePointDto
import com.ssafy.e102.eumgil.data.remote.dto.PlaceSummaryDto
import com.ssafy.e102.eumgil.data.remote.dto.PlacesBrowseDto
import org.json.JSONArray
import org.json.JSONObject

internal object PlaceDtoMapper {
    fun parsePlacesBrowseDto(body: String): PlacesBrowseDto {
        val responseJson = JSONObject(body)
        val dataJson = responseJson.optJSONObject("data") ?: error("places response missing data object")
        val placesJson = dataJson.optJSONArray("places") ?: JSONArray()

        return PlacesBrowseDto(
            places =
                List(placesJson.length()) { index ->
                    placesJson.getJSONObject(index).toPlaceSummaryDto()
                },
        )
    }

    fun toPlaceSummaries(dto: PlacesBrowseDto): List<PlaceSummary> =
        dto.places.map { placeDto ->
            val features =
                placeDto.accessibilityFeatures.mapNotNull { featureDto ->
                    featureDto.toPlaceFeatureAvailabilityOrNull()
                }
            PlaceSummary(
                placeId = placeDto.placeId.toString(),
                name = placeDto.name,
                address = placeDto.address.orEmpty(),
                latitude = placeDto.point.lat,
                longitude = placeDto.point.lng,
                category = placeDto.category.toPlaceCategory(),
                features = features,
                isBookmarked = placeDto.isBookmarked,
                accessibilityTags = features.toAccessibilityTagKeys(),
            )
        }

    fun parsePlaceDetailDto(body: String): PlaceDetailDto {
        val responseJson = JSONObject(body)
        val dataJson = responseJson.optJSONObject("data") ?: error("place detail response missing data object")
        return dataJson.toPlaceDetailDto()
    }

    fun toPlaceDetail(dto: PlaceDetailDto): PlaceDetail {
        val features =
            dto.accessibilityFeatures.mapNotNull { featureDto ->
                featureDto.toPlaceFeatureAvailabilityOrNull()
            }
        return PlaceDetail(
            placeId = dto.placeId.toString(),
            name = dto.name,
            address = dto.address.orEmpty(),
            latitude = dto.point.lat,
            longitude = dto.point.lng,
            category = dto.category.toPlaceCategory(),
            features = features,
            isBookmarked = dto.isBookmarked,
            accessibilityTags = features.toAccessibilityTagKeys(),
            providerPlaceId = dto.providerPlaceId?.takeIf { providerPlaceId -> providerPlaceId.isNotBlank() },
            description = dto.description?.takeIf { description -> description.isNotBlank() },
        )
    }

    fun PlaceCategory.toApiValue(): String =
        when (this) {
            PlaceCategory.OTHER -> "ETC"
            else -> name
        }

    fun PlaceFeatureType.toApiValue(): String =
        when (this) {
            PlaceFeatureType.ACCESSIBLE_ENTRANCE -> "accessibleEntrance"
            PlaceFeatureType.ELEVATOR -> "elevator"
            PlaceFeatureType.ACCESSIBLE_TOILET -> "accessibleToilet"
            PlaceFeatureType.ACCESSIBLE_PARKING -> "accessibleParking"
            PlaceFeatureType.CHARGING_STATION -> "chargingStation"
            PlaceFeatureType.ACCESSIBLE_ROOM -> "accessibleRoom"
            PlaceFeatureType.GUIDANCE_FACILITY -> "guidanceFacility"
        }

    private fun JSONObject.toPlaceSummaryDto(): PlaceSummaryDto {
        val pointJson = optJSONObject("point") ?: error("place response missing point object")

        return PlaceSummaryDto(
            placeId = optLong("placeId"),
            name = optString("name"),
            category = optString("category"),
            address = optNullableString("address"),
            point =
                PlacePointDto(
                    lat = pointJson.optDouble("lat"),
                    lng = pointJson.optDouble("lng"),
                ),
            accessibilityFeatures =
                optJSONArray("accessibilityFeatures")
                    ?.let(::toAccessibilityFeatureDtos)
                    .orEmpty(),
            isBookmarked = optBoolean("isBookmarked"),
        )
    }

    private fun JSONObject.toPlaceDetailDto(): PlaceDetailDto {
        val pointJson = optJSONObject("point") ?: error("place detail response missing point object")

        return PlaceDetailDto(
            placeId = optLong("placeId"),
            name = optString("name"),
            category = optString("category"),
            address = optNullableString("address"),
            point =
                PlacePointDto(
                    lat = pointJson.optDouble("lat"),
                    lng = pointJson.optDouble("lng"),
                ),
            providerPlaceId = optNullableString("providerPlaceId"),
            accessibilityFeatures =
                optJSONArray("accessibilityFeatures")
                    ?.let(::toAccessibilityFeatureDtos)
                    .orEmpty(),
            isBookmarked = optBoolean("isBookmarked"),
            description = optNullableString("description"),
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

    private fun String.toPlaceCategory(): PlaceCategory =
        when (trim().uppercase()) {
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

    private fun PlaceAccessibilityFeatureDto.toPlaceFeatureAvailabilityOrNull(): PlaceFeatureAvailability? {
        val featureType =
            when (featureType.trim()) {
                "accessibleEntrance" -> PlaceFeatureType.ACCESSIBLE_ENTRANCE
                "elevator" -> PlaceFeatureType.ELEVATOR
                "accessibleToilet" -> PlaceFeatureType.ACCESSIBLE_TOILET
                "accessibleParking" -> PlaceFeatureType.ACCESSIBLE_PARKING
                "chargingStation" -> PlaceFeatureType.CHARGING_STATION
                "accessibleRoom" -> PlaceFeatureType.ACCESSIBLE_ROOM
                "guidanceFacility" -> PlaceFeatureType.GUIDANCE_FACILITY
                else -> return null
            }

        return PlaceFeatureAvailability(
            featureType = featureType,
            isAvailable = isAvailable,
        )
    }

    private fun List<PlaceFeatureAvailability>.toAccessibilityTagKeys(): List<String> =
        mapNotNull { feature ->
            if (!feature.isAvailable) {
                null
            } else {
                when (feature.featureType) {
                    PlaceFeatureType.ACCESSIBLE_ENTRANCE -> "step-free-entrance"
                    PlaceFeatureType.ELEVATOR -> "elevator"
                    PlaceFeatureType.ACCESSIBLE_TOILET -> "accessible-toilet"
                    PlaceFeatureType.ACCESSIBLE_PARKING -> "accessible-parking"
                    PlaceFeatureType.CHARGING_STATION -> "charging-station"
                    PlaceFeatureType.ACCESSIBLE_ROOM -> "accessible-room"
                    PlaceFeatureType.GUIDANCE_FACILITY -> "guidance-facility"
                }
            }
        }.distinct()

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name)) {
            null
        } else {
            optString(name).takeIf { value -> value.isNotBlank() }
        }
}
