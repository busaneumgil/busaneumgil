package com.ssafy.e102.eumgil.feature.savedroute

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListAmber
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListBg
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListCard
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListSubText
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListTabBar
import com.ssafy.e102.eumgil.core.designsystem.theme.BusanEumgilTheme
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

/**
 * 저장 목록(북마크) 화면 — 다크 테마.
 *
 * 상단 제목: "저장 목록"
 * LazyColumn 아이템이 뷰포트 높이의 ~47%를 차지하므로 한 화면에 2개가 보이고,
 * 위아래 스크롤로 추가 장소를 탐색할 수 있습니다.
 *
 * SearchScreen과 동일한 [PlaceListCard] + [PlaceListTabBar] 프레임을 사용합니다.
 * 차이점: 제목 "저장 목록", 북마크 버튼 레이블 "삭제", 탭 바 활성 탭 "bookmark".
 */
@Composable
fun SavedRouteScreen(
    uiState: SavedRouteUiState,
    onAction: (SavedRouteUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PlaceListBg),
    ) {
        // ── 제목 ──────────────────────────────────────────────────────────────
        Text(
            text = "저장 목록",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PlaceListAmber,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EumSpacing.large, bottom = EumSpacing.medium),
        )

        // ── 콘텐츠 영역 ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (uiState.screenState) {
                SavedRouteScreenState.LOADING -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PlaceListAmber)
                    }
                }

                SavedRouteScreenState.EMPTY -> {
                    SavedRouteStateMessage(message = "저장된 장소가 없습니다\n길찾기 후 북마크를 추가해보세요")
                }

                SavedRouteScreenState.ERROR -> {
                    SavedRouteStateMessage(
                        message = uiState.errorMessage ?: "불러오기에 실패했습니다\n잠시 후 다시 시도해주세요",
                        isError = true,
                    )
                }

                SavedRouteScreenState.CONTENT -> {
                    SavedPlaceList(
                        places = uiState.places,
                        onRemoveClick = { place ->
                            onAction(SavedRouteUiAction.BookmarkRemoveClicked(placeId = place.placeId))
                        },
                        onNavigateClick = { place ->
                            onAction(SavedRouteUiAction.RouteGuideClicked(placeId = place.placeId))
                        },
                    )
                }
            }
        }

        // ── 하단 탭 바 ─────────────────────────────────────────────────────────
        PlaceListTabBar(
            activeTab = "bookmark",
            onHomeClick = { onAction(SavedRouteUiAction.ExploreMapClicked) },
            onBookmarkClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── 저장 목록 LazyColumn ──────────────────────────────────────────────────────

@Composable
private fun SavedPlaceList(
    places: List<SavedPlaceUiModel>,
    onRemoveClick: (SavedPlaceUiModel) -> Unit,
    onNavigateClick: (SavedPlaceUiModel) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
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
                // 한 뷰포트에 2개가 보이도록 각 카드 높이를 전체의 절반으로 고정
                modifier = Modifier.fillParentMaxHeight(fraction = 0.47f),
            )
        }
    }
}

// ── 빈 / 오류 상태 메시지 ─────────────────────────────────────────────────────

@Composable
private fun SavedRouteStateMessage(
    message: String,
    isError: Boolean = false,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = if (isError) Color(0xFFFF6B6B) else PlaceListSubText,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
        )
    }
}

// ── 프리뷰 ────────────────────────────────────────────────────────────────────

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF1C1C1E,
    name = "SavedRoute — 목록 있음",
)
@Composable
private fun SavedRouteContentPreview() {
    BusanEumgilTheme {
        SavedRouteScreen(
            uiState = SavedRouteUiState(
                screenState = SavedRouteScreenState.CONTENT,
                places = listOf(
                    SavedPlaceUiModel(
                        placeId = "1",
                        name = "해운대역\n공공화장실",
                        address = "해운대구",
                        category = "TOILET",
                        latitude = 35.163,
                        longitude = 129.163,
                    ),
                    SavedPlaceUiModel(
                        placeId = "2",
                        name = "해운대구\n보건소",
                        address = "해운대구",
                        category = "OTHER",
                        latitude = 35.160,
                        longitude = 129.160,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFF1C1C1E,
    name = "SavedRoute — 비어 있음",
)
@Composable
private fun SavedRouteEmptyPreview() {
    BusanEumgilTheme {
        SavedRouteScreen(
            uiState = SavedRouteUiState(screenState = SavedRouteScreenState.EMPTY),
            onAction = {},
        )
    }
}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   