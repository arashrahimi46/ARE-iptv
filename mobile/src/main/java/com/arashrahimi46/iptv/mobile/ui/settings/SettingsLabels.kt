package com.arashrahimi46.iptv.mobile.ui.settings

import androidx.annotation.StringRes
import com.arashrahimi46.iptv.data.settings.AutoRefreshInterval
import com.arashrahimi46.iptv.data.settings.AutoRelock
import com.arashrahimi46.iptv.data.settings.LockedContentDisplay
import com.arashrahimi46.iptv.data.settings.StartScreen
import com.arashrahimi46.iptv.data.settings.SubtitleColorChoice
import com.arashrahimi46.iptv.data.settings.SubtitleEdge
import com.arashrahimi46.iptv.data.settings.SubtitleFontChoice
import com.arashrahimi46.iptv.data.settings.SubtitleTextScale
import com.arashrahimi46.iptv.data.settings.ThemeMode
import java.util.Locale
import com.arashrahimi46.iptv.core.R as CoreR

/**
 * Every `@StringRes` mapper Settings needs, in one place so the root list and the four sub-screens
 * can share them (§6.7). Nothing here is a composable -- callers wrap the result in
 * `stringResource(...)`, which is what `AreChoiceRow`'s `label` lambda expects.
 *
 * The one behavioural change vs the old panes: [SubtitleColorChoice] no longer renders `.name`
 * (raw, untranslated "WHITE"/"YELLOW"); it maps to real `CoreR` keys like every other enum.
 */

@StringRes
internal fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.DARK -> CoreR.string.settings_theme_dark
    ThemeMode.LIGHT -> CoreR.string.settings_theme_light
    ThemeMode.SYSTEM -> CoreR.string.settings_theme_system
}

@StringRes
internal fun StartScreen.labelRes(): Int = when (this) {
    StartScreen.HOME -> CoreR.string.nav_home
    StartScreen.LIVE -> CoreR.string.nav_live_tv
    StartScreen.MOVIES -> CoreR.string.nav_movies
    StartScreen.SERIES -> CoreR.string.nav_series
    StartScreen.LAST_USED -> CoreR.string.settings_start_last_used
}

@StringRes
internal fun AutoRefreshInterval.labelRes(): Int = when (this) {
    AutoRefreshInterval.OFF -> CoreR.string.settings_auto_refresh_off
    AutoRefreshInterval.DAILY -> CoreR.string.settings_auto_refresh_daily
    AutoRefreshInterval.WEEKLY -> CoreR.string.settings_auto_refresh_weekly
}

/** Stale-catalog window, in days; `0` means "never mark stale". */
internal val StaleWindowOptions: List<Long> = listOf(7L, 14L, 30L, 0L)

@StringRes
internal fun staleWindowLabelRes(days: Long): Int = when (days) {
    7L -> CoreR.string.settings_stale_7d
    14L -> CoreR.string.settings_stale_14d
    30L -> CoreR.string.settings_stale_30d
    else -> CoreR.string.settings_stale_off
}

/** Autoplay-next countdown, in seconds; `0` disables it. */
internal val AutoplayDelayOptions: List<Long> = listOf(0L, 5L, 10L)

@StringRes
internal fun autoplayDelayLabelRes(seconds: Long): Int = when (seconds) {
    5L -> CoreR.string.settings_autoplay_5s
    10L -> CoreR.string.settings_autoplay_10s
    else -> CoreR.string.settings_autoplay_off
}

@StringRes
internal fun SubtitleTextScale.labelRes(): Int = when (this) {
    SubtitleTextScale.SMALL -> CoreR.string.settings_sub_size_s
    SubtitleTextScale.MEDIUM -> CoreR.string.settings_sub_size_m
    SubtitleTextScale.LARGE -> CoreR.string.settings_sub_size_l
    SubtitleTextScale.XLARGE -> CoreR.string.settings_sub_size_xl
}

@StringRes
internal fun SubtitleEdge.labelRes(): Int = when (this) {
    SubtitleEdge.BOX -> CoreR.string.settings_sub_style_box
    SubtitleEdge.OUTLINE -> CoreR.string.settings_sub_style_outline
    SubtitleEdge.SHADOW -> CoreR.string.settings_sub_style_shadow
}

/** One key per enum entry actually present in [SubtitleColorChoice] -- see §4.10. */
@StringRes
internal fun SubtitleColorChoice.labelRes(): Int = when (this) {
    SubtitleColorChoice.WHITE -> CoreR.string.subtitle_color_white
    SubtitleColorChoice.YELLOW -> CoreR.string.subtitle_color_yellow
    SubtitleColorChoice.CYAN -> CoreR.string.subtitle_color_cyan
    SubtitleColorChoice.GREEN -> CoreR.string.subtitle_color_green
}

@StringRes
internal fun SubtitleFontChoice.labelRes(): Int = when (this) {
    SubtitleFontChoice.DEFAULT -> CoreR.string.settings_sub_font_default
    SubtitleFontChoice.SANS -> CoreR.string.settings_sub_font_sans
    SubtitleFontChoice.SERIF -> CoreR.string.settings_sub_font_serif
    SubtitleFontChoice.MONO -> CoreR.string.settings_sub_font_mono
    SubtitleFontChoice.VAZIRMATN -> CoreR.string.settings_sub_font_vazirmatn
}

@StringRes
internal fun AutoRelock.labelRes(): Int = when (this) {
    AutoRelock.IMMEDIATELY -> CoreR.string.settings_relock_immediately
    AutoRelock.MIN_15 -> CoreR.string.settings_relock_15min
    AutoRelock.HOUR_1 -> CoreR.string.settings_relock_1hour
    AutoRelock.NEVER -> CoreR.string.settings_relock_never
}

@StringRes
internal fun LockedContentDisplay.labelRes(): Int = when (this) {
    LockedContentDisplay.HIDE -> CoreR.string.settings_locked_hide
    LockedContentDisplay.BLUR -> CoreR.string.settings_locked_blur
}

/**
 * Same fixed language list :tv's Settings uses (`ui/player/SubtitleMenu.kt`'s SUBTITLE_LANGUAGES).
 * `""` is the "Auto / player default" entry and is only offered for the preferred *audio* track.
 */
internal val SubtitleLanguageCodes: List<String> =
    listOf("en", "fa", "ar", "fr", "es", "de", "it", "tr", "ru", "pt", "nl", "hi")

internal val AudioLanguageCodes: List<String> = listOf("") + SubtitleLanguageCodes

/**
 * Display name for a subtitle/audio language code, rendered in the *user's* locale -- a Persian
 * user picking a subtitle language reads Persian names, not a Latin-script English list. Same
 * `Locale.forLanguageTag(...).displayLanguage` lookup `PlayerScreen` uses for track labels. Falls
 * back to the raw code when the platform has no name for it.
 */
internal fun languageCodeLabel(code: String): String =
    Locale.forLanguageTag(code).displayLanguage
        .ifBlank { code }
        .replaceFirstChar { it.uppercase() }
