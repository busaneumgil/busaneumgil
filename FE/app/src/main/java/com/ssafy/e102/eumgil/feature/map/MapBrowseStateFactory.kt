package com.ssafy.e102.eumgil.feature.map

import com.ssafy.e102.eumgil.core.model.FacilityBrowseData
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilityMarkerSeed
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapFilterSelectionState
import com.ssafy.e102.eumgil.feature.map.model.MapBrailleBlockFilterOption
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerCategoryType
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerDisplayState
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerFilterUiState
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerLoadStatus
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerOverlayState
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerUiModel
import com.ssafy.e102.eumgil.feature.map.model.MapCategoryFilterOption

internal object MapBrowseStateFactory {
    fun createMarkerOverlayState(
        browseData: FacilityBrowseData,
        selection: MapFilterSelectionState,
    ): MapMarkerOverlayState {
        val normalizedSelection = normalizeSelection(selection = selection, browseData = browseData)
        val markers =
            browseData.allMarkers.map { marker ->
                marker.toMapMarkerUiModel(
                    displayState =
                        if (marker.matches(normalizedSelection)) {
                            MapMarkerDisplayState.VISIBLE
                        } else {
                            MapMarkerDisplayState.HIDDEN_BY_FILTER
                        },
                )
            }

        return MapMarkerOverlayState(
            loadStatus = MapMarkerLoadStatus.READY,
            markers = markers,
            visibleMarkerCount = markers.count { marker -> marker.displayState == MapMarkerDisplayState.VISIBLE },
            totalMarkerCount = markers.size,
        )
    }

    fun createFilterUiState(
        browseData: FacilityBrowseData,
        selection: MapFilterSelectionState,
        overlayState: MapMarkerOverlayState,
    ): MapMarkerFilterUiState {
        val normalizedSelection = normalizeSelection(selection = selection, browseData = browseData)
        val allMarkers = browseData.allMarkers
        val visibleMarkers = overlayState.markers

        return MapMarkerFilterUiState(
            loadStatus = MapMarkerLoadStatus.READY,
            selection = normalizedSelection,
            categoryOptions =
                browseData.availableCategories.map { category ->
                    MapCategoryFilterOption(
                        category = category,
                        totalMarkerCount =
                            allMarkers.count { marker -> marker.category == category },
                        visibleMarkerCount =
                            visibleMarkers.count { marker ->
                                marker.displayState == MapMarkerDisplayState.VISIBLE &&
                                    marker.categoryType.category == category
                            },
                        isSelected = normalizedSelection.isCategorySelected(category),
                    )
                },
            brailleBlockTypeOptions =
                browseData.availableBrailleBlockTypes.map { brailleBlockType ->
                    MapBrailleBlockFilterOption(
                        brailleBlockType = brailleBlockType,
                        totalMarkerCount =
                            browseData.brailleBlockMarkers.count { marker ->
                                marker.brailleBlockType == brailleBlockType
                            },
                        visibleMarkerCount =
                            visibleMarkers.count { marker ->
                                marker.displayState == MapMarkerDisplayState.VISIBLE &&
                                    marker.categoryType.brailleBlockType == brailleBlockType
                            },
                        isSelected =
                            normalizedSelection.isBrailleBlockTypeSelected(brailleBlockType),
                    )
                },
            visibleMarkerCount = overlayState.visibleMarkerCount,
            totalMarkerCount = overlayState.totalMarkerCount,
        )
    }

    fun createErrorMarkerOverlayState(): MapMarkerOverlayState =
        MapMarkerOverlayState(
            loadStatus = MapMarkerLoadStatus.ERROR,
        )

    fun createErrorFilterUiState(): MapMarkerFilterUiState =
        MapMarkerFilterUiState(
            loadStatus = MapMarkerLoadStatus.ERROR,
        )

