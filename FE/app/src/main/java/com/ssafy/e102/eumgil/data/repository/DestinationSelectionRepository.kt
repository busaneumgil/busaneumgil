package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.PlaceDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

interface DestinationSelectionRepository {
    val selectedDestination: StateFlow<PlaceDestination?>
    val selectionRequests: Flow<PlaceDestination>

    fun updateSelectedDestination(destination: PlaceDestination)

    fun clearSelectedDestination()
}

class InMemoryDestinationSelectionRepository : DestinationSelectionRepository {
    private val mutableSelectedDestination = MutableStateFlow<PlaceDestination?>(null)
    private val mutableSelectionRequests = MutableSharedFlow<PlaceDestination>(extraBufferCapacity = 1)

    override val selectedDestination: StateFlow<PlaceDestination?> = mutableSelectedDestination.asStateFlow()
    override val selectionRequests: Flow<PlaceDestination> = mutableSelectionRequests.asSharedFlow()

    override fun updateSelectedDestination(destination: PlaceDestination) {
        mutableSelectedDestination.value = destination
        mutableSelectionRequests.tryEmit(destination)
    }

    override fun clearSelectedDestination() {
        mutableSelectedDestination.value = null
    }
}
