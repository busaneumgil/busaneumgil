package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.data.local.dao.ReportDraftDao
import com.ssafy.e102.eumgil.data.local.dao.ReportOutboxDao
import com.ssafy.e102.eumgil.data.local.entity.ReportDraftEntity
import com.ssafy.e102.eumgil.data.local.entity.ReportOutboxEntity
import com.ssafy.e102.eumgil.data.remote.datasource.HazardReportsApiException
import com.ssafy.e102.eumgil.data.remote.datasource.HazardReportsRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.CreateHazardReportRequestDto
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportPointDto
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ReportRepository {
    fun observeReportHistory(): Flow<List<ReportOutboxData>>

    suspend fun getLatestDraft(): ReportDraftData?

    suspend fun saveDraft(draft: ReportDraftData): ReportDraftData

    suspend fun deleteDraft(draftId: String)

    suspend fun saveOutbox(outbox: ReportOutboxData): ReportOutboxData

    suspend fun submitOutboxToServer(outboxId: String): ReportSubmitResult
}

data class ReportDraftData(
    val draftId: String,
    val reportCategory: String?,
    val description: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationSource: String?,
    val photoUri: String?,
    val photoMimeType: String?,
    val photoSizeBytes: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class ReportOutboxData(
    val outboxId: String,
    val reportCategory: String,
    val description: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val photoUri: String?,
    val photoMimeType: String?,
    val photoSizeBytes: Long?,
    val status: ReportOutboxStatus = ReportOutboxStatus.Pending,
    val serverReportId: Long? = null,
    val lastFailureReason: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

enum class ReportOutboxStatus {
    Pending,
    Submitting,
    Submitted,
    Failed,
}

sealed interface ReportSubmitResult {
    data class Success(
        val outboxId: String,
        val serverReportId: Long,
    ) : ReportSubmitResult

    data class Failure(
        val outboxId: String,
        val reason: ReportSubmitFailureReason,
    ) : ReportSubmitResult

    data object Skipped : ReportSubmitResult
}

enum class ReportSubmitFailureReason {
    Unauthorized,
    InvalidInput,
    Network,
    Unknown,
}

class DefaultReportRepository(
    private val reportDraftDao: ReportDraftDao,
    private val reportOutboxDao: ReportOutboxDao,
    private val hazardReportsRemoteDataSource: HazardReportsRemoteDataSource? = null,
    private val accessTokenProvider: suspend () -> String? = { null },
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ReportRepository {
    override fun observeReportHistory(): Flow<List<ReportOutboxData>> =
        reportOutboxDao.observeReportOutboxItems().map { outboxItems ->
            outboxItems.map(ReportOutboxEntity::toData)
        }

    override suspend fun getLatestDraft(): ReportDraftData? =
        reportDraftDao.getLatestReportDraft()?.toData()

    override suspend fun saveDraft(draft: ReportDraftData): ReportDraftData {
        val now = clock()
        val draftId = draft.draftId.ifBlank(idFactory)
        val createdAtMillis = draft.createdAtMillis.takeIf { it > 0L } ?: now
        val savedDraft =
            draft.copy(
                draftId = draftId,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = now,
            )

        reportDraftDao.upsertReportDraft(savedDraft.toEntity())
        return savedDraft
    }

    override suspend fun deleteDraft(draftId: String) {
        reportDraftDao.deleteReportDraft(draftId)
    }

    override suspend fun saveOutbox(outbox: ReportOutboxData): ReportOutboxData {
        val now = clock()
        val outboxId = outbox.outboxId.ifBlank(idFactory)
        val createdAtMillis = outbox.createdAtMillis.takeIf { it > 0L } ?: now
        val savedOutbox =
            outbox.copy(
                outboxId = outboxId,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = now,
            )

        reportOutboxDao.upsertReportOutbox(savedOutbox.toEntity())
        return savedOutbox
    }

    override suspend fun submitOutboxToServer(outboxId: String): ReportSubmitResult {
        val datasource = hazardReportsRemoteDataSource ?: return ReportSubmitResult.Skipped
        val token = accessTokenProvider() ?: return failOutbox(outboxId, ReportSubmitFailureReason.Unauthorized)

        val outboxEntity =
            reportOutboxDao.getReportOutbox(outboxId) ?: return ReportSubmitResult.Skipped

        if (outboxEntity.status == ReportOutboxStatus.Submitted.name && outboxEntity.serverReportId != null) {
            return ReportSubmitResult.Success(
                outboxId = outboxEntity.outboxId,
                serverReportId = outboxEntity.serverReportId,
            )
        }

        markOutboxStatus(outboxEntity, ReportOutboxStatus.Submitting, lastFailureReason = null)

        return runCatching {
            datasource.createHazardReport(
                accessToken = token,
                request =
                    CreateHazardReportRequestDto(
                        reportType = outboxEntity.reportCategory,
                        description = outboxEntity.description.takeIf(String::isNotBlank),
                        reportPoint =
                            HazardReportPointDto(
                                lat = outboxEntity.latitude,
                                lng = outboxEntity.longitude,
                            ),
                        imageUrls = emptyList(),
                    ),
            )
        }.fold(
            onSuccess = { response ->
                val now = clock()
                reportOutboxDao.upsertReportOutbox(
                    outboxEntity.copy(
                        status = ReportOutboxStatus.Submitted.name,
                        serverReportId = response.reportId,
                        lastFailureReason = null,
                        updatedAt = now,
                    ),
                )
                ReportSubmitResult.Success(
                    outboxId = outboxEntity.outboxId,
                    serverReportId = response.reportId,
                )
            },
            onFailure = { throwable ->
                failOutbox(outboxId, throwable.toSubmitFailureReason())
            },
        )
    }

    private suspend fun failOutbox(
        outboxId: String,
        reason: ReportSubmitFailureReason,
    ): ReportSubmitResult.Failure {
        val outboxEntity = reportOutboxDao.getReportOutbox(outboxId)
        if (outboxEntity != null) {
            markOutboxStatus(outboxEntity, ReportOutboxStatus.Failed, lastFailureReason = reason.name)
        }
        return ReportSubmitResult.Failure(outboxId = outboxId, reason = reason)
    }

    private suspend fun markOutboxStatus(
        outboxEntity: ReportOutboxEntity,
        status: ReportOutboxStatus,
        lastFailureReason: String?,
    ) {
        val now = clock()
        reportOutboxDao.upsertReportOutbox(
            outboxEntity.copy(
                status = status.name,
                lastFailureReason = lastFailureReason,
                updatedAt = now,
            ),
        )
    }
}

private fun Throwable.toSubmitFailureReason(): ReportSubmitFailureReason =
    when (this) {
        is HazardReportsApiException ->
            when {
                httpStatusCode == 401 -> ReportSubmitFailureReason.Unauthorized
                httpStatusCode in 400..499 -> ReportSubmitFailureReason.InvalidInput
                else -> ReportSubmitFailureReason.Unknown
            }
        is java.io.IOException -> ReportSubmitFailureReason.Network
        else -> ReportSubmitFailureReason.Unknown
    }

private fun ReportDraftEntity.toData(): ReportDraftData =
    ReportDraftData(
        draftId = draftId,
        reportCategory = reportCategory,
        description = description,
        address = address,
        latitude = latitude,
        longitude = longitude,
        locationSource = locationSource,
        photoUri = photoUri,
        photoMimeType = photoMimeType,
        photoSizeBytes = photoSizeBytes,
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt,
    )

private fun ReportDraftData.toEntity(): ReportDraftEntity =
    ReportDraftEntity(
        draftId = draftId,
        reportCategory = reportCategory,
        description = description,
        address = address,
        latitude = latitude,
        longitude = longitude,
        locationSource = locationSource,
        photoUri = photoUri,
        photoMimeType = photoMimeType,
        photoSizeBytes = photoSizeBytes,
        createdAt = createdAtMillis,
        updatedAt = updatedAtMillis,
    )

private fun ReportOutboxEntity.toData(): ReportOutboxData =
    ReportOutboxData(
        outboxId = outboxId,
        reportCategory = reportCategory,
        description = description,
        address = address,
        latitude = latitude,
        longitude = longitude,
        photoUri = photoUri,
        photoMimeType = photoMimeType,
        photoSizeBytes = photoSizeBytes,
        status = runCatching { ReportOutboxStatus.valueOf(status) }.getOrDefault(ReportOutboxStatus.Pending),
        serverReportId = serverReportId,
        lastFailureReason = lastFailureReason,
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt,
    )

private fun ReportOutboxData.toEntity(): ReportOutboxEntity =
    ReportOutboxEntity(
        outboxId = outboxId,
        reportCategory = reportCategory,
        description = description,
        address = address,
        latitude = latitude,
        longitude = longitude,
        photoUri = photoUri,
        photoMimeType = photoMimeType,
        photoSizeBytes = photoSizeBytes,
        status = status.name,
        serverReportId = serverReportId,
        lastFailureReason = lastFailureReason,
        createdAt = createdAtMillis,
        updatedAt = updatedAtMillis,
    )
