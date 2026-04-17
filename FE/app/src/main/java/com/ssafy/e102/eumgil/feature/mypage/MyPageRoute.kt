package com.ssafy.e102.eumgil.feature.mypage

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.app.BusanEumgilApp

@Composable
fun MyPageRoute(
    onNavigateToMap: () -> Unit,
    onNavigateToSavedRoutes: () -> Unit,
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
            MyPageViewModel.provideFactory(settingsRepository = appContainer.settingsRepository)
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "MyPageRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[MyPageViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyPageScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateToMap = onNavigateToMap,
        onNavigateToSavedRoutes = onNavigateToSavedRoutes,
        modifier = modifier,
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
