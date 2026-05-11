package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import kotlinx.coroutines.launch

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
    val selectedDestination by
        appContainer.destinationSelectionRepository.selectedDestination.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LowVisionFontTheme {
        LowVisionNavigationCompleteScreen(
            isSaveEnabled = selectedDestination != null,
            onSaveClick = {
                val destination = selectedDestination ?: return@LowVisionNavigationCompleteScreen
                coroutineScope.launch {
                    appContainer.bookmarkRepository.saveBookmark(destination.toLowVisionBookmarkData())
                    onNavigateToBookmark()
                }
            },
            onCompleteClick = { onTabSelected(LowVisionBottomTab.HOME) },
            modifier = modifier,
        )
    }
}

private fun PlaceDestination.toLowVisionBookmarkData(): BookmarkData =
    BookmarkData(
        placeId = placeId,
        placeName = name,
        address = address?.takeIf { value -> value.isNotBlank() },
        latitude = latitude,
        longitude = longitude,
        category = category?.name,
    )
