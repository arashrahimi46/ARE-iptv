package com.arashrahimi46.iptv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreNumericKeypad
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/** Which flow [ParentalPinDialog] is running -- set a brand-new PIN, or verify an existing one. */
enum class ParentalPinDialogMode { Set, Verify }

/**
 * PIN entry overlay (Settings.jsx's parental-lock section has no real flow --
 * this is the concrete set/confirm/verify implementation), built on
 * [AreDialog] + [AreNumericKeypad] rather than a full sub-screen, since a
 * 4-digit PIN pad fits comfortably in a modal and doesn't need its own
 * back-stack destination.
 *
 * [Set] mode walks enter -> confirm -> [onPinConfirmed] (mismatches restart
 * the flow with an inline error, never silently accept a mismatched PIN).
 * [Verify] mode calls the suspend [onVerify] (a real hash comparison against
 * [SettingsViewModel.verifyPin], not a stub) and reports success via [onVerified].
 *
 * Wrapped in a real [Dialog] window (not just a composed overlay) because
 * [com.arashrahimi46.iptv.ui.settings.SettingsScreen] lives inside
 * [com.arashrahimi46.iptv.ui.shell.AreIptvAppShell]'s `verticalScroll` content
 * column -- [AreDialog]'s own `fillMaxSize()` scrim can't be measured
 * correctly nested inside a scrollable parent (same unbounded-height
 * constraint issue [com.arashrahimi46.iptv.ui.browse.BrowseLayout]'s doc
 * comment already flags for lazy grids). A platform [Dialog] renders into its
 * own window, sidestepping the parent's scroll constraints entirely.
 */
@Composable
fun ParentalPinDialog(
    mode: ParentalPinDialogMode,
    onDismiss: () -> Unit,
    onPinConfirmed: (pin: String) -> Unit = {},
    onVerify: suspend (pin: String) -> Boolean = { false },
    onVerified: () -> Unit = {},
) {
    var pin by remember { mutableStateOf("") }
    var firstEntry by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var verifying by remember { mutableStateOf(false) }

    val title = when (mode) {
        ParentalPinDialogMode.Set -> if (firstEntry == null) "Set a 4-digit PIN" else "Confirm your PIN"
        ParentalPinDialogMode.Verify -> "Enter PIN"
    }

    LaunchedEffect(pin, mode) {
        if (pin.length != 4) return@LaunchedEffect
        when (mode) {
            ParentalPinDialogMode.Set -> {
                val entry = firstEntry
                if (entry == null) {
                    firstEntry = pin
                    pin = ""
                } else if (pin == entry) {
                    onPinConfirmed(pin)
                } else {
                    error = "PINs didn't match -- try again"
                    firstEntry = null
                    pin = ""
                }
            }
            ParentalPinDialogMode.Verify -> {
                verifying = true
                val ok = onVerify(pin)
                verifying = false
                if (ok) {
                    onVerified()
                } else {
                    error = "Incorrect PIN"
                    pin = ""
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(onDismiss = onDismiss, title = title, width = 360.dp) {
            Column {
                PinDots(length = pin.length)
                Box(Modifier.padding(top = 14.dp))
                if (error != null) {
                    Text(text = error!!, style = AreIptvTheme.typography.caption, color = AreIptvTheme.colors.danger)
                    Box(Modifier.padding(top = 10.dp))
                } else if (verifying) {
                    Text(text = "Checking…", style = AreIptvTheme.typography.caption, color = AreIptvTheme.colors.textTertiary)
                    Box(Modifier.padding(top = 10.dp))
                }
                AreNumericKeypad(
                    onDigit = { digit -> if (pin.length < 4) { error = null; pin += digit } },
                    onBackspace = { pin = pin.dropLast(1) },
                    onClear = { pin = "" },
                )
            }
        }
    }
}

@Composable
private fun PinDots(length: Int) {
    val colors = AreIptvTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(if (index < length) colors.accent else colors.surface3, CircleShape),
            )
        }
    }
}
