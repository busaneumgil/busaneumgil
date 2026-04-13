package com.ssafy.e102.eumgil.feature.savedroute

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SavedRouteRoute(
    onNavigateToMap: () -> Unit,
    onNavigateToMyPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SavedRouteScreen(
        onNavigateToMap = onNavigateToMap,
        onNavigateToMyPage = onNavigateToMyPage,
        modifier = modifier,
    )
}
