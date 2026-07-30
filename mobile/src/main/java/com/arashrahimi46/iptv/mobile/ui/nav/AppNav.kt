package com.arashrahimi46.iptv.mobile.ui.nav

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.mobile.ui.detail.MovieDetailScreen
import com.arashrahimi46.iptv.mobile.ui.favorites.FavoritesScreen
import com.arashrahimi46.iptv.mobile.ui.guide.GuideScreen
import com.arashrahimi46.iptv.mobile.ui.home.HomeScreen
import com.arashrahimi46.iptv.mobile.ui.live.LiveScreen
import com.arashrahimi46.iptv.mobile.ui.search.SearchScreen
import com.arashrahimi46.iptv.mobile.ui.movies.MoviesViewModel
import com.arashrahimi46.iptv.mobile.ui.movies.SeriesViewModel
import com.arashrahimi46.iptv.mobile.ui.movies.VodGridScreen
import com.arashrahimi46.iptv.mobile.ui.player.PlayerScreen
import com.arashrahimi46.iptv.mobile.ui.player.PlayerTarget
import com.arashrahimi46.iptv.mobile.ui.series.SeriesDetailScreen
import com.arashrahimi46.iptv.mobile.ui.settings.SettingsScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.interaction.AreInteractive
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.ControlTone
import com.arashrahimi46.iptv.ui.theme.ProvideOnGlass
import com.arashrahimi46.iptv.ui.theme.controlSkin
import com.arashrahimi46.iptv.ui.theme.glassSurface

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

/**
 * Bottom tab bar, rebuilt on :core's glass primitives (Step 5 milestone A) in place of the stock
 * Material3 [androidx.compose.material3.NavigationBar]. The bar itself is a page-level glass
 * surface -- it sits directly on the screen, not nested inside another glass panel -- so it takes
 * [glassSurface] (the same "translucent fill + lit hairline edge" every full glass surface uses).
 * Each tab is then a control ONE LEVEL IN, so it takes the nested-child treatment via
 * [ProvideOnGlass] rather than a second glass fill (see ControlSkin.kt: "glass never stacks" --
 * two glassSurface fills would compound to ~87% opacity and read as an opaque bar). The selected
 * tab uses the same accent-lens `controlSkin(selectable = true, selected = ...)` funnel TV's
 * `AreTab` (Tabs.kt) uses, for visual parity between the two apps.
 */
@Composable
fun AppBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.xl)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .glassSurface(shape, elevated = true),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ProvideOnGlass {
            tabs.forEach { tab ->
                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                AppBottomBarItem(
                    tab = tab,
                    selected = selected,
                    onClick = {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AppBottomBarItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val skin = controlSkin(ControlTone.Neutral, selected = selected, selectable = true)
    val label = stringResource(tab.labelRes)
    AreInteractive(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(AreIptvTheme.radius.md),
        backgroundColor = skin.fillColor,
        backgroundBrush = skin.fillBrush,
        shadowElevation = skin.elevation,
        borderColor = skin.borderColor,
        borderBrush = skin.borderBrush,
    ) { _, _ ->
        Column(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(tab.icon, contentDescription = null, tint = skin.content, modifier = Modifier.size(22.dp))
            Text(text = label, style = AreIptvTheme.typography.caption, color = skin.content, maxLines = 1)
        }
    }
}

private const val SERIES_DETAIL_ROUTE = "seriesDetail/{id}"
private fun seriesDetailRoute(id: Long) = "seriesDetail/$id"

private const val MOVIE_DETAIL_ROUTE = "movieDetail/{id}"
private fun movieDetailRoute(id: Long) = "movieDetail/$id"

@Composable
fun AppNavHost(navController: NavHostController, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    // Shared by every rail/grid that shows both movies and series (Home, Movies/Series tabs,
    // Favorites): a series has no stream URL of its own (only its episodes do), so it opens the
    // episode picker instead of jumping straight into the player; a movie now opens its own detail
    // screen (poster/meta/plot/cast + Play) instead of jumping straight into the player either.
    val openTitle: (VodTitle) -> Unit = { title ->
        if (title.isSeries) navController.navigate(seriesDetailRoute(title.id))
        else navController.navigate(movieDetailRoute(title.id))
    }
    val openEpisode: (Long) -> Unit = { episodeId -> navController.navigate(playerRoute("episode", episodeId)) }

    NavHost(navController = navController, startDestination = Tab.Home.route, modifier = modifier) {
        composable(Tab.Home.route) {
            HomeScreen(
                onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) },
                onOpenTitle = openTitle,
                onOpenEpisode = openEpisode,
                onOpenSearch = { navController.navigate("search") },
                onOpenGuide = { navController.navigate("guide") },
            )
        }
        composable(Tab.Live.route) {
            LiveScreen(onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) })
        }
        composable("guide") {
            GuideScreen(onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) })
        }
        composable("search") {
            SearchScreen(
                onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) },
                onOpenTitle = openTitle,
            )
        }
        composable(Tab.Movies.route) {
            val vm: MoviesViewModel = viewModel()
            VodGridScreen(vm, openTitle)
        }
        composable(Tab.Series.route) {
            val vm: SeriesViewModel = viewModel()
            VodGridScreen(vm, openTitle)
        }
        composable(Tab.Settings.route) { SettingsScreen(onOpenFavorites = { navController.navigate("favorites") }) }
        composable("favorites") {
            FavoritesScreen(
                onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) },
                onOpenTitle = openTitle,
            )
        }
        composable(
            route = SERIES_DETAIL_ROUTE,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            SeriesDetailScreen(vodTitleId = id, onOpenEpisode = openEpisode, onBack = { navController.popBackStack() })
        }
        composable(
            route = MOVIE_DETAIL_ROUTE,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            MovieDetailScreen(
                vodTitleId = id,
                onPlay = { navController.navigate(playerRoute("movie", it)) },
                onBack = { navController.popBackStack() },
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
            val target = when (kind) {
                "movie" -> PlayerTarget.Movie(id)
                "episode" -> PlayerTarget.Episode(id)
                else -> PlayerTarget.LiveChannel(id)
            }
            PlayerScreen(target)
        }
    }
}
