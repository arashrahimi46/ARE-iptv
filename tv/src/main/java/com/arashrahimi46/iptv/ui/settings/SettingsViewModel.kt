package com.arashrahimi46.iptv.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.data.parser.OmdbClient
import com.arashrahimi46.iptv.data.parser.XtreamAccountInfo
import com.arashrahimi46.iptv.data.parser.OpenSubtitlesClient
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.AutoRefreshInterval
import com.arashrahimi46.iptv.data.settings.AutoRelock
import com.arashrahimi46.iptv.data.settings.LockedContentDisplay
import com.arashrahimi46.iptv.data.settings.MiniPlayerBehavior
import com.arashrahimi46.iptv.data.settings.PinHasher
import com.arashrahimi46.iptv.data.settings.StartScreen
import com.arashrahimi46.iptv.data.settings.SubtitleColorChoice
import com.arashrahimi46.iptv.data.settings.SubtitleEdge
import com.arashrahimi46.iptv.data.settings.SubtitleFontChoice
import com.arashrahimi46.iptv.data.settings.SubtitleTextScale
import com.arashrahimi46.iptv.data.settings.ThemeMode
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.theme.AccentPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Transient result of validating an OpenSubtitles credential (not persisted -- only the key is). */
sealed interface SubsValidation {
    data object Idle : SubsValidation
    data object Validating : SubsValidation
    data class Connected(val languageCount: Int) : SubsValidation
    data class Error(val message: String) : SubsValidation
}

/** Transient result of validating the OMDb metadata key (not persisted -- only the key is). */
sealed interface OmdbValidation {
    data object Idle : OmdbValidation
    data object Validating : OmdbValidation
    data object Connected : OmdbValidation
    data class Error(val message: String) : OmdbValidation
}

/** Transient state of a manual catalog refresh (not persisted -- the timestamp/counts live in Room). */
sealed interface RefreshState {
    data object Idle : RefreshState
    data object Refreshing : RefreshState
    data class Success(val channels: Int, val movies: Int, val series: Int) : RefreshState
    data class Error(val message: String) : RefreshState
}

