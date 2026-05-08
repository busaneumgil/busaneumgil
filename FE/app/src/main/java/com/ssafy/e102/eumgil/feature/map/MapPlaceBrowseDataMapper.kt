package com.ssafy.e102.eumgil.feature.map

import com.ssafy.e102.eumgil.core.model.AccessibilityTag
import com.ssafy.e102.eumgil.core.model.FacilityBrowseData
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.FacilityMarkerSeed
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceFeatureAvailability
import com.ssafy.e102.eumgil.core.model.PlaceFeatureType
import com.ssafy.e102.eumgil.core.model.PlaceSummary

internal object MapPlaceBrowseDataMapper {
    fun toBrowseData(places: List<PlaceSummary>): FacilityBrowseData {
        val markerSeeds = places.map { place -> place.toFacilityMarkerSeed() }
        val availableCategories =
            markerSeeds
                .flatMap { marker -> marker.filterCategories }
                .distinct()
        val detailsById =
            places.associate { place ->
                place.placeId to place.toFacilityDetailSeed()
            }

        return FacilityBrowseData(
            facilityMarkers = markerSeeds,
            detailsById = detailsById,
            availableCategories = availableCategories,
        )
    }

    private fun PlaceSummary.toFacilityMarkerSeed(): FacilityMarkerSeed {
        val actualCategory = category.toFacilityCategory()
        val featureCategories = featureFilterCategories()

        return FacilityMarkerSeed(
            facilityId = placeId,
            name = name,
            coordinate = GeoCoordinate(latitude = latitude, longitude = longitude),
            category = featureCategories.firstOrNull() ?: actualCategory,
            filterCategories = (setOf(actualCategory) + featureCategories).distinct().toSet(),
            accessibilityTags = features.toAccessibilityTags(),
        )
    }

    private fun PlaceSummary.toFacilityDetailSeed(): FacilityDetailSeed =
        FacilityDetailSeed(
            facilityId = placeId,
            name = name,
            address = address,
            coordinate = GeoCoordinate(latitude = latitude, longitude = longitude),
            category = category.toFacilityCategory(),
            accessibilityTags = features.toAccessibilityTags(),
        )

    fun toFacilityDetailSeed(detail: PlaceDetail): FacilityDetailSeed =
        FacilityDetailSeed(
            facilityId = detail.placeId,
            name = detail.name,
            address = detail.address,
            coordinate = GeoCoordinate(latitude = detail.latitude, longitude = detail.longitude),
            category = detail.category.toFacilityCategory(),
            accessibilityTags =
                if (detail.features.isNotEmpty()) {
                    detail.features.toAccessibilityTags()
                } else {
                    detail.accessibilityTags.mapNotNull(::rawAccessibilityTagToUiTag)
                },
            description = detail.description,
        )

    private fun PlaceSummary.featureFilterCategories(): List<FacilityCategory> =
        features
            .asSequence()
            .filter { feature -> feature.isAvailable }
            .mapNotNull { feature ->
                when (feature.featureType) {
                    PlaceFeatureType.ACCESSIBLE_TOILET -> FacilityCategory.TOILET
                    PlaceFeatureType.ELEVATOR -> FacilityCategory.ELEVATOR
                    PlaceFeatureType.CHARGING_STATION -> FacilityCategory.CHARGING_STATION
                    PlaceFeatureType.ACCESSIBLE_ENTRANCE,
                    PlaceFeatureType.ACCESSIBLE_PARKING,
                    PlaceFeatureType.ACCESSIBLE_ROOM,
                    PlaceFeatureType.GUIDANCE_FACILITY,
                    -> null
                }
            }.toSet()
            .let { featureCategories ->
                FEATURE_CATEGORY_PRIORITY.filter(featureCategories::contains)
            }

    private fun Iterable<PlaceFeatureAvailability>.toAccessibilityTags(): List<AccessibilityTag> =
        asSequence()
            .filter { feature -> feature.isAvailable }
            .mapNotNull { feature ->
                when (feature.featureType) {
                    PlaceFeatureType.ACCESSIBLE_ENTRANCE -> AccessibilityTag.STEP_FREE_ENTRANCE
                    PlaceFeatureType.ELEVATOR -> AccessibilityTag.ELEVATOR
                    PlaceFeatureType.ACCESSIBLE_TOILET -> AccessibilityTag.ACCESSIBLE_TOILET
                    PlaceFeatureType.ACCESSIBLE_PARKING -> AccessibilityTag.ACCESSIBLE_PARKING
                    PlaceFeatureType.CHARGING_STATION,
                    PlaceFeatureType.ACCESSIBLE_ROOM,
                    PlaceFeatureType.GUIDANCE_FACILITY,
                    -> null
                }
            }.distinct()
            .toList()

    private fun rawAccessibilityTagToUiTag(rawKey: String): AccessibilityTag? =
        when (rawKey.trim().lowercase()) {
            "step-free-entrance",
            "accessible-entrance",
            -> AccessibilityTag.STEP_FREE_ENTRANCE

            "elevator" -> AccessibilityTag.ELEVATOR
            "accessible-toilet" -> AccessibilityTag.ACCESSIBLE_TOILET
            "accessible-parking" -> AccessibilityTag.ACCESSIBLE_PARKING
            else -> null
        }

    private fun PlaceCategory.toFacilityCategory(): FacilityCategory =
        when (this) {
            PlaceCategory.TOILET -> FacilityCategory.TOILET
            PlaceCategory.ELEVATOR -> FacilityCategory.ELEVATOR
            PlaceCategory.CHARGING_STATION -> FacilityCategory.CHARGING_STATION
            PlaceCategory.FOOD_CAFE -> FacilityCategory.FOOD_CAFE
            PlaceCategory.TOURIST_SPOT -> FacilityCategory.TOURIST_SPOT
            PlaceCategory.ACCOMMODATION -> FacilityCategory.ACCOMMODATION
            PlaceCategory.HEALTHCARE -> FacilityCategory.HEALTHCARE
            PlaceCategory.WELFARE -> FacilityCategory.WELFARE
            PlaceCategory.PUBLIC_OFFICE -> FacilityCategory.PUBLIC_OFFICE
            PlaceCategory.BRAILLE_BLOCK -> FacilityCategory.BRAILLE_BLOCK
            PlaceCategory.RESTAURANT -> FacilityCategory.RESTAURANT
            PlaceCategory.TOURIST_ATTRACTION -> FacilityCategory.TOURIST_ATTRACTION
            PlaceCategory.OTHER -> FacilityCategory.OTHER
        }

    private val FEATURE_CATEGORY_PRIORITY =
        listOf(
            FacilityCategory.TOILET,
            FacilityCategory.ELEVATOR,
            FacilityCategory.CHARGING_STATION,
        )
}
