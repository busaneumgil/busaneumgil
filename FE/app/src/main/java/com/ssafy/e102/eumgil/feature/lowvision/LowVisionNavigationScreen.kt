package com.ssafy.e102.eumgil.feature.lowvision

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionBottomNav
import com.ssafy.e102.eumgil.feature.navigation.NavigationScreenState
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiAction
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiState

private val LowVisionNavigationBackground = Color(0xFF0D0D0F)
private val LowVisionNavigationPanel = Color(0xFF202123)
private val LowVisionNavigationYellow = LowVisionScreenDefaults.brandYellow
private val LowVisionNavigationCoral = Color(0xFFFF8B78)
private val LowVisionNavigationInactive = Color(0xFFE7E7E7)
private val LowVisionNavigationDivider = Color(0xFF36363A)

internal object LowVisionNavigationLayoutDefaults {
    val contentTopPadding = 56.dp
    val contentBottomPadding = 18.dp
    val contentGap = 24.dp
    val metricHeaderHeight = 168.dp
    val metricLabelFontSize = 28.sp
    val metricLabelLineHeight = 34.sp
    val metricNumberFontSize = 76.sp
    val metricNumberLineHeight = 84.sp
    val metricUnitFontSize = 34.sp
    val metricUnitLineHeight = 40.sp
    val currentLocationIconSize = 74.dp
    val currentLocationIconTextGap = 10.dp
    val currentLocationVerticalPadding = 12.dp
    val currentLocationLabelFontSize = 44.sp
    val currentLocationLabelLineHeight = 50.sp
}

internal data class LowVisionNavigationMetricSection(
    val label: String,
    val metricIndex: Int,
) {
    fun talkBackText(value: String): String = "$label $value"
}

internal data class LowVisionNavigationActionCard(
    val label: String,
    @DrawableRes val iconRes: Int,
)

internal fun lowVisionNavigationMetricSections(): List<LowVisionNavigationMetricSection> =
    listOf(
        LowVisionNavigationMetricSection(label = "\uB0A8\uC740 \uAC70\uB9AC", metricIndex = 0),
        LowVisionNavigationMetricSection(label = "\uB0A8\uC740 \uC2DC\uAC04", metricIndex = 1),
    )

internal fun lowVisionNavigationActionCards(): List<LowVisionNavigationActionCard> =
    listOf(
        LowVisionNavigationActionCard(
            label = "\uD604\uC7AC \uC704\uCE58",
            iconRes = R.drawable.ic_voice_location_pin,
        ),
        LowVisionNavigationActionCard(
            label = "\uC548\uB0B4 \uC885\uB8CC",
            iconRes = R.drawable.ic_action_close,
        ),
    )

internal fun lowVisionNavigationBottomTabs(): List<LowVisionBottomTab> =
    listOf(
        LowVisionBottomTab.HOME,
        LowVisionBottomTab.BOOKMARK,
        LowVisionBottomTab.CATEGORY,
        LowVisionBottomTab.MY_PAGE,
    )

internal const val LOW_VISION_NAVIGATION_LOAD_ERROR_MESSAGE: String = "길 안내를 불러오지 못했습니다."

internal fun shouldShowLowVisionNavigationLoadError(
    uiState: NavigationUiState,
    loadErrorMessage: String?,
): Boolean = uiState.screenState == NavigationScreenState.Loading && !loadErrorMessage.isNullOrBlank()

internal fun lowVisionNavigationDisplayMetric(
    section: LowVisionNavigationMetricSection,
    rawValue: String,
): String {
    val trimmedValue = rawValue.trim()
    if (trimmedValue.isBlank() || trimmedValue == "-") return "-"

    return when (section.metricIndex) {
        0 -> trimmedValue.asDistanceLabel()
        1 -> trimmedValue.asMinuteLabel()
        else -> trimmedValue
    }
}

private data class LowVisionMetricValueParts(
    val number: String,
    val unit: String,
)

private val metricNumberRegex = Regex("""\d+(?:\.\d+)?""")

private fun String.asDistanceLabel(): String {
    if (endsWith("km", ignoreCase = true)) {
        return metricNumberRegex.find(this)?.value?.let { number -> "${number}km" } ?: this
    }

    val meters = metricNumberRegex.find(this)?.value?.toDoubleOrNull() ?: return this
    return if (meters >= 1000.0) {
        "${(meters / 1000.0).toKilometerText()}km"
    } else {
        "${meters.toInt()}m"
    }
}

private fun String.asMinuteLabel(): String {
    if (endsWith("\uBD84")) return this

    val minutes = metricNumberRegex.find(this)?.value?.toDoubleOrNull() ?: return this
    return "${minutes.toInt()}\uBD84"
}

private fun Double.toKilometerText(): String {
    val tenths = kotlin.math.round(this * 10).toInt()
    val whole = tenths / 10
    val fraction = tenths % 10
    return if (fraction == 0) {
        whole.toString()
    } else {
        "$whole.$fraction"
    }
}

private fun lowVisionNavigationMetricValueParts(value: String): LowVisionMetricValueParts {
    val unit =
        when {
            value.endsWith("km", ignoreCase = true) -> "km"
            value.endsWith("m", ignoreCase = true) -> "m"
            value.endsWith("\uBD84") -> "\uBD84"
            else -> ""
        }

    return if (unit.isBlank()) {
        LowVisionMetricValueParts(number = value, unit = "")
    } else {
        LowVisionMetricValueParts(number = value.removeSuffix(unit), unit = unit)
    }
}

