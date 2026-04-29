package com.ssafy.e102.eumgil.feature.lowvision

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.savedroute.SavedPlaceUiModel
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteScreenState
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteUiAction
import com.ssafy.e102.eumgil.feature.savedroute.SavedRouteUiState

private val LowVisionYellow = Color(0xFFFFD400)
private val LowVisionDivider = Color(0xFF333333)

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
                .background(Color.Black),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(id = R.string.low_vision_bookmark_title),
                modifier = Modifier.fillMaxWidth(),
                color = LowVisionYellow,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 60.sp,
                textAlign = TextAlign.Center,
            )

            when (uiState.screenState) {
                SavedRouteScreenState.LOADING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = LowVisionYellow)
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        itemsIndexed(
                            items = uiState.places,
                            key = { _, place -> place.placeId },
                        ) { index, place ->
                            LowVisionBookmarkPlaceCard(
                                index = index + 1,
                                place = place,
                                onNavigateClick = {
                                    onAction(SavedRouteUiAction.RouteGuideClicked(place.placeId))
                                },
                                onRemoveClick = {
                                    onAction(SavedRouteUiAction.BookmarkRemoveClicked(place.placeId))
                                },
                            )
                        }
                    }
                }
            }
        }

        LowVisionBookmarkBottomNav(
            selectedTab = LowVisionBottomTab.BOOKMARK,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun LowVisionBookmarkPlaceCard(
    index: Int,
    place: SavedPlaceUiModel,
    onNavigateClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(width = 3.dp, color = LowVisionYellow, shape = RoundedCornerShape(16.dp))
                .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "$index. ${place.name}",
            color = LowVisionYellow,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 36.sp,
        )
        Text(
            text = place.address ?: stringResource(id = R.string.low_vision_bookmark_no_address),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 30.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LowVisionBookmarkButton(
                labelRes = R.string.low_vision_bookmark_navigate,
                filled = true,
                onClick = onNavigateClick,
                modifier = Modifier.weight(1f),
            )
            LowVisionBookmarkButton(
                labelRes = R.string.low_vision_bookmark_remove,
                filled = false,
                onClick = onRemoveClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LowVisionBookmarkButton(
    @StringRes labelRes: Int,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(id = labelRes)
    Surface(
        modifier =
            modifier
                .heightIn(min = 56.dp)
                .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (filled) LowVisionYellow else Color.Black,
        border = BorderStroke(width = 2.dp, color = LowVisionYellow),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (filled) Color.Black else LowVisionYellow,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
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
            color = LowVisionYellow,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 38.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LowVisionBookmarkBottomNav(
    selectedTab: LowVisionBottomTab,
    onTabSelected: (LowVisionBottomTab) -> Unit,
) {
    val items =
        listOf(
            LowVisionBottomTab.HOME to LowVisionBookmarkBottomNavItem(
                iconRes = R.drawable.ic_nav_home_filled,
                labelRes = R.string.low_vision_nav_home,
            ),
            LowVisionBottomTab.BOOKMARK to LowVisionBookmarkBottomNavItem(
                iconRes = R.drawable.ic_nav_bookmark_outline,
                labelRes = R.string.low_vision_nav_bookmark,
            ),
            LowVisionBottomTab.CATEGORY to LowVisionBookmarkBottomNavItem(
                iconRes = R.drawable.ic_nav_category_grid,
                labelRes = R.string.low_vision_nav_category,
            ),
            LowVisionBottomTab.MY_PAGE to LowVisionBookmarkBottomNavItem(
                iconRes = R.drawable.ic_nav_person_outline,
                labelRes = R.string.low_vision_nav_my_page,
            ),
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(104.dp)
                .background(Color.Black)
                .border(width = 1.dp, color = LowVisionDivider),
    ) {
        items.forEach { (tab, item) ->
            val selected = tab == selectedTab
            val backgroundColor = if (selected) LowVisionYellow else Color.Black
            val contentColor = if (selected) Color.Black else Color.White
            val label = stringResource(id = item.labelRes)

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(backgroundColor)
                        .border(width = 1.dp, color = LowVisionDivider)
                        .clickable(onClickLabel = label, role = Role.Tab) { onTabSelected(tab) }
                        .semantics {
                            role = Role.Tab
                            contentDescription = label
                        }
                        .padding(vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(33.dp),
                    )
                }
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private data class LowVisionBookmarkBottomNavItem(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
)
