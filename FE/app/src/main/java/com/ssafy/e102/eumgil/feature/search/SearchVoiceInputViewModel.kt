package com.ssafy.e102.eumgil.feature.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeIntent
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeMode
import com.ssafy.e102.eumgil.core.stt.AudioRecorder
import com.ssafy.e102.eumgil.core.stt.SherpaManager
import com.ssafy.e102.eumgil.core.stt.SttManager
import com.ssafy.e102.eumgil.core.stt.VadManager
import com.ssafy.e102.eumgil.data.repository.VoiceAnalyzeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SearchVoiceInputEvent {
    data class TranscriptReady(val text: String) : SearchVoiceInputEvent
    data object TranscriptEmpty : SearchVoiceInputEvent
    data class SpeakError(val text: String) : SearchVoiceInputEvent

    /** TTS "말씀해 주세요" 재생 요청 — Route가 TTS 완료 후 [beginRecording]을 호출한다. */
    data object ReadyToRecord : SearchVoiceInputEvent
}

/**
 * 일반 사용자 음성 검색 입력 ViewModel.
 *
 * [startListening] 호출 시 AudioRecorder + VadManager + SttManager 파이프라인을 실행한다.
 * [stopListening] 호출 시 녹음을 즉시 중단하고 job을 취소한다 — 이벤트는 발행하지 않는다.
 * (뒤로 이동은 SearchViewModel이 NavigateBack 이벤트로 처리)
 *
 * STT 완료 → [SearchVoiceInputEvent.TranscriptReady]
 * 발화 없음 / 빈 결과 → [SearchVoiceInputEvent.TranscriptEmpty]
 */
class SearchVoiceInputViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SearchVoiceInputVM"
        private const val SILENCE_FRAMES_FOR_STOP = 30
    }

    private val voiceAnalyzeRepository: VoiceAnalyzeRepository by lazy {
        (getApplication<Application>() as BusanEumgilApp).appContainer.voiceAnalyzeRepository
    }

    private val _uiEvent = Channel<SearchVoiceInputEvent>(Channel.BUFFERED)
    val uiEvent: Flow<SearchVoiceInputEvent> = _uiEvent.receiveAsFlow()

    private val audioRecorder = AudioRecorder()
    private var vadManager: VadManager? = null
    private var sttManager: SttManager? = null
    private var listeningJob: Job? = null

    /**
     * 모델 초기화 후 [SearchVoiceInputEvent.ReadyToRecord]를 발행한다.
     * Route가 TTS "말씀해 주세요" 완료 후 [beginRecording]을 호출한다.
     */
    fun startListening() {
        if (listeningJob?.isActive == true) return
        listeningJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                SherpaManager.ensureModelsExtracted(context)

                if (!SherpaManager.modelsExist(context)) {
                    Log.e(TAG, "모델 파일 없음 — 음성 입력 취소")
                    _uiEvent.send(SearchVoiceInputEvent.TranscriptEmpty)
                    return@launch
                }

                if (vadManager == null) vadManager = VadManager(context)
                if (sttManager == null) sttManager = SttManager.getInstance(context)

                _uiEvent.send(SearchVoiceInputEvent.ReadyToRecord)
            } catch (e: Exception) {
                Log.e(TAG, "초기화 실패: ${e.message}", e)
                _uiEvent.send(SearchVoiceInputEvent.TranscriptEmpty)
            }
        }
    }

    /**
     * 실제 VAD+STT 파이프라인을 시작한다.
     * Route에서 TTS 완료 후 호출한다.
     */
    fun beginRecording() {
        if (listeningJob?.isActive == true) return
        listeningJob = viewModelScope.launch(Dispatchers.IO) {
            runPipeline()
        }
    }

    /** 진행 중인 녹음을 중단한다. 이벤트를 발행하지 않으므로 호출부가 직접 뒤로 이동해야 한다. */
    fun stopListening() {
        audioRecorder.stop()
        listeningJob?.cancel()
    }

    private suspend fun runPipeline() {
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
                    _uiEvent.send(SearchVoiceInputEvent.TranscriptEmpty)
                } else {
                    dispatchAnalyze(text)
                }
            } else {
                Log.d(TAG, "발화 없음 또는 취소 — 뒤로 이동")
                _uiEvent.send(SearchVoiceInputEvent.TranscriptEmpty)
            }
        } catch (e: Exception) {
            Log.e(TAG, "녹음 오류: ${e.message}", e)
            withContext(Dispatchers.Main) {
                _uiEvent.send(SearchVoiceInputEvent.TranscriptEmpty)
            }
        }
    }

    private suspend fun dispatchAnalyze(sttText: String) {
        try {
            Log.d(TAG, "=== 음성 분석 요청: '$sttText' ===")
            val result = voiceAnalyzeRepository.analyze(
                text = sttText,
                mode = VoiceAnalyzeMode.MOBILITY_IMPAIRED,
            )
            Log.d(TAG, "=== 음성 분석 완료: intent=${result.intent}, placeName=${result.placeName} ===")
            if (result.intent == VoiceAnalyzeIntent.PLACE_SEARCH && !result.placeName.isNullOrBlank()) {
                _uiEvent.send(SearchVoiceInputEvent.TranscriptReady(text = result.placeName))
            } else {
                Log.e(TAG, "음성 분석 API 호출 실패: intent=${result.intent}, placeName=${result.placeName}")
                _uiEvent.send(SearchVoiceInputEvent.SpeakError(getApplication<Application>().getString(R.string.voice_input_retry)))
                _uiEvent.send(SearchVoiceInputEvent.TranscriptEmpty)
            }
        } catch (e: Exception) {
            Log.e(TAG, "음성 분석 API 호출 실패: ${e.message}")
            _uiEvent.send(SearchVoiceInputEvent.SpeakError(getApplication<Application>().getString(R.string.voice_input_retry)))
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stop()
        listeningJob?.cancel()
        vadManager?.release()
        Log.d(TAG, "SearchVoiceInputViewModel cleared")
    }
}
