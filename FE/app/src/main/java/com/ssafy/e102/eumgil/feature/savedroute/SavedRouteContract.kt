package com.ssafy.e102.eumgil.feature.savedroute

data class SavedRouteUiState(
    val screenState: SavedRouteScreenState = SavedRouteScreenState.LOADING,
    val selectedTab: BookmarkTab = BookmarkTab.PLACE,
    val isEditMode: Boolean = false,
    val places: List<SavedPlaceUiModel> = emptyList(),
    val errorMessage: String? = null,
) {
    val canEnterEditMode: Boolean
        get() = selectedTab == BookmarkTab.PLACE && places.isNotEmpty()
}

data class SavedPlaceUiModel(
    val placeId: String,
    val name: String,
    val address: String?,
    val category: String?,
    val latitude: Double,
    val longitude: Double,
)

enum class SavedRouteScreenState {
    LOADING,
    CONTENT,
    EMPTY,
    ERROR,
}

enum class BookmarkTab {
    PLACE,
    ROUTE,
}

sealed interface SavedRouteUiAction {
    data object ExploreMapClicked : SavedRouteUiAction

    data object RetryClicked : SavedRouteUiAction

    data class TabSelected(
        val tab: BookmarkTab,
    ) : SavedRouteUiAction

    data object EditModeToggled : SavedRouteUiAction

    data class PlaceClicked(
        val placeId: String,
    ) : SavedRouteUiAction

    data class RouteGuideClicked(
        val placeId: String,
    ) : SavedRouteUiAction

    data class BookmarkRemoveClicked(
        val placeId: String,
    ) : SavedRouteUiAction
}

sealed interface SavedRouteUiEvent {
    data object NavigateToMap : SavedRouteUiEvent

    data object NavigateToRouteSetting : SavedRouteUiEvent

    data class ShowSnackbar(
        val message: String,
    ) : SavedRouteUiEvent
}
