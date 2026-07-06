package com.techv1.xplay.presentation.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NamedNavArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.techv1.xplay.presentation.home.HomeScreen

import com.techv1.xplay.presentation.search.SearchScreen

import com.techv1.xplay.presentation.library.LibraryScreen

import com.techv1.xplay.presentation.player.UnifiedPlayerScreen

// ── Routes ────────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Home    : Screen("home")
    object Search  : Screen("search")
    object Library : Screen("library")
    object Watch : Screen("watch/{videoId}") {
        fun createRoute(videoId: String) = "watch/$videoId"
    }
}

@Composable
fun XPlayNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController   = navController,
        startDestination = Screen.Home.route,
        enterTransition = { fadeIn(animationSpec = tween(250)) },
        exitTransition = { fadeOut(animationSpec = tween(250)) },
        popEnterTransition = { fadeIn(animationSpec = tween(250)) },
        popExitTransition = { fadeOut(animationSpec = tween(250)) }
    ) {
        animatedComposable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        animatedComposable(Screen.Search.route) {
            SearchScreen(navController = navController)
        }
        animatedComposable(Screen.Library.route) {
            LibraryScreen(navController = navController)
        }
        animatedComposable(
            route = Screen.Watch.route,
            arguments = listOf(navArgument("videoId") { type = NavType.StringType })
        ) {
            UnifiedPlayerScreen(navController = navController)
        }
    }
}

private fun NavGraphBuilder.animatedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = { fadeIn(animationSpec = tween(250)) },
        exitTransition = { fadeOut(animationSpec = tween(250)) },
        popEnterTransition = { fadeIn(animationSpec = tween(250)) },
        popExitTransition = { fadeOut(animationSpec = tween(250)) },
        content = content
    )
}
