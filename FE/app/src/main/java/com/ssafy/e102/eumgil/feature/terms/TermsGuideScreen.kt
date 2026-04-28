package com.ssafy.e102.eumgil.feature.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.ssafy.e102.eumgil.feature.terms.component.TermsAccessibleBottomNav
import com.ssafy.e102.eumgil.feature.terms.component.TermsPagerIndicator

/**
 * High-contrast "약관 안내" walkthrough screen — generic shell for all 5 steps.
 *
 * Source: Figma file MREqSzkmwhRcXnFS3lzW17 (E102-mockup), nodes
 * 328:486 / 328:528 / 328:570 / 328:612 / 328:652.
 *
 * Per-step content (icon, card label, hint copy, presence of "자세히 보기" button)
 * comes from [TermsGuideStep]. The screen itself is intentionally pure UI: it does
 * not own step state, navigation, or persistence — those belong to [TermsGuideRoute].
 *
 * Design rules taken from the Figma variables panel:
 *   - color/yellow/50  = #FFCC00  (Supernova)
 *   - color/black/solid = #000000
 *   - color/grey/27    = #444444  (Tundora, inactive dots)
 *   - color/grey/47    = #777777  (Boulder, inactive nav text)
 *   - color/grey/13    = #222222  (Mine Shaft, bottom nav top border)
 *   - radius main-card = 35dp, button = 12dp
 *   - title 24sp/ExtraBold, card 40sp/Black + letter spacing -1sp,
 *     hint 18sp/Bold, button 20sp/Black, nav label 10sp/Normal.
 */
@Composable
fun TermsGuideScreen(
    uiState: TermsGuideUiState,
    onAdvance: () -> Unit,
    onMoreDetails: () -> Unit,
    onTabSelected: (TermsBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: TermsBottomTab = TermsBottomTab.HOME,
) {
    val step = uiState.step
    val cardLabel = stringResource(id = step.cardLabelRes)
    val hintText = stringResource(id = step.hintRes)
    val cardA11y = stringResource(id = R.string.terms_guide_card_a11y, cardLabel, hintText)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // 1. Header area: pagination dots + "약관 안내" title.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TermsPagerIndicator(
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
            )
            Text(
                text = stringResource(id = R.string.terms_guide_header_title),
                color = Color(0xFFFFCC00),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 2. Main card — yellow surface with step icon + step label.
        //    Double-tap commits the step (advance or finalize), per Figma hint copy
        //    and the existing onboarding voice-guide pattern.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 42.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(335.dp)
                    .clip(RoundedCornerShape(35.dp))
                    .background(Color(0xFFFFCC00))
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(35.dp),
                    )
                    .pointerInput(step) {
                        detectTapGestures(
                            onDoubleTap = { onAdvance() },
                        )
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = cardA11y
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(id = step.iconRes),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(90.dp),
                    )
                    Text(
                        text = cardLabel,
                        color = Color.Black,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 3. Bottom content — hint always present; "자세히 보기" button conditional.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = hintText,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp, bottom = 22.dp),
            )

            if (step.showMoreButton) {
                Box(
                    modifier = Modifier
                        .width(288.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFCC00))
                        .clickable { onMoreDetails() }
                        .padding(top = 20.dp, bottom = 21.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(id = R.string.terms_guide_more_button),
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }

        // 4. Bottom navigation (top border #222 = grey/13).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222))
                .padding(top = 1.dp),
        ) {
            TermsAccessibleBottomNav(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }
    }
}
