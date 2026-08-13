package com.arashrahimi46.iptv.mobile.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.model.PlaylistSource
import com.arashrahimi46.iptv.mobile.data.model.SourceType
import com.arashrahimi46.iptv.mobile.data.parser.StalkerAccountInfo
import com.arashrahimi46.iptv.mobile.data.parser.XtreamAccountInfo
import com.arashrahimi46.iptv.mobile.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.mobile.data.settings.AutoRefreshInterval
import com.arashrahimi46.iptv.mobile.data.settings.AutoRelock
import com.arashrahimi46.iptv.mobile.data.settings.LockedContentDisplay
import com.arashrahimi46.iptv.mobile.data.settings.ParentalGate
import com.arashrahimi46.iptv.mobile.data.settings.PinHasher
import com.arashrahimi46.iptv.mobile.data.settings.StartScreen
import com.arashrahimi46.iptv.mobile.data.settings.SubtitleColorChoice
import com.arashrahimi46.iptv.mobile.data.settings.SubtitleEdge
import com.arashrahimi46.iptv.mobile.data.settings.SubtitleFontChoice
import com.arashrahimi46.iptv.mobile.data.settings.SubtitleTextScale
import com.arashrahimi46.iptv.mobile.data.settings.ThemeMode
import com.arashrahimi46.iptv.mobile.data.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Transient state of a manual catalog refresh (not persisted). */
sealed interface MobileRefreshState {
    data object Idle : MobileRefreshState
    data object Refreshing : MobileRefreshState
    data class Success(val channels: Int, val movies: Int, val series: Int) : MobileRefreshState
    data class Error(val message: String) : MobileRefreshState
}

