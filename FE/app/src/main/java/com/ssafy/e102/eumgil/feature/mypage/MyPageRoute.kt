package com.ssafy.e102.eumgil.feature.mypage

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.app.BusanEumgilApp

@Composable
fun MyPageRoute(
    onNavigateToUserTypePrimary: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToReportHistory: () -> Unit,
    onNavigateToAppInfo: () -> Unit,
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
            MyPageViewModel.provideFactory(
                settingsRepository = appContainer.settingsRepository,
                authSessionRepository = appContainer.authSessionRepository,
            )
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "MyPageRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[MyPageViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val preparingMessage = stringResource(id = R.string.my_page_preparing_message)

    LaunchedEffect(viewModel, snackbarHostState, preparingMessage) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                MyPageUiEvent.NavigateToUserTypePrimary -> onNavigateToUserTypePrimary()
                MyPageUiEvent.NavigateToLogin -> onNavigateToLogin()
                MyPageUiEvent.NavigateToReportHistory -> onNavigateToReportHistory()
                MyPageUiEvent.NavigateToAppInfo -> onNavigateToAppInfo()
                MyPageUiEvent.ShowPreparingMessage -> snackbarHostState.showSnackbar(preparingMessage)
            }
        }
    }

    MyPageScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
