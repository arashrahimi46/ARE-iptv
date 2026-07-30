package com.arashrahimi46.iptv.mobile

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.StartScreen
import com.arashrahimi46.iptv.data.settings.ThemeMode
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.mobile.ui.language.LanguageSelectScreen
import com.arashrahimi46.iptv.mobile.ui.nav.AppBottomBar
import com.arashrahimi46.iptv.mobile.ui.nav.AppNavHost
import com.arashrahimi46.iptv.mobile.ui.nav.isPlayerRoute
import com.arashrahimi46.iptv.mobile.ui.nav.tabRoutes
import com.arashrahimi46.iptv.mobile.ui.onboarding.OnboardingScreen
import com.arashrahimi46.iptv.mobile.ui.splash.MobileSplashScreen
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    /** Tracks whether the currently-composed screen is the full-screen player, so
     * [onUserLeaveHint] knows whether "leaving the app" should trigger real Android PiP. */
    private var playerActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository: PlaylistRepository = PlaylistRepositoryImpl(applicationContext)
        val settings = UserSettings(applicationContext)

        // Ported from :tv's MainActivity (same bug, same fix): AppCompatDelegate.setApplicationLocales()
        // only takes effect for an Activity that wraps its base context via AppCompatDelegate -- a
        // plain ComponentActivity (what this was before) never applies the selected locale to its
        // Resources at all, so LanguageSelectScreen's write was silently a no-op. The manifest's
        // AppLocalesMetadataHolderService (autoStoreLocales) restores AppCompatDelegate's own store on
        // cold start on API < 33, so that's the primary path; this reconciles both ways off the main
        // thread against the UserSettings/DataStore mirror in case that store is ever empty, or the
        // two disagree (the delegate is authoritative -- it's also what Android 13's system per-app
        // language setting writes, which never touches the mirror).
        lifecycleScope.launch {
            val stored = withContext(Dispatchers.IO) { settings.languageTag.first() }
            val applied = AppCompatDelegate.getApplicationLocales()
            if (applied.isEmpty) {
                if (stored.isNotBlank() && stored != "en") {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(stored))
                }
            } else {
                val effective = applied[0]?.toLanguageTag().orEmpty()
                if (effective.isNotBlank() && !effective.equals(stored, ignoreCase = true)) {
                    settings.setLanguageTag(effective)
                }
            }
        }

        setContent {
            // Mirrors :tv's MainActivity: DARK/LIGHT force a mode, SYSTEM resolves via
            // isSystemInDarkTheme() (a Composable API, so it can't live in UserSettings itself).
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            AreIptvMobileTheme(isDark = isDark) {
                // Every branch below (including the splash/onboarding ones, which don't mount a
                // Scaffold) needs the theme's own background painted -- without this the Activity's
                // static white window background shows through instead, which made the onboarding
                // title unreadable in dark mode (near-white text on the un-themed white window).
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var hasSource by remember { mutableStateOf<Boolean?>(null) }
                    val hasSelectedLanguage by settings.hasSelectedLanguage.collectAsStateWithLifecycle(initialValue = null)
                    LaunchedEffect(Unit) {
                        hasSource = repository.hasAnySource()
                    }
                    // Same gate order as :tv: Splash (both loads still pending) -> Language (once,
                    // per UserSettings.hasSelectedLanguage) -> Onboarding (no source yet) -> app.
                    when {
                        hasSource == null || hasSelectedLanguage == null -> MobileSplashScreen()
                        hasSelectedLanguage == false -> LanguageSelectScreen(onDone = { /* hasSelectedLanguage flips via DataStore */ })
                        hasSource == false -> OnboardingScreen(onDone = { hasSource = true })
                        else -> {
                            // Phase: General "Start screen" + "Confirm before exit" -- startScreen
                            // starts null (DataStore not read yet); gate so AppNavHost is built with
                            // the RIGHT start destination rather than flashing Home first, same as
                            // :tv's MainActivity does for its own startScreen/lastUsedTab.
                            val startScreen by settings.startScreen.collectAsStateWithLifecycle(initialValue = null)
                            val lastUsedTab by settings.lastUsedTab.collectAsStateWithLifecycle(initialValue = "home")
                            val confirmExit by settings.confirmBeforeExit.collectAsStateWithLifecycle(initialValue = true)
                            val resolvedStartScreen = startScreen
                            if (resolvedStartScreen == null) {
                                MobileSplashScreen()
                                return@Surface
                            }
                            val startRoute = resolvedStartScreen.route
                                ?: lastUsedTab.takeIf { it in tabRoutes }
                                ?: "home"

                            val navController = rememberNavController()
                            val backStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = backStackEntry?.destination?.route
                            playerActive = isPlayerRoute(currentRoute)

                            // Remember the current tab so StartScreen.LAST_USED can restore it next launch.
                            LaunchedEffect(currentRoute) {
                                if (currentRoute != null && currentRoute in tabRoutes) settings.setLastUsedTab(currentRoute)
                            }

                            var showExitDialog by remember { mutableStateOf(false) }
                            // Back at the nav graph's start destination pops out of the app -- intercept
                            // to confirm first, mirroring :tv's MainActivity. Disabled while the dialog
                            // itself is open so its own back-to-dismiss isn't double-handled.
                            BackHandler(enabled = !showExitDialog) {
                                if (!navController.popBackStack()) {
                                    if (confirmExit) showExitDialog = true else finish()
                                }
                            }

                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = { if (!playerActive) AppBottomBar(navController) },
                            ) { padding ->
                                AppNavHost(navController, modifier = Modifier.padding(padding), startDestination = startRoute)
                            }

                            if (showExitDialog) {
                                Dialog(
                                    onDismissRequest = { showExitDialog = false },
                                    properties = DialogProperties(usePlatformDefaultWidth = false),
                                ) {
                                    AreDialog(
                                        onDismiss = { showExitDialog = false },
                                        title = stringResource(com.arashrahimi46.iptv.core.R.string.shell_leave_app_title),
                                        actions = {
                                            AreButton(
                                                stringResource(com.arashrahimi46.iptv.core.R.string.shell_stay),
                                                onClick = { showExitDialog = false },
                                                variant = AreButtonVariant.Ghost,
                                            )
                                            AreButton(
                                                stringResource(com.arashrahimi46.iptv.core.R.string.shell_leave),
                                                onClick = { showExitDialog = false; finish() },
                                                variant = AreButtonVariant.Primary,
                                            )
                                        },
                                    ) {
                                        Text(
                                            text = stringResource(com.arashrahimi46.iptv.core.R.string.shell_leave_app_body),
                                            style = AreIptvTheme.typography.body,
                                            color = AreIptvTheme.colors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Real Android PiP (not :tv's UI-only corner player): entering the background while the
     * player is on screen shrinks it to a floating window instead of stopping playback. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (playerActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build(),
            )
        }
    }
}
