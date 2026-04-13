package com.ssafy.e102.eumgil.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MapRoute(
    onNavigateToSavedRoutes: () -> Unit,
    onNavigateToMyPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapScreen(
        onNavigateToSavedRoutes = onNavigateToSavedRoutes,
        onNavigateToMyPage = onNavigateToMyPage,
        modifier = modifier,
    )
}
