package com.arashrahimi46.iptv.mobile.ui.nav

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.arashrahimi46.iptv.core.R as CoreR
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
import com.arashrahimi46.iptv.mobile.ui.recordings.RecordingsScreen
import com.arashrahimi46.iptv.mobile.ui.series.SeriesDetailScreen
import com.arashrahimi46.iptv.mobile.ui.streams.StreamsScreen
import com.arashrahimi46.iptv.mobile.ui.settings.SettingsScreen
import com.arashrahimi46.iptv.mobile.ui.settings.AboutSettingsScreen
import com.arashrahimi46.iptv.mobile.ui.settings.ParentalSettingsScreen
import com.arashrahimi46.iptv.mobile.ui.settings.PlaybackSettingsScreen
import com.arashrahimi46.iptv.mobile.ui.settings.SubtitleSettingsScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/** Bottom-nav destinations, per product-lead's Phase 1 spec: Home / Live / Movies / Series / Settings. */
sealed class Tab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : Tab("home", CoreR.string.nav_home, Icons.Filled.Home)
    data object Live : Tab("live", CoreR.string.nav_live_tv, Icons.Filled.LiveTv)
    data object Movies : Tab("movies", CoreR.string.nav_movies, Icons.Filled.Movie)
    data object Series : Tab("series", CoreR.string.nav_series, Icons.Filled.Tv)
    data object Settings : Tab("settings", CoreR.string.nav_settings, Icons.Filled.Settings)
}

private val tabs = listOf(Tab.Home, Tab.Live, Tab.Movies, Tab.Series, Tab.Settings)

/** Routes eligible for [com.arashrahimi46.iptv.data.settings.StartScreen.LAST_USED] tracking and
 * for [AppNavHost]'s startDestination -- the 5 bottom-nav tab routes only, not any child screen. */
val tabRoutes: Set<String> = tabs.map { it.route }.toSet()

private const val PLAYER_ROUTE = "player/{kind}/{id}"
fun playerRoute(kind: String, id: Long) = "player/$kind/$id"

/** True when [route] is the full-screen player -- used by the host Activity to hide the bottom
 * bar and to know whether entering PiP on "leave app" makes sense. */
fun isPlayerRoute(route: String?): Boolean = route?.startsWith("player/") == true

/**
 * Bottom tab bar: the stock Material 3 [NavigationBar].
 *
 * This was previously a hand-built floating pill row on the glass primitives -- a capsule container
 * with each tab drawn as its own smaller capsule via the `controlSkin(selectable = true)` funnel,
 * for visual parity with :tv's `AreTab`. It didn't hold up on a phone. Chasing parity with a D-pad
 * app imported a focus-shaped idiom into a touch surface: five nested capsules inside a capsule read
 * as buttons in a tray rather than navigation, and the selected state was a ~30% accent lens with a
 * neutral label -- the weakest-contrast element on screen doing the most important job (telling you
 * where you are). M3's indicator pill answers that unambiguously, and the component brings correct
 * spec height, touch targets, `Role.Tab` + selected semantics, and its own window-inset handling
 * for free -- all of which the custom version had to restate by hand.
 *
 * The theme is still ours: `AreIptvColors` maps onto [NavigationBarItemDefaults] explicitly, since
 * this app doesn't drive Material's own `colorScheme`.
 */
@Composable
fun AppBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val colors = AreIptvTheme.colors

    // Only Home/Live/Movies/Series/Settings are real tab destinations; every other route (search,
    // guide, favorites, streams, recordings, the detail/player screens) is a child screen pushed
    // FROM a tab, not nested under it in the nav graph -- so a plain `hierarchy` walk (the previous
    // approach) never matched any of them and left the bar with nothing selected while browsing a
    // child screen (this is also why the Home tab looked like it never "activated": returning to a
    // child of Home showed no highlight at all, which read as the tap not registering). Track the
    // last tab actually landed on instead, so the bar keeps highlighting the owning tab through any
    // number of child-screen pushes.
    // Derived from the actual back stack, not accumulated as a side effect. The previous version
    // only ever ASSIGNED when the current route was itself a tab, so any route reached without
    // passing through its owning tab -- Settings > Playback opened while the last visited tab was
    // Series, a deep link, or a process-death restore -- left the bar highlighting whatever tab
    // happened to be latched, e.g. Series while sitting in Settings. Walking the back stack for the
    // nearest tab entry answers "which tab owns this screen" directly, and needs no remembered
    // state to survive recomposition or restore.
    val backStack by navController.currentBackStack.collectAsState()
    val selectedTab: Tab = remember(backStack) {
        backStack.asReversed()
            .firstNotNullOfOrNull { entry -> tabs.firstOrNull { it.route == entry.destination.route } }
            ?: Tab.Home
    }

    NavigationBar(
        // Opaque, matching the top bar. Both are page chrome sitting on the solid page, where a
        // translucent fill composites to little more than a lighter grey while still costing a
        // blend -- and on the top bar it was letting scrolled content render through the title.
        containerColor = colors.surface1,
        contentColor = colors.textPrimary,
        // The 1dp hairline where the bar meets content, same treatment as AreScreenScaffold's bar.
        modifier = Modifier.drawBehind {
            val stroke = 1.dp.toPx()
            drawLine(
                color = colors.borderDefault,
                start = Offset(0f, stroke / 2f),
                end = Offset(size.width, stroke / 2f),
                strokeWidth = stroke,
            )
        },
    ) {
        tabs.forEach { tab ->
            val selected = tab == selectedTab
            val label = stringResource(tab.labelRes)
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
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        // Five items on a narrow handset leave ~60dp each; without this a long
                        // translation (de "Einstellungen", fa "تنظیمات") hard-truncates mid-glyph.
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    // Accent lives in the indicator pill and the active glyph, so "where am I" is
                    // carried by hue AND shape rather than by a faint fill difference alone.
                    selectedIconColor = colors.accentFg,
                    selectedTextColor = colors.textPrimary,
                    indicatorColor = colors.accent,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                ),
            )
        }
    }
}

