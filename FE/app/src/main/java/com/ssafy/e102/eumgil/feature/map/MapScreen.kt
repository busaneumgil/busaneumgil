package com.ssafy.e102.eumgil.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.common.model.PlaceholderAction
import com.ssafy.e102.eumgil.core.designsystem.component.layout.EumPlaceholderScaffold

@Composable
fun MapScreen(
    onNavigateToSavedRoutes: () -> Unit,
    onNavigateToMyPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EumPlaceholderScaffold(
        title = stringResource(id = R.string.map_screen_title),
        description = stringResource(id = R.string.map_screen_description),
        featurePath = stringResource(id = R.string.feature_path_map),
        actions = listOf(
            PlaceholderAction(
                label = stringResource(id = R.string.action_go_saved_routes),
                onClick = onNavigateToSavedRoutes,
                isPrimary = true,
            ),
            PlaceholderAction(
                label = stringResource(id = R.string.action_go_my_page),
                onClick = onNavigateToMyPage,
            ),
        ),
        modifier = modifier,
    )
}
