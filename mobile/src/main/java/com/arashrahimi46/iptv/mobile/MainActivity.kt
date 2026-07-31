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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.AutoRelock
import com.arashrahimi46.iptv.data.settings.LockedContentDisplay
import com.arashrahimi46.iptv.data.settings.ParentalGate
import com.arashrahimi46.iptv.data.settings.PinHasher
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
import com.arashrahimi46.iptv.mobile.ui.components.AreAlertDialog
import com.arashrahimi46.iptv.mobile.ui.components.LocalParentalBlur
import com.arashrahimi46.iptv.mobile.ui.components.ParentalBlurState
import com.arashrahimi46.iptv.mobile.ui.settings.ParentalPinDialog
import com.arashrahimi46.iptv.mobile.ui.settings.ParentalPinDialogMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
            val reducedMotion by settings.isReducedMotion.collectAsStateWithLifecycle(initialValue = false)
            AreIptvMobileTheme(isDark = isDark, reducedMotion = reducedMotion) {
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

                            // Parental runtime (mirrors :tv's MainActivity): the lock + hide/blur
                            // choice + custom keywords + PIN-on-launch, plus the process-scoped
                            // session unlock ([ParentalGate]). Together these drive the tile blur
                            // ([LocalParentalBlur]), the launch PIN gate and the reveal-on-tap
                            // prompt. HIDE mode is NOT handled here -- those items are dropped
                            // upstream by each view-model's `settings.parentalFilter` combine.
                            //
                            // Every one of these starts null rather than at its default: reading a
                            // DataStore flow is asynchronous, so seeding `parentalLock = false`
                            // would paint the unlocked catalog for the frame or two before the real
                            // value lands -- exactly the leak the gate exists to prevent. Splash
                            // holds until they have all resolved.
                            val parentalLock by settings.isParentalLockEnabled.collectAsStateWithLifecycle(initialValue = null)
                            val lockedDisplay by settings.lockedContentDisplay.collectAsStateWithLifecycle(initialValue = null)
                            val parentalKeywords by settings.parentalKeywords.collectAsStateWithLifecycle(initialValue = emptySet())
                            val pinOnLaunch by settings.isPinOnLaunch.collectAsStateWithLifecycle(initialValue = null)
                            val autoRelock by settings.parentalAutoRelock.collectAsStateWithLifecycle(initialValue = AutoRelock.IMMEDIATELY)
                            // Only "is a PIN set" is observed; the hash/salt themselves are read on
                            // demand inside verifyPin so they never sit in composition state.
                            val hasPinFlow = remember { settings.parentalPinHash.map { it != null } }
                            val hasPin by hasPinFlow.collectAsStateWithLifecycle(initialValue = null)
                            val sessionUnlocked by ParentalGate.unlocked.collectAsStateWithLifecycle()
                            val verifyPin: suspend (String) -> Boolean = verify@{ pin ->
                                val hash = settings.parentalPinHash.first() ?: return@verify false
                                val salt = settings.parentalPinSalt.first() ?: return@verify false
                                PinHasher.verify(pin, salt, hash)
                            }

                            val resolvedStartScreen = startScreen
                            if (resolvedStartScreen == null || parentalLock == null ||
                                lockedDisplay == null || pinOnLaunch == null || hasPin == null
                            ) {
                                MobileSplashScreen()
                                return@Surface
                            }

                            // PIN-on-launch gate: nothing but the splash renders until the PIN is
                            // entered (once per process). :tv can leave its shell composed behind
                            // the dialog because a remote cannot dismiss it; on a phone the scrim is
                            // one tap away, so the catalog must not be on screen at all -- a scrim
                            // tap here closes the app instead of revealing it.
                            var launchUnlocked by rememberSaveable { mutableStateOf(false) }
                            if (pinOnLaunch == true && parentalLock == true && hasPin == true && !launchUnlocked) {
                                MobileSplashScreen()
                                ParentalPinDialog(
                                    mode = ParentalPinDialogMode.Verify,
                                    onDismiss = { finish() },
                                    onVerify = verifyPin,
                                    onVerified = { launchUnlocked = true; ParentalGate.unlock(autoRelock) },
                                )
                                return@Surface
                            }

                            // Reveal-on-tap: an obscured tile's click routes to onReveal, which
                            // prompts for the PIN and grants a session unlock on success.
                            var showRevealPin by remember { mutableStateOf(false) }
                            val parentalBlur = ParentalBlurState(
                                enabled = parentalLock == true && lockedDisplay == LockedContentDisplay.BLUR &&
                                    !sessionUnlocked && hasPin == true,
                                keywords = parentalKeywords,
                                onReveal = { showRevealPin = true },
                            )
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
                            BackHandler(enabled = !showExitDialog && !showRevealPin) {
                                if (!navController.popBackStack()) {
                                    if (confirmExit) showExitDialog = true else finish()
                                }
                            }

                            CompositionLocalProvider(LocalParentalBlur provides parentalBlur) {
                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    // No system-bar insets here. Everything below already handles its
                                    // own: AreScreenScaffold's TopAppBar applies the status-bar inset,
                                    // the detail screens pad their floating back button, onboarding /
                                    // language use safeDrawingPadding, and the player hides the bars
                                    // outright. Letting Scaffold add them too inset every screen
                                    // TWICE -- which is what made the top bar look about double
                                    // height, and what put a status-bar-tall black band above the
                                    // video. The bottom bar is unaffected: Scaffold still measures it
                                    // into contentPadding, and it applies navigationBarsPadding itself.
                                    // Bottom only. The top/side comment above still holds -- those
                                    // stay at zero so nothing insets twice. The bottom is the
                                    // shell's to own in BOTH states: with the bar, Scaffold reports
                                    // the bar's height (it applies navigationBarsPadding itself);
                                    // without it, this hands child screens the system nav inset
                                    // they'd otherwise have to each remember to add.
                                    contentWindowInsets = WindowInsets.navigationBars
                                        .only(WindowInsetsSides.Bottom),
                                    // Tab roots only. `!playerActive` also left the bar on child
                                    // screens -- detail, search, guide, settings sub-pages -- where
                                    // it highlighted whichever tab you happened to arrive from, and
                                    // on detail it stacked underneath that screen's own sticky Play
                                    // bar, giving two bottom bars. Splash and onboarding got one
                                    // too, which is why setup looked like a tab of the app.
                                    bottomBar = { if (currentRoute in tabRoutes) AppBottomBar(navController) },
                                ) { padding ->
                                    AppNavHost(navController, modifier = Modifier.padding(padding), startDestination = startRoute)
                                }
                            }

                            if (showRevealPin) {
                                ParentalPinDialog(
                                    mode = ParentalPinDialogMode.Verify,
                                    onDismiss = { showRevealPin = false },
                                    onVerify = verifyPin,
                                    onVerified = { showRevealPin = false; ParentalGate.unlock(autoRelock) },
                                )
                            }

                            if (showExitDialog) {
                                AreAlertDialog(
                                    onDismiss = { showExitDialog = false },
                                    title = stringResource(com.arashrahimi46.iptv.core.R.string.shell_leave_app_title),
                                    text = stringResource(com.arashrahimi46.iptv.core.R.string.shell_leave_app_body),
                                    dismissLabel = stringResource(com.arashrahimi46.iptv.core.R.string.shell_stay),
                                    confirmLabel = stringResource(com.arashrahimi46.iptv.core.R.string.shell_leave),
                                    onConfirm = { showExitDialog = false; finish() },
                                )
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
