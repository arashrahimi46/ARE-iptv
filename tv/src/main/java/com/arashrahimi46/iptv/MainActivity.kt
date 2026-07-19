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
import com.arashrahimi46.iptv.ui.guide.GuideScreen
import com.arashrahimi46.iptv.ui.home.HomeScreen
import com.arashrahimi46.iptv.ui.live.LiveScreen
import com.arashrahimi46.iptv.ui.onboarding.OnboardingFlow
import com.arashrahimi46.iptv.ui.player.LivePlayerScreen
import com.arashrahimi46.iptv.ui.shell.AreIptvAppShell
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AreIptvTheme {
                AreIptvApp()
            }
        }
    }
}

/**
 * Top-level app graph (real back stack, not a boolean flag -- see report):
 * "onboarding" while there's no active [com.arashrahimi46.iptv.data.model.PlaylistSource],
 * "home"/"live"/"guide" (each wrapped in [AreIptvAppShell]) once one exists,
 * and "player/{channelId}" (content-id-driven, the same convention Detail
 * will use in Phase 3) as a full-bleed overlay outside the shell -- same
 * pattern Onboarding already uses.
 */
@Composable
fun AreIptvApp() {
    val context = LocalContext.current
    val settings = remember { UserSettings(context) }
    val activeSourceId by settings.activeSourceId.collectAsState(initial = UNKNOWN)
    val navController = rememberNavController()

    // Wait for the first real read from DataStore before deciding the start
    // destination, so a source that already exists on launch doesn't flash Onboarding.
    if (activeSourceId == UNKNOWN) return

    val startDestination = if (activeSourceId == null) "onboarding" else "home"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingFlow(onFinished = {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable("home") {
            ShellScreen(navController, activeNav = "home") { HomeScreen() }
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
        composable(
            route = "player/{channelId}",
            arguments = listOf(navArgument("channelId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getLong("channelId") ?: return@composable
            LivePlayerScreen(channelId = channelId, onBack = { navController.popBackStack() })
        }
    }
}

/** Wraps a top-level nav destination in [AreIptvAppShell], routing sidebar taps for the built routes to real navigation. */
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
    ) {
        content()
    }
}

/** Routes that actually exist in the NavHost -- other sidebar items (Movies/Series/Search/Favorites/Settings) are inert until later phases. */
private val KnownRoutes = setOf("home", "live", "guide")

/** Sentinel distinguishing "DataStore hasn't emitted yet" from "no active source" (null). */
private val UNKNOWN = -1L
