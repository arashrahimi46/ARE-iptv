# Settings Enrichment — v1 Design

> Goal: make are-iptv **fully customizable** by growing the Settings surface to cover the knobs our
> existing features already imply. This spec is grounded in the **current** code — every proposal
> names the real seam it plugs into, so nothing here is speculative UI with no backing feature.
>
> Read this before building any settings work. Decisions in the locked table were made deliberately.

---

## 1. Where we are today (inventory)

All settings live in `UserSettings` (`data/settings/UserSettings.kt`, a Preferences DataStore) and
are presented by `ui/settings/SettingsScreen.kt`, grouped into sections:

| Section | Controls today |
|---|---|
| Playlists | Refresh catalog now (+ stale/last-updated) |
| Provider | Read-only Xtream account panel (Xtream only) |
| Appearance | Dark theme, per-mode Accent, Reduce motion, List view |
| Language | App language |
| Playback | Hardware decoding, Autoplay next episode, Mini-player behavior, Recording indicator |
| Parental | Lock adult content, Set/Change PIN |
| Subtitles | Subtitle language, OpenSubtitles key + account |
| Metadata | OMDb key |
| About | Version, Donate, Feedback, Analytics opt-out |

Also user-configurable, but **outside** the Settings screen (in-context): Home rail layout
(`ui/home`), pinned categories per browse screen, multi-view channel set, favorites, guide category.

### 1.1 Findings that shape this spec

- **Orphan setting — `videoAspectMode`.** `UserSettings` stores it (default `"FIT"`) with a
  `setVideoAspectMode(...)` setter, but there is **no Settings row and no player consumer** (grep:
  zero readers outside `UserSettings.kt`). An aspect/zoom control is half-built — finishing it is the
  cheapest high-value win here.
- **Hardcoded constants that are really settings:**
  - Continue-watching cap `MAX_ENTRIES = 15` (`ContinueWatchingRepository`).
  - Catalog "stale" window `REFRESH_STALE_MS = 14 days` (`SettingsScreen`).
  - EPG/HUD clock hardcoded to 24h `HH:mm` (`GuideScreen`, `LivePlayerScreen`).
  - Adult classification is heuristic-only (`AdultContentFilter`) with no user override list.
