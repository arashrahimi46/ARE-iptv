package com.arashrahimi46.iptv.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import android.content.Intent
import android.net.Uri
import com.arashrahimi46.iptv.BuildConfig
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.data.parser.XtreamAccountInfo
import com.arashrahimi46.iptv.data.settings.AutoRefreshInterval
import com.arashrahimi46.iptv.data.settings.AutoRelock
import com.arashrahimi46.iptv.data.settings.LockedContentDisplay
import com.arashrahimi46.iptv.data.settings.MiniPlayerBehavior
import com.arashrahimi46.iptv.data.settings.SidebarStyle
import com.arashrahimi46.iptv.data.settings.StartScreen
import com.arashrahimi46.iptv.data.settings.SubtitleColorChoice
import com.arashrahimi46.iptv.data.settings.SubtitleEdge
import com.arashrahimi46.iptv.data.settings.SubtitleFontChoice
import com.arashrahimi46.iptv.data.settings.SubtitleTextScale
import com.arashrahimi46.iptv.data.settings.ThemeMode
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreChipSize
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreLanguageOptions
import com.arashrahimi46.iptv.ui.components.AreLanguageSelector
import com.arashrahimi46.iptv.ui.components.AreSwitch
import com.arashrahimi46.iptv.ui.onboarding.LegalDocumentDialog
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.language.localizedConfirmCopy
import com.arashrahimi46.iptv.ui.player.SUBTITLE_LANGUAGES
import com.arashrahimi46.iptv.ui.theme.AccentPreset
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/**
 * Per-tab content panes for [SettingsScreen]. Each pane is a self-contained LazyColumn that reads
 * the shared [SettingsViewModel] flows and owns its own local state/dialogs, so the shell stays
 * thin. Rows reuse [SettingsSection]/[SettingsRow]/[SelectionChangeControl] from SettingsScreen.kt.
 */

// Top pad gives the first row's focus ring headroom before the LazyColumn's top clip; bottom pad
// keeps the last row clear of the screen edge.
private val PaneBottomPad = PaddingValues(top = 8.dp, bottom = 40.dp)

// PERF: which shape a pane item composes -- a stack of SettingsRows, a section carrying a text-field
// form block, or a read-only provider info panel. Without these, every item in a pane's LazyColumn
// shares the default null contentType, so Compose's slot-reuse pool can't tell them apart and may
// try to reuse a disposed node of the wrong shape when a differently-shaped section scrolls into
// view, forcing a full rebuild instead of a cheap skip. Reuse is per-lazy-layout, so these only
// need to be distinct within one pane -- the same three constants serve every pane.
private const val PaneItemRows = "rows"
private const val PaneItemForm = "form"
private const val PaneItemPanel = "panel"

// =============================================================================================
// GENERAL — playlists, provider, language, metadata, catalog reminder, storage & reset
// =============================================================================================

