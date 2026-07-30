package com.arashrahimi46.iptv.mobile.ui.settings

import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.data.settings.AutoRefreshInterval
import com.arashrahimi46.iptv.data.settings.AutoRelock
import com.arashrahimi46.iptv.data.settings.LockedContentDisplay
import com.arashrahimi46.iptv.data.settings.StartScreen
import com.arashrahimi46.iptv.data.settings.SubtitleColorChoice
import com.arashrahimi46.iptv.data.settings.SubtitleEdge
import com.arashrahimi46.iptv.data.settings.SubtitleFontChoice
import com.arashrahimi46.iptv.data.settings.SubtitleTextScale
import com.arashrahimi46.iptv.data.settings.ThemeMode
import com.arashrahimi46.iptv.mobile.BuildConfig
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreLanguageSelector
import com.arashrahimi46.iptv.ui.components.AreLanguageOptions
import com.arashrahimi46.iptv.ui.components.AreSegmentedControl
import com.arashrahimi46.iptv.ui.components.AreSwitch
import com.arashrahimi46.iptv.core.R as CoreR
import java.util.Locale

/** Real phone Settings: simple scrollable panes behind a touch [TabRow] -- no TV sidebar layout,
 * no D-pad focus. Scope mirrors :tv's `SettingsPanes.kt` General/Playback/Subtitles/Parental tabs
 * (see [SettingsViewModel]'s doc for what was deliberately left out). */
@Composable
fun SettingsScreen(
    onOpenFavorites: () -> Unit = {},
    onOpenRecordings: () -> Unit = {},
    onOpenStreams: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val colors = AreIptvMobileTheme.colors
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf(
        stringResource(R.string.settings_tab_general),
        stringResource(R.string.settings_tab_playback),
        stringResource(R.string.settings_tab_subtitles),
        stringResource(R.string.settings_tab_parental),
        stringResource(CoreR.string.settings_tab_about),
    )

    Column(Modifier.fillMaxSize()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.nav_favorites)) },
            leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null, tint = colors.accent) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary) },
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFavorites),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.nav_recordings)) },
            leadingContent = { Icon(Icons.Filled.Videocam, contentDescription = null, tint = colors.accent) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary) },
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenRecordings),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.nav_streams)) },
            leadingContent = { Icon(Icons.Filled.Wifi, contentDescription = null, tint = colors.accent) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary) },
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenStreams),
        )
        AreSegmentedControl(
            options = titles.indices.toList(),
            selected = tab,
            label = { titles[it] },
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        when (tab) {
            0 -> GeneralPane(viewModel)
            1 -> PlaybackPane(viewModel)
            2 -> SubtitlesPane(viewModel)
            3 -> ParentalPane(viewModel)
            4 -> AboutPane(viewModel)
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = AreIptvMobileTheme.typography.caption,
        color = AreIptvMobileTheme.colors.textTertiary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsSwitchRow(title: String, desc: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = desc?.let { { Text(it) } },
        trailingContent = { AreSwitch(checked = checked, onCheckedChange = onCheckedChange, disabled = !enabled) },
    )
}

@Composable
private fun <T> SettingsChoiceRow(title: String, desc: String?, options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                desc?.let { Text(it) }
                // Horizontally scrollable so each chip keeps its natural (unconstrained) label width --
                // a plain Row here squeezed chips to fit all options on one line, wrapping labels like
                // "Spanish" letter-by-letter once there were more options than would fit.
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { option ->
                        AreChip(text = label(option), selected = option == selected, onClick = { onSelect(option) })
                    }
                }
            }
        },
    )
}

// =============================================================================================
// GENERAL -- catalog refresh (backs EPG data too) + auto-refresh interval
// =============================================================================================

