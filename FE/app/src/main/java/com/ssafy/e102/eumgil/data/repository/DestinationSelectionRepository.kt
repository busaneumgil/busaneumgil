package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.PlaceDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RouteEditingTarget {
    ORIGIN,
    DESTINATION,
}

data class RouteSelectionState(
    val selectedOrigin: PlaceDestination? = null,
    val selectedDestination: PlaceDestination? = null,
    val editingTarget: RouteEditingTarget = RouteEditingTarget.DESTINATION,
)

enum class RouteSelectionRequestReason {
    ORIGIN_UPDATED,
    ORIGIN_CLEARED,
    DESTINATION_UPDATED,
    DESTINATION_CLEARED,
    SWAPPED,
}

data class RouteSelectionRequest(
    val state: RouteSelectionState,
    val reason: RouteSelectionRequestReason,
)

interface DestinationSelectionRepository {
    val selectionState: StateFlow<RouteSelectionState>
    val selectedOrigin: StateFlow<PlaceDestination?>
    val selectedDestination: StateFlow<PlaceDestination?>
    val editingTarget: StateFlow<RouteEditingTarget>
    val selectionRequests: Flow<RouteSelectionRequest>

    fun setEditingTarget(target: RouteEditingTarget)

    fun updateSelectedOrigin(origin: PlaceDestination)

    fun updateSelectedDestination(destination: PlaceDestination)

    fun updateSelectionForEditingTarget(destination: PlaceDestination)

    fun swapSelections(
        origin: PlaceDestination,
        destination: PlaceDestination,
    )

    fun clearSelectedOrigin()

    fun clearSelectedDestination()
}

class InMemoryDestinationSelectionRepository : DestinationSelectionRepository {
    private val mutableSelectedOrigin = MutableStateFlow<PlaceDestination?>(null)
    private val mutableSelectedDestination = MutableStateFlow<PlaceDestination?>(null)
    private val mutableEditingTarget = MutableStateFlow(RouteEditingTarget.DESTINATION)
    private val mutableSelectionState = MutableStateFlow(RouteSelectionState())
    private val mutableSelectionRequests = MutableSharedFlow<RouteSelectionRequest>(extraBufferCapacity = 1)

    override val selectionState: StateFlow<RouteSelectionState> = mutableSelectionState.asStateFlow()
    override val selectedOrigin: StateFlow<PlaceDestination?> = mutableSelectedOrigin.asStateFlow()
    override val selectedDestination: StateFlow<PlaceDestination?> = mutableSelectedDestination.asStateFlow()
    override val editingTarget: StateFlow<RouteEditingTarget> = mutableEditingTarget.asStateFlow()
    override val selectionRequests: Flow<RouteSelectionRequest> = mutableSelectionRequests.asSharedFlow()

    override fun setEditingTarget(target: RouteEditingTarget) {
        if (mutableEditingTarget.value == target) {
            return
        }

        mutableEditingTarget.value = target
        syncSelectionState()
    }

    override fun updateSelectedOrigin(origin: PlaceDestination) {
        mutableSelectedOrigin.value = origin
        syncSelectionState()
        emitSelectionRequest(RouteSelectionRequestReason.ORIGIN_UPDATED)
    }

    override fun updateSelectedDestination(destination: PlaceDestination) {
        mutableSelectedDestination.value = destination
        syncSelectionState()
        emitSelectionRequest(RouteSelectionRequestReason.DESTINATION_UPDATED)
    }

    override fun updateSelectionForEditingTarget(destination: PlaceDestination) {
        when (mutableEditingTarget.value) {
            RouteEditingTarget.ORIGIN -> updateSelectedOrigin(destination)
            RouteEditingTarget.DESTINATION -> updateSelectedDestination(destination)
        }
    }

    override fun swapSelections(
        origin: PlaceDestination,
        destination: PlaceDestination,
    ) {
        mutableSelectedOrigin.value = origin
        mutableSelectedDestination.value = destination
        syncSelectionState()
        emitSelectionRequest(RouteSelectionRequestReason.SWAPPED)
    }

    override fun clearSelectedOrigin() {
        mutableSelectedOrigin.value = null
        syncSelectionState()
        emitSelectionRequest(RouteSelectionRequestReason.ORIGIN_CLEARED)
    }

    override fun clearSelectedDestination() {
        mutableSelectedDestination.value = null
        syncSelectionState()
        emitSelectionRequest(RouteSelectionRequestReason.DESTINATION_CLEARED)
    }

    private fun syncSelectionState() {
        mutableSelectionState.value =
            RouteSelectionState(
                selectedOrigin = mutableSelectedOrigin.value,
                selectedDestination = mutableSelectedDestination.value,
                editingTarget = mutableEditingTarget.value,
            )
    }

    private fun emitSelectionRequest(reason: RouteSelectionRequestReason) {
        mutableSelectionRequests.tryEmit(
            RouteSelectionRequest(
                state = mutableSelectionState.value,
                reason = reason,
            ),
        )
    }
}