@Composable
internal fun GeneralPane(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val activeSource by viewModel.activeSource.collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()
    val staleWindowDays by viewModel.staleWindowDays.collectAsState()
    val providerInfo by viewModel.providerInfo.collectAsState()
    val stalkerInfo by viewModel.stalkerInfo.collectAsState()
    val languageTag by viewModel.languageTag.collectAsState()
    val omdbKey by viewModel.omdbKey.collectAsState()
    val omdbValidation by viewModel.omdbValidation.collectAsState()
    val startScreen by viewModel.startScreen.collectAsState()
    val autoRefreshInterval by viewModel.autoRefreshInterval.collectAsState()
    val confirmBeforeExit by viewModel.confirmBeforeExit.collectAsState()

    var omdbKeyInput by remember { mutableStateOf("") }
    var guide by remember { mutableStateOf<IntegrationGuide?>(null) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var pendingLanguage by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<GeneralConfirm?>(null) }
    // The confirmed locale, applied from OUTSIDE the confirm dialog's composition -- see the
    // LaunchedEffect below for why that separation is the whole fix.
    var applyLanguage by remember { mutableStateOf<String?>(null) }

    // Applying a locale calls Activity.recreate() (AppCompatDelegate's pre-API-33 path). Doing that
    // while the confirm Dialog is still on screen tears the dialog's window down mid-relaunch, and
    // the recreated Activity then never gains a focused window: WindowManager logs "Input channel
    // object ... was disposed without first being removed with the input manager", sits in "no window
    // has focus but ActivityRecord may eventually add a window", and force-finishes the app ~30s
    // later with "ANR ... Application does not have a focused window". The UI is drawn and looks fine
    // the whole time, which is why this reads as a random crash rather than an ANR -- the app dies on
    // the NEXT key press, not on the language change. Reproduced on the Sony XL95 (API 31).
    //
    // So: the confirm button only records the choice and closes the dialog; this effect lives in the
    // pane body (it outlives the dialog) and waits for the dialog window to actually be removed
    // before touching the locale. Two frames, not one -- dismissal runs through composition apply and
    // then the window teardown, and one frame lands too early.
    LaunchedEffect(applyLanguage) {
        val tag = applyLanguage ?: return@LaunchedEffect
        viewModel.setLanguageTag(tag)
        withFrameNanos { }
        withFrameNanos { }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    val refreshingText = stringResource(R.string.settings_refreshing)
    val neverRefreshedText = stringResource(R.string.settings_never_refreshed)
    val syncingSuffix = stringResource(R.string.settings_refresh_syncing)
    val staleSuffix = stringResource(R.string.settings_refresh_stale)

    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaneBottomPad) {
        item(key = "playlists", contentType = PaneItemRows) {
            SettingsSection(title = stringResource(R.string.settings_section_playlists)) {
                val refreshing = refreshState is RefreshState.Refreshing
                val stale = activeSource?.lastRefreshedAtMs.isStale(staleWindowDays)
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
                        text = if (refreshing) refreshingText else stringResource(R.string.settings_refresh_now),
                        onClick = { viewModel.refresh() },
                        disabled = refreshing || activeSource == null,
                        variant = if (stale) AreButtonVariant.Primary else AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Update,
                    title = stringResource(R.string.settings_stale_title),
                    desc = stringResource(R.string.settings_stale_desc),
                ) {
                    ChipChoiceRow(
                        options = StaleWindowOptions,
                        isSelected = { it.first == staleWindowDays },
                        label = { stringResource(it.second) },
                        onSelect = { viewModel.setStaleWindowDays(it.first) },
                    )
                }
            }
        }

        item(key = "preferences", contentType = PaneItemRows) {
            SettingsSection(title = stringResource(R.string.settings_section_preferences)) {
                SettingsRow(
                    icon = Icons.Filled.Home,
                    title = stringResource(R.string.settings_start_screen_title),
                    desc = stringResource(R.string.settings_start_screen_desc),
                ) {
                    ChipChoiceRow(
                        modifier = Modifier.weight(1f),
                        options = StartScreen.values().asIterable(),
                        isSelected = { it == startScreen },
                        label = { stringResource(it.labelRes()) },
                        onSelect = { viewModel.setStartScreen(it) },
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Sync,
                    title = stringResource(R.string.settings_auto_refresh_title),
                    desc = stringResource(R.string.settings_auto_refresh_desc),
                ) {
                    ChipChoiceRow(
                        modifier = Modifier.weight(1f),
                        options = AutoRefreshInterval.values().asIterable(),
                        isSelected = { it == autoRefreshInterval },
                        label = { stringResource(it.labelRes()) },
                        onSelect = { viewModel.setAutoRefreshInterval(it) },
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.ExitToApp,
                    title = stringResource(R.string.settings_confirm_exit_title),
                    desc = stringResource(R.string.settings_confirm_exit_desc),
                ) {
                    AreSwitch(checked = confirmBeforeExit, onCheckedChange = { viewModel.setConfirmBeforeExit(it) })
                }
            }
        }

        // Provider account panel -- Xtream/Stalker carry account metadata; M3U playlists don't.
        if (activeSource?.type == SourceType.XTREAM) {
            item(key = "provider-xtream", contentType = PaneItemPanel) {
                SettingsSection(title = stringResource(R.string.settings_section_provider)) {
                    ProviderPanel(info = providerInfo)
                }
            }
        }
        if (activeSource?.type == SourceType.STALKER) {
            item(key = "provider-stalker", contentType = PaneItemPanel) {
                SettingsSection(title = stringResource(R.string.settings_section_provider)) {
                    StalkerProviderPanel(info = stalkerInfo)
                }
            }
        }

        item(key = "language", contentType = PaneItemRows) {
            SettingsSection(title = stringResource(R.string.settings_section_language)) {
                SettingsRow(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.settings_language_row_title),
                    desc = stringResource(R.string.settings_language_row_desc),
                ) {
                    // Show the locale that is ACTUALLY rendering, not the DataStore mirror. The two
                    // can diverge -- e.g. the apply below is a LaunchedEffect that waits two frames
                    // before calling setApplicationLocales, and if the pane leaves composition in
                    // that window the effect is cancelled while setLanguageTag (on viewModelScope)
                    // has already persisted. The row then claimed "فارسی" over an English UI, with
                    // no way back because MainActivity's resync only fires when the delegate is
                    // empty. The delegate is the single source of truth for what the user sees; the
                    // mirror is only a fallback for a cold start before it has been restored.
                    val effectiveTag = AppCompatDelegate.getApplicationLocales()
                        .takeUnless { it.isEmpty }?.get(0)?.toLanguageTag() ?: languageTag
                    SelectionChangeControl(
                        current = AreLanguageOptions.firstOrNull { it.tag.equals(effectiveTag, ignoreCase = true) }
                            ?.let { stringResource(it.nativeNameRes) }
                        // A tag like "pt-BR" can come back region-tagged ("pt-BR") or bare ("pt");
                        // fall back to a language-only match before giving up and showing the raw tag.
                            ?: AreLanguageOptions.firstOrNull {
                                it.tag.substringBefore('-').equals(effectiveTag.substringBefore('-'), ignoreCase = true)
                            }?.let { stringResource(it.nativeNameRes) } ?: effectiveTag,
                        onChange = { showLanguagePicker = true },
                    )
                }
            }
        }

        item(key = "metadata", contentType = PaneItemForm) {
            SettingsSection(title = stringResource(R.string.settings_section_metadata)) {
                OmdbBlock(omdbKey, omdbValidation, omdbKeyInput, { omdbKeyInput = it }, { guide = IntegrationGuide.Omdb }, viewModel)
            }
        }

        item(key = "storage", contentType = PaneItemRows) {
            SettingsSection(title = stringResource(R.string.settings_section_storage)) {
                SettingsRow(
                    icon = Icons.Filled.Image,
                    title = stringResource(R.string.settings_clear_cache_title),
                    desc = stringResource(R.string.settings_clear_cache_desc),
                ) {
                    AreButton(
                        text = stringResource(R.string.action_clear),
                        onClick = { confirm = GeneralConfirm.ClearCache },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.History,
                    title = stringResource(R.string.settings_clear_history_title),
                    desc = stringResource(R.string.settings_clear_history_desc),
                ) {
                    AreButton(
                        text = stringResource(R.string.action_clear),
                        onClick = { confirm = GeneralConfirm.ClearHistory },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.RestartAlt,
                    title = stringResource(R.string.settings_reset_title),
                    desc = stringResource(R.string.settings_reset_desc),
                ) {
                    AreButton(
                        text = stringResource(R.string.settings_reset_action),
                        onClick = { confirm = GeneralConfirm.Reset },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                }
            }
        }
    }

    // --- Dialogs ---
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

    val targetLanguage = pendingLanguage
    if (targetLanguage != null) {
        val context = LocalContext.current
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
                            // Close the dialog FIRST; the effect above applies the locale once its
                            // window is gone.
                            pendingLanguage = null
                            applyLanguage = targetLanguage
                        },
                        variant = AreButtonVariant.Primary,
                    )
                },
            ) {}
        }
    }

    guide?.let { g ->
        Dialog(onDismissRequest = { guide = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            IntegrationGuideDialog(guide = g, onDismiss = { guide = null })
        }
    }

    when (confirm) {
        GeneralConfirm.ClearCache -> ConfirmActionDialog(
            title = stringResource(R.string.settings_clear_cache_title),
            message = stringResource(R.string.settings_clear_cache_confirm),
            confirmText = stringResource(R.string.action_clear),
            onConfirm = { viewModel.clearImageCache() },
            onDismiss = { confirm = null },
        )
        GeneralConfirm.ClearHistory -> ConfirmActionDialog(
            title = stringResource(R.string.settings_clear_history_title),
            message = stringResource(R.string.settings_clear_history_confirm),
            confirmText = stringResource(R.string.action_clear),
            onConfirm = { viewModel.clearHistory() },
            onDismiss = { confirm = null },
        )
        GeneralConfirm.Reset -> ConfirmActionDialog(
            title = stringResource(R.string.settings_reset_title),
            message = stringResource(R.string.settings_reset_confirm),
            confirmText = stringResource(R.string.settings_reset_action),
            onConfirm = { viewModel.resetToDefaults() },
            onDismiss = { confirm = null },
        )
        null -> Unit
    }
}

