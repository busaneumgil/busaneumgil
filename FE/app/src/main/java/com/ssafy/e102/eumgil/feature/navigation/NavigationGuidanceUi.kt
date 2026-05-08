package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.R

internal fun NavigationGuidanceAction.iconRes(): Int =
    when (this) {
        NavigationGuidanceAction.STRAIGHT -> R.drawable.ic_direction_straight
        NavigationGuidanceAction.TURN_LEFT -> R.drawable.ic_direction_turn_left
        NavigationGuidanceAction.TURN_RIGHT -> R.drawable.ic_direction_turn_right
        NavigationGuidanceAction.CROSSWALK -> R.drawable.ic_direction_crosswalk
    }
