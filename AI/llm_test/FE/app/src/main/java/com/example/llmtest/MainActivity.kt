package com.example.llmtest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.llmtest.audio.AudioRecorder
import com.example.llmtest.audio.TTSManager
import com.example.llmtest.databinding.ActivityMainBinding
import com.example.llmtest.network.RetrofitClient
import com.example.llmtest.network.models.STTRequest
import com.example.llmtest.utils.PerformanceLogger
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.*

enum class ConversationState {
    INITIAL,
    WAITING_CONFIRMATION,
    CONFIRMED
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var ttsManager: TTSManager

    private val sessionId = UUID.randomUUID().toString()
    private var isAndroidSTTRecording = false
    private var isWhisperSTTRecording = false

    private var currentState = ConversationState.INITIAL
    private var currentDeparture: String? = null
    private var currentDestination: String? = null

    private val modelMap = mapOf(
        "Gemma-ko 2B" to "gemma-ko-2b",
        "Gemma-ko 9B" to "gemma-ko-9b",
        "KULLM 5.8B" to "kullm-5.8b",
        "Qwen 2.5 7B" to "qwen2.5:7b",
        "SOLAR 10.7B" to "solar",
        "Mistral 7B" to "mistral"
    )

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate - Session ID: $sessionId")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
        initializeComponents()
        setupListeners()
    }

    // 권한 체크
    private fun checkPermissions() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting RECORD_AUDIO permission")
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_CODE)
        } else {
            Log.d(TAG, "RECORD_AUDIO permission already granted")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "RECORD_AUDIO permission granted")
                Toast.makeText(this, "마이크 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "RECORD_AUDIO permission denied")
                Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 컴포넌트 초기화
    private fun initializeComponents() {
        Log.d(TAG, "Initializing components...")

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(createRecognitionListener())

        audioRecorder = AudioRecorder(this)

        ttsManager = TTSManager(this) { success ->
            if (success) {
                Log.d(TAG, "TTS ready")
            } else {
                Log.e(TAG, "TTS init failed")
                runOnUiThread {
                    Toast.makeText(this, "TTS 초기화 실패. 한국어 TTS가 설치되어 있는지 확인하세요.", Toast.LENGTH_LONG).show()
                }
            }
        }

        Log.d(TAG, "Components initialized")
    }

    // 리스너 설정
    private fun setupListeners() {
        Log.d(TAG, "Setting up listeners")

        // Android STT 버튼
        binding.btnAndroidSTT.setOnClickListener {
            if (!isAndroidSTTRecording) {
                startAndroidSTT()
            } else {
                stopAndroidSTT()
            }
        }

        // Whisper STT 버튼
        binding.btnWhisperSTT.setOnClickListener {
            if (!isWhisperSTTRecording) {
                startWhisperRecording()
            } else {
                stopWhisperRecording()
            }
        }

        // 길찾기 시작 버튼
        binding.btnStartNavigation.setOnClickListener {
            Log.d("NAVIGATION", "길찾기 실행: $currentDeparture → $currentDestination")
            Toast.makeText(this, "길찾기 기능은 아직 구현되지 않았습니다", Toast.LENGTH_SHORT).show()
            resetConversationState()
        }

        // 다크모드 스위치
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            toggleDarkMode(isChecked)
        }
    }

    private fun resetConversationState() {
        currentState = ConversationState.INITIAL
        currentDeparture = null
        currentDestination = null
        binding.btnStartNavigation.visibility = View.GONE
        binding.layoutConfirm.visibility = View.GONE
        updateConversationStateUI()
    }

    private fun updateConversationStateUI() {
        binding.tvConversationState.text = "상태: ${currentState.name.lowercase()}"
    }

    private fun handleLLMResponse(response: com.example.llmtest.network.models.LLMResponse) {
        ttsManager.speak(response.response)

        when (currentState) {
            ConversationState.INITIAL -> {
                if (response.departure != null && response.destination != null) {
                    currentDeparture = response.departure
                    currentDestination = response.destination
                    currentState = ConversationState.WAITING_CONFIRMATION
                    binding.layoutConfirm.visibility = View.VISIBLE
                    binding.tvDeparture.text = response.departure
                    binding.tvDestination.text = response.destination
                } else {
                    currentState = ConversationState.INITIAL
                }
            }
            ConversationState.WAITING_CONFIRMATION -> {
                if (response.confirmed) {
                    currentState = ConversationState.CONFIRMED
                    binding.btnStartNavigation.visibility = View.VISIBLE
                    ttsManager.speak("길찾기를 시작합니다")
                } else {
                    resetConversationState()
                    ttsManager.speak("다시 말씀해주세요")
                }
            }
            ConversationState.CONFIRMED -> {}
        }
        updateConversationStateUI()
    }

    // Android STT 시작
    private fun startAndroidSTT() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val requestId = "req_${System.currentTimeMillis()}"
        PerformanceLogger.start(requestId, "Android STT")

        Log.d(TAG, "Starting Android STT - Request ID: $requestId")

        isAndroidSTTRecording = true
        binding.btnAndroidSTT.text = "⏹️\nAndroid\nSTT\n중지"
        binding.btnAndroidSTT.isEnabled = true
        appendResult("\n━━━━━━━━━━━━━━━━━━━━\n📱 [Android STT] 음성 인식 중...\n")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        PerformanceLogger.log(requestId, "Speech recognition started")

        speechRecognizer.startListening(intent)
    }

    // Android STT 중지
    private fun stopAndroidSTT() {
        Log.d(TAG, "Stopping Android STT")
        speechRecognizer.stopListening()
        isAndroidSTTRecording = false
        binding.btnAndroidSTT.text = "📱\nAndroid\nSTT"
    }

    // RecognitionListener 생성
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            private var requestId: String = ""
            private var startTime: Long = 0

            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
                requestId = "req_${System.currentTimeMillis()}"
                startTime = System.currentTimeMillis()
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
                PerformanceLogger.logStep("Android STT", "Speech begin", startTime)
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
                PerformanceLogger.logStep("Android STT", "Speech end", startTime)
                isAndroidSTTRecording = false
                runOnUiThread {
                    binding.btnAndroidSTT.text = "📱\nAndroid\nSTT"
                    binding.btnAndroidSTT.isEnabled = true
                }
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "오디오 오류"
                    SpeechRecognizer.ERROR_CLIENT -> "클라이언트 오류"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 없음"
                    SpeechRecognizer.ERROR_NETWORK -> "네트워크 오류"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                    SpeechRecognizer.ERROR_NO_MATCH -> "인식 실패"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "인식기 사용 중"
                    SpeechRecognizer.ERROR_SERVER -> "서버 오류"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 타임아웃"
                    else -> "알 수 없는 오류 ($error)"
                }

                Log.e(TAG, "STT Error: $errorMsg")

                isAndroidSTTRecording = false
                runOnUiThread {
                    binding.btnAndroidSTT.text = "📱\nAndroid\nSTT"
                    binding.btnAndroidSTT.isEnabled = true
                    showLoading(false)
                    appendResult("❌ STT 오류: $errorMsg\n")
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""

                PerformanceLogger.logStep("Android STT", "STT result received", startTime)
                Log.d(TAG, "STT Result: '$recognizedText'")

                isAndroidSTTRecording = false
                runOnUiThread {
                    binding.btnAndroidSTT.text = "📱\nAndroid\nSTT"
                    appendResult("🗣️ 인식된 텍스트: $recognizedText\n")
                }

                if (recognizedText.isNotBlank()) {
                    sendAndroidSTTToLLM(recognizedText)
                } else {
                    runOnUiThread {
                        showLoading(false)
                        appendResult("⚠️ 음성을 인식하지 못했습니다.\n")
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    // Android STT 결과를 LLM으로 전송
    private fun sendAndroidSTTToLLM(text: String) {
        val selectedModelName = binding.spinnerModel.selectedItem.toString()
        val modelId = modelMap[selectedModelName] ?: "qwen2.5:7b"
        val requestId = "android_${System.currentTimeMillis()}"

        Log.d(TAG, "Sending to LLM - Model: $modelId, Text: '$text'")

        val startTime = System.currentTimeMillis()
        PerformanceLogger.start(requestId, "LLM Request (Android STT)")

        lifecycleScope.launch {
            try {
                runOnUiThread {
                    showLoading(true)
                    appendResult("🤖 모델: $selectedModelName\n")
                    appendResult("⏳ LLM 추론 중...\n")
                }

                val request = STTRequest(
                    model_name = modelId,
                    message = text,
                    session_id = sessionId,
                    conversation_state = currentState.name.lowercase()
                )

                PerformanceLogger.log(requestId, "Server request sent")

                val response = RetrofitClient.apiService.chatWithAndroidSTT(request)

                val elapsed = PerformanceLogger.end(requestId)

                Log.d(TAG, "LLM Response received in ${elapsed}ms")

                runOnUiThread {
                    showLoading(false)
                    appendResult("✅ 응답: ${response.response}\n")
                    appendResult("⏱️ 추론 시간: ${response.inference_time}s\n")
                    if (response.departure != null || response.destination != null) {
                        appendResult("🗺️ 출발지: ${response.departure ?: "미확인"}\n")
                        appendResult("🏁 도착지: ${response.destination ?: "미확인"}\n")
                    }
                    appendResult("📊 총 시간: ${elapsed}ms\n")

                    handleAction(response.action)
                    handleLLMResponse(response)
                }

            } catch (e: Exception) {
                Log.e(TAG, "LLM request failed", e)
                runOnUiThread {
                    showLoading(false)
                    appendResult("❌ 오류: ${e.message}\n")
                    Toast.makeText(this@MainActivity, "서버 연결 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Whisper STT 녹음 시작
    private fun startWhisperRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val file = audioRecorder.startRecording()
        if (file != null) {
            isWhisperSTTRecording = true
            binding.btnWhisperSTT.text = "⏹️\n중지"
            appendResult("\n━━━━━━━━━━━━━━━━━━━━\n🎙️ [Whisper STT] 녹음 중...\n")
            Log.d(TAG, "Whisper recording started")
        } else {
            Toast.makeText(this, "녹음 시작 실패", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Failed to start Whisper recording")
        }
    }

    // Whisper STT 녹음 중지 및 전송
    private fun stopWhisperRecording() {
        Log.d(TAG, "Stopping Whisper recording...")

        val audioFile = audioRecorder.stopRecording()
        isWhisperSTTRecording = false
        binding.btnWhisperSTT.text = "🎙️\nWhisper\nSTT"

        if (audioFile != null && audioFile.exists()) {
            Log.d(TAG, "Audio file ready: ${audioFile.absolutePath}, size: ${audioFile.length()} bytes")
            appendResult("🎵 녹음 완료 (${audioFile.length()} bytes)\n")
            showLoading(true)
            sendAudioToWhisper(audioFile)
        } else {
            Log.e(TAG, "Audio file is null or does not exist")
            appendResult("❌ 녹음 파일 없음\n")
        }
    }

    // 음성 파일을 Whisper STT로 전송
    private fun sendAudioToWhisper(audioFile: File) {
        val selectedModelName = binding.spinnerModel.selectedItem.toString()
        val modelId = modelMap[selectedModelName] ?: "qwen2.5:7b"
        val requestId = "whisper_${System.currentTimeMillis()}"

        Log.d(TAG, "Sending audio to Whisper - Model: $modelId, File: ${audioFile.name}")

        PerformanceLogger.start(requestId, "Whisper STT + LLM")

        lifecycleScope.launch {
            try {
                runOnUiThread {
                    appendResult("🤖 모델: $selectedModelName\n")
                    appendResult("⏳ Whisper STT 변환 중...\n")
                }

                val requestFile = audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val audioPart = MultipartBody.Part.createFormData("audio", audioFile.name, requestFile)
                val modelNameBody = modelId.toRequestBody("text/plain".toMediaTypeOrNull())
                val conversationStateBody = currentState.name.lowercase().toRequestBody("text/plain".toMediaTypeOrNull())

                PerformanceLogger.log(requestId, "Server request sent")

                val response = RetrofitClient.apiService.chatWithWhisperSTT(audioPart, modelNameBody, conversationStateBody)

                val elapsed = PerformanceLogger.end(requestId)

                Log.d(TAG, "Whisper+LLM response received in ${elapsed}ms")

                runOnUiThread {
                    showLoading(false)

                    if (!response.transcribed_text.isNullOrBlank()) {
                        appendResult("🗣️ Whisper 인식: ${response.transcribed_text}\n")
                    }

                    appendResult("✅ 응답: ${response.response}\n")
                    appendResult("⏱️ 추론 시간 (STT+LLM): ${response.inference_time}s\n")

                    if (response.departure != null || response.destination != null) {
                        appendResult("🗺️ 출발지: ${response.departure ?: "미확인"}\n")
                        appendResult("🏁 도착지: ${response.destination ?: "미확인"}\n")
                    }

                    appendResult("📊 총 시간: ${elapsed}ms\n")

                    handleAction(response.action)
                    handleLLMResponse(response)
                }

                // 임시 파일 삭제
                if (audioFile.exists()) {
                    audioFile.delete()
                    Log.d(TAG, "Temp audio file deleted")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Whisper request failed", e)
                runOnUiThread {
                    showLoading(false)
                    appendResult("❌ 오류: ${e.message}\n")
                    Toast.makeText(this@MainActivity, "서버 연결 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                if (audioFile.exists()) audioFile.delete()
            }
        }
    }

    // 액션 처리 (다크모드 등)
    private fun handleAction(action: String) {
        Log.d(TAG, "Handling action: $action")
        when (action) {
            "enable_dark_mode" -> {
                binding.switchDarkMode.isChecked = true
                toggleDarkMode(true)
            }
            "disable_dark_mode" -> {
                binding.switchDarkMode.isChecked = false
                toggleDarkMode(false)
            }
            else -> {
                Log.d(TAG, "No UI action for: $action")
            }
        }
    }

    // 다크모드 토글
    private fun toggleDarkMode(enable: Boolean) {
        Log.d(TAG, "Toggle dark mode: $enable")
        val mode = if (enable) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    // 결과 텍스트 추가
    private fun appendResult(text: String) {
        val current = binding.tvResult.text.toString()
        val updated = if (current.startsWith("테스트를 시작하려면")) text else current + text
        binding.tvResult.text = updated

        // 스크롤을 아래로
        binding.scrollResult.post {
            binding.scrollResult.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    // 로딩 표시
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnAndroidSTT.isEnabled = !show
        binding.btnWhisperSTT.isEnabled = !show
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        speechRecognizer.destroy()
        ttsManager.shutdown()
        super.onDestroy()
    }
}