@Composable
private fun GeneralPane(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val activeSource by viewModel.activeSource.collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()
    val staleWindowDays by viewModel.staleWindowDays.collectAsState()
    val autoRefreshInterval by viewModel.autoRefreshInterval.collectAsState()
    val startScreen by viewModel.startScreen.collectAsState()
    val confirmBeforeExit by viewModel.confirmBeforeExit.collectAsState()
    val providerInfo by viewModel.providerInfo.collectAsState()
    val stalkerInfo by viewModel.stalkerInfo.collectAsState()
    val languageTag by viewModel.languageTag.collectAsState()

    val refreshingText = stringResource(R.string.settings_refreshing)
    val neverRefreshedText = stringResource(R.string.settings_never_refreshed)

    var showLanguagePicker by remember { mutableStateOf(false) }
    var pendingLanguage by remember { mutableStateOf<String?>(null) }
    var applyLanguage by remember { mutableStateOf<String?>(null) }
    var generalConfirm by remember { mutableStateOf<GeneralConfirm?>(null) }

    // Same two-frame delay :tv's General pane uses before calling setApplicationLocales -- doing it
    // while the confirm dialog's own window is still tearing down can strand the recreated Activity
    // with no focused window (reproduced on-device; see tv's SettingsPanes.kt for the full story).
    LaunchedEffect(applyLanguage) {
        val tag = applyLanguage ?: return@LaunchedEffect
        viewModel.setLanguageTag(tag)
        androidx.compose.runtime.withFrameNanos { }
        androidx.compose.runtime.withFrameNanos { }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            SettingsSectionTitle(stringResource(R.string.settings_section_appearance))
            SettingsChoiceRow(
                title = stringResource(R.string.settings_theme_title),
                desc = stringResource(R.string.settings_theme_desc),
                options = ThemeMode.entries,
                selected = themeMode,
                label = { stringResource(it.labelRes()) },
                onSelect = viewModel::setThemeMode,
            )
            SettingsSectionTitle(stringResource(R.string.settings_section_playlists))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_refresh_catalog)) },
                supportingContent = {
                    Text(
                        when (val r = refreshState) {
                            is MobileRefreshState.Success -> stringResource(R.string.settings_refresh_success, r.channels, r.movies, r.series)
                            is MobileRefreshState.Error -> r.message
                            else -> lastUpdatedLabel(activeSource?.lastRefreshedAtMs, neverRefreshedText)
                        },
                    )
                },
                trailingContent = {
                    AreButton(
                        text = if (refreshState is MobileRefreshState.Refreshing) refreshingText else stringResource(R.string.settings_refresh_now),
                        onClick = viewModel::refresh,
                        size = AreButtonSize.Small,
                        disabled = refreshState is MobileRefreshState.Refreshing || activeSource == null,
                    )
                },
            )
            SettingsChoiceRow(
                title = stringResource(R.string.settings_stale_title),
                desc = stringResource(R.string.settings_stale_desc),
                options = StaleWindowOptions,
                selected = StaleWindowOptions.first { it.first == staleWindowDays },
                label = { stringResource(it.second) },
                onSelect = { viewModel.setStaleWindowDays(it.first) },
            )

            SettingsSectionTitle(stringResource(CoreR.string.settings_section_preferences))
            SettingsChoiceRow(
                title = stringResource(CoreR.string.settings_start_screen_title),
                desc = stringResource(CoreR.string.settings_start_screen_desc),
                options = StartScreen.entries,
                selected = startScreen,
                label = { stringResource(it.labelRes()) },
                onSelect = { viewModel.setStartScreen(it) },
            )
            SettingsChoiceRow(
                title = stringResource(R.string.settings_auto_refresh_title),
                desc = stringResource(R.string.settings_auto_refresh_desc),
                options = AutoRefreshInterval.entries,
                selected = autoRefreshInterval,
                label = { stringResource(it.labelRes()) },
                onSelect = { viewModel.setAutoRefreshInterval(it) },
            )
            SettingsSwitchRow(
                title = stringResource(CoreR.string.settings_confirm_exit_title),
                desc = stringResource(CoreR.string.settings_confirm_exit_desc),
                checked = confirmBeforeExit,
                onCheckedChange = { viewModel.setConfirmBeforeExit(it) },
            )

            // Provider account panel -- Xtream/Stalker carry account metadata; M3U playlists don't.
            if (activeSource?.type == SourceType.XTREAM) {
                SettingsSectionTitle(stringResource(CoreR.string.settings_section_provider))
                ProviderInfoPanel(
                    status = providerInfo?.status,
                    expiresText = providerInfo?.let { expiryText(it.expiresAtMs) },
                    server = providerInfo?.let { serverText(it.host, it.port) },
                )
            }
            if (activeSource?.type == SourceType.STALKER) {
                SettingsSectionTitle(stringResource(CoreR.string.settings_section_provider))
                ProviderInfoPanel(
                    status = stalkerInfo?.status,
                    expiresText = stalkerInfo?.expiresRaw,
                    server = stalkerInfo?.host?.takeIf { it.isNotBlank() },
                )
            }

            SettingsSectionTitle(stringResource(CoreR.string.settings_section_language))
            val effectiveTag = AppCompatDelegate.getApplicationLocales().takeUnless { it.isEmpty }?.get(0)?.toLanguageTag() ?: languageTag
            ListItem(
                headlineContent = { Text(stringResource(CoreR.string.settings_language_row_title)) },
                supportingContent = { Text(stringResource(CoreR.string.settings_language_row_desc)) },
                leadingContent = { Icon(Icons.Filled.Language, contentDescription = null) },
                trailingContent = {
                    AreButton(
                        text = AreLanguageOptions.firstOrNull { it.tag.equals(effectiveTag, ignoreCase = true) }
                            ?.let { stringResource(it.nativeNameRes) }
                            ?: AreLanguageOptions.firstOrNull { it.tag.substringBefore('-').equals(effectiveTag.substringBefore('-'), ignoreCase = true) }
                                ?.let { stringResource(it.nativeNameRes) }
                            ?: effectiveTag,
                        onClick = { showLanguagePicker = true },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                },
            )

            SettingsSectionTitle(stringResource(CoreR.string.settings_section_storage))
            ListItem(
                headlineContent = { Text(stringResource(CoreR.string.settings_clear_cache_title)) },
                supportingContent = { Text(stringResource(CoreR.string.settings_clear_cache_desc)) },
                trailingContent = {
                    AreButton(
                        text = stringResource(CoreR.string.action_clear),
                        onClick = { generalConfirm = GeneralConfirm.ClearCache },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(CoreR.string.settings_clear_history_title)) },
                supportingContent = { Text(stringResource(CoreR.string.settings_clear_history_desc)) },
                trailingContent = {
                    AreButton(
                        text = stringResource(CoreR.string.action_clear),
                        onClick = { generalConfirm = GeneralConfirm.ClearHistory },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(CoreR.string.settings_reset_title)) },
                supportingContent = { Text(stringResource(CoreR.string.settings_reset_desc)) },
                trailingContent = {
                    AreButton(
                        text = stringResource(CoreR.string.settings_reset_action),
                        onClick = { generalConfirm = GeneralConfirm.Reset },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                },
            )
        }
    }

    if (showLanguagePicker) {
        Dialog(onDismissRequest = { showLanguagePicker = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            AreDialog(onDismiss = { showLanguagePicker = false }, title = stringResource(CoreR.string.settings_language_row_title)) {
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
                            pendingLanguage = null
                            applyLanguage = targetLanguage
                        },
                        variant = AreButtonVariant.Primary,
                    )
                },
            ) {}
        }
    }

    when (generalConfirm) {
        GeneralConfirm.ClearCache -> ConfirmActionDialog(
            title = stringResource(CoreR.string.settings_clear_cache_title),
            message = stringResource(CoreR.string.settings_clear_cache_confirm),
            confirmText = stringResource(CoreR.string.action_clear),
            onConfirm = { viewModel.clearImageCache() },
            onDismiss = { generalConfirm = null },
        )
        GeneralConfirm.ClearHistory -> ConfirmActionDialog(
            title = stringResource(CoreR.string.settings_clear_history_title),
            message = stringResource(CoreR.string.settings_clear_history_confirm),
            confirmText = stringResource(CoreR.string.action_clear),
            onConfirm = { viewModel.clearHistory() },
            onDismiss = { generalConfirm = null },
        )
        GeneralConfirm.Reset -> ConfirmActionDialog(
            title = stringResource(CoreR.string.settings_reset_title),
            message = stringResource(CoreR.string.settings_reset_confirm),
            confirmText = stringResource(CoreR.string.settings_reset_action),
            onConfirm = { viewModel.resetToDefaults() },
            onDismiss = { generalConfirm = null },
        )
        null -> Unit
    }
}

