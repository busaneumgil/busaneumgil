package com.ssafy.e102.eumgil.feature.lowvision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.core.model.toRouteWaypointOrNull
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LowVisionRouteBriefingUiState(
    val isLoading: Boolean = true,
    val destinationName: String = "목적지",
    val steps: List<LowVisionRouteBriefingStepUiState> = emptyList(),
    val errorMessage: String? = null,
) {
    val briefingText: String
        get() = steps.toBriefingSpeechText()
}

data class LowVisionRouteBriefingStepUiState(
    val sequence: Int,
    val instruction: String,
    val icon: LowVisionRouteBriefingStepIcon,
)

enum class LowVisionRouteBriefingStepIcon {
    STRAIGHT,
    TRANSIT,
    TURN,
}

internal const val BRIEFING_VISIBLE_STEP_COUNT: Int = 3

internal fun List<LowVisionRouteBriefingStepUiState>.visibleBriefingSteps(
    startIndex: Int,
): List<LowVisionRouteBriefingStepUiState> {
    if (isEmpty()) return emptyList()

    val clampedStartIndex = startIndex.coerceIn(0, lastIndex)
    val windowStartIndex = clampedStartIndex - clampedStartIndex % BRIEFING_VISIBLE_STEP_COUNT
    return drop(windowStartIndex).take(BRIEFING_VISIBLE_STEP_COUNT)
}

internal fun List<LowVisionRouteBriefingStepUiState>.nextBriefingWindowStart(
    startIndex: Int,
): Int? {
    val nextStartIndex =
        if (startIndex < 0) {
            0
        } else {
            startIndex - startIndex % BRIEFING_VISIBLE_STEP_COUNT + BRIEFING_VISIBLE_STEP_COUNT
        }

    return nextStartIndex.takeIf { it < size }
}

internal fun List<LowVisionRouteBriefingStepUiState>.briefingSpeechTextFrom(
    startIndex: Int,
): String =
    visibleBriefingSteps(startIndex).toBriefingSpeechText()

internal fun List<LowVisionRouteBriefingStepUiState>.toBriefingSpeechText(): String =
    buildString {
        append("경로 브리핑. ")
        this@toBriefingSpeechText.forEach { step ->
            append("${step.sequence}번. ${step.instruction}. ")
        }
    }.trim()

class LowVisionRouteBriefingViewModel(
    private val routeRepository: RouteRepository,
    private val destinationSelectionRepository: DestinationSelectionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LowVisionRouteBriefingUiState())
    val uiState = mutableUiState.asStateFlow()

    init {
        loadBriefing()
    }

    private fun loadBriefing() {
        val destination = destinationSelectionRepository.selectedDestination.value
        val destinationWaypoint = destination.toBriefingDestinationWaypoint()

        mutableUiState.update {
            it.copy(
                isLoading = true,
                destinationName = destinationWaypoint.name.orEmpty().ifBlank { "목적지" },
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                routeRepository.getRouteSearchData(
                    RouteSearchQuery(
                        origin = DEFAULT_ORIGIN,
                        destination = destinationWaypoint,
                        requestedOptions = listOf(RouteOption.SAFE),
                    ),
                )
            }.onSuccess { searchData ->
                val route = searchData.primaryRoute ?: searchData.routes.firstOrNull()
                mutableUiState.update { state ->
                    state.copy(
                        isLoading = false,
                        steps = route?.segments.orEmpty().map(RouteSegment::toBriefingStepUiState),
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                mutableUiState.update { state ->
                    state.copy(
                        isLoading = false,
                        steps = emptyList(),
                        errorMessage = throwable.message ?: "경로 브리핑을 불러오지 못했습니다.",
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            routeRepository: RouteRepository,
            destinationSelectionRepository: DestinationSelectionRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LowVisionRouteBriefingViewModel::class.java)) {
                        return LowVisionRouteBriefingViewModel(
                            routeRepository = routeRepository,
                            destinationSelectionRepository = destinationSelectionRepository,
                        ) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}

private fun PlaceDestination?.toBriefingDestinationWaypoint(): RouteWaypoint =
    this?.toRouteWaypointOrNull()
        ?: DEFAULT_DESTINATION

private fun RouteSegment.toBriefingStepUiState(): LowVisionRouteBriefingStepUiState =
    LowVisionRouteBriefingStepUiState(
        sequence = sequence,
        instruction = toBriefingInstruction(),
        icon =
            when (sequence % 3) {
                1 -> LowVisionRouteBriefingStepIcon.STRAIGHT
                2 -> LowVisionRouteBriefingStepIcon.TRANSIT
                else -> LowVisionRouteBriefingStepIcon.TURN
            },
    )

private fun RouteSegment.toBriefingInstruction(): String =
    when (sequence) {
        1 -> "${distanceMeters.coerceAtLeast(100)}미터 직진"
        2 -> "지하도 입구"
        3 -> "도착지 우측"
        else -> guidanceMessage.ifBlank { "경로를 따라 이동" }
    }

private val DEFAULT_ORIGIN =
    RouteWaypoint(
        name = "현재 위치",
        address = "데모 출발지",
        coordinate = GeoCoordinate(latitude = 35.1796, longitude = 129.0756),
    )

private val DEFAULT_DESTINATION =
    RouteWaypoint(
        name = "부산역",
        address = "부산 동구 중앙대로 206",
        coordinate = GeoCoordinate(latitude = 35.1151, longitude = 129.0414),
    )
