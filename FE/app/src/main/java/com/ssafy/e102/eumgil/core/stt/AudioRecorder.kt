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

    @Volatile
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<FloatArray> = flow {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = minBuf.coerceAtLeast(WINDOW_SIZE * 2)

        // 인스턴스 필드가 아닌 flow 호출마다 독립된 지역 변수로 선언.
        // 이전 flow의 finally가 새 flow의 객체를 null로 만드는 레이스 컨디션 방지.
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize,
        )

        val sessionId = audioRecord.audioSessionId

        val noiseSuppressor: NoiseSuppressor? =
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor.create(sessionId)?.also {
                    it.enabled = true
                    Log.d(TAG, "NoiseSuppressor 활성화")
                }
            } else null

        val echoCanceler: AcousticEchoCanceler? =
            if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler.create(sessionId)?.also {
                    it.enabled = true
                    Log.d(TAG, "AcousticEchoCanceler 활성화")
                }
            } else null

        val gainControl: AutomaticGainControl? =
            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl.create(sessionId)?.also {
                    it.enabled = true
                    Log.d(TAG, "AutomaticGainControl 활성화")
                }
            } else null

        audioRecord.startRecording()
        isRecording = true
        Log.d(TAG, "녹음 시작")

        val shortBuffer = ShortArray(WINDOW_SIZE)
        try {
            while (isRecording) {
                val read = audioRecord.read(shortBuffer, 0, shortBuffer.size)
                if (read > 0) {
                    emit(FloatArray(read) { shortBuffer[it] / 32768f })
                }
            }
        } finally {
            // 각 flow가 자신이 만든 지역 변수만 정리하므로 Effect 리소스 누수 없음
            noiseSuppressor?.release()
            echoCanceler?.release()
            gainControl?.release()
            audioRecord.stop()
            audioRecord.release()
            Log.d(TAG, "녹음 중지")
        }
    }.flowOn(Dispatchers.IO)

    fun stop() {
        isRecording = false
    }

    fun isRecording(): Boolean = isRecording
}
