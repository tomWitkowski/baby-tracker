package com.babytracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    tertiaryContainer = TertiaryContainer,
    error = Error,
    errorContainer = ErrorContainer,
    background = BackgroundColor,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceVariant = SurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = FeedingColor,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFF8B2020),
    secondary = DiaperColor,
    onSecondary = OnSecondary,
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF252525),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5)
)

@Composable
fun BabyTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
