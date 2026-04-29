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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.llmtest.databinding.ActivityWalkBinding
import com.example.llmtest.network.VoiceApiClient
import com.example.llmtest.network.models.VoiceAnalyzeRequest
import kotlinx.coroutines.launch

class WalkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWalkBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false

    companion object {
        private const val MIC_PERMISSION_REQUEST = 100
        private const val COLOR_BLUE = "#2196F3"
        private const val COLOR_RED = "#F44336"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpeechRecognizer()

        binding.btnMic.setOnClickListener {
            if (!hasMicPermission()) {
                requestMicPermission()
                return@setOnClickListener
            }
            if (isListening) {
                speechRecognizer.stopListening()
            } else {
                startListening()
            }
        }
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
                setMicColor(COLOR_BLUE)
                binding.tvStatus.text = "음성 인식 중..."
            }

            override fun onError(error: Int) {
                isListening = false
                setMicColor(COLOR_BLUE)
                binding.tvStatus.text = "마이크를 눌러 말씀하세요"
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.getOrNull(0) ?: return
                binding.tvStatus.text = "분석 중..."
                callAnalyzeApi(text)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
        }
        speechRecognizer.startListening(intent)
    }

    private fun callAnalyzeApi(text: String) {
        lifecycleScope.launch {
            try {
                val response = VoiceApiClient.service.analyze(
                    VoiceAnalyzeRequest(text = text, model = "gemini", mode = "mobility")
                )
                binding.tvStatus.text = "완료"
                binding.tvResult.text = buildString {
                    appendLine("intent: ${response.intent ?: "-"}")
                    appendLine("장소명: ${response.place_name ?: "-"}")
                    append("응답시간: ${response.latency_ms ?: "-"}ms")
                }
            } catch (e: Exception) {
                binding.tvStatus.text = "오류가 발생했습니다. 다시 시도해주세요."
                binding.tvResult.text = e.message
            }
        }
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
    }
}