/**
 * Backs the real Settings screen (Phase 4): every flow here is a live read
 * off [UserSettings]' DataStore, and every setter writes straight back to it
 * (off the main thread, via [UserSettings]' own `dataStore.edit`). Theme and
 * reduced-motion take effect immediately in the running app because
 * [com.arashrahimi46.iptv.MainActivity] collects [UserSettings.isDarkTheme] /
 * [UserSettings.isReducedMotion] directly and feeds them into
 * [com.arashrahimi46.iptv.ui.theme.AreIptvTheme] at the composition root --
 * see report for how this was verified.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = UserSettings(app)
    private val playlists = PlaylistRepositoryImpl(app)

    private fun <T> flowState(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    val themeMode: StateFlow<ThemeMode> = flowState(settings.themeMode, ThemeMode.DARK)
    val isReducedMotion: StateFlow<Boolean> = flowState(settings.isReducedMotion, false)
    val is24HourClock: StateFlow<Boolean> = flowState(settings.is24HourClock, true)
    val staleWindowDays: StateFlow<Long> = flowState(settings.staleWindowDays, 14L)
    val preferredAudioLanguage: StateFlow<String> = flowState(settings.preferredAudioLanguage, "")
    val autoplayNextDelaySeconds: StateFlow<Long> = flowState(settings.autoplayNextDelaySeconds, 0L)
    val subtitleTextScale: StateFlow<SubtitleTextScale> = flowState(settings.subtitleTextScale, SubtitleTextScale.MEDIUM)
    val subtitleEdge: StateFlow<SubtitleEdge> = flowState(settings.subtitleEdge, SubtitleEdge.BOX)
    val subtitleColor: StateFlow<SubtitleColorChoice> = flowState(settings.subtitleColor, SubtitleColorChoice.WHITE)
    val subtitleFont: StateFlow<SubtitleFontChoice> = flowState(settings.subtitleFont, SubtitleFontChoice.DEFAULT)

    /** Per-mode accent presets; the Display tab picks which one to show/edit from the effective
     * (SYSTEM-resolved) dark state, so SYSTEM mode edits whichever mode is actually on screen. */
    val darkAccent: StateFlow<AccentPreset> = flowState(settings.darkAccent, AccentPreset.BLUE)
    val lightAccent: StateFlow<AccentPreset> = flowState(settings.lightAccent, AccentPreset.BLUE)
    val isHardwareDecoding: StateFlow<Boolean> = flowState(settings.isHardwareDecoding, true)
    val isAutoplayNextEpisode: StateFlow<Boolean> = flowState(settings.isAutoplayNextEpisode, true)
    val miniPlayerBehavior: StateFlow<MiniPlayerBehavior> = flowState(settings.miniPlayerBehavior, MiniPlayerBehavior.DODGE)
    val isParentalLockEnabled: StateFlow<Boolean> = flowState(settings.isParentalLockEnabled, false)
    val isBrowseListMode: StateFlow<Boolean> = flowState(settings.isBrowseListMode, false)
    val isAnalyticsEnabled: StateFlow<Boolean> = flowState(settings.isAnalyticsEnabled, true)
    val isRecordingIndicatorEnabled: StateFlow<Boolean> = flowState(settings.isRecordingIndicatorEnabled, true)
    val hudLayout: StateFlow<List<com.arashrahimi46.iptv.ui.player.HudSlot>> =
        flowState(settings.hudLayout, com.arashrahimi46.iptv.ui.player.DEFAULT_HUD_LAYOUT)
    /** Nullable so "not yet loaded" (null) is distinct from "loaded, no PIN set" (false) --
     * the Settings screen gates the PIN row / lock toggle until this resolves so a fast tap
     * during the initial async DataStore read can't route to a no-verify PIN-set flow. */
    val hasPinSet: StateFlow<Boolean?> = flowState(settings.parentalPinHash.map { it != null }, null)

    // Phase 3 -- General
    val startScreen: StateFlow<StartScreen> = flowState(settings.startScreen, StartScreen.HOME)
    val autoRefreshInterval: StateFlow<AutoRefreshInterval> = flowState(settings.autoRefreshInterval, AutoRefreshInterval.OFF)
    val confirmBeforeExit: StateFlow<Boolean> = flowState(settings.confirmBeforeExit, true)

    // Phase 3 -- Parental depth
    val parentalAutoRelock: StateFlow<AutoRelock> = flowState(settings.parentalAutoRelock, AutoRelock.IMMEDIATELY)
    val lockedContentDisplay: StateFlow<LockedContentDisplay> = flowState(settings.lockedContentDisplay, LockedContentDisplay.HIDE)
    val parentalKeywords: StateFlow<Set<String>> = flowState(settings.parentalKeywords, emptySet())
    val isPinOnLaunch: StateFlow<Boolean> = flowState(settings.isPinOnLaunch, false)

    /** Preferred subtitle language (ISO code) used first in online search; defaults to English. */
    val subtitleLanguage: StateFlow<String> = flowState(settings.subtitleLanguage, "en")

    /** Current app language tag (BCP-47), e.g. "en"/"es"/"pt-BR"; see [UserSettings.languageTag]. */
    val languageTag: StateFlow<String> = flowState(settings.languageTag, "en")

    /** The stored OpenSubtitles credential, or null when not connected. */
    val openSubsCredential: StateFlow<String?> = flowState(settings.openSubsCredential, null)

    private val _subsValidation = MutableStateFlow<SubsValidation>(SubsValidation.Idle)
    /** Live feedback for the connect action; resets to [SubsValidation.Idle] on disconnect. */
    val subsValidation: StateFlow<SubsValidation> = _subsValidation.asStateFlow()

    /** The stored OpenSubtitles account username, or null when not signed in (needed for downloads). */
    val openSubsUsername: StateFlow<String?> = flowState(settings.openSubsUsername, null)

    private val _subsLogin = MutableStateFlow<SubsValidation>(SubsValidation.Idle)
    /** Live feedback for the account sign-in action ([SubsValidation.Connected.languageCount] = remaining downloads). */
    val subsLogin: StateFlow<SubsValidation> = _subsLogin.asStateFlow()

    /** The stored OMDb metadata key, or null when not connected. */
    val omdbKey: StateFlow<String?> = flowState(settings.omdbKey, null)

    private val _omdbValidation = MutableStateFlow<OmdbValidation>(OmdbValidation.Idle)
    /** Live feedback for the OMDb connect action; resets to [OmdbValidation.Idle] on disconnect. */
    val omdbValidation: StateFlow<OmdbValidation> = _omdbValidation.asStateFlow()

    // Async, off the main thread -- these are DataStore file writes and must not block the UI
    // thread's switch callback (jank/ANR on low-end boxes). Matches every other setter below.
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setReducedMotion(enabled: Boolean) = viewModelScope.launch { settings.setReducedMotion(enabled) }
    fun set24HourClock(enabled: Boolean) = viewModelScope.launch { settings.set24HourClock(enabled) }
    fun setStaleWindowDays(days: Long) = viewModelScope.launch { settings.setStaleWindowDays(days) }
    fun setPreferredAudioLanguage(code: String) = viewModelScope.launch { settings.setPreferredAudioLanguage(code) }
    fun setAutoplayNextDelaySeconds(seconds: Long) = viewModelScope.launch { settings.setAutoplayNextDelaySeconds(seconds) }
    fun setSubtitleTextScale(scale: SubtitleTextScale) = viewModelScope.launch { settings.setSubtitleTextScale(scale) }
    fun setSubtitleEdge(edge: SubtitleEdge) = viewModelScope.launch { settings.setSubtitleEdge(edge) }
    fun setSubtitleColor(color: SubtitleColorChoice) = viewModelScope.launch { settings.setSubtitleColor(color) }
    fun setSubtitleFont(font: SubtitleFontChoice) = viewModelScope.launch { settings.setSubtitleFont(font) }

    /** Writes the accent for the mode currently on screen ([isDark], effective); the other mode's
     * accent is untouched. The caller passes the SYSTEM-resolved dark state (see [darkAccent]). */
    fun setActiveAccent(isDark: Boolean, preset: AccentPreset) = viewModelScope.launch {
        settings.setAccent(isDark, preset)
    }

    /** Reset user-facing prefs to defaults (scoped -- see [UserSettings.resetToDefaults]). */
    fun resetToDefaults() = viewModelScope.launch { settings.resetToDefaults() }

    /** Wipe the Coil memory + disk image cache (posters/logos re-download on next view). */
    fun clearImageCache() = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val loader = coil.Coil.imageLoader(getApplication())
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
    }

    /** Clear every Continue-Watching / resume bookmark. */
    fun clearHistory() = viewModelScope.launch {
        com.arashrahimi46.iptv.data.repository.ContinueWatchingRepository(getApplication()).clearAll()
    }
    fun setHardwareDecoding(enabled: Boolean) = viewModelScope.launch { settings.setHardwareDecoding(enabled) }
    fun setAutoplayNextEpisode(enabled: Boolean) = viewModelScope.launch { settings.setAutoplayNextEpisode(enabled) }

    fun setMiniPlayerBehavior(choice: MiniPlayerBehavior) = viewModelScope.launch { settings.setMiniPlayerBehavior(choice) }

    fun setBrowseListMode(enabled: Boolean) = viewModelScope.launch { settings.setBrowseListMode(enabled) }

    fun setRecordingIndicatorEnabled(enabled: Boolean) = viewModelScope.launch { settings.setRecordingIndicatorEnabled(enabled) }

    fun setHudLayout(slots: List<com.arashrahimi46.iptv.ui.player.HudSlot>) = viewModelScope.launch { settings.setHudLayout(slots) }

    /** Persist the analytics opt-out AND flip live collection so it takes effect without a restart. */
    fun setAnalyticsEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setAnalyticsEnabled(enabled)
        com.arashrahimi46.iptv.analytics.Analytics.setEnabled(enabled)
    }

    fun setSubtitleLanguage(code: String) = viewModelScope.launch { settings.setSubtitleLanguage(code) }

    /** Persists the app language tag -- the caller (SettingsScreen) applies the locale via
     * `AppCompatDelegate.setApplicationLocales` itself, since that's a platform call best made
     * right at the confirm action rather than hidden inside the ViewModel. */
    fun setLanguageTag(tag: String) = viewModelScope.launch { settings.setLanguageTag(tag) }

    /** Validates [credential] against OpenSubtitles and only persists it once a real request succeeds. */
    fun connectOpenSubs(credential: String) {
        val trimmed = credential.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _subsValidation.value = SubsValidation.Validating
            _subsValidation.value = when (val result = OpenSubtitlesClient.validateKey(trimmed)) {
                is OpenSubtitlesClient.KeyResult.Valid -> {
                    settings.setOpenSubsCredential(trimmed)
                    SubsValidation.Connected(result.languageCount)
                }
                is OpenSubtitlesClient.KeyResult.Invalid -> SubsValidation.Error(result.message)
            }
        }
    }

    fun disconnectOpenSubs() = viewModelScope.launch {
        settings.setOpenSubsCredential(null)
        settings.setOpenSubsAccount(null, null)
        _subsValidation.value = SubsValidation.Idle
        _subsLogin.value = SubsValidation.Idle
    }

    /** Validates [key] against OMDb and only persists it once a real request succeeds. */
    fun connectOmdb(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _omdbValidation.value = OmdbValidation.Validating
            _omdbValidation.value = when (val result = OmdbClient.validateKey(trimmed)) {
                is OmdbClient.KeyResult.Valid -> {
                    settings.setOmdbKey(trimmed)
                    OmdbValidation.Connected
                }
                is OmdbClient.KeyResult.Invalid -> OmdbValidation.Error(result.message)
            }
        }
    }

    fun disconnectOmdb() = viewModelScope.launch {
        settings.setOmdbKey(null)
        _omdbValidation.value = OmdbValidation.Idle
    }

    /** Logs in to the OpenSubtitles account (for downloads) and persists the credentials on success. */
    fun signInOpenSubs(username: String, phrase: String) {
        if (username.isBlank() || phrase.isBlank()) return
        viewModelScope.launch {
            _subsLogin.value = SubsValidation.Validating
            val cred = settings.openSubsCredential.first()
            if (cred == null) {
                _subsLogin.value = SubsValidation.Error(getApplication<Application>().getString(R.string.settings_subs_connect_key_first))
                return@launch
            }
            _subsLogin.value = when (val r = OpenSubtitlesClient.login(cred, username, phrase)) {
                is OpenSubtitlesClient.LoginResult.Success -> {
                    settings.setOpenSubsAccount(username, phrase)
                    SubsValidation.Connected(r.remaining)
                }
                is OpenSubtitlesClient.LoginResult.Failure -> SubsValidation.Error(r.message)
            }
        }
    }

    fun signOutOpenSubs() = viewModelScope.launch {
        settings.setOpenSubsAccount(null, null)
        _subsLogin.value = SubsValidation.Idle
    }

    // --- Playlist refresh (manual catalog sync) ---

    /** The active playlist source, live -- drives the "Last updated" label and the Refresh row. */
    val activeSource: StateFlow<PlaylistSource?> =
        flowState(
            settings.activeSourceId.flatMapLatest { id ->
                if (id == null) flowOf(null) else playlists.observeSource(id)
            },
            null,
        )

    /** Parsed Xtream provider metadata for the active source (Provider panel); null for M3U sources
     *  and Xtream rows not yet refreshed since this feature shipped. Snapshot as of the last refresh. */
    val providerInfo: StateFlow<XtreamAccountInfo?> =
        flowState(
            settings.activeSourceId.flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)
                } else {
                    playlists.observeSource(id).map { src ->
                        if (src?.type == SourceType.XTREAM) XtreamAccountInfo.fromJson(src.accountInfoJson) else null
                    }
                }
            },
            null,
        )

    /** Parsed Stalker portal metadata for the active source (Provider panel); null for non-Stalker
     *  sources. Snapshot as of the last refresh, mirroring [providerInfo] for Xtream. */
    val stalkerInfo: StateFlow<com.arashrahimi46.iptv.data.parser.StalkerAccountInfo?> =
        flowState(
            settings.activeSourceId.flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)
                } else {
                    playlists.observeSource(id).map { src ->
                        if (src?.type == SourceType.STALKER) {
                            com.arashrahimi46.iptv.data.parser.StalkerAccountInfo.fromJson(src.accountInfoJson)
                        } else {
                            null
                        }
                    }
                }
            },
            null,
        )

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    /** Live feedback for the manual refresh action. */
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    /**
     * Manually re-sync the active playlist with the provider (id-preserving upsert for Xtream --
     * favorites, continue-watching, and lazily-loaded metadata survive). A failed/empty fetch leaves
     * the existing catalog untouched (see [PlaylistRepository.refreshSource]) and surfaces the error.
     */
    fun refresh() {
        if (_refreshState.value is RefreshState.Refreshing) return
        viewModelScope.launch {
            val sourceId = settings.activeSourceId.first()
            if (sourceId == null) {
                _refreshState.value = RefreshState.Error(getApplication<Application>().getString(R.string.settings_refresh_no_playlist))
                return@launch
            }
            _refreshState.value = RefreshState.Refreshing
            _refreshState.value = try {
                val summary = playlists.refreshSource(sourceId)
                RefreshState.Success(summary.channels, summary.movies, summary.series)
            } catch (e: Exception) {
                RefreshState.Error(e.message ?: getApplication<Application>().getString(R.string.settings_refresh_failed))
            }
        }
    }

    /** Turning the lock ON never needs a PIN check; the caller (SettingsScreen) only calls this after a PIN exists. */
    fun setParentalLockEnabled(enabled: Boolean) = viewModelScope.launch { settings.setParentalLockEnabled(enabled) }

    /** Hashes+salts [pin] via [PinHasher] -- the raw PIN is never persisted. */
    fun setPin(pin: String) {
        viewModelScope.launch {
            val salt = PinHasher.newSalt()
            settings.setParentalPin(PinHasher.hash(pin, salt), salt)
        }
    }

    /** Suspend so the PIN-entry dialog can await a real DataStore read before showing success/failure. */
    suspend fun verifyPin(pin: String): Boolean {
        val hash = settings.parentalPinHash.first() ?: return false
        val salt = settings.parentalPinSalt.first() ?: return false
        return PinHasher.verify(pin, salt, hash)
    }

    // Phase 3 setters -- General
    fun setStartScreen(value: StartScreen) = viewModelScope.launch { settings.setStartScreen(value) }
    fun setAutoRefreshInterval(value: AutoRefreshInterval) = viewModelScope.launch { settings.setAutoRefreshInterval(value) }
    fun setConfirmBeforeExit(enabled: Boolean) = viewModelScope.launch { settings.setConfirmBeforeExit(enabled) }

    // Phase 3 setters -- Parental depth
    fun setParentalAutoRelock(value: AutoRelock) = viewModelScope.launch { settings.setParentalAutoRelock(value) }
    fun setLockedContentDisplay(value: LockedContentDisplay) = viewModelScope.launch { settings.setLockedContentDisplay(value) }
    fun toggleParentalKeyword(word: String) = viewModelScope.launch { settings.toggleParentalKeyword(word) }
    fun setPinOnLaunch(enabled: Boolean) = viewModelScope.launch { settings.setPinOnLaunch(enabled) }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(app) as T
            }
    }
}
