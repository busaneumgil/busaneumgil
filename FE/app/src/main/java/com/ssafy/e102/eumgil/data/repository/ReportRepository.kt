package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.data.local.dao.ReportDraftDao
import com.ssafy.e102.eumgil.data.local.dao.ReportOutboxDao
import com.ssafy.e102.eumgil.data.local.entity.ReportDraftEntity
import com.ssafy.e102.eumgil.data.local.entity.ReportOutboxEntity
import java.util.UUID

interface ReportRepository {
    suspend fun getLatestDraft(): ReportDraftData?

    suspend fun saveDraft(draft: ReportDraftData): ReportDraftData

    suspend fun deleteDraft(draftId: String)

    suspend fun saveOutbox(outbox: ReportOutboxData): ReportOutboxData
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
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

enum class ReportOutboxStatus {
    Pending,
}

class DefaultReportRepository(
    private val reportDraftDao: ReportDraftDao,
    private val reportOutboxDao: ReportOutboxDao,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ReportRepository {
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
        createdAt = createdAtMillis,
        updatedAt = updatedAtMillis,
    )
