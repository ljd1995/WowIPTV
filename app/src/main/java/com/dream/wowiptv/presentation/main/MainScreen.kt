package com.dream.wowiptv.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.dream.wowiptv.presentation.home.HomeScreen
import com.dream.wowiptv.presentation.live.LiveScreen
import com.dream.wowiptv.presentation.movies.MoviesScreen
import com.dream.wowiptv.presentation.navigation.BottomNavItem
import com.dream.wowiptv.presentation.navigation.Routes
import com.dream.wowiptv.presentation.series.SeriesScreen
import com.dream.wowiptv.presentation.settings.SettingsScreen
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme

@Composable
fun MainScreen(outerNavController: NavHostController) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var hideBottomBar by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF1A1A1A),
        bottomBar = {
            if (!hideBottomBar) {
                MaterialTheme(colorScheme = DarkColorScheme) {
                    NavigationBar(
                        containerColor = Color(0xFF1A1A1A)
                    ) {
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
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = BottomNavItem.Home.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                HomeScreen(
                    onMovieClick = { vodId ->
                        outerNavController.navigate(Routes.vodRoute(vodId))
                    },
                    onSeriesClick = { seriesId ->
                        outerNavController.navigate(Routes.seriesRoute(seriesId))
                    },
                    onViewAllFavorites = {
                        outerNavController.navigate(Routes.ALL_FAVORITES)
                    },
                    onViewAllRecent = {
                        outerNavController.navigate(Routes.allItemsRoute(0))
                    },
                    onViewAllHistory = {
                        outerNavController.navigate(Routes.ALL_HISTORY)
                    }
                )
            }
            composable(
                route = BottomNavItem.Live.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                LiveScreen(
                    onFullscreenChanged = { fullscreen ->
                        hideBottomBar = fullscreen
                    }
                )
            }
            composable(
                route = BottomNavItem.Movies.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                MoviesScreen(
                    onMovieClick = { vodId ->
                        outerNavController.navigate(Routes.vodRoute(vodId))
                    }
                )
            }
            composable(
                route = BottomNavItem.Series.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                SeriesScreen(
                    onSeriesClick = { seriesId ->
                        outerNavController.navigate(Routes.seriesRoute(seriesId))
                    }
                )
            }
            composable(
                route = BottomNavItem.Settings.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                SettingsScreen(
                    onAddSource = { outerNavController.navigate(Routes.SOURCE_ADD) },
                    onEditSource = { sourceId -> outerNavController.navigate(Routes.sourceEditRoute(sourceId.toInt())) }
                )
            }
        }
    }
}
