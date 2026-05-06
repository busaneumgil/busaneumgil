package com.ssafy.e102.eumgil.di

import com.ssafy.e102.eumgil.core.config.AppEnvironment
import com.ssafy.e102.eumgil.data.local.dao.BookmarkDao
import com.ssafy.e102.eumgil.data.local.dao.FavoriteRouteDao
import com.ssafy.e102.eumgil.data.local.dao.ReportDraftDao
import com.ssafy.e102.eumgil.data.local.dao.ReportOutboxDao
import com.ssafy.e102.eumgil.data.local.datasource.AuthSessionLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.DebugSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.FacilitySeedLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.InitSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.PlacesLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.SearchLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.FacilitySeedMockDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.PlacesMockDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.RouteMockDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.SearchMockDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.AuthRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.BookmarksRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.FavoriteRoutesRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.PlacesRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.SearchRemoteDataSource
import com.ssafy.e102.eumgil.data.repository.AuthLoginRepository
import com.ssafy.e102.eumgil.data.repository.AuthSignupRepository
import com.ssafy.e102.eumgil.data.repository.AuthSessionRepository
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DefaultAuthSessionRepository
import com.ssafy.e102.eumgil.data.repository.DefaultBookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DefaultRouteBookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DefaultFacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.DefaultPlacesRepository
import com.ssafy.e102.eumgil.data.repository.DefaultReportRepository
import com.ssafy.e102.eumgil.data.repository.DefaultRouteRepository
import com.ssafy.e102.eumgil.data.repository.DefaultSearchRepository
import com.ssafy.e102.eumgil.data.repository.DefaultSettingsRepository
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.FacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.LocalOnlyAuthLoginRepository
import com.ssafy.e102.eumgil.data.repository.LocalOnlyAuthSignupRepository
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.ReportRepository
import com.ssafy.e102.eumgil.data.repository.RouteBookmarkRepository
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import com.ssafy.e102.eumgil.data.repository.ServerAuthSignupRepository
import com.ssafy.e102.eumgil.data.repository.ServerAuthLoginRepository
import com.ssafy.e102.eumgil.data.repository.SettingsRepository
import com.ssafy.e102.eumgil.data.repository.SocialAccessTokenProvider
import com.ssafy.e102.eumgil.data.repository.policy.DefaultRepositorySourcePolicy
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySourcePolicy

object RepositoryModule {
    fun provideDestinationSelectionRepository(): DestinationSelectionRepository =
        InMemoryDestinationSelectionRepository()

    fun provideAuthSessionRepository(
        authSessionLocalDataSource: AuthSessionLocalDataSource,
    ): AuthSessionRepository =
        DefaultAuthSessionRepository(authSessionLocalDataSource = authSessionLocalDataSource)

    fun provideAuthLoginRepository(
        authRemoteDataSource: AuthRemoteDataSource,
        socialAccessTokenProvider: SocialAccessTokenProvider,
        authSessionRepository: AuthSessionRepository,
        settingsRepository: SettingsRepository,
    ): AuthLoginRepository =
        if (AppEnvironment.isMockMode) {
            LocalOnlyAuthLoginRepository(authSessionRepository = authSessionRepository)
        } else {
            ServerAuthLoginRepository(
                authRemoteDataSource = authRemoteDataSource,
                socialAccessTokenProvider = socialAccessTokenProvider,
                authSessionRepository = authSessionRepository,
                settingsRepository = settingsRepository,
            )
        }

    fun provideAuthSignupRepository(
        authRemoteDataSource: AuthRemoteDataSource,
        authSessionRepository: AuthSessionRepository,
        settingsRepository: SettingsRepository,
    ): AuthSignupRepository =
        if (AppEnvironment.isMockMode) {
            LocalOnlyAuthSignupRepository()
        } else {
            ServerAuthSignupRepository(
                authRemoteDataSource = authRemoteDataSource,
                authSessionRepository = authSessionRepository,
                settingsRepository = settingsRepository,
            )
        }

    fun provideBookmarkRepository(
        bookmarkDao: BookmarkDao,
        bookmarksRemoteDataSource: BookmarksRemoteDataSource? = null,
        accessTokenProvider: suspend () -> String? = { null },
        initialBookmarks: List<BookmarkData> = emptyList(),
    ): BookmarkRepository =
        DefaultBookmarkRepository(
            bookmarkDao = bookmarkDao,
            bookmarksRemoteDataSource = bookmarksRemoteDataSource,
            accessTokenProvider = accessTokenProvider,
            initialBookmarks = initialBookmarks,
        )

    fun provideRouteBookmarkRepository(
        favoriteRouteDao: FavoriteRouteDao,
        favoriteRoutesRemoteDataSource: FavoriteRoutesRemoteDataSource? = null,
        accessTokenProvider: suspend () -> String? = { null },
    ): RouteBookmarkRepository =
        DefaultRouteBookmarkRepository(
            favoriteRouteDao = favoriteRouteDao,
            favoriteRoutesRemoteDataSource = favoriteRoutesRemoteDataSource,
            accessTokenProvider = accessTokenProvider,
        )

    fun provideSettingsRepository(
        initSettingsLocalDataSource: InitSettingsLocalDataSource,
        debugSettingsLocalDataSourceProvider: () -> DebugSettingsLocalDataSource,
    ): SettingsRepository =
        DefaultSettingsRepository(
            initSettingsLocalDataSource = initSettingsLocalDataSource,
            debugSettingsLocalDataSourceProvider = debugSettingsLocalDataSourceProvider,
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

    fun provideFacilitySeedRepository(
        localDataSource: FacilitySeedLocalDataSource,
        mockDataSource: FacilitySeedMockDataSource,
    ): FacilitySeedRepository =
        DefaultFacilitySeedRepository(
            localDataSource = localDataSource,
            mockDataSource = mockDataSource,
        )

    fun provideRouteRepository(
        localDataSource: RouteLocalDataSource,
        mockDataSource: RouteMockDataSource,
    ): RouteRepository =
        DefaultRouteRepository(
            localDataSource = localDataSource,
            mockDataSource = mockDataSource,
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

    fun provideReportRepository(
        reportDraftDao: ReportDraftDao,
        reportOutboxDao: ReportOutboxDao,
    ): ReportRepository =
        DefaultReportRepository(
            reportDraftDao = reportDraftDao,
            reportOutboxDao = reportOutboxDao,
        )
}
