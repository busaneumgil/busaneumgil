package com.ssafy.e102.eumgil.feature.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumBorderInfo
import com.ssafy.e102.eumgil.core.designsystem.theme.EumBorderSubtle
import com.ssafy.e102.eumgil.core.designsystem.theme.EumHighlightYellow
import com.ssafy.e102.eumgil.core.designsystem.theme.EumPrimary600
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSurfaceInfo
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSurfaceSubtle
import com.ssafy.e102.eumgil.core.designsystem.theme.EumTextMuted
import com.ssafy.e102.eumgil.core.designsystem.theme.EumTextTertiary
import com.ssafy.e102.eumgil.core.designsystem.theme.EumWhite

@Composable
fun TutorialScreen(
    uiState: TutorialUiState,
    onPrimaryActionClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryActionLabel =
        stringResource(
            id =
                resolveTutorialPrimaryActionLabel(
                    entryPoint = uiState.entryPoint,
                    isLastStep = uiState.step.isLast,
                ),
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(EumSurfaceSubtle)
                .statusBarsPadding()
                .padding(horizontal = EumSpacing.medium)
                .padding(bottom = EumSpacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onSkipClick) {
                Text(
                    text = stringResource(id = R.string.tutorial_action_skip),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            TutorialHeader(uiState = uiState)
            TutorialPhoneMockup(step = uiState.step)
            TutorialPagerIndicator(
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
            )
        }

        Button(
            onClick = onPrimaryActionClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = TutorialLayoutDefaults.primaryButtonMinHeight),
            shape = RoundedCornerShape(EumRadius.small),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = EumPrimary600,
                    contentColor = EumWhite,
                ),
        ) {
            Text(
                text = primaryActionLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TutorialHeader(uiState: TutorialUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = stringResource(id = uiState.step.titleRes),
            color = EumPrimary600,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = uiState.step.headlineRes),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.headlineSmall.lineHeight,
        )
        Text(
            text = stringResource(id = uiState.step.descriptionRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TutorialPhoneMockup(step: TutorialStep) {
    Box(
        modifier =
            Modifier
                .widthIn(max = TutorialLayoutDefaults.phoneMockupWidth)
                .height(TutorialLayoutDefaults.phoneMockupMaxHeight)
                .fillMaxWidth()
                .clip(RoundedCornerShape(TutorialLayoutDefaults.phoneOuterCorner))
                .background(TutorialLayoutDefaults.phoneOuterColor)
                .padding(TutorialLayoutDefaults.phoneFramePadding),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(TutorialLayoutDefaults.phoneInnerCorner),
            color = EumWhite,
        ) {
            when (step) {
                TutorialStep.DESTINATION -> DestinationMockup()
                TutorialStep.ROUTE_COMPARISON -> RouteComparisonMockup()
                TutorialStep.REPORT -> ReportMockup()
            }
        }
    }
}

@Composable
private fun DestinationMockup() {
    Box(modifier = Modifier.fillMaxSize()) {
        MapGridBackground()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(TutorialLayoutDefaults.screenContentPadding),
            verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.tightGap),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TutorialLayoutDefaults.panelCorner),
                color = EumWhite,
                shadowElevation = TutorialLayoutDefaults.lowElevation,
                border = BorderStroke(TutorialLayoutDefaults.hairlineWidth, EumBorderSubtle),
            ) {
                Column(
                    modifier = Modifier.padding(TutorialLayoutDefaults.panelPadding),
                    verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.contentGap),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.contentGap),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_search),
                            contentDescription = null,
                            modifier = Modifier.size(TutorialLayoutDefaults.iconSize),
                            tint = EumTextTertiary,
                        )
                        Text(
                            text = stringResource(id = R.string.tutorial_destination_search_hint),
                            modifier = Modifier.weight(1f),
                            color = EumTextTertiary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search_voice_mic),
                            contentDescription = null,
                            modifier = Modifier.size(TutorialLayoutDefaults.iconSize),
                            tint = EumPrimary600,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.chipGap)) {
                        TutorialChip(R.string.tutorial_filter_toilet)
                        TutorialChip(R.string.tutorial_filter_elevator)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.chipGap)) {
                        TutorialChip(R.string.tutorial_filter_parking)
                        TutorialChip(R.string.tutorial_filter_more)
                    }
                }
            }
        }

        OverlayHighlight(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(TutorialLayoutDefaults.destinationHighlightHeight)
                    .padding(
                        horizontal = TutorialLayoutDefaults.highlightHorizontalInset,
                        vertical = TutorialLayoutDefaults.destinationHighlightVerticalInset,
                    ),
        )

        CurrentLocationPin(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun RouteComparisonMockup() {
    Box(modifier = Modifier.fillMaxSize()) {
        MapGridBackground()
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(TutorialLayoutDefaults.routePanelOuterPadding),
            verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.contentGap),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(TutorialLayoutDefaults.panelCorner),
                color = EumWhite,
                shadowElevation = TutorialLayoutDefaults.mediumElevation,
            ) {
                Column(
                    modifier = Modifier.padding(TutorialLayoutDefaults.panelPadding),
                    verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.contentGap),
                ) {
                    TutorialRouteCard(
                        title = stringResource(id = R.string.tutorial_route_recommended),
                        time = stringResource(id = R.string.tutorial_route_recommended_time),
                        transfer = stringResource(id = R.string.tutorial_route_recommended_transfer),
                        selected = true,
                    )
                    TutorialRouteCard(
                        title = stringResource(id = R.string.tutorial_route_efficient),
                        time = stringResource(id = R.string.tutorial_route_efficient_time),
                        transfer = stringResource(id = R.string.tutorial_route_efficient_transfer),
                        selected = false,
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TutorialLayoutDefaults.buttonCorner),
                        color = EumPrimary600,
                    ) {
                        Text(
                            text = stringResource(id = R.string.tutorial_route_start_navigation),
                            modifier = Modifier.padding(vertical = TutorialLayoutDefaults.panelPadding),
                            color = EumWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        OverlayHighlight(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(TutorialLayoutDefaults.routeHighlightHeight)
                    .padding(
                        horizontal = TutorialLayoutDefaults.highlightHorizontalInset,
                        vertical = TutorialLayoutDefaults.routeHighlightVerticalInset,
                    ),
        )
    }
}

@Composable
private fun ReportMockup() {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.panelPadding),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.panelPadding)) {
                ReportCard(
                    labelRes = R.string.tutorial_report_tactile_block,
                    iconRes = R.drawable.ic_report_tactile_damage,
                    modifier = Modifier.weight(1f),
                )
                ReportCard(
                    labelRes = R.string.tutorial_report_guiding_block,
                    iconRes = R.drawable.ic_route_tactile_blocks,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.panelPadding)) {
                ReportCard(
                    labelRes = R.string.tutorial_report_facility_damage,
                    iconRes = R.drawable.ic_status_warning,
                    modifier = Modifier.weight(1f),
                )
                ReportCard(
                    labelRes = R.string.tutorial_report_other,
                    iconRes = R.drawable.ic_report_other,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(TutorialLayoutDefaults.bottomBarHeight)
                        .background(EumWhite)
                        .border(TutorialLayoutDefaults.hairlineWidth, EumBorderSubtle),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomTab(R.drawable.ic_nav_home, R.string.tutorial_tab_home)
                BottomTab(R.drawable.ic_nav_bookmark_outline, R.string.tutorial_tab_bookmark)
                BottomTab(R.drawable.ic_nav_report_selected, R.string.tutorial_tab_report, selected = true)
                BottomTab(R.drawable.ic_nav_mypage, R.string.tutorial_tab_my_page)
            }
            OverlayHighlight(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .width(TutorialLayoutDefaults.bottomHighlightWidth)
                        .height(TutorialLayoutDefaults.bottomHighlightHeight),
                cornerRadius = TutorialLayoutDefaults.panelCorner,
            )
        }
    }
}

