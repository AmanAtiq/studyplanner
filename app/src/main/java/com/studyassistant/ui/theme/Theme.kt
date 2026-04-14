package com.studyassistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Colors ────────────────────────────────────────────
val PrimaryGreen = Color(0xFF1DB954)
val PrimaryGreenDark = Color(0xFF17A349)
val SecondaryBlue = Color(0xFF2196F3)
val AccentOrange = Color(0xFFFF6B35)
val SurfaceLight = Color(0xFFF8F9FA)
val SurfaceDark = Color(0xFF1A1A2E)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF16213E)
val BackgroundDark = Color(0xFF0F0E17)
val UrduAccent = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFE53935)
val WarningAmber = Color(0xFFFFB300)
val WeakAreaRed = Color(0xFFFF5252)
val StrongAreaGreen = Color(0xFF4CAF50)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F5E3),
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    background = SurfaceLight,
    surface = CardLight,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    error = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003820),
    secondary = SecondaryBlue,
    onSecondary = Color.Black,
    background = BackgroundDark,
    surface = CardDark,
    onBackground = Color(0xFFE8E8F0),
    onSurface = Color(0xFFE8E8F0),
    error = ErrorRed
)

@Composable
fun StudyAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}