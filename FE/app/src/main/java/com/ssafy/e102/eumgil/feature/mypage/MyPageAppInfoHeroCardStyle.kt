package com.ssafy.e102.eumgil.feature.mypage

import androidx.compose.ui.graphics.Color
import com.ssafy.e102.eumgil.core.designsystem.theme.EumPrimary600
import com.ssafy.e102.eumgil.core.designsystem.theme.EumWhite

data class MyPageAppInfoHeroCardStyle(
    val containerColor: Color,
    val borderColor: Color,
    val logoSurfaceColor: Color,
    val titleColor: Color,
    val versionColor: Color,
    val descriptionColor: Color,
)

fun myPageAppInfoHeroCardStyle(): MyPageAppInfoHeroCardStyle =
    MyPageAppInfoHeroCardStyle(
        containerColor = EumPrimary600,
        borderColor = EumPrimary600,
        logoSurfaceColor = EumWhite,
        titleColor = EumWhite,
        versionColor = EumWhite,
        descriptionColor = EumWhite,
    )
