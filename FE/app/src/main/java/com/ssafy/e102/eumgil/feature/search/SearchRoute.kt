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
fun SearchEntryRoute(
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String) -> Unit,
    onNavigateToRouteSetting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchRouteContent(
        destination = SearchScreenDestination.Entry,
        initialQuery = null,
        onNavigateBack = onNavigateBack,
        onNavigateToResults = onNavigateToResults,
        onNavigateToRouteSetting = onNavigateToRouteSetting,
        modifier = modifier,
    )
}

@Composable
fun SearchResultsRoute(
    initialQuery: String,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String) -> Unit,
    onNavigateToRouteSetting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchRouteContent(
        destination = SearchScreenDestination.Results,
        initialQuery = initialQuery,
        onNavigateBack = onNavigateBack,
        onNavigateToResults = onNavigateToResults,
        onNavigateToRouteSetting = onNavigateToRouteSetting,
        modifier = modifier,
    )
}

@Composable
private fun SearchRouteContent(
    destination: SearchScreenDestination,
    initialQuery: String?,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String) -> Unit,
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
                bookmarkRepository = appContainer.bookmarkRepository,
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

    LaunchedEffect(viewModel, initialQuery) {
        if (initialQuery != null) {
            viewModel.onAction(SearchUiAction.ResultsRouteEntered(query = initialQuery))
        }
    }

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToResults, onNavigateToRouteSetting) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                SearchUiEvent.NavigateBack -> onNavigateBack()
                is SearchUiEvent.NavigateToResults -> onNavigateToResults(event.query)
                SearchUiEvent.NavigateToRouteSetting -> onNavigateToRouteSetting()
            }
        }
    }

    SearchScreen(
        destination = destination,
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
