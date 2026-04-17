package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.PlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceQuery
import com.ssafy.e102.eumgil.core.model.PlaceSummary
import com.ssafy.e102.eumgil.data.local.datasource.PlacesLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.PlacesMockDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.PlacesRemoteDataSource
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryDomain
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySource
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySourcePolicy

interface PlacesRepository {
    suspend fun getPlaces(query: PlaceQuery): List<PlaceSummary>

    suspend fun getPlaceDetail(placeId: String): PlaceDetail?
}

class DefaultPlacesRepository(
    private val remoteDataSource: PlacesRemoteDataSource,
    private val localDataSource: PlacesLocalDataSource,
    private val mockDataSource: PlacesMockDataSource,
    private val sourcePolicy: RepositorySourcePolicy,
) : PlacesRepository {
    override suspend fun getPlaces(query: PlaceQuery): List<PlaceSummary> {
        val readPlan = sourcePolicy.readPlan(RepositoryDomain.PLACES)
        val lastSource = readPlan.sources.last()
        var remoteFailure: Throwable? = null

        for (source in readPlan.sources) {
            when (source) {
                RepositorySource.REMOTE -> {
                    val remoteResult = runCatching { remoteDataSource.getPlaces(query) }
                    if (remoteResult.isSuccess) {
                        val places = remoteResult.getOrDefault(emptyList())
                        localDataSource.updateCachedPlaces(query = query, places = places)
                        return places
                    }
                    remoteFailure = remoteResult.exceptionOrNull()
                }

                RepositorySource.LOCAL -> {
                    val cachedPlaces = localDataSource.getCachedPlaces(query)
                    if (cachedPlaces.isNotEmpty() || source == lastSource) {
                        return cachedPlaces
                    }
                }

                RepositorySource.MOCK -> return mockDataSource.getPlaces(query)
            }
        }

        throw remoteFailure ?: IllegalStateException("No place data source matched the current policy.")
    }

    override suspend fun getPlaceDetail(placeId: String): PlaceDetail? {
        val readPlan = sourcePolicy.readPlan(RepositoryDomain.PLACES)
        val lastSource = readPlan.sources.last()
        var remoteFailure: Throwable? = null

        for (source in readPlan.sources) {
            when (source) {
                RepositorySource.REMOTE -> {
                    val remoteResult = runCatching { remoteDataSource.getPlaceDetail(placeId) }
                    if (remoteResult.isSuccess) {
                        val placeDetail = remoteResult.getOrNull()
                        if (placeDetail != null) {
                            localDataSource.updateCachedPlaceDetail(placeDetail)
                        }
                        return placeDetail
                    }
                    remoteFailure = remoteResult.exceptionOrNull()
                }

                RepositorySource.LOCAL -> {
                    val cachedPlaceDetail = localDataSource.getCachedPlaceDetail(placeId)
                    if (cachedPlaceDetail != null || source == lastSource) {
                        return cachedPlaceDetail
                    }
                }

                RepositorySource.MOCK -> return mockDataSource.getPlaceDetail(placeId)
            }
        }

        throw remoteFailure ?: IllegalStateException("No place detail source matched the current policy.")
    }
}
