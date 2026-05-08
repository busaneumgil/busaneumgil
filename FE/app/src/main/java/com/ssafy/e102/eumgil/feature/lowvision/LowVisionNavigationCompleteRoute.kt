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
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiAction
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiEvent
import com.ssafy.e102.eumgil.feature.navigation.NavigationViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun LowVisionNavigationCompleteRoute(
    onNavigateToBookmark: () -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val appContainer =
        remember(appContext) {
            (appContext as BusanEumgilApp).appContainer
        }
    val activity = remember(context) { context.findComponentActivity() }
    val viewModelFactory =
        remember(appContainer.currentLocationManager, appContainer.bookmarkRepository) {
            NavigationViewModel.provideFactory(
                currentLocationManager = appContainer.currentLocationManager,
                bookmarkRepository = appContainer.bookmarkRepository,
            )
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "LowVisionNavigationCompleteRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[NavigationViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onNavigateToBookmark) {
        viewModel.uiEvent.collect { event ->
            if (event == NavigationUiEvent.NavigateToSavedRoute) {
                onNavigateToBookmark()
            }
        }
    }

    LowVisionNavigationCompleteScreen(
        isSaveEnabled = uiState.isExitEnabled,
        onSaveClick = { viewModel.onAction(NavigationUiAction.SaveBookmarkClicked) },
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
