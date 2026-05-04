package com.test.sherpatest.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioRecorder {

    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
        private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var isRecording = false

    val bufferSize: Int
        get() = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(SAMPLE_RATE / 10 * 2) // 최소 100ms 버퍼

    fun start() {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val buffer = minBuffer.coerceAtLeast(SAMPLE_RATE / 10 * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            buffer
        )

        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(audioRecord!!.audioSessionId)
            noiseSuppressor?.enabled = true
            Log.d(TAG, "NoiseSuppressor enabled")
        }

        audioRecord?.startRecording()
        isRecording = true
        Log.d(TAG, "Recording started")
    }

    fun stop() {
        isRecording = false
        noiseSuppressor?.release()
        noiseSuppressor = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d(TAG, "Recording stopped")
    }

    fun isRecording() = isRecording

    suspend fun readSamples(buffer: ShortArray): Int = withContext(Dispatchers.IO) {
        audioRecord?.read(buffer, 0, buffer.size) ?: -1
    }

    fun readSamplesSync(buffer: ShortArray): Int {
        return audioRecord?.read(buffer, 0, buffer.size) ?: -1
    }
}
