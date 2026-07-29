package com.arashrahimi46.iptv.mobile.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.mobile.ui.favorites.FavoritesScreen
import com.arashrahimi46.iptv.mobile.ui.home.HomeScreen
import com.arashrahimi46.iptv.mobile.ui.live.LiveScreen
import com.arashrahimi46.iptv.mobile.ui.movies.MoviesViewModel
import com.arashrahimi46.iptv.mobile.ui.movies.SeriesViewModel
import com.arashrahimi46.iptv.mobile.ui.movies.VodGridScreen
import com.arashrahimi46.iptv.mobile.ui.player.PlayerScreen
import com.arashrahimi46.iptv.mobile.ui.player.PlayerTarget
import com.arashrahimi46.iptv.mobile.ui.settings.SettingsScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

/** Bottom-nav destinations, per product-lead's Phase 1 spec: Home / Live / Movies / Series / Settings. */
sealed class Tab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : Tab("home", R.string.nav_home, Icons.Filled.Home)
    data object Live : Tab("live", R.string.nav_live_tv, Icons.Filled.LiveTv)
    data object Movies : Tab("movies", R.string.nav_movies, Icons.Filled.Movie)
    data object Series : Tab("series", R.string.nav_series, Icons.Filled.Tv)
    data object Settings : Tab("settings", R.string.nav_settings, Icons.Filled.Settings)
}

private val tabs = listOf(Tab.Home, Tab.Live, Tab.Movies, Tab.Series, Tab.Settings)

private const val PLAYER_ROUTE = "player/{kind}/{id}"
fun playerRoute(kind: String, id: Long) = "player/$kind/$id"

/** True when [route] is the full-screen player -- used by the host Activity to hide the bottom
 * bar and to know whether entering PiP on "leave app" makes sense. */
fun isPlayerRoute(route: String?): Boolean = route?.startsWith("player/") == true

@Composable
fun AppBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    NavigationBar {
        tabs.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    NavHost(navController = navController, startDestination = Tab.Home.route, modifier = modifier) {
        composable(Tab.Home.route) {
            HomeScreen(
                onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) },
                onOpenTitle = { navController.navigate(playerRoute("movie", it.id)) },
            )
        }
        composable(Tab.Live.route) {
            LiveScreen(onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) })
        }
        composable(Tab.Movies.route) {
            val vm: MoviesViewModel = viewModel()
            VodGridScreen(vm) { navController.navigate(playerRoute("movie", it.id)) }
        }
        composable(Tab.Series.route) {
            val vm: SeriesViewModel = viewModel()
            VodGridScreen(vm) { navController.navigate(playerRoute("movie", it.id)) }
        }
        composable(Tab.Settings.route) { SettingsScreen(onOpenFavorites = { navController.navigate("favorites") }) }
        composable("favorites") {
            FavoritesScreen(
                onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) },
                onOpenTitle = { navController.navigate(playerRoute("movie", it.id)) },
            )
        }
        composable(
            route = PLAYER_ROUTE,
            arguments = listOf(
                navArgument("kind") { type = NavType.StringType },
                navArgument("id") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val kind = backStackEntry.arguments?.getString("kind") ?: "channel"
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            val target = if (kind == "movie") PlayerTarget.Movie(id) else PlayerTarget.LiveChannel(id)
            PlayerScreen(target)
        }
    }
}
