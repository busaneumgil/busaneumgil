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
import com.example.llmtest.audio.TTSManager
import com.example.llmtest.compare.CompareActivity
import com.example.llmtest.databinding.ActivityMainBinding
import com.example.llmtest.network.RetrofitClient
import com.example.llmtest.network.models.CompareResult
import com.example.llmtest.network.models.LLMRequest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var ttsManager: TTSManager

    private var isRecording = false
    private var sttStartMs = 0L

    private val modelMap = mapOf(
        "Gemini 2.5 Flash" to "gemini",
        "Claude Haiku 4.5" to "claude",
        "GPT-5 mini"       to "gpt_mini"
    )

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkMicPermission()
        setupSpeechRecognizer()
        setupTTS()
        setupListeners()
    }

    private fun checkMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE &&
            (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                sttStartMs = System.currentTimeMillis()
                runOnUiThread { binding.tvStatus.text = "말씀하세요..." }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isRecording = false
                runOnUiThread {
                    binding.btnRecord.text = "🎙️\n녹음 시작"
                    binding.tvStatus.text = "변환 중..."
                }
            }
            override fun onError(error: Int) {
                isRecording = false
                Log.e(TAG, "STT error: $error")
                runOnUiThread {
                    binding.btnRecord.text = "🎙️\n녹음 시작"
                    binding.tvStatus.text = "음성 인식 오류 (code=$error)"
                    showLoading(false)
                }
            }
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                runOnUiThread {
                    binding.tvRecognizedText.text = "\"$text\""
                    binding.tvStatus.text = "LLM 분석 중..."
                }
                sendToLLM(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun setupTTS() {
        ttsManager = TTSManager(this) { success ->
            if (!success) runOnUiThread {
                Toast.makeText(this, "TTS 초기화 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnRecord.setOnClickListener {
            if (!isRecording) startRecording() else stopRecording()
        }
        binding.btnOpenCompare.setOnClickListener {
            startActivity(Intent(this, CompareActivity::class.java))
        }
        binding.btnStartNavigation.setOnClickListener {
            Toast.makeText(this, "길찾기 기능은 아직 구현되지 않았습니다", Toast.LENGTH_SHORT).show()
        }
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        isRecording = true
        binding.btnRecord.text = "⏹️\n녹음 중단"
        binding.tvStatus.text = "준비 중..."
        binding.cardResult.visibility = View.GONE
        binding.btnStartNavigation.visibility = View.GONE

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer.startListening(intent)
    }

    private fun stopRecording() {
        speechRecognizer.stopListening()
        isRecording = false
        binding.btnRecord.text = "🎙️\n녹음 시작"
        binding.tvStatus.text = ""
    }

    private fun sendToLLM(text: String) {
        val selectedModel = binding.spinnerModel.selectedItem.toString()
        val modelId = modelMap[selectedModel] ?: "gemini"

        showLoading(true)
        lifecycleScope.launch {
            try {
                val result = RetrofitClient.apiService.chatWithLLM(
                    LLMRequest(text = text, model = modelId, stt_start_ms = sttStartMs)
                )
                runOnUiThread { displayResult(result) }
            } catch (e: Exception) {
                Log.e(TAG, "LLM request failed", e)
                runOnUiThread {
                    showLoading(false)
                    binding.tvStatus.text = "❌ 서버 오류: ${e.message}"
                }
            }
        }
    }

    private fun displayResult(result: CompareResult) {
        showLoading(false)
        binding.cardResult.visibility = View.VISIBLE

        val providerLabel = mapOf(
            "gemini"   to "Gemini 2.5 Flash",
            "claude"   to "Claude Haiku 4.5",
            "gpt_mini" to "GPT-5 mini"
        )[result.provider] ?: result.provider

        binding.tvResultProvider.text = if (result.success) "✅ $providerLabel" else "❌ $providerLabel"
        binding.tvResultDeparture.text = "출발지: ${result.departure ?: "없음"}"
        binding.tvResultDestination.text = "도착지: ${result.destination ?: "없음"}"
        binding.tvResultLatency.text =
            "전체: ${result.total_latency_ms.toLong()}ms  |  LLM: ${result.llm_latency_ms.toLong()}ms"
        binding.tvResultCost.text =
            "크레딧: ${"%.4f".format(result.cost_credit)}  |  토큰: ${result.input_tokens}+${result.output_tokens}"

        binding.tvStatus.text = if (result.success) "✅ 분석 완료" else "❌ ${result.error ?: "분석 실패"}"

        if (result.success && result.intent == "navigation" &&
            (result.departure != null || result.destination != null)) {
            binding.btnStartNavigation.visibility = View.VISIBLE
        }

        result.confirmation_message?.let { ttsManager.speak(it) }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRecord.isEnabled = !show
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        ttsManager.shutdown()
        super.onDestroy()
    }
}