@Composable
fun LowVisionNavigationScreen(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
    modifier: Modifier = Modifier,
    onTabSelected: (LowVisionBottomTab) -> Unit = {},
    loadErrorMessage: String? = null,
    currentLocationAddress: String? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(LowVisionNavigationBackground),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(
                        top = LowVisionNavigationLayoutDefaults.contentTopPadding,
                        bottom = LowVisionNavigationLayoutDefaults.contentBottomPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(LowVisionNavigationLayoutDefaults.contentGap),
        ) {
            if (shouldShowLowVisionNavigationLoadError(uiState, loadErrorMessage)) {
                LowVisionNavigationLoadError(
                    message = loadErrorMessage.orEmpty(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )
            } else {
                LowVisionNavigationMetricHeader(
                    uiState = uiState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(LowVisionNavigationLayoutDefaults.metricHeaderHeight),
                )

                LowVisionCurrentLocationCard(
                    card = lowVisionNavigationActionCards().first(),
                    display =
                        lowVisionCurrentLocationDisplay(
                            coordinate = uiState.mapOverlay.currentLocation?.coordinate,
                            address = currentLocationAddress,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )

                LowVisionExitNavigationCard(
                    card = lowVisionNavigationActionCards()[1],
                    enabled = uiState.isExitEnabled,
                    onClick = { onAction(NavigationUiAction.ExitNavigationClicked) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1.55f),
                )
            }
        }

        LowVisionBottomNav(
            selectedTab = LowVisionBottomTab.HOME,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun LowVisionNavigationLoadError(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .clearAndSetSemantics {
                    contentDescription = message
                },
        shape = RoundedCornerShape(18.dp),
        color = LowVisionNavigationPanel,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message,
                color = LowVisionNavigationYellow,
                fontSize = 56.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LowVisionNavigationMetricHeader(
    uiState: NavigationUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        lowVisionNavigationMetricSections().forEachIndexed { index, section ->
            val rawValue = uiState.stepCard.metrics.getOrNull(section.metricIndex)?.value.orEmpty()
            val value = lowVisionNavigationDisplayMetric(section, rawValue)
            LowVisionNavigationMetricItem(
                section = section,
                value = value,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
            )
            if (index == 0) {
                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .height(132.dp)
                            .background(LowVisionNavigationDivider),
                )
            }
        }
    }
}

@Composable
private fun LowVisionNavigationMetricItem(
    section: LowVisionNavigationMetricSection,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clearAndSetSemantics {
                    contentDescription = section.talkBackText(value)
                }
                .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = section.label,
            color = Color.White,
            fontSize = LowVisionNavigationLayoutDefaults.metricLabelFontSize,
            lineHeight = LowVisionNavigationLayoutDefaults.metricLabelLineHeight,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        LowVisionNavigationMetricValue(
            value = value,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun LowVisionNavigationMetricValue(
    value: String,
    modifier: Modifier = Modifier,
) {
    val parts = lowVisionNavigationMetricValueParts(value)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = parts.number,
            color = LowVisionNavigationYellow,
            fontSize = LowVisionNavigationLayoutDefaults.metricNumberFontSize,
            lineHeight = LowVisionNavigationLayoutDefaults.metricNumberLineHeight,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        if (parts.unit.isNotBlank()) {
            Text(
                text = parts.unit,
                color = LowVisionNavigationYellow,
                fontSize = LowVisionNavigationLayoutDefaults.metricUnitFontSize,
                lineHeight = LowVisionNavigationLayoutDefaults.metricUnitLineHeight,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(start = 3.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun LowVisionCurrentLocationCard(
    card: LowVisionNavigationActionCard,
    display: LowVisionCurrentLocationDisplay,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = LowVisionNavigationYellow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {
                        contentDescription = display.talkBackText
                    }
                    .padding(
                        horizontal = 24.dp,
                        vertical = LowVisionNavigationLayoutDefaults.currentLocationVerticalPadding,
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = card.iconRes),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(LowVisionNavigationLayoutDefaults.currentLocationIconSize),
            )
            Spacer(modifier = Modifier.height(LowVisionNavigationLayoutDefaults.currentLocationIconTextGap))
            Text(
                text = display.title,
                color = Color.Black,
                fontSize = LowVisionNavigationLayoutDefaults.currentLocationLabelFontSize,
                lineHeight = LowVisionNavigationLayoutDefaults.currentLocationLabelLineHeight,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = display.supportingText,
                color = Color.Black,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LowVisionExitNavigationCard(
    card: LowVisionNavigationActionCard,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = if (enabled) 1f else 0.55f

    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(18.dp))
                .lowVisionButtonSemantics(
                    label = card.label,
                    actionHint = "\uB450 \uBC88 \uD0ED\uD558\uBA74 \uAE38 \uC548\uB0B4\uB97C \uC885\uB8CC\uD569\uB2C8\uB2E4.",
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = LowVisionNavigationPanel,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = LowVisionNavigationCoral.copy(alpha = contentAlpha),
            ) {
                Box(
                    modifier = Modifier.size(112.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = card.iconRes),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(58.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(38.dp))
            Text(
                text = card.label,
                color = LowVisionNavigationInactive.copy(alpha = contentAlpha),
                fontSize = 60.sp,
                lineHeight = 68.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
