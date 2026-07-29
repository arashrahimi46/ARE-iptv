package com.arashrahimi46.iptv

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import com.arashrahimi46.iptv.ui.player.LiveMiniPlayerOverlay
import com.arashrahimi46.iptv.ui.player.LivePlaybackController
import com.arashrahimi46.iptv.ui.player.LocalLivePlaybackController
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.AutoRefreshInterval
import com.arashrahimi46.iptv.data.settings.AutoRelock
import com.arashrahimi46.iptv.data.settings.LockedContentDisplay
import com.arashrahimi46.iptv.data.settings.MiniPlayerBehavior
import com.arashrahimi46.iptv.data.settings.ParentalGate
import com.arashrahimi46.iptv.data.settings.PinHasher
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.components.DefaultSidebarNavItems
import com.arashrahimi46.iptv.ui.components.LocalParentalBlur
import com.arashrahimi46.iptv.ui.components.ParentalBlurState
import com.arashrahimi46.iptv.ui.settings.ParentalPinDialog
import com.arashrahimi46.iptv.ui.settings.ParentalPinDialogMode
import com.arashrahimi46.iptv.ui.detail.DetailScreen
import com.arashrahimi46.iptv.ui.detail.PlayTarget
import com.arashrahimi46.iptv.ui.explore.ExploreScreen
import com.arashrahimi46.iptv.ui.favorites.FavoritesScreen
import com.arashrahimi46.iptv.ui.guide.GuideScreen
import com.arashrahimi46.iptv.ui.home.HomeScreen
import com.arashrahimi46.iptv.ui.language.LanguageSelectScreen
import com.arashrahimi46.iptv.ui.live.LiveScreen
import com.arashrahimi46.iptv.ui.multiview.MultiViewScreen
import com.arashrahimi46.iptv.ui.movies.MoviesScreen
import com.arashrahimi46.iptv.ui.onboarding.OnboardingFlow
import com.arashrahimi46.iptv.analytics.CrashReporting
import com.arashrahimi46.iptv.ui.onboarding.PrivacyTermsStep
import com.arashrahimi46.iptv.ui.player.LivePlayerScreen
import com.arashrahimi46.iptv.ui.player.PlaybackSource
import com.arashrahimi46.iptv.ui.recordings.RecordingsScreen
import com.arashrahimi46.iptv.ui.search.SearchScreen
import com.arashrahimi46.iptv.ui.series.SeriesScreen
import com.arashrahimi46.iptv.ui.settings.SettingsScreen
import com.arashrahimi46.iptv.ui.settings.isStale
import com.arashrahimi46.iptv.ui.streams.StreamsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import com.arashrahimi46.iptv.ui.sources.SelectSourceScreen
import com.arashrahimi46.iptv.ui.shell.AreIptvAppShell
import com.arashrahimi46.iptv.ui.splash.AreSplashScreen
import com.arashrahimi46.iptv.ui.shell.AreTopBar
import com.arashrahimi46.iptv.ui.theme.AccentPreset
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The one allocation of the "Settings needs a refresh" badge set -- see the use site in AppShell. */
private val SettingsBadge = setOf("settings")

/**
 * Auto-repeat pacing for a HELD direction key, as an interval between accepted repeats.
 *
 * The interval ramps with how long the key has been down, interpolated continuously between these
 * three anchors so the list visibly picks up speed instead of stepping between gears:
 *
 *   held 0.0s -> 125ms (~8 steps/s)   precise nudging, one item at a time
 *   held 1.0s ->  50ms (~20 steps/s)  browsing
 *   held 2.5s ->  25ms (~40 steps/s)  crossing a long catalogue
 *
 * The slow anchor is matched to how long a bring-into-view scroll takes to land, so the first
 * several steps of a hold are each visibly their own step -- that was the whole point of the
 * original flat 140ms gate, and it is preserved for exactly the case it was tuned for.
 */
private const val DPAD_STEP_SLOW_MS = 125f
private const val DPAD_STEP_MID_MS = 50f
private const val DPAD_STEP_FAST_MS = 25f

/** Hold durations (ms) at which the intervals above are reached. */
private const val DPAD_RAMP_MID_MS = 1000f
private const val DPAD_RAMP_FAST_MS = 2500f

