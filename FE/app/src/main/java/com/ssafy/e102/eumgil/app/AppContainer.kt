package com.ssafy.e102.eumgil.app

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ssafy.e102.eumgil.core.config.AppEnvironment
import com.ssafy.e102.eumgil.core.location.AndroidCurrentLocationManager
import com.ssafy.e102.eumgil.core.location.AndroidLocationPermissionManager
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionManager
import com.ssafy.e102.eumgil.data.local.datasource.AuthSessionLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.DebugSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.FacilitySeedLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.InitSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.PlacesLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.SearchLocalDataSource
import com.ssafy.e102.eumgil.data.local.datastore.initSettingsDataStore
import com.ssafy.e102.eumgil.data.local.db.EumgilDatabase
import com.ssafy.e102.eumgil.data.mock.datasource.FacilitySeedMockDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.PlacesMockDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.RouteMockDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.SearchMockDataSource
import com.ssafy.e102.eumgil.data.mock.fixture.MockBookmarkFixtures
import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.datasource.AuthRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.PlacesRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.SearchRemoteDataSource
import com.ssafy.e102.eumgil.data.repository.AuthLoginRepository
import com.ssafy.e102.eumgil.data.repository.AuthSessionRepository
import com.ssafy.e102.eumgil.data.repository.AuthSocialProvider
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.CompositeSocialAccessTokenProvider
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.FacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.GoogleSocialAccessTokenProvider
import com.ssafy.e102.eumgil.data.repository.KakaoSocialAccessTokenProvider
import com.ssafy.e102.eumgil.data.repository.NaverSocialAccessTokenProvider
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.ReportRepository
import com.ssafy.e102.eumgil.data.repository.RouteBookmarkRepository
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

    private val initSettingsLocalDataSource by lazy(LazyThreadSafetyMode.NONE) {
        InitSettingsLocalDataSource(dataStore = appContext.initSettingsDataStore)
    }

    private val authSessionDataStore by lazy(LazyThreadSafetyMode.NONE) {
        PreferenceDataStoreFactory.create(
            produceFile = { appContext.preferencesDataStoreFile("auth_session") },
        )
    }

    private val authSessionLocalDataSource by lazy(LazyThreadSafetyMode.NONE) {
        AuthSessionLocalDataSource(dataStore = authSessionDataStore)
    }

    private val debugSettingsLocalDataSource by lazy(LazyThreadSafetyMode.NONE) {
        DebugSettingsLocalDataSource(appSettingDao = localDatabase.appSettingDao())
    }

    private val placesLocalDataSource by lazy(LazyThreadSafetyMode.NONE) { PlacesLocalDataSource() }
    private val facilitySeedLocalDataSource by lazy(LazyThreadSafetyMode.NONE) { FacilitySeedLocalDataSource() }
    private val routeLocalDataSource by lazy(LazyThreadSafetyMode.NONE) { RouteLocalDataSource() }
    private val searchLocalDataSource by lazy(LazyThreadSafetyMode.NONE) { SearchLocalDataSource() }

    private val httpJsonClient by lazy(LazyThreadSafetyMode.NONE) {
        HttpJsonClient(baseUrl = AppEnvironment.baseUrl)
    }
    private val authRemoteDataSource by lazy(LazyThreadSafetyMode.NONE) {
        AuthRemoteDataSource(httpJsonClient = httpJsonClient)
    }
    private val placesRemoteDataSource by lazy(LazyThreadSafetyMode.NONE) {
        PlacesRemoteDataSource(baseUrl = AppEnvironment.baseUrl)
    }
    private val searchRemoteDataSource by lazy(LazyThreadSafetyMode.NONE) {
        SearchRemoteDataSource(baseUrl = AppEnvironment.baseUrl)
    }

    private val placesMockDataSource by lazy(LazyThreadSafetyMode.NONE) { PlacesMockDataSource() }
    private val facilitySeedMockDataSource by lazy(LazyThreadSafetyMode.NONE) { FacilitySeedMockDataSource() }
    private val routeMockDataSource by lazy(LazyThreadSafetyMode.NONE) { RouteMockDataSource() }
    private val searchMockDataSource by lazy(LazyThreadSafetyMode.NONE) { SearchMockDataSource() }

    private val repositorySourcePolicy: RepositorySourcePolicy by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.provideRepositorySourcePolicy(
            debugSettingsLocalDataSource = debugSettingsLocalDataSource,
        )
    }

    val destinationSelectionRepository: DestinationSelectionRepository by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.provideDestinationSelectionRepository()
    }

    val authSessionRepository: AuthSessionRepository by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.provideAuthSessionRepository(
            authSessionLocalDataSource = authSessionLocalDataSource,
        )
    }

    val authLoginRepository: AuthLoginRepository by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.provideAuthLoginRepository(
            authRemoteDataSource = authRemoteDataSource,
            socialAccessTokenProvider =
                CompositeSocialAccessTokenProvider(
                    providersBySocialProvider =
                        mapOf(
                            AuthSocialProvider.KAKAO to
                                KakaoSocialAccessTokenProvider(context = appContext),
                            AuthSocialProvider.GOOGLE to
                                GoogleSocialAccessTokenProvider(
                                    activityProvider = { ForegroundActivityProvider.currentActivity },
                                ),
                            AuthSocialProvider.NAVER to
                                NaverSocialAccessTokenProvider(
                                    activityProvider = { ForegroundActivityProvider.currentActivity },
                                ),
                        ),
                ),
            authSessionRepository = authSessionRepository,
            settingsRepository = settingsRepository,
        )
    }

    val bookmarkRepository: BookmarkRepository by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.provideBookmarkRepository(
            bookmarkDao = localDatabase.bookmarkDao(),
            initialBookmarks =
                if (AppEnvironment.isDebugBuild) {
                    MockBookmarkFixtures.defaultBookmarks
                } else {
                    emptyList()
                },
        )
    }

    val routeBookmarkRepository: RouteBookmarkRepository by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.provideRouteBookmarkRepository()
    }

    val settingsRepository: SettingsRepository by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.provideSettingsRepository(
            initSettingsLocalDataSource = initSettingsLocalDataSource,
            debugSettingsLocalDataSourceProvider = { debugSettingsLocalDataSource },
        )
    }

    val placesRepository: PlacesRepository by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.providePlacesRepository(
            remoteDataSource = placesRemoteDataSource,
            localDataSource = placesLocalDataSource,
            mockDataSource = placesMockDataSource,
            sourcePolicy = repositorySourcePolicy,
        )
    }

    val facilitySeedRepository: FacilitySeedRepository by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.provideFacilitySeedRepository(
            localDataSource = facilitySeedLocalDataSource,
            mockDataSource = facilitySeedMockDataSource,
        )
    }

    val routeRepository: RouteRepository by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.provideRouteRepository(
            localDataSource = routeLocalDataSource,
            mockDataSource = routeMockDataSource,
        )
    }

    val searchRepository: SearchRepository by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.provideSearchRepository(
            remoteDataSource = searchRemoteDataSource,
            localDataSource = searchLocalDataSource,
            mockDataSource = searchMockDataSource,
            sourcePolicy = repositorySourcePolicy,
        )
    }

    val reportRepository: ReportRepository by lazy(LazyThreadSafetyMode.NONE) {
        RepositoryModule.provideReportRepository(
            reportDraftDao = localDatabase.reportDraftDao(),
            reportOutboxDao = localDatabase.reportOutboxDao(),
        )
    }

    val locationPermissionManager: LocationPermissionManager by lazy(LazyThreadSafetyMode.NONE) {
        AndroidLocationPermissionManager(context = appContext)
    }

    val currentLocationManager: CurrentLocationManager by lazy(LazyThreadSafetyMode.NONE) {
        AndroidCurrentLocationManager(context = appContext)
    }
}
