package com.techv1.xplay.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// ── Routes ────────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Home    : Screen("home")
    object Search  : Screen("search")
    object Library : Screen("library")
    object Details : Screen("details/{videoId}") {
        fun createRoute(videoId: String) = "details/$videoId"
    }
    object Player  : Screen("player/{videoId}") {
        fun createRoute(videoId: String) = "player/$videoId"
    }
}

@Composable
fun XPlayNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController   = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            // HomeScreen(navController = navController)
            // TODO: Milestone 3 — wire in HomeScreen
            PlaceholderScreen(label = "Home")
        }
        composable(Screen.Search.route) {
            PlaceholderScreen(label = "Search")
        }
        composable(Screen.Library.route) {
            PlaceholderScreen(label = "Library")
        }
        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument("videoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
            PlaceholderScreen(label = "Details — $videoId")
        }
        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("videoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
            PlaceholderScreen(label = "Player — $videoId")
        }
    }
}