@Composable
private fun MapGridBackground() {
    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .background(TutorialLayoutDefaults.mapBackgroundColor),
    ) {
        val step = TutorialLayoutDefaults.mapGridStep
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = EumWhite,
                start = Offset(x, 0f),
                end = Offset(x + size.height / 2f, size.height),
                strokeWidth = TutorialLayoutDefaults.mapMajorLineStroke,
            )
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = TutorialLayoutDefaults.mapGridLineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y + TutorialLayoutDefaults.mapLineOffset),
                strokeWidth = TutorialLayoutDefaults.mapMinorLineStroke,
            )
            y += step
        }
    }
}

@Composable
private fun TutorialChip(labelRes: Int) {
    Surface(
        shape = RoundedCornerShape(TutorialLayoutDefaults.buttonCorner),
        color = EumWhite,
        border = BorderStroke(TutorialLayoutDefaults.hairlineWidth, EumBorderSubtle),
    ) {
        Text(
            text = stringResource(id = labelRes),
            modifier = Modifier.padding(
                horizontal = TutorialLayoutDefaults.chipHorizontalPadding,
                vertical = TutorialLayoutDefaults.chipVerticalPadding,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TutorialRouteCard(
    title: String,
    time: String,
    transfer: String,
    selected: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TutorialLayoutDefaults.cardCorner),
        color = EumWhite,
        border =
            BorderStroke(
                width =
                    if (selected) {
                        TutorialLayoutDefaults.selectedStrokeWidth
                    } else {
                        TutorialLayoutDefaults.hairlineWidth
                    },
                color = if (selected) EumPrimary600 else EumBorderSubtle,
            ),
    ) {
        Column(
            modifier = Modifier.padding(TutorialLayoutDefaults.panelPadding),
            verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.routeCardGap),
        ) {
            Text(
                text = title,
                color = if (selected) EumPrimary600 else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.routeSummaryGap),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = transfer,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = EumTextMuted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.chipGap)) {
                TutorialBadge(R.string.tutorial_route_badge_elevator)
                TutorialBadge(
                    if (selected) {
                        R.string.tutorial_route_badge_access
                    } else {
                        R.string.tutorial_route_badge_no_step
                    },
                )
                TutorialBadge(
                    if (selected) {
                        R.string.tutorial_route_badge_plus_two
                    } else {
                        R.string.tutorial_route_badge_plus_one
                    },
                )
            }
        }
    }
}

