package com.ssafy.e102.eumgil.feature.mypage

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import kotlinx.coroutines.launch

private const val DURIBAL_PHONE_NUMBER = "1555-1114"

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
                authLogoutRepository = appContainer.authLogoutRepository,
                userProfileRepository = appContainer.userProfileRepository,
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
    val coroutineScope = rememberCoroutineScope()
    var isDuribalConfirmDialogVisible by rememberSaveable { mutableStateOf(false) }

    fun showSnackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(viewModel, snackbarHostState, preparingMessage) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                MyPageUiEvent.NavigateToUserTypePrimary -> onNavigateToUserTypePrimary()
                MyPageUiEvent.NavigateToLogin -> onNavigateToLogin()
                MyPageUiEvent.NavigateToReportHistory -> onNavigateToReportHistory()
                MyPageUiEvent.NavigateToAppInfo -> onNavigateToAppInfo()
                MyPageUiEvent.ShowPreparingMessage -> showSnackbar(preparingMessage)
                MyPageUiEvent.ShowProfileSyncFailedMessage ->
                    showSnackbar(
                        context.getString(R.string.my_page_profile_sync_failed),
                    )
                is MyPageUiEvent.ShowSnackbar -> showSnackbar(event.message)
            }
        }
    }

    MyPageScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        isDuribalConfirmDialogVisible = isDuribalConfirmDialogVisible,
        onDuribalCallClick = { isDuribalConfirmDialogVisible = true },
        onDuribalConfirmDismiss = { isDuribalConfirmDialogVisible = false },
        onDuribalConfirm = {
            isDuribalConfirmDialogVisible = false
            context.startActivity(createDuribalDialIntent())
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

internal fun createDuribalDialIntent(): Intent =
    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${DURIBAL_PHONE_NUMBER.filter(Char::isDigit)}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
