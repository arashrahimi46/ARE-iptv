package com.arashrahimi46.iptv

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.detail.DetailScreen
import com.arashrahimi46.iptv.ui.detail.PlayTarget
import com.arashrahimi46.iptv.ui.favorites.FavoritesScreen
import com.arashrahimi46.iptv.ui.guide.GuideScreen
import com.arashrahimi46.iptv.ui.home.HomeScreen
import com.arashrahimi46.iptv.ui.live.LiveScreen
import com.arashrahimi46.iptv.ui.multiview.MultiViewScreen
import com.arashrahimi46.iptv.ui.movies.MoviesScreen
import com.arashrahimi46.iptv.ui.onboarding.OnboardingFlow
import com.arashrahimi46.iptv.ui.onboarding.PrivacyTermsStep
import com.arashrahimi46.iptv.ui.player.LivePlayerScreen
import com.arashrahimi46.iptv.ui.player.PlaybackSource
import com.arashrahimi46.iptv.ui.search.SearchScreen
import com.arashrahimi46.iptv.ui.series.SeriesScreen
import com.arashrahimi46.iptv.ui.settings.SettingsScreen
import com.arashrahimi46.iptv.ui.shell.AreIptvAppShell
import com.arashrahimi46.iptv.ui.splash.AreSplashScreen
import com.arashrahimi46.iptv.ui.shell.AreTopBar
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AreIptvApp()
        }
    }
}

/**
 * Top-level app graph. The persistent chrome (sidebar + top bar) lives in a
 * single long-lived "shell" destination whose CONTENT is driven by a nested
 * NavHost -- so switching tabs (home/live/guide/...) recomposes only the
 * content pane, not the whole screen. This is what makes navigation feel like
 * "the content changed" instead of "the whole page reloaded": the sidebar and
 * top bar are never torn down and rebuilt on a tab switch.
 *
 * "onboarding" shows while there's no active
 * [com.arashrahimi46.iptv.data.model.PlaylistSource]; the full-bleed overlays
 * "player/...", "detail/..." and "multiview" live OUTSIDE the shell (no
 * sidebar), same as before.
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
    // Issue #11: first-run Privacy & Terms acceptance gate. `null` distinguishes "DataStore
    // hasn't emitted yet" from a real false, same reasoning as UNKNOWN below for activeSourceId.
    val hasAcceptedTerms: Boolean? by settings.hasAcceptedTerms.collectAsState(initial = null)
    val navController = rememberNavController()

    // Issue #13: cold-start splash, shown unconditionally for a couple of seconds before any
    // real decision is made below. No androidx.core.splashscreen setup exists in the manifest
    // yet, so this is a plain Compose state gate rather than the system splash API.
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(SPLASH_DURATION_MS); showSplash = false }
    if (showSplash) {
        AreIptvTheme(isDark = isDarkTheme, reducedMotion = isReducedMotion) {
            AreSplashScreen()
        }
        return
    }

    // Wait for the first real read from DataStore before deciding the start
    // destination, so a source that already exists on launch doesn't flash Onboarding.
    if (activeSourceId == UNKNOWN || hasAcceptedTerms == null) return

    val startDestination = when {
        hasAcceptedTerms == false -> "privacy"
        activeSourceId == null -> "onboarding"
        else -> "shell"
    }

    AreIptvTheme(isDark = isDarkTheme, reducedMotion = isReducedMotion) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("privacy") {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            PrivacyTermsStep(onAccepted = {
                scope.launch {
                    settings.setTermsAccepted(true)
                    navController.navigate(if (activeSourceId == null) "onboarding" else "shell") {
                        popUpTo("privacy") { inclusive = true }
                    }
                }
            })
        }
        composable("onboarding") {
            OnboardingFlow(onFinished = {
                navController.navigate("shell") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        // Persistent shell. Optional `tab` arg lets full-bleed screens (e.g. the
        // player's "open guide") return to the shell AND select a specific tab.
        composable(
            route = "shell?tab={tab}",
            arguments = listOf(navArgument("tab") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { backStackEntry ->
            ShellHost(rootNav = navController, initialTab = backStackEntry.arguments?.getString("tab"))
        }
        composable(
            route = "player/{channelId}",
            arguments = listOf(navArgument("channelId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getLong("channelId") ?: return@composable
            LivePlayerScreen(
                source = PlaybackSource.Channel(channelId),
                onBack = { navController.popBackStack() },
                onMultiView = { navController.navigate("multiview") },
                onOpenGuide = { navController.openShellTab("guide") },
            )
        }
        composable(
            route = "player/vod/{vodTitleId}",
            arguments = listOf(navArgument("vodTitleId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val vodTitleId = backStackEntry.arguments?.getLong("vodTitleId") ?: return@composable
            LivePlayerScreen(
                source = PlaybackSource.Vod(vodTitleId),
                onBack = { navController.popBackStack() },
                onMultiView = { navController.navigate("multiview") },
                onOpenGuide = { navController.openShellTab("guide") },
            )
        }
        composable(
            route = "player/episode/{episodeId}",
            arguments = listOf(navArgument("episodeId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val episodeId = backStackEntry.arguments?.getLong("episodeId") ?: return@composable
            LivePlayerScreen(
                source = PlaybackSource.Episode(episodeId),
                onBack = { navController.popBackStack() },
                onMultiView = { navController.navigate("multiview") },
                onOpenGuide = { navController.openShellTab("guide") },
            )
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
        composable("multiview") {
            MultiViewScreen(onBack = { navController.popBackStack() })
        }
    }
    }
}

/**
 * The persistent app shell: one [AreIptvAppShell] (sidebar + top bar) hosting a
 * nested NavHost for the tab content. Sidebar taps and the top-bar icons drive
 * the INNER controller (content-only swap); player/detail open on the OUTER
 * [rootNav] as full-bleed overlays.
 */
