package com.ssafy.e102.eumgil.core.designsystem.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R

val BusanEumgilFontFamily = FontFamily(
    Font(R.font.koddi_ud_on_gothic_regular, FontWeight.Normal),
    Font(R.font.koddi_ud_on_gothic_regular, FontWeight.Medium),
    Font(R.font.koddi_ud_on_gothic_bold, FontWeight.SemiBold),
    Font(R.font.koddi_ud_on_gothic_bold, FontWeight.Bold),
    Font(R.font.koddi_ud_on_gothic_extra_bold, FontWeight.ExtraBold),
)

private fun busanEumgilTextStyle(
    fontWeight: FontWeight,
    fontSize: Int,
    lineHeight: Int,
) = TextStyle(
    fontFamily = BusanEumgilFontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
)

val BusanEumgilTypography = Typography(
    displayLarge = busanEumgilTextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32,
        lineHeight = 40,
    ),
    headlineMedium = busanEumgilTextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28,
        lineHeight = 36,
    ),
    titleLarge = busanEumgilTextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22,
        lineHeight = 30,
    ),
    titleMedium = busanEumgilTextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18,
        lineHeight = 26,
    ),
    bodyLarge = busanEumgilTextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16,
        lineHeight = 24,
    ),
    bodyMedium = busanEumgilTextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14,
        lineHeight = 22,
    ),
    labelLarge = busanEumgilTextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14,
        lineHeight = 20,
    ),
)
