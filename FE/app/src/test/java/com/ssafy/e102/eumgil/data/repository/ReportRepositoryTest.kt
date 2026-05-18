package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.data.local.dao.ReportDraftDao
import com.ssafy.e102.eumgil.data.local.dao.ReportOutboxDao
import com.ssafy.e102.eumgil.data.local.entity.ReportDraftEntity
import com.ssafy.e102.eumgil.data.local.entity.ReportOutboxEntity
import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.datasource.HazardReportsRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportDetailDto
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportListItemDto
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportPageDto
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportPointDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportRepositoryTest {
    @Test
    fun `server history coexists with unsynced outbox and suppresses submitted duplicate`() =
        runTest {
            val reportOutboxDao =
                FakeReportOutboxDao(
                    listOf(
                        reportOutboxEntity(
                            outboxId = "pending-local",
                            status = ReportOutboxStatus.Pending,
                            serverReportId = null,
                            updatedAt = 1_714_097_400_000L,
                        ),
                        reportOutboxEntity(
                            outboxId = "submitted-duplicate",
                            status = ReportOutboxStatus.Submitted,
                            serverReportId = 7L,
                            updatedAt = 1_714_097_500_000L,
                        ),
                    ),
                )
            val remoteDataSource =
                FakeHazardReportsRemoteDataSource(
                    listItems =
                        listOf(
                            hazardReportListItem(reportId = 7L, reportType = "STAIRS_STEP"),
                            hazardReportListItem(reportId = 8L, reportType = "OTHER_OBSTACLE"),
                        ),
                )
            val repository =
                DefaultReportRepository(
                    reportDraftDao = FakeReportDraftDao(),
                    reportOutboxDao = reportOutboxDao,
                    hazardReportsRemoteDataSource = remoteDataSource,
                    accessTokenProvider = { "access-token" },
                    clock = { 1_714_200_000_000L },
                )

            val history = repository.observeReportHistoryEntries().first()

            assertEquals(listOf("server:7", "server:8", "outbox:pending-local"), history.map { it.historyId })
            assertEquals(1, remoteDataSource.listRequestCount)
        }

    @Test
    fun `server detail is loaded by server history id`() =
        runTest {
            val remoteDataSource =
                FakeHazardReportsRemoteDataSource(
                    detail =
                        HazardReportDetailDto(
                            reportId = 7L,
                            reportType = "SIDEWALK_MISSING",
                            status = "APPROVED",
                            description = "인도 통행이 어렵습니다.",
                            reportPoint = HazardReportPointDto(lat = 35.1796, lng = 129.0756),
                            createdAt = "2026-04-28T17:00:00",
                            imageUrls = listOf("https://example.com/7-1.jpg"),
                        ),
                )
            val repository =
                DefaultReportRepository(
                    reportDraftDao = FakeReportDraftDao(),
                    reportOutboxDao = FakeReportOutboxDao(),
                    hazardReportsRemoteDataSource = remoteDataSource,
                    accessTokenProvider = { "access-token" },
                )

            val detail = repository.getReportHistoryDetail("server:7")

            assertEquals("SIDEWALK_MISSING", detail?.reportCategory)
            assertEquals(ReportProcessingStatus.APPROVED, detail?.processingStatus)
            assertEquals("인도 통행이 어렵습니다.", detail?.description)
            assertEquals(listOf("https://example.com/7-1.jpg"), detail?.imageRefs)
            assertEquals(7L, detail?.serverReportId)
        }

    @Test
    fun `processing counts include pending and approved but exclude rejected`() =
        runTest {
            val remoteDataSource =
                FakeHazardReportsRemoteDataSource(
                    listItems =
                        listOf(
                            hazardReportListItem(reportId = 7L, reportType = "STAIRS_STEP", status = "PENDING"),
                            hazardReportListItem(reportId = 8L, reportType = "OTHER_OBSTACLE", status = "APPROVED"),
                            hazardReportListItem(reportId = 9L, reportType = "RAMP", status = "REJECTED"),
                        ),
                )
            val repository =
                DefaultReportRepository(
                    reportDraftDao = FakeReportDraftDao(),
                    reportOutboxDao = FakeReportOutboxDao(),
                    hazardReportsRemoteDataSource = remoteDataSource,
                    accessTokenProvider = { "access-token" },
                )

            val counts = repository.observeReportProcessingCounts().first()

            assertEquals(1, counts.pending)
            assertEquals(1, counts.approved)
        }
}