private enum class GeneralConfirm { ClearCache, ClearHistory, Reset }

/** Touch confirm dialog for a destructive/one-shot action -- tap-outside-to-dismiss via [AreDialog]'s
 * real [Dialog] window, same pattern as every other mobile dialog. */
@Composable
private fun ConfirmActionDialog(title: String, message: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(
            onDismiss = onDismiss,
            title = title,
            actions = {
                AreButton(stringResource(CoreR.string.action_cancel), onClick = onDismiss, variant = AreButtonVariant.Ghost)
                AreButton(confirmText, onClick = { onConfirm(); onDismiss() }, variant = AreButtonVariant.Primary)
            },
        ) {
            Text(text = message, style = AreIptvMobileTheme.typography.body, color = AreIptvMobileTheme.colors.textSecondary)
        }
    }
}

/** Read-only provider account rows (status/expiry/server) shared by the Xtream and Stalker branches. */
@Composable
private fun ProviderInfoPanel(status: String?, expiresText: String?, server: String?) {
    val colors = AreIptvMobileTheme.colors
    if (status == null && expiresText == null && server == null) {
        Text(
            text = stringResource(CoreR.string.settings_provider_loading),
            style = AreIptvMobileTheme.typography.caption,
            color = colors.textTertiary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        return
    }
    val dash = stringResource(CoreR.string.settings_provider_unknown)
    status?.let { ProviderInfoRow(stringResource(CoreR.string.settings_provider_status), it.replaceFirstChar { c -> c.uppercase() }) }
    ProviderInfoRow(stringResource(CoreR.string.settings_provider_expires), expiresText ?: dash)
    server?.let { ProviderInfoRow(stringResource(CoreR.string.settings_provider_server), it) }
}

@Composable
private fun ProviderInfoRow(label: String, value: String) {
    val colors = AreIptvMobileTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = label, style = AreIptvMobileTheme.typography.label, color = colors.textSecondary, modifier = Modifier.weight(1f))
        Text(text = value, style = AreIptvMobileTheme.typography.label, color = colors.textPrimary, modifier = Modifier.weight(1f))
    }
}

