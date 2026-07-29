package com.arashrahimi46.iptv.mobile

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.ThemeMode
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.mobile.ui.nav.AppBottomBar
import com.arashrahimi46.iptv.mobile.ui.nav.AppNavHost
import com.arashrahimi46.iptv.mobile.ui.nav.isPlayerRoute
import com.arashrahimi46.iptv.mobile.ui.onboarding.OnboardingScreen
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme

class MainActivity : ComponentActivity() {
    /** Tracks whether the currently-composed screen is the full-screen player, so
     * [onUserLeaveHint] knows whether "leaving the app" should trigger real Android PiP. */
    private var playerActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository: PlaylistRepository = PlaylistRepositoryImpl(applicationContext)
        val settings = UserSettings(applicationContext)

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
                    LaunchedEffect(Unit) {
                        hasSource = repository.hasAnySource()
                    }
                    when (hasSource) {
                        null -> Unit // splash-equivalent blank frame while the DB is checked
                        false -> OnboardingScreen(onDone = { hasSource = true })
                        true -> {
                            val navController = rememberNavController()
                            val backStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = backStackEntry?.destination?.route
                            playerActive = isPlayerRoute(currentRoute)

                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = { if (!playerActive) AppBottomBar(navController) },
                            ) { padding ->
                                AppNavHost(navController, modifier = Modifier.padding(padding))
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
