package com.arashrahimi46.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.home.HomeScreen
import com.arashrahimi46.iptv.ui.onboarding.OnboardingFlow
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
 * "home" once one exists. Later overlay flows (Detail/LivePlayer/MultiView,
 * Phase 2/3) build on this same NavHost.
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
            var activeNav by remember { mutableStateOf("home") }
            AreIptvAppShell(activeNav = activeNav, onNavSelect = { activeNav = it }) {
                HomeScreen()
            }
        }
    }
}

/** Sentinel distinguishing "DataStore hasn't emitted yet" from "no active source" (null). */
private val UNKNOWN = -1L