private fun expiryText(expiresAtMs: Long?): String? {
    if (expiresAtMs == null) return null
    val date = java.text.SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(java.util.Date(expiresAtMs))
    val daysLeft = (expiresAtMs - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)
    return "$date · ${daysLeft}d"
}

private fun serverText(host: String?, port: String?): String? {
    val h = host?.takeIf { it.isNotBlank() } ?: return null
    return if (!port.isNullOrBlank()) "$h:$port" else h
}

private fun StartScreen.labelRes(): Int = when (this) {
    StartScreen.HOME -> CoreR.string.nav_home
    StartScreen.LIVE -> CoreR.string.nav_live_tv
    StartScreen.MOVIES -> CoreR.string.nav_movies
    StartScreen.SERIES -> CoreR.string.nav_series
    StartScreen.LAST_USED -> CoreR.string.settings_start_last_used
}

/** Title + Confirm/Cancel labels for the language-switch confirmation modal, phrased in the TARGET
 * language being switched to -- mirrors :tv's `localizedConfirmCopy` (tv-module-only, so duplicated
 * here rather than shared) via the same [CoreR] string keys. */
private data class LanguageConfirmCopy(val title: String, val confirm: String, val cancel: String)

private fun localizedConfirmCopy(context: android.content.Context, languageTag: String): LanguageConfirmCopy {
    val locale = Locale.forLanguageTag(languageTag)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    val localizedContext = context.createConfigurationContext(config)
    return LanguageConfirmCopy(
        title = localizedContext.getString(CoreR.string.language_confirm_title),
        confirm = localizedContext.getString(CoreR.string.language_confirm_action),
        cancel = localizedContext.getString(CoreR.string.language_confirm_cancel),
    )
}

