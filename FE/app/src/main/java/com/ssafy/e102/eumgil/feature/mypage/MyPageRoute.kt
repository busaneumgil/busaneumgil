package com.ssafy.e102.eumgil.feature.mypage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MyPageRoute(
    onNavigateToMap: () -> Unit,
    onNavigateToSavedRoutes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MyPageScreen(
        onNavigateToMap = onNavigateToMap,
        onNavigateToSavedRoutes = onNavigateToSavedRoutes,
        modifier = modifier,
    )
}
