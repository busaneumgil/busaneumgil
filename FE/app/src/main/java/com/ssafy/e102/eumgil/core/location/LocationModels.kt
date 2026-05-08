package com.ssafy.e102.eumgil.core.location

enum class LocationGrantAccuracy {
    APPROXIMATE,
    PRECISE,
}

sealed interface LocationPermissionState {
    data class Granted(val accuracy: LocationGrantAccuracy) : LocationPermissionState

    data object Denied : LocationPermissionState

    data class Unavailable(val reason: LocationPermissionUnavailableReason) : LocationPermissionState
}

enum class LocationPermissionUnavailableReason {
    LOCATION_SERVICES_DISABLED,
    NO_LOCATION_FEATURE,
}

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val recordedAtEpochMillis: Long,
)