/** "N days/hours/mins ago" label for the last catalog refresh; [neverRefreshedText] when unset.
 * Mirrors :tv's `SettingsScreen.kt` helper of the same name. */
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

private val StaleWindowOptions = listOf(
    7L to R.string.settings_stale_7d,
    14L to R.string.settings_stale_14d,
    30L to R.string.settings_stale_30d,
    0L to R.string.settings_stale_off,
)

private fun AutoRefreshInterval.labelRes(): Int = when (this) {
    AutoRefreshInterval.OFF -> R.string.settings_auto_refresh_off
    AutoRefreshInterval.DAILY -> R.string.settings_auto_refresh_daily
    AutoRefreshInterval.WEEKLY -> R.string.settings_auto_refresh_weekly
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.DARK -> R.string.settings_theme_dark
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.SYSTEM -> R.string.settings_theme_system
}

// =============================================================================================
// PLAYBACK -- hardware decoding, preferred audio language, autoplay-next
// =============================================================================================

@Composable
private fun PlaybackPane(viewModel: SettingsViewModel) {
    val isHardwareDecoding by viewModel.isHardwareDecoding.collectAsState()
    val autoplayNextDelay by viewModel.autoplayNextDelaySeconds.collectAsState()
    val preferredAudioLang by viewModel.preferredAudioLanguage.collectAsState()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            SettingsSectionTitle(stringResource(R.string.settings_section_playback))
            SettingsSwitchRow(
                title = stringResource(R.string.settings_hardware_decoding),
                desc = stringResource(R.string.settings_hardware_decoding_desc),
                checked = isHardwareDecoding,
                onCheckedChange = viewModel::setHardwareDecoding,
            )
            SettingsChoiceRow(
                title = stringResource(R.string.settings_preferred_audio_title),
                desc = stringResource(R.string.settings_preferred_audio_desc),
                options = listOf("" to stringResource(R.string.settings_audio_auto)) + SUBTITLE_LANGUAGES,
                selected = ("" to stringResource(R.string.settings_audio_auto)).let { auto ->
                    SUBTITLE_LANGUAGES.firstOrNull { it.first == preferredAudioLang } ?: auto
                },
                label = { it.second },
                onSelect = { viewModel.setPreferredAudioLanguage(it.first) },
            )
            SettingsChoiceRow(
                title = stringResource(R.string.settings_autoplay_next),
                desc = stringResource(R.string.settings_autoplay_next_desc),
                options = AutoplayDelayOptions,
                selected = AutoplayDelayOptions.first { it.first == autoplayNextDelay },
                label = { stringResource(it.second) },
                onSelect = { viewModel.setAutoplayNextDelaySeconds(it.first) },
            )
        }
    }
}

private val AutoplayDelayOptions = listOf(
    0L to R.string.settings_autoplay_off,
    5L to R.string.settings_autoplay_5s,
    10L to R.string.settings_autoplay_10s,
)