private enum class GeneralConfirm { ClearCache, ClearHistory, Reset }

/** Stale-window chip options: (days, label). 0 = never nudge. Order = shortest → Off. */
private val StaleWindowOptions = listOf(
    7L to R.string.settings_stale_7d,
    14L to R.string.settings_stale_14d,
    30L to R.string.settings_stale_30d,
    0L to R.string.settings_stale_off,
)

@Composable
private fun OmdbBlock(
    omdbKey: String?,
    omdbValidation: OmdbValidation,
    input: String,
    onInput: (String) -> Unit,
    onHowTo: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val colors = AreIptvTheme.colors
    if (omdbKey != null) {
        SettingsRow(
            icon = Icons.Filled.Star,
            title = stringResource(R.string.settings_omdb_title),
            desc = stringResource(R.string.settings_omdb_connected),
        ) {
            AreButton(
                text = stringResource(R.string.action_disconnect),
                onClick = { viewModel.disconnectOmdb(); onInput("") },
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
            Text(text = stringResource(R.string.settings_omdb_key_desc), style = AreIptvTheme.typography.caption, color = colors.textTertiary)
            Box(Modifier.padding(top = 12.dp))
            AreTextField(
                value = input,
                onValueChange = onInput,
                placeholder = stringResource(R.string.settings_placeholder_api_key),
                mono = true,
                icon = Icons.Filled.VpnKey,
                error = omdbError,
                activateOnClick = true,
            )
            Box(Modifier.padding(top = 12.dp))
            // The walkthrough sits in this branch only, so it disappears the moment a key is
            // connected -- nobody needs setup instructions for something already set up.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AreButton(
                    text = if (validatingOmdb) stringResource(R.string.settings_checking) else stringResource(R.string.action_connect),
                    onClick = { viewModel.connectOmdb(input) },
                    disabled = validatingOmdb || input.isBlank(),
                    variant = AreButtonVariant.Primary,
                    size = AreButtonSize.Small,
                )
                AreButton(
                    text = stringResource(R.string.settings_howto_key),
                    onClick = onHowTo,
                    variant = AreButtonVariant.Ghost,
                    size = AreButtonSize.Small,
                    icon = Icons.Filled.HelpOutline,
                )
            }
        }
    }
}

// =============================================================================================
// DISPLAY — theme mode, accent, reduce motion, list view, clock
// =============================================================================================

