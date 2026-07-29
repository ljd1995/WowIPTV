package com.dream.wowiptv.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Live : BottomNavItem("live", Icons.Default.Tv, "直播")
    object Movies : BottomNavItem("movies", Icons.Default.Movie, "电影")
    object Series : BottomNavItem("series", Icons.AutoMirrored.Filled.PlaylistPlay, "剧集")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "设置")

    companion object {
        val items = listOf(Live, Movies, Series, Settings)
    }
}