/** Same fixed language list :tv's Settings uses (see `ui/player/SubtitleMenu.kt`'s
 * SUBTITLE_LANGUAGES) -- duplicated rather than shared since it's not exposed from :core. */
private val SUBTITLE_LANGUAGES: List<Pair<String, String>> = listOf(
    "en" to "English", "fa" to "Persian", "ar" to "Arabic", "fr" to "French",
    "es" to "Spanish", "de" to "German", "it" to "Italian", "tr" to "Turkish",
    "ru" to "Russian", "pt" to "Portuguese", "nl" to "Dutch", "hi" to "Hindi",
)

// =============================================================================================
// SUBTITLES -- language, size, edge style, color, font
// =============================================================================================

@Composable
private fun SubtitlesPane(viewModel: SettingsViewModel) {
    val subtitleLanguage by viewModel.subtitleLanguage.collectAsState()
    val subtitleTextScale by viewModel.subtitleTextScale.collectAsState()
    val subtitleEdge by viewModel.subtitleEdge.collectAsState()
    val subtitleColor by viewModel.subtitleColor.collectAsState()
    val subtitleFont by viewModel.subtitleFont.collectAsState()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            SettingsSectionTitle(stringResource(R.string.settings_section_subtitles))
            SettingsChoiceRow(
                title = stringResource(R.string.settings_subtitle_lang_title),
                desc = stringResource(R.string.settings_subtitle_lang_desc),
                options = SUBTITLE_LANGUAGES,
                selected = SUBTITLE_LANGUAGES.firstOrNull { it.first == subtitleLanguage } ?: SUBTITLE_LANGUAGES.first(),
                label = { it.second },
                onSelect = { viewModel.setSubtitleLanguage(it.first) },
            )
            SettingsSectionTitle(stringResource(R.string.settings_section_subtitle_appearance))
            SettingsChoiceRow(
                title = stringResource(R.string.settings_sub_size_title),
                desc = stringResource(R.string.settings_sub_size_desc),
                options = SubtitleTextScale.entries,
                selected = subtitleTextScale,
                label = { stringResource(it.labelRes()) },
                onSelect = { viewModel.setSubtitleTextScale(it) },
            )
            SettingsChoiceRow(
                title = stringResource(R.string.settings_sub_style_title),
                desc = stringResource(R.string.settings_sub_style_desc),
                options = SubtitleEdge.entries,
                selected = subtitleEdge,
                label = { stringResource(it.labelRes()) },
                onSelect = { viewModel.setSubtitleEdge(it) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_sub_color_title)) },
                supportingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        SubtitleColorChoice.entries.forEach { choice ->
                            AreChip(
                                text = choice.name,
                                selected = choice == subtitleColor,
                                icon = if (choice == subtitleColor) Icons.Filled.Check else null,
                                onClick = { viewModel.setSubtitleColor(choice) },
                            )
                        }
                    }
                },
            )
            SettingsChoiceRow(
                title = stringResource(R.string.settings_sub_font_title),
                desc = stringResource(R.string.settings_sub_font_desc),
                options = SubtitleFontChoice.entries,
                selected = subtitleFont,
                label = { stringResource(it.labelRes()) },
                onSelect = { viewModel.setSubtitleFont(it) },
            )
        }
    }
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

// =============================================================================================
// PARENTAL -- lock toggle, PIN, auto-relock, locked-content display, PIN-on-launch
// =============================================================================================

