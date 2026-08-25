package com.spectra.lifepilot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF00838F)
private val TealLight = Color(0xFF4DD0E1)

private val LightColors = lightColorScheme(primary = Teal, secondary = Teal)
private val DarkColors = darkColorScheme(primary = TealLight, secondary = TealLight)

@Composable
fun LifePilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
