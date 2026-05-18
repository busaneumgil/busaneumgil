package com.ssafy.e102.eumgil.feature.voiceassistant

import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeHistoryItem
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeIntent
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeMode
import com.ssafy.e102.eumgil.core.model.toJsonString
import com.ssafy.e102.eumgil.data.repository.VoiceAnalyzeRepository

interface VoiceAssistantInterpreter {
    suspend fun interpret(
        transcript: String,
        context: VoiceAssistantContext = VoiceAssistantContext(),
    ): VoiceAssistantAction
}

class RuleBasedVoiceAssistantInterpreter : VoiceAssistantInterpreter {
    override suspend fun interpret(
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

class AiVoiceAssistantInterpreter(
    private val voiceAnalyzeRepository: VoiceAnalyzeRepository,
) : VoiceAssistantInterpreter {

    private val conversationHistory = mutableListOf<VoiceAnalyzeHistoryItem>()

    override suspend fun interpret(
        transcript: String,
        context: VoiceAssistantContext,
    ): VoiceAssistantAction {
        // 현재 발화 추가 전 snapshot 저장 — AI 서버에서 text와 history 중복 방지
        val historySnapshot = conversationHistory.toList()

        conversationHistory.add(VoiceAnalyzeHistoryItem(role = "user", content = transcript))

        val result = voiceAnalyzeRepository.analyze(
            text = transcript,
            mode = VoiceAnalyzeMode.MOBILITY_IMPAIRED,
            history = historySnapshot,
            currentRoute = context.currentRoute,
        )

        conversationHistory.add(
            VoiceAnalyzeHistoryItem(role = "assistant", content = result.toJsonString())
        )

        return when (result.intent) {
            VoiceAnalyzeIntent.PLACE_SEARCH ->
                VoiceAssistantAction.SearchPlace(
                    query = result.placeName.orEmpty(),
                    editingTarget = context.editingTarget,
                )
            VoiceAnalyzeIntent.CATEGORY_SEARCH ->
                VoiceAssistantAction.CategorySearch(category = result.category.orEmpty())
            VoiceAnalyzeIntent.NAVIGATE ->
                VoiceAssistantAction.Navigate(
                    departure = result.departure,
                    destination = result.destination.orEmpty(),
                )
            VoiceAnalyzeIntent.SHOW_BOOKMARKS ->
                VoiceAssistantAction.ShowBookmarks()
            VoiceAnalyzeIntent.SHOW_FAVORITE_ROUTES ->
                VoiceAssistantAction.OpenSavedRoutes()
            VoiceAnalyzeIntent.LOGOUT ->
                VoiceAssistantAction.Logout()
            VoiceAnalyzeIntent.REPORT ->
                VoiceAssistantAction.OpenReport()
            VoiceAnalyzeIntent.NAVIGATION_END ->
                VoiceAssistantAction.StopNavigation()
            VoiceAnalyzeIntent.OPEN_MY_PAGE ->
                VoiceAssistantAction.OpenMyPage()
            VoiceAnalyzeIntent.OPEN_MAP ->
                VoiceAssistantAction.OpenMap()
            VoiceAnalyzeIntent.ASK ->
                VoiceAssistantAction.Ask(message = result.confirmationMessage.orEmpty())
            VoiceAnalyzeIntent.UNKNOWN -> {
                conversationHistory.clear()
                VoiceAssistantAction.UnknownCommand(rawCommand = transcript)
            }
            else -> {
                conversationHistory.clear()
                VoiceAssistantAction.UnknownCommand(rawCommand = transcript)
            }
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }
}
