package com.ssafy.e102.eumgil.feature.lowvision

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
import com.ssafy.e102.eumgil.feature.search.SearchUiAction
import com.ssafy.e102.eumgil.feature.search.SearchUiEvent
import com.ssafy.e102.eumgil.feature.search.SearchViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun LowVisionSearchRoute(
    initialQuery: String,
    onNavigateBack: () -> Unit,
    onNavigateToRouteSetting: () -> Unit,
    onNavigateToRouteBriefing: () -> Unit,
    onNavigateToBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    categoryLabel: String? = null,
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
            val owner = checkNotNull(activity) { "LowVisionSearchRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[SearchViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, initialQuery) {
        viewModel.onAction(SearchUiAction.ResultsRouteEntered(query = initialQuery))
    }

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToRouteSetting) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                SearchUiEvent.NavigateBack -> onNavigateBack()
                is SearchUiEvent.NavigateToResults -> Unit
                SearchUiEvent.NavigateToRouteSetting -> onNavigateToRouteSetting()
                SearchUiEvent.NavigateToRouteBriefing -> onNavigateToRouteBriefing()
                SearchUiEvent.NavigateToLowVisionBookmark -> onNavigateToBookmark()
            }
        }
    }

    LowVisionFontTheme {
        LowVisionSearchScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            modifier = modifier,
            categoryLabel = categoryLabel,
        )
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
