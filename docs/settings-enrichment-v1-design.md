# Settings Enrichment — v1 Design

> Goal: make are-iptv **fully customizable** by growing the Settings surface to cover the knobs our
> existing features already imply, and reorganize the screen into **tabs** so it stays navigable.
> This spec is grounded in the **current** code — every proposal names the real seam it plugs into.
>
> **Scope was locked interactively** (2026-07-24). Sections 4a/4b below are the agreed v1 set; the
> original superset lives in git history. Read this before building any settings work.

---

## 1. Where we are today (inventory)

All settings live in `UserSettings` (`data/settings/UserSettings.kt`, a Preferences DataStore) and
are presented by `ui/settings/SettingsScreen.kt` as **one long scroll** of `SettingsSection`s:

| Section | Controls today |
|---|---|
| Playlists | Refresh catalog now (+ stale/last-updated) |
| Provider | Read-only Xtream/Stalker account panel |
| Appearance | Dark theme, per-mode Accent, Reduce motion, List view |
| Language | App language |
| Playback | Hardware decoding, Autoplay next episode, Mini-player behavior, Recording indicator |
| Parental | Lock adult content, Set/Change PIN |
| Subtitles | Subtitle language, OpenSubtitles key + account |
| Metadata | OMDb key |
| About | Version, Donate, Feedback, Analytics opt-out |

Also user-configurable but **outside** Settings (in-context): Home rail layout, pinned categories,
multi-view channel set, favorites, guide category.

### 1.1 Findings that shape this spec

- **Orphan setting — `videoAspectMode`.** ~~Stored with no consumer.~~ **Resolved in commit
  `380be67`** (per-channel aspect memory): `AspectMode` is now fully consumed in `LivePlayerScreen`
  (global + per-channel, set from the HUD). D2 is therefore already satisfied — no aspect row is
  needed in the Playback tab; it's controlled in-context from the player.
- **Autoplay-next is a stored flag with no runner.** `isAutoplayNextEpisode` exists but the
  auto-advance in `DetailScreen` is **not wired**. The v1 "autoplay delay" chip therefore also
  requires building that base behavior — it is **M cost, not a free chip** (see §4b, flag).
- **Hardcoded constants that are really settings:** EPG/HUD clock hardcoded to 24h `HH:mm`
  (`GuideScreen`, `LivePlayerScreen`); catalog "stale" window `REFRESH_STALE_MS = 14 days`.
- **Parental lock is coarse.** It gates the *toggle*, not individual titles (no per-title mature flag
  in the schema). v1 parental depth (§4b) stays within that limit — no schema change.

---

## 2. Design principles (locked)

