package com.ssafy.e102.eumgil.feature.search

import com.ssafy.e102.eumgil.R
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchScreenTest {
    @Test
    fun `destination promo banner model stays aligned with dest01 asset`() {
        val model = searchDestinationPromoBannerModel()

        assertEquals(R.drawable.dest01_accessibility_banner, model.imageRes)
        assertEquals(
            R.string.search_screen_promo_banner_content_description,
            model.contentDescriptionRes,
        )
    }
}
