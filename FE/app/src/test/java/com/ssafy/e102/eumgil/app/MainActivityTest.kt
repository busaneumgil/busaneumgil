package com.ssafy.e102.eumgil.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityTest {
    @Test
    fun `app defaults hardware volume buttons to media stream`() {
        assertEquals(EXPECTED_STREAM_MUSIC, defaultAppVolumeControlStream())
    }

    @Test
    fun `app allows audio playback capture by all capture clients`() {
        assertEquals(EXPECTED_ALLOW_CAPTURE_BY_ALL, defaultAppAudioPlaybackCapturePolicy())
    }
}

private const val EXPECTED_STREAM_MUSIC = 3
private const val EXPECTED_ALLOW_CAPTURE_BY_ALL = 1