1. **Feature-backed only.** A setting ships only when a real feature reads it (the `videoAspectMode`
   orphan is the anti-pattern we're paying down, not repeating).
2. **Reuse the existing shape.** New rows use `SettingsRow` + `AreSwitch`/`AreChip`/
   `SelectionChangeControl`; new groups use `SettingsSection`. No new control primitives.
3. **Live-apply, no restart.** Settings are DataStore flows read at the consumer, so changes take
   effect without a restart. Language is the documented exception (Activity recreate).
4. **Every string is translated as we go.** Any new user-facing string goes into `values/strings.xml`
   **and all 21 `values-*/strings.xml`** in the same change (locked: translate-per-key, not a
   deferred batch — see §3 D8). Keep terminology consistent via the glossary in §6.1.
5. **Sensible defaults = current behavior.** Every new setting defaults to exactly what the app does
   today, so existing users see no change until they opt in.

---

## 3. Locked decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | Settings stay in one DataStore (`UserSettings`); no new store, no DB migration. | Every proposal is a scalar/enum/string pref. |
| D2 | Finish `videoAspectMode` (wire it) before/with adding new playback prefs. | Pay down the orphan; validates the enum-chip pattern. |
| D3 | Numeric/enum prefs use a discrete **chip set** (D-pad friendly, no IME). | Matches TV conventions and existing chip rows. |
| D4 | **Full parental depth** for v1: auto-relock, hide-vs-blur, custom keywords, PIN-on-launch — all within "gate the toggle" (no per-title schema change). | User-selected full scope; none requires the schema change. |
| D5 | **Reset to defaults** clears DataStore keys but never touches sources/DB/recordings. | Safe, reversible-in-spirit, expected. |
| D6 | **Tabbed navigation** via a **horizontal top tab strip** (6 tabs: General, Display, Playback, Subtitles, Parental, About). | User-selected. Replaces the single long scroll. |
| D7 | **No settings export/import** in v1. Reset-to-defaults covers the intent. | Most surface, least-used on a single-device TV box. |
| D8 | **Translate every new key across all 21 locales in the same change.** | User-selected; keeps locales always in sync. |
| D9 | **Bundle subtitle fonts** (incl. **Vazirmatn**, SIL OFL) in `res/font/`, user-selectable. | User-requested; Vazirmatn renders Persian/Arabic subtitles well and is license-clean. |

---

## 4. Navigation — the tab shell (D6)

The screen gains a top tab strip. Each tab renders the existing `SettingsSection`/`SettingsRow`
shape below it; only the visible tab's sections compose.

| Tab | Sections under it |
|---|---|
| **General** | Playlists · Provider · Language · Metadata (OMDb) · Preferences (start screen, auto-refresh, confirm-exit, stale-window) · Storage (clear image cache, clear history) · Reset to defaults |
| **Display** | Theme mode · Accent · Reduce motion · List view · Clock 12/24h |
| **Playback** | Hardware decoding · Aspect/zoom · Preferred audio · Autoplay-next + delay · Mini-player · Recording indicator |
| **Subtitles** | Subtitle language · OpenSubtitles account/key · Text size · Style · Color · Font |
| **Parental** | Lock adult · Set/Change PIN · Auto-relock · Hide-vs-blur · Custom keywords · PIN-on-launch |
| **About** | Version · Donate · Feedback · Analytics opt-out |

TV note: the tab strip is above the content; focus enters the strip, DPAD-DOWN drops into the pane,
Back returns to the strip. Each tab is a `TvFocusable` chip with the accent ring (do not regress).

---

## 4a. New settings — kept for v1

**Seam** = the file/constant the setting feeds. **Cost**: S = row + flow, M = row + consumer wiring.
i18n across 21 locales is implied for every one (D8).

### Display
| Setting | Type | Default | Seam | Cost |
|---|---|---|---|---|
| **Theme mode** (Dark / Light / System) | 3-chip | Dark | theme root; add `ThemeMode` enum replacing the bare `isDarkTheme` bool at the read site | M |
| **Clock 12h / 24h** | 2-chip | 24h | replace hardcoded `HH:mm` in `GuideScreen` + `LivePlayerScreen` with a formatter from this pref | S |

### Playback
| Setting | Type | Default | Seam | Cost |
|---|---|---|---|---|
| **Aspect / zoom** (Fit / Fill / Zoom / Stretch) — *finish the orphan* | 4-chip | Fit | wire `videoAspectMode` → ExoPlayer `resizeMode` in `LivePlayerScreen`; add the row | M |
| **Preferred audio language** | picker (reuse `SUBTITLE_LANGUAGES`-style list) | none | `TrackSelectionParameters` default audio at player build (`withAudioTrack` exists) | M |
| **Autoplay next episode + delay** (Off / 5s / 10s) | 3-chip | Off | **build** the auto-advance in `DetailScreen` (currently unwired) driven by `isAutoplayNextEpisode` + this delay | M |

### Subtitles (real ExoPlayer `SubtitleView` knobs)
| Setting | Type | Default | Seam | Cost |
|---|---|---|---|---|
| **Subtitle text size** (S/M/L/XL) | 4-chip | M | `SubtitleView.setFractionalTextSize` | M |
| **Subtitle style** (box / outline / drop-shadow) | 3-chip | box | `SubtitleView.setStyle(CaptionStyleCompat)` | M |
| **Subtitle color** | swatch row (reuse `AccentSwatch`) | white | same `CaptionStyleCompat` | M |
| **Subtitle font** (Default / Sans / Serif / Mono / **Vazirmatn**) | chip row | Default | bundled `res/font/` typeface → `CaptionStyleCompat` typeface (D9) | M |

### General
| Setting | Type | Default | Seam | Cost |
|---|---|---|---|---|
| **Start screen** (Home / Live / Movies / Series / Last used) | picker | Home | shell start destination (`ui/shell`) | M |
| **Auto-refresh catalog on launch** (Off / Daily / Weekly) | 3-chip | Off | scheduled refresh at startup | M |
| **Confirm before exit** | switch | Off | back-press handler at shell root | S |
| **Catalog stale-window** (7 / 14 / 30 days / Off) | 4-chip | 14 days | replace `REFRESH_STALE_MS` const | S |
| **Clear image cache** | action | — | Coil cache clear | S |
| **Clear continue-watching / history** | action (confirm) | — | add clear-all to `ContinueWatchingRepository` | S |
| **Reset to defaults** | action (confirm) | — | clear DataStore keys only (D5) | M |

## 4b. Parental — full depth (D4)

All within the existing "gate the toggle" limit; no per-title schema change.

| Setting | Type | Default | Seam | Cost |
|---|---|---|---|---|
| **Auto-relock after** (Immediately / 15 min / 1 h / Never) | 4-chip | Immediately | timer in the gate check | M |
| **Hide vs blur locked content** | 2-chip | Hide | browse/rail filter that already consults `AdultContentFilter` | M |
| **Custom blocked keywords** | managed list (add/remove chips, reuse pinned-category pattern) | empty | merge a user set into `AdultContentFilter.MARKERS` | M |
| **Require PIN on app launch** | switch | Off | check at shell entry | M |

## 4c. Cut from v1 (in the original superset, dropped here)

Text size / UI scale · Tile density · Buffering profile · Default launch volume · Continue-watching
on/off & list size · Recording-location surfacing · **Export / import settings** (D7). Revisit later
if demand shows up; each defaults to today's behavior so nothing is lost by deferring.

---

## 5. UI primitives

Everything reuses existing controls — chips (D3), swatch row (`AccentSwatch`), managed chip list
(pinned-categories pattern), and action rows with confirm dialogs. **No new primitive.** The tab
strip is a `Row` of `TvFocusable` chips driving a `selectedTab` state; content is a `when(tab)`.

---

## 6. Component / file map

| File | Change |
|---|---|
| `data/settings/UserSettings.kt` | One flow + setter per new pref; add `ThemeMode`, `AspectMode`, subtitle style/size/font enums, start-screen/auto-refresh/stale/auto-relock/relock-scope enums. |
| `ui/settings/SettingsScreen.kt` | Add the top tab strip + `when(tab)` content; move existing sections under their tab; add the new rows. Watch the 500-line limit — split per-tab composables into `ui/settings/tabs/` if it grows. |
| `ui/settings/SettingsViewModel.kt` | Expose new flows + actions (reset, clear-cache, clear-history). |
| `ui/player/LivePlayerScreen.kt` | Consume aspect mode (`resizeMode`), preferred audio, subtitle size/style/color/font. |
| `ui/detail/DetailScreen.kt` | **Build** auto-advance driven by autoplay flag + delay. |
| `ui/guide/GuideScreen.kt` | 12h/24h formatter. |
| `data/repository/ContinueWatchingRepository.kt` | Add clear-all. |
| `data/settings/AdultContentFilter.kt` | Merge user keyword set; hide/blur mode; auto-relock timer. |
| `ui/shell/*` | Start screen, confirm-on-exit, PIN-on-launch. |
| `res/font/` | Bundle Vazirmatn (+ any extra faces), SIL OFL license note. |
| `values/strings.xml` + 21 `values-*/strings.xml` | New keys, all locales in the same change (D8). |

No DB migration. No new DataStore. No new dependency.

### 6.1 Translation glossary (keep terminology consistent, D8)

Reuse these canonical terms across all keys/locales so we don't ship "Zoom" and "Fill" three
different ways:

| Concept | Canonical English | Notes for translators |
|---|---|---|
| Aspect modes | Fit / Fill / Zoom / Stretch | Keep as short verbs; don't expand to sentences. |
| Theme mode | Dark / Light / System | "System" = follow device. |
| Subtitle style | Box / Outline / Shadow | Visual render style, not language. |
| Relock timing | Immediately / 15 min / 1 hour / Never | |
| Locked-content display | Hide / Blur | |
| Clock | 12-hour / 24-hour | |

Prefix all new keys `settings_` and group by tab (`settings_display_…`, `settings_playback_…`,
`settings_parental_…`) so gap-auditing per tab is a clean grep.

---

## 7. Phasing

Each phase is independently shippable; every setting defaults to today's behavior.

**Phase 1 — Tab shell + cheap wins. ✅ DONE.** Built the 6-tab top strip (`SettingsScreen.kt` shell +
`SettingsPanes.kt`) and moved existing sections into tabs. Added: clock 12/24h (`ClockFormat.kt`,
consumed in Guide + player), theme mode Dark/Light/System (`ThemeMode`, resolved in `MainActivity`
via `isSystemInDarkTheme`, effective mode exposed as `AreIptvTheme.isDark`), catalog stale-window
(replaces the hardcoded `REFRESH_STALE_MS`), reset-to-defaults (scoped — keeps sources/credentials/
language/parental), clear image cache (Coil) and clear watch history. Aspect/zoom dropped — already
wired in `380be67` (see §1.1). All 32 new keys translated across the 23 locale dirs (D8).

**Phase 2 — Playback & subtitles depth. ✅ DONE.** Preferred audio language (`setPreferredAudioLanguage`
at the track effect, only when the user hasn't force-picked a track); autoplay-next + delay
(Off/5s/10s) — the base auto-advance is now **built**: on `STATE_ENDED` of a VOD episode with a next
episode, a focus-trapping countdown dialog advances via the existing `switchEpisode(1)`; subtitle
text size/style/color/font applied to `PlayerView.subtitleView` via `CaptionStyleCompat` +
`setFractionalTextSize` (`setApplyEmbeddedStyles(false)` so our style wins). **Vazirmatn was already
bundled** (`res/font/vazirmatn_*.ttf` from the RTL type work) — D9 needed no new asset. 30 new keys
across all 23 locale dirs (10 universal: S/M/L/XL, Sans/Serif/Mono/Vazirmatn, 5s/10s).

**Phase 3 — General & parental. ✅ DONE.** General: **start screen** (Home/Live/Movies/Series/Last
used — resolves the shell inner-NavHost start route; `lastUsedTab` tracked on every tab change),
**auto-refresh on launch** (Off/Daily/Weekly — a once-per-process `LaunchedEffect` in `AreIptvApp`
kicks `refreshSource` when the active catalog is older than the window), **confirm-before-exit**
(gates the existing shell exit dialog; **default On** to preserve today's prompt — turning it off
exits immediately, a deliberate deviation from the spec's "Off" default per principle #5 no-regress).
Full **parental depth** (D4, all four built): a process-scoped runtime unlock (`ParentalGate`) with
**auto-relock** as its session TTL (Immediately = one-shot, 15 min/1 h timed, Never = until relaunch);
**hide-vs-blur** — HIDE drops adult items (today), BLUR keeps them and obscures the tile
(`LocalParentalBlur` + `ParentalLockOverlay` scrim/lock + `Modifier.blur`, reveal-on-tap via PIN) on
the surfaces that already honor the lock (Home/Series/Search — Live/Movies never filtered, left
as-is); **custom keywords** merged into `AdultContentFilter.isAdult` via a single `parentalFilter`
flow the view-models consult; **PIN-on-launch** gate at shell entry. Also (user request): a **What's
new** button on the About > Version row opening a bullet changelog modal. 33 new keys across all 23
locale dirs (D8). No DB migration, no new DataStore.

---

## 8. Explicitly out of scope for v1

- Per-title mature rating (needs a schema change).
- Cloud sync / account-based settings (no backend).
- Settings export/import (D7).
- Multi-playlist management UI (tracked separately).
- Per-source overrides of global settings.
- Text size / tile density / buffering profile / launch volume / CW size (§4c).

<!-- POLISH BACKLOG (fix after final phase, per user 2026-07-24):
  - Settings tab strip: top of the chip / focus ring is clipped at the screen top edge (needs top padding/headroom).
  - First content row under the strip is clipped at its top edge (same headroom issue).
-->
