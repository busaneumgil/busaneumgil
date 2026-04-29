package com.ssafy.e102.eumgil.feature.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchScreenTest {
    @Test
    fun `search screen destination exposes entry and results modes`() {
        assertEquals(
            listOf(SearchScreenDestination.Entry, SearchScreenDestination.Results),
            SearchScreenDestination.entries.toList(),
        )
    }
}
