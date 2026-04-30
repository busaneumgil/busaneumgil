package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LowVisionCategoryRoute(
    onBackClick: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LowVisionCategoryScreen(
        onBackClick = onBackClick,
        onCategorySelected = onCategorySelected,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}
