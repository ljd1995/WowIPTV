package com.dream.wowiptv.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dream.wowiptv.presentation.live.LiveScreen
import com.dream.wowiptv.presentation.navigation.BottomNavItem
import com.dream.wowiptv.presentation.navigation.Routes
import com.dream.wowiptv.presentation.settings.SettingsScreen

@Composable
fun MainScreen(outerNavController: NavHostController) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Live.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Live.route) {
                LiveScreen(
                    onPlayStream = { streamId ->
                        outerNavController.navigate(Routes.playerRoute("live", streamId))
                    },
                    onNavigateToEpg = { streamId ->
                        outerNavController.navigate(Routes.epgRoute(streamId))
                    }
                )
            }
            composable(BottomNavItem.Movies.route) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("电影")
                }
            }
            composable(BottomNavItem.Series.route) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("剧集")
                }
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    onAddSource = { outerNavController.navigate(Routes.SOURCE_ADD) },
                    onEditSource = { sourceId -> outerNavController.navigate(Routes.sourceEditRoute(sourceId.toInt())) }
                )
            }
        }
    }
}
