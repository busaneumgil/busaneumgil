package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeHistoryItem
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeIntent
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeMode
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeResult
import com.ssafy.e102.eumgil.data.mock.datasource.MockVoiceAnalyzeRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.VoiceAnalyzeRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.VoiceAnalyzeHistoryDto
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryDomain
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySource
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySourcePolicy

interface VoiceAnalyzeRepository {
    suspend fun analyze(
        text: String,
        mode: VoiceAnalyzeMode,
        history: List<VoiceAnalyzeHistoryItem> = emptyList(),
    ): VoiceAnalyzeResult
}

class DefaultVoiceAnalyzeRepository(
    private val remoteDataSource: VoiceAnalyzeRemoteDataSource,
    private val mockDataSource: MockVoiceAnalyzeRemoteDataSource,
    private val sourcePolicy: RepositorySourcePolicy,
) : VoiceAnalyzeRepository {

    override suspend fun analyze(
        text: String,
        mode: VoiceAnalyzeMode,
        history: List<VoiceAnalyzeHistoryItem>,
    ): VoiceAnalyzeResult {
        val readPlan = sourcePolicy.readPlan(RepositoryDomain.VOICE_ANALYZE)
        val historyDtos = history.map { VoiceAnalyzeHistoryDto(role = it.role, content = it.content) }
        var remoteFailure: Throwable? = null

        for (source in readPlan.sources) {
            when (source) {
                RepositorySource.REMOTE -> {
                    val result = runCatching {
                        remoteDataSource.analyze(
                            text = text,
                            mode = mode.name,
                            history = historyDtos,
                        )
                    }
                    if (result.isSuccess) {
                        return result.getOrThrow().toVoiceAnalyzeResult()
                    }
                    remoteFailure = result.exceptionOrNull()
                }

                RepositorySource.MOCK -> {
                    return mockDataSource
                        .analyze(text = text, mode = mode.name, history = historyDtos)
                        .toVoiceAnalyzeResult()
                }

                RepositorySource.LOCAL -> {
                    // LOCAL source is not supported for voice analyze — skip
                }
            }
        }

        throw remoteFailure
            ?: IllegalStateException("No voice analyze data source matched the current policy.")
    }

    private fun com.ssafy.e102.eumgil.data.remote.dto.VoiceAnalyzeResponseDto.toVoiceAnalyzeResult(): VoiceAnalyzeResult =
        VoiceAnalyzeResult(
            intent = runCatching { enumValueOf<VoiceAnalyzeIntent>(intent) }.getOrDefault(VoiceAnalyzeIntent.UNKNOWN),
            placeName = placeName,
            category = category,
            bookmarkAction = bookmarkAction,
            departure = departure,
            destination = destination,
            reportType = reportType,
            description = description,
            confirmed = confirmed,
            confirmationMessage = confirmationMessage,
        )
}
