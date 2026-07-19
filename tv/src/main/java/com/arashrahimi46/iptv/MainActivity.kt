package com.arashrahimi46.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.detail.DetailScreen
import com.arashrahimi46.iptv.ui.detail.PlayTarget
import com.arashrahimi46.iptv.ui.favorites.FavoritesScreen
import com.arashrahimi46.iptv.ui.guide.GuideScreen
import com.arashrahimi46.iptv.ui.home.HomeScreen
import com.arashrahimi46.iptv.ui.live.LiveScreen
import com.arashrahimi46.iptv.ui.movies.MoviesScreen
import com.arashrahimi46.iptv.ui.onboarding.OnboardingFlow
import com.arashrahimi46.iptv.ui.player.LivePlayerScreen
import com.arashrahimi46.iptv.ui.player.PlaybackSource
import com.arashrahimi46.iptv.ui.search.SearchScreen
import com.arashrahimi46.iptv.ui.series.SeriesScreen
import com.arashrahimi46.iptv.ui.settings.SettingsScreen
import com.arashrahimi46.iptv.ui.shell.AreIptvAppShell
import com.arashrahimi46.iptv.ui.shell.AreTopBar
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AreIptvApp()
        }
    }
}

/**
 * Top-level app graph (real back stack, not a boolean flag -- see report):
 * "onboarding" while there's no active [com.arashrahimi46.iptv.data.model.PlaylistSource],
 * "home"/"live"/"guide"/"movies"/"series"/"search" (each wrapped in
 * [AreIptvAppShell]) once one exists, and the full-bleed overlays
 * "player/{channelId}", "player/vod/{vodTitleId}", "player/episode/{episodeId}"
 * and "detail/{contentType}/{contentId}" (content-id-driven, mirroring
 * "player/{channelId}"'s existing convention) outside the shell -- same
 * pattern Onboarding already uses.
 */
@Composable
fun AreIptvApp() {
    val context = LocalContext.current
    val settings = remember { UserSettings(context) }
    val activeSourceId by settings.activeSourceId.collectAsState(initial = UNKNOWN)
    // Real wiring of the Settings screen's theme/reduced-motion toggles: this is the single
    // composition root wrapping the whole NavHost in AreIptvTheme, so a change from
    // SettingsScreen recomposes here immediately -- no restart, no separate "apply" step.
    val isDarkTheme by settings.isDarkTheme.collectAsState(initial = true)
    val isReducedMotion by settings.isReducedMotion.collectAsState(initial = false)
    val navController = rememberNavController()

    // Wait for the first real read from DataStore before deciding the start
    // destination, so a source that already exists on launch doesn't flash Onboarding.
    if (activeSourceId == UNKNOWN) return

    val startDestination = if (activeSourceId == null) "onboarding" else "home"

    fun openDetail(contentType: String, contentId: Long) {
        navController.navigate("detail/$contentType/$contentId")
    }

    AreIptvTheme(isDark = isDarkTheme, reducedMotion = isReducedMotion) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingFlow(onFinished = {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable("home") {
            ShellScreen(navController, activeNav = "home") {
                HomeScreen(
                    onChannelSelected = { channel -> navController.navigate("player/${channel.id}") },
                    onTitleSelected = { title -> openDetail(if (title.isSeries) "series" else "movie", title.id) },
                )
            }
        }
        composable("live") {
            ShellScreen(navController, activeNav = "live") {
                LiveScreen(onChannelSelected = { channelId -> navController.navigate("player/$channelId") })
            }
        }
        composable("guide") {
            ShellScreen(navController, activeNav = "guide") {
                GuideScreen(onChannelSelected = { channelId -> navController.navigate("player/$channelId") })
            }
        }
        composable("movies") {
            ShellScreen(navController, activeNav = "movies") {
                MoviesScreen(onMovieSelected = { movie -> openDetail("movie", movie.id) })
            }
        }
        composable("series") {
            ShellScreen(navController, activeNav = "series") {
                SeriesScreen(onSeriesSelected = { series -> openDetail("series", series.id) })
            }
        }
        composable("search") {
            ShellScreen(navController, activeNav = "search") {
                SearchScreen(
                    onChannelSelected = { channel -> navController.navigate("player/${channel.id}") },
                    onTitleSelected = { title -> openDetail(if (title.isSeries) "series" else "movie", title.id) },
                )
            }
        }
        composable(
            route = "player/{channelId}",
            arguments = listOf(navArgument("channelId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getLong("channelId") ?: return@composable
            LivePlayerScreen(source = PlaybackSource.Channel(channelId), onBack = { navController.popBackStack() })
        }
        composable(
            route = "player/vod/{vodTitleId}",
            arguments = listOf(navArgument("vodTitleId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val vodTitleId = backStackEntry.arguments?.getLong("vodTitleId") ?: return@composable
            LivePlayerScreen(source = PlaybackSource.Vod(vodTitleId), onBack = { navController.popBackStack() })
        }
        composable(
            route = "player/episode/{episodeId}",
            arguments = listOf(navArgument("episodeId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val episodeId = backStackEntry.arguments?.getLong("episodeId") ?: return@composable
            LivePlayerScreen(source = PlaybackSource.Episode(episodeId), onBack = { navController.popBackStack() })
        }
        composable(
            route = "detail/{contentType}/{contentId}",
            arguments = listOf(
                navArgument("contentType") { type = NavType.StringType },
                navArgument("contentId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val contentId = backStackEntry.arguments?.getLong("contentId") ?: return@composable
            DetailScreen(
                contentId = contentId,
                onBack = { navController.popBackStack() },
                onPlay = { target ->
                    when (target) {
                        is PlayTarget.Movie -> navController.navigate("player/vod/${target.vodTitleId}")
                        is PlayTarget.Episode -> navController.navigate("player/episode/${target.episodeId}")
                    }
                },
            )
        }
        composable("favorites") {
            ShellScreen(navController, activeNav = "favorites") {
                FavoritesScreen(
                    onChannelSelected = { channelId -> navController.navigate("player/$channelId") },
                    onTitleSelected = { title -> openDetail(if (title.isSeries) "series" else "movie", title.id) },
                )
            }
        }
        composable("settings") {
            ShellScreen(navController, activeNav = "settings") {
                SettingsScreen()
            }
        }
    }
    }
}

/** Wraps a top-level nav destination in [AreIptvAppShell], routing sidebar taps and the top-bar search icon to real navigation. */
@Composable
private fun ShellScreen(navController: NavHostController, activeNav: String, content: @Composable () -> Unit) {
    AreIptvAppShell(
        activeNav = activeNav,
        onNavSelect = { id ->
            if (id != activeNav && id in KnownRoutes) {
                navController.navigate(id) {
                    popUpTo(0) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        topBar = {
            AreTopBar(
                onSearch = {
                    if (activeNav != "search") {
                        navController.navigate("search") {
                            popUpTo(0) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        },
    ) {
        content()
    }
}

/** Routes that actually exist in the NavHost. */
private val KnownRoutes = setOf("home", "live", "guide", "movies", "series", "search", "favorites", "settings")

/** Sentinel distinguishing "DataStore hasn't emitted yet" from "no active source" (null). */
private val UNKNOWN = -1L
