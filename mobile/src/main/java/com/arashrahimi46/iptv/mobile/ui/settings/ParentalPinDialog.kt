package com.arashrahimi46.iptv.mobile.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import kotlinx.coroutines.launch

/** Which flow [ParentalPinDialog] is running -- set a brand-new PIN, or verify an existing one. */
enum class ParentalPinDialogMode { Set, Verify }

/**
 * Touch-first PIN entry: an [AreDialog] with a masked, numeric [OutlinedTextField] (no :core text
 * field exists yet -- known gap, see :tv's equivalent [com.arashrahimi46.iptv.ui.settings.ParentalPinDialog]
 * for the D-pad keypad this intentionally does NOT copy) instead of :tv's on-screen
 * [com.arashrahimi46.iptv.ui.components.AreNumericKeypad] -- a phone already has a numeric software
 * keyboard, so a custom D-pad-oriented keypad isn't needed here.
 * [Set] mode walks enter -> confirm -> [onPinConfirmed] (a mismatch restarts the flow with an
 * inline error). [Verify] mode calls the suspend [onVerify] and reports success via [onVerified].
 */
@Composable
fun ParentalPinDialog(
    mode: ParentalPinDialogMode,
    onDismiss: () -> Unit,
    onPinConfirmed: (pin: String) -> Unit = {},
    onVerify: suspend (pin: String) -> Boolean = { false },
    onVerified: () -> Unit = {},
) {
    val colors = AreIptvMobileTheme.colors
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var firstEntry by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var verifying by remember { mutableStateOf(false) }

    val title = when (mode) {
        ParentalPinDialogMode.Set -> if (firstEntry == null) stringResource(R.string.pin_set_title) else stringResource(R.string.pin_confirm_title)
        ParentalPinDialogMode.Verify -> stringResource(R.string.pin_enter_title)
    }
    val mismatchError = stringResource(R.string.pin_mismatch_error)
    val incorrectError = stringResource(R.string.pin_incorrect_error)

    fun submit() {
        if (pin.length != 4) return
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
                scope.launch {
                    val ok = onVerify(pin)
                    verifying = false
                    if (ok) {
                        onVerified()
                    } else {
                        error = incorrectError
                        pin = ""
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(
            onDismiss = onDismiss,
            title = title,
            width = 360.dp,
            actions = {
                AreButton(text = stringResource(android.R.string.cancel), onClick = onDismiss, variant = AreButtonVariant.Secondary)
            },
        ) {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        error = null
                        pin = value.filter(Char::isDigit).take(4)
                        submit()
                    },
                    label = { Text(stringResource(R.string.settings_change_pin)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error != null,
                )
                val currentError = error
                if (currentError != null) {
                    Text(text = currentError, style = AreIptvMobileTheme.typography.caption, color = colors.danger)
                } else if (verifying) {
                    Text(text = stringResource(R.string.pin_checking), style = AreIptvMobileTheme.typography.caption, color = colors.textTertiary)
                }
            }
        }
    }
}
