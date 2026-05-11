package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.MapPlaceDetailType
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DestinationPreviewRequest(
    val requestId: Long,
    val destination: PlaceDestination,
    val editingTarget: RouteEditingTarget = RouteEditingTarget.DESTINATION,
    val accessibilityTagKeys: List<String> = emptyList(),
    val detailType: MapPlaceDetailType = MapPlaceDetailType.INTERNAL_PLACE,
    val bookmarkTargetId: String? = null,
    val provider: String? = null,
    val providerPlaceId: String? = null,
)

interface DestinationPreviewRepository {
    val pendingPreview: StateFlow<DestinationPreviewRequest?>

    fun requestPreview(
        destination: PlaceDestination,
        editingTarget: RouteEditingTarget = RouteEditingTarget.DESTINATION,
        accessibilityTagKeys: List<String> = emptyList(),
        detailType: MapPlaceDetailType = MapPlaceDetailType.INTERNAL_PLACE,
        bookmarkTargetId: String? = null,
        provider: String? = null,
        providerPlaceId: String? = null,
    )

    fun consumePreview(requestId: Long)
}

class InMemoryDestinationPreviewRepository : DestinationPreviewRepository {
    private val mutablePendingPreview = MutableStateFlow<DestinationPreviewRequest?>(null)
    private var nextRequestId = 0L

    override val pendingPreview: StateFlow<DestinationPreviewRequest?> = mutablePendingPreview.asStateFlow()

    override fun requestPreview(
        destination: PlaceDestination,
        editingTarget: RouteEditingTarget,
        accessibilityTagKeys: List<String>,
        detailType: MapPlaceDetailType,
        bookmarkTargetId: String?,
        provider: String?,
        providerPlaceId: String?,
    ) {
        nextRequestId += 1L
        mutablePendingPreview.value =
            DestinationPreviewRequest(
                requestId = nextRequestId,
                destination = destination,
                editingTarget = editingTarget,
                accessibilityTagKeys = accessibilityTagKeys,
                detailType = detailType,
                bookmarkTargetId = bookmarkTargetId,
                provider = provider,
                providerPlaceId = providerPlaceId,
            )
    }

    override fun consumePreview(requestId: Long) {
        mutablePendingPreview.update { pendingPreview ->
            pendingPreview?.takeUnless { it.requestId == requestId }
        }
    }
}

object NoOpDestinationPreviewRepository : DestinationPreviewRepository {
    override val pendingPreview: StateFlow<DestinationPreviewRequest?> = MutableStateFlow(null)

    override fun requestPreview(
        destination: PlaceDestination,
        editingTarget: RouteEditingTarget,
        accessibilityTagKeys: List<String>,
        detailType: MapPlaceDetailType,
        bookmarkTargetId: String?,
        provider: String?,
        providerPlaceId: String?,
    ) = Unit

    override fun consumePreview(requestId: Long) = Unit
}
