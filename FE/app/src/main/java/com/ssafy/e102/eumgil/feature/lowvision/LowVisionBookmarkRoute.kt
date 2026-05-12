package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteUiEvent
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteViewModel

@Composable
fun LowVisionBookmarkRoute(
    onNavigateToRouteSetting: () -> Unit,
    onNavigateToRouteBriefing: () -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContainer =
        remember(context.applicationContext) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    val viewModelFactory =
        remember(appContainer) {
            SavedRouteViewModel.provideFactory(
                authSessionRepository = appContainer.authSessionRepository,
                bookmarkRepository = appContainer.bookmarkRepository,
                routeBookmarkRepository = appContainer.routeBookmarkRepository,
                destinationSelectionRepository = appContainer.destinationSelectionRepository,
                searchRepository = appContainer.searchRepository,
            )
        }
    val owner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "LowVisionBookmarkRoute requires a ViewModelStoreOwner."
        }
    val viewModel =
        remember(owner, viewModelFactory) {
            ViewModelProvider(owner, viewModelFactory)[SavedRouteViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onNavigateToRouteSetting) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SavedRouteUiEvent.NavigateToRouteSetting -> onNavigateToRouteSetting()
                SavedRouteUiEvent.NavigateToRouteBriefing -> onNavigateToRouteBriefing()
                SavedRouteUiEvent.NavigateToMap,
                is SavedRouteUiEvent.ShowSnackbar,
                -> Unit
            }
        }
    }

    LowVisionFontTheme {
        LowVisionBookmarkScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onTabSelected = onTabSelected,
            modifier = modifier,
        )
    }
}
