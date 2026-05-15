package com.ssafy.e102.eumgil.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.ssafy.e102.eumgil.data.remote.datasource.HazardReportImagesRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.HazardReportsApiException
import com.ssafy.e102.eumgil.data.remote.dto.PresignedUploadRequestDto
import kotlin.coroutines.cancellation.CancellationException

/**
 * 제보 outbox의 사진 목록을 presigned URL을 통해 S3/MinIO에 업로드한 뒤 안정 저장값(`objectKey`) 목록을 돌려준다 (Task 5.5).
 *
 * 책임 분리: Repository는 outbox state machine과 서버 submit을 다루고, 이 uploader는 binary I/O와
 * S3 호출만 처리한다. 두 흐름은 인터페이스 단위로 분리되어 테스트하기 쉽게 둔다.
 *
 * 부분 성공 정책: 한 장이라도 실패하면 그 시점까지 성공한 `objectKey`만 돌려준다. 호출자(Repository)는
 * 이 결과를 outbox에 보존하여 재시도 시 미업로드분만 다시 시도할 수 있다.
 */
fun interface HazardReportImageUploader {
    suspend fun uploadAll(
        accessToken: String,
        photos: List<ReportOutboxPhotoData>,
        alreadyUploadedCount: Int,
    ): HazardReportImageUploadResult
}

data class HazardReportImageUploadResult(
    val newlyUploadedObjectKeys: List<String>,
    val allSucceeded: Boolean,
)

object NoOpHazardReportImageUploader : HazardReportImageUploader {
    override suspend fun uploadAll(
        accessToken: String,
        photos: List<ReportOutboxPhotoData>,
        alreadyUploadedCount: Int,
    ): HazardReportImageUploadResult =
        HazardReportImageUploadResult(
            newlyUploadedObjectKeys = emptyList(),
            // 사진 0장이거나 이미 전부 업로드된 경우는 성공으로 간주.
            allSucceeded = photos.size <= alreadyUploadedCount,
        )
}

/**
 * `ContentResolver`로 local URI에서 binary를 읽고 `HazardReportImagesRemoteDataSource`로 presigned 업로드를 수행하는 기본 구현.
 *
 * 동작:
 * 1. `alreadyUploadedCount` 만큼은 이미 업로드된 사진이라 건너뛴다 (재시도 시 중복 업로드 방지).
 * 2. 남은 사진을 **순차** 업로드 — 한 장 실패 시 거기서 멈추고 그때까지의 `objectKey`를 반환.
 * 3. 모든 사진이 성공하면 `allSucceeded = true`, 부분 성공이면 false.
 */
class DefaultHazardReportImageUploader(
    private val contentResolver: ContentResolver,
    private val remoteDataSource: HazardReportImagesRemoteDataSource,
) : HazardReportImageUploader {
    override suspend fun uploadAll(
        accessToken: String,
        photos: List<ReportOutboxPhotoData>,
        alreadyUploadedCount: Int,
    ): HazardReportImageUploadResult {
        if (photos.isEmpty()) return HazardReportImageUploadResult(emptyList(), allSucceeded = true)
        if (alreadyUploadedCount >= photos.size) {
            return HazardReportImageUploadResult(emptyList(), allSucceeded = true)
        }

        val pending = photos.drop(alreadyUploadedCount)
        val uploadedKeys = mutableListOf<String>()
        var allSucceeded = true

        for ((index, photo) in pending.withIndex()) {
            val absoluteIndex = alreadyUploadedCount + index
            val ok =
                try {
                    uploadSingle(accessToken, photo, absoluteIndex)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (apiException: HazardReportsApiException) {
                    // Task 5.9 — 401(A4010) 등 인증 실패는 swallow하지 않고 throw해서
                    // Repository의 AuthenticatedRequestRunner가 /auth/reissue + 재시도할 수 있게 한다.
                    if (apiException.httpStatusCode == HTTP_UNAUTHORIZED) throw apiException
                    Log.w(IMAGE_UPLOADER_LOG_TAG, "Photo upload failed at index $absoluteIndex", apiException)
                    null
                } catch (other: Throwable) {
                    // 그 외 IO/네트워크 실패는 부분 성공 정책에 맞춰 흐름을 유지한다.
                    Log.w(IMAGE_UPLOADER_LOG_TAG, "Photo upload failed at index $absoluteIndex", other)
                    null
                }
            if (ok == null) {
                allSucceeded = false
                break
            }
            uploadedKeys += ok
        }

        return HazardReportImageUploadResult(
            newlyUploadedObjectKeys = uploadedKeys,
            allSucceeded = allSucceeded,
        )
    }

    private suspend fun uploadSingle(
        accessToken: String,
        photo: ReportOutboxPhotoData,
        index: Int,
    ): String? {
        val uri = Uri.parse(photo.localUri)
        val mime = photo.mimeType ?: contentResolver.getType(uri) ?: DEFAULT_MIME_TYPE
        val bytes =
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
        val presigned =
            remoteDataSource.requestPresignedUpload(
                accessToken = accessToken,
                request =
                    PresignedUploadRequestDto(
                        fileName = uri.lastPathSegment ?: "photo_$index",
                        contentType = mime,
                        contentLength = bytes.size.toLong(),
                    ),
            )
        val uploaded =
            remoteDataSource.uploadBinary(
                uploadUrl = presigned.uploadUrl,
                contentType = mime,
                body = bytes,
            )
        return if (uploaded) presigned.objectKey else null
    }

    private companion object {
        private const val IMAGE_UPLOADER_LOG_TAG = "HazardReportImageUploader"
        private const val DEFAULT_MIME_TYPE = "image/jpeg"
        private const val HTTP_UNAUTHORIZED = 401
    }
}