- **Parental lock is coarse.** It gates the *toggle*, not individual titles (schema has no per-title
  mature flag) — noted in `SettingsScreen` class doc. In-scope refinements below stay within that
  limit (they don't require the schema change).
- **No backup/export, no reset, no per-source management** in Settings — the class doc calls these
  intentionally deferred. Some are cheap now and worth reconsidering (see Phase 3).

---

## 2. Design principles (locked)

1. **Feature-backed only.** A new setting ships only when a real feature reads it. No storage-only
   toggles (the `videoAspectMode` orphan is the anti-pattern we're paying down, not repeating).
2. **Reuse the existing shape.** New rows use `SettingsRow` + `AreSwitch`/`AreChip`/
   `SelectionChangeControl`; new groups use `SettingsSection`. No new UI primitives unless a control
   type genuinely doesn't exist yet (only the number-stepper does — §5).
3. **Live-apply, no restart.** Follow the theme/reduced-motion precedent: settings are DataStore
   flows read at the consumer, so changes take effect without an app restart. Language is the one
   documented exception (Activity recreate).
4. **Every string is translated.** Any new user-facing string goes into `values/strings.xml` **and
   all 21 `values-*/strings.xml`** (see CLAUDE.md i18n rules). This is the dominant per-setting cost.
5. **Sensible defaults = current behavior.** Every new setting defaults to exactly what the app does
   today, so existing users see no change until they opt in.

---

## 3. Locked decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | Settings stay in one DataStore (`UserSettings`); no new store, no DB migration. | Every proposal is a scalar/enum/string pref. Schema stays as-is. |
| D2 | Finish `videoAspectMode` (wire it) **before** adding any new playback pref. | Pay down the orphan; it's the cheapest win and validates the enum-chip pattern. |
| D3 | Numeric prefs (buffer, cap, PIN timeout) use a discrete **chip set**, not a free text field or slider. | D-pad friendly, no IME, matches TV conventions and the existing chip rows. |
| D4 | Parental scope stays "gate the toggle" for v1; add **auto-relock timeout** and **hide vs blur** only — no per-title schema change. | Respects the documented schema limit; still meaningfully more configurable. |
| D5 | Backup/export ships as **local file export/import** (SAF), not cloud. | No backend; recordings already use SAF tree URIs, so the pattern exists. |
| D6 | A **"Reset to defaults"** action clears DataStore keys but never touches sources/DB/recordings. | Safe, reversible-in-spirit, and users expect it. |
| D7 | Group new controls into existing sections where they fit; add at most **two** new sections (Data & storage, General). | Avoid a wall of sections. |

---

## 4. Proposed settings

Grouped by section. **Seam** = the file/constant the setting feeds. **Cost** = rough size
(S = new row + flow, M = row + light consumer wiring, L = new consumer behavior). i18n add is implied
for every one.

### 4.1 Appearance
| Setting | Type | Default | Seam | Cost |
|---|---|---|---|---|
| **Follow system theme** (Dark / Light / System) | 3-chip | Dark (today) | `MainActivity` theme root; add `ThemeMode` enum replacing the bare `isDarkTheme` bool at the read site | M |
| **Text size / UI scale** (S / M / L) | 3-chip | M | `AreIptvTheme.typography` multiplier at the theme root | M |
| **Tile density** (Comfortable / Compact) | 2-chip | Comfortable | browse grid column count in `ui/browse` | M |
| **Show clock in 24h / 12h** | 2-chip | 24h | replace hardcoded `HH:mm` in `GuideScreen` + `LivePlayerScreen` with a formatter chosen from this pref | S |

### 4.2 Playback (biggest opportunity)
| Setting | Type | Default | Seam | Cost |
|---|---|---|---|---|
| **Aspect / zoom** (Fit / Fill / Zoom / Stretch) — *finish the orphan* | 4-chip | Fit | wire `videoAspectMode` → ExoPlayer `resizeMode` in `LivePlayerScreen`; add the missing Settings row | M |
| **Buffering profile** (Low latency / Balanced / Smooth) | 3-chip | Balanced | maps to a `DefaultLoadControl` buffer preset in the player's `ExoPlayer.Builder` (none set today) | M |
| **Default volume on launch** | stepper | last-used | player volume init | S |
| **Preferred audio language** | picker (reuse `SUBTITLE_LANGUAGES`-style list) | none | `TrackSelectionParameters` default audio at player build (there's already `withAudioTrack`) | M |
| **Autoplay next episode delay** (Off / 5s / 10s) | 3-chip | 10s | pairs with the existing `isAutoplayNextEpisode`; feeds the (still-unwired) auto-advance in `DetailScreen` | M |
| **Remember playback position** (continue-watching on/off) | switch | On | gate `ContinueWatchingRepository.updateProgress` | S |
| **Continue-watching list size** (10 / 15 / 25) | 3-chip | 15 | replace `MAX_ENTRIES` const with this pref | S |

### 4.3 Subtitles (styling — real ExoPlayer `SubtitleView` knobs)
| Setting | Type | Default | Seam | Cost |
|---|---|---|---|---|
| **Subtitle text size** (S/M/L/XL) | 4-chip | M | `SubtitleView.setFractionalTextSize` on the player's caption view | M |
| **Subtitle style** (background box / outline / drop-shadow) | 3-chip | box | `SubtitleView.setStyle(CaptionStyleCompat)` | M |
| **Subtitle color** | swatch row (reuse `AccentSwatch` pattern) | white | same `CaptionStyleCompat` | M |
| **Auto-load online subtitles** when embedded absent | switch | Off | gate the auto-search in `SubtitleMenu`/player | S |

### 4.4 Parental (within D4 limits)
| Setting | Type | Default | Seam | Cost |
|---|---|---|---|---|
| **Auto-relock after** (Immediately / 15 min / 1 h / Never) | 4-chip | Immediately | timer in the gate check | M |
| **Hide vs blur locked content** | 2-chip | Hide | browse/rail filter that already consults `AdultContentFilter` | M |
| **Custom blocked keywords** | managed list (add/remove chips) | empty | extend `AdultContentFilter.MARKERS` with a user set | M |
| **Require PIN on app launch** | switch | Off | check at shell entry | M |

### 4.5 General (new section)
| Setting | Type | Default | Seam | Cost |
|---|---|---|---|---|
| **Start screen** (Home / Live / Movies / Series / last used) | picker | Home | shell start destination (`ui/shell`) | M |
| **Catalog "stale" reminder window** (7 / 14 / 30 days / Off) | 4-chip | 14 days | replace `REFRESH_STALE_MS` const | S |
| **Auto-refresh catalog on launch** (Off / Daily / Weekly) | 3-chip | Off | scheduled refresh at startup | L |
| **Confirm before exit** | switch | Off | back-press handler at shell root | S |

### 4.6 Data & storage (new section)
| Setting | Type | Default | Seam | Cost |
|---|---|---|---|---|
| **Recording storage location** | folder picker (SAF) | app default | already partly present in `RecordingsViewModel` (`storageTreeUri`) — surface it here | M |
| **Clear image cache** | action | — | Coil cache clear | S |
| **Clear continue-watching / history** | action | — | `ContinueWatchingRepository.clear` (add clear-all) | S |
| **Export settings** (to file, SAF) | action | — | serialize DataStore → JSON, write via SAF (D5) | M |
| **Import settings** (from file, SAF) | action | — | read JSON → DataStore | M |
| **Reset to defaults** | action (confirm dialog) | — | clear DataStore keys only, never DB/sources/recordings (D6) | M |

---

## 5. One new UI primitive

Everything reuses existing controls **except** a small **`SettingsStepper`** for bounded numeric
prefs (default volume). Discrete chip sets (D3) cover the rest, so the stepper is optional — if we
want zero new primitives, express volume as a 4-chip set (25/50/75/100%) too. **Recommendation:**
skip the stepper; use chips everywhere for D-pad consistency.

Managed-list controls (custom blocked keywords) reuse the `AreChip` + add/remove pattern already
used by pinned categories.

---

## 6. Component / file map

| File | Change |
|---|---|
| `data/settings/UserSettings.kt` | Add one flow + setter per new pref; add `ThemeMode`, `AspectMode`, buffering/subtitle enums. |
| `ui/settings/SettingsScreen.kt` | New rows in existing sections; add General + Data & storage sections. |
| `ui/settings/SettingsViewModel.kt` | Expose the new flows/actions (export/import/reset/clear). |
| `ui/player/LivePlayerScreen.kt` | Consume aspect mode (`resizeMode`), buffering preset, default volume, preferred audio, subtitle styling. |
| `ui/guide/GuideScreen.kt` | 12h/24h formatter. |
| `data/repository/ContinueWatchingRepository.kt` | Cap + on/off + clear-all from prefs. |
| `data/settings/AdultContentFilter.kt` | Merge user keyword set; hide/blur mode. |
| `ui/shell/*` | Start screen, confirm-on-exit, PIN-on-launch. |
| `values/strings.xml` + 21 `values-*/strings.xml` | New keys (dominant cost). |

No DB migration. No new DataStore. No new dependency.

---

## 7. Phasing

**Phase 1 — Pay down + cheap wins (highest value / lowest risk).**
Finish `videoAspectMode` (D2). Add: 12h/24h clock, continue-watching cap + on/off, follow-system
theme, text size, stale-window, reset-to-defaults. All S/M, all backed by existing seams.

**Phase 2 — Playback & subtitles depth.**
Buffering profile, default volume, preferred audio, subtitle text size/style/color, auto-load
online subs, autoplay delay. Needs player wiring but no schema/behavior invention.

**Phase 3 — General, parental, data.**
Start screen, auto-refresh on launch, confirm-on-exit, parental auto-relock / hide-vs-blur / custom
keywords / PIN-on-launch, recording location surfacing, cache/history clear, settings export/import.

Each phase is independently shippable and each setting defaults to today's behavior (P5/D-defaults),
so partial rollout is safe.

---

## 8. Explicitly out of scope for v1

- Per-title mature rating (needs the schema change the parental doc calls out).
- Cloud sync / account-based settings (no backend).
- Multi-playlist management UI (tracked separately; Settings class doc defers it).
- Per-source overrides of global settings (settings stay global for v1).
- Themable custom color ramps beyond the existing accent presets.
