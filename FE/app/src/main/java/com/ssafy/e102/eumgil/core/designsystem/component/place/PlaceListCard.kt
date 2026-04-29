package com.ssafy.e102.eumgil.core.designsystem.component.place

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

// ── 다크 테마 장소 목록 공통 색상 ──────────────────────────────────────────────
val PlaceListBg       = Color(0xFF1C1C1E)
val PlaceListAmber    = Color(0xFFF2B705)
val PlaceListSurface  = Color(0xFF2C2C2E)
val PlaceListDivider  = Color(0xFF3A3A3C)
val PlaceListOnAmber  = Color(0xFF1C1C1E)
val PlaceListSubText  = Color(0xFFAEAEB2)
val PlaceListTabInactive = Color(0xFF636366)

/**
 * 다크 테마 장소 목록 카드.
 *
 * 검색 결과와 저장 목록 화면이 공용으로 사용합니다.
 *
 * @param index          리스트 순번 (1부터 시작, 번호 뱃지에 표시)
 * @param name           장소 이름
 * @param address        주소 또는 지역명 (null 이면 표시 안 함)
 * @param bookmarkLabel  북마크 버튼 레이블 (e.g. "북마크" / "삭제")
 * @param onBookmarkClick 북마크 버튼 클릭
 * @param onNavigateClick 길찾기 버튼 클릭
 */
@Composable
fun PlaceListCard(
    index: Int,
    name: String,
    address: String?,
    bookmarkLabel: String,
    onBookmarkClick: () -> Unit,
    onNavigateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = PlaceListAmber.copy(alpha = 0.8f),
                shape = RoundedCornerShape(EumRadius.large),
            )
            .clip(RoundedCornerShape(EumRadius.large))
            .background(PlaceListBg)
            .padding(EumSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
    ) {
        // ── 헤더: 번호 뱃지 + 이름 + 주소 ──────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            // 번호 뱃지
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = PlaceListAmber,
                        shape = RoundedCornerShape(EumRadius.small),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PlaceListOnAmber,
                )
            }

            // 이름 + 주소
            Column(
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 32.sp,
                )
                if (!address.isNullOrBlank()) {
                    Text(
                        text = address,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = PlaceListSubText,
                    )
                }
            }
        }

        // ── 버튼 영역 ────────────────────────────────────────────────────────
        Column(
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            PlaceActionButton(
                label = bookmarkLabel,
                iconRes = R.drawable.ic_action_favorite,
                onClick = onBookmarkClick,
                contentDescription = "$name $bookmarkLabel",
            )
            PlaceActionButton(
                label = "길찾기",
                iconRes = R.drawable.ic_nav_route,
                onClick = onNavigateClick,
                contentDescription = "$name 길찾기",
            )
        }
    }
}

// ── 내부 버튼 컴포넌트 ────────────────────────────────────────────────────────

@Composable
private fun PlaceActionButton(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(EumRadius.medium))
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
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.size(EumSpacing.small))
        Text(
            text = label,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = PlaceListOnAmber,
        )
    }
}

/**
 * 검색/북마크 화면 공통 하단 탭 바.
 *
 * @param activeTab 활성 탭 ("home" | "bookmark" | "category" | "mypage")
 */
@Composable
fun PlaceListTabBar(
    activeTab: String,
    onHomeClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        Triple("home",     R.drawable.ic_nav_home,            "홈"),
        Triple("bookmark", R.drawable.ic_nav_bookmark_outline, "북마크"),
        Triple("category", R.drawable.ic_nav_category_grid,   "카테고리"),
        Triple("mypage",   R.drawable.ic_nav_mypage,          "마이페이지"),
    )

    Column(modifier = modifier.background(PlaceListBg)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PlaceListDivider),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.small),
        ) {
            tabs.forEach { (key, iconRes, label) ->
                val selected = key == activeTab
                val tint = if (selected) PlaceListOnAmber else PlaceListTabInactive
                val bg   = if (selected) PlaceListAmber   else Color.Transparent

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(EumRadius.small))
                        .background(bg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = when (key) {
                                "home"     -> onHomeClick
                                "bookmark" -> onBookmarkClick
                                else       -> ({})
                            },
                        )
                        .padding(vertical = EumSpacing.xSmall),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = tint,
                    )
                }
            }
        }
    }
}
