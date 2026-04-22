package com.ssafy.e102.eumgil.feature.report

import com.ssafy.e102.eumgil.data.repository.ReportDraftData
import com.ssafy.e102.eumgil.data.repository.ReportOutboxData
import com.ssafy.e102.eumgil.data.repository.ReportRepository
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `save draft stores partial input and exposes saved state`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.DescriptionChanged("  보도 중앙 장애물  "))
            viewModel.onAction(ReportUiAction.SaveDraftClicked)
            advanceUntilIdle()

            val savedDraft = requireNotNull(repository.savedDraft)
            val uiState = viewModel.uiState.value

            assertEquals("보도 중앙 장애물", savedDraft.description)
            assertEquals(savedDraft.draftId, uiState.draftId)
            assertTrue(uiState.hasExistingDraft)
            assertTrue(uiState.draftSaveState is ReportDraftSaveState.Saved)
        }

    @Test
    fun `resume draft restores saved form state without auto filling on init`() =
        runTest {
            val repository =
                FakeReportRepository(
                    latestDraft =
                        ReportDraftData(
                            draftId = "draft-1",
                            reportCategory = ReportType.OBSTACLE.apiValue,
                            description = "복원할 설명",
                            address = "부산역 인근",
                            latitude = 35.1151,
                            longitude = 129.0414,
                            locationSource = ReportLocationSource.MapPin.name,
                            photoUri = "content://draft/photo.jpg",
                            photoMimeType = "image/jpeg",
                            photoSizeBytes = 1000L,
                            createdAtMillis = 10L,
                            updatedAtMillis = 20L,
                        ),
                )
            val viewModel = ReportViewModel(reportRepository = repository)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.hasExistingDraft)
            assertNull(viewModel.uiState.value.reportType.value)

            viewModel.onAction(ReportUiAction.DraftResumeClicked)
            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertEquals(ReportType.OBSTACLE, uiState.reportType.value)
            assertEquals("복원할 설명", uiState.description.value)
            assertEquals("부산역 인근", uiState.location.addressText)
            assertEquals(ReportLocationSource.MapPin, uiState.location.source)
            assertEquals("content://draft/photo.jpg", uiState.photo.value?.localUri)
        }

    @Test
    fun `discard draft deletes local draft and resets form`() =
        runTest {
            val repository =
                FakeReportRepository(
                    latestDraft =
                        ReportDraftData(
                            draftId = "draft-1",
                            reportCategory = ReportType.ROAD_CONSTRUCTION.apiValue,
                            description = "삭제할 draft",
                            address = null,
                            latitude = null,
                            longitude = null,
                            locationSource = null,
                            photoUri = null,
                            photoMimeType = null,
                            photoSizeBytes = null,
                            createdAtMillis = 10L,
                            updatedAtMillis = 20L,
                        ),
                )
            val viewModel = ReportViewModel(reportRepository = repository)
            advanceUntilIdle()

            viewModel.onAction(ReportUiAction.DraftResumeClicked)
            advanceUntilIdle()
            assertEquals(ReportType.ROAD_CONSTRUCTION, viewModel.uiState.value.reportType.value)

            viewModel.onAction(ReportUiAction.DraftDiscardClicked)
            advanceUntilIdle()

            assertEquals("draft-1", repository.deletedDraftId)
            assertEquals(ReportUiState(), viewModel.uiState.value)
        }
}

private class FakeReportRepository(
    private var latestDraft: ReportDraftData? = null,
) : ReportRepository {
    var savedDraft: ReportDraftData? = null
        private set
    var deletedDraftId: String? = null
        private set

    override suspend fun getLatestDraft(): ReportDraftData? = latestDraft

    override suspend fun saveDraft(draft: ReportDraftData): ReportDraftData {
        savedDraft = draft.copy(draftId = draft.draftId.ifBlank { "draft-1" })
        latestDraft = savedDraft
        return requireNotNull(savedDraft)
    }

    override suspend fun deleteDraft(draftId: String) {
        deletedDraftId = draftId
        if (latestDraft?.draftId == draftId) {
            latestDraft = null
        }
    }

    override suspend fun saveOutbox(outbox: ReportOutboxData): ReportOutboxData =
        outbox.copy(outboxId = outbox.outboxId.ifBlank { "outbox-1" })
}
