package com.example.llmtest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.llmtest.databinding.ActivityVisuallyBinding
import com.example.llmtest.network.VoiceApiClient
import com.example.llmtest.network.models.VoiceAnalyzeRequest
import com.example.llmtest.network.models.VoiceConfirmRequest
import kotlinx.coroutines.launch
import java.util.Locale

class VisuallyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVisuallyBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech

    private var isListening = false
    private var isConfirmPhase = false
    private val resultLog = StringBuilder()

    companion object {
        private const val MIC_PERMISSION_REQUEST = 100
        private const val COLOR_YELLOW = "#FFC107"
        private const val COLOR_RED = "#F44336"
        private const val UTTERANCE_CONFIRMATION = "confirmation"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisuallyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTts()
        setupSpeechRecognizer()

        binding.btnMic.setOnClickListener {
            if (!hasMicPermission()) {
                requestMicPermission()
                return@setOnClickListener
            }
            if (isListening) {
                speechRecognizer.stopListening()
            } else {
                resetSession()
                startListening()
            }
        }
    }

    private fun setupTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.KOREAN
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == UTTERANCE_CONFIRMATION) {
                    runOnUiThread {
                        binding.tvStatus.text = "확인 응답을 말씀해 주세요"
                        startListeningForConfirm()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}
        })
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                setMicColor(COLOR_RED)
                binding.tvStatus.text = "녹음 중..."
            }

            override fun onEndOfSpeech() {
                isListening = false
                setMicColor(COLOR_YELLOW)
                binding.tvStatus.text = "음성 인식 중..."
            }

            override fun onError(error: Int) {
                isListening = false
                setMicColor(COLOR_YELLOW)
                binding.tvStatus.text = "마이크를 눌러 말씀하세요"
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.getOrNull(0) ?: return
                binding.tvStatus.text = "분석 중..."
                if (isConfirmPhase) callConfirmApi(text) else callAnalyzeApi(text)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun resetSession() {
        isConfirmPhase = false
        resultLog.clear()
        binding.tvResult.text = ""
    }

    private fun startListening() {
        speechRecognizer.startListening(buildRecognizeIntent())
    }

    private fun startListeningForConfirm() {
        isConfirmPhase = true
        speechRecognizer.startListening(buildRecognizeIntent())
    }

    private fun buildRecognizeIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
    }

    private fun callAnalyzeApi(text: String) {
        lifecycleScope.launch {
            try {
                val response = VoiceApiClient.service.analyze(
                    VoiceAnalyzeRequest(text = text, model = "gemini", mode = "visually")
                )
                val confirmMsg = response.confirmation_message ?: "다시 말씀해 주세요"
                resultLog.appendLine("[1단계] intent: ${response.intent} / 장소명: ${response.place_name ?: "-"}")
                resultLog.appendLine("[TTS] \"$confirmMsg\"")
                binding.tvResult.text = resultLog.toString().trimEnd()
                speakOut(confirmMsg, UTTERANCE_CONFIRMATION)
            } catch (e: Exception) {
                binding.tvStatus.text = "오류가 발생했습니다. 다시 시도해주세요."
            }
        }
    }

    private fun callConfirmApi(text: String) {
        lifecycleScope.launch {
            try {
                val response = VoiceApiClient.service.confirm(
                    VoiceConfirmRequest(text = text, model = "gemini")
                )
                resultLog.appendLine("[2단계] confirmed: ${response.confirmed}")
                resultLog.appendLine("[TTS] \"${response.message}\"")
                binding.tvResult.text = resultLog.toString().trimEnd()
                binding.tvStatus.text = "완료"
                speakOut(response.message, "result")
            } catch (e: Exception) {
                binding.tvStatus.text = "오류가 발생했습니다. 다시 시도해주세요."
            }
        }
    }

    private fun speakOut(text: String, utteranceId: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun setMicColor(hex: String) {
        binding.btnMic.backgroundTintList = ColorStateList.valueOf(Color.parseColor(hex))
    }

    private fun hasMicPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestMicPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.RECORD_AUDIO), MIC_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MIC_PERMISSION_REQUEST &&
            grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tts.shutdown()
    }
}