@Composable
private fun ParentalPane(viewModel: SettingsViewModel) {
    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    val hasPinSet by viewModel.hasPinSet.collectAsState()
    val pinLoaded = hasPinSet != null
    val hasPin = hasPinSet == true
    var pinDialog by remember { mutableStateOf<PinFlow?>(null) }
    val autoRelock by viewModel.parentalAutoRelock.collectAsState()
    val lockedDisplay by viewModel.lockedContentDisplay.collectAsState()
    val pinOnLaunch by viewModel.isPinOnLaunch.collectAsState()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            SettingsSectionTitle(stringResource(R.string.settings_section_parental))
            SettingsSwitchRow(
                title = stringResource(R.string.settings_lock_adult),
                desc = stringResource(R.string.settings_lock_adult_desc),
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
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_change_pin)) },
                supportingContent = {
                    Text(
                        when {
                            !pinLoaded -> stringResource(R.string.settings_pin_loading)
                            hasPin -> stringResource(R.string.settings_pin_is_set)
                            else -> stringResource(R.string.settings_pin_not_set)
                        },
                    )
                },
                trailingContent = {
                    AreButton(
                        text = if (hasPin) stringResource(R.string.settings_change) else stringResource(R.string.settings_set_pin),
                        onClick = { pinDialog = if (hasPin) PinFlow.VerifyThenChange else PinFlow.SetOnly },
                        size = AreButtonSize.Small,
                        disabled = !pinLoaded,
                    )
                },
            )
            SettingsSectionTitle(stringResource(R.string.settings_section_content_locking))
            SettingsChoiceRow(
                title = stringResource(R.string.settings_relock_title),
                desc = stringResource(R.string.settings_relock_desc),
                options = AutoRelock.entries,
                selected = autoRelock,
                label = { stringResource(it.labelRes()) },
                onSelect = { viewModel.setParentalAutoRelock(it) },
            )
            SettingsChoiceRow(
                title = stringResource(R.string.settings_locked_display_title),
                desc = stringResource(R.string.settings_locked_display_desc),
                options = LockedContentDisplay.entries,
                selected = lockedDisplay,
                label = { stringResource(it.labelRes()) },
                onSelect = { viewModel.setLockedContentDisplay(it) },
            )
            SettingsSwitchRow(
                title = stringResource(R.string.settings_pin_launch_title),
                desc = stringResource(R.string.settings_pin_launch_desc),
                checked = pinOnLaunch,
                enabled = hasPin,
                onCheckedChange = { viewModel.setPinOnLaunch(it) },
            )
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

private enum class PinFlow { SetThenEnable, SetOnly, VerifyThenDisable, VerifyThenChange }

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

// =============================================================================================
// ABOUT -- version, support, feedback, analytics/crash opt-out, legal. :tv's About pane
// (SettingsPanes.kt) additionally has a "What's new" changelog dialog and a QR-code feedback
// flow -- both exist there specifically because a TV has no browser/keyboard; a phone does, so
// Support/Feedback here just hand off to the same URLs via a normal browser Intent instead of
// reimplementing a star-rating form or a QR panel. Version/Analytics/Crash-reporting/Legal are
// full parity with :tv, reading and writing the same shared UserSettings keys.
// =============================================================================================

private const val SUPPORT_URL = "https://buymeacoffee.com/arashrahimi46"
private const val FEEDBACK_FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSdxmmP_5GBRVY3gVWMQh4a_W4DrVfLy-vaxfBkPM29N94Cr-A/viewform"

