package com.spectra.lifepilot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette
val BrandCyan = Color(0xFF00C2CB)
val BrandBlue = Color(0xFF0066A6)
val BrandInk = Color(0xFF0B2027)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandCyan,
    primaryContainer = Color(0xFFCDEFF3),
    onPrimaryContainer = BrandInk,
    secondaryContainer = Color(0xFFE3F6F8),
)
private val DarkColors = darkColorScheme(
    primary = BrandCyan,
    onPrimary = BrandInk,
    secondary = BrandCyan,
    primaryContainer = Color(0xFF0E3A42),
    onPrimaryContainer = Color(0xFFCDEFF3),
    secondaryContainer = Color(0xFF12313A),
)

@Composable
fun LifePilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
