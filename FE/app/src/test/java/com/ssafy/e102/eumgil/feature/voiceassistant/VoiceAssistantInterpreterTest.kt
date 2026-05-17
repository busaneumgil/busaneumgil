package com.ssafy.e102.eumgil.feature.voiceassistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceAssistantInterpreterTest {
    private val interpreter = RuleBasedVoiceAssistantInterpreter()

    @Test
    fun `report commands resolve to open report`() {
        listOf("제보", "신고", "불편 제보", "제보해줘").forEach { command ->
            val action = interpreter.interpret(command)

            assertEquals(VoiceAssistantAction.OpenReport(), action)
        }
    }

    @Test
    fun `busan station search commands resolve to search place`() {
        listOf("부산역 찾아줘", "부산역 검색해줘").forEach { command ->
            val action = interpreter.interpret(command)

            assertEquals(VoiceAssistantAction.SearchPlace(query = "부산역"), action)
        }
    }

    @Test
    fun `unknown command resolves to unknown command action`() {
        val action = interpreter.interpret("날씨 알려줘")

        assertEquals(VoiceAssistantAction.UnknownCommand(rawCommand = "날씨 알려줘"), action)
    }
}
