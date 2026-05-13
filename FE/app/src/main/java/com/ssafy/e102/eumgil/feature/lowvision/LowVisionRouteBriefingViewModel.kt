package com.ssafy.e102.eumgil.feature.lowvision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import com.ssafy.e102.eumgil.feature.navigation.NavigationBriefingItem
import com.ssafy.e102.eumgil.feature.navigation.NavigationGuidanceAction
import com.ssafy.e102.eumgil.feature.navigation.toCompactNavigationInstruction
import com.ssafy.e102.eumgil.feature.navigation.toNavigationBriefingItems
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

internal fun List<LowVisionRouteBriefingStepUiState>.toBriefingSpeechText(): String {
    val title = "\uACBD\uB85C \uBE0C\uB9AC\uD551"
    if (isEmpty()) return title

    return buildList {
        add(title)
        this@toBriefingSpeechText.forEach { step ->
            add("${step.sequence}\uBC88")
            add(step.instruction.trim().trimEnd('.'))
        }
    }.joinToString(separator = ". ", postfix = ".")
}

class LowVisionRouteBriefingViewModel(
    private val routeRepository: RouteRepository,
    private val destinationSelectionRepository: DestinationSelectionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LowVisionRouteBriefingUiState())
    val uiState = mutableUiState.asStateFlow()

    fun loadBriefing(origin: RouteWaypoint) {
        mutableUiState.update { state ->
            state.copy(
                isLoading = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                routeRepository.buildLowVisionNavigationPlan(
                    destinationSelectionRepository = destinationSelectionRepository,
                    origin = origin,
                )
            }.onSuccess { plan ->
                mutableUiState.update { state ->
                    state.copy(
                        isLoading = false,
                        destinationName =
                            plan
                                ?.searchData
                                ?.result
                                ?.destination
                                ?.name
                                .orEmpty()
                                .ifBlank { "목적지" },
                        steps = plan?.selectedRoute?.toNavigationBriefingItems().orEmpty().map(NavigationBriefingItem::toUiState),
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

private fun NavigationBriefingItem.toUiState(): LowVisionRouteBriefingStepUiState =
    LowVisionRouteBriefingStepUiState(
        sequence = sequence,
        instruction = instruction,
        icon =
            when (guidanceAction) {
                NavigationGuidanceAction.TURN_LEFT,
                NavigationGuidanceAction.TURN_RIGHT,
                    -> LowVisionRouteBriefingStepIcon.TURN

                NavigationGuidanceAction.BUS,
                NavigationGuidanceAction.SUBWAY,
                NavigationGuidanceAction.CROSSWALK,
                    -> LowVisionRouteBriefingStepIcon.TRANSIT

                NavigationGuidanceAction.STRAIGHT -> LowVisionRouteBriefingStepIcon.STRAIGHT
            },
    )

internal fun RouteSegment.toCompactBriefingInstruction(): String =
    toCompactNavigationInstruction()
