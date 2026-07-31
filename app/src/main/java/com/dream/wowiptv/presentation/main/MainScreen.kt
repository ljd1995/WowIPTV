package com.dream.wowiptv.presentation.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dream.wowiptv.presentation.home.HomeScreen
import com.dream.wowiptv.presentation.live.LiveScreen
import com.dream.wowiptv.presentation.movies.MoviesScreen
import com.dream.wowiptv.presentation.navigation.BottomNavItem
import com.dream.wowiptv.presentation.navigation.Routes
import com.dream.wowiptv.presentation.series.SeriesScreen
import com.dream.wowiptv.presentation.settings.SettingsScreen
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme
import com.dream.wowiptv.presentation.common.SourceTypeViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MainScreen(outerNavController: NavHostController, pendingLiveStreamArg: Int? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var hideBottomBar by remember { mutableStateOf(false) }
    var pendingLiveStream by remember { mutableStateOf(pendingLiveStreamArg) }
    val sourceTypeViewModel: SourceTypeViewModel = hiltViewModel()
    val sourceType by sourceTypeViewModel.sourceType.collectAsState()

    LaunchedEffect(sourceType) {
        if (sourceType == "m3u" &&
            currentRoute in listOf(BottomNavItem.Movies.route, BottomNavItem.Series.route)
        ) {
            navController.navigate(BottomNavItem.Home.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(pendingLiveStreamArg) {
        if (pendingLiveStreamArg != null) {
            navController.navigate(BottomNavItem.Live.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF1A1A1A),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!hideBottomBar) {
                MaterialTheme(colorScheme = DarkColorScheme) {
                    val visibleItems = if (sourceType == "m3u") {
                        BottomNavItem.items.filter {
                            it.route != BottomNavItem.Movies.route && it.route != BottomNavItem.Series.route
                        }
                    } else {
                        BottomNavItem.items
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A1A))
                            .navigationBarsPadding()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color(0xFF6366F1).copy(alpha = 0.6f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            visibleItems.forEach { item ->
                                val selected = currentRoute == item.route
                                val itemColor = if (selected) Color(0xFF8B5CF6) else Color(0xFF8A8A93)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = stringResource(item.labelRes),
                                        tint = itemColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = stringResource(item.labelRes),
                                        color = itemColor,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
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
                    onLiveClick = { streamId, name ->
                        pendingLiveStream = streamId
                        navController.navigate(BottomNavItem.Live.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
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
                    pendingStreamId = pendingLiveStream,
                    onStreamPlayed = { pendingLiveStream = null },
                    onFullscreenChanged = { fullscreen ->
                        hideBottomBar = fullscreen
                    },
                    onOpenEpg = { streamId ->
                        outerNavController.navigate(Routes.epgRoute(streamId))
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
