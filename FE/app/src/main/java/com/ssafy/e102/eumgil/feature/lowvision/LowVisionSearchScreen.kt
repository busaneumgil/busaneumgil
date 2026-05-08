package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListAmber
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListBg
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListOnAmber
import com.ssafy.e102.eumgil.core.designsystem.component.place.PlaceListSubText
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.feature.search.SearchResultUiState
import com.ssafy.e102.eumgil.feature.search.SearchUiAction
import com.ssafy.e102.eumgil.feature.search.SearchUiState

internal object LowVisionSearchLayoutDefaults {
    val resultCardMinHeight = 320.dp
    val resultCardGap = 12.dp
    val twoCardViewportBudget = 680.dp
    val resultListBottomPadding = 64.dp
    val actionButtonHeight = 58.dp
    val actionButtonGap = 10.dp
    val cardHorizontalPadding = 20.dp
    val cardVerticalPadding = 16.dp
    val cardContentGap = 10.dp
    val cardHeaderGap = 16.dp
    val indexBadgeSize = 52.dp
    val indexFontSize = 30.sp
    val indexLineHeight = 34.sp
    val titleFontSize = 30.sp
    val titleLineHeight = 34.sp
    val addressFontSize = 20.sp
    val addressLineHeight = 24.sp
    val actionIconSize = 32.dp
    val actionIconTextGap = 16.dp
    val actionLabelFontSize = 28.sp
    val actionLabelLineHeight = 32.sp
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

                is SearchResultUiState.Error ->
                    LowVisionSearchNoResultMessage(
                        message = state.message ?: "검색 결과를 다시 확인해 주세요.",
                    )

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
        verticalArrangement = Arrangement.spacedBy(LowVisionSearchLayoutDefaults.resultCardGap),
    ) {
        itemsIndexed(
            items = results,
            key = { _, result -> result.placeId },
        ) { index, result ->
            LowVisionSearchResultCard(
                index = index + 1,
                name = result.title,
                address = result.subtitle.ifBlank { null },
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
private fun LowVisionSearchResultCard(
    index: Int,
    name: String,
    address: String?,
    onBookmarkClick: () -> Unit,
    onNavigateClick: () -> Unit,
    onContentClick: () -> Unit,
    contentClickDescription: String,
    bookmarkContentDescription: String,
    navigateContentDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    width = 3.dp,
                    color = PlaceListAmber,
                    shape = RoundedCornerShape(18.dp),
                )
                .clip(RoundedCornerShape(18.dp))
                .background(PlaceListBg)
                .clickable(role = Role.Button, onClick = onContentClick)
                .semantics {
                    contentDescription = contentClickDescription
                }
                .padding(
                    horizontal = LowVisionSearchLayoutDefaults.cardHorizontalPadding,
                    vertical = LowVisionSearchLayoutDefaults.cardVerticalPadding,
                ),
        verticalArrangement = Arrangement.spacedBy(LowVisionSearchLayoutDefaults.cardContentGap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LowVisionSearchLayoutDefaults.cardHeaderGap),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(LowVisionSearchLayoutDefaults.indexBadgeSize)
                        .background(
                            color = PlaceListAmber,
                            shape = RoundedCornerShape(8.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString(),
                    fontSize = LowVisionSearchLayoutDefaults.indexFontSize,
                    fontWeight = FontWeight.Black,
                    color = PlaceListOnAmber,
                    lineHeight = LowVisionSearchLayoutDefaults.indexLineHeight,
                    letterSpacing = 0.sp,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = name,
                    fontSize = LowVisionSearchLayoutDefaults.titleFontSize,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    lineHeight = LowVisionSearchLayoutDefaults.titleLineHeight,
                    letterSpacing = 0.sp,
                    maxLines = 2,
                )
                if (!address.isNullOrBlank()) {
                    Text(
                        text = address,
                        fontSize = LowVisionSearchLayoutDefaults.addressFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = LowVisionSearchLayoutDefaults.addressLineHeight,
                        letterSpacing = 0.sp,
                        maxLines = 2,
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(LowVisionSearchLayoutDefaults.actionButtonGap),
        ) {
            LowVisionSearchActionButton(
                label = "\uC800\uC7A5",
                iconRes = R.drawable.ic_action_favorite,
                onClick = onBookmarkClick,
                contentDescription = bookmarkContentDescription,
            )
            LowVisionSearchActionButton(
                label = "\uAE38\uCC3E\uAE30",
                iconRes = R.drawable.ic_nav_route,
                onClick = onNavigateClick,
                contentDescription = navigateContentDescription,
            )
        }
    }
}

@Composable
private fun LowVisionSearchActionButton(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(LowVisionSearchLayoutDefaults.actionButtonHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(PlaceListAmber)
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = PlaceListOnAmber),
                    onClick = onClick,
                )
                .semantics {
                    role = Role.Button
                    this.contentDescription = contentDescription
                },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = PlaceListOnAmber,
            modifier = Modifier.size(LowVisionSearchLayoutDefaults.actionIconSize),
        )
        Spacer(modifier = Modifier.size(LowVisionSearchLayoutDefaults.actionIconTextGap))
        Text(
            text = label,
            fontSize = LowVisionSearchLayoutDefaults.actionLabelFontSize,
            lineHeight = LowVisionSearchLayoutDefaults.actionLabelLineHeight,
            fontWeight = FontWeight.Black,
            color = PlaceListOnAmber,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun LowVisionSearchNoResultMessage(message: String = "목록 없음.") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PlaceListSubText,
            textAlign = TextAlign.Center,
        )
    }
}
