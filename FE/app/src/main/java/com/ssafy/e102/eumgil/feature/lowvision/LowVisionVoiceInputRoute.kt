package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Route wrapper for [LowVisionVoiceInputScreen].
 *
 * [LowVisionVoiceInputViewModel]을 생성·연결하고, VAD+STT 파이프라인 결과를
 * NavGraph 콜백([onRecordingCompleted] / [onCancelRecording])으로 위임한다.
 */
@Composable
fun LowVisionVoiceInputRoute(
    onCancelRecording: () -> Unit,
    onRecordingCompleted: (String) -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LowVisionVoiceInputViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is LowVisionVoiceInputEvent.RecordingCompleted -> onRecordingCompleted(event.query)
                LowVisionVoiceInputEvent.RecordingCancelled -> onCancelRecording()
            }
        }
    }

    LowVisionVoiceInputScreen(
        uiState = uiState,
        onCancelRecording = viewModel::cancelRecording,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}
