package com.dream.wowiptv.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dream.wowiptv.presentation.epg.EpgTimelineScreen
import com.dream.wowiptv.presentation.main.MainScreen
import com.dream.wowiptv.presentation.movies.MovieDetailScreen
import com.dream.wowiptv.presentation.player.PlayerScreen
import com.dream.wowiptv.presentation.series.SeriesDetailScreen
import com.dream.wowiptv.presentation.settings.SourceFormScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            MainScreen(outerNavController = navController)
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("streamType") { type = NavType.StringType },
                navArgument("streamId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val streamType = backStackEntry.arguments?.getString("streamType") ?: "live"
            val streamId = backStackEntry.arguments?.getString("streamId") ?: "0"
            PlayerScreen(
                streamType = streamType,
                streamId = streamId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EPG,
            arguments = listOf(
                navArgument("streamId") { type = NavType.IntType }
            )
        ) {
            EpgTimelineScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlayChannel = { streamId ->
                    navController.navigate(Routes.playerRoute("live", streamId.toString()))
                }
            )
        }

        composable(
            route = Routes.VOD,
            arguments = listOf(
                navArgument("vodId") { type = NavType.IntType }
            )
        ) {
            MovieDetailScreen(
                onBack = { navController.popBackStack() },
                onPlay = { vodId ->
                    navController.navigate(Routes.playerRoute("vod", vodId.toString()))
                }
            )
        }

        composable(
            route = Routes.SERIES,
            arguments = listOf(
                navArgument("seriesId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getInt("seriesId") ?: 0
            SeriesDetailScreen(
                seriesId = seriesId,
                onBack = { navController.popBackStack() },
                onPlayEpisode = { episodeId ->
                    navController.navigate(Routes.playerRoute("series", episodeId))
                }
            )
        }

        composable(Routes.SOURCE_ADD) {
            SourceFormScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.SOURCE_EDIT,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getInt("sourceId")?.toLong()
            SourceFormScreen(
                sourceId = sourceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