/**
 * Lean Settings ViewModel for the phone app, covering exactly the Phase 3 follow-up scope
 * (catalog/EPG refresh, subtitles, playback quality, parental) directly over [UserSettings] --
 * the same shared DataStore :tv's `SettingsViewModel` writes to, so a change made here is visible
 * there (and vice versa) without any extra plumbing.
 *
 * Deliberately NOT the full port of :tv's `SettingsViewModel`: that class also owns OMDb/
 * OpenSubtitles account integration, the accent picker, the HUD layout editor, and storage/reset
 * actions -- all outside the "subtitles/EPG/quality/parental panes" scope this follow-up was
 * asked to cover, and mobile has no accent picker or HUD editor UI to back in the first place.
 * See the Phase 3 report for this scope call.
 *
 * Dark/Light/System [themeMode] IS included even though it wasn't in the original Phase 3 list:
 * mobile's `MainActivity` shipped only reading the legacy [com.arashrahimi46.iptv.mobile.data.settings.UserSettings.isDarkTheme]
 * boolean (always true, i.e. permanently dark) with no way for the user to change it, which left
 * light theme -- explicitly called out as needing to be best-in-class -- unreachable in the app.
 * Ported minimally: just the picker, not :tv's whole "Display" pane (accent/reduce-motion/list-
 * mode/clock), which is out of scope here.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = UserSettings(app)
    private val playlists = PlaylistRepositoryImpl(app)

    private fun <T> flowState(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    // --- Display ---
    val themeMode: StateFlow<ThemeMode> = flowState(settings.themeMode, ThemeMode.DARK)
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    // --- Catalog / EPG ---
    val activeSource: StateFlow<PlaylistSource?> =
        flowState(settings.activeSourceId.flatMapLatest { id -> if (id == null) flowOf(null) else playlists.observeSource(id) }, null)
    val staleWindowDays: StateFlow<Long> = flowState(settings.staleWindowDays, 14L)
    val autoRefreshInterval: StateFlow<AutoRefreshInterval> = flowState(settings.autoRefreshInterval, AutoRefreshInterval.OFF)

    private val _refreshState = MutableStateFlow<MobileRefreshState>(MobileRefreshState.Idle)
    val refreshState: StateFlow<MobileRefreshState> = _refreshState.asStateFlow()

    fun setStaleWindowDays(days: Long) = viewModelScope.launch { settings.setStaleWindowDays(days) }
    fun setAutoRefreshInterval(value: AutoRefreshInterval) = viewModelScope.launch { settings.setAutoRefreshInterval(value) }

    // --- General: preferences, provider, language, storage (full :tv parity) ---
    val startScreen: StateFlow<StartScreen> = flowState(settings.startScreen, StartScreen.HOME)
    val confirmBeforeExit: StateFlow<Boolean> = flowState(settings.confirmBeforeExit, true)
    fun setStartScreen(value: StartScreen) = viewModelScope.launch { settings.setStartScreen(value) }
    fun setConfirmBeforeExit(enabled: Boolean) = viewModelScope.launch { settings.setConfirmBeforeExit(enabled) }

    /** Parsed Xtream provider metadata for the active source (Provider panel); null for M3U/Stalker
     * sources and Xtream rows not yet refreshed since this feature shipped. Same shape as :tv's. */
    val providerInfo: StateFlow<XtreamAccountInfo?> =
        flowState(
            settings.activeSourceId.flatMapLatest { id ->
                if (id == null) flowOf(null)
                else playlists.observeSource(id).map { src -> if (src?.type == SourceType.XTREAM) XtreamAccountInfo.fromJson(src.accountInfoJson) else null }
            },
            null,
        )

    /** Parsed Stalker portal metadata for the active source (Provider panel); null for non-Stalker
     * sources, mirroring [providerInfo] for Xtream. */
    val stalkerInfo: StateFlow<StalkerAccountInfo?> =
        flowState(
            settings.activeSourceId.flatMapLatest { id ->
                if (id == null) flowOf(null)
                else playlists.observeSource(id).map { src -> if (src?.type == SourceType.STALKER) StalkerAccountInfo.fromJson(src.accountInfoJson) else null }
            },
            null,
        )

    /** Current app language tag (BCP-47); the caller (SettingsScreen) applies the locale via
     * `AppCompatDelegate.setApplicationLocales` itself once its confirm dialog's window is gone --
     * same two-frame-delay rule :tv's General pane documents (calling it while the dialog window is
     * still up can strand the Activity with no focused window on the recreate that follows). */
    val languageTag: StateFlow<String> = flowState(settings.languageTag, "en")
    fun setLanguageTag(tag: String) = viewModelScope.launch { settings.setLanguageTag(tag) }

    /** Wipe the Coil memory + disk image cache (posters/logos re-download on next view). */
    fun clearImageCache() = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val loader = coil.Coil.imageLoader(getApplication())
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
    }

    /** Clear every Continue-Watching / resume bookmark. */
    fun clearHistory() = viewModelScope.launch {
        ContinueWatchingRepository(getApplication()).clearAll()
    }

    /** Reset user-facing prefs to defaults (scoped -- see [UserSettings.resetToDefaults]); playlists,
     * sign-ins, language and parental lock are kept, same scope :tv's reset uses. */
    fun resetToDefaults() = viewModelScope.launch { settings.resetToDefaults() }

    fun refresh() {
        if (_refreshState.value is MobileRefreshState.Refreshing) return
        viewModelScope.launch {
            val sourceId = settings.activeSourceId.first()
            if (sourceId == null) {
                _refreshState.value = MobileRefreshState.Error(getApplication<Application>().getString(CoreR.string.settings_refresh_no_playlist))
                return@launch
            }
            _refreshState.value = MobileRefreshState.Refreshing
            _refreshState.value = try {
                val summary = playlists.refreshSource(sourceId)
                MobileRefreshState.Success(summary.channels, summary.movies, summary.series)
            } catch (e: Exception) {
                MobileRefreshState.Error(e.message ?: getApplication<Application>().getString(CoreR.string.settings_refresh_failed))
            }
        }
    }

    // --- Subtitles ---
    val subtitleLanguage: StateFlow<String> = flowState(settings.subtitleLanguage, "en")
    val subtitleTextScale: StateFlow<SubtitleTextScale> = flowState(settings.subtitleTextScale, SubtitleTextScale.MEDIUM)
    val subtitleEdge: StateFlow<SubtitleEdge> = flowState(settings.subtitleEdge, SubtitleEdge.BOX)
    val subtitleColor: StateFlow<SubtitleColorChoice> = flowState(settings.subtitleColor, SubtitleColorChoice.WHITE)
    val subtitleFont: StateFlow<SubtitleFontChoice> = flowState(settings.subtitleFont, SubtitleFontChoice.DEFAULT)

    fun setSubtitleLanguage(code: String) = viewModelScope.launch { settings.setSubtitleLanguage(code) }
    fun setSubtitleTextScale(scale: SubtitleTextScale) = viewModelScope.launch { settings.setSubtitleTextScale(scale) }
    fun setSubtitleEdge(edge: SubtitleEdge) = viewModelScope.launch { settings.setSubtitleEdge(edge) }
    fun setSubtitleColor(color: SubtitleColorChoice) = viewModelScope.launch { settings.setSubtitleColor(color) }
    fun setSubtitleFont(font: SubtitleFontChoice) = viewModelScope.launch { settings.setSubtitleFont(font) }

    // --- Playback quality ---
    val isHardwareDecoding: StateFlow<Boolean> = flowState(settings.isHardwareDecoding, true)
    val preferredAudioLanguage: StateFlow<String> = flowState(settings.preferredAudioLanguage, "")
    val autoplayNextDelaySeconds: StateFlow<Long> = flowState(settings.autoplayNextDelaySeconds, 0L)

    fun setHardwareDecoding(enabled: Boolean) = viewModelScope.launch { settings.setHardwareDecoding(enabled) }
    fun setPreferredAudioLanguage(code: String) = viewModelScope.launch { settings.setPreferredAudioLanguage(code) }
    fun setAutoplayNextDelaySeconds(seconds: Long) = viewModelScope.launch { settings.setAutoplayNextDelaySeconds(seconds) }

    // --- Parental ---
    val isParentalLockEnabled: StateFlow<Boolean> = flowState(settings.isParentalLockEnabled, false)
    /** Nullable so "not yet loaded" (null) is distinct from "loaded, no PIN set" (false). */
    val hasPinSet: StateFlow<Boolean?> = flowState(settings.parentalPinHash.map { it != null }, null)
    val parentalAutoRelock: StateFlow<AutoRelock> = flowState(settings.parentalAutoRelock, AutoRelock.IMMEDIATELY)
    val lockedContentDisplay: StateFlow<LockedContentDisplay> = flowState(settings.lockedContentDisplay, LockedContentDisplay.HIDE)
    val isPinOnLaunch: StateFlow<Boolean> = flowState(settings.isPinOnLaunch, false)

    /** Turning the lock ON also drops any session unlock still in flight ([ParentalGate]): without
     * this, enabling the lock inside an unlocked window leaves [UserSettings.parentalFilter]
     * reporting "nothing to hide" and the switch appears to do nothing until the relock timer
     * happens to fire. */
    fun setParentalLockEnabled(enabled: Boolean) = viewModelScope.launch {
        if (enabled) ParentalGate.relock()
        settings.setParentalLockEnabled(enabled)
    }
    fun setParentalAutoRelock(value: AutoRelock) = viewModelScope.launch { settings.setParentalAutoRelock(value) }
    fun setLockedContentDisplay(value: LockedContentDisplay) = viewModelScope.launch { settings.setLockedContentDisplay(value) }
    fun setPinOnLaunch(enabled: Boolean) = viewModelScope.launch { settings.setPinOnLaunch(enabled) }

    fun setPin(pin: String) {
        viewModelScope.launch {
            val salt = PinHasher.newSalt()
            settings.setParentalPin(PinHasher.hash(pin, salt), salt)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val hash = settings.parentalPinHash.first() ?: return false
        val salt = settings.parentalPinSalt.first() ?: return false
        return PinHasher.verify(pin, salt, hash)
    }

    // --- About ---
    // Same shared UserSettings keys :tv's About pane reads/writes -- a toggle made here is
    // visible there too, no extra plumbing (same pattern as themeMode above).
    val isAnalyticsEnabled: StateFlow<Boolean> = flowState(settings.isAnalyticsEnabled, true)
    val isCrashReportingEnabled: StateFlow<Boolean> = flowState(settings.isCrashReportingEnabled, true)
    fun setAnalyticsEnabled(enabled: Boolean) = viewModelScope.launch { settings.setAnalyticsEnabled(enabled) }
    fun setCrashReportingEnabled(enabled: Boolean) = viewModelScope.launch { settings.setCrashReportingEnabled(enabled) }
}
