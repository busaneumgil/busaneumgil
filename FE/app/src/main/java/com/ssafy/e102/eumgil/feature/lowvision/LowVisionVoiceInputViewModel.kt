package com.ssafy.e102.eumgil.feature.lowvision

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.stt.AudioRecorder
import com.ssafy.e102.eumgil.core.stt.SherpaManager
import com.ssafy.e102.eumgil.core.stt.SttManager
import com.ssafy.e102.eumgil.core.stt.VadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LowVisionVoiceInputEvent {
    /** VAD + STT 파이프라인 완료: [query]를 검색어로 결과 화면으로 이동. */
    data class RecordingCompleted(val query: String) : LowVisionVoiceInputEvent

    /** 사용자 취소 또는 오류: 홈 화면으로 복귀. */
    data object RecordingCancelled : LowVisionVoiceInputEvent
}

/**
 * 시각지원 음성 입력 화면 ViewModel.
 *
 * 화면 진입 즉시 모델 초기화 → VAD 기반 녹음 → SenseVoice STT 파이프라인을
 * 자동으로 실행한다.
 *
 * 이벤트:
 *  - [LowVisionVoiceInputEvent.RecordingCompleted]: STT 결과 텍스트와 함께 발행
 *  - [LowVisionVoiceInputEvent.RecordingCancelled]: 사용자 취소 / 발화 없음 / 오류
 */
class LowVisionVoiceInputViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "LowVisionVoiceInputVM"
        private const val SILENCE_FRAMES_FOR_STOP = 30
    }

    private val _uiState = MutableStateFlow(LowVisionVoiceInputUiState())
    val uiState: StateFlow<LowVisionVoiceInputUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<LowVisionVoiceInputEvent>(Channel.BUFFERED)
    val uiEvent: Flow<LowVisionVoiceInputEvent> = _uiEvent.receiveAsFlow()

    private val audioRecorder = AudioRecorder()
    private var vadManager: VadManager? = null
    private var sttManager: SttManager? = null
    private var recordingJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                SherpaManager.ensureModelsExtracted(context)

                if (!SherpaManager.modelsExist(context)) {
                    Log.e(TAG, "모델 파일 없음 — 음성 입력 취소")
                    _uiEvent.send(LowVisionVoiceInputEvent.RecordingCancelled)
                    return@launch
                }

                vadManager = VadManager(context)
                sttManager = SttManager.getInstance(context)

                startRecording()
            } catch (e: Exception) {
                Log.e(TAG, "초기화 실패: ${e.message}", e)
                _uiEvent.send(LowVisionVoiceInputEvent.RecordingCancelled)
            }
        }
    }

    /** 화면 탭 → 녹음 즉시 취소하고 홈으로 복귀. */
    fun cancelRecording() {
        audioRecorder.stop()
        recordingJob?.cancel()
        viewModelScope.launch {
            _uiEvent.send(LowVisionVoiceInputEvent.RecordingCancelled)
        }
    }

    private fun startRecording() {
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                var voiceDetectedEver = false
                var silenceFrameCount = 0
                val accumulatedSamples = mutableListOf<Float>()
                var skipStt = false

                Log.d(TAG, "=== 녹음 시작 ===")

                audioRecorder.startRecording().collect { floatSamples ->
                    vadManager?.acceptWaveform(floatSamples)

                    var hadSegment = false
                    while (vadManager?.isEmpty() == false) {
                        val segment = vadManager?.front() ?: break
                        vadManager?.popSegment()
                        accumulatedSamples.addAll(segment.samples.toList())
                        hadSegment = true
                    }

                    val currentlySpeaking = vadManager?.isSpeechDetected() ?: false

                    when {
                        hadSegment -> {
                            if (!voiceDetectedEver) Log.d(TAG, ">>> 발화 감지 시작")
                            voiceDetectedEver = true
                            silenceFrameCount = 0
                        }
                        currentlySpeaking -> {
                            voiceDetectedEver = true
                            silenceFrameCount = 0
                        }
                        else -> {
                            silenceFrameCount++
                        }
                    }

                    when {
                        voiceDetectedEver && silenceFrameCount >= SILENCE_FRAMES_FOR_STOP -> {
                            Log.d(TAG, "=== 무음 지속 → STT 준비 ===")
                            audioRecorder.stop()
                        }
                        !voiceDetectedEver && silenceFrameCount >= SILENCE_FRAMES_FOR_STOP * 2 -> {
                            Log.d(TAG, "=== 발화 없음 타임아웃 → 취소 ===")
                            skipStt = true
                            audioRecorder.stop()
                        }
                    }
                }

                // flush 후 잔여 세그먼트 수집
                vadManager?.flush()
                while (vadManager?.isEmpty() == false) {
                    val segment = vadManager?.front() ?: break
                    vadManager?.popSegment()
                    accumulatedSamples.addAll(segment.samples.toList())
                }

                if (!skipStt && voiceDetectedEver && accumulatedSamples.isNotEmpty()) {
                    Log.d(TAG, "=== STT 추론 시작 (${accumulatedSamples.size} samples) ===")
                    val text = sttManager?.recognize(accumulatedSamples.toFloatArray()).orEmpty()
                    Log.d(TAG, "=== STT 완료: '$text' ===")
                    if (text.isBlank()) {
                        _uiEvent.send(LowVisionVoiceInputEvent.RecordingCancelled)
                    } else {
                        _uiEvent.send(LowVisionVoiceInputEvent.RecordingCompleted(query = text))
                    }
                } else {
                    Log.d(TAG, "발화 없음 또는 취소 — 홈으로 복귀")
                    _uiEvent.send(LowVisionVoiceInputEvent.RecordingCancelled)
                }
            } catch (e: Exception) {
                Log.e(TAG, "녹음 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiEvent.send(LowVisionVoiceInputEvent.RecordingCancelled)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stop()
        recordingJob?.cancel()
        vadManager?.release()
    }
}
