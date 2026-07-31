package com.arashrahimi46.iptv.mobile.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.arashrahimi46.iptv.mobile.ui.components.AreAlertDialog
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.core.R as CoreR
import kotlinx.coroutines.launch

/** Which flow [ParentalPinDialog] is running -- set a brand-new PIN, or verify an existing one. */
enum class ParentalPinDialogMode { Set, Verify }

/**
 * PIN entry on [AreAlertDialog] + a masked numeric [AreTextField] -- a phone already has a numeric
 * software keyboard, so :tv's on-screen D-pad keypad has no phone equivalent. Auto-submits at four
 * digits; the scrim tap and system Back both cancel.
 *
 * [Set] walks enter -> confirm -> [onPinConfirmed] (a mismatch restarts the flow with an inline
 * error). [Verify] calls the suspend [onVerify] and reports success via [onVerified].
 */
@Composable
fun ParentalPinDialog(
    mode: ParentalPinDialogMode,
    onDismiss: () -> Unit,
    onPinConfirmed: (pin: String) -> Unit = {},
    onVerify: suspend (pin: String) -> Boolean = { false },
    onVerified: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var firstEntry by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var verifying by remember { mutableStateOf(false) }

    val title = when (mode) {
        ParentalPinDialogMode.Set ->
            if (firstEntry == null) stringResource(CoreR.string.pin_set_title) else stringResource(CoreR.string.pin_confirm_title)
        ParentalPinDialogMode.Verify -> stringResource(CoreR.string.pin_enter_title)
    }
    val mismatchError = stringResource(CoreR.string.pin_mismatch_error)
    val incorrectError = stringResource(CoreR.string.pin_incorrect_error)
    val checkingText = stringResource(CoreR.string.pin_checking)

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

    AreAlertDialog(
        onDismiss = onDismiss,
        title = title,
        dismissLabel = stringResource(CoreR.string.action_cancel),
    ) {
        AreTextField(
            value = pin,
            onValueChange = { value ->
                error = null
                pin = value.filter(Char::isDigit).take(4)
                submit()
            },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(CoreR.string.settings_change_pin),
            enabled = !verifying,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error != null,
            supportingText = error ?: if (verifying) checkingText else null,
        )
    }
}
