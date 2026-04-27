package com.ssafy.e102.eumgil.app

import android.content.Context
import com.ssafy.e102.eumgil.core.config.AppEnvironment
import com.ssafy.e102.eumgil.core.location.AndroidCurrentLocationManager
import com.ssafy.e102.eumgil.core.location.AndroidLocationPermissionManager
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionManager
import com.ssafy.e102.eumgil.data.local.db.EumgilDatabase
import com.ssafy.e102.eumgil.data.local.datasource.DebugSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.FacilitySeedLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.InitSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.PlacesLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.SearchLocalDataSource
import com.ssafy.e102.eumgil.data.local.datastore.initSettingsDataStore
import com.ssafy.e102.eumgil.data.mock.datasource.FacilitySeedMockDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.PlacesMockDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.RouteMockDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.SearchMockDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.PlacesRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.SearchRemoteDataSource
import com.ssafy.e102.eumgil.data.repository.AuthSessionRepository
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.FacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.ReportRepository
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import com.ssafy.e102.eumgil.data.repository.SettingsRepository
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySourcePolicy
import com.ssafy.e102.eumgil.di.RepositoryModule

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    val localDatabase: EumgilDatabase by lazy(LazyThreadSafetyMode.NONE) {
        EumgilDatabase.getInstance(appContext)
    }

    private val initSettingsLocalDataSource =
        InitSettingsLocalDataSource(dataStore = appContext.initSettingsDataStore)

    private val debugSettingsLocalDataSource =
        DebugSettingsLocalDataSource(appSettingDao = localDatabase.appSettingDao())

    private val placesLocalDataSource = PlacesLocalDataSource()
    private val facilitySeedLocalDataSource = FacilitySeedLocalDataSource()
    private val routeLocalDataSource = RouteLocalDataSource()
    private val searchLocalDataSource = SearchLocalDataSource()

    private val placesRemoteDataSource = PlacesRemoteDataSource(baseUrl = AppEnvironment.baseUrl)
    private val searchRemoteDataSource = SearchRemoteDataSource(baseUrl = AppEnvironment.baseUrl)

    private val placesMockDataSource = PlacesMockDataSource()
    private val facilitySeedMockDataSource = FacilitySeedMockDataSource()
    private val routeMockDataSource = RouteMockDataSource()
    private val searchMockDataSource = SearchMockDataSource()

    private val repositorySourcePolicy: RepositorySourcePolicy =
        RepositoryModule.provideRepositorySourcePolicy(
            debugSettingsLocalDataSource = debugSettingsLocalDataSource,
        )

    val destinationSelectionRepository: DestinationSelectionRepository =
        RepositoryModule.provideDestinationSelectionRepository()

    val authSessionRepository: AuthSessionRepository =
        RepositoryModule.provideAuthSessionRepository()

    val bookmarkRepository: BookmarkRepository =
        RepositoryModule.provideBookmarkRepository(
            bookmarkDao = localDatabase.bookmarkDao(),
        )

    val settingsRepository: SettingsRepository =
        RepositoryModule.provideSettingsRepository(
            initSettingsLocalDataSource = initSettingsLocalDataSource,
            debugSettingsLocalDataSource = debugSettingsLocalDataSource,
        )

    val placesRepository: PlacesRepository =
        RepositoryModule.providePlacesRepository(
            remoteDataSource = placesRemoteDataSource,
            localDataSource = placesLocalDataSource,
            mockDataSource = placesMockDataSource,
            sourcePolicy = repositorySourcePolicy,
        )

    val facilitySeedRepository: FacilitySeedRepository =
        RepositoryModule.provideFacilitySeedRepository(
            localDataSource = facilitySeedLocalDataSource,
            mockDataSource = facilitySeedMockDataSource,
        )

    val routeRepository: RouteRepository =
        RepositoryModule.provideRouteRepository(
            localDataSource = routeLocalDataSource,
            mockDataSource = routeMockDataSource,
        )

    val searchRepository: SearchRepository =
        RepositoryModule.provideSearchRepository(
            remoteDataSource = searchRemoteDataSource,
            localDataSource = searchLocalDataSource,
            mockDataSource = searchMockDataSource,
            sourcePolicy = repositorySourcePolicy,
        )

    val reportRepository: ReportRepository =
        RepositoryModule.provideReportRepository(
            reportDraftDao = localDatabase.reportDraftDao(),
            reportOutboxDao = localDatabase.reportOutboxDao(),
        )

    val locationPermissionManager: LocationPermissionManager =
        AndroidLocationPermissionManager(context = appContext)

    val currentLocationManager: CurrentLocationManager =
        AndroidCurrentLocationManager(context = appContext)
}
