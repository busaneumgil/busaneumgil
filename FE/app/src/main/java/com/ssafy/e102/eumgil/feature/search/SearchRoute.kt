package com.ssafy.e102.eumgil.feature.search

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import kotlinx.coroutines.flow.collect

@Composable
fun SearchRoute(
    onNavigateBack: () -> Unit,
    onNavigateToRouteSetting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContainer =
        remember(context.applicationContext) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    val activity = remember(context) { context.findComponentActivity() }
    val viewModelFactory =
        remember(appContainer) {
            SearchViewModel.provideFactory(
                searchRepository = appContainer.searchRepository,
                destinationSelectionRepository = appContainer.destinationSelectionRepository,
                placesRepository = appContainer.placesRepository,
            )
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "SearchRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[SearchViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToRouteSetting) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                SearchUiEvent.NavigateBack -> onNavigateBack()
                SearchUiEvent.NavigateToRouteSetting -> onNavigateToRouteSetting()
            }
        }
    }

    SearchScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
