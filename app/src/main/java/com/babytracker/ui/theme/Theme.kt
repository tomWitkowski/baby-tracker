package com.babytracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── App color palette for light / dark ────────────────────────────────────────
data class AppColors(
    val background: Color,
    val surface: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color
)

private val LightAppColors = AppColors(
    background    = Color(0xFFFAF8F5),
    surface       = Color(0xFFFFFFFF),
    divider       = Color(0xFFF0EDE8),
    textPrimary   = Color(0xFF2C2C2C),
    textSecondary = Color(0xFF8A8A8A),
    textHint      = Color(0xFFBBBBBB)
)

private val DarkAppColors = AppColors(
    background    = Color(0xFF1A1A1A),
    surface       = Color(0xFF252525),
    divider       = Color(0xFF333333),
    textPrimary   = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFAAAAAA),
    textHint      = Color(0xFF666666)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

// ── Material color schemes ─────────────────────────────────────────────────────
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
    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF2C2C2C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFF8A8A8A),
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

// ── Theme entry point ──────────────────────────────────────────────────────────
@Composable
fun BabyTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
