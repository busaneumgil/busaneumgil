package com.ssafy.e102.eumgil.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidCurrentLocationManager(
    context: Context,
) : CurrentLocationManager {
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val mutableLatestLocation = MutableStateFlow<LocationSnapshot?>(null)
    private var isTracking = false

    private val locationListener =
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                mutableLatestLocation.value = location.toSnapshot()
            }

            override fun onProviderEnabled(provider: String) {
                refreshLatestLocation()
            }

            override fun onProviderDisabled(provider: String) = Unit
        }

    override val latestLocation: StateFlow<LocationSnapshot?> = mutableLatestLocation.asStateFlow()

    override fun refreshLatestLocation() {
        mutableLatestLocation.value = resolveBestLastKnownLocation()?.toSnapshot()
    }

    override fun startLocationUpdates() {
        stopLocationUpdates()

        val accuracy = appContext.resolveLocationGrantAccuracy() ?: return
        val providers = locationManager.usableProviders(accuracy)
        if (providers.isEmpty()) return

        refreshLatestLocation()
        registerLocationUpdates(providers)
        isTracking = true
    }

    override fun stopLocationUpdates() {
        if (!isTracking) return

        runCatching { locationManager.removeUpdates(locationListener) }
        isTracking = false
    }

    @SuppressLint("MissingPermission")
    private fun registerLocationUpdates(providers: List<String>) {
        providers.forEach { provider ->
            runCatching {
                locationManager.requestLocationUpdates(
                    provider,
                    LOCATION_UPDATE_INTERVAL_MILLIS,
                    LOCATION_UPDATE_MIN_DISTANCE_METERS,
                    locationListener,
                    Looper.getMainLooper(),
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun resolveBestLastKnownLocation(): Location? {
        val accuracy = appContext.resolveLocationGrantAccuracy() ?: return null
        val providers = locationManager.usableProviders(accuracy)
        if (providers.isEmpty()) return null

        return providers
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }.maxWithOrNull(
                compareByDescending<Location> { it.time }
                    .thenBy { it.accuracy },
            )
    }

    private companion object {
        const val LOCATION_UPDATE_INTERVAL_MILLIS = 2_000L
        const val LOCATION_UPDATE_MIN_DISTANCE_METERS = 5f
    }
}
