package com.arashrahimi46.iptv.mobile.ui.settings

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.data.parser.StalkerAccountInfo
import com.arashrahimi46.iptv.data.parser.XtreamAccountInfo
import com.arashrahimi46.iptv.data.settings.AutoRefreshInterval
import com.arashrahimi46.iptv.data.settings.StartScreen
import com.arashrahimi46.iptv.data.settings.ThemeMode
import com.arashrahimi46.iptv.mobile.ui.components.AreAlertDialog
import com.arashrahimi46.iptv.mobile.ui.components.AreBottomSheet
import com.arashrahimi46.iptv.mobile.ui.components.AreChoiceRow
import com.arashrahimi46.iptv.mobile.ui.components.AreLanguageList
import com.arashrahimi46.iptv.mobile.ui.components.AreLanguageOptions
import com.arashrahimi46.iptv.mobile.ui.components.AreListRow
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSectionHeader
import com.arashrahimi46.iptv.mobile.ui.components.AreSwitchRow
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.core.R as CoreR
import java.util.Locale

/**
 * Settings root. The old 5-pane segmented control is gone: this is one `LazyColumn` of
 * [AreListRow]/[AreSwitchRow]/[AreChoiceRow] sections, and Playback / Subtitles / Parental / About
 * are real sub-screens with their own back stack entry (§4.10).
 *
 * Every choice that used to be a horizontally-scrolling chip strip is now an [AreChoiceRow] whose
 * supporting line is the current value and whose tap opens a bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPlayback: () -> Unit = {},
    onOpenSubtitles: () -> Unit = {},
    onOpenParental: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
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

    val neverRefreshedText = stringResource(CoreR.string.settings_never_refreshed)

    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    var pendingLanguage by rememberSaveable { mutableStateOf<String?>(null) }
    var applyLanguage by rememberSaveable { mutableStateOf<String?>(null) }
    var storageConfirm by rememberSaveable { mutableStateOf<StorageAction?>(null) }

    // Same two-frame delay :tv's General pane uses before calling setApplicationLocales -- doing it
    // while the confirm dialog's own window is still tearing down can strand the recreated Activity
    // with no focused window (reproduced on-device; see tv's SettingsPanes.kt for the full story).
    LaunchedEffect(applyLanguage) {
        val tag = applyLanguage ?: return@LaunchedEffect
        viewModel.setLanguageTag(tag)
        withFrameNanos { }
        withFrameNanos { }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    val effectiveTag = AppCompatDelegate.getApplicationLocales().takeUnless { it.isEmpty }
        ?.get(0)?.toLanguageTag() ?: languageTag
    val currentLanguageName = AreLanguageOptions
        .firstOrNull { it.tag.equals(effectiveTag, ignoreCase = true) }
        ?: AreLanguageOptions.firstOrNull {
            it.tag.substringBefore('-').equals(effectiveTag.substringBefore('-'), ignoreCase = true)
        }

    AreScreenScaffold(title = stringResource(CoreR.string.nav_settings)) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            // The Library section (Favorites / Recordings / Streams) moved to Home's quick-action
            // cards -- they are destinations you browse to, not preferences, and three taps deep
            // in a settings list was the wrong place for them.

            // ---- General -------------------------------------------------------------------
            item(contentType = "header") { AreSectionHeader(stringResource(CoreR.string.settings_section_general)) }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_theme_title),
                    options = ThemeMode.entries,
                    selected = themeMode,
                    label = { stringResource(it.labelRes()) },
                    onSelect = viewModel::setThemeMode,
                )
            }
            item(contentType = "row") {
                val refreshing = refreshState is MobileRefreshState.Refreshing
                val spinner: (@Composable () -> Unit)? = if (!refreshing) null else {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = AreIptvTheme.colors.accent,
                        )
                    }
                }
                AreListRow(
                    title = stringResource(CoreR.string.settings_refresh_catalog),
                    supporting = when (val r = refreshState) {
                        is MobileRefreshState.Success ->
                            stringResource(CoreR.string.settings_refresh_success, r.channels, r.movies, r.series)
                        is MobileRefreshState.Error -> r.message
                        is MobileRefreshState.Refreshing -> stringResource(CoreR.string.settings_refreshing)
                        else -> lastUpdatedLabel(activeSource?.lastRefreshedAtMs, neverRefreshedText)
                    },
                    enabled = !refreshing && activeSource != null,
                    onClick = viewModel::refresh,
                    trailing = spinner,
                )
            }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_stale_title),
                    options = StaleWindowOptions,
                    selected = StaleWindowOptions.firstOrNull { it == staleWindowDays },
                    label = { stringResource(staleWindowLabelRes(it)) },
                    onSelect = viewModel::setStaleWindowDays,
                )
            }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_start_screen_title),
                    options = StartScreen.entries,
                    selected = startScreen,
                    label = { stringResource(it.labelRes()) },
                    onSelect = viewModel::setStartScreen,
                )
            }
            item(contentType = "row") {
                AreChoiceRow(
                    title = stringResource(CoreR.string.settings_auto_refresh_title),
                    options = AutoRefreshInterval.entries,
                    selected = autoRefreshInterval,
                    label = { stringResource(it.labelRes()) },
                    onSelect = viewModel::setAutoRefreshInterval,
                )
            }
            item(contentType = "row") {
                AreSwitchRow(
                    title = stringResource(CoreR.string.settings_confirm_exit_title),
                    supporting = stringResource(CoreR.string.settings_confirm_exit_desc),
                    checked = confirmBeforeExit,
                    onCheckedChange = viewModel::setConfirmBeforeExit,
                )
            }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_language_row_title),
                    supporting = currentLanguageName?.let { stringResource(it.nativeNameRes) } ?: effectiveTag,
                    leadingIcon = Icons.Filled.Language,
                    onClick = { showLanguageSheet = true },
                )
            }

            // ---- Provider (read-only; Xtream/Stalker only) ----------------------------------
            if (activeSource?.type == SourceType.XTREAM) {
                item(contentType = "header") { AreSectionHeader(stringResource(CoreR.string.settings_section_provider)) }
                item(contentType = "provider") { XtreamProviderRows(providerInfo) }
            }
            if (activeSource?.type == SourceType.STALKER) {
                item(contentType = "header") { AreSectionHeader(stringResource(CoreR.string.settings_section_provider)) }
                item(contentType = "provider") { StalkerProviderRows(stalkerInfo) }
            }

            // ---- More ----------------------------------------------------------------------
            item(contentType = "header") { AreSectionHeader(stringResource(CoreR.string.settings_section_more)) }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_section_playback),
                    supporting = stringResource(CoreR.string.settings_more_playback_desc),
                    leadingIcon = Icons.Filled.PlayCircle,
                    onClick = onOpenPlayback,
                )
            }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_section_subtitles),
                    supporting = stringResource(CoreR.string.settings_more_subtitles_desc),
                    leadingIcon = Icons.Filled.ClosedCaption,
                    onClick = onOpenSubtitles,
                )
            }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_section_parental),
                    supporting = stringResource(CoreR.string.settings_more_parental_desc),
                    leadingIcon = Icons.Filled.Lock,
                    onClick = onOpenParental,
                )
            }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_tab_about),
                    supporting = stringResource(CoreR.string.settings_more_about_desc),
                    leadingIcon = Icons.Filled.Info,
                    onClick = onOpenAbout,
                )
            }

            // ---- Storage -------------------------------------------------------------------
            item(contentType = "header") { AreSectionHeader(stringResource(CoreR.string.settings_section_storage)) }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_clear_cache_title),
                    supporting = stringResource(CoreR.string.settings_clear_cache_desc),
                    leadingIcon = Icons.Filled.DeleteSweep,
                    onClick = { storageConfirm = StorageAction.ClearCache },
                )
            }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_clear_history_title),
                    supporting = stringResource(CoreR.string.settings_clear_history_desc),
                    leadingIcon = Icons.Filled.History,
                    onClick = { storageConfirm = StorageAction.ClearHistory },
                )
            }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_reset_title),
                    supporting = stringResource(CoreR.string.settings_reset_desc),
                    leadingIcon = Icons.Filled.RestartAlt,
                    onClick = { storageConfirm = StorageAction.Reset },
                )
            }
        }
    }

    if (showLanguageSheet) {
        AreBottomSheet(
            onDismiss = { showLanguageSheet = false },
            title = stringResource(CoreR.string.settings_language_row_title),
        ) {
            AreLanguageList(
                selectedTag = effectiveTag,
                onSelect = { picked ->
                    showLanguageSheet = false
                    if (!picked.equals(effectiveTag, ignoreCase = true)) pendingLanguage = picked
                },
                contentPadding = PaddingValues(bottom = 12.dp),
            )
        }
    }

    val targetLanguage = pendingLanguage
    if (targetLanguage != null) {
        val context = LocalContext.current
        val copy = remember(targetLanguage) { localizedConfirmCopy(context, targetLanguage) }
        AreAlertDialog(
            onDismiss = { pendingLanguage = null },
            title = copy.title,
            confirmLabel = copy.confirm,
            onConfirm = {
                pendingLanguage = null
                applyLanguage = targetLanguage
            },
            dismissLabel = copy.cancel,
        )
    }

    when (storageConfirm) {
        StorageAction.ClearCache -> StorageConfirmDialog(
            title = stringResource(CoreR.string.settings_clear_cache_title),
            message = stringResource(CoreR.string.settings_clear_cache_confirm),
            confirmText = stringResource(CoreR.string.action_clear),
            onConfirm = viewModel::clearImageCache,
            onDismiss = { storageConfirm = null },
        )
        StorageAction.ClearHistory -> StorageConfirmDialog(
            title = stringResource(CoreR.string.settings_clear_history_title),
            message = stringResource(CoreR.string.settings_clear_history_confirm),
            confirmText = stringResource(CoreR.string.action_clear),
            onConfirm = viewModel::clearHistory,
            onDismiss = { storageConfirm = null },
        )
        StorageAction.Reset -> StorageConfirmDialog(
            title = stringResource(CoreR.string.settings_reset_title),
            message = stringResource(CoreR.string.settings_reset_confirm),
            confirmText = stringResource(CoreR.string.settings_reset_action),
            onConfirm = viewModel::resetToDefaults,
            onDismiss = { storageConfirm = null },
        )
        null -> Unit
    }
}

private enum class StorageAction { ClearCache, ClearHistory, Reset }

/** Destructive confirm -- scrim tap and system Back both dismiss (§3.3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AreAlertDialog(
        onDismiss = onDismiss,
        title = title,
        text = message,
        confirmLabel = confirmText,
        onConfirm = { onConfirm(); onDismiss() },
        dismissLabel = stringResource(CoreR.string.action_cancel),
        destructive = true,
    )
}

/** Read-only Xtream account rows -- same fields and [CoreR] keys as before, as list rows. */
@Composable
private fun XtreamProviderRows(info: XtreamAccountInfo?) {
    if (info == null) {
        AreListRow(title = stringResource(CoreR.string.settings_provider_loading))
        return
    }
    val dash = stringResource(CoreR.string.settings_provider_unknown)
    AreListRow(
        title = stringResource(CoreR.string.settings_provider_status),
        supporting = info.status?.replaceFirstChar { it.uppercase() } ?: dash,
    )
    AreListRow(
        title = stringResource(CoreR.string.settings_provider_expires),
        supporting = expiryText(info.expiresAtMs),
    )
    if (info.isTrial) {
        AreListRow(
            title = stringResource(CoreR.string.settings_provider_trial),
            supporting = stringResource(CoreR.string.settings_provider_trial_value),
        )
    }
    connectionsText(info.maxConnections, info.activeConnections)?.let {
        AreListRow(title = stringResource(CoreR.string.settings_provider_connections), supporting = it)
    }
    info.timezone?.takeIf { it.isNotBlank() }?.let {
        AreListRow(title = stringResource(CoreR.string.settings_provider_timezone), supporting = it)
    }
    info.serverTime?.takeIf { it.isNotBlank() }?.let {
        AreListRow(title = stringResource(CoreR.string.settings_provider_server_time), supporting = it)
    }
    serverText(info.host, info.port)?.let {
        AreListRow(title = stringResource(CoreR.string.settings_provider_server), supporting = it)
    }
}