@Composable
private fun ShellHost(rootNav: NavHostController, initialTab: String?) {
    val innerNav = rememberNavController()
    val backStackEntry by innerNav.currentBackStackEntryAsState()
    // Route pattern -> base id (e.g. "search?category={category}" -> "search").
    val activeNav = (backStackEntry?.destination?.route ?: "home").substringBefore("?")
    val activity = LocalContext.current as? Activity
    var showExitDialog by remember { mutableStateOf(false) }

    // Honor a tab requested by a full-bleed caller (player -> open guide) once.
    LaunchedEffect(initialTab) {
        if (initialTab != null && initialTab in KnownRoutes && initialTab != activeNav) {
            innerNav.selectTab(initialTab)
        }
    }

    fun openDetail(contentType: String, contentId: Long) {
        rootNav.navigate("detail/$contentType/$contentId")
    }

    Box(modifier = Modifier.fillMaxSize()) {
    AreIptvAppShell(
        activeNav = activeNav,
        onNavSelect = { id ->
            if (id != activeNav && id in KnownRoutes) innerNav.selectTab(id)
        },
        topBar = {
            AreTopBar(
                onMultiView = { rootNav.navigate("multiview") },
                onSearch = { if (activeNav != "search") innerNav.selectTab("search") },
                onAddPlaylist = { rootNav.navigate("onboarding") },
                // QA MEDIUM defect: onAvatar was declared on AreTopBar but never attached to
                // anything. Settings is the closest existing real destination for an
                // account/profile icon (no dedicated profile screen exists).
                onAvatar = { if (activeNav != "settings") innerNav.selectTab("settings") },
            )
        },
    ) {
        NavHost(navController = innerNav, startDestination = "home") {
            composable("home") {
                ScrollableTab {
                    HomeScreen(
                        onChannelSelected = { channel -> rootNav.navigate("player/${channel.id}") },
                        onTitleSelected = { title -> openDetail(if (title.isSeries) "series" else "movie", title.id) },
                        onCategorySelected = { category -> innerNav.navigate("search?category=${Uri.encode(category)}") },
                    )
                }
            }
            composable("live") {
                ScrollableTab {
                    LiveScreen(onChannelSelected = { channelId -> rootNav.navigate("player/$channelId") })
                }
            }
            composable("guide") {
                ScrollableTab {
                    GuideScreen(onChannelSelected = { channelId -> rootNav.navigate("player/$channelId") })
                }
            }
            composable("movies") {
                ScrollableTab {
                    MoviesScreen(onMovieSelected = { movie -> openDetail("movie", movie.id) })
                }
            }
            composable("series") {
                ScrollableTab {
                    SeriesScreen(onSeriesSelected = { series -> openDetail("series", series.id) })
                }
            }
            composable(
                route = "search?category={category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val category = entry.arguments?.getString("category")
                ScrollableTab {
                    SearchScreen(
                        onChannelSelected = { channel -> rootNav.navigate("player/${channel.id}") },
                        onTitleSelected = { title -> openDetail(if (title.isSeries) "series" else "movie", title.id) },
                        initialCategory = category,
                    )
                }
            }
            composable("favorites") {
                ScrollableTab {
                    FavoritesScreen(
                        onChannelSelected = { channelId -> rootNav.navigate("player/$channelId") },
                        onTitleSelected = { title -> openDetail(if (title.isSeries) "series" else "movie", title.id) },
                    )
                }
            }
            composable("settings") {
                ScrollableTab { SettingsScreen() }
            }
        }
    }

    }

    // Rendered in a real Dialog WINDOW (not inline) so it traps D-pad focus -- an
    // inline overlay let focus leak to the sidebar behind it. The window also
    // handles back-to-dismiss (dismissOnBackPress), returning to the app.
    if (showExitDialog) {
        val leaveFocus = remember { FocusRequester() }
        Dialog(
            onDismissRequest = { showExitDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        ) {
            LaunchedEffect(Unit) { leaveFocus.requestFocus() }
            AreDialog(
                onDismiss = { showExitDialog = false },
                title = "Leave ARE iptv?",
                actions = {
                    AreButton("Stay", onClick = { showExitDialog = false }, variant = AreButtonVariant.Ghost)
                    AreButton(
                        "Leave",
                        onClick = { showExitDialog = false; activity?.finish() },
                        variant = AreButtonVariant.Primary,
                        modifier = Modifier.focusRequester(leaveFocus),
                    )
                },
            ) {
                Text(
                    text = "Do you want to exit ARE iptv?",
                    style = AreIptvTheme.typography.body,
                    color = AreIptvTheme.colors.textSecondary,
                )
            }
        }
    }

    // Back at the shell's start tab exits the app -- intercept to confirm first.
    // Disabled while the dialog is open (the Dialog window owns back then).
    BackHandler(enabled = !showExitDialog) {
        if (!innerNav.popBackStack()) showExitDialog = true
    }
}

/**
 * Per-tab vertical scroll. The shell no longer owns a single scroll (that
 * prevented hosting a nested NavHost), so each tab scrolls independently --
 * which also means each tab remembers its own scroll position.
 */
@Composable
private fun ScrollableTab(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        content()
    }
}

/** Switch the inner shell tab, preserving each tab's state (single-top, save/restore). */
private fun NavHostController.selectTab(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.findStartDestination().id) { saveState = true }
    }
}

/** From a full-bleed overlay, return to the shell and select [tab]. */
private fun NavHostController.openShellTab(tab: String) {
    navigate("shell?tab=$tab") {
        launchSingleTop = true
        popUpTo("shell") { inclusive = true }
    }
}

/** Routes that actually exist in the shell's inner NavHost. */
private val KnownRoutes = setOf("home", "live", "guide", "movies", "series", "search", "favorites", "settings")

/** Sentinel distinguishing "DataStore hasn't emitted yet" from "no active source" (null). */
private val UNKNOWN = -1L

/** How long [AreSplashScreen] stays up on cold start (Issue #13) -- product-lead placeholder duration. */
private const val SPLASH_DURATION_MS = 1800L
