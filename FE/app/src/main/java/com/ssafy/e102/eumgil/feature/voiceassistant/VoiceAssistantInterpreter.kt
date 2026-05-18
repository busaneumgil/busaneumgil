package com.ssafy.e102.eumgil.feature.voiceassistant

interface VoiceAssistantInterpreter {
    fun interpret(
        transcript: String,
        context: VoiceAssistantContext = VoiceAssistantContext(),
    ): VoiceAssistantAction
}

class RuleBasedVoiceAssistantInterpreter : VoiceAssistantInterpreter {
    override fun interpret(
        transcript: String,
        context: VoiceAssistantContext,
    ): VoiceAssistantAction {
        val command = transcript.normalizedCommand()
        return when {
            command in reportCommands -> VoiceAssistantAction.OpenReport()
            command in busanStationSearchCommands ->
                VoiceAssistantAction.SearchPlace(
                    query = BUSAN_STATION_QUERY,
                    editingTarget = context.editingTarget,
                )

            else -> VoiceAssistantAction.UnknownCommand(rawCommand = command)
        }
    }

    private fun String.normalizedCommand(): String =
        trim()
            .replace(WHITESPACE_REGEX, " ")

    private companion object {
        const val BUSAN_STATION_QUERY = "부산역"

        val WHITESPACE_REGEX = Regex("\\s+")

        val reportCommands =
            setOf(
                "제보",
                "신고",
                "불편 제보",
                "제보해줘",
            )

        val busanStationSearchCommands =
            setOf(
                "부산역 찾아줘",
                "부산역 검색해줘",
            )
    }
}