/** Read-only Stalker portal rows; the portal info block carries no expiry countdown or seat count. */
@Composable
private fun StalkerProviderRows(info: StalkerAccountInfo?) {
    if (info == null) {
        AreListRow(title = stringResource(CoreR.string.settings_provider_loading))
        return
    }
    val dash = stringResource(CoreR.string.settings_provider_unknown)
    AreListRow(
        title = stringResource(CoreR.string.settings_provider_status),
        supporting = info.status?.replaceFirstChar { it.uppercase() } ?: dash,
    )
    AreListRow(
        title = stringResource(CoreR.string.settings_provider_expires),
        supporting = info.expiresRaw ?: dash,
    )
    info.host?.takeIf { it.isNotBlank() }?.let {
        AreListRow(title = stringResource(CoreR.string.settings_provider_server), supporting = it)
    }
}

/** null -> "Unlimited", past -> "Expired", today -> "expires today", else "<date> · N days left". */
@Composable
private fun expiryText(expiresAtMs: Long?): String {
    if (expiresAtMs == null) return stringResource(CoreR.string.settings_provider_unlimited)
    val date = java.text.SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(java.util.Date(expiresAtMs))
    val daysLeft = (expiresAtMs - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)
    return when {
        daysLeft < 0 -> stringResource(CoreR.string.settings_provider_expired)
        daysLeft == 0L -> stringResource(CoreR.string.settings_provider_expires_today, date)
        else -> stringResource(CoreR.string.settings_provider_expires_days, date, daysLeft)
    }
}

