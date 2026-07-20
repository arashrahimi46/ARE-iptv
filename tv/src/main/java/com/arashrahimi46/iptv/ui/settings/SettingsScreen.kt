package com.arashrahimi46.iptv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.settings.ExternalPlayerChoice
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreSwitch
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Real Settings screen (Settings.jsx). Every control here persists through
 * [SettingsViewModel] -> [com.arashrahimi46.iptv.data.settings.UserSettings]'
 * DataStore. Theme and reduced-motion take visible effect immediately because
 * [com.arashrahimi46.iptv.MainActivity] reads those same DataStore flows at
 * the composition root and feeds them straight into
 * [com.arashrahimi46.iptv.ui.theme.AreIptvTheme] -- there is no separate
 * "restart to apply" step. Hardware decoding and autoplay-next-episode are
 * real preferences read by [com.arashrahimi46.iptv.ui.player.LivePlayerScreen]
 * and (persisted only, no auto-advance wiring yet -- see report)
 * [com.arashrahimi46.iptv.ui.detail.DetailScreen] respectively.
 * Picture-in-picture is storage-only by explicit product-lead scoping (see
 * the toggle's own comment below) -- no enter-PiP-mode code was added.
 *
 * Parental lock gates the *toggle itself* rather than individual titles: the
 * schema has no per-title "mature" flag yet (a real gate-a-title feature
 * needs a schema addition), so once a PIN is set, turning the lock back OFF
 * requires re-entering it -- a real, if simple, security-meaningful gate for v1.
 *
 * The "Playlists & sync" / "About & support" sections from Settings.jsx are
 * out of scope for this phase (no multi-playlist management, no backup/export,
 * no store rating/licenses infra) and are intentionally not built here. Same
 * reason [com.arashrahimi46.iptv.ui.shell.AreTopBar]'s "Add playlist" glyph is
 * a static icon, not a real button -- see that file's comment.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(context.applicationContext as android.app.Application),
    )
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing

    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isReducedMotion by viewModel.isReducedMotion.collectAsState()
    val isHardwareDecoding by viewModel.isHardwareDecoding.collectAsState()
    val isAutoplayNextEpisode by viewModel.isAutoplayNextEpisode.collectAsState()
    val isPictureInPicture by viewModel.isPictureInPicture.collectAsState()
    val isBrowseListMode by viewModel.isBrowseListMode.collectAsState()
    val externalPlayer by viewModel.externalPlayer.collectAsState()
    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    val hasPinSet by viewModel.hasPinSet.collectAsState()

    var pinDialog by remember { mutableStateOf<PinFlow?>(null) }

    Column(modifier = modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp6).widthIn(max = 900.dp)) {
        Text(text = "Settings", style = AreIptvTheme.typography.display, color = colors.textPrimary)
        Box(Modifier.padding(top = spacing.sp8))

        SettingsSection(title = "Appearance") {
            SettingsRow(
                icon = Icons.Filled.Brightness4,
                title = "Dark theme",
                desc = "Recommended for lean-back viewing.",
            ) {
                AreSwitch(checked = isDarkTheme, onCheckedChange = viewModel::setDarkTheme)
            }
            SettingsRow(icon = Icons.Filled.Bolt, title = "Reduce motion", desc = "Softer focus animations.") {
                AreSwitch(checked = isReducedMotion, onCheckedChange = viewModel::setReducedMotion)
            }
            SettingsRow(
                icon = Icons.Filled.ViewAgenda,
                title = "List view",
                desc = "Show channels, movies and series as a list instead of a tile grid.",
            ) {
                AreSwitch(checked = isBrowseListMode, onCheckedChange = viewModel::setBrowseListMode)
            }
        }

        SettingsSection(title = "Playback") {
            SettingsRow(
                icon = Icons.Filled.HighQuality,
                title = "Hardware decoding",
                desc = "Prefer hardware decoders; falls back to software if a stream needs it.",
            ) {
                AreSwitch(checked = isHardwareDecoding, onCheckedChange = viewModel::setHardwareDecoding)
            }
            SettingsRow(
                icon = Icons.Filled.SkipNext,
                title = "Autoplay next episode",
                desc = "Preference only for now -- auto-advance isn't wired up yet; pick the next episode from Detail.",
            ) {
                AreSwitch(checked = isAutoplayNextEpisode, onCheckedChange = viewModel::setAutoplayNextEpisode)
            }
            SettingsRow(
                icon = Icons.Filled.PictureInPictureAlt,
                title = "Picture-in-picture",
                desc = "Saved for later -- PiP mode itself isn't implemented yet (pending device verification).",
            ) {
                // Storage-only per explicit product-lead scoping -- see UserSettings.isPictureInPicture.
                AreSwitch(checked = isPictureInPicture, onCheckedChange = viewModel::setPictureInPicture)
            }
            SettingsRow(icon = Icons.Filled.OpenInNew, title = "External player", desc = "Choice is saved; playback still uses the built-in player.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExternalPlayerChoice.entries.forEach { choice ->
                        AreChip(
                            text = choice.label(),
                            selected = choice == externalPlayer,
                            onClick = { viewModel.setExternalPlayer(choice) },
                            size = com.arashrahimi46.iptv.ui.components.AreChipSize.Small,
                        )
                    }
                }
            }
        }

        SettingsSection(title = "Parental controls") {
            SettingsRow(
                icon = Icons.Filled.Lock,
                title = "Lock adult categories",
                desc = "Once on, turning it off requires your PIN.",
            ) {
                AreSwitch(
                    checked = isParentalLockEnabled,
                    onCheckedChange = { turnOn ->
                        if (turnOn) {
                            if (hasPinSet) {
                                viewModel.setParentalLockEnabled(true)
                            } else {
                                pinDialog = PinFlow.SetThenEnable
                            }
                        } else {
                            pinDialog = PinFlow.VerifyThenDisable
                        }
                    },
                )
            }
            SettingsRow(icon = Icons.Filled.VpnKey, title = "Change PIN", desc = if (hasPinSet) "A PIN is set." else "No PIN set yet.") {
                AreButton(
                    text = if (hasPinSet) "Change" else "Set PIN",
                    onClick = { pinDialog = if (hasPinSet) PinFlow.VerifyThenChange else PinFlow.SetOnly },
                    variant = AreButtonVariant.Secondary,
                    size = AreButtonSize.Small,
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

/** Local dialog step -- not persisted, purely UI navigation for the PIN flow. */
private enum class PinFlow { SetThenEnable, SetOnly, VerifyThenDisable, VerifyThenChange }

private fun ExternalPlayerChoice.label(): String = when (this) {
    ExternalPlayerChoice.BUILT_IN -> "Built-in"
    ExternalPlayerChoice.VLC -> "VLC"
    ExternalPlayerChoice.MX -> "MX"
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val colors = AreIptvTheme.colors
    Column(modifier = Modifier.padding(bottom = 34.dp)) {
        Text(
            text = title.uppercase(),
            style = AreIptvTheme.typography.caption,
            color = colors.textTertiary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(
            modifier = Modifier
                .background(colors.surface1, RoundedCornerShape(AreIptvTheme.radius.lg)),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsRow(icon: ImageVector, title: String, desc: String? = null, control: @Composable () -> Unit) {
    val colors = AreIptvTheme.colors
    // QA MEDIUM defect (PIN row wrapping one-char-per-line): this Row had no fillMaxWidth,
    // so it sized to wrap-content and the label Column's weight(1f) had no real remaining
    // space to expand into -- every row was affected, the AreButton control (PIN) just made
    // it most visible. fillMaxWidth is the real fix, not a per-row special case.
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(colors.surface2, RoundedCornerShape(AreIptvTheme.radius.sm)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AreIptvTheme.typography.label, color = colors.textPrimary)
            if (desc != null) {
                Text(text = desc, style = AreIptvTheme.typography.caption, color = colors.textTertiary)
            }
        }
        control()
    }
}
