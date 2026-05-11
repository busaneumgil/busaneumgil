package com.ssafy.e102.eumgil.data.local.datasource

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.RecentSearch
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SearchLocalDataSourceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `recent destinations persist across data source recreation`() =
        runTest {
            val dataStore = createDataStore()
            val firstDataSource = SearchLocalDataSource(dataStore = dataStore)
            val secondDataSource = SearchLocalDataSource(dataStore = dataStore)

            firstDataSource.saveRecentDestination(
                RecentDestination(
                    placeId = "place-1",
                    name = "서울역",
                    address = "서울특별시 용산구 한강대로 405",
                    latitude = 37.5547,
                    longitude = 126.9706,
                    accessibilityTagKeys = listOf("WHEELCHAIR_TOILET", "ELEVATOR", "ELEVATOR"),
                ),
            )

            val recentDestinations = secondDataSource.getRecentDestinations()

            assertEquals(
                listOf(
                    RecentDestination(
                        placeId = "place-1",
                        name = "서울역",
                        address = "서울특별시 용산구 한강대로 405",
                        latitude = 37.5547,
                        longitude = 126.9706,
                        accessibilityTagKeys = listOf("WHEELCHAIR_TOILET", "ELEVATOR"),
                        searchedAtMillis = recentDestinations.single().searchedAtMillis,
                    ),
                ),
                recentDestinations,
            )
        }

    @Test
    fun `recent searches persist across data source recreation`() =
        runTest {
            val dataStore = createDataStore()
            val firstDataSource = SearchLocalDataSource(dataStore = dataStore)
            val secondDataSource = SearchLocalDataSource(dataStore = dataStore)

            firstDataSource.saveRecentSearch("부산역")

            val recentSearches = secondDataSource.getRecentSearches()

            assertEquals(
                listOf(
                    RecentSearch(
                        keyword = "부산역",
                        searchedAtMillis = recentSearches.single().searchedAtMillis,
                    ),
                ),
                recentSearches,
            )
        }

    private fun TestScope.createDataStore() =
        PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = {
                File(temporaryFolder.newFolder(), "search_local.preferences_pb")
            },
        )
}