@Composable
internal fun DisplayPane(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val isDark = AreIptvTheme.isDark
    val themeMode by viewModel.themeMode.collectAsState()
    val sidebarStyle by viewModel.sidebarStyle.collectAsState()
    val darkAccent by viewModel.darkAccent.collectAsState()
    val lightAccent by viewModel.lightAccent.collectAsState()
    val isReducedMotion by viewModel.isReducedMotion.collectAsState()
    val isBrowseListMode by viewModel.isBrowseListMode.collectAsState()
    val is24HourClock by viewModel.is24HourClock.collectAsState()

    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaneBottomPad) {
        item(key = "appearance", contentType = PaneItemRows) {
            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                SettingsRow(
                    icon = Icons.Filled.DarkMode,
                    title = stringResource(R.string.settings_theme_title),
                    desc = stringResource(R.string.settings_theme_desc),
                ) {
                    ChipChoiceRow(
                        options = ThemeMode.entries,
                        isSelected = { it == themeMode },
                        label = { stringResource(it.labelRes()) },
                        onSelect = { viewModel.setThemeMode(it) },
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Style,
                    title = stringResource(R.string.settings_sidebar_style),
                    desc = stringResource(R.string.settings_sidebar_style_desc),
                ) {
                    ChipChoiceRow(
                        options = SidebarStyle.entries,
                        isSelected = { it == sidebarStyle },
                        label = { stringResource(it.labelRes()) },
                        onSelect = { viewModel.setSidebarStyle(it) },
                    )
                }
                AccentPickerBlock(
                    selected = if (isDark) darkAccent else lightAccent,
                    isDark = isDark,
                    onSelect = { viewModel.setActiveAccent(isDark, it) },
                )
                SettingsRow(
                    icon = Icons.Filled.Bolt,
                    title = stringResource(R.string.settings_reduce_motion),
                    desc = stringResource(R.string.settings_reduce_motion_desc),
                ) {
                    AreSwitch(checked = isReducedMotion, onCheckedChange = viewModel::setReducedMotion)
                }
                SettingsRow(
                    icon = Icons.Filled.ViewAgenda,
                    title = stringResource(R.string.settings_list_view),
                    desc = stringResource(R.string.settings_list_view_desc),
                ) {
                    AreSwitch(checked = isBrowseListMode, onCheckedChange = viewModel::setBrowseListMode)
                }
                SettingsRow(
                    icon = Icons.Filled.Schedule,
                    title = stringResource(R.string.settings_clock_title),
                    desc = stringResource(R.string.settings_clock_desc),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AreChip(
                            text = stringResource(R.string.settings_clock_24h),
                            selected = is24HourClock,
                            onClick = { viewModel.set24HourClock(true) },
                            size = AreChipSize.Small,
                        )
                        AreChip(
                            text = stringResource(R.string.settings_clock_12h),
                            selected = !is24HourClock,
                            onClick = { viewModel.set24HourClock(false) },
                            size = AreChipSize.Small,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.DARK -> R.string.settings_theme_dark
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.SYSTEM -> R.string.settings_theme_system
}

private fun SidebarStyle.labelRes(): Int = when (this) {
    SidebarStyle.FLOATING -> R.string.settings_sidebar_style_floating
    SidebarStyle.EDGE -> R.string.settings_sidebar_style_edge
}

private fun StartScreen.labelRes(): Int = when (this) {
    StartScreen.HOME -> R.string.nav_home
    StartScreen.LIVE -> R.string.nav_live_tv
    StartScreen.MOVIES -> R.string.nav_movies
    StartScreen.SERIES -> R.string.nav_series
    StartScreen.LAST_USED -> R.string.settings_start_last_used
}

private fun AutoRefreshInterval.labelRes(): Int = when (this) {
    AutoRefreshInterval.OFF -> R.string.settings_auto_refresh_off
    AutoRefreshInterval.DAILY -> R.string.settings_auto_refresh_daily
    AutoRefreshInterval.WEEKLY -> R.string.settings_auto_refresh_weekly
}

private fun AutoRelock.labelRes(): Int = when (this) {
    AutoRelock.IMMEDIATELY -> R.string.settings_relock_immediately
    AutoRelock.MIN_15 -> R.string.settings_relock_15min
    AutoRelock.HOUR_1 -> R.string.settings_relock_1hour
    AutoRelock.NEVER -> R.string.settings_relock_never
}

private fun LockedContentDisplay.labelRes(): Int = when (this) {
    LockedContentDisplay.HIDE -> R.string.settings_locked_hide
    LockedContentDisplay.BLUR -> R.string.settings_locked_blur
}

private fun SubtitleTextScale.labelRes(): Int = when (this) {
    SubtitleTextScale.SMALL -> R.string.settings_sub_size_s
    SubtitleTextScale.MEDIUM -> R.string.settings_sub_size_m
    SubtitleTextScale.LARGE -> R.string.settings_sub_size_l
    SubtitleTextScale.XLARGE -> R.string.settings_sub_size_xl
}

private fun SubtitleEdge.labelRes(): Int = when (this) {
    SubtitleEdge.BOX -> R.string.settings_sub_style_box
    SubtitleEdge.OUTLINE -> R.string.settings_sub_style_outline
    SubtitleEdge.SHADOW -> R.string.settings_sub_style_shadow
}

private fun SubtitleFontChoice.labelRes(): Int = when (this) {
    SubtitleFontChoice.DEFAULT -> R.string.settings_sub_font_default
    SubtitleFontChoice.SANS -> R.string.settings_sub_font_sans
    SubtitleFontChoice.SERIF -> R.string.settings_sub_font_serif
    SubtitleFontChoice.MONO -> R.string.settings_sub_font_mono
    SubtitleFontChoice.VAZIRMATN -> R.string.settings_sub_font_vazirmatn
}

/** A focusable color square for the subtitle color picker (mirrors the accent swatch shape). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SubtitleColorSwatch(color: Color, selected: Boolean, onSelect: () -> Unit) {
    val colors = AreIptvTheme.colors
    val fillShape = RoundedCornerShape(AreIptvTheme.radius.sm)
    TvFocusable(onClick = onSelect, modifier = Modifier.size(40.dp), shape = RoundedCornerShape(AreIptvTheme.radius.md)) { focused, _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .background(color, fillShape)
                .border(
                    width = if (focused || selected) 2.dp else 1.dp,
                    color = if (focused || selected) colors.textPrimary else colors.borderDefault,
                    shape = fillShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        }
    }
}

// =============================================================================================
// PLAYBACK — hardware decoding, autoplay next, mini-player, recording indicator
// =============================================================================================

@Composable
internal fun PlaybackPane(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val isHardwareDecoding by viewModel.isHardwareDecoding.collectAsState()
    val autoplayNextDelay by viewModel.autoplayNextDelaySeconds.collectAsState()
    val preferredAudioLang by viewModel.preferredAudioLanguage.collectAsState()
    val miniPlayerBehavior by viewModel.miniPlayerBehavior.collectAsState()
    val isRecordingIndicatorEnabled by viewModel.isRecordingIndicatorEnabled.collectAsState()
    val hudLayoutLive by viewModel.hudLayoutLive.collectAsState()
    val hudLayoutVod by viewModel.hudLayoutVod.collectAsState()

    var showAudioLangPicker by remember { mutableStateOf(false) }
    var showHudEditor by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaneBottomPad) {
        item(key = "playback", contentType = PaneItemRows) {
            SettingsSection(title = stringResource(R.string.settings_section_playback)) {
                SettingsRow(
                    icon = Icons.Filled.HighQuality,
                    title = stringResource(R.string.settings_hardware_decoding),
                    desc = stringResource(R.string.settings_hardware_decoding_desc),
                ) {
                    AreSwitch(checked = isHardwareDecoding, onCheckedChange = viewModel::setHardwareDecoding)
                }
                SettingsRow(
                    icon = Icons.Filled.Audiotrack,
                    title = stringResource(R.string.settings_preferred_audio_title),
                    desc = stringResource(R.string.settings_preferred_audio_desc),
                ) {
                    SelectionChangeControl(
                        current = SUBTITLE_LANGUAGES.firstOrNull { it.first == preferredAudioLang }?.second
                            ?: stringResource(R.string.settings_audio_auto),
                        onChange = { showAudioLangPicker = true },
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.SkipNext,
                    title = stringResource(R.string.settings_autoplay_next),
                    desc = stringResource(R.string.settings_autoplay_next_desc),
                ) {
                    ChipChoiceRow(
                        options = AutoplayDelayOptions,
                        isSelected = { it.first == autoplayNextDelay },
                        label = { stringResource(it.second) },
                        onSelect = { viewModel.setAutoplayNextDelaySeconds(it.first) },
                    )
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
                                size = AreChipSize.Small,
                            )
                        }
                    }
                }
                SettingsRow(
                    icon = Icons.Filled.FiberManualRecord,
                    title = stringResource(R.string.settings_recording_indicator_title),
                    desc = stringResource(R.string.settings_recording_indicator_desc),
                ) {
                    AreSwitch(checked = isRecordingIndicatorEnabled, onCheckedChange = viewModel::setRecordingIndicatorEnabled)
                }
                SettingsRow(
                    icon = Icons.Filled.Tune,
                    title = stringResource(R.string.settings_rearrange_hud),
                    desc = stringResource(R.string.settings_rearrange_hud_desc),
                ) {
                    AreButton(
                        text = stringResource(R.string.settings_change),
                        onClick = { showHudEditor = true },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                }
            }
        }
    }

    if (showHudEditor) {
        Dialog(onDismissRequest = { showHudEditor = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            HudLayoutEditor(
                liveArrangement = hudLayoutLive,
                vodArrangement = hudLayoutVod,
                onApply = viewModel::setHudLayout,
                onDismiss = { showHudEditor = false },
            )
        }
    }

    if (showAudioLangPicker) {
        // "Auto" first (blank code = let the player choose), then the shared language list.
        val options = listOf("" to stringResource(R.string.settings_audio_auto)) + SUBTITLE_LANGUAGES
        Dialog(onDismissRequest = { showAudioLangPicker = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            AreDialog(onDismiss = { showAudioLangPicker = false }, title = stringResource(R.string.settings_preferred_audio_title)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    options.forEach { (code, name) ->
                        AreChip(
                            text = name,
                            selected = code == preferredAudioLang,
                            onClick = {
                                viewModel.setPreferredAudioLanguage(code)
                                showAudioLangPicker = false
                            },
                            size = AreChipSize.Small,
                        )
                    }
                }
            }
        }
    }
}

/** Autoplay-next-episode chip options: (delaySeconds, label). 0 = off. */
private val AutoplayDelayOptions = listOf(
    0L to R.string.settings_autoplay_off,
    5L to R.string.settings_autoplay_5s,
    10L to R.string.settings_autoplay_10s,
)

// =============================================================================================
// SUBTITLES — subtitle language, OpenSubtitles account/key, appearance
// =============================================================================================

@Composable
internal fun SubtitlesPane(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val colors = AreIptvTheme.colors
    val subtitleLanguage by viewModel.subtitleLanguage.collectAsState()
    val subtitleTextScale by viewModel.subtitleTextScale.collectAsState()
    val subtitleEdge by viewModel.subtitleEdge.collectAsState()
    val subtitleColor by viewModel.subtitleColor.collectAsState()
    val subtitleFont by viewModel.subtitleFont.collectAsState()
    val openSubsCredential by viewModel.openSubsCredential.collectAsState()
    val subsValidation by viewModel.subsValidation.collectAsState()
    val openSubsUsername by viewModel.openSubsUsername.collectAsState()
    val subsLogin by viewModel.subsLogin.collectAsState()

    var subsKeyInput by remember { mutableStateOf("") }
    var subsUserInput by remember { mutableStateOf("") }
    var subsPassInput by remember { mutableStateOf("") }
    var guide by remember { mutableStateOf<IntegrationGuide?>(null) }
    var showSubtitlePicker by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaneBottomPad) {
        item(key = "subtitles", contentType = PaneItemForm) {
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
                        desc = if (openSubsUsername != null) stringResource(R.string.settings_opensubs_ready) else stringResource(R.string.settings_opensubs_key_connected),
                    ) {
                        AreButton(
                            text = stringResource(R.string.action_disconnect),
                            onClick = {
                                viewModel.disconnectOpenSubs()
                                subsKeyInput = ""; subsUserInput = ""; subsPassInput = ""
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
                            Text(text = stringResource(R.string.settings_signin_desc), style = AreIptvTheme.typography.caption, color = colors.textTertiary)
                            Box(Modifier.padding(top = 12.dp))
                            AreTextField(
                                value = subsUserInput,
                                onValueChange = { subsUserInput = it },
                                placeholder = stringResource(R.string.settings_placeholder_username),
                                icon = Icons.Filled.Person,
                                activateOnClick = true,
                            )
                            Box(Modifier.padding(top = 10.dp))
                            AreTextField(
                                value = subsPassInput,
                                onValueChange = { subsPassInput = it },
                                placeholder = stringResource(R.string.settings_placeholder_password),
                                masked = true,
                                icon = Icons.Filled.Lock,
                                error = loginError,
                                activateOnClick = true,
                            )
                            Box(Modifier.padding(top = 12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AreButton(
                                    text = if (signingIn) stringResource(R.string.settings_signing_in) else stringResource(R.string.action_sign_in),
                                    onClick = { viewModel.signInOpenSubs(subsUserInput, subsPassInput) },
                                    disabled = signingIn || subsUserInput.isBlank() || subsPassInput.isBlank(),
                                    variant = AreButtonVariant.Primary,
                                    size = AreButtonSize.Small,
                                )
                                AreButton(
                                    text = stringResource(R.string.settings_howto_account),
                                    onClick = { guide = IntegrationGuide.OpenSubsAccount },
                                    variant = AreButtonVariant.Ghost,
                                    size = AreButtonSize.Small,
                                    icon = Icons.Filled.HelpOutline,
                                )
                            }
                        }
                    }
                } else {
                    val validating = subsValidation is SubsValidation.Validating
                    val errorMsg = (subsValidation as? SubsValidation.Error)?.message
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                        Text(text = stringResource(R.string.settings_opensubs_key_title), style = AreIptvTheme.typography.label, color = colors.textPrimary)
                        Box(Modifier.padding(top = 4.dp))
                        Text(text = stringResource(R.string.settings_opensubs_key_desc), style = AreIptvTheme.typography.caption, color = colors.textTertiary)
                        Box(Modifier.padding(top = 12.dp))
                        AreTextField(
                            value = subsKeyInput,
                            onValueChange = { subsKeyInput = it },
                            placeholder = stringResource(R.string.settings_placeholder_api_key),
                            mono = true,
                            icon = Icons.Filled.VpnKey,
                            error = errorMsg,
                            activateOnClick = true,
                        )
                        Box(Modifier.padding(top = 12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AreButton(
                                text = if (validating) stringResource(R.string.settings_checking) else stringResource(R.string.action_connect),
                                onClick = { viewModel.connectOpenSubs(subsKeyInput) },
                                disabled = validating || subsKeyInput.isBlank(),
                                variant = AreButtonVariant.Primary,
                                size = AreButtonSize.Small,
                            )
                            AreButton(
                                text = stringResource(R.string.settings_howto_key),
                                onClick = { guide = IntegrationGuide.OpenSubsKey },
                                variant = AreButtonVariant.Ghost,
                                size = AreButtonSize.Small,
                                icon = Icons.Filled.HelpOutline,
                            )
                        }
                    }
                }
            }
        }

        item(key = "subtitle-appearance", contentType = PaneItemRows) {
            SettingsSection(title = stringResource(R.string.settings_section_subtitle_appearance)) {
                SettingsRow(
                    icon = Icons.Filled.FormatSize,
                    title = stringResource(R.string.settings_sub_size_title),
                    desc = stringResource(R.string.settings_sub_size_desc),
                ) {
                    ChipChoiceRow(
                        options = SubtitleTextScale.entries,
                        isSelected = { it == subtitleTextScale },
                        label = { stringResource(it.labelRes()) },
                        onSelect = { viewModel.setSubtitleTextScale(it) },
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Style,
                    title = stringResource(R.string.settings_sub_style_title),
                    desc = stringResource(R.string.settings_sub_style_desc),
                ) {
                    ChipChoiceRow(
                        options = SubtitleEdge.entries,
                        isSelected = { it == subtitleEdge },
                        label = { stringResource(it.labelRes()) },
                        onSelect = { viewModel.setSubtitleEdge(it) },
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_sub_color_title),
                    desc = stringResource(R.string.settings_sub_color_desc),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SubtitleColorChoice.entries.forEach { choice ->
                            SubtitleColorSwatch(
                                color = Color(choice.argb),
                                selected = choice == subtitleColor,
                                onSelect = { viewModel.setSubtitleColor(choice) },
                            )
                        }
                    }
                }
                SettingsRow(
                    icon = Icons.Filled.FontDownload,
                    title = stringResource(R.string.settings_sub_font_title),
                    desc = stringResource(R.string.settings_sub_font_desc),
                ) {
                    ChipChoiceRow(
                        options = SubtitleFontChoice.entries,
                        isSelected = { it == subtitleFont },
                        label = { stringResource(it.labelRes()) },
                        onSelect = { viewModel.setSubtitleFont(it) },
                    )
                }
            }
        }
    }

    guide?.let { g ->
        Dialog(onDismissRequest = { guide = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            IntegrationGuideDialog(guide = g, onDismiss = { guide = null })
        }
    }

    if (showSubtitlePicker) {
        Dialog(onDismissRequest = { showSubtitlePicker = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            AreDialog(onDismiss = { showSubtitlePicker = false }, title = stringResource(R.string.settings_subtitle_lang_title)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SUBTITLE_LANGUAGES.forEach { (code, name) ->
                        AreChip(
                            text = name,
                            selected = code == subtitleLanguage,
                            onClick = {
                                viewModel.setSubtitleLanguage(code)
                                showSubtitlePicker = false
                            },
                            size = AreChipSize.Small,
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================================
// PARENTAL — lock adult content, set/change PIN
// =============================================================================================

@Composable
internal fun ParentalPane(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    val hasPinSet by viewModel.hasPinSet.collectAsState()
    val pinLoaded = hasPinSet != null
    val hasPin = hasPinSet == true
    var pinDialog by remember { mutableStateOf<PinFlow?>(null) }
    val autoRelock by viewModel.parentalAutoRelock.collectAsState()
    val lockedDisplay by viewModel.lockedContentDisplay.collectAsState()
    val keywords by viewModel.parentalKeywords.collectAsState()
    val pinOnLaunch by viewModel.isPinOnLaunch.collectAsState()
    var keywordInput by remember { mutableStateOf("") }

    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaneBottomPad) {
        item(key = "parental", contentType = PaneItemRows) {
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
                                if (hasPin) viewModel.setParentalLockEnabled(true) else pinDialog = PinFlow.SetThenEnable
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

        item(key = "content-locking", contentType = PaneItemForm) {
            SettingsSection(title = stringResource(R.string.settings_section_content_locking)) {
                SettingsRow(
                    icon = Icons.Filled.Schedule,
                    title = stringResource(R.string.settings_relock_title),
                    desc = stringResource(R.string.settings_relock_desc),
                ) {
                    ChipChoiceRow(
                        options = AutoRelock.values().asIterable(),
                        isSelected = { it == autoRelock },
                        label = { stringResource(it.labelRes()) },
                        onSelect = { viewModel.setParentalAutoRelock(it) },
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Visibility,
                    title = stringResource(R.string.settings_locked_display_title),
                    desc = stringResource(R.string.settings_locked_display_desc),
                ) {
                    ChipChoiceRow(
                        options = LockedContentDisplay.values().asIterable(),
                        isSelected = { it == lockedDisplay },
                        label = { stringResource(it.labelRes()) },
                        onSelect = { viewModel.setLockedContentDisplay(it) },
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Login,
                    title = stringResource(R.string.settings_pin_launch_title),
                    desc = stringResource(R.string.settings_pin_launch_desc),
                ) {
                    AreSwitch(
                        checked = pinOnLaunch,
                        disabled = !hasPin,
                        onCheckedChange = { viewModel.setPinOnLaunch(it) },
                    )
                }
                ParentalKeywordsBlock(
                    keywords = keywords,
                    input = keywordInput,
                    onInput = { keywordInput = it },
                    onAdd = {
                        if (keywordInput.isNotBlank()) { viewModel.toggleParentalKeyword(keywordInput); keywordInput = "" }
                    },
                    onRemove = { viewModel.toggleParentalKeyword(it) },
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

/** Custom blocked-keyword manager: type a word to add, tap a chip to remove. Words merge into
 * [com.arashrahimi46.iptv.data.settings.AdultContentFilter]'s built-in markers. */
@Composable
private fun ParentalKeywordsBlock(
    keywords: Set<String>,
    input: String,
    onInput: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    val colors = AreIptvTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(text = stringResource(R.string.settings_keywords_title), style = AreIptvTheme.typography.label, color = colors.textPrimary)
        Box(Modifier.padding(top = 4.dp))
        Text(text = stringResource(R.string.settings_keywords_desc), style = AreIptvTheme.typography.caption, color = colors.textTertiary)
        Box(Modifier.padding(top = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                AreTextField(
                    value = input,
                    onValueChange = onInput,
                    placeholder = stringResource(R.string.settings_keywords_placeholder),
                    icon = Icons.Filled.Label,
                    activateOnClick = true,
                )
            }
            AreButton(
                text = stringResource(R.string.settings_keywords_add),
                onClick = onAdd,
                disabled = input.isBlank(),
                variant = AreButtonVariant.Secondary,
                size = AreButtonSize.Small,
            )
        }
        if (keywords.isNotEmpty()) {
            Box(Modifier.padding(top = 14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                keywords.sorted().forEach { word ->
                    AreChip(text = word, selected = true, onClick = { onRemove(word) }, size = AreChipSize.Small)
                }
            }
        }
    }
}

// =============================================================================================
// ABOUT — version, donate, feedback, analytics
// =============================================================================================

@Composable
internal fun AboutPane(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isAnalyticsEnabled by viewModel.isAnalyticsEnabled.collectAsState()
    val isCrashReportingEnabled by viewModel.isCrashReportingEnabled.collectAsState()
    var showFeedback by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var showLegal by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaneBottomPad) {
        item(key = "about", contentType = PaneItemRows) {
            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                SettingsRow(icon = Icons.Filled.Info, title = stringResource(R.string.settings_version_title), desc = stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME)) {
                    AreButton(
                        text = stringResource(R.string.settings_whats_new),
                        onClick = { showWhatsNew = true },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Favorite,
                    title = stringResource(R.string.settings_support_title),
                    desc = stringResource(R.string.settings_support_desc),
                ) {
                    AreButton(
                        text = stringResource(R.string.action_buy_coffee),
                        // Shows a QR instead of firing ACTION_VIEW: a TV often has no browser at
                        // all, and where it does, paying on one with a D-pad is miserable. The
                        // phone is the right device for this, so hand off to it.
                        onClick = { showSupport = true },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Feedback,
                    title = stringResource(R.string.settings_feedback_title),
                    desc = stringResource(R.string.settings_feedback_desc),
                ) {
                    AreButton(
                        text = stringResource(R.string.settings_feedback_action),
                        onClick = { showFeedback = true },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                }
                SettingsRow(
                    icon = Icons.Filled.Analytics,
                    title = stringResource(R.string.settings_analytics_title),
                    desc = stringResource(R.string.settings_analytics_desc),
                ) {
                    AreSwitch(checked = isAnalyticsEnabled, onCheckedChange = viewModel::setAnalyticsEnabled)
                }
                // The opt-out the Privacy Policy promises. Without this row the policy would be
                // describing a control that does not exist.
                SettingsRow(
                    icon = Icons.Filled.BugReport,
                    title = stringResource(R.string.settings_crash_title),
                    desc = stringResource(R.string.settings_crash_desc),
                ) {
                    AreSwitch(checked = isCrashReportingEnabled, onCheckedChange = viewModel::setCrashReportingEnabled)
                }
                SettingsRow(
                    icon = Icons.Filled.Policy,
                    title = stringResource(R.string.settings_legal_title),
                    desc = stringResource(R.string.settings_legal_desc),
                ) {
                    AreButton(
                        text = stringResource(R.string.settings_legal_action),
                        onClick = { showLegal = true },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                }
            }
        }
    }

    if (showLegal) LegalDocumentDialog(onDismiss = { showLegal = false })

    if (showSupport) {
        Dialog(onDismissRequest = { showSupport = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            SupportDialog(onDismiss = { showSupport = false })
        }
    }

    if (showFeedback) {
        Dialog(onDismissRequest = { showFeedback = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            FeedbackDialog(onDismiss = { showFeedback = false })
        }
    }

    if (showWhatsNew) {
        Dialog(onDismissRequest = { showWhatsNew = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            WhatsNewDialog(onDismiss = { showWhatsNew = false })
        }
    }
}

/** The Buy Me a Coffee page. Encoded into the QR at render time rather than shipping a QR image, so
 *  the code and the printed address can never drift apart. */
private const val SUPPORT_URL = "https://buymeacoffee.com/arashrahimi46"

/**
 * Support hand-off: a QR to scan plus the address in plain text underneath.
 *
 * Deliberately not `ACTION_VIEW`: many Android TV devices ship no browser, and on the ones that do,
 * entering card details with a D-pad is punishing. The phone is the right device, so the TV's only
 * job is to get the URL across the room. Same pattern as [IntegrationGuideDialog].
 */
@Composable
private fun SupportDialog(onDismiss: () -> Unit) {
    val colors = AreIptvTheme.colors
    val closeFocus = remember { FocusRequester() }
    // Sole focusable in the dialog -- claim focus on open or the remote has nothing to act on.
    LaunchedEffect(Unit) { runCatching { closeFocus.requestFocus() } }

    AreDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.support_dialog_title),
        width = 860.dp,
        actions = {
            AreButton(
                text = stringResource(R.string.action_close),
                onClick = onDismiss,
                variant = AreButtonVariant.Primary,
                modifier = Modifier.focusRequester(closeFocus),
            )
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            // Buy Me a Coffee's own branded code (their cup mark in the centre). Kept as an asset
            // rather than generated: the mark is theirs and a generator can't reproduce it.
            // If SUPPORT_URL ever changes this PNG must be regenerated with it -- they are the one
            // pair in this file that can silently disagree.
            Image(
                painter = painterResource(R.drawable.support_qr),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    // White quiet-zone plate: the code is black-on-transparent, so on the dark
                    // glass dialog it would be unscannable without a light backing.
                    .background(Color.White, RoundedCornerShape(AreIptvTheme.radius.md))
                    .padding(10.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.support_dialog_body),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.support_dialog_scan),
                        style = AreIptvTheme.typography.label,
                        color = colors.textPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.support_dialog_visit),
                            style = AreIptvTheme.typography.caption,
                            color = colors.textTertiary,
                        )
                        Text(
                            // Scheme stripped: it's noise on a card someone is reading off a TV to
                            // type into a phone browser, which supplies https:// itself.
                            text = SUPPORT_URL.removePrefix("https://"),
                            style = AreIptvTheme.typography.mono,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

/** Changelog modal shown from the About > Version row. Bullets are translated per locale; keep the
 * copy short and generic ("fixed bugs") so translators aren't chasing every release note. */
@Composable
private fun WhatsNewDialog(onDismiss: () -> Unit) {
    val colors = AreIptvTheme.colors
    val bullets = listOf(
        stringResource(R.string.settings_changelog_1),
        stringResource(R.string.settings_changelog_2),
        stringResource(R.string.settings_changelog_3),
    )
    AreDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.settings_whats_new_title),
        actions = { AreButton(stringResource(R.string.action_close), onClick = onDismiss, variant = AreButtonVariant.Primary) },
    ) {
        Text(
            text = stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME),
            style = AreIptvTheme.typography.caption,
            color = colors.textTertiary,
        )
        Box(Modifier.padding(top = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bullets.forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "•", style = AreIptvTheme.typography.body, color = colors.accent)
                    Text(text = line, style = AreIptvTheme.typography.body, color = colors.textSecondary)
                }
            }
        }
    }
}

// =============================================================================================
// Shared pane helpers
// =============================================================================================

/** A row of small chips for a discrete enum/number choice (D3); [isSelected] marks the active one. */
@Composable
private fun <T> ChipChoiceRow(
    modifier: Modifier = Modifier,
    options: Iterable<T>,
    isSelected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    // FlowRow (not Row) so a wide chip set wraps to a second line instead of clipping / squeezing
    // the row's title to zero width when the content area is narrow (e.g. the rail is expanded).
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            AreChip(text = label(option), selected = isSelected(option), onClick = { onSelect(option) }, size = AreChipSize.Small)
        }
    }
}

/** Confirm dialog for a destructive/one-shot action -- real Dialog window so D-pad focus is trapped. */
@Composable
private fun ConfirmActionDialog(title: String, message: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(
            onDismiss = onDismiss,
            title = title,
            actions = {
                AreButton(stringResource(R.string.action_cancel), onClick = onDismiss, variant = AreButtonVariant.Ghost)
                AreButton(confirmText, onClick = { onConfirm(); onDismiss() }, variant = AreButtonVariant.Primary)
            },
        ) {
            Text(text = message, style = AreIptvTheme.typography.body, color = AreIptvTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun MiniPlayerBehavior.label(): String = when (this) {
    MiniPlayerBehavior.DODGE -> stringResource(R.string.settings_mini_player_dodge)
    MiniPlayerBehavior.FADE -> stringResource(R.string.settings_mini_player_fade)
}

/**
 * Full-width accent-color picker: a labelled block with a row of preset swatches. Selecting one
 * persists immediately for the mode currently on screen and recolors the whole app live.
 */
@Composable
private fun AccentPickerBlock(selected: AccentPreset, isDark: Boolean, onSelect: (AccentPreset) -> Unit) {
    val colors = AreIptvTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(text = stringResource(R.string.settings_accent_color), style = AreIptvTheme.typography.label, color = colors.textPrimary)
        Box(Modifier.padding(top = 4.dp))
        Text(
            text = stringResource(if (isDark) R.string.settings_accent_color_desc_dark else R.string.settings_accent_color_desc_light),
            style = AreIptvTheme.typography.caption,
            color = colors.textTertiary,
        )
        Box(Modifier.padding(top = 16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccentPreset.entries.forEach { preset ->
                AccentSwatch(preset = preset, isDark = isDark, selected = preset == selected, onSelect = { onSelect(preset) })
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AccentSwatch(preset: AccentPreset, isDark: Boolean, selected: Boolean, onSelect: () -> Unit) {
    val colors = AreIptvTheme.colors
    val swatchColor = preset.swatch(isDark)
    val outerShape = RoundedCornerShape(AreIptvTheme.radius.md)
    val fillShape = RoundedCornerShape(AreIptvTheme.radius.sm)
    TvFocusable(onClick = onSelect, modifier = Modifier.size(52.dp), shape = outerShape) { focused, _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .background(swatchColor, fillShape)
                .border(
                    width = if (focused || selected) 2.dp else 1.dp,
                    color = if (focused || selected) colors.textPrimary else colors.borderDefault,
                    shape = fillShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = preset.tokens(isDark).accentFg, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// --- Provider account panels (read-only) ---

@Composable
private fun StalkerProviderPanel(info: com.arashrahimi46.iptv.data.parser.StalkerAccountInfo?) {
    val colors = AreIptvTheme.colors
    if (info == null) {
        Text(
            text = stringResource(R.string.settings_provider_loading),
            style = AreIptvTheme.typography.caption,
            color = colors.textTertiary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        )
        return
    }
    val dash = stringResource(R.string.settings_provider_unknown)
    Column {
        val status = info.status?.replaceFirstChar { it.uppercase() }
        val statusColor = when (status?.lowercase()) {
            "active" -> colors.success
            "expired", "banned", "disabled" -> colors.danger
            else -> colors.textPrimary
        }
        ProviderInfoRow(stringResource(R.string.settings_provider_status), status ?: dash, statusColor)
        ProviderInfoRow(stringResource(R.string.settings_provider_expires), info.expiresRaw ?: dash)
        info.host?.takeIf { it.isNotBlank() }?.let { ProviderInfoRow(stringResource(R.string.settings_provider_server), it) }
    }
}

@Composable
private fun ProviderPanel(info: XtreamAccountInfo?) {
    val colors = AreIptvTheme.colors
    if (info == null) {
        Text(
            text = stringResource(R.string.settings_provider_loading),
            style = AreIptvTheme.typography.caption,
            color = colors.textTertiary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        )
        return
    }
    val dash = stringResource(R.string.settings_provider_unknown)
    Column {
        val status = info.status?.replaceFirstChar { it.uppercase() }
        val statusColor = when (status?.lowercase()) {
            "active" -> colors.success
            "expired", "banned", "disabled" -> colors.danger
            else -> colors.textPrimary
        }
        ProviderInfoRow(stringResource(R.string.settings_provider_status), status ?: dash, statusColor)
        ProviderInfoRow(stringResource(R.string.settings_provider_expires), expiryText(info.expiresAtMs), expiryColor(info.expiresAtMs))
        if (info.isTrial) {
            ProviderInfoRow(stringResource(R.string.settings_provider_trial), stringResource(R.string.settings_provider_trial_value), colors.warning)
        }
        connectionsText(info.maxConnections, info.activeConnections)?.let {
            ProviderInfoRow(stringResource(R.string.settings_provider_connections), it)
        }
        info.timezone?.takeIf { it.isNotBlank() }?.let { ProviderInfoRow(stringResource(R.string.settings_provider_timezone), it) }
        info.serverTime?.takeIf { it.isNotBlank() }?.let { ProviderInfoRow(stringResource(R.string.settings_provider_server_time), it) }
        serverText(info.host, info.port)?.let { ProviderInfoRow(stringResource(R.string.settings_provider_server), it) }
    }
}

@Composable
private fun ProviderInfoRow(label: String, value: String, valueColor: Color? = null) {
    val colors = AreIptvTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = label, style = AreIptvTheme.typography.label, color = colors.textSecondary, modifier = Modifier.width(150.dp))
        Text(text = value, style = AreIptvTheme.typography.label, color = valueColor ?: colors.textPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun expiryText(expiresAtMs: Long?): String {
    if (expiresAtMs == null) return stringResource(R.string.settings_provider_unlimited)
    val date = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(expiresAtMs))
    val daysLeft = (expiresAtMs - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)
    return when {
        daysLeft < 0 -> stringResource(R.string.settings_provider_expired)
        daysLeft == 0L -> stringResource(R.string.settings_provider_expires_today, date)
        else -> stringResource(R.string.settings_provider_expires_days, date, daysLeft)
    }
}

@Composable
private fun expiryColor(expiresAtMs: Long?): Color {
    val colors = AreIptvTheme.colors
    if (expiresAtMs == null) return colors.textPrimary
    val daysLeft = (expiresAtMs - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)
    return when {
        daysLeft < 0 -> colors.danger
        daysLeft <= 7 -> colors.warning
        else -> colors.textPrimary
    }
}

@Composable
private fun connectionsText(max: Int?, active: Int?): String? = when {
    max != null && active != null -> stringResource(R.string.settings_provider_connections_value, active, max)
    max != null -> stringResource(R.string.settings_provider_connections_max, max)
    else -> null
}

private fun serverText(host: String?, port: String?): String? {
    val h = host?.takeIf { it.isNotBlank() } ?: return null
    return if (!port.isNullOrBlank()) "$h:$port" else h
}
