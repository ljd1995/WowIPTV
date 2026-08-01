package com.dream.wowiptv.presentation.common.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class AppColors(
    val isDark: Boolean,
    val background: Brush,
    val glow: Color,
    val glowTopEnd: Color,
    val card: Color,
    val cardBorder: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val divider: Color
)

object AppTheme {
    val dark = AppColors(
        isDark = true,
        background = Brush.verticalGradient(listOf(Color(0xFF201E2A), Color(0xFF141318))),
        glow = Color(0xFF6366F1).copy(alpha = 0.14f),
        glowTopEnd = Color(0xFFA855F7).copy(alpha = 0.10f),
        card = Color.White.copy(alpha = 0.05f),
        cardBorder = Color(0xFF3A3A4A),
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFF999999),
        divider = Color(0xFF3A3A3A)
    )

    val light = AppColors(
        isDark = false,
        background = Brush.verticalGradient(listOf(Color(0xFFF6F4FF), Color(0xFFE7E3F3))),
        glow = Color(0xFF6366F1).copy(alpha = 0.10f),
        glowTopEnd = Color(0xFFA855F7).copy(alpha = 0.08f),
        card = Color.White.copy(alpha = 0.75f),
        cardBorder = Color(0xFFDDD8EE),
        onSurface = Color(0xFF1C1B1F),
        onSurfaceVariant = Color(0xFF6B6B76),
        divider = Color(0xFFE0DDEB)
    )

    var colors: AppColors = dark
        private set

    val isDark: Boolean get() = colors.isDark

    fun setDark(dark: Boolean) {
        colors = if (dark) this.dark else this.light
    }
}
