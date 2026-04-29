package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import kotlinx.coroutines.launch

@Composable
fun LowVisionMyPageRoute(
    onModeChangeClick: () -> Unit,
    onAppInfoClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContainer =
        remember(context.applicationContext) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    val coroutineScope = rememberCoroutineScope()

    LowVisionMyPageScreen(
        onModeChangeClick = onModeChangeClick,
        onAppInfoClick = onAppInfoClick,
        onLogoutClick = {
            coroutineScope.launch {
                appContainer.authSessionRepository.clearAuthSession()
                onLogoutClick()
            }
        },
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

@Composable
fun LowVisionAppInfoRoute(
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LowVisionAppInfoScreen(
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}
