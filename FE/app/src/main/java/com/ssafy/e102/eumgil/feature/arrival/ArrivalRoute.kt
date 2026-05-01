package com.ssafy.e102.eumgil.feature.arrival

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ArrivalRoute(
    onNavigateToMap: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArrivalViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onNavigateToMap, onNavigateToSearch) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                ArrivalUiEvent.NavigateToMap -> onNavigateToMap()
                ArrivalUiEvent.NavigateToSearch -> onNavigateToSearch()
            }
        }
    }

    ArrivalScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}
