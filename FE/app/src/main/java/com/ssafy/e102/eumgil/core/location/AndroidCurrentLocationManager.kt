package com.ssafy.e102.eumgil.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidCurrentLocationManager(
    context: Context,
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext),
) : CurrentLocationManager {
    private val appContext = context.applicationContext
    private val mutableLatestLocation = MutableStateFlow<LocationSnapshot?>(null)
    private var isTracking = false

    private val locationCallback =
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.toSnapshot()?.let { snapshot ->
                    mutableLatestLocation.value = snapshot
                }
            }
        }

    override val latestLocation: StateFlow<LocationSnapshot?> = mutableLatestLocation.asStateFlow()

    @SuppressLint("MissingPermission")
    override fun refreshLatestLocation() {
        if (appContext.resolveLocationGrantAccuracy() == null) return
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                mutableLatestLocation.value =
                    resolveCurrentLocationRefreshSnapshot(
                        previous = mutableLatestLocation.value,
                        candidate = location?.toSnapshot(),
                    )
            }
    }

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        stopLocationUpdates()

        val accuracy = appContext.resolveLocationGrantAccuracy() ?: return
        refreshLatestLocation()
        fusedLocationClient
            .requestLocationUpdates(
                createLocationRequest(accuracy = accuracy),
                locationCallback,
                Looper.getMainLooper(),
            ).addOnSuccessListener {
                isTracking = true
            }.addOnFailureListener {
                isTracking = false
            }
    }

    override fun stopLocationUpdates() {
        if (!isTracking) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking = false
    }

    private fun createLocationRequest(accuracy: LocationGrantAccuracy): LocationRequest {
        val priority =
            when (accuracy) {
                LocationGrantAccuracy.PRECISE -> Priority.PRIORITY_HIGH_ACCURACY
                LocationGrantAccuracy.APPROXIMATE -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }
        return LocationRequest
            .Builder(priority, LOCATION_UPDATE_INTERVAL_MILLIS)
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_UPDATE_INTERVAL_MILLIS)
            .setMinUpdateDistanceMeters(LOCATION_UPDATE_MIN_DISTANCE_METERS)
            .build()
    }

    private companion object {
        const val LOCATION_UPDATE_INTERVAL_MILLIS = 2_000L
        const val LOCATION_FASTEST_UPDATE_INTERVAL_MILLIS = 1_000L
        const val LOCATION_UPDATE_MIN_DISTANCE_METERS = 5f
    }
}

internal fun resolveCurrentLocationRefreshSnapshot(
    previous: LocationSnapshot?,
    candidate: LocationSnapshot?,
    nowEpochMillis: Long = System.currentTimeMillis(),
): LocationSnapshot? =
    candidate
        ?.takeIf { snapshot -> snapshot.isFreshCurrentLocation(nowEpochMillis = nowEpochMillis) }
        ?: previous?.takeIf { snapshot -> snapshot.isFreshCurrentLocation(nowEpochMillis = nowEpochMillis) }
