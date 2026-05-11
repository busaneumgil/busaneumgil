package com.ssafy.e102.eumgil.feature.map.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Composable
fun MapBottomSheetSurface(
    modifier: Modifier = Modifier,
    showHandle: Boolean = true,
    handleModifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedContainerColor =
        if (containerColor == Color.Unspecified) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.995f)
        } else {
            containerColor
        }

    Surface(
        modifier = modifier,
        shape =
            RoundedCornerShape(
                topStart = EumRadius.large,
                topEnd = EumRadius.large,
            ),
        color = resolvedContainerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            if (showHandle) {
                Box(
                    modifier = Modifier.fillMaxWidth().then(handleModifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .width(42.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(EumRadius.full))
                                .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }

            content()
        }
    }
}
