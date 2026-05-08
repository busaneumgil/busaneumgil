package com.ssafy.e102.eumgil.feature.report

import com.ssafy.e102.eumgil.data.repository.ReportDraftData
import com.ssafy.e102.eumgil.data.repository.ReportOutboxData
import com.ssafy.e102.eumgil.data.repository.ReportRepository
import com.ssafy.e102.eumgil.data.repository.ReportSubmitFailureReason
import com.ssafy.e102.eumgil.data.repository.ReportSubmitResult
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
                            reportCategory = ReportType.OTHER_OBSTACLE.apiValue,
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

            assertEquals(ReportType.OTHER_OBSTACLE, uiState.reportType.value)
            assertEquals("복원할 설명", uiState.description.value)
            assertEquals("부산역 인근", uiState.location.addressText)
            assertEquals(ReportLocationSource.MapPin, uiState.location.source)
            assertEquals("content://draft/photo.jpg", uiState.photo.values.firstOrNull()?.localUri)
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
                            reportCategory = ReportType.OTHER_OBSTACLE.apiValue,
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
            assertEquals(ReportType.OTHER_OBSTACLE, viewModel.uiState.value.reportType.value)

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

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.BRAILLE_BLOCK))
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

            assertEquals(ReportType.BRAILLE_BLOCK.apiValue, savedOutbox.reportCategory)
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

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.OTHER_OBSTACLE))
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

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.OTHER_OBSTACLE))
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

            assertEquals(ReportType.OTHER_OBSTACLE, uiState.reportType.value)
            assertEquals("장애물", uiState.description.value)
            assertTrue(uiState.screenState is ReportScreenState.Failure)
            assertTrue(uiState.submitState is ReportSubmitState.Failed)
            assertTrue(uiState.outboxState is ReportOutboxState.Failed)
        }

    @Test
    fun `submit success with server reportId completes flow with returned reportId`() =
        runTest {
            val repository =
                FakeReportRepository(
                    submitResultFactory = { outboxId ->
                        ReportSubmitResult.Success(outboxId = outboxId, serverReportId = 42L)
                    },
                )
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.RAMP))
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

            assertTrue(uiState.screenState is ReportScreenState.Completed)
            val submit = uiState.submitState
            assertTrue(submit is ReportSubmitState.Success)
            assertEquals(42L, (submit as ReportSubmitState.Success).reportId)
            assertTrue(uiState.outboxState is ReportOutboxState.Saved)
            assertEquals(listOf("outbox-1"), repository.submittedOutboxIds)
        }

    @Test
    fun `server submit failure keeps outbox saved and surfaces retryable failure`() =
        runTest {
            val repository =
                FakeReportRepository(
                    submitResultFactory = { outboxId ->
                        ReportSubmitResult.Failure(
                            outboxId = outboxId,
                            reason = ReportSubmitFailureReason.Network,
                        )
                    },
                )
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.SIDEWALK_MISSING))
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

            assertTrue(uiState.screenState is ReportScreenState.Failure)
            assertEquals(
                ReportFailureReason.NetworkUnavailable,
                (uiState.screenState as ReportScreenState.Failure).reason,
            )
            val submit = uiState.submitState
            assertTrue(submit is ReportSubmitState.Failed)
            assertEquals(
                ReportFailureReason.NetworkUnavailable,
                (submit as ReportSubmitState.Failed).reason,
            )
            assertTrue(uiState.outboxState is ReportOutboxState.Saved)
        }

    @Test
    fun `retry after server failure reuses same outboxId without saving outbox again`() =
        runTest {
            var attempt = 0
            val repository =
                FakeReportRepository(
                    submitResultFactory = { outboxId ->
                        attempt += 1
                        if (attempt == 1) {
                            ReportSubmitResult.Failure(
                                outboxId = outboxId,
                                reason = ReportSubmitFailureReason.Network,
                            )
                        } else {
                            ReportSubmitResult.Success(outboxId = outboxId, serverReportId = 7L)
                        }
                    },
                )
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.RAMP))
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

            val firstOutboxId = requireNotNull(repository.savedOutbox).outboxId

            viewModel.onAction(ReportUiAction.RetrySubmitClicked)
            advanceUntilIdle()

            assertEquals(2, repository.submittedOutboxIds.size)
            assertEquals(firstOutboxId, repository.submittedOutboxIds[0])
            assertEquals(firstOutboxId, repository.submittedOutboxIds[1])
            val uiState = viewModel.uiState.value
            assertTrue(uiState.screenState is ReportScreenState.Completed)
            val submit = uiState.submitState
            assertTrue(submit is ReportSubmitState.Success)
            assertEquals(7L, (submit as ReportSubmitState.Success).reportId)
        }

    @Test
    fun `selecting report type advances step to LocationConfirm`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            assertEquals(ReportStep.TypeSelection, viewModel.uiState.value.currentStep)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.STAIRS_STEP))
            advanceUntilIdle()

            assertEquals(ReportStep.LocationConfirm, viewModel.uiState.value.currentStep)
            assertEquals(ReportType.STAIRS_STEP, viewModel.uiState.value.reportType.value)
        }

    @Test
    fun `next step click on location confirm with valid location advances to detail input`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.RAMP))
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
            viewModel.onAction(ReportUiAction.NextStepClicked)
            advanceUntilIdle()

            assertEquals(ReportStep.DetailInput, viewModel.uiState.value.currentStep)
        }

    @Test
    fun `next step click on location confirm without location stays on same step`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.OTHER_OBSTACLE))
            viewModel.onAction(ReportUiAction.NextStepClicked)
            advanceUntilIdle()

            assertEquals(ReportStep.LocationConfirm, viewModel.uiState.value.currentStep)
        }

    @Test
    fun `back click on intermediate step moves to previous step without navigating`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.BRAILLE_BLOCK))
            assertEquals(ReportStep.LocationConfirm, viewModel.uiState.value.currentStep)

            viewModel.onAction(ReportUiAction.BackClicked)
            advanceUntilIdle()

            assertEquals(ReportStep.TypeSelection, viewModel.uiState.value.currentStep)
        }

    @Test
    fun `back click on type selection emits NavigateBack`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)
            val event = async { viewModel.uiEvent.first() }

            viewModel.onAction(ReportUiAction.BackClicked)
            advanceUntilIdle()

            assertEquals(ReportUiEvent.NavigateBack, event.await())
            assertEquals(ReportStep.TypeSelection, viewModel.uiState.value.currentStep)
        }

    @Test
    fun `successful submit sets current step to Complete`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.OTHER_OBSTACLE))
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

            assertEquals(ReportStep.Complete, viewModel.uiState.value.currentStep)
        }

    @Test
    fun `report history click after complete resets form for next report`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)
            val event =
                async {
                    viewModel.uiEvent.first { emittedEvent ->
                        emittedEvent is ReportUiEvent.NavigateToReportHistory
                    }
                }

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.OTHER_OBSTACLE))
            viewModel.onAction(
                ReportUiAction.LocationSelected(
                    location =
                        ReportLocation(
                            latitude = 35.1796,
                            longitude = 129.0756,
                            address = "éºÂ€?ê³—ë–†ï§£??ë©¸ë ",
                        ),
                    source = ReportLocationSource.MapPin,
                ),
            )
            viewModel.onAction(ReportUiAction.SubmitClicked)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.screenState is ReportScreenState.Completed)

            viewModel.onAction(ReportUiAction.ReportHistoryClicked)
            advanceUntilIdle()

            assertEquals(ReportUiEvent.NavigateToReportHistory, event.await())
            assertEquals(ReportUiState(), viewModel.uiState.value)
        }

    @Test
    fun `resume draft with location jumps to DetailInput step`() =
        runTest {
            val repository =
                FakeReportRepository(
                    latestDraft =
                        ReportDraftData(
                            draftId = "draft-1",
                            reportCategory = ReportType.STAIRS_STEP.apiValue,
                            description = "복원할 설명",
                            address = "부산역 인근",
                            latitude = 35.1151,
                            longitude = 129.0414,
                            locationSource = ReportLocationSource.MapPin.name,
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

            assertEquals(ReportStep.DetailInput, viewModel.uiState.value.currentStep)
        }

    @Test
    fun `adding photos appends to list and updates count`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.PhotoAddClicked)
            viewModel.onAction(ReportUiAction.PhotoAddClicked)
            viewModel.onAction(ReportUiAction.PhotoAddClicked)
            advanceUntilIdle()

            val photoInput = viewModel.uiState.value.photo
            assertEquals(3, photoInput.count)
            assertNull(photoInput.error)
            assertTrue(photoInput.canAddMore)
        }

    @Test
    fun `adding more than max photos is capped without TooMany error via UI path`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            repeat(ReportFormLimits.PHOTO_MAX_COUNT + 2) {
                viewModel.onAction(ReportUiAction.PhotoAddClicked)
            }
            advanceUntilIdle()

            val photoInput = viewModel.uiState.value.photo
            assertEquals(ReportFormLimits.PHOTO_MAX_COUNT, photoInput.count)
            assertFalse(photoInput.canAddMore)
            assertNull(photoInput.error)
        }

    @Test
    fun `removing photo at index drops only that entry`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.PhotoAddClicked)
            viewModel.onAction(ReportUiAction.PhotoAddClicked)
            viewModel.onAction(ReportUiAction.PhotoAddClicked)
            advanceUntilIdle()
            val before = viewModel.uiState.value.photo.values
            assertEquals(3, before.size)
            val targetUri = before[1].localUri

            viewModel.onAction(ReportUiAction.PhotoRemovedAt(1))
            advanceUntilIdle()

            val after = viewModel.uiState.value.photo.values
            assertEquals(2, after.size)
            assertFalse(after.any { it.localUri == targetUri })
        }

    @Test
    fun `outbox saves only first photo when multiple are attached`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.STAIRS_STEP))
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
            viewModel.onAction(ReportUiAction.PhotoAddClicked)
            viewModel.onAction(ReportUiAction.PhotoAddClicked)
            viewModel.onAction(ReportUiAction.PhotoAddClicked)
            viewModel.onAction(ReportUiAction.SubmitClicked)
            advanceUntilIdle()

            val savedOutbox = requireNotNull(repository.savedOutbox)
            val firstPhoto = viewModel.uiState.value.photo.values.first()
            assertEquals(firstPhoto.localUri, savedOutbox.photoUri)
        }

    @Test
    fun `description max length 300 marks error when exceeded`() =
        runTest {
            val repository = FakeReportRepository()
            val viewModel = ReportViewModel(reportRepository = repository)

            val longText = "가".repeat(ReportFormLimits.DESCRIPTION_MAX_LENGTH + 1)
            viewModel.onAction(ReportUiAction.DescriptionChanged(longText))
            advanceUntilIdle()

            val descError = viewModel.uiState.value.description.error
            assertEquals(ReportDescriptionError.TooLong, descError)
        }

    @Test
    fun `editing after outbox failure clears retry failure state`() =
        runTest {
            val repository = FakeReportRepository(failOutbox = true)
            val viewModel = ReportViewModel(reportRepository = repository)

            viewModel.onAction(ReportUiAction.ReportTypeSelected(ReportType.OTHER_OBSTACLE))
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
    private val submitResultFactory: (String) -> ReportSubmitResult = { _ ->
        ReportSubmitResult.Skipped
    },
) : ReportRepository {
    var savedDraft: ReportDraftData? = null
        private set
    var savedOutbox: ReportOutboxData? = null
        private set
    var deletedDraftId: String? = null
        private set
    var submittedOutboxIds: MutableList<String> = mutableListOf()
        private set

    override fun observeReportHistory(): Flow<List<ReportOutboxData>> = flowOf(emptyList())

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

    override suspend fun submitOutboxToServer(outboxId: String): ReportSubmitResult {
        submittedOutboxIds.add(outboxId)
        return submitResultFactory(outboxId)
    }
}