@Composable
private fun connectionsText(max: Int?, active: Int?): String? = when {
    max != null && active != null -> stringResource(CoreR.string.settings_provider_connections_value, active, max)
    max != null -> stringResource(CoreR.string.settings_provider_connections_max, max)
    else -> null
}

private fun serverText(host: String?, port: String?): String? {
    val h = host?.takeIf { it.isNotBlank() } ?: return null
    return if (!port.isNullOrBlank()) "$h:$port" else h
}

/** Title + Confirm/Cancel for the language-switch confirm, phrased in the TARGET language. */
private data class LanguageConfirmCopy(val title: String, val confirm: String, val cancel: String)

private fun localizedConfirmCopy(context: Context, languageTag: String): LanguageConfirmCopy {
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

/** "N days/hours/mins ago" label for the last catalog refresh; [neverRefreshedText] when unset. */
@Composable
private fun lastUpdatedLabel(ts: Long?, neverRefreshedText: String): String {
    if (ts == null) return neverRefreshedText
    val ago = System.currentTimeMillis() - ts
    val days = ago / (24 * 60 * 60 * 1000)
    val hours = ago / (60 * 60 * 1000)
    val mins = ago / (60 * 1000)
    return when {
        days >= 1 -> if (days == 1L) stringResource(CoreR.string.settings_last_updated_days, days)
        else stringResource(CoreR.string.settings_last_updated_days_plural, days)
        hours >= 1 -> if (hours == 1L) stringResource(CoreR.string.settings_last_updated_hours, hours)
        else stringResource(CoreR.string.settings_last_updated_hours_plural, hours)
        mins >= 1 -> stringResource(CoreR.string.settings_last_updated_mins, mins)
        else -> stringResource(CoreR.string.settings_last_updated_now)
    }
}