@Composable
private fun AboutPane(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val isAnalyticsEnabled by viewModel.isAnalyticsEnabled.collectAsState()
    val isCrashReportingEnabled by viewModel.isCrashReportingEnabled.collectAsState()
    var showLegal by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            SettingsSectionTitle(stringResource(CoreR.string.settings_section_about))
            ListItem(
                headlineContent = { Text(stringResource(CoreR.string.settings_version_title)) },
                supportingContent = { Text(stringResource(CoreR.string.settings_version_value, BuildConfig.VERSION_NAME)) },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
            )
            ListItem(
                headlineContent = { Text(stringResource(CoreR.string.settings_support_title)) },
                supportingContent = { Text(stringResource(CoreR.string.settings_support_desc)) },
                leadingContent = { Icon(Icons.Filled.VolunteerActivism, contentDescription = null) },
                trailingContent = {
                    AreButton(
                        text = stringResource(CoreR.string.action_buy_coffee),
                        onClick = { openUrl(SUPPORT_URL) },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(CoreR.string.settings_feedback_title)) },
                supportingContent = { Text(stringResource(CoreR.string.settings_feedback_desc)) },
                leadingContent = { Icon(Icons.Filled.Feedback, contentDescription = null) },
                trailingContent = {
                    AreButton(
                        text = stringResource(CoreR.string.settings_feedback_action),
                        onClick = { openUrl(FEEDBACK_FORM_URL) },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                },
            )
            SettingsSwitchRow(
                title = stringResource(CoreR.string.settings_analytics_title),
                desc = stringResource(CoreR.string.settings_analytics_desc),
                checked = isAnalyticsEnabled,
                onCheckedChange = viewModel::setAnalyticsEnabled,
            )
            SettingsSwitchRow(
                title = stringResource(CoreR.string.settings_crash_title),
                desc = stringResource(CoreR.string.settings_crash_desc),
                checked = isCrashReportingEnabled,
                onCheckedChange = viewModel::setCrashReportingEnabled,
            )
            ListItem(
                headlineContent = { Text(stringResource(CoreR.string.settings_legal_title)) },
                supportingContent = { Text(stringResource(CoreR.string.settings_legal_desc)) },
                leadingContent = { Icon(Icons.Filled.Policy, contentDescription = null) },
                trailingContent = {
                    AreButton(
                        text = stringResource(CoreR.string.settings_legal_action),
                        onClick = { showLegal = true },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Small,
                    )
                },
            )
        }
    }

    if (showLegal) LegalDocumentDialog(onDismiss = { showLegal = false })
}

/** Same 15-clause document as :tv's `PrivacyTermsScreen.kt` `LegalDocumentDialog` (same
 * [CoreR] string keys, English-only by design -- see that file's comment on `legal_doc_title`),
 * rebuilt on plain touch scroll instead of D-pad focus-driven scroll since :mobile can't depend
 * on :tv's package to reuse the composable directly. */
@Composable
private fun LegalDocumentDialog(onDismiss: () -> Unit) {
    val sections = listOf(
        CoreR.string.legal_s1_title to CoreR.string.legal_s1_body,
        CoreR.string.legal_s2_title to CoreR.string.legal_s2_body,
        CoreR.string.legal_s3_title to CoreR.string.legal_s3_body,
        CoreR.string.legal_s4_title to CoreR.string.legal_s4_body,
        CoreR.string.legal_s5_title to CoreR.string.legal_s5_body,
        CoreR.string.legal_s6_title to CoreR.string.legal_s6_body,
        CoreR.string.legal_s7_title to CoreR.string.legal_s7_body,
        CoreR.string.legal_s8_title to CoreR.string.legal_s8_body,
        CoreR.string.legal_s9_title to CoreR.string.legal_s9_body,
        CoreR.string.legal_s10_title to CoreR.string.legal_s10_body,
        CoreR.string.legal_s11_title to CoreR.string.legal_s11_body,
        CoreR.string.legal_s12_title to CoreR.string.legal_s12_body,
        CoreR.string.legal_s13_title to CoreR.string.legal_s13_body,
        CoreR.string.legal_s14_title to CoreR.string.legal_s14_body,
        CoreR.string.legal_s15_title to CoreR.string.legal_s15_body,
    )
    val colors = AreIptvMobileTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(
            onDismiss = onDismiss,
            title = stringResource(CoreR.string.legal_doc_title),
            actions = { AreButton(stringResource(CoreR.string.action_close), onClick = onDismiss) },
        ) {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
            ) {
                sections.forEach { (titleRes, bodyRes) ->
                    Text(
                        text = stringResource(titleRes),
                        style = AreIptvMobileTheme.typography.label,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    Text(
                        text = stringResource(bodyRes),
                        style = AreIptvMobileTheme.typography.body,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}
