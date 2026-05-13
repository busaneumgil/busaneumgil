package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.data.local.dao.BookmarkDao
import com.ssafy.e102.eumgil.data.local.dao.FavoriteRouteDao

interface AccountScopedLocalCacheCleaner {
    suspend fun clearCurrentAccountCache()
}

class DefaultAccountScopedLocalCacheCleaner(
    private val authSessionRepository: AuthSessionRepository,
    private val bookmarkDao: BookmarkDao,
    private val favoriteRouteDao: FavoriteRouteDao,
) : AccountScopedLocalCacheCleaner {
    override suspend fun clearCurrentAccountCache() {
        val accountScopeKey = authSessionRepository.getAccountScopeKey() ?: return
        bookmarkDao.clearBookmarks(accountScopeKey)
        favoriteRouteDao.clearFavoriteRoutes(accountScopeKey)
    }
}
