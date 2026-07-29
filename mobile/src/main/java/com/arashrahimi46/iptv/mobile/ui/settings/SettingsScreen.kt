package com.arashrahimi46.iptv.mobile.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.data.settings.AutoRefreshInterval
import com.arashrahimi46.iptv.data.settings.AutoRelock
import com.arashrahimi46.iptv.data.settings.LockedContentDisplay
import com.arashrahimi46.iptv.data.settings.SubtitleColorChoice
import com.arashrahimi46.iptv.data.settings.SubtitleEdge
import com.arashrahimi46.iptv.data.settings.SubtitleFontChoice
import com.arashrahimi46.iptv.data.settings.SubtitleTextScale
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme

/** Real phone Settings: simple scrollable panes behind a touch [TabRow] -- no TV sidebar layout,
 * no D-pad focus. Scope mirrors :tv's `SettingsPanes.kt` General/Playback/Subtitles/Parental tabs
 * (see [SettingsViewModel]'s doc for what was deliberately left out). */
@Composable
fun SettingsScreen(onOpenFavorites: () -> Unit = {}, viewModel: SettingsViewModel = viewModel()) {
    val colors = AreIptvMobileTheme.colors
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf(
        stringResource(R.string.settings_tab_general),
        stringResource(R.string.settings_tab_playback),
        stringResource(R.string.settings_tab_subtitles),
        stringResource(R.string.settings_tab_parental),
    )

    Column(Modifier.fillMaxSize()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.nav_favorites)) },
            leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null, tint = colors.accent) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary) },
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFavorites),
        )
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> GeneralPane(viewModel)
            1 -> PlaybackPane(viewModel)
            2 -> SubtitlesPane(viewModel)
            3 -> ParentalPane(viewModel)
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = AreIptvMobileTheme.colors.textTertiary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsSwitchRow(title: String, desc: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = desc?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
    )
}

@Composable
private fun <T> SettingsChoiceRow(title: String, desc: String?, options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                desc?.let { Text(it) }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { option ->
                        FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
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
    val activeSource by viewModel.activeSource.collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()
    val staleWindowDays by viewModel.staleWindowDays.collectAsState()
    val autoRefreshInterval by viewModel.autoRefreshInterval.collectAsState()

    val refreshingText = stringResource(R.string.settings_refreshing)
    val neverRefreshedText = stringResource(R.string.settings_never_refreshed)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
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
                    Button(onClick = viewModel::refresh, enabled = refreshState !is MobileRefreshState.Refreshing && activeSource != null) {
                        Text(if (refreshState is MobileRefreshState.Refreshing) refreshingText else stringResource(R.string.settings_refresh_now))
                    }
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
            SettingsChoiceRow(
                title = stringResource(R.string.settings_auto_refresh_title),
                desc = stringResource(R.string.settings_auto_refresh_desc),
                options = AutoRefreshInterval.entries,
                selected = autoRefreshInterval,
                label = { stringResource(it.labelRes()) },
                onSelect = { viewModel.setAutoRefreshInterval(it) },
            )
        }
    }
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
                            AssistChip(
                                onClick = { viewModel.setSubtitleColor(choice) },
                                label = { Text(choice.name) },
                                leadingIcon = if (choice == subtitleColor) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else {
                                    null
                                },
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
                    Button(onClick = { pinDialog = if (hasPin) PinFlow.VerifyThenChange else PinFlow.SetOnly }, enabled = pinLoaded) {
                        Text(if (hasPin) stringResource(R.string.settings_change) else stringResource(R.string.settings_set_pin))
                    }
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
