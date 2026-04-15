package com.ssafy.e102.eumgil.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.map.component.MapIntegrationState
import com.ssafy.e102.eumgil.feature.map.component.MapShellScaffold
import com.ssafy.e102.eumgil.feature.map.component.MapTopSearchBar
import com.ssafy.e102.eumgil.feature.map.component.MapViewport
import com.ssafy.e102.eumgil.feature.map.component.MapViewportUiState

@Immutable
data class MapScreenUiState(
    val searchTitle: String,
    val searchHint: String,
    val searchActionLabel: String,
    val searchAccessibilityLabel: String,
    val viewportState: MapViewportUiState,
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun MapScreen(
    onNavigateToSavedRoutes: () -> Unit,
    onNavigateToMyPage: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: MapScreenUiState? = null,
    onSearchEntryClick: () -> Unit = {},
) {
    val resolvedUiState = uiState ?: defaultMapScreenUiState()

    MapShellScaffold(
        modifier = modifier,
        mapContent = {
            MapViewport(
                state = resolvedUiState.viewportState,
                modifier = Modifier.fillMaxSize(),
            )
        },
        topOverlay = {
            MapTopSearchBar(
                title = resolvedUiState.searchTitle,
                hint = resolvedUiState.searchHint,
                actionLabel = resolvedUiState.searchActionLabel,
                accessibilityLabel = resolvedUiState.searchAccessibilityLabel,
                onClick = onSearchEntryClick,
            )
        },
    )
}

@Composable
private fun defaultMapScreenUiState(): MapScreenUiState {
    return MapScreenUiState(
        searchTitle = stringResource(id = R.string.map_shell_search_title),
        searchHint = stringResource(id = R.string.map_shell_search_hint),
        searchActionLabel = stringResource(id = R.string.map_shell_search_action),
        searchAccessibilityLabel = stringResource(id = R.string.map_shell_search_a11y_label),
        viewportState = MapViewportUiState(
            integrationState = MapIntegrationState.Unbound,
            regionLabel = stringResource(id = R.string.map_shell_region_label),
            statusLabel = stringResource(id = R.string.map_shell_status_label),
            title = stringResource(id = R.string.map_shell_viewport_title),
            description = stringResource(id = R.string.map_shell_viewport_description),
            supportingText = stringResource(id = R.string.map_shell_viewport_supporting_text),
        ),
    )
}
