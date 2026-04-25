package com.rork.safetygembawalk.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Navy Blue - Primary corporate color
val Navy900 = Color(0xFF0A1628)
val Navy800 = Color(0xFF1A365D)
val Navy700 = Color(0xFF234876)
val Navy600 = Color(0xFF2C5A8F)

// Amber/Gold - Accent color for safety highlights
val Amber600 = Color(0xFFD69E2E)
val Amber500 = Color(0xFFECC94B)
val Amber400 = Color(0xFFF6E05E)
val Amber300 = Color(0xFFFAF089)

// Semantic colors
val SuccessGreen = Color(0xFF38A169)
val WarningOrange = Color(0xFFDD6B20)
val ErrorRed = Color(0xFFE53E3E)
val InfoBlue = Color(0xFF3182CE)

// Light theme colors
private val LightColorScheme = lightColorScheme(
    primary = Navy800,
    onPrimary = Color.White,
    primaryContainer = Navy700,
    onPrimaryContainer = Color.White,
    secondary = Amber600,
    onSecondary = Color.Black,
    secondaryContainer = Amber300,
    onSecondaryContainer = Navy900,
    tertiary = Navy600,
    onTertiary = Color.White,
    tertiaryContainer = Navy700,
    onTertiaryContainer = Color.White,
    background = Color(0xFFF7FAFC),
    onBackground = Navy900,
    surface = Color.White,
    onSurface = Navy900,
    surfaceVariant = Color(0xFFEDF2F7),
    onSurfaceVariant = Navy700,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFED7D7),
    onErrorContainer = ErrorRed,
    outline = Navy600.copy(alpha = 0.5f),
    outlineVariant = Navy600.copy(alpha = 0.2f),
    scrim = Navy900.copy(alpha = 0.5f),
    inverseSurface = Navy800,
    inverseOnSurface = Color.White,
    inversePrimary = Amber500,
    surfaceTint = Navy800.copy(alpha = 0.05f)
)

// Dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = Amber500,
    onPrimary = Navy900,
    primaryContainer = Navy700,
    onPrimaryContainer = Color.White,
    secondary = Amber400,
    onSecondary = Navy900,
    secondaryContainer = Navy600,
    onSecondaryContainer = Color.White,
    tertiary = Amber300,
    onTertiary = Navy900,
    tertiaryContainer = Navy800,
    onTertiaryContainer = Color.White,
    background = Navy900,
    onBackground = Color.White,
    surface = Navy800,
    onSurface = Color.White,
    surfaceVariant = Navy700,
    onSurfaceVariant = Color(0xFFE2E8F0),
    error = Color(0xFFFC8181),
    onError = Navy900,
    errorContainer = Color(0xFF742A2A),
    onErrorContainer = Color(0xFFFED7D7),
    outline = Color(0xFF718096),
    outlineVariant = Color(0xFF4A5568),
    scrim = Navy900.copy(alpha = 0.8f),
    inverseSurface = Color(0xFFF7FAFC),
    inverseOnSurface = Navy900,
    inversePrimary = Navy800,
    surfaceTint = Amber500.copy(alpha = 0.05f)
)

@Composable
fun AppTheme(
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
