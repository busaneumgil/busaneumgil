package com.ssafy.e102.eumgil.feature.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.app.BusanEumgilApp

@Composable
fun RouteSettingEntryRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContainer =
        remember(context.applicationContext) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    val selectedDestination by
        appContainer.destinationSelectionRepository.selectedDestination.collectAsStateWithLifecycle()

    RouteSettingScreen(
        selectedDestination = selectedDestination,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}