private fun dpadLerp(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction

/** The accepted-repeat interval for a key that has been held for [heldMs]. */
private fun dpadRepeatIntervalMs(heldMs: Long): Long {
    val t = heldMs.coerceAtLeast(0L).toFloat()
    val ms = when {
        t < DPAD_RAMP_MID_MS -> dpadLerp(DPAD_STEP_SLOW_MS, DPAD_STEP_MID_MS, t / DPAD_RAMP_MID_MS)
        t < DPAD_RAMP_FAST_MS -> dpadLerp(
            DPAD_STEP_MID_MS,
            DPAD_STEP_FAST_MS,
            (t - DPAD_RAMP_MID_MS) / (DPAD_RAMP_FAST_MS - DPAD_RAMP_MID_MS),
        )
        else -> DPAD_STEP_FAST_MS
    }
    return ms.toLong()
}

/** The four D-pad directions the gate below paces. OK/Back/media keys are never gated. */
private val DpadDirections = intArrayOf(
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
)

class MainActivity : AppCompatActivity() {
    /** Uptime of the last accepted directional key; 0 until the first one. */
    private var lastDpadStepMs = 0L

    /** The direction key currently being held, or [KeyEvent.KEYCODE_UNKNOWN] when none is. */
    private var heldDpadKeyCode = KeyEvent.KEYCODE_UNKNOWN

    /**
     * Paces the AUTO-REPEAT of a HELD direction key, dropping the repeats in between. A fresh
     * discrete press is never gated.
     *
     * A TV remote's auto-repeat (~20/s, and bursts much faster) moves focus far more often than a
     * step can be *seen*. Compose moves focus once per event regardless, and on a scrolling screen
     * each move retargets the in-flight bring-into-view animation -- so a held key collapses into
     * one big scroll teleport instead of a run of visible steps. That is the reported "it jumps"
     * and "it tries to handle them all at once".
     *
     * The step is a duration, not a frame. An earlier version gated on the Choreographer frame
     * instead, which sounds equivalent and is not: once the sidebar frost was removed frames fell to
     * ~6.6ms, so the gate reopened every vsync and dropped nothing at all -- verified on the XL95,
     * where a true back-to-back burst of six presses still moved six rails. A frame gate only stops
     * input running ahead of a *janking* renderer; it cannot pace a step to the scroll animation,
     * because the animation is many frames long. This is what the pacing has to match.
     *
     * Held down, the cadence ACCELERATES -- see [dpadRepeatIntervalMs]. A steady cadence keeps the
     * first steps precise but makes a 400-channel list a crawl; ramping keeps the precision of the
     * first second and then lets the list fly. Hold time is taken from the event's own
     * `downTime` (the kernel's, not ours), so it is immune to dropped repeats and to our own
     * dispatch latency.
     *
     * A NEW key-down -- `repeatCount == 0`, or simply a different direction from the one being held
     * -- ALWAYS passes through immediately and resets the ramp. The gate used to be a flat
     * activity-global interval applied to every press, so selecting a tab with LEFT/RIGHT and then
     * pressing DOWN within 140ms silently swallowed the DOWN: the reported "I have to press it
     * twice". Only repeats of an already-held key are ever dropped now.
     *
     * Extra repeats are DROPPED, not queued: queueing preserves every one but lets focus keep
     * travelling after the user has let go, which reads as lag rather than as precision.
     *
     * ACTION_UP is never gated (focus travel is driven by ACTION_DOWN, and there are KeyUp-driven
     * handlers in TvFocusable and AreTextField) and neither is DPAD_CENTER, Back or any media key --
     * [com.arashrahimi46.iptv.ui.theme.TvFocusable] resolves short vs long press from its own
     * down->up span, so swallowing either half would break OK entirely.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode in DpadDirections) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    val now = SystemClock.uptimeMillis()
                    if (event.repeatCount == 0 || event.keyCode != heldDpadKeyCode) {
                        // Fresh press (or a change of direction): always accepted, ramp restarts.
                        heldDpadKeyCode = event.keyCode
                    } else {
                        val heldMs = event.eventTime - event.downTime
                        if (now - lastDpadStepMs < dpadRepeatIntervalMs(heldMs)) return true
                    }
                    lastDpadStepMs = now
                }
                // Releasing the key drops the cadence back to the slow anchor for the next hold.
                KeyEvent.ACTION_UP -> if (event.keyCode == heldDpadKeyCode) {
                    heldDpadKeyCode = KeyEvent.KEYCODE_UNKNOWN
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Round 1 multi-language support: AppCompatDelegate.setApplicationLocales() only takes
        // effect for an Activity that wraps its base context via AppCompatDelegate -- plain
        // ComponentActivity never applied the selected locale to its Resources at all, which is
        // why the whole feature was silently a no-op (persisted correctly, never rendered). The
        // AppLocalesMetadataHolderService (see manifest, autoStoreLocales) restores AppCompatDelegate's
        // own locale store on cold start on API < 33, so that is the primary path. As a
        // belt-and-suspenders safeguard we also resync from the UserSettings/DataStore mirror in
        // case that store is ever empty -- but OFF the main thread: the old runBlocking DataStore
        // read here blocked the main thread on a cold disk read during cold start, ANR'ing low-end
        // API < 33 devices for non-English users (Sentry ANDROID-1, BRAVIA / Android 12). The
        // re-check of isEmpty inside the coroutine keeps this a no-op once the service has restored.
        // Reconciles BOTH ways. It used to only run when the delegate was empty, which let the two
        // stores disagree permanently: Settings read the DataStore mirror and announced "فارسی"
        // over an English UI, and because the delegate was non-empty the resync never fired again.
        // The delegate is authoritative for what actually renders (it is also what Android 13's
        // system per-app language setting writes, which never touches our mirror), so when they
        // disagree the mirror follows it -- not the other way round.
        lifecycleScope.launch {
            val settings = UserSettings(applicationContext)
            val stored = withContext(Dispatchers.IO) { settings.languageTag.first() }
            val applied = AppCompatDelegate.getApplicationLocales()
            if (applied.isEmpty) {
                // Cold start before AppLocalesMetadataHolderService restored the delegate's own
                // store (API < 33): the mirror is the only record of the choice, so apply it.
                if (stored.isNotBlank() && stored != "en" && AppCompatDelegate.getApplicationLocales().isEmpty) {
                    AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.forLanguageTags(stored)
                    )
                }
            } else {
                val effective = applied[0]?.toLanguageTag().orEmpty()
                if (effective.isNotBlank() && !effective.equals(stored, ignoreCase = true)) {
                    settings.setLanguageTag(effective)
                }
            }
        }
        setContent {
            // One Activity-scoped controller owns the in-app "minimized to a corner" live player's
            // ExoPlayer while it's docked, so playback survives navigating between the fullscreen
            // player and the shell's tabs. Released when the Activity's composition tears down.
            val livePlayback = androidx.compose.runtime.remember { LivePlaybackController() }
            DisposableEffect(Unit) { onDispose { livePlayback.closeMini() } }
            CompositionLocalProvider(LocalLivePlaybackController provides livePlayback) {
                AreIptvApp()
            }
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
 * "language" (Round 1 multi-language support) shows once, right after Splash and before
 * Onboarding, gated by [UserSettings.hasSelectedLanguage]. "onboarding" shows while there's no
 * active [com.arashrahimi46.iptv.data.model.PlaylistSource]; the full-bleed overlays
 * "player/...", "detail/..." and "multiview" live OUTSIDE the shell (no
 * sidebar), same as before.
 */
@Composable
fun AreIptvApp() {
    val context = LocalContext.current
    val settings = remember { UserSettings(context) }
    val activeSourceId by settings.activeSourceId.collectAsState(initial = UNKNOWN)
    // Product decision: the startup picker (added so multiple saved sources are actually
    // listed somewhere) is only worth showing when there's something to PICK BETWEEN. With
    // exactly one saved source there's nothing to choose -- landing on a picker every cold
    // start would be worse UX than the "existing sources were invisible" bug it fixes -- so
    // that case skips straight to the shell, same as before this picker existed. `null`
    // (not yet loaded) is treated like "unknown" below, same reasoning as activeSourceId/
    // hasAcceptedTerms, so a real source count doesn't get raced by a default empty list.
    val playlistRepository = remember { PlaylistRepositoryImpl(context) }
    val sources by playlistRepository.observeSources().collectAsState(initial = null)
    // Real wiring of the Settings screen's theme/reduced-motion toggles: this is the single
    // composition root wrapping the whole NavHost in AreIptvTheme, so a change from
    // SettingsScreen recomposes here immediately -- no restart, no separate "apply" step.
    // Theme mode resolves to an effective dark/light boolean here (the only place with a Composable
    // scope for isSystemInDarkTheme); everything downstream keeps consuming that plain boolean.
    val themeMode by settings.themeMode.collectAsState(initial = com.arashrahimi46.iptv.data.settings.ThemeMode.DARK)
    val isDarkTheme = when (themeMode) {
        com.arashrahimi46.iptv.data.settings.ThemeMode.DARK -> true
        com.arashrahimi46.iptv.data.settings.ThemeMode.LIGHT -> false
        com.arashrahimi46.iptv.data.settings.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val isReducedMotion by settings.isReducedMotion.collectAsState(initial = false)
    // Per-mode accent: dark and light are chosen independently in Settings, so pick the one that
    // matches the active mode. Recomposes (and recolors the whole app) the moment either changes.
    val darkAccent by settings.darkAccent.collectAsState(initial = AccentPreset.BLUE)
    val lightAccent by settings.lightAccent.collectAsState(initial = AccentPreset.BLUE)
    val accent = if (isDarkTheme) darkAccent else lightAccent
    // Issue #11: first-run Privacy & Terms acceptance gate. `null` distinguishes "DataStore
    // hasn't emitted yet" from a real false, same reasoning as UNKNOWN below for activeSourceId.
    val hasAcceptedTerms: Boolean? by settings.hasAcceptedTerms.collectAsState(initial = null)
    // Which version they last accepted; only used to tell a first run from a terms update.
    val acceptedTermsVersion: Int? by settings.acceptedTermsVersion.collectAsState(initial = null)
    // Round 1 multi-language support: first-run language selector gate, same "null = not loaded
    // yet" reasoning as hasAcceptedTerms above.
    val hasSelectedLanguage: Boolean? by settings.hasSelectedLanguage.collectAsState(initial = null)
    val navController = rememberNavController()

    // Issue #13: cold-start splash. No androidx.core.splashscreen setup exists in the manifest yet,
    // so this is a plain Compose state gate rather than the system splash API.
    //
    // It holds for the LONGER of two things: the reveal choreography, and the first real read from
    // DataStore/Room (so a source that already exists on launch doesn't flash Onboarding). It used to
    // be a flat delay(3400ms) regardless -- but the reveal is a 1900ms one-shot tween and the data
    // settles in well under a second, so ~1.5s of every cold start was spent holding a finished
    // animation over ready data. The floor tracks the animation's own length: shorter and the reveal
    // gets cut off mid-mark, before the brand ever fades in (see AreSplashScreen's `intro`).
    var splashFloorElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(SPLASH_FLOOR_MS); splashFloorElapsed = true }
    val startStateReady = activeSourceId != UNKNOWN && hasAcceptedTerms != null &&
        hasSelectedLanguage != null && sources != null
    if (!splashFloorElapsed || !startStateReady) {
        AreIptvTheme(isDark = isDarkTheme, accent = accent, reducedMotion = isReducedMotion) {
            AreSplashScreen()
        }
        return
    }
    val hasMultipleSources = (sources?.size ?: 0) > 1
    val hasAnySource = (sources?.isNotEmpty() == true)

    // Onboarding shows only when NO playlist exists at all -- keyed off the source list, not
    // activeSourceId, because deleting the active playlist clears the active pointer while others
    // may remain: that case must land on the picker (choose a surviving playlist), not shove the
    // user back into add-a-playlist. With >1 source, or exactly one but none active (the active one
    // was just deleted), land on the picker; a single active source goes straight to the shell.
    //
    // Captured ONCE (remember) rather than recomputed on every state change: this is only the
    // NavHost's INITIAL destination -- every subsequent move is an explicit navController.navigate.
    // If it stayed reactive, a source appearing mid-onboarding-import (addXtreamSource inserts its
    // row before the catalog finishes) would flip this "onboarding"->"sources", rebuild the graph,
    // tear down OnboardingFlow, and cancel its still-running import -- leaving a half-written source
    // plus a bogus M3U fallback (two dead playlists from one add). We only reach here past the guard
    // above, so every value read is already settled.
    val startDestination = remember {
        when {
            hasSelectedLanguage == false -> "language"
            hasAcceptedTerms == false -> "privacy"
            !hasAnySource -> "onboarding"
            activeSourceId == null || hasMultipleSources -> "sources"
            else -> "shell"
        }
    }

    // Auto-refresh catalog on launch (Phase 3): once per process, if the user opted into Daily/Weekly
    // and the active catalog is older than that window, kick a background refresh. Opt-in (default Off),
    // so no existing behavior changes until it's turned on.
    LaunchedEffect(Unit) {
        if (autoRefreshChecked) return@LaunchedEffect
        autoRefreshChecked = true
        val interval = settings.autoRefreshInterval.first()
        if (interval == AutoRefreshInterval.OFF) return@LaunchedEffect
        val sid = settings.activeSourceId.first() ?: return@LaunchedEffect
        val source = playlistRepository.observeSource(sid).first() ?: return@LaunchedEffect
        val last = source.lastRefreshedAtMs
        if (last == null || System.currentTimeMillis() - last >= interval.maxAgeMs) {
            runCatching { playlistRepository.refreshSource(sid) }
        }
    }

    AreIptvTheme(isDark = isDarkTheme, accent = accent, reducedMotion = isReducedMotion) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("language") {
            LanguageSelectScreen(onDone = {
                val destination = when {
                    hasAcceptedTerms == false -> "privacy"
                    activeSourceId == null -> "onboarding"
                    hasMultipleSources -> "sources"
                    else -> "shell"
                }
                navController.navigate(destination) {
                    popUpTo("language") { inclusive = true }
                }
            })
        }
        composable("privacy") {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            PrivacyTermsStep(
                // >0 means they accepted an earlier version, so this is a re-consent, not first run.
                isUpdate = (acceptedTermsVersion ?: 0) > 0,
                onAccepted = { crashReportingEnabled ->
                scope.launch {
                    settings.setCrashReportingEnabled(crashReportingEnabled)
                    CrashReporting.setEnabled(crashReportingEnabled)
                    settings.acceptCurrentTerms()
                    val destination = when {
                        activeSourceId == null -> "onboarding"
                        hasMultipleSources -> "sources"
                        else -> "shell"
                    }
                    navController.navigate(destination) {
                        popUpTo("privacy") { inclusive = true }
                    }
                }
                },
            )
        }
        composable("onboarding") {
            OnboardingFlow(
                onFinished = {
                    navController.navigate("shell") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onExplore = { navController.navigate("explore") },
            )
        }
        // Curated free-playlist catalogue. An add here is an ordinary M3U import, so it lands in the
        // shell exactly like one the user typed -- popping onboarding too, since the goal is met.
        composable("explore") {
            ExploreScreen(
                onAdded = {
                    navController.navigate("shell") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        // Startup playlist picker: lists every added source and activates the chosen one
        // before entering the shell. "Add new" routes back into onboarding.
        composable("sources") {
            SelectSourceScreen(
                onSelected = {
                    navController.navigate("shell") {
                        popUpTo("sources") { inclusive = true }
                    }
                },
                onAddNew = { navController.navigate("onboarding") },
            )
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
                // Return to the shell with the Guide tab selected (via the shell's `tab` arg),
                // not popBackStack -- which landed on whatever launched the player (Home/Detail).
                // inclusive pops the old shell so the new instance recomposes with tab=guide.
                onOpenGuide = {
                    navController.navigate("shell?tab=guide") {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                },
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
                onOpenGuide = { navController.popBackStack() },
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
                onOpenGuide = { navController.popBackStack() },
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
        composable(
            route = "player/recording/{recordingId}",
            arguments = listOf(navArgument("recordingId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val recordingId = backStackEntry.arguments?.getLong("recordingId") ?: return@composable
            LivePlayerScreen(
                source = PlaybackSource.LocalRecording(recordingId),
                onBack = { navController.popBackStack() },
                onMultiView = { navController.navigate("multiview") },
                onOpenGuide = { navController.popBackStack() },
            )
        }
        composable(
            route = "player/direct/{streamId}",
            arguments = listOf(navArgument("streamId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val streamId = backStackEntry.arguments?.getLong("streamId") ?: return@composable
            LivePlayerScreen(
                source = PlaybackSource.DirectStream(streamId),
                onBack = { navController.popBackStack() },
                onMultiView = { navController.navigate("multiview") },
                onOpenGuide = { navController.popBackStack() },
            )
        }
        // Catch-up / archive: play an already-aired programme (identified by channel + EPG window).
        // Wired here in Phase 1; the Guide starts navigating to it in Phase 2. See docs/catchup-v1-design.md.
        composable(
            route = "player/catchup/{channelId}/{startMs}/{endMs}",
            arguments = listOf(
                navArgument("channelId") { type = NavType.LongType },
                navArgument("startMs") { type = NavType.LongType },
                navArgument("endMs") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments ?: return@composable
            LivePlayerScreen(
                source = PlaybackSource.Catchup(
                    channelId = args.getLong("channelId"),
                    programStartMs = args.getLong("startMs"),
                    programEndMs = args.getLong("endMs"),
                ),
                onBack = { navController.popBackStack() },
                onMultiView = { navController.navigate("multiview") },
                onOpenGuide = { navController.popBackStack() },
            )
        }
        composable("multiview") {
            MultiViewScreen(
                onBack = { navController.popBackStack() },
                onOpenChannel = { navController.navigate("player/$it") },
            )
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShellHost(rootNav: NavHostController, initialTab: String?) {
    val innerNav = rememberNavController()
    val backStackEntry by innerNav.currentBackStackEntryAsState()
    // Route pattern -> base id (e.g. "search?category={category}" -> "search").
    val activeNav = (backStackEntry?.destination?.route ?: "home").substringBefore("?")
    // Analytics: log the tab the user landed on -- answers "which part of the app they go to".
    LaunchedEffect(activeNav) { com.arashrahimi46.iptv.analytics.Analytics.logScreen(activeNav) }
    val context = LocalContext.current
    val activity = context as? Activity
    var showExitDialog by remember { mutableStateOf(false) }
    val livePlayback = LocalLivePlaybackController.current
    // Focus target for the docked mini-player -- so the remote's Window/PiP key can jump focus
    // straight to it from any tab (the tab grids otherwise trap D-pad focus).
    val miniFocus = remember { FocusRequester() }
    // Mini-player anti-occlusion: the user's DODGE-vs-FADE choice + reduced-motion, and the rect of
    // whatever tab content currently holds focus (fed to the overlay so it can slide out of the way).
    val settings = remember { UserSettings(context) }
    val miniBehavior by settings.miniPlayerBehavior.collectAsState(initial = MiniPlayerBehavior.DODGE)
    val reducedMotion by settings.isReducedMotion.collectAsState(initial = false)
    val sidebarStyle by settings.sidebarStyle.collectAsState(initial = com.arashrahimi46.iptv.data.settings.SidebarStyle.FLOATING)
    var focusedContentBounds by remember { mutableStateOf<Rect?>(null) }

    // Settings "!" badge: the active playlist's catalog is overdue for a refresh (>2 weeks, or never).
    val playlistRepository = remember { PlaylistRepositoryImpl(context) }
    val activeSourceId by settings.activeSourceId.collectAsState(initial = null)
    val activeSource by remember(activeSourceId) {
        activeSourceId?.let { playlistRepository.observeSource(it) } ?: flowOf(null)
    }.collectAsState(initial = null)
    // Only nudge a refresh when a playlist actually exists -- with no source there's nothing to
    // refresh, and isStale() treats a null timestamp as stale, which wrongly badged a fresh install.
    val currentSource = activeSource
    val staleWindowDays by settings.staleWindowDays.collectAsState(initial = 14L)
    // SettingsBadge is a file-level val, not an inline setOf(): a Set is unstable, so strong skipping
    // compares it by identity, and minting a new one per recomposition meant the shell and the whole
    // sidebar could never skip for as long as the badge was showing. (emptySet() is already a
    // singleton, which is why this only bit users with a stale source.)
    val badgedNavIds = if (currentSource != null && currentSource.lastRefreshedAtMs.isStale(staleWindowDays)) SettingsBadge else emptySet()

    // Phase 3 -- General: start screen (resolved once for the inner NavHost) + last-used tracking,
    // and confirm-before-exit. startScreen starts null (DataStore not read yet); gate below so the
    // NavHost is built with the RIGHT start destination rather than flashing Home first.
    val startScreen by settings.startScreen.collectAsState(initial = null)
    val lastUsedTab by settings.lastUsedTab.collectAsState(initial = "home")
    val confirmExit by settings.confirmBeforeExit.collectAsState(initial = true)

    // Phase 3 -- Parental runtime: the lock + hide/blur choice + custom keywords + PIN-on-launch,
    // plus the process-scoped session unlock ([ParentalGate]). Together these drive the tile blur
    // ([LocalParentalBlur]), the launch PIN gate, and the reveal-on-tap PIN prompt.
    val parentalLock by settings.isParentalLockEnabled.collectAsState(initial = false)
    val lockedDisplay by settings.lockedContentDisplay.collectAsState(initial = LockedContentDisplay.HIDE)
    val parentalKeywords by settings.parentalKeywords.collectAsState(initial = emptySet())
    val pinOnLaunch by settings.isPinOnLaunch.collectAsState(initial = false)
    val autoRelock by settings.parentalAutoRelock.collectAsState(initial = AutoRelock.IMMEDIATELY)
    val pinHash by settings.parentalPinHash.collectAsState(initial = null)
    val pinSalt by settings.parentalPinSalt.collectAsState(initial = null)
    val sessionUnlocked by ParentalGate.unlocked.collectAsState()
    val hasPin = pinHash != null && pinSalt != null
    val verifyPin: suspend (String) -> Boolean = verify@{ pin ->
        val h = pinHash ?: return@verify false
        val s = pinSalt ?: return@verify false
        PinHasher.verify(pin, s, h)
    }

    if (startScreen == null) return
    val startRoute = (initialTab?.takeIf { it in KnownRoutes })
        ?: startScreen?.route
        ?: lastUsedTab.takeIf { it in KnownRoutes }
        ?: "home"

    // Reveal-on-tap: a blurred adult tile's click routes here; a correct PIN unlocks the session.
    var showRevealPin by remember { mutableStateOf(false) }
    val parentalBlur = ParentalBlurState(
        enabled = parentalLock && lockedDisplay == LockedContentDisplay.BLUR && !sessionUnlocked && hasPin,
        keywords = parentalKeywords,
        onReveal = { showRevealPin = true },
    )
    // Launch gate: require the PIN before the shell is usable (once per process; a relaunch re-locks).
    var launchUnlocked by rememberSaveable { mutableStateOf(false) }
    val needsLaunchPin = pinOnLaunch && parentalLock && hasPin && !launchUnlocked

    // Remember the current tab so StartScreen.LAST_USED can restore it next launch.
    LaunchedEffect(activeNav) {
        if (activeNav in KnownRoutes) settings.setLastUsedTab(activeNav)
    }

    // Honor a tab requested by a full-bleed caller (player -> open guide) once.
    LaunchedEffect(initialTab) {
        if (initialTab != null && initialTab in KnownRoutes && initialTab != activeNav) {
            innerNav.selectTab(initialTab)
        }
    }

    fun openDetail(contentType: String, contentId: Long) {
        rootNav.navigate("detail/$contentType/$contentId")
    }

    // Home layout edit mode, hoisted here so the top-bar Customize action (rendered in the shell,
    // outside HomeScreen) can toggle it. Only surfaced on Home; survives inner-tab switches.
    var homeEditMode by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // onPreviewKeyEvent on this ancestor sees the key BEFORE the focused tab content, so
            // the remote's dedicated Window/PiP key reaches the docked mini even while a lazy grid
            // holds focus. Fires once per press (KeyUp) to avoid auto-repeat re-triggering.
            .onPreviewKeyEvent { event ->
                val isWindowKey = event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_WINDOW
                if (isWindowKey && event.type == KeyEventType.KeyUp &&
                    livePlayback?.mode == com.arashrahimi46.iptv.ui.player.LiveMiniMode.Mini
                ) {
                    runCatching { miniFocus.requestFocus() }
                    true
                } else {
                    false
                }
            },
    ) {
    // Report the focused tab content's bounds to the mini overlay so it can dodge out of the way.
    // Scoped to the shell only (NOT the whole outer Box) so focus landing on the mini itself --
    // which sits outside this wrapper -- reports null instead of the mini self-triggering a dodge.
    Box(modifier = Modifier.fillMaxSize().onFocusedBoundsChanged { focusedContentBounds = it?.boundsInRoot() }) {
    AreIptvAppShell(
        activeNav = activeNav,
        badgedNavIds = badgedNavIds,
        sidebarStyle = sidebarStyle,
        onNavSelect = { id ->
            if (id != activeNav && id in KnownRoutes) innerNav.selectTab(id)
        },
        topBar = {
            AreTopBar(
                // Page title lives in the bar (see AreTopBar.title). Reuses the sidebar's own
                // label for the route so nav item and page heading can never drift apart -- and
                // Home stays untitled, since its content starts with its own rail headers.
                title = DefaultSidebarNavItems
                    .firstOrNull { it.id == activeNav && it.id != "home" }
                    ?.let { stringResource(it.labelRes) },
                onMultiView = { rootNav.navigate("multiview") },
                onSearch = { if (activeNav != "search") innerNav.selectTab("search") },
                // Show the existing-playlists picker first (which itself has an "add new" ->
                // onboarding action), rather than jumping straight into onboarding.
                onAddPlaylist = { rootNav.navigate("sources") },
                onCustomize = if (activeNav == "home") ({ homeEditMode = !homeEditMode }) else null,
                customizeActive = homeEditMode,
            )
        },
    ) {
        CompositionLocalProvider(LocalParentalBlur provides parentalBlur) {
        // A tab switch cross-fades the outgoing + incoming screen, which composites BOTH offscreen for
        // the whole duration -- opening the blur-heavy Settings screen over Home was the worst case and
        // the source of the "opening Settings is laggy" spike. navigation-compose defaults to a 700ms
        // fade; a fast fade on the motion token cuts that window ~4x (and collapses to instant under
        // reduced motion, since durFastMs -> 0). No slide: pure alpha is the cheapest cross-fade.
        val tabFade = AreIptvTheme.motion.durFastMs
        NavHost(
            navController = innerNav,
            startDestination = startRoute,
            enterTransition = { fadeIn(tween(tabFade)) },
            exitTransition = { fadeOut(tween(tabFade)) },
        ) {
            composable("home") {
                // Home owns a real LazyColumn now (see HomeScreen), so it needs a bounded height
                // like the other lazy screens -- FullSizeTab, not ScrollableTab. Under the old
                // verticalScroll every rail stayed composed and in the display list at once.
                FullSizeTab {
                    HomeScreen(
                        editMode = homeEditMode,
                        onChannelSelected = { channel -> rootNav.navigate("player/${channel.id}") },
                        onTitleSelected = { title -> openDetail(if (title.isSeries) "series" else "movie", title.id) },
                        onCategorySelected = { category -> innerNav.navigate("search?category=${Uri.encode(category)}") },
                        onSeeAll = { route -> if (route != activeNav && route in KnownRoutes) innerNav.selectTab(route) },
                        onResumeVod = { vodTitleId -> rootNav.navigate("player/vod/$vodTitleId") },
                        onResumeEpisode = { episodeId -> rootNav.navigate("player/episode/$episodeId") },
                    )
                }
            }
            // P0.2: live/guide/movies/series now use FullSizeTab, NOT ScrollableTab --
            // BrowseLayout (Live/Movies/Series) and GuideScreen each own a real lazy
            // layout internally (LazyVerticalGrid/LazyColumn) and need a genuine bounded
            // height from their parent to be valid. ScrollableTab's verticalScroll is an
            // unbounded-height container on the same (vertical) axis, which a lazy layout
            // can't be measured inside ("infinite height" crash) -- these screens fill the
            // available size themselves and scroll their own content instead.
            composable("live") {
                FullSizeTab {
                    LiveScreen(onChannelSelected = { channelId -> rootNav.navigate("player/$channelId") })
                }
            }
            composable("guide") {
                FullSizeTab {
                    GuideScreen(
                        onChannelSelected = { channelId -> rootNav.navigate("player/$channelId") },
                        onCatchup = { channelId, startMs, endMs ->
                            rootNav.navigate("player/catchup/$channelId/$startMs/$endMs")
                        },
                    )
                }
            }
            composable("movies") {
                FullSizeTab {
                    MoviesScreen(onMovieSelected = { movie -> openDetail("movie", movie.id) })
                }
            }
            composable("series") {
                FullSizeTab {
                    SeriesScreen(onSeriesSelected = { series -> openDetail("series", series.id) })
                }
            }
            composable(
                route = "search?category={category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val category = entry.arguments?.getString("category")
                // Search owns a real LazyColumn now (see SearchResults there), so it needs a
                // bounded height -- FullSizeTab, not ScrollableTab. Under the old verticalScroll
                // every result tile stayed composed and in the display list at once.
                FullSizeTab {
                    SearchScreen(
                        onChannelSelected = { channel -> rootNav.navigate("player/${channel.id}") },
                        onTitleSelected = { title -> openDetail(if (title.isSeries) "series" else "movie", title.id) },
                        initialCategory = category,
                    )
                }
            }
            composable("favorites") {
                // Favorites owns real LazyColumns now (see TileRows there), so it needs a bounded
                // height -- FullSizeTab, not ScrollableTab. Under the old verticalScroll every
                // favorite tile stayed composed and in the display list at once.
                FullSizeTab {
                    FavoritesScreen(
                        onChannelSelected = { channelId -> rootNav.navigate("player/$channelId") },
                        onTitleSelected = { title -> openDetail(if (title.isSeries) "series" else "movie", title.id) },
                    )
                }
            }
            composable("recordings") {
                // Owns its own LazyColumn -> bounded height (FullSizeTab), like Settings.
                FullSizeTab {
                    RecordingsScreen(onPlay = { recordingId -> rootNav.navigate("player/recording/$recordingId") })
                }
            }
            composable("streams") {
                // Owns its own LazyVerticalGrid -> bounded height (FullSizeTab), like Recordings.
                FullSizeTab {
                    StreamsScreen(onStreamSelected = { streamId -> rootNav.navigate("player/direct/$streamId") })
                }
            }
            composable("settings") {
                // Settings owns its own LazyColumn now (smoother scroll than an eager Column), so it
                // needs a bounded height like the other lazy screens -- FullSizeTab, not ScrollableTab.
                FullSizeTab { SettingsScreen() }
            }
        }
        }
    }
    } // end focused-bounds wrapper

    // In-app corner mini-player, drawn over ALL tab content so a docked live channel stays visible
    // and selectable on every tab. Short OK expands it back to fullscreen (the same instance, no
    // re-buffer); long OK opens its Stop dialog. Only renders while a live channel is docked.
    livePlayback?.let { controller ->
        LiveMiniPlayerOverlay(
            controller = controller,
            onExpand = { channelId -> rootNav.navigate("player/$channelId") },
            behavior = miniBehavior,
            focusedContentBounds = focusedContentBounds,
            reducedMotion = reducedMotion,
            focusRequester = miniFocus,
        )
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
                title = stringResource(R.string.shell_leave_app_title),
                actions = {
                    AreButton(stringResource(R.string.shell_stay), onClick = { showExitDialog = false }, variant = AreButtonVariant.Ghost)
                    AreButton(
                        stringResource(R.string.shell_leave),
                        onClick = { showExitDialog = false; activity?.finish() },
                        variant = AreButtonVariant.Primary,
                        modifier = Modifier.focusRequester(leaveFocus),
                    )
                },
            ) {
                Text(
                    text = stringResource(R.string.shell_leave_app_body),
                    style = AreIptvTheme.typography.body,
                    color = AreIptvTheme.colors.textSecondary,
                )
            }
        }
    }

    // Back at the shell's start tab exits the app -- intercept to confirm first.
    // Disabled while the dialog is open (the Dialog window owns back then).
    BackHandler(enabled = !showExitDialog && !showRevealPin && !needsLaunchPin) {
        if (homeEditMode && activeNav == "home") {
            // In Home edit mode, Back means "done": every move/hide/delete is already persisted
            // as it happens, so just leave edit mode instead of falling through to the exit prompt.
            homeEditMode = false
        } else if (!innerNav.popBackStack()) {
            // Confirm-before-exit (Phase 3): on by default (keeps today's prompt); off exits at once.
            if (confirmExit) showExitDialog = true else activity?.finish()
        }
    }

    // Reveal-on-tap PIN: a blurred adult tile was clicked -- verify then unlock the session.
    if (showRevealPin) {
        ParentalPinDialog(
            mode = ParentalPinDialogMode.Verify,
            onDismiss = { showRevealPin = false },
            onVerify = verifyPin,
            onVerified = { showRevealPin = false; ParentalGate.unlock(autoRelock) },
        )
    }

    // PIN-on-launch gate: block the shell until the PIN is entered (once per process).
    if (needsLaunchPin) {
        ParentalPinDialog(
            mode = ParentalPinDialogMode.Verify,
            onDismiss = { activity?.finish() },
            onVerify = verifyPin,
            onVerified = { launchUnlocked = true; ParentalGate.unlock(autoRelock) },
        )
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

/**
 * P0.2: for tabs that own a real lazy layout internally (BrowseLayout-backed
 * Live/Movies/Series, GuideScreen) -- just hands down a bounded fillMaxSize, no
 * scroll container. A lazy layout can't be nested inside another unbounded-height
 * vertical scroll on the same axis (that's [ScrollableTab]), so these screens scroll
 * their own content instead of the tab scrolling around them.
 */
@Composable
private fun FullSizeTab(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
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

/** Routes that actually exist in the shell's inner NavHost. */
private val KnownRoutes = setOf("home", "live", "guide", "movies", "series", "search", "favorites", "recordings", "streams", "settings")

/** Sentinel distinguishing "DataStore hasn't emitted yet" from "no active source" (null). */
private val UNKNOWN = -1L

/** Guards the launch auto-refresh so it fires at most once per process, not on every recomposition. */
private var autoRefreshChecked = false

/** Minimum time [AreSplashScreen] stays up on cold start (Issue #13) -- one full pass of the
 *  choreographed glass reveal (bloom -> plate settle -> mark -> brand), matching `intro`'s 1900ms
 *  tween so the reveal is never cut off mid-play. Past this floor the splash leaves as soon as the
 *  start state has loaded, so it holds the animation and nothing else. */
private const val SPLASH_FLOOR_MS = 1900L
