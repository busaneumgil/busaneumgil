package com.ssafy.e102.eumgil.feature.lowvision

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.permission.hasGrantedMicrophonePermission
import com.ssafy.e102.eumgil.core.stt.KeywordSpottingManager
import com.ssafy.e102.eumgil.core.stt.SherpaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface LowVisionEvent {
    /** 웨이크워드("HEY LINK") 감지 → 저시력 음성 입력 화면 열기. */
    data object NavigateToVoiceInput : LowVisionEvent
}

/**
 * LowVision 네비게이션 그래프 공유 ViewModel.
 *
 * `navigation(route = LOW_VISION_GRAPH_ROUTE, ...)` 중첩 그래프의
 * NavBackStackEntry에 스코프되어, LowVision 내 모든 탭에서 동일 인스턴스를 공유한다.
 *
 * 화면 진입 시 자동으로 "HEY LINK" 웨이크워드 청취를 시작하며,
 * 감지되면 [LowVisionEvent.NavigateToVoiceInput]을 발행한다.
 * LowVision 그래프를 완전히 벗어날 때 ViewModel이 cleared되어 KWS가 해제된다.
 */
class LowVisionViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "LowVisionVM"
    }

    private val _uiEvent = Channel<LowVisionEvent>(Channel.BUFFERED)
    val uiEvent: Flow<LowVisionEvent> = _uiEvent.receiveAsFlow()

    private var kwsManager: KeywordSpottingManager? = null
    private var kwsJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                initializeKeywordSpotting(context)
            } catch (e: Exception) {
                Log.e(TAG, "KWS 초기화 실패: ${e.message}", e)
            }
        }
    }

    private fun startSpotting() {
        kwsJob = viewModelScope.launch(Dispatchers.IO) {
            kwsManager?.startSpotting()?.collect {
                Log.d(TAG, "웨이크워드 감지 → 저시력 음성 입력 화면 열기")
                // 저시력 음성 입력 화면이 마이크를 점유할 수 있도록 KWS 녹음 즉시 중단
                kwsManager?.stop()
                _uiEvent.send(LowVisionEvent.NavigateToVoiceInput)
            }
        }
    }

    /**
     * 탭 화면으로 복귀 시 KWS를 재시작한다.
     *
     * [LowVisionKwsNavEffect]의 LaunchedEffect에서 탭 화면 진입 시마다 호출되어,
     * 저시력 음성 입력 화면 사용 후 돌아왔을 때 웨이크워드 감지가 자동으로 재개된다.
     *
     * - KWS가 이미 실행 중이면 아무 작업도 하지 않는다.
     * - [kwsManager]가 null이면 권한 허용 이후 진입한 경우를 포함해 초기화 후 재시작을 시도한다.
     */
    fun resumeSpotting() {
        if (kwsJob?.isActive == true) return
        val context = getApplication<Application>()
        if (!context.hasGrantedMicrophonePermission()) {
            Log.w(TAG, "RECORD_AUDIO 권한 없음 — KWS 재시작 스킵")
            return
        }
        if (kwsManager == null) {
            Log.d(TAG, "kwsManager null — 초기화 후 KWS 시작")
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    initializeKeywordSpotting(context)
                } catch (e: Exception) {
                    Log.e(TAG, "KWS 재초기화 실패: ${e.message}", e)
                }
            }
            return
        }
        Log.d(TAG, "KWS 재시작")
        startSpotting()
    }

    private suspend fun initializeKeywordSpotting(context: Application) {
        SherpaManager.ensureKwsModelsExtracted(context)

        if (!context.hasGrantedMicrophonePermission()) {
            Log.w(TAG, "RECORD_AUDIO 권한 없음 — 웨이크워드 감지 비활성화")
            return
        }

        if (!SherpaManager.kwsModelsExist(context)) {
            Log.e(TAG, "KWS 모델 없음 — 웨이크워드 감지 비활성화")
            return
        }

        kwsManager = KeywordSpottingManager(context)
        Log.d(TAG, "KWS 초기화 완료 — 웨이크워드 청취 시작")
        startSpotting()
    }

    override fun onCleared() {
        super.onCleared()
        kwsJob?.cancel()
        kwsManager?.release()
        Log.d(TAG, "LowVisionViewModel cleared — KWS 해제")
    }
}
