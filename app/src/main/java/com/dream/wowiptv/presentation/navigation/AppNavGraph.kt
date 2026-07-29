package com.dream.wowiptv.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dream.wowiptv.presentation.main.MainScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            MainScreen()
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("streamType") { type = NavType.StringType },
                navArgument("streamId") { type = NavType.IntType }
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Player Screen")
            }
        }

        composable(
            route = Routes.EPG,
            arguments = listOf(
                navArgument("streamId") { type = NavType.IntType }
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("EPG Screen")
            }
        }

        composable(
            route = Routes.VOD,
            arguments = listOf(
                navArgument("vodId") { type = NavType.IntType }
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("VOD Screen")
            }
        }

        composable(
            route = Routes.SERIES,
            arguments = listOf(
                navArgument("seriesId") { type = NavType.IntType }
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Series Screen")
            }
        }

        composable(Routes.SOURCE_ADD) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Add Source Screen")
            }
        }

        composable(
            route = Routes.SOURCE_EDIT,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.IntType }
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Edit Source Screen")
            }
        }
    }
}
