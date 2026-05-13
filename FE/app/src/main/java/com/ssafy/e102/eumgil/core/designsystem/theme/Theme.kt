package com.ssafy.e102.eumgil.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun BusanEumgilTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BusanEumgilLightColorScheme,
        typography = PretendardTypography,
        content = content,
    )
}
