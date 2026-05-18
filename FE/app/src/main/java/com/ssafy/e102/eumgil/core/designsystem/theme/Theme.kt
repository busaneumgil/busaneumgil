package com.ssafy.e102.eumgil.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ssafy.e102.eumgil.core.model.TextSizePreference

@Composable
fun BusanEumgilTheme(
    textSizePreference: TextSizePreference = TextSizePreference.DEFAULT,
    content: @Composable () -> Unit,
) {
    BusanEumgilTheme(
        textSizeScale = textSizePreference.scale,
        content = content,
    )
}

@Composable
fun BusanEumgilTheme(
    textSizeScale: Float,
    content: @Composable () -> Unit,
) {
    val typography = remember(textSizeScale) {
        PretendardTypography.scaledBy(textSizeScale)
    }

    MaterialTheme(
        colorScheme = BusanEumgilLightColorScheme,
        typography = typography,
        content = content,
    )
}
