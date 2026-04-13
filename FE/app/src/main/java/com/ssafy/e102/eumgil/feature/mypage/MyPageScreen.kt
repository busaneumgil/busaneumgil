package com.ssafy.e102.eumgil.feature.mypage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.common.model.PlaceholderAction
import com.ssafy.e102.eumgil.core.designsystem.component.layout.EumPlaceholderScaffold

@Composable
fun MyPageScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToSavedRoutes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EumPlaceholderScaffold(
        title = stringResource(id = R.string.my_page_screen_title),
        description = stringResource(id = R.string.my_page_screen_description),
        featurePath = stringResource(id = R.string.feature_path_my_page),
        actions = listOf(
            PlaceholderAction(
                label = stringResource(id = R.string.action_go_map),
                onClick = onNavigateToMap,
                isPrimary = true,
            ),
            PlaceholderAction(
                label = stringResource(id = R.string.action_go_saved_routes),
                onClick = onNavigateToSavedRoutes,
            ),
        ),
        modifier = modifier,
    )
}