private class FakeReportDraftDao : ReportDraftDao {
    private val drafts = MutableStateFlow(emptyList<ReportDraftEntity>())

    override fun observeReportDrafts(): Flow<List<ReportDraftEntity>> = drafts

    override fun observeReportDraft(draftId: String): Flow<ReportDraftEntity?> =
        MutableStateFlow(drafts.value.firstOrNull { it.draftId == draftId })

    override suspend fun getReportDraft(draftId: String): ReportDraftEntity? =
        drafts.value.firstOrNull { it.draftId == draftId }

    override suspend fun getLatestReportDraft(): ReportDraftEntity? =
        drafts.value.maxByOrNull(ReportDraftEntity::updatedAt)

    override suspend fun upsertReportDraft(reportDraft: ReportDraftEntity) {
        drafts.value = drafts.value.filterNot { it.draftId == reportDraft.draftId } + reportDraft
    }

    override suspend fun upsertReportDrafts(reportDrafts: List<ReportDraftEntity>) {
        reportDrafts.forEach { upsertReportDraft(it) }
    }

    override suspend fun deleteReportDraft(draftId: String) {
        drafts.value = drafts.value.filterNot { it.draftId == draftId }
    }

    override suspend fun clearReportDrafts() {
        drafts.value = emptyList()
    }
}

private class FakeReportOutboxDao(
    initialItems: List<ReportOutboxEntity> = emptyList(),
) : ReportOutboxDao {
    private val outboxItems = MutableStateFlow(initialItems)

    override fun observeReportOutboxItems(): Flow<List<ReportOutboxEntity>> = outboxItems

    override suspend fun getReportOutbox(outboxId: String): ReportOutboxEntity? =
        outboxItems.value.firstOrNull { it.outboxId == outboxId }

    override suspend fun upsertReportOutbox(reportOutbox: ReportOutboxEntity) {
        outboxItems.value = outboxItems.value.filterNot { it.outboxId == reportOutbox.outboxId } + reportOutbox
    }

    override suspend fun deleteReportOutbox(outboxId: String) {
        outboxItems.value = outboxItems.value.filterNot { it.outboxId == outboxId }
    }

    override suspend fun resetSubmittingOutboxesToPending(now: Long): Int {
        var resetCount = 0
        outboxItems.value =
            outboxItems.value.map { item ->
                if (item.status == "Submitting") {
                    resetCount += 1
                    item.copy(status = "Pending", updatedAt = now)
                } else {
                    item
                }
            }
        return resetCount
    }
}

private class FakeHazardReportsRemoteDataSource(
    private val listItems: List<HazardReportListItemDto> = emptyList(),
    private val detail: HazardReportDetailDto? = null,
) : HazardReportsRemoteDataSource(HttpJsonClient(baseUrl = "http://test.invalid")) {
    var listRequestCount = 0
        private set

    override suspend fun getMyHazardReports(
        accessToken: String,
        cursor: Long?,
        size: Int?,
    ): HazardReportPageDto {
        listRequestCount += 1
        return HazardReportPageDto(
            content = listItems,
            size = size ?: listItems.size,
            nextCursor = null,
            hasNext = false,
        )
    }

    override suspend fun getMyHazardReportDetail(
        accessToken: String,
        reportId: Long,
    ): HazardReportDetailDto =
        checkNotNull(detail) { "detail is not configured" }
}

private fun reportOutboxEntity(
    outboxId: String,
    status: ReportOutboxStatus,
    serverReportId: Long?,
    updatedAt: Long,
): ReportOutboxEntity =
    ReportOutboxEntity(
        outboxId = outboxId,
        reportCategory = "STAIRS_STEP",
        description = "",
        address = "부산진구 가야대로 772",
        latitude = 35.1796,
        longitude = 129.0756,
        photoUri = null,
        photoMimeType = null,
        photoSizeBytes = null,
        status = status.name,
        serverReportId = serverReportId,
        lastFailureReason = null,
        createdAt = updatedAt - 1_000L,
        updatedAt = updatedAt,
    )

private fun hazardReportListItem(
    reportId: Long,
    reportType: String,
    status: String = "PENDING",
): HazardReportListItemDto =
    HazardReportListItemDto(
        reportId = reportId,
        reportType = reportType,
        status = status,
        reportPoint = HazardReportPointDto(lat = 35.1796, lng = 129.0756),
        createdAt = "2026-04-28T17:00:00",
        representativeImageUrl = null,
    )
