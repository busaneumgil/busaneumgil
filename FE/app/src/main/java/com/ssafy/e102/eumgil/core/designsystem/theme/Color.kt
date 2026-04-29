package com.ssafy.e102.eumgil.core.designsystem.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val EumWhite = Color(0xFFFFFFFF)
val EumTextPrimary = Color(0xFF111827)
val EumTextSecondary = Color(0xFF374151)
val EumSurfaceMuted = Color(0xFFF3F4F6)
val EumBorderDefault = Color(0xFFD1D5DB)
val EumPrimary600 = Color(0xFF2563EB)
val EumPrimary500 = Color(0xFF3B82F6)
val EumPrimary200 = Color(0xFFDBEAFE)
val EumStatusWarning = Color(0xFFF59E0B)
val EumStatusDanger = Color(0xFFEF4444)

val BusanEumgilLightColorScheme = lightColorScheme(
    primary = EumPrimary600,
    onPrimary = EumWhite,
    primaryContainer = EumPrimary200,
    onPrimaryContainer = EumTextPrimary,
    secondary = EumPrimary500,
    onSecondary = EumWhite,
    secondaryContainer = EumPrimary200,
    onSecondaryContainer = EumTextPrimary,
    background = EumWhite,
    onBackground = EumTextPrimary,
    surface = EumWhite,
    onSurface = EumTextPrimary,
    surfaceVariant = EumSurfaceMuted,
    onSurfaceVariant = EumTextSecondary,
    outline = EumBorderDefault,
    error = EumStatusDanger,
)
