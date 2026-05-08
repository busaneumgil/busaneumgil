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
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import kotlinx.coroutines.flow.collect

@Composable
fun SearchEntryRoute(
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String, RouteEditingTarget) -> Unit,
    onNavigateToVoiceInput: () -> Unit,
    onNavigateToRouteSetting: () -> Unit,
    initialEditingTarget: RouteEditingTarget = RouteEditingTarget.DESTINATION,
    preserveEntryStateOnReentry: Boolean = false,
    modifier: Modifier = Modifier,
) {
    SearchRouteContent(
        destination = SearchScreenDestination.Entry,
        initialQuery = null,
        initialEditingTarget = initialEditingTarget,
        preserveEntryStateOnReentry = preserveEntryStateOnReentry,
        onNavigateBack = onNavigateBack,
        onNavigateToResults = onNavigateToResults,
        onNavigateToVoiceInput = onNavigateToVoiceInput,
        onNavigateToRouteSetting = onNavigateToRouteSetting,
        modifier = modifier,
    )
}

@Composable
fun SearchResultsRoute(
    initialQuery: String,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String, RouteEditingTarget) -> Unit,
    onNavigateToVoiceInput: () -> Unit,
    onNavigateToRouteSetting: () -> Unit,
    initialEditingTarget: RouteEditingTarget = RouteEditingTarget.DESTINATION,
    modifier: Modifier = Modifier,
) {
    SearchRouteContent(
        destination = SearchScreenDestination.Results,
        initialQuery = initialQuery,
        initialEditingTarget = initialEditingTarget,
        onNavigateBack = onNavigateBack,
        onNavigateToResults = onNavigateToResults,
        onNavigateToVoiceInput = onNavigateToVoiceInput,
        onNavigateToRouteSetting = onNavigateToRouteSetting,
        modifier = modifier,
    )
}

@Composable
fun SearchVoiceInputRoute(
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String, RouteEditingTarget) -> Unit,
    initialEditingTarget: RouteEditingTarget = RouteEditingTarget.DESTINATION,
    onStartVoiceCapture: () -> Unit = {},
    onStopVoiceCapture: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SearchRouteContent(
        destination = SearchScreenDestination.VoiceInput,
        initialQuery = null,
        initialEditingTarget = initialEditingTarget,
        onNavigateBack = onNavigateBack,
        onNavigateToResults = onNavigateToResults,
        onNavigateToVoiceInput = {},
        onNavigateToRouteSetting = {},
        onStartVoiceCapture = onStartVoiceCapture,
        onStopVoiceCapture = onStopVoiceCapture,
        modifier = modifier,
    )
}

@Composable
private fun SearchRouteContent(
    destination: SearchScreenDestination,
    initialQuery: String?,
    initialEditingTarget: RouteEditingTarget,
    preserveEntryStateOnReentry: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String, RouteEditingTarget) -> Unit,
    onNavigateToVoiceInput: () -> Unit,
    onNavigateToRouteSetting: () -> Unit,
    onStartVoiceCapture: () -> Unit = {},
    onStopVoiceCapture: () -> Unit = {},
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

    LaunchedEffect(viewModel, initialEditingTarget) {
        viewModel.onAction(SearchUiAction.EditingTargetConfigured(editingTarget = initialEditingTarget))
    }

    LaunchedEffect(viewModel, destination, preserveEntryStateOnReentry) {
        if (destination == SearchScreenDestination.Entry) {
            viewModel.onAction(
                SearchUiAction.EntryRouteEntered(
                    preserveState = preserveEntryStateOnReentry,
                ),
            )
        }
    }

    LaunchedEffect(viewModel, initialQuery) {
        if (initialQuery != null) {
            viewModel.onAction(SearchUiAction.ResultsRouteEntered(query = initialQuery))
        }
    }

    LaunchedEffect(viewModel, destination) {
        if (destination == SearchScreenDestination.VoiceInput) {
            viewModel.onAction(SearchUiAction.VoiceRouteEntered)
        }
    }

    LaunchedEffect(
        viewModel,
        onNavigateBack,
        onNavigateToResults,
        onNavigateToVoiceInput,
        onNavigateToRouteSetting,
        onStartVoiceCapture,
        onStopVoiceCapture,
    ) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                SearchUiEvent.NavigateBack -> onNavigateBack()
                SearchUiEvent.NavigateToVoiceInput -> onNavigateToVoiceInput()
                is SearchUiEvent.NavigateToResults -> onNavigateToResults(event.query, event.editingTarget)
                SearchUiEvent.StartVoiceCapture -> onStartVoiceCapture()
                SearchUiEvent.StopVoiceCapture -> onStopVoiceCapture()
                SearchUiEvent.NavigateToRouteSetting -> onNavigateToRouteSetting()
                SearchUiEvent.NavigateToRouteBriefing,
                SearchUiEvent.NavigateToLowVisionBookmark -> Unit
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
