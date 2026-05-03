package com.test.sherpatest

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.test.sherpatest.audio.AudioRecorder
import com.test.sherpatest.metrics.MetricsExporter
import com.test.sherpatest.metrics.WerCalculator
import com.test.sherpatest.model.MeasurementRecord
import com.test.sherpatest.model.SttResult
import com.test.sherpatest.sherpa.SherpaManager
import com.test.sherpatest.sherpa.SttManager
import com.test.sherpatest.sherpa.VadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val isRecording: Boolean = false,
    val isInitializing: Boolean = false,
    val modelsReady: Boolean = false,
    val lastResult: SttResult? = null,
    val lastWer: Float? = null,
    val referenceText: String = "",
    val history: List<MeasurementRecord> = emptyList(),
    val errorMessage: String? = null,
    val exportMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
        private const val MAX_HISTORY = 20
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val audioRecorder = AudioRecorder()
    private var vadManager: VadManager? = null
    private var sttManager: SttManager? = null

    private var recordingJob: Job? = null
    private var recordingStartTimeMs = 0L
    private var nextId = 1

    init {
        initializeModels()
    }

    private fun initializeModels() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isInitializing = true) }
            try {
                val context = getApplication<Application>()
                SherpaManager.ensureModelsExtracted(context)

                if (!SherpaManager.modelsExist(context)) {
                    _uiState.update {
                        it.copy(
                            isInitializing = false,
                            modelsReady = false,
                            errorMessage = "모델 파일이 없습니다. assets/models/ 에 배치하세요."
                        )
                    }
                    return@launch
                }

                vadManager = VadManager(context)
                sttManager = SttManager.getInstance(context)

                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        modelsReady = true,
                        errorMessage = null
                    )
                }
                Log.d(TAG, "Models initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Init failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        modelsReady = false,
                        errorMessage = "초기화 실패: ${e.message}"
                    )
                }
            }
        }
    }

    fun setReferenceText(text: String) {
        _uiState.update { it.copy(referenceText = text) }
    }

    fun startRecording() {
        if (_uiState.value.isRecording) return
        if (!_uiState.value.modelsReady) return

        _uiState.update { it.copy(isRecording = true, errorMessage = null, exportMessage = null) }
        recordingStartTimeMs = System.currentTimeMillis()
        vadManager?.reset()

        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                audioRecorder.start()
                val windowSamples = ShortArray(VadManager.WINDOW_SIZE)
                val floatWindow = FloatArray(VadManager.WINDOW_SIZE)

                while (_uiState.value.isRecording) {
                    val read = audioRecorder.readSamplesSync(windowSamples)
                    if (read <= 0) continue

                    // Short → Float 정규화
                    for (i in 0 until read) {
                        floatWindow[i] = windowSamples[i] / 32768.0f
                    }

                    val vadStart = System.currentTimeMillis()
                    vadManager?.acceptWaveform(floatWindow.copyOf(read))
                    val vadTimeMs = System.currentTimeMillis() - vadStart

                    // 발화 세그먼트 소비
                    while (vadManager?.isEmpty() == false) {
                        val segment = vadManager?.front() ?: break
                        vadManager?.popSegment()

                        val samples = segment.samples
                        processSegment(samples, vadTimeMs)
                    }
                }

                // 녹음 종료 후 flush
                vadManager?.flush()
                while (vadManager?.isEmpty() == false) {
                    val segment = vadManager?.front() ?: break
                    vadManager?.popSegment()
                    processSegment(segment.samples, 0L)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Recording error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            } finally {
                audioRecorder.stop()
            }
        }
    }

    private suspend fun processSegment(samples: FloatArray, vadTimeMs: Long) {
        val stt = sttManager ?: return
        try {
            val result = stt.recognize(samples, recordingStartTimeMs, vadTimeMs)
            val wer = if (_uiState.value.referenceText.isNotBlank())
                WerCalculator.calculate(_uiState.value.referenceText, result.text)
            else null

            withContext(Dispatchers.Main) {
                val record = MeasurementRecord(
                    id = nextId++,
                    referenceText = _uiState.value.referenceText,
                    sttText = result.text,
                    totalTimeMs = result.totalTimeMs,
                    vadTimeMs = result.vadTimeMs,
                    sttTimeMs = result.sttTimeMs,
                    audioLengthMs = result.audioLengthMs,
                    rtf = result.rtf,
                    wer = wer ?: 0f
                )
                _uiState.update { state ->
                    val newHistory = (listOf(record) + state.history).take(MAX_HISTORY)
                    state.copy(
                        lastResult = result,
                        lastWer = wer,
                        history = newHistory
                    )
                }
                recordingStartTimeMs = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e(TAG, "STT error: ${e.message}", e)
        }
    }

    fun stopRecording() {
        _uiState.update { it.copy(isRecording = false) }
        recordingJob?.cancel()
        recordingJob = null
    }

    fun exportCsv() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = MetricsExporter.exportCsv(
                    getApplication(),
                    _uiState.value.history.reversed()
                )
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(exportMessage = "저장 완료: $path") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(exportMessage = "저장 실패: ${e.message}") }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
        vadManager?.release()
    }
}
