package com.ssafy.e102.eumgil.data.repository.policy

import com.ssafy.e102.eumgil.core.config.AppEnvironment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RepositorySourcePolicyTest {
    @Test
    fun `settings domain always uses local only plan`() =
        runBlocking {
            val policy = DefaultRepositorySourcePolicy()

            assertEquals(
                RepositoryReadPlan.localOnly(),
                policy.readPlan(RepositoryDomain.SETTINGS),
            )
        }

    @Test
    fun `places and search domains depend only on build time mock mode`() =
        runBlocking {
            val policy = DefaultRepositorySourcePolicy()
            val expectedPlan =
                if (AppEnvironment.isMockMode) {
                    RepositoryReadPlan.mockOnly()
                } else {
                    RepositoryReadPlan(
                        sources = listOf(RepositorySource.REMOTE, RepositorySource.LOCAL),
                    )
                }

            assertEquals(expectedPlan, policy.readPlan(RepositoryDomain.PLACES))
            assertEquals(expectedPlan, policy.readPlan(RepositoryDomain.SEARCH))
        }
}
