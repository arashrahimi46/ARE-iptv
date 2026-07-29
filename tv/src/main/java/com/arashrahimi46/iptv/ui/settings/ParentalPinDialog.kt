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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.core.R
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
    // Attempt throttling (Verify mode only) -- the more meaningful protection for a 4-digit
    // PIN than hash strength alone (product-lead/qa-reviewer follow-up on the Phase 4 PIN
    // review): a short lockout after repeated wrong entries stops trivial keypad-mashing,
    // independent of how strong the underlying hash is.
    var wrongAttempts by remember { mutableStateOf(0) }
    var lockedUntilMs by remember { mutableStateOf(0L) }
    var lockRemainingSec by remember { mutableStateOf(0) }

    val setTitle = stringResource(R.string.pin_set_title)
    val confirmTitle = stringResource(R.string.pin_confirm_title)
    val enterTitle = stringResource(R.string.pin_enter_title)
    val mismatchError = stringResource(R.string.pin_mismatch_error)
    val incorrectError = stringResource(R.string.pin_incorrect_error)

    val title = when (mode) {
        ParentalPinDialogMode.Set -> if (firstEntry == null) setTitle else confirmTitle
        ParentalPinDialogMode.Verify -> enterTitle
    }

    // Ticks the lockout countdown while active; keypad input is ignored during this window
    // (see AreNumericKeypad's onDigit below).
    LaunchedEffect(lockedUntilMs) {
        while (System.currentTimeMillis() < lockedUntilMs) {
            lockRemainingSec = ((lockedUntilMs - System.currentTimeMillis()) / 1000L).toInt() + 1
            kotlinx.coroutines.delay(250)
        }
        lockRemainingSec = 0
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
                    error = mismatchError
                    firstEntry = null
                    pin = ""
                }
            }
            ParentalPinDialogMode.Verify -> {
                verifying = true
                val ok = onVerify(pin)
                verifying = false
                if (ok) {
                    wrongAttempts = 0
                    onVerified()
                } else {
                    wrongAttempts++
                    pin = ""
                    // 3 wrong: 3s lockout. 4th: 6s. 5th+: 10s. Short enough not to be
                    // punishing on a genuine mistake, long enough to kill mash-guessing.
                    val lockoutSec = when {
                        wrongAttempts >= 5 -> 10
                        wrongAttempts == 4 -> 6
                        wrongAttempts >= 3 -> 3
                        else -> 0
                    }
                    if (lockoutSec > 0) {
                        lockedUntilMs = System.currentTimeMillis() + lockoutSec * 1000L
                        error = null
                    } else {
                        error = incorrectError
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(onDismiss = onDismiss, title = title, width = 360.dp) {
            Column {
                PinDots(length = pin.length)
                Box(Modifier.padding(top = 14.dp))
                val currentError = error
                if (lockRemainingSec > 0) {
                    Text(
                        text = stringResource(R.string.pin_lockout_message, lockRemainingSec),
                        style = AreIptvTheme.typography.caption,
                        color = AreIptvTheme.colors.danger,
                    )
                    Box(Modifier.padding(top = 10.dp))
                } else if (currentError != null) {
                    Text(text = currentError, style = AreIptvTheme.typography.caption, color = AreIptvTheme.colors.danger)
                    Box(Modifier.padding(top = 10.dp))
                } else if (verifying) {
                    Text(text = stringResource(R.string.pin_checking), style = AreIptvTheme.typography.caption, color = AreIptvTheme.colors.textTertiary)
                    Box(Modifier.padding(top = 10.dp))
                }
                AreNumericKeypad(
                    onDigit = { digit -> if (lockRemainingSec == 0 && pin.length < 4) { error = null; pin += digit } },
                    onBackspace = { if (lockRemainingSec == 0) pin = pin.dropLast(1) },
                    onClear = { if (lockRemainingSec == 0) pin = "" },
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
