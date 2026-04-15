package com.ssafy.e102.eumgil.feature.map.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Composable
fun MapShellScaffold(
    mapContent: @Composable BoxScope.() -> Unit,
    topOverlay: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
        ) {
            mapContent()
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.small),
        ) {
            topOverlay()
        }
    }
}
