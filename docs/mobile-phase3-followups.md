# Mobile Phase 3 follow-ups

Scope cuts from the Phase 1+2 mobile build (commits `a5a7034`, `5672f89`), approved by
product-lead as reasonable for v1. Tracked here so they don't get lost before Phase 3.

1. **Settings screen is a placeholder.** `mobile/.../ui/settings/SettingsScreen.kt` only
   shows a "coming in a later phase" message. Needs subtitles/EPG/quality/parental panes,
   mirroring `:tv`'s `SettingsViewModel`/`SettingsPanes.kt` scope.

2. **Favorites toggle isn't wired.** Home/Live/Movies/Series rails *read* existing
   favorites (`FavoritesRepository.favoriteChannelIds`/`favoriteVodIds`) so anything
   favorited on `:tv` shows up, but there's no tile affordance on mobile to toggle a
   favorite, and no dedicated Favorites screen/tab yet.

3. **Continue Watching only resolves movies.** `HomeViewModel.kt` reads
   `ContinueWatchingEntry.vodTitleId` only; `seriesEpisodeId`/`recordingId` entries are
   dropped (needs episode→parent-title resolution, and recordings are out of v1 scope
   entirely per the original spec).

4. **RTL font swap skipped.** `mobile/.../ui/theme/Type.kt` always uses the Latin brand
   fonts (Space Grotesk/Manrope/JetBrains Mono); `:tv`'s Vazirmatn swap for fa/ar locales
   wasn't ported. Layout still mirrors correctly (`supportsRtl="true"`), just not the
   font. Upgrade path: copy `vazirmatn_*.ttf` into `mobile/res/font` and mirror
   `AreIptvTheme`'s `LocalLayoutDirection` check in `AreIptvMobileTheme`.
