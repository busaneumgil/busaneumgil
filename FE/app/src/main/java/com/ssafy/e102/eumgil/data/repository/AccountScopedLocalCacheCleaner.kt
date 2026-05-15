package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.data.local.dao.BookmarkDao
import com.ssafy.e102.eumgil.data.local.dao.FavoriteRouteDao
import com.ssafy.e102.eumgil.data.local.datasource.PlacesLocalDataSource

interface AccountScopedLocalCacheCleaner {
    suspend fun clearCurrentAccountCache()
}

class DefaultAccountScopedLocalCacheCleaner(
    private val authSessionRepository: AuthSessionRepository,
    private val bookmarkDao: BookmarkDao,
    private val favoriteRouteDao: FavoriteRouteDao,
    private val placesLocalDataSource: PlacesLocalDataSource? = null,
    private val destinationSelectionRepository: DestinationSelectionRepository? = null,
    private val destinationPreviewRepository: DestinationPreviewRepository? = null,
) : AccountScopedLocalCacheCleaner {
    override suspend fun clearCurrentAccountCache() {
        authSessionRepository.getAccountScopeKey()?.let { accountScopeKey ->
            bookmarkDao.clearBookmarks(accountScopeKey)
            favoriteRouteDao.clearFavoriteRoutes(accountScopeKey)
        }
        placesLocalDataSource?.clearCurrentAccountCache()
        destinationSelectionRepository?.clearSelectedOriginSilently()
        destinationSelectionRepository?.clearSelectedDestination()
        destinationSelectionRepository?.setEditingTarget(RouteEditingTarget.DESTINATION)
        destinationPreviewRepository?.clearPreview()
    }
}
