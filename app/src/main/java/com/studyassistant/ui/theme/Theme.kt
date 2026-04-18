package com.studyassistant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Soft Doodle Theme Colors ────────────────────────────────────────────
// Primary: Soft Purple (#788AC1)

// Secondary: Soft Purple
val AccentPurple = Color(0xFFB7A1E2)
val AccentPurpleDark = Color(0xFF9A86C1)

// Neutral & Background
val BackgroundDark = Color(0xFF1F1A28)
val CardDark = Color(0xFF3D3251)      // Soft dark card

// Status Colors
val ErrorRed = Color(0xFFE53935)
val WarningAmber = Color(0xFFFFB300)
val WeakAreaRed = Color(0xFFE85D75)   // Soft red-pink
val StrongAreaGreen = Color(0xFF4CAF50)

// Text Colors
val TextOnCards = Color(0xFF000000)   // Black text on cards
val TextSecondary = Color(0xFF000000) // Black text
val TextLight = Color(0xFF000000)     // Black text

private val LightColorScheme = lightColorScheme(
    primary = AccentPurple,
    onPrimary = TextOnCards,  // Black text on purple
    primaryContainer = AccentPurple.copy(alpha = 0.15f),
    secondary = AccentPurple,
    onSecondary = TextOnCards,  // Black text on purple
    secondaryContainer = AccentPurple.copy(alpha = 0.1f),
    background = Color(0xFFFFFFFF),  // Pure white
    surface = Color(0xFFFFFFFF),  // Pure white
    onBackground = Color(0xFF000000),  // Black text on white background
    onSurface = Color(0xFF000000),  // Black text on white surface
    error = ErrorRed,
    outline = Color(0xFF000000)  // Black outline/border
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = TextOnCards, // Force black text on purple
    primaryContainer = AccentPurpleDark.copy(alpha = 0.3f),
    secondary = AccentPurple,
    onSecondary = TextOnCards, // Force black text on purple
    secondaryContainer = AccentPurple.copy(alpha = 0.15f),
    background = BackgroundDark,
    surface = CardDark,
    onBackground = TextLight,
    onSurface = AccentPurple,
    error = ErrorRed,
    outline = AccentPurple.copy(alpha = 0.3f)
)

@Composable
fun StudyAssistantTheme(
    darkTheme: Boolean = false,  // Force light mode by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp)
        ),
        content = content
    )
}
