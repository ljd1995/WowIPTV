package com.dream.wowiptv.presentation.common.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = OnAccentBlue,
    primaryContainer = AccentBlueVariant,
    secondary = AccentBlueVariant,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    error = LightError,
    onError = LightOnError,
    outline = LightOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = OnAccentBlue,
    primaryContainer = AccentBlueVariant,
    secondary = AccentBlueVariant,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = DarkError,
    onError = DarkOnError,
    outline = DarkOutline
)

@Composable
fun WowIPTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = WowIPTVTypography,
        content = content
    )
}
