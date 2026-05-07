package com.ssafy.e102.eumgil.core.stt

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * 마이크 오디오를 16kHz mono PCM FloatArray Flow로 스트리밍하는 녹음기.
 *
 * NoiseSuppressor · AcousticEchoCanceler · AutomaticGainControl 을 항상 활성화한다.
 * [stop] 호출 시 [startRecording] Flow가 자연 종료된다.
 */
internal class AudioRecorder {

    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
        private const val WINDOW_SIZE = 512
        private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var gainControl: AutomaticGainControl? = null

    @Volatile
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<FloatArray> = flow {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = minBuf.coerceAtLeast(WINDOW_SIZE * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize,
        )

        val sessionId = audioRecord!!.audioSessionId

        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(sessionId)?.also {
                it.enabled = true
                Log.d(TAG, "NoiseSuppressor 활성화")
            }
        }
        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(sessionId)?.also {
                it.enabled = true
                Log.d(TAG, "AcousticEchoCanceler 활성화")
            }
        }
        if (AutomaticGainControl.isAvailable()) {
            gainControl = AutomaticGainControl.create(sessionId)?.also {
                it.enabled = true
                Log.d(TAG, "AutomaticGainControl 활성화")
            }
        }

        audioRecord!!.startRecording()
        isRecording = true
        Log.d(TAG, "녹음 시작")

        val shortBuffer = ShortArray(WINDOW_SIZE)
        try {
            while (isRecording) {
                val read = audioRecord!!.read(shortBuffer, 0, shortBuffer.size)
                if (read > 0) {
                    emit(FloatArray(read) { shortBuffer[it] / 32768f })
                }
            }
        } finally {
            noiseSuppressor?.release(); noiseSuppressor = null
            echoCanceler?.release(); echoCanceler = null
            gainControl?.release(); gainControl = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(TAG, "녹음 중지")
        }
    }.flowOn(Dispatchers.IO)

    fun stop() {
        isRecording = false
    }

    fun isRecording(): Boolean = isRecording
}
