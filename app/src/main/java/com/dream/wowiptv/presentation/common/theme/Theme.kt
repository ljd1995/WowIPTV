package com.dream.wowiptv.presentation.common.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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
        colorScheme = DarkColorScheme,
        typography = WowIPTVTypography,
        content = content
    )
}
