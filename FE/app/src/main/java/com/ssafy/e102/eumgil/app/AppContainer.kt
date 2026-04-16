package com.ssafy.e102.eumgil.app

import android.content.Context
import com.ssafy.e102.eumgil.core.location.AndroidCurrentLocationManager
import com.ssafy.e102.eumgil.core.location.AndroidLocationPermissionManager
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionManager
import com.ssafy.e102.eumgil.data.local.db.EumgilDatabase
import com.ssafy.e102.eumgil.data.local.datasource.InitSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.datastore.initSettingsDataStore
import com.ssafy.e102.eumgil.data.repository.DefaultInitSettingsRepository
import com.ssafy.e102.eumgil.data.repository.InitSettingsRepository

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    val localDatabase: EumgilDatabase by lazy(LazyThreadSafetyMode.NONE) {
        EumgilDatabase.getInstance(appContext)
    }

    private val initSettingsLocalDataSource =
        InitSettingsLocalDataSource(dataStore = appContext.initSettingsDataStore)

    val initSettingsRepository: InitSettingsRepository =
        DefaultInitSettingsRepository(localDataSource = initSettingsLocalDataSource)

    val locationPermissionManager: LocationPermissionManager =
        AndroidLocationPermissionManager(context = appContext)

    val currentLocationManager: CurrentLocationManager =
        AndroidCurrentLocationManager(context = appContext)
}
