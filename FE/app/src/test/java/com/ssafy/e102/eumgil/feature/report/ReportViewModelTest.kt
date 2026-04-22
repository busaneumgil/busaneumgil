package com.ssafy.e102.eumgil.feature.report

import com.ssafy.e102.eumgil.data.repository.ReportDraftData
import com.ssafy.e102.eumgil.data.repository.ReportOutboxData
import com.ssafy.e102.eumgil.data.repository.ReportRepository
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `editing while draft save is pending does not mark current input saved`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.DescriptionChanged("처음 입력"))
            viewModel.onAction(ReportUiAction.SaveDraftClicked)
            viewModel.onAction(ReportUiAction.DescriptionChanged("수정된 입력"))
            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertEquals("처음 입력", repository.savedDraft?.description)
            assertEquals("수정된 입력", uiState.description.value)
            assertEquals(ReportDraftSaveState.Idle, uiState.draftSaveState)
            assertTrue(uiState.hasExistingDraft)
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
    fun `resume partial draft does not expose required error before submit or blur`() =
        runTest {
            val repository =
                FakeReportRepository(
                    latestDraft =
                        ReportDraftData(
                            draftId = "draft-1",
                            reportCategory = null,
                            description = "유형 없이 저장된 draft",
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

            val uiState = viewModel.uiState.value

            assertNull(uiState.reportType.value)
            assertNull(uiState.reportType.error)
            assertFalse(uiState.isSubmitEnabled)
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

    @Test
    fun `invalid submit marks errors and does not save outbox`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)
            val event = async { viewModel.uiEvent.first() }

            viewModel.onAction(ReportUiAction.SubmitClicked)
            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertNull(repository.savedOutbox)
            assertEquals(ReportTypeError.Required, uiState.reportType.error)
            assertEquals(ReportLocationError.Required, uiState.location.error)
            assertFalse(uiState.isSubmitEnabled)
            assertEquals(ReportUiEvent.ScrollToFirstError, event.await())
        }

    @Test
    fun `valid submit saves outbox clears draft and emits complete event`() =
        runTest {
            val repository =
                FakeReportRepository(
                    latestDraft =
                        ReportDraftData(
                            draftId = "draft-1",
                            reportCategory = null,
                            description = "",
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
            val event =
                async {
                    viewModel.uiEvent.first { emittedEvent ->
                        emittedEvent is ReportUiEvent.NavigateToReportComplete
                    }
                }

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.TACTILE_BLOCK_DAMAGE))
            viewModel.onAction(
                ReportUiAction.LocationSelected(
                    location =
                        ReportLocation(
                            latitude = 35.1796,
                            longitude = 129.0756,
                            address = "부산시청 인근",
                        ),
                    source = ReportLocationSource.MapPin,
                ),
            )
            viewModel.onAction(ReportUiAction.DescriptionChanged("  점자블록 파손  "))
            viewModel.onAction(ReportUiAction.SubmitClicked)
            advanceUntilIdle()

            val savedOutbox = requireNotNull(repository.savedOutbox)
            val uiState = viewModel.uiState.value
            val completeEvent = event.await() as ReportUiEvent.NavigateToReportComplete

            assertEquals(ReportType.TACTILE_BLOCK_DAMAGE.apiValue, savedOutbox.reportCategory)
            assertEquals("점자블록 파손", savedOutbox.description)
            assertEquals(35.1796, savedOutbox.latitude, 0.0)
            assertEquals(129.0756, savedOutbox.longitude, 0.0)
            assertEquals("draft-1", repository.deletedDraftId)
            assertNull(uiState.draftId)
            assertFalse(uiState.hasExistingDraft)
            assertTrue(uiState.screenState is ReportScreenState.Completed)
            assertTrue(uiState.submitState is ReportSubmitState.Success)
            assertTrue(uiState.outboxState is ReportOutboxState.Saved)
            assertEquals("outbox-1", completeEvent.outboxId)
        }

    @Test
    fun `valid submit keeps draft state when outbox succeeds but draft delete fails`() =
        runTest {
            val repository =
                FakeReportRepository(
                    latestDraft =
                        ReportDraftData(
                            draftId = "draft-1",
                            reportCategory = null,
                            description = "",
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
                    failDeleteDraft = true,
                )
            val viewModel = ReportViewModel(reportRepository = repository)
            advanceUntilIdle()

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.OBSTACLE))
            viewModel.onAction(
                ReportUiAction.LocationSelected(
                    location =
                        ReportLocation(
                            latitude = 35.1796,
                            longitude = 129.0756,
                            address = "부산시청 인근",
                        ),
                    source = ReportLocationSource.MapPin,
                ),
            )
            viewModel.onAction(ReportUiAction.SubmitClicked)
            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertEquals("draft-1", repository.deletedDraftId)
            assertEquals("draft-1", uiState.draftId)
            assertTrue(uiState.hasExistingDraft)
            assertTrue(uiState.screenState is ReportScreenState.Completed)
            assertTrue(uiState.outboxState is ReportOutboxState.Saved)
            assertTrue(uiState.draftSaveState is ReportDraftSaveState.Failed)
        }

    @Test
    fun `outbox failure keeps input and exposes retryable failure state`() =
        runTest {
            val repository = FakeReportRepository(failOutbox = true)
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.OBSTACLE))
            viewModel.onAction(
                ReportUiAction.LocationSelected(
                    location =
                        ReportLocation(
                            latitude = 35.1796,
                            longitude = 129.0756,
                            address = "부산시청 인근",
                        ),
                    source = ReportLocationSource.MapPin,
                ),
            )
            viewModel.onAction(ReportUiAction.DescriptionChanged("장애물"))
            viewModel.onAction(ReportUiAction.SubmitClicked)
            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertEquals(ReportType.OBSTACLE, uiState.reportType.value)
            assertEquals("장애물", uiState.description.value)
            assertTrue(uiState.screenState is ReportScreenState.Failure)
            assertTrue(uiState.submitState is ReportSubmitState.Failed)
            assertTrue(uiState.outboxState is ReportOutboxState.Failed)
        }

    @Test
    fun `editing after outbox failure clears retry failure state`() =
        runTest {
            val repository = FakeReportRepository(failOutbox = true)
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.OBSTACLE))
            viewModel.onAction(
                ReportUiAction.LocationSelected(
                    location =
                        ReportLocation(
                            latitude = 35.1796,
                            longitude = 129.0756,
                            address = "부산시청 인근",
                        ),
                    source = ReportLocationSource.MapPin,
                ),
            )
            viewModel.onAction(ReportUiAction.DescriptionChanged("장애물"))
            viewModel.onAction(ReportUiAction.SubmitClicked)
            advanceUntilIdle()

            viewModel.onAction(ReportUiAction.DescriptionChanged("장애물 위치 변경"))
            val uiState = viewModel.uiState.value

            assertTrue(uiState.screenState is ReportScreenState.Editing)
            assertEquals(ReportSubmitState.Idle, uiState.submitState)
            assertEquals(ReportOutboxState.NotSaved, uiState.outboxState)
            assertTrue(uiState.isSubmitEnabled)
        }
}

private class FakeReportRepository(
    private var latestDraft: ReportDraftData? = null,
    private val failOutbox: Boolean = false,
    private val failDeleteDraft: Boolean = false,
) : ReportRepository {
    var savedDraft: ReportDraftData? = null
        private set
    var savedOutbox: ReportOutboxData? = null
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
        if (failDeleteDraft) {
            error("draft delete failed")
        }
        if (latestDraft?.draftId == draftId) {
            latestDraft = null
        }
    }

    override suspend fun saveOutbox(outbox: ReportOutboxData): ReportOutboxData =
        if (failOutbox) {
            error("outbox save failed")
        } else {
            outbox.copy(outboxId = outbox.outboxId.ifBlank { "outbox-1" }).also { saved ->
                savedOutbox = saved
            }
        }
}
