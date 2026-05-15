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
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
    // 좌표 → RGC 자동 변환 결과(도로명/지번). 사용자가 손대지 않는 객관 정보.
    val address: String?,
    // 사용자가 "건물명·주변 장소" 입력란에 직접 적은 현장 맥락 보충 메모 (v8 신설).
    // 기존 단일 address 필드만 사용하던 코드/테스트 호환을 위해 default null.
    val addressDetail: String? = null,
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
    // 자동 RGC 결과. 서버 submit DTO에는 address 필드 자체가 없어 로컬에서만 사용된다.
    val address: String?,
    // 사용자 직접 보충 메모. 마찬가지로 로컬 전용 (v8 신설).
    val addressDetail: String? = null,
    val latitude: Double,
    val longitude: Double,
    // legacy v9 이전 단일 사진 필드. 새 코드는 photos 리스트를 source of truth로 사용한다.
    val photoUri: String?,
    val photoMimeType: String?,
    val photoSizeBytes: Long?,
    // v10 (Task 5.5) — 업로드 대상 사진 메타데이터 다장 보존.
    val photos: List<ReportOutboxPhotoData> = emptyList(),
    // v10 (Task 5.5) — presigned 업로드 성공한 S3 object key 목록. 제출 시 BE에 전달.
    val imageObjectKeys: List<String> = emptyList(),
    val status: ReportOutboxStatus = ReportOutboxStatus.Pending,
    val serverReportId: Long? = null,
    val lastFailureReason: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

/**
 * Outbox에 보존되는 사진 메타데이터 (Task 5.5).
 *
 * `ReportDraftPhotoData`와 동일 shape이지만 단계(draft vs outbox 제출 직전)가 달라 별도 데이터 클래스로 둔다.
 */
