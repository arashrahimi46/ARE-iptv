package com.arashrahimi46.iptv.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import android.content.Intent
import android.net.Uri
import com.arashrahimi46.iptv.BuildConfig
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.settings.MiniPlayerBehavior
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreLanguageOptions
import com.arashrahimi46.iptv.ui.components.AreLanguageSelector
import com.arashrahimi46.iptv.ui.components.AreSwitch
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.language.localizedConfirmCopy
import com.arashrahimi46.iptv.ui.player.SUBTITLE_LANGUAGES
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import kotlinx.coroutines.launch

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
 *
 * Round 1 multi-language support: the Language row lets the user re-open the same
 * [AreLanguageSelector] used on first run. Picking a DIFFERENT language than the current one
 * shows a confirmation modal phrased in the TARGET language (via [localizedConfirmCopy]) before
 * applying -- confirm applies the locale (`AppCompatDelegate.setApplicationLocales`, an Activity
 * recreate is expected) and persists it; cancel leaves the picker showing the still-current
 * language (it was never changed until confirm).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(context.applicationContext as android.app.Application),
    )
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing
    val scope = rememberCoroutineScope()

    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isReducedMotion by viewModel.isReducedMotion.collectAsState()
    val isHardwareDecoding by viewModel.isHardwareDecoding.collectAsState()
    val isAutoplayNextEpisode by viewModel.isAutoplayNextEpisode.collectAsState()
    val isBrowseListMode by viewModel.isBrowseListMode.collectAsState()
    val miniPlayerBehavior by viewModel.miniPlayerBehavior.collectAsState()
    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    // null = the PIN state hasn't loaded from DataStore yet. Until it resolves the PIN row and
    // lock toggle are disabled, so a fast tap during that window can't route to a no-verify
    // SetOnly / SetThenEnable flow and clobber an existing PIN.
    val hasPinSet by viewModel.hasPinSet.collectAsState()
    val pinLoaded = hasPinSet != null
    val hasPin = hasPinSet == true

    val subtitleLanguage by viewModel.subtitleLanguage.collectAsState()
    val languageTag by viewModel.languageTag.collectAsState()
    // Different-language pick pending confirmation (see class doc); null = no dialog open.
    var pendingLanguage by remember { mutableStateOf<String?>(null) }
    // Change-button modals for the app-language and subtitle-language pickers.
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showSubtitlePicker by remember { mutableStateOf(false) }
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

    // Settings owns a LazyColumn so only the on-screen sections compose/draw -- far smoother scroll
    // than the old single eager Column that laid out every section (incl. the long subtitle-chip row
    // and the API-key text fields) up front. The caller must therefore hand this a bounded height
    // (FullSizeTab, NOT ScrollableTab -- a LazyColumn can't nest inside another verticalScroll).
    val refreshingText = stringResource(R.string.settings_refreshing)
    val neverRefreshedText = stringResource(R.string.settings_never_refreshed)
    val syncingSuffix = stringResource(R.string.settings_refresh_syncing)
    val staleSuffix = stringResource(R.string.settings_refresh_stale)
    // Arrow-right from the sidebar must land on the FIRST control (Refresh now), not whichever row
    // happens to sit at the sidebar icon's height. focusRestorer's fallback only fires when THIS group
    // is entered without a target, but the shell's directional search resolves straight to the nearest
    // focusable (Dark theme) and never consults it. focusProperties{enter} intercepts every entry into
    // this focus group and redirects it -- so it wins over the directional pick.
    val topFocus = remember { FocusRequester() }
    // vertical inset via contentPadding (NOT modifier .padding): with modifier padding the list's clip
    // edge lands exactly on the first item's top and shears the big display title's glyph tops. As
    // contentPadding the clip edge sits above the first item, so the title's ascenders have room.
    LazyColumn(
        modifier = modifier.fillMaxSize().focusProperties { enter = { topFocus } }.focusGroup().padding(horizontal = spacing.safeX).widthIn(max = 900.dp),
        contentPadding = PaddingValues(vertical = spacing.sp6),
    ) {
        item {
            Text(text = stringResource(R.string.settings_title), style = AreIptvTheme.typography.display, color = colors.textPrimary)
            Box(Modifier.padding(top = spacing.sp8))
        }

        item {
        SettingsSection(title = stringResource(R.string.settings_section_playlists)) {
            val refreshing = refreshState is RefreshState.Refreshing
            val stale = activeSource?.lastRefreshedAtMs.isStale()
            val lastUpdatedText = lastUpdatedLabel(activeSource?.lastRefreshedAtMs, neverRefreshedText)
            SettingsRow(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.settings_refresh_catalog),
                desc = buildString {
                    append(lastUpdatedText)
                    when (val r = refreshState) {
                        is RefreshState.Refreshing -> append(" · $syncingSuffix")
                        is RefreshState.Success -> append(" · " + stringResource(R.string.settings_refresh_success, r.channels, r.movies, r.series))
                        is RefreshState.Error -> append(" · ${r.message}")
                        RefreshState.Idle -> if (stale) append(" · $staleSuffix")
                    }
                },
            ) {
                AreButton(
                    modifier = Modifier.focusRequester(topFocus),
                    text = if (refreshing) refreshingText else stringResource(R.string.settings_refresh_now),
                    onClick = { viewModel.refresh() },
                    disabled = refreshing || activeSource == null,
                    variant = if (stale) AreButtonVariant.Primary else AreButtonVariant.Secondary,
                    size = AreButtonSize.Small,
                )
            }
        }
        }

        item {
        SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
            SettingsRow(
                icon = Icons.Filled.Brightness4,
                title = stringResource(R.string.settings_dark_theme),
                desc = stringResource(R.string.settings_dark_theme_desc),
            ) {
                AreSwitch(checked = isDarkTheme, onCheckedChange = viewModel::setDarkTheme)
            }
            SettingsRow(icon = Icons.Filled.Bolt, title = stringResource(R.string.settings_reduce_motion), desc = stringResource(R.string.settings_reduce_motion_desc)) {
                AreSwitch(checked = isReducedMotion, onCheckedChange = viewModel::setReducedMotion)
            }
            SettingsRow(
                icon = Icons.Filled.ViewAgenda,
                title = stringResource(R.string.settings_list_view),
                desc = stringResource(R.string.settings_list_view_desc),
            ) {
                AreSwitch(checked = isBrowseListMode, onCheckedChange = viewModel::setBrowseListMode)
            }
        }
        }

        item {
        SettingsSection(title = stringResource(R.string.settings_section_language)) {
            SettingsRow(
                icon = Icons.Filled.Language,
                title = stringResource(R.string.settings_language_row_title),
                desc = stringResource(R.string.settings_language_row_desc),
            ) {
                SelectionChangeControl(
                    current = AreLanguageOptions.firstOrNull { it.tag.equals(languageTag, ignoreCase = true) }
                        ?.let { stringResource(it.nativeNameRes) } ?: languageTag,
                    onChange = { showLanguagePicker = true },
                )
            }
        }
        }

        item {
        SettingsSection(title = stringResource(R.string.settings_section_playback)) {
            SettingsRow(
                icon = Icons.Filled.HighQuality,
                title = stringResource(R.string.settings_hardware_decoding),
                desc = stringResource(R.string.settings_hardware_decoding_desc),
            ) {
                AreSwitch(checked = isHardwareDecoding, onCheckedChange = viewModel::setHardwareDecoding)
            }
            SettingsRow(
                icon = Icons.Filled.SkipNext,
                title = stringResource(R.string.settings_autoplay_next),
                desc = stringResource(R.string.settings_autoplay_next_desc),
            ) {
                AreSwitch(checked = isAutoplayNextEpisode, onCheckedChange = viewModel::setAutoplayNextEpisode)
            }
            SettingsRow(
                icon = Icons.Filled.OpenWith,
                title = stringResource(R.string.settings_mini_player_behavior),
                desc = stringResource(R.string.settings_mini_player_behavior_desc),
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
        }
        }

        item {
        SettingsSection(title = stringResource(R.string.settings_section_parental)) {
            SettingsRow(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.settings_lock_adult),
                desc = stringResource(R.string.settings_lock_adult_desc),
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
                title = stringResource(R.string.settings_change_pin),
                desc = when {
                    !pinLoaded -> stringResource(R.string.settings_pin_loading)
                    hasPin -> stringResource(R.string.settings_pin_is_set)
                    else -> stringResource(R.string.settings_pin_not_set)
                },
            ) {
                AreButton(
                    text = if (hasPin) stringResource(R.string.settings_change) else stringResource(R.string.settings_set_pin),
                    onClick = { pinDialog = if (hasPin) PinFlow.VerifyThenChange else PinFlow.SetOnly },
                    disabled = !pinLoaded,
                    variant = AreButtonVariant.Secondary,
                    size = AreButtonSize.Small,
                )
            }
        }
        }

        item {
        SettingsSection(title = stringResource(R.string.settings_section_subtitles)) {
            SettingsRow(
                icon = Icons.Filled.ClosedCaption,
                title = stringResource(R.string.settings_subtitle_lang_title),
                desc = stringResource(R.string.settings_subtitle_lang_desc),
            ) {
                SelectionChangeControl(
                    current = SUBTITLE_LANGUAGES.firstOrNull { it.first == subtitleLanguage }?.second ?: subtitleLanguage,
                    onChange = { showSubtitlePicker = true },
                )
            }
            if (openSubsCredential != null) {
                SettingsRow(
                    icon = Icons.Filled.ClosedCaption,
                    title = stringResource(R.string.settings_opensubs_title),
                    desc = if (openSubsUsername != null) {
                        stringResource(R.string.settings_opensubs_ready)
                    } else {
                        stringResource(R.string.settings_opensubs_key_connected)
                    },
                ) {
                    AreButton(
                        text = stringResource(R.string.action_disconnect),
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
                        title = stringResource(R.string.settings_account),
                        desc = stringResource(R.string.settings_account_signed_in, openSubsUsername ?: ""),
                    ) {
                        AreButton(
                            text = stringResource(R.string.action_sign_out),
                            onClick = { viewModel.signOutOpenSubs() },
                            variant = AreButtonVariant.Secondary,
                            size = AreButtonSize.Small,
                        )
                    }
                } else {
                    val signingIn = subsLogin is SubsValidation.Validating
                    val loginError = (subsLogin as? SubsValidation.Error)?.message
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                        Text(text = stringResource(R.string.settings_signin_title), style = AreIptvTheme.typography.label, color = colors.textPrimary)
                        Box(Modifier.padding(top = 4.dp))
                        Text(
                            text = stringResource(R.string.settings_signin_desc),
                            style = AreIptvTheme.typography.caption,
                            color = colors.textTertiary,
                        )
                        Box(Modifier.padding(top = 12.dp))
                        AreTextField(
                            value = subsUserInput,
                            onValueChange = { subsUserInput = it },
                            placeholder = stringResource(R.string.settings_placeholder_username),
                            icon = Icons.Filled.Person,
                        )
                        Box(Modifier.padding(top = 10.dp))
                        AreTextField(
                            value = subsPassInput,
                            onValueChange = { subsPassInput = it },
                            placeholder = stringResource(R.string.settings_placeholder_password),
                            masked = true,
                            icon = Icons.Filled.Lock,
                            error = loginError,
                        )
                        Box(Modifier.padding(top = 12.dp))
                        AreButton(
                            text = if (signingIn) stringResource(R.string.settings_signing_in) else stringResource(R.string.action_sign_in),
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
                    Text(text = stringResource(R.string.settings_opensubs_key_title), style = AreIptvTheme.typography.label, color = colors.textPrimary)
                    Box(Modifier.padding(top = 4.dp))
                    Text(
                        text = stringResource(R.string.settings_opensubs_key_desc),
                        style = AreIptvTheme.typography.caption,
                        color = colors.textTertiary,
                    )
                    Box(Modifier.padding(top = 12.dp))
                    AreTextField(
                        value = subsKeyInput,
                        onValueChange = { subsKeyInput = it },
                        placeholder = stringResource(R.string.settings_placeholder_api_key),
                        mono = true,
                        icon = Icons.Filled.VpnKey,
                        error = errorMsg,
                    )
                    Box(Modifier.padding(top = 12.dp))
                    AreButton(
                        text = if (validating) stringResource(R.string.settings_checking) else stringResource(R.string.action_connect),
                        onClick = { viewModel.connectOpenSubs(subsKeyInput) },
                        disabled = validating || subsKeyInput.isBlank(),
                        variant = AreButtonVariant.Primary,
                        size = AreButtonSize.Small,
                    )
                }
            }
        }
        }

        item {
        SettingsSection(title = stringResource(R.string.settings_section_metadata)) {
            if (omdbKey != null) {
                SettingsRow(
                    icon = Icons.Filled.Star,
                    title = stringResource(R.string.settings_omdb_title),
                    desc = stringResource(R.string.settings_omdb_connected),
                ) {
                    AreButton(
                        text = stringResource(R.string.action_disconnect),
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
                    Text(text = stringResource(R.string.settings_omdb_key_title), style = AreIptvTheme.typography.label, color = colors.textPrimary)
                    Box(Modifier.padding(top = 4.dp))
                    Text(
                        text = stringResource(R.string.settings_omdb_key_desc),
                        style = AreIptvTheme.typography.caption,
                        color = colors.textTertiary,
                    )
                    Box(Modifier.padding(top = 12.dp))
                    AreTextField(
                        value = omdbKeyInput,
                        onValueChange = { omdbKeyInput = it },
                        placeholder = stringResource(R.string.settings_placeholder_api_key),
                        mono = true,
                        icon = Icons.Filled.VpnKey,
                        error = omdbError,
                    )
                    Box(Modifier.padding(top = 12.dp))
                    AreButton(
                        text = if (validatingOmdb) stringResource(R.string.settings_checking) else stringResource(R.string.action_connect),
                        onClick = { viewModel.connectOmdb(omdbKeyInput) },
                        disabled = validatingOmdb || omdbKeyInput.isBlank(),
                        variant = AreButtonVariant.Primary,
                        size = AreButtonSize.Small,
                    )
                }
            }
        }
        }

        // Issue #11: About section -- app version + a donation link. Everything else under
        // Settings.jsx's original "About & support" (store rating, licenses, etc.) is still out
        // of scope, per this file's class doc.
        item {
        SettingsSection(title = stringResource(R.string.settings_section_about)) {
            SettingsRow(icon = Icons.Filled.Info, title = stringResource(R.string.settings_version_title), desc = stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME)) {}
            SettingsRow(
                icon = Icons.Filled.Favorite,
                title = stringResource(R.string.settings_support_title),
                desc = stringResource(R.string.settings_support_desc),
            ) {
                AreButton(
                    text = stringResource(R.string.action_donate),
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

    // App-language picker (opened by the "Change" button). Picking a different language closes this
    // and hands off to the confirm-in-target-language dialog below (pendingLanguage), unchanged.
    if (showLanguagePicker) {
        Dialog(onDismissRequest = { showLanguagePicker = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            AreDialog(onDismiss = { showLanguagePicker = false }, title = stringResource(R.string.settings_language_row_title)) {
                AreLanguageSelector(
                    selectedTag = languageTag,
                    onSelect = { picked ->
                        showLanguagePicker = false
                        if (!picked.equals(languageTag, ignoreCase = true)) pendingLanguage = picked
                    },
                )
            }
        }
    }

    // Subtitle-language picker (opened by the "Change" button) -- persists immediately on pick.
    if (showSubtitlePicker) {
        Dialog(onDismissRequest = { showSubtitlePicker = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            AreDialog(onDismiss = { showSubtitlePicker = false }, title = stringResource(R.string.settings_subtitle_lang_title)) {
                // Vertical gap must clear the focused chip's glow halo (~10dp past its edge) plus the
                // 1.06x focus scale, or the focus glow bleeds onto the chip in the row below.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SUBTITLE_LANGUAGES.forEach { (code, name) ->
                        AreChip(
                            text = name,
                            selected = code == subtitleLanguage,
                            onClick = {
                                viewModel.setSubtitleLanguage(code)
                                showSubtitlePicker = false
                            },
                            size = com.arashrahimi46.iptv.ui.components.AreChipSize.Small,
                        )
                    }
                }
            }
        }
    }

    // Language-switch confirmation, phrased in the TARGET language (see class doc). Cancel just
    // closes the dialog -- the picker above was never changed, so it already shows the current
    // language again with no extra revert step needed.
    val targetLanguage = pendingLanguage
    if (targetLanguage != null) {
        val copy = remember(targetLanguage) { localizedConfirmCopy(context, targetLanguage) }
        Dialog(onDismissRequest = { pendingLanguage = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            AreDialog(
                onDismiss = { pendingLanguage = null },
                title = copy.title,
                actions = {
                    AreButton(copy.cancel, onClick = { pendingLanguage = null }, variant = AreButtonVariant.Ghost)
                    AreButton(
                        copy.confirm,
                        onClick = {
                            scope.launch {
                                viewModel.setLanguageTag(targetLanguage)
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(targetLanguage))
                                pendingLanguage = null
                            }
                        },
                        variant = AreButtonVariant.Primary,
                    )
                },
            ) {}
        }
    }
}

/** A catalog is "stale" (nudge a refresh) once its last sync is older than this -- also the window
 * the sidebar Settings badge uses. Null (never refreshed / legacy row) counts as stale. */
const val REFRESH_STALE_MS = 14L * 24 * 60 * 60 * 1000

/** True when [this] last-refresh timestamp is missing or older than [REFRESH_STALE_MS]. */
fun Long?.isStale(): Boolean = this == null || System.currentTimeMillis() - this > REFRESH_STALE_MS

@Composable
private fun lastUpdatedLabel(ts: Long?, neverRefreshedText: String): String {
    if (ts == null) return neverRefreshedText
    val ago = System.currentTimeMillis() - ts
    val days = ago / (24 * 60 * 60 * 1000)
    val hours = ago / (60 * 60 * 1000)
    val mins = ago / (60 * 1000)
    return when {
        days >= 1 -> if (days == 1L) stringResource(R.string.settings_last_updated_days, days) else stringResource(R.string.settings_last_updated_days_plural, days)
        hours >= 1 -> if (hours == 1L) stringResource(R.string.settings_last_updated_hours, hours) else stringResource(R.string.settings_last_updated_hours_plural, hours)
        mins >= 1 -> stringResource(R.string.settings_last_updated_mins, mins)
        else -> stringResource(R.string.settings_last_updated_now)
    }
}

/** Local dialog step -- not persisted, purely UI navigation for the PIN flow. */
private enum class PinFlow { SetThenEnable, SetOnly, VerifyThenDisable, VerifyThenChange }

/** Right-hand control shared by the app-language and subtitle-language rows: the current selection
 * as text, with a "Change" button that opens the picker modal. */
@Composable
private fun SelectionChangeControl(current: String, onChange: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = current, style = AreIptvTheme.typography.label, color = AreIptvTheme.colors.textSecondary)
        AreButton(
            text = stringResource(R.string.settings_change),
            onClick = onChange,
            variant = AreButtonVariant.Secondary,
            size = AreButtonSize.Small,
        )
    }
}

@Composable
private fun MiniPlayerBehavior.label(): String = when (this) {
    MiniPlayerBehavior.DODGE -> stringResource(R.string.settings_mini_player_dodge)
    MiniPlayerBehavior.FADE -> stringResource(R.string.settings_mini_player_fade)
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
