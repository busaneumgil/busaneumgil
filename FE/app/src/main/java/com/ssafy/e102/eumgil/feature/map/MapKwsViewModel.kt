package com.ssafy.e102.eumgil.feature.map

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.stt.KeywordSpottingManager
import com.ssafy.e102.eumgil.core.stt.SherpaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface MapKwsEvent {
    data object NavigateToVoiceInput : MapKwsEvent
}

/**
 * 이동약자(Map) 화면 웨이크워드 감지 ViewModel.
 *
 * 화면 진입 시 KWS를 초기화하고 "HEY LINK" 웨이크워드를 청취한다.
 * [resumeSpotting]: ON_RESUME 시 KWS 재시작 (VoiceInput 사용 후 복귀 포함)
 * [pauseSpotting]: ON_PAUSE 시 KWS 일시정지 (마이크 충돌 방지)
 * 웨이크워드 감지 시 [MapKwsEvent.NavigateToVoiceInput] 이벤트를 발행한다.
 */
class MapKwsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MapKwsVM"
    }

    private val _uiEvent = Channel<MapKwsEvent>(Channel.BUFFERED)
    val uiEvent: Flow<MapKwsEvent> = _uiEvent.receiveAsFlow()

    private var kwsManager: KeywordSpottingManager? = null
    private var kwsJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                SherpaManager.ensureKwsModelsExtracted(context)

                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "RECORD_AUDIO 권한 없음 — 웨이크워드 감지 비활성화")
                    return@launch
                }

                if (!SherpaManager.kwsModelsExist(context)) {
                    Log.e(TAG, "KWS 모델 없음 — 웨이크워드 감지 비활성화")
                    return@launch
                }

                kwsManager = KeywordSpottingManager(context)
                Log.d(TAG, "KWS 초기화 완료 — 웨이크워드 청취 시작")
                startSpotting()
            } catch (e: Exception) {
                Log.e(TAG, "KWS 초기화 실패: ${e.message}", e)
            }
        }
    }

    private fun startSpotting() {
        kwsJob = viewModelScope.launch(Dispatchers.IO) {
            kwsManager?.startSpotting()?.collect {
                Log.d(TAG, "웨이크워드 감지 → VoiceInput 이동")
                kwsManager?.stop()
                _uiEvent.send(MapKwsEvent.NavigateToVoiceInput)
            }
        }
    }

    fun resumeSpotting() {
        if (kwsJob?.isActive == true) return
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "RECORD_AUDIO 권한 없음 — KWS 재시작 스킵")
            return
        }
        if (kwsManager == null) {
            Log.d(TAG, "kwsManager null — 초기화 후 KWS 시작")
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    SherpaManager.ensureKwsModelsExtracted(context)
                    if (!SherpaManager.kwsModelsExist(context)) {
                        Log.e(TAG, "KWS 모델 없음 — 재시작 스킵")
                        return@launch
                    }
                    kwsManager = KeywordSpottingManager(context)
                    startSpotting()
                } catch (e: Exception) {
                    Log.e(TAG, "KWS 재초기화 실패: ${e.message}", e)
                }
            }
            return
        }
        Log.d(TAG, "KWS 재시작")
        startSpotting()
    }

    fun pauseSpotting() {
        kwsJob?.cancel()
        kwsManager?.stop()
        Log.d(TAG, "KWS 일시정지")
    }

    override fun onCleared() {
        super.onCleared()
        kwsJob?.cancel()
        kwsManager?.release()
        Log.d(TAG, "MapKwsViewModel cleared — KWS 해제")
    }
}
