package com.ssafy.e102.eumgil.data.repository.policy

import com.ssafy.e102.eumgil.core.config.AppEnvironment
import com.ssafy.e102.eumgil.data.local.datasource.DebugSettingsLocalDataSource

enum class RepositoryDomain {
    PLACES,
    SEARCH,
    SETTINGS,
}

enum class RepositorySource {
    REMOTE,
    LOCAL,
    MOCK,
}

data class RepositoryReadPlan(
    val sources: List<RepositorySource>,
) {
    init {
        require(sources.isNotEmpty()) { "Repository read plan requires at least one source." }
    }

    companion object {
        fun remoteLocalMock(): RepositoryReadPlan =
            RepositoryReadPlan(
                sources = listOf(RepositorySource.REMOTE, RepositorySource.LOCAL, RepositorySource.MOCK),
            )

        fun localOnly(): RepositoryReadPlan =
            RepositoryReadPlan(
                sources = listOf(RepositorySource.LOCAL),
            )

        fun mockOnly(): RepositoryReadPlan =
            RepositoryReadPlan(
                sources = listOf(RepositorySource.MOCK),
            )
    }
}

interface RepositorySourcePolicy {
    suspend fun readPlan(domain: RepositoryDomain): RepositoryReadPlan
}

class DefaultRepositorySourcePolicy(
    private val debugSettingsLocalDataSource: DebugSettingsLocalDataSource,
) : RepositorySourcePolicy {
    override suspend fun readPlan(domain: RepositoryDomain): RepositoryReadPlan =
        when (domain) {
            RepositoryDomain.SETTINGS -> RepositoryReadPlan.localOnly()
            RepositoryDomain.PLACES,
            RepositoryDomain.SEARCH ->
                if (shouldForceMock()) {
                    RepositoryReadPlan.mockOnly()
                } else {
                    RepositoryReadPlan.remoteLocalMock()
                }
        }

    private suspend fun shouldForceMock(): Boolean =
        AppEnvironment.isMockMode ||
            (AppEnvironment.isDebugBuild && debugSettingsLocalDataSource.getForceMockEnabled())
}
