package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListAmber
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListBg
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListCard
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListSubText
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.feature.search.SearchResultUiState
import com.ssafy.e102.eumgil.feature.search.SearchUiAction
import com.ssafy.e102.eumgil.feature.search.SearchUiState

internal object LowVisionSearchLayoutDefaults {
    val resultCardMinHeight = 560.dp
    val resultListBottomPadding = 96.dp
    val actionButtonHeight = 78.dp
    val actionButtonGap = 16.dp
    const val actionButtonCount = 2
}

@Composable
fun LowVisionSearchScreen(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
    modifier: Modifier = Modifier,
    categoryLabel: String? = null,
) {
    Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(PlaceListBg)
                    .statusBarsPadding()
                    .padding(
                        horizontal = LowVisionScreenDefaults.screenHorizontalPadding,
                        vertical = LowVisionScreenDefaults.screenVerticalPadding,
                    ),
        verticalArrangement = Arrangement.spacedBy(LowVisionScreenDefaults.headerGap),
    ) {
        Text(
            text = "검색 결과",
            fontSize = LowVisionScreenDefaults.headerFontSize,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = LowVisionScreenDefaults.headerLineHeight,
            color = PlaceListAmber,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!categoryLabel.isNullOrBlank()) {
            LowVisionSearchCategoryHeader(categoryLabel = categoryLabel)
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            when (val state = uiState.resultState) {
                is SearchResultUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PlaceListAmber)
                    }
                }

                is SearchResultUiState.Success -> {
                    if (state.results.isEmpty()) {
                        LowVisionSearchNoResultMessage()
                    } else {
                        LowVisionSearchResultList(
                            results = state.results,
                            onBookmarkClick = { result ->
                                onAction(SearchUiAction.LowVisionBookmarkSaveClicked(result = result))
                            },
                            onNavigateClick = { result ->
                                onAction(SearchUiAction.SearchResultClicked(result = result))
                            },
                            onBriefingClick = { result ->
                                onAction(SearchUiAction.SearchResultBriefingClicked(result = result))
                            },
                        )
                    }
                }

                else -> LowVisionSearchNoResultMessage()
            }
        }
    }
}

@Composable
private fun LowVisionSearchCategoryHeader(categoryLabel: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = categoryLabel,
            color = Color.White,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.18f)
                    .height(6.dp)
                    .background(
                        color = PlaceListAmber,
                        shape = RoundedCornerShape(999.dp),
                    ),
        )
    }
}

@Composable
private fun LowVisionSearchResultList(
    results: List<SearchResult>,
    onBookmarkClick: (SearchResult) -> Unit,
    onNavigateClick: (SearchResult) -> Unit,
    onBriefingClick: (SearchResult) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = EumSpacing.medium,
                top = EumSpacing.medium,
                end = EumSpacing.medium,
                bottom = LowVisionSearchLayoutDefaults.resultListBottomPadding,
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
                bookmarkLabel = "저장",
                onBookmarkClick = { onBookmarkClick(result) },
                onNavigateClick = { onNavigateClick(result) },
                onContentClick = { onBriefingClick(result) },
                contentClickDescription = "${result.title} 경로 브리핑. 두 번 탭하면 브리핑 화면으로 이동합니다.",
                bookmarkContentDescription = "${result.title} 저장. 저장 후 북마크로 이동합니다.",
                navigateContentDescription = "${result.title} 길찾기. 저시력 안내 화면으로 이동합니다.",
                modifier = Modifier.height(LowVisionSearchLayoutDefaults.resultCardMinHeight),
            )
        }
    }
}

@Composable
private fun LowVisionSearchNoResultMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "목록 없음.",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PlaceListSubText,
            textAlign = TextAlign.Center,
        )
    }
}
