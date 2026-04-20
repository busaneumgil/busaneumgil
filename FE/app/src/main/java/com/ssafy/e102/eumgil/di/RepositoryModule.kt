package com.ssafy.e102.eumgil.di

import com.ssafy.e102.eumgil.data.local.datasource.DebugSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.InitSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.PlacesLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.SearchLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.PlacesMockDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.SearchMockDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.PlacesRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.SearchRemoteDataSource
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.DefaultPlacesRepository
import com.ssafy.e102.eumgil.data.repository.DefaultSearchRepository
import com.ssafy.e102.eumgil.data.repository.DefaultSettingsRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import com.ssafy.e102.eumgil.data.repository.SettingsRepository
import com.ssafy.e102.eumgil.data.repository.policy.DefaultRepositorySourcePolicy
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySourcePolicy

object RepositoryModule {
    fun provideDestinationSelectionRepository(): DestinationSelectionRepository =
        InMemoryDestinationSelectionRepository()

    fun provideSettingsRepository(
        initSettingsLocalDataSource: InitSettingsLocalDataSource,
        debugSettingsLocalDataSource: DebugSettingsLocalDataSource,
    ): SettingsRepository =
        DefaultSettingsRepository(
            initSettingsLocalDataSource = initSettingsLocalDataSource,
            debugSettingsLocalDataSource = debugSettingsLocalDataSource,
        )

    fun provideRepositorySourcePolicy(
        debugSettingsLocalDataSource: DebugSettingsLocalDataSource,
    ): RepositorySourcePolicy =
        DefaultRepositorySourcePolicy(
            debugSettingsLocalDataSource = debugSettingsLocalDataSource,
        )

    fun providePlacesRepository(
        remoteDataSource: PlacesRemoteDataSource,
        localDataSource: PlacesLocalDataSource,
        mockDataSource: PlacesMockDataSource,
        sourcePolicy: RepositorySourcePolicy,
    ): PlacesRepository =
        DefaultPlacesRepository(
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource,
            mockDataSource = mockDataSource,
            sourcePolicy = sourcePolicy,
        )

    fun provideSearchRepository(
        remoteDataSource: SearchRemoteDataSource,
        localDataSource: SearchLocalDataSource,
        mockDataSource: SearchMockDataSource,
        sourcePolicy: RepositorySourcePolicy,
    ): SearchRepository =
        DefaultSearchRepository(
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource,
            mockDataSource = mockDataSource,
            sourcePolicy = sourcePolicy,
        )
}