@Composable
private fun TutorialBadge(labelRes: Int) {
    Surface(
        shape = RoundedCornerShape(TutorialLayoutDefaults.badgeCorner),
        color = EumSurfaceInfo,
        border = BorderStroke(TutorialLayoutDefaults.hairlineWidth, EumBorderInfo),
    ) {
        Text(
            text = stringResource(id = labelRes),
            modifier = Modifier.padding(
                horizontal = TutorialLayoutDefaults.badgeHorizontalPadding,
                vertical = TutorialLayoutDefaults.badgeVerticalPadding,
            ),
            color = EumPrimary600,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ReportCard(
    labelRes: Int,
    iconRes: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .heightIn(min = TutorialLayoutDefaults.reportCardMinHeight),
        shape = RoundedCornerShape(TutorialLayoutDefaults.cardCorner),
        colors = CardDefaults.cardColors(containerColor = EumWhite),
        border = BorderStroke(TutorialLayoutDefaults.hairlineWidth, EumBorderSubtle),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(TutorialLayoutDefaults.routePanelOuterPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(TutorialLayoutDefaults.reportIconSize),
                tint = if (iconRes == R.drawable.ic_report_other) Color.Unspecified else EumPrimary600,
            )
            Text(
                text = stringResource(id = labelRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun BottomTab(
    iconRes: Int,
    labelRes: Int,
    selected: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.microGap),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(TutorialLayoutDefaults.iconSize),
            tint = if (selected) EumPrimary600 else EumTextTertiary,
        )
        Text(
            text = stringResource(id = labelRes),
            color = if (selected) EumPrimary600 else EumTextTertiary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
        )
    }
}

@Composable
private fun OverlayHighlight(
    modifier: Modifier,
    cornerRadius: Dp = TutorialLayoutDefaults.highlightCorner,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .border(
                    width = TutorialLayoutDefaults.highlightStrokeWidth,
                    color = EumHighlightYellow,
                    shape = RoundedCornerShape(cornerRadius),
                ),
    )
}

@Composable
private fun CurrentLocationPin(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(TutorialLayoutDefaults.routePanelOuterPadding),
            color = EumPrimary600,
        ) {
            Text(
                text = stringResource(id = R.string.tutorial_current_location),
                modifier = Modifier.padding(
                    horizontal = TutorialLayoutDefaults.panelPadding,
                    vertical = TutorialLayoutDefaults.chipVerticalPadding,
                ),
                color = EumWhite,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Surface(
            modifier = Modifier.size(TutorialLayoutDefaults.pinSize),
            shape = CircleShape,
            color = EumPrimary600,
            border = BorderStroke(TutorialLayoutDefaults.pinBorderWidth, EumWhite),
        ) {}
    }
}

@Composable
private fun TutorialPagerIndicator(
    currentStep: Int,
    totalSteps: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.tightGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val selected = index + 1 == currentStep
            Box(
                modifier =
                    Modifier
                        .width(
                            if (selected) {
                                TutorialLayoutDefaults.indicatorSelectedWidth
                            } else {
                                TutorialLayoutDefaults.indicatorDefaultWidth
                            },
                        )
                        .height(TutorialLayoutDefaults.indicatorHeight)
                        .clip(RoundedCornerShape(TutorialLayoutDefaults.pillCorner))
                        .background(if (selected) EumPrimary600 else TutorialLayoutDefaults.indicatorTrackColor),
            )
        }
        Spacer(modifier = Modifier.width(TutorialLayoutDefaults.microGap))
        Text(
            text = stringResource(id = R.string.tutorial_progress_count, currentStep, totalSteps),
            color = EumTextTertiary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

internal object TutorialLayoutDefaults {
    const val totalStepCount: Int = TutorialStep.TOTAL_STEPS

    val primaryButtonMinHeight = 56.dp
    val phoneMockupWidth = 328.dp
    val phoneMockupMaxHeight = 456.dp
    val phoneOuterCorner = 32.dp
    val phoneInnerCorner = 26.dp
    val phoneFramePadding = 8.dp
    val phoneOuterColor = Color(0xFF171717)

    val panelPadding = 12.dp
    val screenContentPadding = 12.dp
    val routePanelOuterPadding = 14.dp
    val panelCorner = 18.dp
    val cardCorner = 12.dp
    val buttonCorner = 10.dp
    val badgeCorner = 8.dp

    val contentGap = 10.dp
    val tightGap = 8.dp
    val chipGap = 6.dp
    val microGap = 4.dp
    val routeCardGap = 7.dp
    val routeSummaryGap = 18.dp

    val hairlineWidth = 1.dp
    val selectedStrokeWidth = 2.dp
    val highlightStrokeWidth = 4.dp
    val highlightCorner = 22.dp
    val highlightHorizontalInset = 6.dp
    val destinationHighlightHeight = 132.dp
    val destinationHighlightVerticalInset = 42.dp
    val routeHighlightHeight = 250.dp
    val routeHighlightVerticalInset = 68.dp

    val lowElevation = 3.dp
    val mediumElevation = 4.dp

    val chipHorizontalPadding = 9.dp
    val chipVerticalPadding = 7.dp
    val badgeHorizontalPadding = 8.dp
    val badgeVerticalPadding = 5.dp

    val bottomBarHeight = 76.dp
    val bottomHighlightWidth = 76.dp
    val bottomHighlightHeight = 86.dp
    val reportCardMinHeight = 128.dp
    val reportIconSize = 34.dp
    val iconSize = 24.dp
    val pinSize = 24.dp
    val pinBorderWidth = 3.dp

    val indicatorSelectedWidth = 70.dp
    val indicatorDefaultWidth = 54.dp
    val indicatorHeight = 6.dp
    val pillCorner = 99.dp

    val mapBackgroundColor = Color(0xFFF0F6FD)
    val mapGridLineColor = Color(0xFFD9E8F8)
    val indicatorTrackColor = Color(0xFFD9DDE7)
    const val mapGridStep = 34f
    const val mapLineOffset = 40f
    const val mapMajorLineStroke = 3f
    const val mapMinorLineStroke = 2f
}
