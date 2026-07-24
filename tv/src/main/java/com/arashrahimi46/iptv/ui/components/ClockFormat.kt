package com.arashrahimi46.iptv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.arashrahimi46.iptv.data.settings.UserSettings
import java.time.format.DateTimeFormatter

/**
 * Time-of-day formatter honoring the user's Settings clock choice: 24-hour (HH:mm) or 12-hour
 * (h:mm a). Backs the EPG/HUD clocks; recomposes live when the pref flips (no restart). Defaults
 * to 24-hour to match the app's previous hardcoded format.
 */
@Composable
fun rememberClockFormatter(): DateTimeFormatter {
    val context = LocalContext.current
    val settings = remember { UserSettings(context) }
    val is24h by settings.is24HourClock.collectAsState(initial = true)
    return remember(is24h) { DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a") }
}