private const val SERIES_DETAIL_ROUTE = "seriesDetail/{id}"
private fun seriesDetailRoute(id: Long) = "seriesDetail/$id"

private const val MOVIE_DETAIL_ROUTE = "movieDetail/{id}"
private fun movieDetailRoute(id: Long) = "movieDetail/$id"

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    startDestination: String = Tab.Home.route,
) {
    // Shared by every rail/grid that shows both movies and series (Home, Movies/Series tabs,
    // Favorites): a series has no stream URL of its own (only its episodes do), so it opens the
    // episode picker instead of jumping straight into the player; a movie now opens its own detail
    // screen (poster/meta/plot/cast + Play) instead of jumping straight into the player either.
    val openTitle: (VodTitle) -> Unit = { title ->
        if (title.isSeries) navController.navigate(seriesDetailRoute(title.id))
        else navController.navigate(movieDetailRoute(title.id))
    }
    val openEpisode: (Long) -> Unit = { episodeId -> navController.navigate(playerRoute("episode", episodeId)) }

    // Same fast cross-fade as :tv's tab switch (MainActivity.kt) instead of navigation-compose's
    // 700ms default slide -- there is no D-pad/TV concept of "slide from the right" on a bottom-nav
    // phone app, and a long default transition on every tab tap/back read as janky.
    val fade = AreIptvTheme.motion.durFastMs
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(fade)) },
        exitTransition = { fadeOut(tween(fade)) },
    ) {
        composable(Tab.Home.route) {
            HomeScreen(
                onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) },
                onOpenTitle = openTitle,
                onOpenEpisode = openEpisode,
                onOpenSearch = { navController.navigate("search") },
                onOpenGuide = { navController.navigate("guide") },
                onOpenFavorites = { navController.navigate("favorites") },
                onOpenRecordings = { navController.navigate("recordings") },
                onOpenStreams = { navController.navigate("streams") },
            )
        }
        composable(Tab.Live.route) {
            LiveScreen(onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) })
        }
        composable("guide") {
            GuideScreen(
                onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable("search") {
            SearchScreen(
                onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) },
                onOpenTitle = openTitle,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Tab.Movies.route) {
            val vm: MoviesViewModel = viewModel()
            VodGridScreen(vm, openTitle, onOpenSearch = { navController.navigate("search") })
        }
        composable(Tab.Series.route) {
            val vm: SeriesViewModel = viewModel()
            VodGridScreen(vm, openTitle, onOpenSearch = { navController.navigate("search") })
        }
        composable(Tab.Settings.route) {
            SettingsScreen(
                onOpenPlayback = { navController.navigate("settings/playback") },
                onOpenSubtitles = { navController.navigate("settings/subtitles") },
                onOpenParental = { navController.navigate("settings/parental") },
                onOpenAbout = { navController.navigate("settings/about") },
            )
        }
        composable("settings/playback") { PlaybackSettingsScreen(onBack = { navController.popBackStack() }) }
        composable("settings/subtitles") { SubtitleSettingsScreen(onBack = { navController.popBackStack() }) }
        composable("settings/parental") { ParentalSettingsScreen(onBack = { navController.popBackStack() }) }
        composable("settings/about") { AboutSettingsScreen(onBack = { navController.popBackStack() }) }
        composable("favorites") {
            FavoritesScreen(
                onOpenChannel = { navController.navigate(playerRoute("channel", it.id)) },
                onOpenTitle = openTitle,
                onBack = { navController.popBackStack() },
            )
        }
        composable("recordings") {
            RecordingsScreen(
                onPlay = { navController.navigate(playerRoute("recording", it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable("streams") {
            StreamsScreen(
                onPlay = { navController.navigate(playerRoute("direct", it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = SERIES_DETAIL_ROUTE,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            SeriesDetailScreen(
                vodTitleId = id,
                onOpenEpisode = openEpisode,
                onBack = { navController.popBackStack() },
                onPlayTitle = { navController.navigate(playerRoute("movie", it)) },
            )
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
                "recording" -> PlayerTarget.Recording(id)
                "direct" -> PlayerTarget.DirectStream(id)
                else -> PlayerTarget.LiveChannel(id)
            }
            PlayerScreen(target)
        }
    }
}
