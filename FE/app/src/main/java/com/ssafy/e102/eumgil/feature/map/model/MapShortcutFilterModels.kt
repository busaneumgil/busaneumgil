package com.ssafy.e102.eumgil.feature.map.model

enum class MapShortcutFilterKey {
    TOILET,
    ELEVATOR,
    ACCESSIBLE_PARKING,
    MORE,
    CHARGING_STATION,
    BRAILLE_BLOCK,
    TOURIST_ATTRACTION,
    RESTAURANT,
}

data class MapShortcutFilterChipState(
    val key: MapShortcutFilterKey,
    val isSelected: Boolean = false,
    val isEnabled: Boolean = true,
)

data class MapShortcutFilterRowState(
    val chips: List<MapShortcutFilterChipState> = emptyList(),
)
