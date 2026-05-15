package com.ssafy.e102.eumgil.feature.lowvision

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeHistoryItem
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeIntent
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeMode
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeResult
import com.ssafy.e102.eumgil.core.stt.AudioRecorder
import com.ssafy.e102.eumgil.core.stt.SherpaManager
import com.ssafy.e102.eumgil.core.stt.SttManager
import com.ssafy.e102.eumgil.core.stt.VadManager
import com.ssafy.e102.eumgil.data.repository.VoiceAnalyzeRepository
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
import org.json.JSONObject

sealed interface LowVisionVoiceInputEvent {
    /** VAD + STT 파이프라인 완료: [query]를 검색어로 결과 화면으로 이동. */
    data class RecordingCompleted(val query: String) : LowVisionVoiceInputEvent

    /** 사용자 취소 또는 오류: 홈 화면으로 복귀. */
    data object RecordingCancelled : LowVisionVoiceInputEvent

    /** TTS "말씀해 주세요" 재생 요청 — Route가 TTS 완료 후 [beginRecording]을 호출한다. */
    data object ReadyToRecord : LowVisionVoiceInputEvent
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
        private const val SILENCE_FRAMES_FOR_STOP = 20 // before: 30
        private const val ROLE_USER = "user"
        private const val ROLE_ASSISTANT = "assistant"
    }

    private val voiceAnalyzeRepository: VoiceAnalyzeRepository by lazy {
        (getApplication<Application>() as BusanEumgilApp).appContainer.voiceAnalyzeRepository
    }

    private val _uiState = MutableStateFlow(LowVisionVoiceInputUiState())
    val uiState: StateFlow<LowVisionVoiceInputUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<LowVisionVoiceInputEvent>(Channel.BUFFERED)
    val uiEvent: Flow<LowVisionVoiceInputEvent> = _uiEvent.receiveAsFlow()

    private val audioRecorder = AudioRecorder(getApplication())
    private var vadManager: VadManager? = null
    private var sttManager: SttManager? = null
    private var recordingJob: Job? = null

    /** 멀티턴 대화 히스토리 (user/assistant 교번 구조). */
    private val conversationHistory = mutableListOf<VoiceAnalyzeHistoryItem>()

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

    /**
     * TTS "말씀해 주세요" 재생을 Route에 요청한다.
     * Route가 TTS 완료를 감지하면 [beginRecording]을 호출한다.
     */
    private suspend fun startRecording() {
        _uiEvent.send(LowVisionVoiceInputEvent.ReadyToRecord)
    }

    /**
     * 실제 VAD+STT 파이프라인을 시작한다.
     * Route에서 TTS 완료 후 호출한다.
     */
    fun beginRecording() {
        if (recordingJob?.isActive == true) return
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
                        handleSttResult(text)
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

    /**
     * STT 결과를 AI 분석 API에 전달하고 멀티턴 대화를 진행한다.
     *
     * - confirmed == null && confirmationMessage != null → TTS 확인 요청, 다음 발화 대기
     * - confirmed == true → [LowVisionVoiceInputEvent.RecordingCompleted] 발행
     * - confirmed == false / intent == UNKNOWN → 히스토리 초기화 후 재녹음
     */
    private suspend fun handleSttResult(sttText: String) {
        // 사용자 발화를 히스토리에 추가
        conversationHistory.add(VoiceAnalyzeHistoryItem(role = ROLE_USER, content = sttText))

        try {
            Log.d(TAG, "=== 음성 분석 요청 (history=${conversationHistory.size}턴): '$sttText' ===")
            val result = voiceAnalyzeRepository.analyze(
                text = sttText,
                mode = VoiceAnalyzeMode.LOW_VISION,
                history = conversationHistory.toList(),
            )
            Log.d(TAG, "=== 음성 분석 완료: intent=${result.intent}, confirmed=${result.confirmed}, placeName=${result.placeName} ===")

            // 어시스턴트 응답을 JSON 직렬화하여 히스토리에 추가
            conversationHistory.add(
                VoiceAnalyzeHistoryItem(
                    role = ROLE_ASSISTANT,
                    content = result.toJsonString(),
                ),
            )

            when {
                result.intent == VoiceAnalyzeIntent.PLACE_SEARCH && result.confirmed == true -> {
                    // 사용자 확인 완료 → 검색 결과 화면으로
                    val placeName = result.placeName.orEmpty()
                    Log.d(TAG, "=== 확인 완료 → '$placeName' 검색 ===")
                    _uiEvent.send(LowVisionVoiceInputEvent.RecordingCompleted(query = placeName))
                }

                result.intent == VoiceAnalyzeIntent.PLACE_SEARCH && result.confirmed == null && !result.confirmationMessage.isNullOrBlank() -> {
                    // AI 확인 요청 → TTS 메시지 표시 후 다음 발화 대기
                    Log.d(TAG, "=== 확인 요청: '${result.confirmationMessage}' ===")
                    _uiState.value = _uiState.value.copy(
                        confirmationMessage = result.confirmationMessage,
                        ttsNonce = _uiState.value.ttsNonce + 1,
                    )
                    startRecording()
                }

                result.intent == VoiceAnalyzeIntent.PLACE_SEARCH && result.confirmed == false -> {
                    // 장소 부정 → 새 장소로 전환, history 유지 + TTS 대기 (confirmed=null 브랜치와 동일)
                    Log.d(TAG, "=== 장소 부정 → 새 장소 탐색 (confirmationMessage=${result.confirmationMessage}) ===")
                    _uiState.value = _uiState.value.copy(
                        confirmationMessage = result.confirmationMessage,
                        ttsNonce = _uiState.value.ttsNonce + 1,
                    )
                    startRecording()
                }

                result.intent == VoiceAnalyzeIntent.UNKNOWN -> {
                    // confirmed 값 무관 — 의도 파악 실패 → 히스토리 초기화 후 재녹음
                    Log.d(TAG, "=== 의도 미인식 → 히스토리 초기화 후 재녹음 ===")
                    conversationHistory.clear()
                    if (!result.confirmationMessage.isNullOrBlank()) {
                        _uiState.value = _uiState.value.copy(
                            confirmationMessage = result.confirmationMessage,
                            ttsNonce = _uiState.value.ttsNonce + 1,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(confirmationMessage = null)
                    }
                    startRecording()
                }

                else -> {
                    // 예외 케이스 → 재녹음
                    Log.d(TAG, "=== 예외 케이스 → 재녹음 ===")
                    conversationHistory.clear()
                    _uiState.value = _uiState.value.copy(confirmationMessage = null)
                    startRecording()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "음성 분석 실패 — 재녹음: ${e.message}", e)
            // 분석 실패 시 히스토리 초기화 후 재녹음
            conversationHistory.clear()
            _uiState.value = _uiState.value.copy(confirmationMessage = null)
            withContext(Dispatchers.Main) { startRecording() }
        }
    }

    /** [VoiceAnalyzeResult]를 히스토리용 JSON 문자열로 직렬화한다. */
    private fun VoiceAnalyzeResult.toJsonString(): String =
        JSONObject().apply {
            put("intent", intent.name)
            if (placeName != null) put("placeName", placeName) else put("placeName", JSONObject.NULL)
            if (confirmed != null) put("confirmed", confirmed) else put("confirmed", JSONObject.NULL)
            if (confirmationMessage != null) put("confirmationMessage", confirmationMessage) else put("confirmationMessage", JSONObject.NULL)
        }.toString()

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stop()
        recordingJob?.cancel()
        vadManager?.release()
    }
}
