package com.checkmatey.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dynamic (wallpaper) colour is intentionally OFF: it steals whatever colours the user's wallpaper
// has, so the app had no consistent brand. This fixed role palette is the brand — light and dark
// are completed together (every role has both), per the design rules.

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentSoft,
    onPrimaryContainer = AccentDark,
    secondary = Info,
    onSecondary = Color.White,
    secondaryContainer = InfoSoft,
    onSecondaryContainer = InfoDark,
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = AmberDark,
    background = CanvasLight,
    onBackground = InkLight,
    surface = CanvasLight,
    onSurface = InkLight,
    surfaceVariant = CardHighLight,
    onSurfaceVariant = BodyLight,
    error = Coral,
    onError = Color.White,
    errorContainer = CoralSoft,
    onErrorContainer = CoralDark,
    outline = OutlineLight,
    outlineVariant = BorderLight,
    surfaceContainer = CardLight,
    surfaceContainerHigh = CardHighLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentLight,
    onPrimary = Color(0xFF00391E),
    primaryContainer = Color(0xFF14724A),
    onPrimaryContainer = AccentSoft,
    secondary = InfoLight,
    onSecondary = Color(0xFF0A1F6B),
    secondaryContainer = Color(0xFF2A4BB8),
    onSecondaryContainer = InfoSoft,
    tertiary = AmberLight,
    onTertiary = Color(0xFF3E2600),
    tertiaryContainer = Color(0xFF5A3A00),
    onTertiaryContainer = AmberSoft,
    background = CanvasDark,
    onBackground = InkDark,
    surface = CanvasDark,
    onSurface = InkDark,
    surfaceVariant = CardHighDark,
    onSurfaceVariant = BodyDark,
    error = CoralLight,
    onError = Color(0xFF5A1512),
    errorContainer = Color(0xFF7A1712),
    onErrorContainer = CoralSoft,
    outline = OutlineDark,
    outlineVariant = BorderDark,
    surfaceContainer = CardDark,
    surfaceContainerHigh = CardHighDark,
)

@Composable
fun CheckmateyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
