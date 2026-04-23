package com.example.llmtest.compare

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.llmtest.R
import com.example.llmtest.databinding.ActivityCompareBinding
import com.example.llmtest.network.models.CompareResult
import com.example.llmtest.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

class CompareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompareBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private val gson = Gson()
    private var eventSource: EventSource? = null
    private var resultCount = 0
    private var isListening = false

    private val sseClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "CompareActivity"
        private val MEDALS = listOf("🥇", "🥈", "🥉", "4️⃣")
        private val PROVIDER_LABELS = mapOf(
            "qwen_ollama" to "Qwen 2.5 7B (Ollama)",
            "gemini"      to "Gemini 2.5 Flash Lite",
            "claude"      to "Claude 3.5 Haiku",
            "gpt"         to "GPT-5 nano"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "LLM 비교 분석"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(createRecognitionListener())

        binding.btnStartCompare.setOnClickListener {
            if (!isListening) startSTT() else stopSTT()
        }
    }

    private fun startSTT() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        isListening = true
        binding.btnStartCompare.text = "⏹️  음성 인식 중... (탭하여 중지)"
        binding.tvCompareStatus.text = "음성을 말씀해주세요..."
        binding.layoutResults.removeAllViews()
        resultCount = 0
        speechRecognizer.startListening(intent)
    }

    private fun stopSTT() {
        speechRecognizer.stopListening()
        isListening = false
        binding.btnStartCompare.text = "🎙️  음성 입력 후 4개 LLM 비교 시작"
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        private var sttStartMs = 0L

        override fun onReadyForSpeech(params: Bundle?) {
            sttStartMs = System.currentTimeMillis()
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            isListening = false
            runOnUiThread {
                binding.btnStartCompare.text = "🎙️  음성 입력 후 4개 LLM 비교 시작"
            }
        }
        override fun onError(error: Int) {
            isListening = false
            runOnUiThread {
                binding.btnStartCompare.text = "🎙️  음성 입력 후 4개 LLM 비교 시작"
                binding.tvCompareStatus.text = "음성 인식 오류 (error=$error)"
            }
        }
        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            runOnUiThread {
                binding.tvRecognizedText.text = "\"$text\""
                binding.tvCompareStatus.text = "4개 LLM에 동시 전송 중..."
                binding.btnStartCompare.isEnabled = false
            }
            startCompareStream(text, sttStartMs)
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun startCompareStream(text: String, sttStartMs: Long) {
        val baseUrl = RetrofitClient.BASE_URL
        val url = "$baseUrl/api/compare/stream" +
                  "?text=${android.net.Uri.encode(text)}&stt_start_ms=$sttStartMs"

        val request = Request.Builder().url(url).build()

        eventSource = EventSources.createFactory(sseClient)
            .newEventSource(request, object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    Log.d(TAG, "SSE connected")
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    if (data.contains("\"event\":\"done\"")) {
                        runOnUiThread {
                            binding.tvCompareStatus.text = "✅ 비교 완료"
                            binding.btnStartCompare.isEnabled = true
                        }
                        return
                    }
                    try {
                        val result = gson.fromJson(data, CompareResult::class.java)
                        runOnUiThread { addResultCard(result) }
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON parse error: $data", e)
                    }
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    Log.e(TAG, "SSE failure", t)
                    runOnUiThread {
                        binding.tvCompareStatus.text = "❌ 연결 오류: ${t?.message ?: response?.code}"
                        binding.btnStartCompare.isEnabled = true
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    Log.d(TAG, "SSE closed")
                }
            })
    }

    private fun addResultCard(result: CompareResult) {
        val card = LayoutInflater.from(this)
            .inflate(R.layout.item_llm_result, binding.layoutResults, false)

        val medal = if (resultCount < MEDALS.size) MEDALS[resultCount] else "▪️"
        resultCount++

        card.findViewById<TextView>(R.id.tvRank).text = medal
        card.findViewById<TextView>(R.id.tvProviderName).text =
            PROVIDER_LABELS[result.provider] ?: result.provider

        if (result.success) {
            card.findViewById<TextView>(R.id.tvSuccess).apply {
                text = "✅ 성공"
                setTextColor(ContextCompat.getColor(this@CompareActivity, android.R.color.holo_green_dark))
            }
            card.findViewById<TextView>(R.id.tvDeparture).text =
                "출발지: ${result.departure ?: "없음"}"
            card.findViewById<TextView>(R.id.tvDestination).text =
                "도착지: ${result.destination ?: "없음"}"

            if (result.intent == "navigation" &&
                (result.departure != null || result.destination != null)) {
                card.findViewById<MaterialButton>(R.id.btnNavigate).apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        Toast.makeText(
                            this@CompareActivity,
                            "${result.provider}: ${result.departure} → ${result.destination}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            card.findViewById<TextView>(R.id.tvSuccess).apply {
                text = "❌ 실패"
                setTextColor(ContextCompat.getColor(this@CompareActivity, android.R.color.holo_red_dark))
            }
            card.findViewById<TextView>(R.id.tvError).apply {
                text = result.error ?: "알 수 없는 오류"
                visibility = View.VISIBLE
            }
        }

        card.findViewById<TextView>(R.id.tvTotalLatency).text =
            "전체: ${result.total_latency_ms.toLong()}ms"
        card.findViewById<TextView>(R.id.tvLlmLatency).text =
            "LLM: ${result.llm_latency_ms.toLong()}ms"
        card.findViewById<TextView>(R.id.tvCost).text =
            "크레딧: %.4f".format(result.cost_credit)
        card.findViewById<TextView>(R.id.tvTokens).text =
            "토큰: ${result.input_tokens}+${result.output_tokens}"

        card.alpha = 0f
        binding.layoutResults.addView(card)
        card.animate().alpha(1f).setDuration(400).start()

        binding.scrollCompareResults.post {
            binding.scrollCompareResults.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        eventSource?.cancel()
        speechRecognizer.destroy()
        super.onDestroy()
    }
}
