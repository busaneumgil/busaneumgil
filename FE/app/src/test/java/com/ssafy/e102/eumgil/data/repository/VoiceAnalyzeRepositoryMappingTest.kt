package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeHistoryItem
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeIntent
import com.ssafy.e102.eumgil.core.model.VoiceAnalyzeMode
import com.ssafy.e102.eumgil.data.mock.datasource.MockVoiceAnalyzeRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.VoiceAnalyzeRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.VoiceAnalyzeHistoryDto
import com.ssafy.e102.eumgil.data.remote.dto.VoiceAnalyzeResponseDto
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryDomain
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryReadPlan
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySource
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySourcePolicy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceAnalyzeRepositoryMappingTest {

    private val alwaysRemotePolicy = object : RepositorySourcePolicy {
        override suspend fun readPlan(domain: RepositoryDomain) =
            RepositoryReadPlan(sources = listOf(RepositorySource.REMOTE))
    }

    private fun repositoryWith(dto: VoiceAnalyzeResponseDto): DefaultVoiceAnalyzeRepository {
        val remote = object : VoiceAnalyzeRemoteDataSource {
            override suspend fun analyze(
                text: String,
                mode: String,
                history: List<VoiceAnalyzeHistoryDto>,
                currentRoute: String?,
            ) = dto
        }
        return DefaultVoiceAnalyzeRepository(
            remoteDataSource = remote,
            mockDataSource = MockVoiceAnalyzeRemoteDataSource(),
            sourcePolicy = alwaysRemotePolicy,
        )
    }

    // ── 기존 필드 매핑 유지 확인 ──────────────────────────────

    @Test
    fun `existing fields placeName confirmed confirmationMessage are mapped correctly`() = runTest {
        val dto = VoiceAnalyzeResponseDto(
            intent = "PLACE_SEARCH",
            placeName = "부산역",
            category = null,
            bookmarkAction = null,
            departure = null,
            destination = null,
            reportType = null,
            description = null,
            confirmed = true,
            confirmationMessage = "부산역을 찾으시나요?",
        )
        val result = repositoryWith(dto).analyze("부산역", VoiceAnalyzeMode.LOW_VISION)

        assertEquals(VoiceAnalyzeIntent.PLACE_SEARCH, result.intent)
        assertEquals("부산역", result.placeName)
        assertEquals(true, result.confirmed)
        assertEquals("부산역을 찾으시나요?", result.confirmationMessage)
    }

    // ── 신규 필드 6개 매핑 확인 ──────────────────────────────

    @Test
    fun `new field category is mapped from dto to result`() = runTest {
        val dto = VoiceAnalyzeResponseDto(
            intent = "CATEGORY_SEARCH",
            placeName = null,
            category = "음식점",
            bookmarkAction = null,
            departure = null,
            destination = null,
            reportType = null,
            description = null,
            confirmed = true,
            confirmationMessage = null,
        )
        val result = repositoryWith(dto).analyze("음식점 찾아줘", VoiceAnalyzeMode.LOW_VISION)

        assertEquals("음식점", result.category)
    }

    @Test
    fun `new field bookmarkAction is mapped from dto to result`() = runTest {
        val dto = VoiceAnalyzeResponseDto(
            intent = "BOOKMARK_ADD",
            placeName = "부산역",
            category = null,
            bookmarkAction = "add",
            departure = null,
            destination = null,
            reportType = null,
            description = null,
            confirmed = true,
            confirmationMessage = null,
        )
        val result = repositoryWith(dto).analyze("부산역 저장해줘", VoiceAnalyzeMode.LOW_VISION)

        assertEquals("add", result.bookmarkAction)
    }

    @Test
    fun `new fields departure and destination are mapped from dto to result`() = runTest {
        val dto = VoiceAnalyzeResponseDto(
            intent = "NAVIGATE",
            placeName = null,
            category = null,
            bookmarkAction = null,
            departure = "서면",
            destination = "해운대",
            reportType = null,
            description = null,
            confirmed = true,
            confirmationMessage = null,
        )
        val result = repositoryWith(dto).analyze("해운대 가줘", VoiceAnalyzeMode.LOW_VISION)

        assertEquals("서면", result.departure)
        assertEquals("해운대", result.destination)
    }

    @Test
    fun `new fields reportType and description are mapped from dto to result`() = runTest {
        val dto = VoiceAnalyzeResponseDto(
            intent = "REPORT",
            placeName = null,
            category = null,
            bookmarkAction = null,
            departure = null,
            destination = null,
            reportType = "STAIRS_STEP",
            description = "계단 있음",
            confirmed = null,
            confirmationMessage = null,
        )
        val result = repositoryWith(dto).analyze("제보할게요", VoiceAnalyzeMode.MOBILITY_IMPAIRED)

        assertEquals("STAIRS_STEP", result.reportType)
        assertEquals("계단 있음", result.description)
    }

    @Test
    fun `null new fields are preserved as null in result`() = runTest {
        val dto = VoiceAnalyzeResponseDto(
            intent = "PLACE_SEARCH",
            placeName = "부산역",
            category = null,
            bookmarkAction = null,
            departure = null,
            destination = null,
            reportType = null,
            description = null,
            confirmed = null,
            confirmationMessage = null,
        )
        val result = repositoryWith(dto).analyze("부산역", VoiceAnalyzeMode.LOW_VISION)

        assertNull(result.category)
        assertNull(result.bookmarkAction)
        assertNull(result.departure)
        assertNull(result.destination)
        assertNull(result.reportType)
        assertNull(result.description)
    }

    // ── 알 수 없는 intent는 UNKNOWN으로 폴백 ─────────────────

    @Test
    fun `unrecognized intent string falls back to UNKNOWN`() = runTest {
        val dto = VoiceAnalyzeResponseDto(
            intent = "NOT_EXIST",
            placeName = null,
            category = null,
            bookmarkAction = null,
            departure = null,
            destination = null,
            reportType = null,
            description = null,
            confirmed = null,
            confirmationMessage = null,
        )
        val result = repositoryWith(dto).analyze("알수없는말", VoiceAnalyzeMode.LOW_VISION)

        assertEquals(VoiceAnalyzeIntent.UNKNOWN, result.intent)
    }
}