package com.ssafy.e102.eumgil.feature.mypage

import com.ssafy.e102.eumgil.data.repository.ReportDraftData
import com.ssafy.e102.eumgil.data.repository.ReportOutboxData
import com.ssafy.e102.eumgil.data.repository.ReportRepository
import com.ssafy.e102.eumgil.feature.report.ReportType
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyPageReportHistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `outbox items are mapped in latest updated order`() =
        runTest {
            val repository = FakeReportHistoryRepository()
            val viewModel = MyPageReportHistoryViewModel(reportRepository = repository)

            repository.emit(
                listOf(
                    reportOutbox(
                        outboxId = "old",
                        reportCategory = ReportType.STAIRS_STEP.apiValue,
                        address = "부산진구 가야대로 772 앞",
                        updatedAtMillis = 1_714_097_400_000L,
                    ),
                    reportOutbox(
                        outboxId = "new",
                        reportCategory = ReportType.OTHER_OBSTACLE.apiValue,
                        address = "부산역 1번 출구 엘리베이터",
                        updatedAtMillis = 1_714_104_000_000L,
                        photoUri = "content://reports/elevator.jpg",
                    ),
                ),
            )
            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertEquals(MyPageReportHistoryScreenState.CONTENT, uiState.screenState)
            assertEquals(listOf("new", "old"), uiState.reports.map { it.outboxId })
            assertEquals("기타 장애물", uiState.reports.first().title)
            assertEquals("부산역 1번 출구 엘리베이터", uiState.reports.first().address)
            assertEquals("content://reports/elevator.jpg", uiState.reports.first().photoUri)
            assertTrue(uiState.reports.first().submittedAtText.contains("2024.04"))
        }

    @Test
    fun `empty outbox exposes empty state`() =
        runTest {
            val repository = FakeReportHistoryRepository()
            val viewModel = MyPageReportHistoryViewModel(reportRepository = repository)

            repository.emit(emptyList())
            advanceUntilIdle()

            assertEquals(MyPageReportHistoryScreenState.EMPTY, viewModel.uiState.value.screenState)
            assertTrue(viewModel.uiState.value.reports.isEmpty())
        }

    @Test
    fun `repository failure exposes error state`() =
        runTest {
            val repository = FakeReportHistoryRepository()
            val viewModel = MyPageReportHistoryViewModel(reportRepository = repository)

            repository.fail()
            advanceUntilIdle()

            assertEquals(MyPageReportHistoryScreenState.ERROR, viewModel.uiState.value.screenState)
        }

    @Test
    fun `report cta emits navigate to report event`() =
        runTest {
            val repository = FakeReportHistoryRepository()
            val viewModel = MyPageReportHistoryViewModel(reportRepository = repository)
            val event = async { viewModel.uiEvent.first() }

            viewModel.onAction(MyPageReportHistoryUiAction.ReportCtaClicked)
            advanceUntilIdle()

            assertEquals(MyPageReportHistoryUiEvent.NavigateToReport, event.await())
        }

    @Test
    fun `back click emits navigate back event`() =
        runTest {
            val repository = FakeReportHistoryRepository()
            val viewModel = MyPageReportHistoryViewModel(reportRepository = repository)
            val event = async { viewModel.uiEvent.first() }

            viewModel.onAction(MyPageReportHistoryUiAction.BackClicked)
            advanceUntilIdle()

            assertEquals(MyPageReportHistoryUiEvent.NavigateBack, event.await())
        }
}

private class FakeReportHistoryRepository : ReportRepository {
    private val reports = MutableSharedFlow<ReportHistoryEmission>()

    override suspend fun getLatestDraft(): ReportDraftData? = null

    override suspend fun saveDraft(draft: ReportDraftData): ReportDraftData = draft

    override suspend fun deleteDraft(draftId: String) = Unit

    override suspend fun saveOutbox(outbox: ReportOutboxData): ReportOutboxData = outbox

    override fun observeReportHistory(): Flow<List<ReportOutboxData>> =
        reports.map { emission ->
            when (emission) {
                ReportHistoryEmission.Failure -> error("history load failed")
                is ReportHistoryEmission.Items -> emission.items
            }
        }

    suspend fun emit(items: List<ReportOutboxData>) {
        reports.emit(ReportHistoryEmission.Items(items))
    }

    suspend fun fail() {
        reports.emit(ReportHistoryEmission.Failure)
    }
}

private sealed interface ReportHistoryEmission {
    data class Items(
        val items: List<ReportOutboxData>,
    ) : ReportHistoryEmission

    data object Failure : ReportHistoryEmission
}

private fun reportOutbox(
    outboxId: String,
    reportCategory: String,
    address: String,
    updatedAtMillis: Long,
    photoUri: String? = null,
): ReportOutboxData =
    ReportOutboxData(
        outboxId = outboxId,
        reportCategory = reportCategory,
        description = "",
        address = address,
        latitude = 35.1796,
        longitude = 129.0756,
        photoUri = photoUri,
        photoMimeType = photoUri?.let { "image/jpeg" },
        photoSizeBytes = photoUri?.let { 1024L },
        createdAtMillis = updatedAtMillis - 1_000L,
        updatedAtMillis = updatedAtMillis,
    )
