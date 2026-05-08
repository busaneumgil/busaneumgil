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
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteUiEvent
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteViewModel

@Composable
fun LowVisionBookmarkRoute(
    onNavigateToRouteSetting: () -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
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
            SavedRouteViewModel.provideFactory(
                bookmarkRepository = appContainer.bookmarkRepository,
                routeBookmarkRepository = appContainer.routeBookmarkRepository,
                destinationSelectionRepository = appContainer.destinationSelectionRepository,
            )
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "LowVisionBookmarkRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[SavedRouteViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onNavigateToRouteSetting) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SavedRouteUiEvent.NavigateToRouteSetting -> onNavigateToRouteSetting()
                SavedRouteUiEvent.NavigateToMap,
                is SavedRouteUiEvent.ShowSnackbar,
                -> Unit
            }
        }
    }

    LowVisionBookmarkScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
