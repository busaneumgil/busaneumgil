package com.ssafy.e102.eumgil.data.remote.dto

data class BookmarkPointDto(
    val lat: Double,
    val lng: Double,
)

data class BookmarkListItemDto(
    val bookmarkId: Long,
    val placeId: Long,
    val provider: String?,
    val providerPlaceId: String?,
    val name: String,
    val category: String,
    val address: String?,
    val point: BookmarkPointDto,
)

data class BookmarkPageDto(
    val content: List<BookmarkListItemDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)

data class CreateBookmarkResponseDto(
    val bookmarkId: Long,
    val placeId: Long,
)
