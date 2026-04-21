package com.ssafy.e102.eumgil.feature.navigation

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
import kotlinx.coroutines.flow.collect

@Composable
fun NavigationRoute(
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    val viewModelFactory = remember { NavigationViewModel.provideFactory() }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "NavigationRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[NavigationViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToMap) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                NavigationUiEvent.NavigateBack -> onNavigateBack()
                NavigationUiEvent.NavigateToMap -> onNavigateToMap()
                is NavigationUiEvent.ShowSnackbar -> Unit
            }
        }
    }

    NavigationScreen(
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
