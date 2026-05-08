package com.ssafy.e102.eumgil.feature.lowvision

import androidx.annotation.FontRes
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ssafy.e102.eumgil.R

internal data class LowVisionFontResource(
    @FontRes val fontRes: Int,
    val weight: FontWeight,
)

internal fun lowVisionFontResources(): List<LowVisionFontResource> =
    listOf(
        LowVisionFontResource(R.font.koddi_ud_on_gothic_regular, FontWeight.Normal),
        LowVisionFontResource(R.font.koddi_ud_on_gothic_bold, FontWeight.Bold),
        LowVisionFontResource(R.font.koddi_ud_on_gothic_extra_bold, FontWeight.ExtraBold),
        LowVisionFontResource(R.font.koddi_ud_on_gothic_extra_bold, FontWeight.Black),
    )

internal val LowVisionFontFamily =
    FontFamily(
        lowVisionFontResources().map { fontResource ->
            Font(
                resId = fontResource.fontRes,
                weight = fontResource.weight,
            )
        },
    )

@Composable
internal fun LowVisionFontTheme(content: @Composable () -> Unit) {
    ProvideTextStyle(
        value = LocalTextStyle.current.merge(TextStyle(fontFamily = LowVisionFontFamily)),
        content = content,
    )
}
