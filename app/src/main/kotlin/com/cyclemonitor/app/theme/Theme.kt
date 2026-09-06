package com.cyclemonitor.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val CycleDarkColorScheme = darkColorScheme(
    primary = CycleColors.NavigationCyan,
    onPrimary = CycleColors.BackgroundCharcoal,
    secondary = CycleColors.StatusGreen,
    onSecondary = CycleColors.BackgroundCharcoal,
    tertiary = CycleColors.StatusOrange,
    background = CycleColors.BackgroundCharcoal,
    onBackground = CycleColors.TextPrimary,
    surface = CycleColors.SurfaceCharcoal,
    onSurface = CycleColors.TextPrimary,
    surfaceVariant = CycleColors.SurfaceRaised,
    onSurfaceVariant = CycleColors.TextSecondary,
    outline = CycleColors.OutlineSubtle,
    error = CycleColors.StatusRed,
    onError = CycleColors.TextPrimary,
)

// A light scheme exists for completeness (Settings > Theme), but the product is explicitly
// dark-first: outdoor glanceability and battery both favor dark, so this is the non-default path.
private val CycleLightColorScheme = lightColorScheme(
    primary = CycleColors.NavigationCyan,
    secondary = CycleColors.StatusGreen,
    tertiary = CycleColors.StatusOrange,
    error = CycleColors.StatusRed,
)

private val gaugeNumberStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 56.sp, letterSpacing = (-0.5).sp)

val CycleTypography = Typography(
    displayLarge = gaugeNumberStyle,
)

enum class AppThemeMode { DARK, LIGHT, SYSTEM }

@Composable
fun CycleMonitorTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) CycleDarkColorScheme else CycleLightColorScheme,
        typography = CycleTypography,
        content = content,
    )
}
