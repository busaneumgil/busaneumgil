package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListAmber
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListBg
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListCard
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListSubText
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionBottomNav
import com.ssafy.e102.eumgil.feature.savedroute.SavedPlaceUiModel
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteScreenState
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteUiAction
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteUiState

@Composable
fun LowVisionBookmarkScreen(
    uiState: SavedRouteUiState,
    onAction: (SavedRouteUiAction) -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(PlaceListBg),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .padding(
                        horizontal = LowVisionScreenDefaults.screenHorizontalPadding,
                        vertical = LowVisionScreenDefaults.screenVerticalPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(LowVisionScreenDefaults.headerGap),
        ) {
            Text(
                text = "북마크 목록",
                modifier = Modifier.fillMaxWidth(),
                color = PlaceListAmber,
                fontSize = LowVisionScreenDefaults.headerFontSize,
                fontWeight = FontWeight.Black,
                lineHeight = LowVisionScreenDefaults.headerLineHeight,
                textAlign = TextAlign.Center,
            )

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            ) {
                when (uiState.screenState) {
                    SavedRouteScreenState.LOADING -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = PlaceListAmber)
                        }
                    }
                    SavedRouteScreenState.EMPTY -> {
                        LowVisionBookmarkMessage(
                            message = stringResource(id = R.string.low_vision_bookmark_empty),
                        )
                    }
                    SavedRouteScreenState.ERROR -> {
                        LowVisionBookmarkMessage(
                            message = uiState.errorMessage ?: stringResource(id = R.string.low_vision_bookmark_error),
                        )
                    }
                    SavedRouteScreenState.CONTENT -> {
                        LowVisionBookmarkPlaceList(
                            places = uiState.places,
                            onNavigateClick = { place ->
                                onAction(SavedRouteUiAction.RouteGuideClicked(place.placeId))
                            },
                            onRemoveClick = { place ->
                                onAction(SavedRouteUiAction.BookmarkRemoveClicked(place.placeId))
                            },
                        )
                    }
                }
            }
        }

        LowVisionBottomNav(
            selectedTab = LowVisionBottomTab.BOOKMARK,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun LowVisionBookmarkPlaceList(
    places: List<SavedPlaceUiModel>,
    onNavigateClick: (SavedPlaceUiModel) -> Unit,
    onRemoveClick: (SavedPlaceUiModel) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = EumSpacing.medium,
                vertical = EumSpacing.medium,
            ),
        verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
    ) {
        itemsIndexed(
            items = places,
            key = { _, place -> place.placeId },
        ) { index, place ->
            PlaceListCard(
                index = index + 1,
                name = place.name,
                address = place.address,
                bookmarkLabel = "삭제",
                onBookmarkClick = { onRemoveClick(place) },
                onNavigateClick = { onNavigateClick(place) },
                bookmarkContentDescription = "${place.name} 삭제",
                navigateContentDescription = "${place.name} 길찾기. 저시력 안내 화면으로 이동합니다.",
                modifier = Modifier.fillParentMaxHeight(fraction = 0.47f),
            )
        }
    }
}

@Composable
private fun LowVisionBookmarkMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = PlaceListSubText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp,
            textAlign = TextAlign.Center,
        )
    }
}
