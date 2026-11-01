package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF0D1117),
    primaryContainer = CyberCyanDark,
    onPrimaryContainer = Color(0xFFE6EDF3),
    secondary = CyberAmber,
    onSecondary = Color(0xFF1E1700),
    secondaryContainer = Color(0xFF4D3800),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = CyberPurple,
    onTertiary = Color(0xFF2B0052),
    background = SlateBg,
    onBackground = SlateTextPrimary,
    surface = SlateSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorder,
    error = CyberRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightWorkbenchPrimary,
    onPrimary = Color.White,
    secondary = CyberAmber,
    tertiary = CyberPurple,
    background = LightWorkbenchBg,
    onBackground = LightWorkbenchTextPrimary,
    surface = LightWorkbenchSurface,
    onSurface = LightWorkbenchTextPrimary,
    surfaceVariant = Color(0xFFEAEEF2),
    onSurfaceVariant = LightWorkbenchTextSecondary,
    outline = LightWorkbenchBorder,
    error = CyberRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep developer aesthetic by default
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