data class ReportOutboxPhotoData(
    val localUri: String,
    val mimeType: String?,
    val sizeBytes: Long?,
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
    // Task 5.5 — outbox 사진들을 BE submit 직전에 presigned URL로 업로드. null/NoOp이면 업로드 skip.
    private val imageUploader: HazardReportImageUploader = NoOpHazardReportImageUploader,
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

        // Task 5.5 — 서버 제출 직전에 outbox 사진들을 presigned URL로 업로드한다.
        // 부분 성공 시 성공한 objectKey는 outbox에 보존되어 다음 재시도 시 중복 업로드를 피한다.
        val outboxData = outboxEntity.toData()
        val uploadResult =
            runCatching {
                imageUploader.uploadAll(
                    accessToken = token,
                    photos = outboxData.photos,
                    alreadyUploadedCount = outboxData.imageObjectKeys.size,
                )
            }.getOrElse {
                return failOutbox(outboxId, ReportSubmitFailureReason.Network)
            }

        val mergedObjectKeys = outboxData.imageObjectKeys + uploadResult.newlyUploadedObjectKeys
        // 업로드 결과(부분 성공이라도 그때까지의 objectKey)를 outbox에 미리 반영하여 재시도 시 활용.
        val outboxAfterUpload = persistObjectKeys(outboxEntity, mergedObjectKeys)

        if (!uploadResult.allSucceeded) {
            // 일부 사진 업로드 실패 시 즉시 실패 처리. 이미 보존된 objectKey는 다음 재시도에 재활용.
            return failOutbox(outboxId, ReportSubmitFailureReason.Network)
        }

        return runCatching {
            datasource.createHazardReport(
                accessToken = token,
                request =
                    CreateHazardReportRequestDto(
                        reportType = outboxAfterUpload.reportCategory,
                        description = outboxAfterUpload.description.takeIf(String::isNotBlank),
                        reportPoint =
                            HazardReportPointDto(
                                lat = outboxAfterUpload.latitude,
                                lng = outboxAfterUpload.longitude,
                            ),
                        // Task 5.6에서 imageObjectKeys 필드로 전환 예정. 이번 분기는 5.5 범위라
                        // 기존 imageUrls 필드 그대로 두되 업로드된 objectKey는 outbox에 보존.
                        imageUrls = emptyList(),
                    ),
            )
        }.fold(
            onSuccess = { response ->
                val now = clock()
                reportOutboxDao.upsertReportOutbox(
                    outboxAfterUpload.copy(
                        status = ReportOutboxStatus.Submitted.name,
                        serverReportId = response.reportId,
                        lastFailureReason = null,
                        updatedAt = now,
                    ),
                )
                ReportSubmitResult.Success(
                    outboxId = outboxAfterUpload.outboxId,
                    serverReportId = response.reportId,
                )
            },
            onFailure = { throwable ->
                failOutbox(outboxId, throwable.toSubmitFailureReason())
            },
        )
    }

    /**
     * 업로드 단계의 결과(목록의 일부 또는 전부 성공한 objectKey)를 outbox에 미리 보존한다.
     * 이후 단계가 실패하더라도 다음 재시도에서 이미 업로드된 사진은 다시 올리지 않는다.
     */
    private suspend fun persistObjectKeys(
        outboxEntity: ReportOutboxEntity,
        objectKeys: List<String>,
    ): ReportOutboxEntity {
        if (objectKeys == deserializeStringList(outboxEntity.imageObjectKeysJson)) {
            return outboxEntity
        }
        val updated =
            outboxEntity.copy(
                imageObjectKeysJson = serializeStringList(objectKeys),
                updatedAt = clock(),
            )
        reportOutboxDao.upsertReportOutbox(updated)
        return updated
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

    /**
     * 서버 응답의 createdAt(ISO 8601 형식)을 epoch millis로 변환한다.
     *
     * BE 명세에는 `"2026-04-28T17:00:00"`처럼 timezone offset이 명시되지 않은 LocalDateTime
     * 형식으로 정의되어 있다. 기존 구현은 이 값을 `ZoneId.systemDefault()`(=KST)로 해석해
     * BE가 UTC로 보낸 경우 9시간 오차가 발생했다.
     *
     * Fallback chain으로 견고하게 처리:
     * 1. ISO Instant("...Z") — 추후 BE가 UTC offset을 명시할 때 자동 호환
     * 2. ISO with offset("...+09:00") — 추후 BE가 KST offset 명시할 때
     * 3. offset 없는 LocalDateTime — 일반 REST API 관례대로 UTC로 가정
     */
    private fun String.toServerEpochMillisOrNull(): Long? {
        runCatching { Instant.parse(this).toEpochMilli() }
            .getOrNull()
            ?.let { return it }
        runCatching { OffsetDateTime.parse(this).toInstant().toEpochMilli() }
            .getOrNull()
            ?.let { return it }
        return runCatching {
            LocalDateTime.parse(this)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

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
        addressDetail = addressDetail,
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
        addressDetail = addressDetail,
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
        addressDetail = addressDetail,
        latitude = latitude,
        longitude = longitude,
        photoUri = photoUri,
        photoMimeType = photoMimeType,
        photoSizeBytes = photoSizeBytes,
        // v10 — Task 5.5: 새 photos 컬럼이 있으면 그걸 source of truth로, 없으면 legacy 단일 photo로 fallback.
        photos = resolveOutboxPhotosFromEntity(),
        imageObjectKeys = deserializeStringList(imageObjectKeysJson),
        status = runCatching { ReportOutboxStatus.valueOf(status) }.getOrDefault(ReportOutboxStatus.Pending),
        serverReportId = serverReportId,
        lastFailureReason = lastFailureReason,
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt,
    )

private fun ReportOutboxData.toEntity(): ReportOutboxEntity {
    val firstPhoto = photos.firstOrNull()
    return ReportOutboxEntity(
        outboxId = outboxId,
        reportCategory = reportCategory,
        description = description,
        address = address,
        addressDetail = addressDetail,
        latitude = latitude,
        longitude = longitude,
        // legacy 단일 사진 컬럼은 첫 사진으로 채워둔다. 마이그레이션 직후 old reader가 이 row를
        // 읽어도 1장은 복원 가능하도록 유지.
        photoUri = firstPhoto?.localUri ?: photoUri,
        photoMimeType = firstPhoto?.mimeType ?: photoMimeType,
        photoSizeBytes = firstPhoto?.sizeBytes ?: photoSizeBytes,
        photosJson = serializeOutboxPhotos(photos),
        imageObjectKeysJson = serializeStringList(imageObjectKeys),
        status = status.name,
        serverReportId = serverReportId,
        lastFailureReason = lastFailureReason,
        createdAt = createdAtMillis,
        updatedAt = updatedAtMillis,
    )
}

/**
 * v10 photosJson이 있으면 그걸 source of truth로 사용한다.
 * 없으면 legacy single-photo 컬럼으로 fallback (v9 이전 row).
 */
private fun ReportOutboxEntity.resolveOutboxPhotosFromEntity(): List<ReportOutboxPhotoData> {
    val deserialized = photosJson?.let(::deserializeOutboxPhotos)
    if (!deserialized.isNullOrEmpty()) return deserialized

    val legacyUri = photoUri?.takeIf(String::isNotBlank) ?: return emptyList()
    return listOf(
        ReportOutboxPhotoData(
            localUri = legacyUri,
            mimeType = photoMimeType,
            sizeBytes = photoSizeBytes,
        ),
    )
}

private fun serializeOutboxPhotos(photos: List<ReportOutboxPhotoData>): String? {
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

private fun deserializeOutboxPhotos(json: String): List<ReportOutboxPhotoData> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val array = org.json.JSONArray(json)
        buildList(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val uri = obj.optString("uri").takeIf { it.isNotBlank() } ?: continue
                add(
                    ReportOutboxPhotoData(
                        localUri = uri,
                        mimeType = obj.optString("mime").takeIf { it.isNotBlank() },
                        sizeBytes = if (obj.has("size")) obj.optLong("size") else null,
                    ),
                )
            }
        }
    }.getOrElse { emptyList() }
}

private fun serializeStringList(values: List<String>): String? {
    if (values.isEmpty()) return null
    val array = org.json.JSONArray()
    values.forEach { array.put(it) }
    return array.toString()
}

private fun deserializeStringList(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = org.json.JSONArray(json)
        List(array.length()) { array.optString(it) }
            .filter { it.isNotBlank() }
    }.getOrElse { emptyList() }
}

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
