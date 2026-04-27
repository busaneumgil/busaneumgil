package com.ssafy.e102.eumgil.core.designsystem.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val EumPrimary600 = Color(0xFF2563EB)

private val DeepBlue = Color(0xFF0F4C81)
private val DeepBluePressed = Color(0xFF0B3A63)
private val White = Color(0xFFFFFFFF)
private val Ink = Color(0xFF111827)
private val Slate = Color(0xFF374151)
private val Muted = Color(0xFFF4F6F8)
private val Border = Color(0xFFD6DBE1)
private val Amber = Color(0xFFF2B705)
private val AmberContainer = Color(0xFFFFF4CC)
private val Danger = Color(0xFFC53030)

val BusanEumgilLightColorScheme = lightColorScheme(
    primary = DeepBlue,
    onPrimary = White,
    primaryContainer = DeepBluePressed,
    secondary = Amber,
    onSecondary = Ink,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Ink,
    background = White,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Muted,
    onSurfaceVariant = Slate,
    outline = Border,
    error = Danger,
)
