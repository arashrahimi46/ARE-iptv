package com.arashrahimi46.iptv.mobile.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arashrahimi46.iptv.mobile.ui.home.DEFAULT_HOME_LAYOUT
import com.arashrahimi46.iptv.mobile.ui.home.HomeSection
import com.arashrahimi46.iptv.mobile.ui.home.decodeHomeLayout
import com.arashrahimi46.iptv.mobile.ui.home.encodeHomeLayout
import com.arashrahimi46.iptv.mobile.ui.player.DEFAULT_HUD_ARRANGEMENT
import com.arashrahimi46.iptv.mobile.ui.player.HudArrangement
import com.arashrahimi46.iptv.mobile.ui.player.decodeHudLayout
import com.arashrahimi46.iptv.mobile.ui.player.encodeHudLayout
import com.arashrahimi46.iptv.mobile.design.AccentPreset
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "are_iptv_mobile_settings")

/** How the docked mini-player avoids covering the tab content behind it: [DODGE] slides it to the
 * free corner when focus reaches its slot (returning home when focus leaves); [FADE] keeps it put
 * but fades+shrinks it while the user browses. See [com.arashrahimi46.iptv.mobile.ui.player.LiveMiniPlayerOverlay]. */
enum class MiniPlayerBehavior { DODGE, FADE }

/** App theme selection. [SYSTEM] follows the device's day/night setting (resolved in a Composable
 * via `isSystemInDarkTheme()`); [DARK]/[LIGHT] force one mode regardless of the device. */
enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** Left-nav container style (see [com.arashrahimi46.iptv.ui.components.AreSidebarNav]). [FLOATING] is
 * the glass box inset off the screen edge (the app's new identity); [EDGE] is the full-height rail
 * flush to the bezel. Same nav items and focus model either way — only the surface/shape differs. */
enum class SidebarStyle { FLOATING, EDGE }

/** Subtitle text size as a fraction of view height (Media3 `SubtitleView.setFractionalTextSize`);
 * [MEDIUM] matches Media3's own default (0.0533). */
enum class SubtitleTextScale(val fraction: Float) { SMALL(0.04f), MEDIUM(0.0533f), LARGE(0.072f), XLARGE(0.095f) }

/** Subtitle edge/background render style ([CaptionStyleCompat]): filled box, outline, or drop shadow. */
enum class SubtitleEdge { BOX, OUTLINE, SHADOW }

/** Subtitle foreground color choice (a small fixed, high-contrast palette). [argb] is the fill color. */
enum class SubtitleColorChoice(val argb: Int) {
    WHITE(0xFFFFFFFF.toInt()),
    YELLOW(0xFFFFEB3B.toInt()),
    CYAN(0xFF00E5FF.toInt()),
    GREEN(0xFF69F0AE.toInt()),
}

/** Subtitle typeface. [VAZIRMATN] uses the bundled Vazirmatn face (renders Persian/Arabic well). */
enum class SubtitleFontChoice { DEFAULT, SANS, SERIF, MONO, VAZIRMATN }

/** Which shell tab opens on launch. [LAST_USED] restores whatever tab the user left off on
 * (see [UserSettings.lastUsedTab]); the rest map to a fixed inner-NavHost route. */
enum class StartScreen(val route: String?) { HOME("home"), LIVE("live"), MOVIES("movies"), SERIES("series"), LAST_USED(null) }

/** Auto-refresh the active catalog on launch when it's older than [maxAgeMs]. [OFF] never refreshes. */
enum class AutoRefreshInterval(val maxAgeMs: Long) {
    OFF(0L), DAILY(24L * 60 * 60 * 1000), WEEKLY(7L * 24 * 60 * 60 * 1000)
}

/** How long a PIN unlock reveals adult content before it re-locks (see [ParentalGate]).
 * [IMMEDIATELY] grants no lingering session (each protected item needs its own PIN);
 * [NEVER] stays unlocked until the app is restarted. */
enum class AutoRelock(val durationMs: Long) {
    IMMEDIATELY(0L), MIN_15(15L * 60 * 1000), HOUR_1(60L * 60 * 1000), NEVER(Long.MAX_VALUE)
}

/** How locked adult content appears while the lock is engaged. [HIDE] drops it from every list
 * (today's behavior); [BLUR] keeps it as an obscured, PIN-to-reveal tile. */
enum class LockedContentDisplay { HIDE, BLUR }

/**
 * Preferences DataStore for app-wide settings that outlive a single screen.
 * Fully wired to the real Settings screen (Phase 4) -- see
 * [com.arashrahimi46.iptv.ui.settings.SettingsScreen].
 */
class UserSettings(private val context: Context) {
    private object Keys {
        val ACTIVE_SOURCE_ID = longPreferencesKey("active_source_id")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        /** Theme selection (enum name, see [ThemeMode]); absent = derive from legacy [DARK_THEME]. */
        val THEME_MODE = stringPreferencesKey("theme_mode")
        /** Left-nav container style (enum name, see [SidebarStyle]); default FLOATING. */
        val SIDEBAR_STYLE = stringPreferencesKey("sidebar_style")
        /** True renders EPG/HUD clocks in 24-hour (HH:mm); false = 12-hour (h:mm a). Default 24h. */
        val CLOCK_24H = booleanPreferencesKey("clock_24h")
        /** Days after which a catalog is "stale" (nudge refresh); 0 = never stale. Default 14. */
        val STALE_WINDOW_DAYS = longPreferencesKey("stale_window_days")
        /** Preferred audio language (ISO code) auto-selected at player build; blank = automatic. */
        val PREFERRED_AUDIO_LANG = stringPreferencesKey("preferred_audio_lang")
        /** Auto-advance-to-next-episode delay in seconds; 0 = off (no auto-advance). Default 0. */
        val AUTOPLAY_NEXT_DELAY = longPreferencesKey("autoplay_next_delay")
        /** Subtitle appearance (enum names / int color). */
        val SUBTITLE_TEXT_SCALE = stringPreferencesKey("subtitle_text_scale")
        val SUBTITLE_EDGE = stringPreferencesKey("subtitle_edge")
        val SUBTITLE_COLOR = stringPreferencesKey("subtitle_color")
        val SUBTITLE_FONT = stringPreferencesKey("subtitle_font")
        /** Selected accent preset id per theme mode (see [AccentPreset]); independent choices. */
        val ACCENT_DARK = stringPreferencesKey("accent_dark")
        val ACCENT_LIGHT = stringPreferencesKey("accent_light")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val HARDWARE_DECODING = booleanPreferencesKey("hardware_decoding")
        /** Video aspect/resize mode applied in the player (enum name, see AspectMode); default fit. */
        val VIDEO_ASPECT_MODE = stringPreferencesKey("video_aspect_mode")
        /** Per-live-channel aspect overrides (channelId -> AspectMode name); see [videoAspectByChannel]. */
        val VIDEO_ASPECT_BY_CHANNEL = stringPreferencesKey("video_aspect_by_channel")
        val AUTOPLAY_NEXT_EPISODE = booleanPreferencesKey("autoplay_next_episode")
        val MINI_PLAYER_BEHAVIOR = stringPreferencesKey("mini_player_behavior")
        val PARENTAL_LOCK_ENABLED = booleanPreferencesKey("parental_lock_enabled")
        val PARENTAL_PIN_HASH = stringPreferencesKey("parental_pin_hash")
        val PARENTAL_PIN_SALT = stringPreferencesKey("parental_pin_salt")
        /** Parental depth (Phase 3): auto-relock timing, hide-vs-blur, custom keyword set, PIN-on-launch. */
        val PARENTAL_AUTO_RELOCK = stringPreferencesKey("parental_auto_relock")
        val PARENTAL_LOCKED_DISPLAY = stringPreferencesKey("parental_locked_display")
        val PARENTAL_KEYWORDS = stringSetPreferencesKey("parental_keywords")
        val PARENTAL_PIN_ON_LAUNCH = booleanPreferencesKey("parental_pin_on_launch")
        /** General (Phase 3): start screen + last-used tab, launch auto-refresh, confirm-before-exit. */
        val START_SCREEN = stringPreferencesKey("start_screen")
        val LAST_USED_TAB = stringPreferencesKey("last_used_tab")
        val AUTO_REFRESH = stringPreferencesKey("auto_refresh_interval")
        val CONFIRM_EXIT = booleanPreferencesKey("confirm_exit")
        val GUIDE_SELECTED_CATEGORY = stringPreferencesKey("guide_selected_category")
        val BROWSE_LIST_MODE = booleanPreferencesKey("browse_list_mode")
        val TERMS_ACCEPTED = booleanPreferencesKey("terms_accepted")
        val TERMS_VERSION = intPreferencesKey("terms_version")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
        val RECORDING_INDICATOR = booleanPreferencesKey("recording_indicator")
        val OPENSUBS_CRED = stringPreferencesKey("opensubs_cred")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val OPENSUBS_U = stringPreferencesKey("opensubs_u")
        val OPENSUBS_P = stringPreferencesKey("opensubs_p")
        val OMDB_KEY = stringPreferencesKey("omdb_key")
        val HOME_LAYOUT = stringPreferencesKey("home_layout")
        /** Persisted live-TV player-HUD arrangement (see [hudLayoutLive]). */
        val HUD_LAYOUT = stringPreferencesKey("hud_layout")
        /** Persisted movies/series player-HUD arrangement (see [hudLayoutVod]). */
        val HUD_LAYOUT_VOD = stringPreferencesKey("hud_layout_vod")
        /** BCP-47 app language tag ("en", "es", "fr", "de", "it", "pt-BR"); see [languageTag]. */
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        /** Whether the first-run language selector has been completed; see [hasSelectedLanguage]. */
        val LANGUAGE_CHOSEN = booleanPreferencesKey("language_chosen")
        /** Pinned category names, namespaced per browse screen (see [pinnedCategoriesKey]). */
        fun pinnedCategoriesKey(namespace: String) = stringSetPreferencesKey("pinned_categories_$namespace")

        /** Curated multi-view channel ids, per source (see [multiViewChannelIds]). */
        fun multiViewChannelsKey(sourceId: Long) = stringPreferencesKey("multiview_channels_$sourceId")
    }

    /**
     * Every exposed preference flow goes through here.
     *
     * PERF: the `.distinctUntilChanged()` is the whole point. DataStore emits a fresh [Preferences]
     * snapshot on ANY `edit`, so without it a write to one key woke all ~48 mapped flows on every
     * live [UserSettings] instance; each re-ran its transform and re-emitted an identical value,
     * which then propagated through the ViewModels' `combine`s and re-triggered their
     * `flatMapLatest`/`mapLatest` blocks -- re-running catalog queries and rail loads for a setting
     * that did not change. The keys written most often are the cheap incidental ones: LAST_USED_TAB
     * fires on every tab change, GUIDE_SELECTED_CATEGORY on every guide category, and
     * VIDEO_ASPECT_BY_CHANNEL during playback.
     *
     * (The decode itself was never duplicated -- `dataStore` is a Context extension delegate, so all
     * call sites share one SingleProcessDataStore and DataStore caches the decoded Preferences. Only
     * the transform lambdas re-ran, which is why this is a wake-up problem, not an IO one.)
     */
    private fun <T> pref(transform: (Preferences) -> T): Flow<T> =
        context.dataStore.data.map(transform).distinctUntilChanged()

    val activeSourceId: Flow<Long?> = pref { prefs ->
        prefs[Keys.ACTIVE_SOURCE_ID]?.takeIf { it > 0 }
    }

    val isDarkTheme: Flow<Boolean> = pref { it[Keys.DARK_THEME] ?: true }

    /** Theme selection; when unset, migrates the legacy [DARK_THEME] bool (true→DARK, false→LIGHT)
     * so existing users keep exactly the mode they had before this pref existed. Default DARK. */
    val themeMode: Flow<ThemeMode> = pref { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: if (prefs[Keys.DARK_THEME] == false) ThemeMode.LIGHT else ThemeMode.DARK
    }

    /** Left-nav container style; defaults to [SidebarStyle.FLOATING] (the glass box is the new default). */
    val sidebarStyle: Flow<SidebarStyle> = pref { prefs ->
        prefs[Keys.SIDEBAR_STYLE]?.let { runCatching { SidebarStyle.valueOf(it) }.getOrNull() } ?: SidebarStyle.FLOATING
    }

    /** EPG/HUD clock format; true = 24-hour (HH:mm), false = 12-hour (h:mm a). Defaults to 24-hour. */
    val is24HourClock: Flow<Boolean> = pref { it[Keys.CLOCK_24H] ?: true }

    /** Catalog stale window in days (0 = never stale). Backs the Settings "stale-window" chips and
     * the sidebar refresh nudge; defaults to 14 days (the previous hardcoded window). */
    val staleWindowDays: Flow<Long> = pref { it[Keys.STALE_WINDOW_DAYS] ?: 14L }

    /** Preferred audio language (ISO code) auto-selected when the user hasn't picked a track in the
     * player; blank = let ExoPlayer choose automatically. */
    val preferredAudioLanguage: Flow<String> = pref { it[Keys.PREFERRED_AUDIO_LANG] ?: "" }

    /** Auto-advance-to-next-episode delay in seconds; 0 = off. Default 0 (matches today: no auto-advance). */
    val autoplayNextDelaySeconds: Flow<Long> = pref { it[Keys.AUTOPLAY_NEXT_DELAY] ?: 0L }

    val subtitleTextScale: Flow<SubtitleTextScale> = pref { prefs ->
        prefs[Keys.SUBTITLE_TEXT_SCALE]?.let { runCatching { SubtitleTextScale.valueOf(it) }.getOrNull() } ?: SubtitleTextScale.MEDIUM
    }
    val subtitleEdge: Flow<SubtitleEdge> = pref { prefs ->
        prefs[Keys.SUBTITLE_EDGE]?.let { runCatching { SubtitleEdge.valueOf(it) }.getOrNull() } ?: SubtitleEdge.BOX
    }
    val subtitleColor: Flow<SubtitleColorChoice> = pref { prefs ->
        prefs[Keys.SUBTITLE_COLOR]?.let { runCatching { SubtitleColorChoice.valueOf(it) }.getOrNull() } ?: SubtitleColorChoice.WHITE
    }
    val subtitleFont: Flow<SubtitleFontChoice> = pref { prefs ->
        prefs[Keys.SUBTITLE_FONT]?.let { runCatching { SubtitleFontChoice.valueOf(it) }.getOrNull() } ?: SubtitleFontChoice.DEFAULT
    }

    val isReducedMotion: Flow<Boolean> = pref { it[Keys.REDUCED_MOTION] ?: false }

    /** Accent preset chosen for dark mode; defaults to [AccentPreset.BLUE] (the original accent). */
    val darkAccent: Flow<AccentPreset> = pref { AccentPreset.fromId(it[Keys.ACCENT_DARK]) }

    /** Accent preset chosen for light mode; independent of [darkAccent], defaults to BLUE. */
    val lightAccent: Flow<AccentPreset> = pref { AccentPreset.fromId(it[Keys.ACCENT_LIGHT]) }

    /** ExoPlayer decoder-fallback preference -- see [com.arashrahimi46.iptv.mobile.ui.player.LivePlayerScreen]'s `ExoPlayer.Builder`. */
    val isHardwareDecoding: Flow<Boolean> = pref { it[Keys.HARDWARE_DECODING] ?: true }

    /**
     * Preference only in this phase -- persisted for real, but no auto-advance-on-completion
     * listener is wired yet (documented follow-up; see report). [com.arashrahimi46.iptv.ui.detail.DetailScreen]'s
     * episode list already lets a user manually pick the next episode.
     */
    val isAutoplayNextEpisode: Flow<Boolean> = pref { it[Keys.AUTOPLAY_NEXT_EPISODE] ?: true }

    /** Docked mini-player anti-occlusion behavior; defaults to [MiniPlayerBehavior.DODGE]. */
    val miniPlayerBehavior: Flow<MiniPlayerBehavior> = pref { prefs ->
        prefs[Keys.MINI_PLAYER_BEHAVIOR]?.let { runCatching { MiniPlayerBehavior.valueOf(it) }.getOrNull() } ?: MiniPlayerBehavior.DODGE
    }

    val isParentalLockEnabled: Flow<Boolean> = pref { it[Keys.PARENTAL_LOCK_ENABLED] ?: false }

    /** Salted SHA-256 hash -- never the raw PIN. Null when no PIN has been set yet. */
    val parentalPinHash: Flow<String?> = pref { it[Keys.PARENTAL_PIN_HASH] }
    val parentalPinSalt: Flow<String?> = pref { it[Keys.PARENTAL_PIN_SALT] }

    /** How long a PIN unlock reveals adult content before re-locking; defaults to [AutoRelock.IMMEDIATELY]. */
    val parentalAutoRelock: Flow<AutoRelock> = pref { prefs ->
        prefs[Keys.PARENTAL_AUTO_RELOCK]?.let { runCatching { AutoRelock.valueOf(it) }.getOrNull() } ?: AutoRelock.IMMEDIATELY
    }

    /** Whether locked content is hidden or shown blurred; defaults to [LockedContentDisplay.HIDE] (today's behavior). */
    val lockedContentDisplay: Flow<LockedContentDisplay> = pref { prefs ->
        prefs[Keys.PARENTAL_LOCKED_DISPLAY]?.let { runCatching { LockedContentDisplay.valueOf(it) }.getOrNull() } ?: LockedContentDisplay.HIDE
    }

    /** User-added lowercase substrings merged into [AdultContentFilter]'s built-in markers; empty by default. */
    val parentalKeywords: Flow<Set<String>> = pref { it[Keys.PARENTAL_KEYWORDS] ?: emptySet() }

    /** Require the parental PIN before the app opens; only enforced when the lock is on and a PIN is set. Default off. */
    val isPinOnLaunch: Flow<Boolean> = pref { it[Keys.PARENTAL_PIN_ON_LAUNCH] ?: false }

    /** Which tab opens on launch; defaults to [StartScreen.HOME] (today's behavior). */
    val startScreen: Flow<StartScreen> = pref { prefs ->
        prefs[Keys.START_SCREEN]?.let { runCatching { StartScreen.valueOf(it) }.getOrNull() } ?: StartScreen.HOME
    }

    /** Last shell tab the user was on; backs [StartScreen.LAST_USED]. Defaults to "home". */
    val lastUsedTab: Flow<String> = pref { it[Keys.LAST_USED_TAB] ?: "home" }

    /** Auto-refresh the active catalog on launch; defaults to [AutoRefreshInterval.OFF]. */
    val autoRefreshInterval: Flow<AutoRefreshInterval> = pref { prefs ->
        prefs[Keys.AUTO_REFRESH]?.let { runCatching { AutoRefreshInterval.valueOf(it) }.getOrNull() } ?: AutoRefreshInterval.OFF
    }

    /** Confirm before leaving the app on Back at the shell root; defaults on (preserves the existing
     * exit-confirm dialog -- turning it off exits immediately). */
    val confirmBeforeExit: Flow<Boolean> = pref { it[Keys.CONFIRM_EXIT] ?: true }

    /**
     * Single source of truth for how the parental lock filters catalogs, folding together the lock
     * toggle, hide-vs-blur choice, custom keywords, and the runtime [ParentalGate] unlock. View-models
     * consult [ParentalFilter.hidden] instead of calling [AdultContentFilter] directly, so BLUR mode
     * keeps adult items (the tiles obscure them) while HIDE mode drops them -- and a session unlock
     * reveals everything. Swaps in for the old bare `isParentalLockEnabled` flow at each combine.
     */
    val parentalFilter: Flow<ParentalFilter> =
        combine(isParentalLockEnabled, lockedContentDisplay, parentalKeywords, ParentalGate.unlocked) { lock, display, keywords, unlocked ->
            ParentalFilter(
                hideLocked = lock && display == LockedContentDisplay.HIDE && !unlocked,
                keywords = keywords,
            )
        }.distinctUntilChanged()
    // Deduped again AFTER the combine, not just on its inputs: `hideLocked` collapses three of them
    // into one boolean, so e.g. flipping display between BOX and OUTLINE while the lock is off is a
    // real upstream change that produces an identical ParentalFilter. Every catalog query in the app
    // is keyed on this flow, so an identical re-emission re-runs them for nothing.

    /** Last channel-group filter picked on the Guide screen (see [com.arashrahimi46.iptv.ui.guide.GuideViewModel]); "All" when unset. */
    val guideSelectedCategory: Flow<String> = pref { it[Keys.GUIDE_SELECTED_CATEGORY] ?: "All" }

    /**
     * Which version of the Privacy Policy / Terms this user has accepted; 0 = never accepted.
     *
     * Replaces the original boolean: acceptance has to be per-VERSION, because a user who agreed
     * to the old text has not agreed to the new one. The legacy [Keys.TERMS_ACCEPTED] boolean is
     * read as version 1, so existing installs are re-prompted exactly once when
     * [CURRENT_TERMS_VERSION] moves past it, and every future revision reuses the same mechanism.
     */
    val acceptedTermsVersion: Flow<Int> = pref { prefs ->
        prefs[Keys.TERMS_VERSION] ?: if (prefs[Keys.TERMS_ACCEPTED] == true) 1 else 0
    }

    /** True once the user has accepted the CURRENT terms; drives the first-run gate. */
    val hasAcceptedTerms: Flow<Boolean> = acceptedTermsVersion.map { it >= CURRENT_TERMS_VERSION }

    /** Anonymous usage analytics (Firebase/GA4) opt-out; on by default, a Settings switch flips it.
     * Read once at startup to seed [com.arashrahimi46.iptv.analytics.Analytics] collection state. */
    val isAnalyticsEnabled: Flow<Boolean> = pref { it[Keys.ANALYTICS_ENABLED] ?: true }

    /** Crash + ANR diagnostics (Sentry) opt-out; on by default. Read at startup to seed
     * [com.arashrahimi46.iptv.analytics.CrashReporting], which gates every outgoing event. */
    val isCrashReportingEnabled: Flow<Boolean> = pref { it[Keys.CRASH_REPORTING_ENABLED] ?: true }

    /** Show the pulsing "REC" badge in the player HUD while recording; on by default (see
     * [com.arashrahimi46.iptv.ui.components.RecordingIndicator]). */
    val isRecordingIndicatorEnabled: Flow<Boolean> = pref { it[Keys.RECORDING_INDICATOR] ?: true }

    /** True renders Live TV/Movies/Series as a list instead of the default tile grid (see [com.arashrahimi46.iptv.ui.browse.BrowseLayout]). */
    val isBrowseListMode: Flow<Boolean> = pref { it[Keys.BROWSE_LIST_MODE] ?: false }

    /** Preferred subtitle language (ISO code) pre-selected when searching subtitles online; defaults to English. */
    val subtitleLanguage: Flow<String> = pref { it[Keys.SUBTITLE_LANGUAGE] ?: "en" }

    /** Video aspect/resize mode name (see AspectMode in the player); "FIT" (whole picture) by default. */
    val videoAspectMode: Flow<String> = pref { it[Keys.VIDEO_ASPECT_MODE] ?: "FIT" }

    /** Per-live-channel aspect overrides (channelId -> AspectMode name); empty until a live channel's
     * aspect is changed from the HUD. Live playback prefers this over the global [videoAspectMode];
     * VOD (no channel id) always uses the global default. */
    val videoAspectByChannel: Flow<Map<String, String>> =
        pref { it[Keys.VIDEO_ASPECT_BY_CHANNEL].parseAspectMap() }

    /** User's personal OpenSubtitles API key for online subtitle search; null until they connect one in Settings. */
    val openSubsCredential: Flow<String?> = pref { it[Keys.OPENSUBS_CRED]?.takeIf { k -> k.isNotBlank() } }

    /** User's personal OMDb API key for movie/series metadata (IMDb & Rotten Tomatoes ranks, plot,
     * cast); null until they connect one in Settings. See [com.arashrahimi46.iptv.mobile.data.parser.OmdbClient]. */
    val omdbKey: Flow<String?> = pref { it[Keys.OMDB_KEY]?.takeIf { k -> k.isNotBlank() } }

    /** OpenSubtitles account username -- needed (with [openSubsPhrase]) to log in for downloads. */
    val openSubsUsername: Flow<String?> = pref { it[Keys.OPENSUBS_U]?.takeIf { k -> k.isNotBlank() } }

    /** OpenSubtitles account password, stored to re-authenticate when the login token expires (~24h). */
    val openSubsPhrase: Flow<String?> = pref { it[Keys.OPENSUBS_P]?.takeIf { k -> k.isNotBlank() } }

    /** Persisted Home rail order/visibility; defaults to [DEFAULT_HOME_LAYOUT] until the user
     * customizes it (or if the stored value decodes to nothing usable). */
    val homeLayout: Flow<List<HomeSection>> = pref { prefs ->
        prefs[Keys.HOME_LAYOUT]?.let(::decodeHomeLayout)?.takeIf { it.isNotEmpty() } ?: DEFAULT_HOME_LAYOUT
    }

    /** Persisted live-TV player-HUD arrangement; defaults to [DEFAULT_HUD_ARRANGEMENT] until the
     * user rearranges it. [decodeHudLayout] self-heals (fills any missing controls) so this always
     * yields every control exactly once. Keeps the original `hud_layout` key: before the live/VOD
     * split there was one shared layout, and live is the dominant context, so it inherits. */
    val hudLayoutLive: Flow<HudArrangement> = pref { prefs ->
        prefs[Keys.HUD_LAYOUT]?.let(::decodeHudLayout) ?: DEFAULT_HUD_ARRANGEMENT
    }

    /** Persisted movies/series player-HUD arrangement. Separate from [hudLayoutLive] because the two
     * contexts offer genuinely different controls (speed vs record/guide/multi-view). */
    val hudLayoutVod: Flow<HudArrangement> = pref { prefs ->
        prefs[Keys.HUD_LAYOUT_VOD]?.let(::decodeHudLayout) ?: DEFAULT_HUD_ARRANGEMENT
    }

    /** BCP-47 app language tag ("en", "es", "fr", "de", "it", "pt-BR"); mirrors whatever was last
     * applied via `AppCompatDelegate.setApplicationLocales` so app logic can read "current
     * language" without touching AppCompatDelegate internals directly. Defaults to "en". */
    val languageTag: Flow<String> = pref { it[Keys.LANGUAGE_TAG] ?: "en" }

    /** Whether the first-run language selector has already been completed once; gates that screen
     * so it is shown exactly once, on first app open. Defaults to false. */
    val hasSelectedLanguage: Flow<Boolean> = pref { it[Keys.LANGUAGE_CHOSEN] ?: false }

    suspend fun setActiveSourceId(id: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_SOURCE_ID] = id }
    }

    /** Drop the active-source pointer (e.g. the active playlist was deleted) so nothing points at a
     *  gone source; [activeSourceId] then reads null until the user picks another. */
    suspend fun clearActiveSourceId() {
        context.dataStore.edit { it.remove(Keys.ACTIVE_SOURCE_ID) }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    /** Persists the theme mode and keeps the legacy [DARK_THEME] bool roughly in sync (SYSTEM leaves
     * it untouched) so any lingering legacy reader still sees a sensible value. */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit {
            it[Keys.THEME_MODE] = mode.name
            when (mode) {
                ThemeMode.DARK -> it[Keys.DARK_THEME] = true
                ThemeMode.LIGHT -> it[Keys.DARK_THEME] = false
                ThemeMode.SYSTEM -> {}
            }
        }
    }

    suspend fun setSidebarStyle(style: SidebarStyle) {
        context.dataStore.edit { it[Keys.SIDEBAR_STYLE] = style.name }
    }

    suspend fun set24HourClock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CLOCK_24H] = enabled }
    }

    suspend fun setStaleWindowDays(days: Long) {
        context.dataStore.edit { it[Keys.STALE_WINDOW_DAYS] = days }
    }

    suspend fun setPreferredAudioLanguage(code: String) {
        context.dataStore.edit {
            if (code.isBlank()) it.remove(Keys.PREFERRED_AUDIO_LANG) else it[Keys.PREFERRED_AUDIO_LANG] = code
        }
    }

    suspend fun setAutoplayNextDelaySeconds(seconds: Long) {
        context.dataStore.edit { it[Keys.AUTOPLAY_NEXT_DELAY] = seconds }
    }

    suspend fun setSubtitleTextScale(scale: SubtitleTextScale) {
        context.dataStore.edit { it[Keys.SUBTITLE_TEXT_SCALE] = scale.name }
    }

    suspend fun setSubtitleEdge(edge: SubtitleEdge) {
        context.dataStore.edit { it[Keys.SUBTITLE_EDGE] = edge.name }
    }

    suspend fun setSubtitleColor(color: SubtitleColorChoice) {
        context.dataStore.edit { it[Keys.SUBTITLE_COLOR] = color.name }
    }

    suspend fun setSubtitleFont(font: SubtitleFontChoice) {
        context.dataStore.edit { it[Keys.SUBTITLE_FONT] = font.name }
    }

    /** Reset user-facing preferences to their defaults by removing their keys (each flow then falls
     * back to its default). Deliberately scoped (D5): playlists/active source, sign-in credentials,
     * language, terms consent, and the parental lock/PIN are all left intact -- reset must not wipe
     * accounts, unlock adult content, or drop the user's catalog. */
    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            listOf(
                Keys.THEME_MODE, Keys.DARK_THEME, Keys.SIDEBAR_STYLE, Keys.ACCENT_DARK, Keys.ACCENT_LIGHT,
                Keys.REDUCED_MOTION, Keys.BROWSE_LIST_MODE, Keys.CLOCK_24H, Keys.STALE_WINDOW_DAYS,
                Keys.HARDWARE_DECODING, Keys.VIDEO_ASPECT_MODE, Keys.VIDEO_ASPECT_BY_CHANNEL,
                Keys.AUTOPLAY_NEXT_EPISODE, Keys.MINI_PLAYER_BEHAVIOR, Keys.RECORDING_INDICATOR,
                Keys.SUBTITLE_LANGUAGE, Keys.PREFERRED_AUDIO_LANG, Keys.AUTOPLAY_NEXT_DELAY,
                Keys.SUBTITLE_TEXT_SCALE, Keys.SUBTITLE_EDGE, Keys.SUBTITLE_COLOR, Keys.SUBTITLE_FONT,
                Keys.START_SCREEN, Keys.LAST_USED_TAB, Keys.AUTO_REFRESH, Keys.CONFIRM_EXIT,
                Keys.HUD_LAYOUT, Keys.HUD_LAYOUT_VOD,
            ).forEach { prefs.remove(it) }
        }
    }

    /** Persists the accent preset for a single mode; the other mode's accent is untouched. */
    suspend fun setAccent(isDark: Boolean, preset: AccentPreset) {
        context.dataStore.edit { it[if (isDark) Keys.ACCENT_DARK else Keys.ACCENT_LIGHT] = preset.id }
    }

    suspend fun setHardwareDecoding(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HARDWARE_DECODING] = enabled }
    }

    suspend fun setAutoplayNextEpisode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOPLAY_NEXT_EPISODE] = enabled }
    }

    suspend fun setMiniPlayerBehavior(choice: MiniPlayerBehavior) {
        context.dataStore.edit { it[Keys.MINI_PLAYER_BEHAVIOR] = choice.name }
    }

    suspend fun setParentalLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PARENTAL_LOCK_ENABLED] = enabled }
    }

    /** Stores only the salted hash produced by [com.arashrahimi46.iptv.mobile.data.settings.PinHasher]. */
    suspend fun setParentalPin(hash: String, salt: String) {
        context.dataStore.edit {
            it[Keys.PARENTAL_PIN_HASH] = hash
            it[Keys.PARENTAL_PIN_SALT] = salt
        }
    }

    suspend fun clearParentalPin() {
        context.dataStore.edit {
            it.remove(Keys.PARENTAL_PIN_HASH)
            it.remove(Keys.PARENTAL_PIN_SALT)
        }
    }

    suspend fun setParentalAutoRelock(value: AutoRelock) {
        context.dataStore.edit { it[Keys.PARENTAL_AUTO_RELOCK] = value.name }
    }

    suspend fun setLockedContentDisplay(value: LockedContentDisplay) {
        context.dataStore.edit { it[Keys.PARENTAL_LOCKED_DISPLAY] = value.name }
    }

    /** Add/remove a custom blocked keyword (stored lowercased, trimmed); blank input is ignored. */
    suspend fun toggleParentalKeyword(word: String) {
        val normalized = word.trim().lowercase()
        if (normalized.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.PARENTAL_KEYWORDS] ?: emptySet()
            prefs[Keys.PARENTAL_KEYWORDS] = if (normalized in current) current - normalized else current + normalized
        }
    }

    suspend fun setPinOnLaunch(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PARENTAL_PIN_ON_LAUNCH] = enabled }
    }

    suspend fun setStartScreen(value: StartScreen) {
        context.dataStore.edit { it[Keys.START_SCREEN] = value.name }
    }

    suspend fun setLastUsedTab(route: String) {
        context.dataStore.edit { it[Keys.LAST_USED_TAB] = route }
    }

    suspend fun setAutoRefreshInterval(value: AutoRefreshInterval) {
        context.dataStore.edit { it[Keys.AUTO_REFRESH] = value.name }
    }

    suspend fun setConfirmBeforeExit(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CONFIRM_EXIT] = enabled }
    }

    suspend fun setGuideSelectedCategory(category: String) {
        context.dataStore.edit { it[Keys.GUIDE_SELECTED_CATEGORY] = category }
    }

    suspend fun setBrowseListMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BROWSE_LIST_MODE] = enabled }
    }

    suspend fun setSubtitleLanguage(code: String) {
        context.dataStore.edit { it[Keys.SUBTITLE_LANGUAGE] = code }
    }

    suspend fun setVideoAspectMode(mode: String) {
        context.dataStore.edit { it[Keys.VIDEO_ASPECT_MODE] = mode }
    }

    /** Remembers [mode] (an AspectMode name) as the aspect for a single live [channelId]; other
     * channels' choices and the global [videoAspectMode] default are untouched. */
    suspend fun setChannelAspectMode(channelId: String, mode: String) {
        context.dataStore.edit { prefs ->
            val map = prefs[Keys.VIDEO_ASPECT_BY_CHANNEL].parseAspectMap().toMutableMap()
            map[channelId] = mode
            prefs[Keys.VIDEO_ASPECT_BY_CHANNEL] = map.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    /** Persists a validated OpenSubtitles API key, or clears it when [key] is null/blank. */
    suspend fun setOpenSubsCredential(key: String?) {
        context.dataStore.edit {
            if (key.isNullOrBlank()) it.remove(Keys.OPENSUBS_CRED) else it[Keys.OPENSUBS_CRED] = key.trim()
        }
    }

    /** Persists a validated OMDb API key, or clears it when [key] is null/blank. */
    suspend fun setOmdbKey(key: String?) {
        context.dataStore.edit {
            if (key.isNullOrBlank()) it.remove(Keys.OMDB_KEY) else it[Keys.OMDB_KEY] = key.trim()
        }
    }

    /** Persists (or clears) the OpenSubtitles account login used for downloads. */
    suspend fun setOpenSubsAccount(username: String?, phrase: String?) {
        context.dataStore.edit {
            if (username.isNullOrBlank() || phrase.isNullOrBlank()) {
                it.remove(Keys.OPENSUBS_U)
                it.remove(Keys.OPENSUBS_P)
            } else {
                it[Keys.OPENSUBS_U] = username.trim()
                it[Keys.OPENSUBS_P] = phrase
            }
        }
    }

    /** Records acceptance of [CURRENT_TERMS_VERSION]. Also keeps the legacy boolean in step so a
     *  downgrade to an older build doesn't re-prompt someone who has already accepted. */
    suspend fun acceptCurrentTerms() {
        context.dataStore.edit {
            it[Keys.TERMS_VERSION] = CURRENT_TERMS_VERSION
            it[Keys.TERMS_ACCEPTED] = true
        }
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANALYTICS_ENABLED] = enabled }
    }

    suspend fun setCrashReportingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CRASH_REPORTING_ENABLED] = enabled }
    }

    suspend fun setRecordingIndicatorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RECORDING_INDICATOR] = enabled }
    }

    suspend fun setHomeLayout(sections: List<HomeSection>) {
        context.dataStore.edit { it[Keys.HOME_LAYOUT] = encodeHomeLayout(sections) }
    }

    /** Persists one context's HUD arrangement; the other context's is untouched. */
    suspend fun setHudLayout(live: Boolean, arrangement: HudArrangement) {
        val key = if (live) Keys.HUD_LAYOUT else Keys.HUD_LAYOUT_VOD
        context.dataStore.edit { it[key] = encodeHudLayout(arrangement) }
    }

    /** Persists the chosen app language tag and marks the first-run selector as completed. Callers
     * are also expected to apply the locale via `AppCompatDelegate.setApplicationLocales` -- this
     * only mirrors the choice into DataStore (see [languageTag] doc). */
    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit {
            it[Keys.LANGUAGE_TAG] = tag
            it[Keys.LANGUAGE_CHOSEN] = true
        }
    }

    /**
     * Category names the user pinned to the top of a browse screen's group column.
     * Namespaced per screen ("live"/"movies"/"series") so pinning "Sports" in Live TV
     * doesn't also pin a same-named genre under Movies. Empty until the user pins one.
     */
    fun pinnedCategories(namespace: String): Flow<Set<String>> =
        pref { it[Keys.pinnedCategoriesKey(namespace)] ?: emptySet() }

    suspend fun togglePinnedCategory(namespace: String, name: String) {
        context.dataStore.edit { prefs ->
            val key = Keys.pinnedCategoriesKey(namespace)
            val current = prefs[key] ?: emptySet()
            prefs[key] = if (name in current) current - name else current + name
        }
    }

    /**
     * Multi-view is a curated, persistent list of live channels the user explicitly added --
     * per source, in insertion order (oldest first) so a full list evicts FIFO. Stored as a CSV
     * of channel ids; see [com.arashrahimi46.iptv.ui.multiview.MultiViewViewModel].
     */
    fun multiViewChannelIds(sourceId: Long): Flow<List<Long>> =
        pref { it[Keys.multiViewChannelsKey(sourceId)].parseIdCsv() }

    /** Append [channelId] (no duplicates); when already at [max], evict the oldest (FIFO). */
    suspend fun addMultiViewChannel(sourceId: Long, channelId: Long, max: Int) {
        context.dataStore.edit { prefs ->
            val key = Keys.multiViewChannelsKey(sourceId)
            val current = prefs[key].parseIdCsv()
            if (channelId in current) return@edit
            val next = (current + channelId).let { if (it.size > max) it.takeLast(max) else it }
            prefs[key] = next.joinToString(",")
        }
    }

    suspend fun removeMultiViewChannel(sourceId: Long, channelId: Long) {
        context.dataStore.edit { prefs ->
            val key = Keys.multiViewChannelsKey(sourceId)
            prefs[key] = prefs[key].parseIdCsv().filterNot { it == channelId }.joinToString(",")
        }
    }
}

/**
 * Version of the Privacy Policy / Terms of Service currently shipped in the app.
 *
 * BUMP THIS whenever the legal text changes materially -- every user is then re-prompted once and
 * their acceptance is recorded against the new number. v1 was the placeholder copy; v2 is the first
 * real document.
 */
const val CURRENT_TERMS_VERSION = 2

private fun String?.parseIdCsv(): List<Long> =
    this?.split(',')?.mapNotNull(String::toLongOrNull) ?: emptyList()

/** Decodes the "id=MODE" pairs (joined by ";") used to store per-channel aspect overrides. */
private fun String?.parseAspectMap(): Map<String, String> =
    this?.split(';')?.mapNotNull { pair ->
        val i = pair.indexOf('=')
        if (i <= 0) null else pair.substring(0, i) to pair.substring(i + 1)
    }?.toMap() ?: emptyMap()
