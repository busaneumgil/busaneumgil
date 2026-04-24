package com.ssafy.e102.eumgil.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TextToSpeechControllerTest {
    @Test
    fun `no op tts controller stays unavailable and ignores commands`() {
        val controller = NoOpTextToSpeechController

        controller.setEnabled(enabled = true)
        controller.speak("route briefing")
        controller.stop()
        controller.shutdown()

        assertFalse(controller.state.value.enabled)
        assertEquals(TextToSpeechAvailability.Unavailable, controller.state.value.availability)
        assertFalse(controller.state.value.canSpeak)
    }
}
