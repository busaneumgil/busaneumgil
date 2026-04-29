package com.ssafy.e102.eumgil.feature.search

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListAmber
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListBg
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListCard
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListSubText
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListTabBar
import com.ssafy.e102.eumgil.core.designsystem.theme.BusanEumgilTheme
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.SearchResult

/**
 * 검색 화면 — 다크 테마.
 *
 * 상단 제목: "검색 결과"
 * 검색 입력 필드 → 결과를 카드 목록으로 표시.
 * LazyColumn 아이템이 뷰포트 높이의 ~47%를 차지하므로 한 화면에 2개가 보이고,
 * 위아래 스크롤로 추가 장소를 탐색할 수 있습니다.
 */
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PlaceListBg),
    ) {
        // ── 제목 ──────────────────────────────────────────────────────────────
        Text(
            text = "검색 결과",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PlaceListAmber,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EumSpacing.large, bottom = EumSpacing.medium),
        )

        // ── 검색 입력창 ────────────────────────────────────────────────────────
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { onAction(SearchUiAction.QueryChanged(query = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EumSpacing.medium),
            placeholder = {
                Text(
                    text = "장소를 입력하세요",
                    color = PlaceListSubText,
                    fontSize = 16.sp,
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { onAction(SearchUiAction.SearchSubmitted) },
            ),
            shape = RoundedCornerShape(EumRadius.large),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = PlaceListAmber,
                unfocusedBorderColor = PlaceListAmber.copy(alpha = 0.5f),
                cursorColor = PlaceListAmber,
            ),
        )

        // ── 결과 영역 ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (val state = uiState.resultState) {
                SearchResultUiState.Initial,
                SearchResultUiState.EmptyQuery -> SearchEmptyStateMessage(
                    message = "검색어를 입력하고\n결과를 확인해보세요",
                )

                is SearchResultUiState.Typing -> SearchEmptyStateMessage(
                    message = "검색 버튼을 눌러\n결과를 확인하세요",
                )

                is SearchResultUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PlaceListAmber)
                    }
                }

                is SearchResultUiState.Success -> {
                    if (state.results.isEmpty()) {
                        SearchEmptyStateMessage(message = "'${state.query}'\n검색 결과가 없습니다")
                    } else {
                        SearchResultList(
                            results = state.results,
                            onBookmarkClick = { result ->
                                onAction(SearchUiAction.BookmarkToggleClicked(result = result))
                            },
                            onNavigateClick = { result ->
                                onAction(SearchUiAction.SearchResultClicked(result = result))
                            },
                        )
                    }
                }

                is SearchResultUiState.Empty -> SearchEmptyStateMessage(
                    message = "'${state.query}'\n검색 결과가 없습니다",
                )

                is SearchResultUiState.Error -> SearchEmptyStateMessage(
                    message = "검색 중 오류가 발생했습니다\n다시 시도해주세요",
                    isError = true,
                )
            }
        }

        // ── 하단 탭 바 ─────────────────────────────────────────────────────────
        PlaceListTabBar(
            activeTab = "home",
            onHomeClick = { onAction(SearchUiAction.BackClicked) },
            onBookmarkClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── 검색 결과 LazyColumn ───────────────────────────────────────────────────────

@Composable
private fun SearchResultList(
    results: List<SearchResult>,
    onBookmarkClick: (SearchResult) -> Unit,
    onNavigateClick: (SearchResult) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            horizontal = EumSpacing.medium,
            vertical = EumSpacing.medium,
        ),
        verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
    ) {
        itemsIndexed(
            items = results,
            key = { _, result -> result.placeId },
        ) { index, result ->
            PlaceListCard(
                index = index + 1,
                name = result.title,
                address = result.subtitle.ifBlank { null },
                bookmarkLabel = "북마크",
                onBookmarkClick = { onBookmarkClick(result) },
                onNavigateClick = { onNavigateClick(result) },
                // 한 뷰포트에 2개가 보이도록 각 카드 높이를 전체의 절반으로 고정
                modifier = Modifier.fillParentMaxHeight(fraction = 0.47f),
            )
        }
    }
}

// ── 빈 / 오류 상태 메시지 ─────────────────────────────────────────────────────

@Composable
private fun SearchEmptyStateMessage(
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
    name = "Search — 결과 있음",
)
@Composable
private fun SearchScreenSuccessPreview() {
    BusanEumgilTheme {
        SearchScreen(
            uiState = SearchUiState(
                query = "해운대",
                resultState = SearchResultUiState.Success(
                    query = "해운대",
                    results = listOf(
                        SearchResult(
                            placeId = "1",
                            title = "해운대역\n공공화장실",
                            subtitle = "해운대구",
                            latitude = 35.163,
                            longitude = 129.163,
                        ),
                        SearchResult(
                            placeId = "2",
                            title = "해운대구\n보건소",
                            subtitle = "해운대구",
                            latitude = 35.160,
                            longitude = 129.160,
                        ),
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
    name = "Search — 초기 상태",
)
@Composable
private fun SearchScreenInitialPreview() {
    BusanEumgilTheme {
        SearchScreen(
            uiState = SearchUiState(),
            onAction = {},
        )
    }
}
