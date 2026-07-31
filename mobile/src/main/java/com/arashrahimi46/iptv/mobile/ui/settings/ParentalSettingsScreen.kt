package com.arashrahimi46.iptv.mobile.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.data.settings.AutoRelock
import com.arashrahimi46.iptv.data.settings.LockedContentDisplay
import com.arashrahimi46.iptv.mobile.ui.components.AreChoiceRow
import com.arashrahimi46.iptv.mobile.ui.components.AreListRow
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSectionHeader
import com.arashrahimi46.iptv.mobile.ui.components.AreSwitchRow

/** Which PIN flow [ParentalPinDialog] is currently running. */
internal enum class PinFlow { SetThenEnable, SetOnly, VerifyThenDisable, VerifyThenChange }

/** `settings/parental` -- lock, PIN, auto-relock, locked-content display, PIN-on-launch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    val hasPinSet by viewModel.hasPinSet.collectAsState()
    val autoRelock by viewModel.parentalAutoRelock.collectAsState()
    val lockedDisplay by viewModel.lockedContentDisplay.collectAsState()
    val pinOnLaunch by viewModel.isPinOnLaunch.collectAsState()

    val pinLoaded = hasPinSet != null
    val hasPin = hasPinSet == true
    var pinDialog by rememberSaveable { mutableStateOf<PinFlow?>(null) }

    AreScreenScaffold(
        title = stringResource(CoreR.string.settings_section_parental),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item(contentType = "header") { AreSectionHeader(stringResource(CoreR.string.settings_section_parental)) }
            item(contentType = "row") {
                AreSwitchRow(
                    title = stringResource(CoreR.string.settings_lock_adult),
                    supporting = stringResource(CoreR.string.settings_lock_adult_desc),
                    checked = isParentalLockEnabled,
                    enabled = pinLoaded,
                    onCheckedChange = { turnOn ->
                        if (turnOn) {
                            if (hasPin) viewModel.setParentalLockEnabled(true) else pinDialog = PinFlow.SetThenEnable
                        } else {
                            pinDialog = PinFlow.VerifyThenDisable
                        }
                    },
                )
            }
            item(contentType = "row") {
                AreListRow(
                    title = if (hasPin) stringResource(CoreR.string.settings_change_pin) else stringResource(CoreR.string.settings_set_pin),
                    supporting = when {
                        !pinLoaded -> stringResource(CoreR.string.settings_pin_loading)
                        hasPin -> stringResource(CoreR.string.settings_pin_is_set)
                        else -> stringResource(CoreR.string.settings_pin_not_set)
                    },
                    enabled = pinLoaded,
                    onClick = { pinDialog = if (hasPin) PinFlow.VerifyThenChange else PinFlow.SetOnly },
                )
            }
            item(contentType = "header") { AreSectionHeader(stringResource(CoreR.string.settings_section_content_locking)) }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_relock_title),
                    options = AutoRelock.entries,
                    selected = autoRelock,
                    label = { stringResource(it.labelRes()) },
                    onSelect = viewModel::setParentalAutoRelock,
                )
            }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_locked_display_title),
                    options = LockedContentDisplay.entries,
                    selected = lockedDisplay,
                    label = { stringResource(it.labelRes()) },
                    onSelect = viewModel::setLockedContentDisplay,
                )
            }
            item(contentType = "row") {
                AreSwitchRow(
                    title = stringResource(CoreR.string.settings_pin_launch_title),
                    supporting = stringResource(CoreR.string.settings_pin_launch_desc),
                    checked = pinOnLaunch,
                    enabled = hasPin,
                    onCheckedChange = viewModel::setPinOnLaunch,
                )
            }
        }
    }

    when (val flow = pinDialog) {
        PinFlow.SetThenEnable, PinFlow.SetOnly -> ParentalPinDialog(
            mode = ParentalPinDialogMode.Set,
            onDismiss = { pinDialog = null },
            onPinConfirmed = { pin ->
                viewModel.setPin(pin)
                if (flow == PinFlow.SetThenEnable) viewModel.setParentalLockEnabled(true)
                pinDialog = null
            },
        )
        PinFlow.VerifyThenDisable -> ParentalPinDialog(
            mode = ParentalPinDialogMode.Verify,
            onDismiss = { pinDialog = null },
            onVerify = viewModel::verifyPin,
            onVerified = {
                viewModel.setParentalLockEnabled(false)
                pinDialog = null
            },
        )
        PinFlow.VerifyThenChange -> ParentalPinDialog(
            mode = ParentalPinDialogMode.Verify,
            onDismiss = { pinDialog = null },
            onVerify = viewModel::verifyPin,
            onVerified = { pinDialog = PinFlow.SetOnly },
        )
        null -> Unit
    }
}
