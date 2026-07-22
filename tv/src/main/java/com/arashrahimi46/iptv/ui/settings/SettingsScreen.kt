package com.arashrahimi46.iptv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
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
import android.content.Intent
import android.net.Uri
import com.arashrahimi46.iptv.BuildConfig
import com.arashrahimi46.iptv.data.settings.ExternalPlayerChoice
import com.arashrahimi46.iptv.data.settings.MiniPlayerBehavior
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreSwitch
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.player.SUBTITLE_LANGUAGES
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
    val miniPlayerBehavior by viewModel.miniPlayerBehavior.collectAsState()
    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    // null = the PIN state hasn't loaded from DataStore yet. Until it resolves the PIN row and
    // lock toggle are disabled, so a fast tap during that window can't route to a no-verify
    // SetOnly / SetThenEnable flow and clobber an existing PIN.
    val hasPinSet by viewModel.hasPinSet.collectAsState()
    val pinLoaded = hasPinSet != null
    val hasPin = hasPinSet == true

    val subtitleLanguage by viewModel.subtitleLanguage.collectAsState()
    val openSubsCredential by viewModel.openSubsCredential.collectAsState()
    val subsValidation by viewModel.subsValidation.collectAsState()
    val openSubsUsername by viewModel.openSubsUsername.collectAsState()
    val subsLogin by viewModel.subsLogin.collectAsState()
    var subsKeyInput by remember { mutableStateOf("") }
    var subsUserInput by remember { mutableStateOf("") }
    var subsPassInput by remember { mutableStateOf("") }

    val omdbKey by viewModel.omdbKey.collectAsState()
    val omdbValidation by viewModel.omdbValidation.collectAsState()
    var omdbKeyInput by remember { mutableStateOf("") }

    val activeSource by viewModel.activeSource.collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()

    var pinDialog by remember { mutableStateOf<PinFlow?>(null) }

    // Note: the caller wraps this in ScrollableTab (its own verticalScroll), so this Column must
    // NOT add a second verticalScroll on the same axis -- nesting two crashes with an
    // "infinity maximum height" measure error.
    Column(modifier = modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp6).widthIn(max = 900.dp)) {
        Text(text = "Settings", style = AreIptvTheme.typography.display, color = colors.textPrimary)
        Box(Modifier.padding(top = spacing.sp8))

        SettingsSection(title = "Playlists & sync") {
            val refreshing = refreshState is RefreshState.Refreshing
            val stale = activeSource?.lastRefreshedAtMs.isStale()
            SettingsRow(
                icon = Icons.Filled.Refresh,
                title = "Refresh catalog",
                desc = buildString {
                    append(lastUpdatedLabel(activeSource?.lastRefreshedAtMs))
                    when (val r = refreshState) {
                        is RefreshState.Refreshing -> append(" · Syncing with the provider…")
                        is RefreshState.Success -> append(" · Updated: ${r.channels} channels, ${r.movies} movies, ${r.series} series")
                        is RefreshState.Error -> append(" · ${r.message}")
                        RefreshState.Idle -> if (stale) append(" · Over 2 weeks old — refresh to get new channels & titles")
                    }
                },
            ) {
                AreButton(
                    text = if (refreshing) "Refreshing…" else "Refresh now",
                    onClick = { viewModel.refresh() },
                    disabled = refreshing || activeSource == null,
                    variant = if (stale) AreButtonVariant.Primary else AreButtonVariant.Secondary,
                    size = AreButtonSize.Small,
                )
            }
        }

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
            SettingsRow(
                icon = Icons.Filled.OpenWith,
                title = "Mini-player behavior",
                desc = "When you browse near the docked mini-player: slide it to the free corner, or fade and shrink it.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniPlayerBehavior.entries.forEach { choice ->
                        AreChip(
                            text = choice.label(),
                            selected = choice == miniPlayerBehavior,
                            onClick = { viewModel.setMiniPlayerBehavior(choice) },
                            size = com.arashrahimi46.iptv.ui.components.AreChipSize.Small,
                        )
                    }
                }
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
                    disabled = !pinLoaded,
                    onCheckedChange = { turnOn ->
                        if (turnOn) {
                            if (hasPin) {
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
            SettingsRow(
                icon = Icons.Filled.VpnKey,
                title = "Change PIN",
                desc = when {
                    !pinLoaded -> "Loading…"
                    hasPin -> "A PIN is set."
                    else -> "No PIN set yet."
                },
            ) {
                AreButton(
                    text = if (hasPin) "Change" else "Set PIN",
                    onClick = { pinDialog = if (hasPin) PinFlow.VerifyThenChange else PinFlow.SetOnly },
                    disabled = !pinLoaded,
                    variant = AreButtonVariant.Secondary,
                    size = AreButtonSize.Small,
                )
            }
        }

        SettingsSection(title = "Subtitles") {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(text = "Preferred language", style = AreIptvTheme.typography.label, color = colors.textPrimary)
                Box(Modifier.padding(top = 4.dp))
                Text(
                    text = "Pre-selected when you search subtitles online. You can still change it per video.",
                    style = AreIptvTheme.typography.caption,
                    color = colors.textTertiary,
                )
                Box(Modifier.padding(top = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SUBTITLE_LANGUAGES.forEach { (code, name) ->
                        AreChip(
                            text = name,
                            selected = code == subtitleLanguage,
                            onClick = { viewModel.setSubtitleLanguage(code) },
                            size = com.arashrahimi46.iptv.ui.components.AreChipSize.Small,
                        )
                    }
                }
            }
            if (openSubsCredential != null) {
                SettingsRow(
                    icon = Icons.Filled.ClosedCaption,
                    title = "OpenSubtitles",
                    desc = if (openSubsUsername != null) {
                        "Ready -- online search and download are enabled."
                    } else {
                        "API key connected. Also sign in below -- both are required for online subtitles."
                    },
                ) {
                    AreButton(
                        text = "Disconnect",
                        onClick = {
                            viewModel.disconnectOpenSubs()
                            subsKeyInput = ""
                            subsUserInput = ""
                            subsPassInput = ""
                        },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                }
                if (openSubsUsername != null) {
                    SettingsRow(
                        icon = Icons.Filled.Person,
                        title = "Account",
                        desc = "Signed in as $openSubsUsername -- subtitle downloads are enabled.",
                    ) {
                        AreButton(
                            text = "Sign out",
                            onClick = { viewModel.signOutOpenSubs() },
                            variant = AreButtonVariant.Secondary,
                            size = AreButtonSize.Small,
                        )
                    }
                } else {
                    val signingIn = subsLogin is SubsValidation.Validating
                    val loginError = (subsLogin as? SubsValidation.Error)?.message
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                        Text(text = "Sign in to download subtitles", style = AreIptvTheme.typography.label, color = colors.textPrimary)
                        Box(Modifier.padding(top = 4.dp))
                        Text(
                            text = "Last step -- both the API key and your account sign-in are required for online subtitles. Sign in with your opensubtitles.com username and password; downloads use your account's daily quota. We stay signed in for you.",
                            style = AreIptvTheme.typography.caption,
                            color = colors.textTertiary,
                        )
                        Box(Modifier.padding(top = 12.dp))
                        AreTextField(
                            value = subsUserInput,
                            onValueChange = { subsUserInput = it },
                            placeholder = "Username",
                            icon = Icons.Filled.Person,
                        )
                        Box(Modifier.padding(top = 10.dp))
                        AreTextField(
                            value = subsPassInput,
                            onValueChange = { subsPassInput = it },
                            placeholder = "Password",
                            masked = true,
                            icon = Icons.Filled.Lock,
                            error = loginError,
                        )
                        Box(Modifier.padding(top = 12.dp))
                        AreButton(
                            text = if (signingIn) "Signing in…" else "Sign in",
                            onClick = { viewModel.signInOpenSubs(subsUserInput, subsPassInput) },
                            disabled = signingIn || subsUserInput.isBlank() || subsPassInput.isBlank(),
                            variant = AreButtonVariant.Primary,
                            size = AreButtonSize.Small,
                        )
                    }
                }
            } else {
                val validating = subsValidation is SubsValidation.Validating
                val errorMsg = (subsValidation as? SubsValidation.Error)?.message
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Text(text = "OpenSubtitles API key", style = AreIptvTheme.typography.label, color = colors.textPrimary)
                    Box(Modifier.padding(top = 4.dp))
                    Text(
                        text = "Online subtitles need a free opensubtitles.com account with two things: this API key (for search) and your account sign-in (for download). Step 1 -- paste the API key; you'll sign in next. We validate it before saving.",
                        style = AreIptvTheme.typography.caption,
                        color = colors.textTertiary,
                    )
                    Box(Modifier.padding(top = 12.dp))
                    AreTextField(
                        value = subsKeyInput,
                        onValueChange = { subsKeyInput = it },
                        placeholder = "API key",
                        mono = true,
                        icon = Icons.Filled.VpnKey,
                        error = errorMsg,
                    )
                    Box(Modifier.padding(top = 12.dp))
                    AreButton(
                        text = if (validating) "Checking…" else "Connect",
                        onClick = { viewModel.connectOpenSubs(subsKeyInput) },
                        disabled = validating || subsKeyInput.isBlank(),
                        variant = AreButtonVariant.Primary,
                        size = AreButtonSize.Small,
                    )
                }
            }
        }

        SettingsSection(title = "Movie & series info") {
            if (omdbKey != null) {
                SettingsRow(
                    icon = Icons.Filled.Star,
                    title = "OMDb",
                    desc = "Connected -- IMDb & Rotten Tomatoes ranks, plot and cast show on Detail.",
                ) {
                    AreButton(
                        text = "Disconnect",
                        onClick = {
                            viewModel.disconnectOmdb()
                            omdbKeyInput = ""
                        },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                }
            } else {
                val validatingOmdb = omdbValidation is OmdbValidation.Validating
                val omdbError = (omdbValidation as? OmdbValidation.Error)?.message
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Text(text = "OMDb API key", style = AreIptvTheme.typography.label, color = colors.textPrimary)
                    Box(Modifier.padding(top = 4.dp))
                    Text(
                        text = "Paste a free key from omdbapi.com to enrich movies and series with IMDb & Rotten Tomatoes ranks, a synopsis and the cast. We validate it before saving.",
                        style = AreIptvTheme.typography.caption,
                        color = colors.textTertiary,
                    )
                    Box(Modifier.padding(top = 12.dp))
                    AreTextField(
                        value = omdbKeyInput,
                        onValueChange = { omdbKeyInput = it },
                        placeholder = "API key",
                        mono = true,
                        icon = Icons.Filled.VpnKey,
                        error = omdbError,
                    )
                    Box(Modifier.padding(top = 12.dp))
                    AreButton(
                        text = if (validatingOmdb) "Checking…" else "Connect",
                        onClick = { viewModel.connectOmdb(omdbKeyInput) },
                        disabled = validatingOmdb || omdbKeyInput.isBlank(),
                        variant = AreButtonVariant.Primary,
                        size = AreButtonSize.Small,
                    )
                }
            }
        }

        // Issue #11: About section -- app version + a donation link. Everything else under
        // Settings.jsx's original "About & support" (store rating, licenses, etc.) is still out
        // of scope, per this file's class doc.
        SettingsSection(title = "About") {
            SettingsRow(icon = Icons.Filled.Info, title = "Version", desc = "ARE iptv ${BuildConfig.VERSION_NAME}") {}
            SettingsRow(
                icon = Icons.Filled.Favorite,
                title = "Support this app",
                desc = "Buy me a coffee if ARE iptv is useful to you.",
            ) {
                AreButton(
                    text = "Donate",
                    onClick = {
                        // PLACEHOLDER URL -- needs a real donation link from product.
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/change-me"))
                        context.startActivity(intent)
                    },
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

/** A catalog is "stale" (nudge a refresh) once its last sync is older than this -- also the window
 * the sidebar Settings badge uses. Null (never refreshed / legacy row) counts as stale. */
const val REFRESH_STALE_MS = 14L * 24 * 60 * 60 * 1000

/** True when [this] last-refresh timestamp is missing or older than [REFRESH_STALE_MS]. */
fun Long?.isStale(): Boolean = this == null || System.currentTimeMillis() - this > REFRESH_STALE_MS

private fun lastUpdatedLabel(ts: Long?): String {
    if (ts == null) return "Never refreshed"
    val ago = System.currentTimeMillis() - ts
    val days = ago / (24 * 60 * 60 * 1000)
    val hours = ago / (60 * 60 * 1000)
    val mins = ago / (60 * 1000)
    return when {
        days >= 1 -> "Last updated $days day${if (days == 1L) "" else "s"} ago"
        hours >= 1 -> "Last updated $hours hour${if (hours == 1L) "" else "s"} ago"
        mins >= 1 -> "Last updated $mins min ago"
        else -> "Last updated just now"
    }
}

/** Local dialog step -- not persisted, purely UI navigation for the PIN flow. */
private enum class PinFlow { SetThenEnable, SetOnly, VerifyThenDisable, VerifyThenChange }

private fun ExternalPlayerChoice.label(): String = when (this) {
    ExternalPlayerChoice.BUILT_IN -> "Built-in"
    ExternalPlayerChoice.VLC -> "VLC"
    ExternalPlayerChoice.MX -> "MX"
}

private fun MiniPlayerBehavior.label(): String = when (this) {
    MiniPlayerBehavior.DODGE -> "Auto-dodge"
    MiniPlayerBehavior.FADE -> "Fade & shrink"
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
