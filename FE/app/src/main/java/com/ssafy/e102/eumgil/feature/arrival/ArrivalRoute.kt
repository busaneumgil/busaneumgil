package com.ssafy.e102.eumgil.feature.arrival

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ArrivalRoute(
    onNavigateToMap: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContainer =
        remember(context.applicationContext) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    val viewModelFactory =
        remember(appContainer) {
            ArrivalViewModel.provideFactory(
                bookmarkRepository = appContainer.bookmarkRepository,
                destinationSelectionRepository = appContainer.destinationSelectionRepository,
            )
        }
    val viewModel: ArrivalViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, onNavigateToMap, onNavigateToSearch, snackbarHostState, context) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                ArrivalUiEvent.NavigateToMap -> onNavigateToMap()
                ArrivalUiEvent.NavigateToSearch -> onNavigateToSearch()
                is ArrivalUiEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(
                        message = context.getString(event.messageResId),
                    )
            }
        }
    }

    ArrivalScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}