    fun toggleCategory(
        selection: MapFilterSelectionState,
        browseData: FacilityBrowseData,
        category: FacilityCategory,
    ): MapFilterSelectionState {
        val availableCategories = browseData.availableCategories.toSet()
        if (category !in availableCategories) return normalizeSelection(selection = selection, browseData = browseData)

        val nextSelection =
            if (selection.isShowingAllCategories) {
                MapFilterSelectionState(
                    isShowingAllCategories = false,
                    selectedFacilityCategories = setOf(category),
                    selectedBrailleBlockTypes = emptySet(),
                )
            } else {
                if (category in selection.selectedFacilityCategories) {
                    val updatedCategories = selection.selectedFacilityCategories - category
                    if (updatedCategories.isEmpty()) {
                        MapFilterSelectionState()
                    } else {
                        val selectedBrailleBlockTypes =
                            if (FacilityCategory.BRAILLE_BLOCK in updatedCategories) {
                                selection.selectedBrailleBlockTypes
                            } else {
                                emptySet()
                            }

                        MapFilterSelectionState(
                            isShowingAllCategories = false,
                            selectedFacilityCategories = updatedCategories,
                            selectedBrailleBlockTypes = selectedBrailleBlockTypes,
                        )
                    }
                } else {
                    MapFilterSelectionState(
                        isShowingAllCategories = false,
                        selectedFacilityCategories = selection.selectedFacilityCategories + category,
                        selectedBrailleBlockTypes = selection.selectedBrailleBlockTypes,
                    )
                }
            }

        return normalizeSelection(selection = nextSelection, browseData = browseData)
    }

    fun resetSelection(): MapFilterSelectionState = MapFilterSelectionState()

    private fun normalizeSelection(
        selection: MapFilterSelectionState,
        browseData: FacilityBrowseData,
    ): MapFilterSelectionState {
        if (selection.isShowingAllCategories) {
            return MapFilterSelectionState()
        }

        val availableCategories = browseData.availableCategories.toSet()
        val availableBrailleBlockTypes = browseData.availableBrailleBlockTypes.toSet()
        val normalizedCategories = selection.selectedFacilityCategories intersect availableCategories
        if (normalizedCategories.isEmpty()) {
            return MapFilterSelectionState()
        }

        val normalizedBrailleBlockTypes =
            if (FacilityCategory.BRAILLE_BLOCK in normalizedCategories) {
                selection.selectedBrailleBlockTypes intersect availableBrailleBlockTypes
            } else {
                emptySet()
            }

        val hasAllCategoriesSelected = normalizedCategories == availableCategories
        val isBrailleFilterReset =
            normalizedBrailleBlockTypes.isEmpty() ||
                normalizedBrailleBlockTypes == availableBrailleBlockTypes

        return if (availableCategories.isNotEmpty() && hasAllCategoriesSelected && isBrailleFilterReset) {
            MapFilterSelectionState()
        } else {
            MapFilterSelectionState(
                isShowingAllCategories = false,
                selectedFacilityCategories = normalizedCategories,
                selectedBrailleBlockTypes = normalizedBrailleBlockTypes,
            )
        }
    }

    private fun FacilityMarkerSeed.matches(selection: MapFilterSelectionState): Boolean {
        if (selection.isShowingAllCategories) return true
        if (category !in selection.selectedFacilityCategories) return false
        if (category != FacilityCategory.BRAILLE_BLOCK) return true
        return selection.selectedBrailleBlockTypes.isEmpty() || brailleBlockType in selection.selectedBrailleBlockTypes
    }

    private fun FacilityMarkerSeed.toMapMarkerUiModel(displayState: MapMarkerDisplayState): MapMarkerUiModel =
        MapMarkerUiModel(
            markerId = facilityId,
            name = name,
            coordinate =
                MapCoordinate(
                    latitude = coordinate.latitude,
                    longitude = coordinate.longitude,
                ),
            categoryType =
                MapMarkerCategoryType(
                    category = category,
                    brailleBlockType = brailleBlockType,
                ),
            displayState = displayState,
        )
}
