package com.ssafy.e102.eumgil.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityTest {
    @Test
    fun `app defaults hardware volume buttons to media stream`() {
        assertEquals(EXPECTED_STREAM_MUSIC, defaultAppVolumeControlStream())
    }
}

private const val EXPECTED_STREAM_MUSIC = 3
