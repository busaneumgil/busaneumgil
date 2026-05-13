package com.ssafy.e102.eumgil.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R

val PretendardFontFamily =
    FontFamily(
        Font(R.font.pretendard_regular, FontWeight.Normal),
        Font(R.font.pretendard_medium, FontWeight.Medium),
        Font(R.font.pretendard_semibold, FontWeight.SemiBold),
        Font(R.font.pretendard_bold, FontWeight.Bold),
    )

private fun pretendardTextStyle(
    fontWeight: FontWeight,
    fontSize: Int,
    lineHeight: Int,
) = TextStyle(
    fontFamily = PretendardFontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
)

val PretendardTypography =
    Typography(
        displayLarge = pretendardTextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 32,
            lineHeight = 40,
        ),
        headlineLarge = pretendardTextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 30,
            lineHeight = 38,
        ),
        headlineMedium = pretendardTextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 28,
            lineHeight = 36,
        ),
        headlineSmall = pretendardTextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 24,
            lineHeight = 32,
        ),
        titleLarge = pretendardTextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 22,
            lineHeight = 30,
        ),
        titleMedium = pretendardTextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 18,
            lineHeight = 26,
        ),
        titleSmall = pretendardTextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16,
            lineHeight = 24,
        ),
        bodyLarge = pretendardTextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16,
            lineHeight = 24,
        ),
        bodyMedium = pretendardTextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14,
            lineHeight = 22,
        ),
        bodySmall = pretendardTextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12,
            lineHeight = 20,
        ),
        labelLarge = pretendardTextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14,
            lineHeight = 20,
        ),
        labelMedium = pretendardTextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 12,
            lineHeight = 18,
        ),
        labelSmall = pretendardTextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 11,
            lineHeight = 16,
        ),
    )
