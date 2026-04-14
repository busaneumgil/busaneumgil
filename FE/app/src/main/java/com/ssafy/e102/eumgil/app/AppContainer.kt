package com.ssafy.e102.eumgil.app

import android.content.Context
import com.ssafy.e102.eumgil.data.local.datasource.InitSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.datastore.initSettingsDataStore
import com.ssafy.e102.eumgil.data.repository.DefaultInitSettingsRepository
import com.ssafy.e102.eumgil.data.repository.InitSettingsRepository

class AppContainer(
    context: Context,
) {
    private val initSettingsLocalDataSource =
        InitSettingsLocalDataSource(dataStore = context.applicationContext.initSettingsDataStore)

    val initSettingsRepository: InitSettingsRepository =
        DefaultInitSettingsRepository(localDataSource = initSettingsLocalDataSource)
}
