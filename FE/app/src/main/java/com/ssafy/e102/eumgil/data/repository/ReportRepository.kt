package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.data.local.dao.ReportDraftDao
import com.ssafy.e102.eumgil.data.local.dao.ReportOutboxDao
import com.ssafy.e102.eumgil.data.local.entity.ReportDraftEntity
import com.ssafy.e102.eumgil.data.local.entity.ReportOutboxEntity
import com.ssafy.e102.eumgil.data.remote.datasource.HazardReportsApiException
import com.ssafy.e102.eumgil.data.remote.datasource.HazardReportsRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.CreateHazardReportRequestDto
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportDetailDto
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportListItemDto
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportPointDto
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

interface ReportRepository {
    fun observeReportHistory(): Flow<List<ReportOutboxData>>

    fun observeReportHistoryEntries(): Flow<List<ReportHistoryData>> =
        observeReportHistory().map { outboxItems ->
            outboxItems.map(ReportOutboxData::toLocalHistoryData)
        }

    suspend fun getReportHistoryDetail(historyId: String): ReportHistoryDetailData? = null

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
    // v7부터 사진 다장(최대 5장) 보존. 단일 사진 시절(v6 이전)에 저장된 draft를 read 시점에 자동
    // 1-item list로 변환하여 자연스럽게 호환된다.
    val photos: List<ReportDraftPhotoData>,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

/**
 * 제보 임시저장에 포함된 단일 사진 메타데이터.
 * `ReportPhoto` (UI 모델)와 동일한 shape이지만 도메인 분리를 위해 별도 데이터 클래스로 둔다.
 */
data class ReportDraftPhotoData(
    val localUri: String,
    val mimeType: String?,
    val sizeBytes: Long?,
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

enum class ReportHistorySource {
    Server,
    LocalOutbox,
}

data class ReportHistoryData(
    val historyId: String,
    val reportCategory: String,
    val description: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val photoUri: String?,
    val imageUrl: String?,
    val source: ReportHistorySource,
    val serverReportId: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class ReportHistoryDetailData(
    val historyId: String,
    val reportCategory: String,
    val description: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val imageRefs: List<String>,
    val source: ReportHistorySource,
    val serverReportId: Long?,
    val createdAtMillis: Long,
)

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
    private val serverReportHistory = MutableStateFlow(emptyList<ReportHistoryData>())

    override fun observeReportHistory(): Flow<List<ReportOutboxData>> =
        reportOutboxDao.observeReportOutboxItems().map { outboxItems ->
            outboxItems.map(ReportOutboxEntity::toData)
        }

    override fun observeReportHistoryEntries(): Flow<List<ReportHistoryData>> =
        combine(
            reportOutboxDao.observeReportOutboxItems(),
            serverReportHistory,
        ) { localOutboxItems, serverItems ->
            mergeServerAndLocalReportHistory(
                serverItems = serverItems,
                localOutboxItems = localOutboxItems.map(ReportOutboxEntity::toData),
            )
        }.onStart {
            refreshReportHistoryFromServerIfPossible()
        }

    override suspend fun getReportHistoryDetail(historyId: String): ReportHistoryDetailData? {
        val localOutboxId = historyId.removePrefixOrNull(LOCAL_HISTORY_PREFIX)
        val localOutbox = localOutboxId?.let { reportOutboxDao.getReportOutbox(it) }?.toData()
        val serverReportId = historyId.removePrefixOrNull(SERVER_HISTORY_PREFIX)?.toLongOrNull() ?: localOutbox?.serverReportId

        if (serverReportId != null) {
            val serverDetail = fetchServerReportDetail(serverReportId)
            if (serverDetail != null) return serverDetail
        }

        return localOutbox?.toDetailData()
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

    private suspend fun refreshReportHistoryFromServerIfPossible() {
        runCatching {
            val datasource = hazardReportsRemoteDataSource ?: return@runCatching
            val token = accessTokenProvider() ?: return@runCatching

            val serverItems = fetchAllReportHistoryFromServer(datasource = datasource, token = token)
            serverReportHistory.value = serverItems.map { item -> item.toHistoryData() }
        }
    }

    private suspend fun fetchAllReportHistoryFromServer(
        datasource: HazardReportsRemoteDataSource,
        token: String,
    ): List<HazardReportListItemDto> {
        val reports = mutableListOf<HazardReportListItemDto>()
        var cursor: Long? = null

        do {
            val page =
                datasource.getMyHazardReports(
                    accessToken = token,
                    cursor = cursor,
                    size = DEFAULT_PAGE_SIZE,
                )
            reports += page.content
            cursor = page.nextCursor
        } while (page.hasNext && cursor != null)

        return reports
    }

    private suspend fun fetchServerReportDetail(reportId: Long): ReportHistoryDetailData? =
        runCatching {
            val datasource = hazardReportsRemoteDataSource ?: return@runCatching null
            val token = accessTokenProvider() ?: return@runCatching null

            datasource.getMyHazardReportDetail(accessToken = token, reportId = reportId).toDetailData()
        }.getOrNull()

    private fun HazardReportListItemDto.toHistoryData(): ReportHistoryData {
        val createdAtMillis = createdAt.toServerEpochMillisOrNull() ?: clock()
        return ReportHistoryData(
            historyId = "$SERVER_HISTORY_PREFIX$reportId",
            reportCategory = reportType,
            description = null,
            address = null,
            latitude = reportPoint.lat,
            longitude = reportPoint.lng,
            photoUri = null,
            imageUrl = representativeImageUrl,
            source = ReportHistorySource.Server,
            serverReportId = reportId,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = createdAtMillis,
        )
    }

    private fun HazardReportDetailDto.toDetailData(): ReportHistoryDetailData {
        val createdAtMillis = createdAt.toServerEpochMillisOrNull() ?: clock()
        return ReportHistoryDetailData(
            historyId = "$SERVER_HISTORY_PREFIX$reportId",
            reportCategory = reportType,
            description = description,
            address = null,
            latitude = reportPoint.lat,
            longitude = reportPoint.lng,
            imageRefs = imageUrls,
            source = ReportHistorySource.Server,
            serverReportId = reportId,
            createdAtMillis = createdAtMillis,
        )
    }

    private fun String.toServerEpochMillisOrNull(): Long? =
        runCatching {
            LocalDateTime.parse(this)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()

    private companion object {
        private const val DEFAULT_PAGE_SIZE = 50
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
        photos = resolveDraftPhotosFromEntity(),
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt,
    )

private fun ReportDraftData.toEntity(): ReportDraftEntity {
    val firstPhoto = photos.firstOrNull()
    return ReportDraftEntity(
        draftId = draftId,
        reportCategory = reportCategory,
        description = description,
        address = address,
        latitude = latitude,
        longitude = longitude,
        locationSource = locationSource,
        // legacy 단일 사진 컬럼도 first photo로 채워둔다. 미래 cleanup 시 제거 예정이지만
        // 그 사이에 old reader가 이 row를 읽어도 1장은 복원 가능하도록 유지.
        photoUri = firstPhoto?.localUri,
        photoMimeType = firstPhoto?.mimeType,
        photoSizeBytes = firstPhoto?.sizeBytes,
        photosJson = serializeDraftPhotos(photos),
        createdAt = createdAtMillis,
        updatedAt = updatedAtMillis,
    )
}

/**
 * v7 photosJson이 있으면 그걸 source of truth로 사용한다.
 * 없으면 legacy single-photo 컬럼으로 fallback (v6 이전 row 또는 마이그레이션 직후 row).
 */
private fun ReportDraftEntity.resolveDraftPhotosFromEntity(): List<ReportDraftPhotoData> {
    val deserialized = photosJson?.let(::deserializeDraftPhotos)
    if (!deserialized.isNullOrEmpty()) return deserialized

    val legacyUri = photoUri?.takeIf(String::isNotBlank) ?: return emptyList()
    return listOf(
        ReportDraftPhotoData(
            localUri = legacyUri,
            mimeType = photoMimeType,
            sizeBytes = photoSizeBytes,
        ),
    )
}

/**
 * `ReportDraftPhotoData` 리스트를 JSON 배열 문자열로 직렬화.
 * 빈 리스트는 null로 저장하여 SQL 컬럼 의미를 "사진 없음"으로 명확하게 둔다.
 */
private fun serializeDraftPhotos(photos: List<ReportDraftPhotoData>): String? {
    if (photos.isEmpty()) return null
    val array = org.json.JSONArray()
    photos.forEach { photo ->
        val obj = org.json.JSONObject()
        obj.put("uri", photo.localUri)
        photo.mimeType?.let { obj.put("mime", it) }
        photo.sizeBytes?.let { obj.put("size", it) }
        array.put(obj)
    }
    return array.toString()
}

private fun deserializeDraftPhotos(json: String): List<ReportDraftPhotoData> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val array = org.json.JSONArray(json)
        buildList(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val uri = obj.optString("uri").takeIf { it.isNotBlank() } ?: continue
                add(
                    ReportDraftPhotoData(
                        localUri = uri,
                        mimeType = obj.optString("mime").takeIf { it.isNotBlank() },
                        sizeBytes = if (obj.has("size")) obj.optLong("size") else null,
                    ),
                )
            }
        }
    }.getOrElse { emptyList() }
}

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

private fun mergeServerAndLocalReportHistory(
    serverItems: List<ReportHistoryData>,
    localOutboxItems: List<ReportOutboxData>,
): List<ReportHistoryData> {
    val serverReportIds = serverItems.mapNotNull(ReportHistoryData::serverReportId).toSet()
    val localItems =
        localOutboxItems
            .map(ReportOutboxData::toLocalHistoryData)
            .filterNot { localItem ->
                localItem.serverReportId != null && localItem.serverReportId in serverReportIds
            }

    return (serverItems + localItems).sortedByDescending(ReportHistoryData::updatedAtMillis)
}

private fun ReportOutboxData.toLocalHistoryData(): ReportHistoryData =
    ReportHistoryData(
        historyId = "$LOCAL_HISTORY_PREFIX$outboxId",
        reportCategory = reportCategory,
        description = description.takeIf(String::isNotBlank),
        address = address,
        latitude = latitude,
        longitude = longitude,
        photoUri = photoUri,
        imageUrl = null,
        source = ReportHistorySource.LocalOutbox,
        serverReportId = serverReportId,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )

private fun ReportOutboxData.toDetailData(): ReportHistoryDetailData =
    ReportHistoryDetailData(
        historyId = "$LOCAL_HISTORY_PREFIX$outboxId",
        reportCategory = reportCategory,
        description = description.takeIf(String::isNotBlank),
        address = address,
        latitude = latitude,
        longitude = longitude,
        imageRefs = listOfNotNull(photoUri?.takeIf(String::isNotBlank)),
        source = ReportHistorySource.LocalOutbox,
        serverReportId = serverReportId,
        createdAtMillis = createdAtMillis,
    )

private fun String.removePrefixOrNull(prefix: String): String? =
    takeIf { it.startsWith(prefix) }?.removePrefix(prefix)

private const val SERVER_HISTORY_PREFIX = "server:"
private const val LOCAL_HISTORY_PREFIX = "outbox:"
