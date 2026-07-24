# Catch-up / Archive — v1 design

> **Status:** ✅ **implemented** (phases 1–6). All three sources (Xtream, M3U, Stalker), the Guide
> glyph + action menu, and the player HUD (⟲ pill, Go Live, ⏮/⏭ program-hop) are built and
> unit-tested; strings are localized across all 24 locales. Remaining: on-device validation against a
> real archive-enabled Xtream provider and a real Ministra portal (the `&start=` param shape is
> to-be-confirmed) — deferred because provider probing from the shell is forbidden (repo rule).
> Sequenced as the #1 roadmap item per
> `docs/competitive-analysis-2026.md` (the single feature 10/10 rivals ship and we don't).
> **Schema impact:** one real migration **v11 → v12** (three columns on `Channel`).
> **Player identity:** catch-up is a *live channel, rewound* — it plays through the existing
> `LivePlayerScreen` on the VOD/timeshift seek path, not a new player.

## What it is

Watch a live-TV programme that already aired. The **provider** records its channels on a rolling
window (typically 1–7 days); the app asks the server for "channel X as it was at time T" and plays it
back as a seekable stream. We store nothing — we only mint the right URL at press-time.

Catch-up and the EPG are joined at the hip: the **Guide is how you pick** a past programme, and the
**programme's start + duration is what we send** to the server.

## Locked decisions

| # | Decision | Choice | Why |
|---|---|---|---|
| D1 | Source coverage | **All three: Xtream + Stalker + M3U** | Full coverage in one release; no source left as a second-class citizen. |
| D2 | Seek model | **Full timeshift + program-hop** | Scrubbable archive, ⏮/⏭ across held EPG, snap to live. Reuses the VOD seek bar. |
| D3 | Past-programme source | **Rely on the feed's past EPG data** | No new retention logic. Hop range = whatever the XMLTV feed carries (usually 1–2 days back). |
| D4 | Guide cell action | **Action menu** ("Watch from start" / "Go to live" / "Record — SOON") | Discoverable + extensible into DVR. Focus-trapped `Dialog`, default focus on Watch-from-start. |
| D5 | Availability signal | **Per-cell ⟲ glyph** | Shows exactly which cells are playable, not just "this channel has archive". |
| D6 | Glyph precision | **Only within the provider's archive window** | The glyph never lies. Requires capturing the window (days) per channel. |
| D7 | Return to live | **Seek to the live edge + a "Go Live" button** | Matches the timeshift mental model; reuses existing transport. |
| D8 | Current (now-airing) cell | **"Watch from start" restarts the in-progress programme** (classic Start-Over) | Same archive mechanism; high-value; Go Live returns to the edge. |
| D9 | Failure behavior | **Clear message, stay put** — "This programme isn't available to catch up." | Honest, non-disruptive; mirrors how Stalker `create_link` failures surface today. No silent live fallback. |
| D10 | Primary surface | **Guide-only for v1** | The Yesterday/past-day filter already exists; least new UI, ships the engine fastest. |
| D11 | v1 player extras | **⏮/⏭ program-hop + disabled "Record — SOON" menu seam** | Hop is in scope; the greyed Record item advertises the DVR seam and proves the menu extends. |
| D12 | Monetization | **Free, no paywall** | On-strategy: TiviMate gates catch-up behind Premium; we give it away. |

**Explicit fast-follows (not v1):** per-channel catch-up list (surface B), continue-watching resume for
archive programmes, deliberate past-EPG retention (to hop further back than the feed carries), and a
Home "Catch up on…" rail.

## Architecture — extend the resolve-on-play seam

Catch-up URL minting lives in exactly one place we already own: the `StreamUrlResolver`
(`data/player/StreamUrlResolver.kt`) — the same seam that mints Stalker `create_link` URLs at
press-time. Today it has one caller (`LivePlayerViewModel.resolvePlayUrl`), so the change is contained.

### New: `PlaybackSource.Catchup`

`ui/player/LivePlayerViewModel.kt` — add to the sealed type:

```kotlin
data class Catchup(
    val channelId: Long,
    val programStartMs: Long,   // epoch ms — the aired programme's start
    val programEndMs: Long,     // epoch ms — used for duration + program-hop bounds
) : PlaybackSource()
```

Nav route in `MainActivity.kt`, alongside `player/{channelId}`:

```
"player/catchup/{channelId}/{startMs}/{endMs}"   // all NavType.LongType
```

### Extended resolver signature

Add one optional parameter — no behaviour change when it's null (M3U/Xtream identity and Stalker live
paths are untouched):

```kotlin
data class CatchupRequest(
    val startMs: Long,
    val endMs: Long,
    val m3uSource: String?,   // Channel.catchupSource — M3U template; null for Xtream/Stalker
    val m3uType: String?,     // Channel.catchupType   — M3U convention; null otherwise
)

suspend fun resolve(
    source: PlaylistSource,
    kind: StreamKind,          // stays LIVE for catch-up (it's a live channel, rewound)
    externalId: String?,
    storedUrl: String?,
    series: Int? = null,
    catchup: CatchupRequest? = null,   // NEW
): String
```

`DefaultStreamUrlResolver` branches on `catchup != null` **before** the existing identity/Stalker logic,
dispatching per `source.type`. Failure throws `StreamResolveException` → the ViewModel surfaces the D9
message; **no live fallback** (D9).

## Per-source URL construction

The raw material everywhere is the same: `startMs`, `durationMin = (endMs − startMs) / 60_000`.

### Xtream — closest to ready

Mirror the existing `XtreamClient.streamUrl(...)` builder (`data/parser/XtreamClient.kt:129`). Add:

```kotlin
// {base}/timeshift/{user}/{pass}/{durationMin}/{YYYY-MM-DD:HH-MM}/{streamId}.{ext}
fun timeshiftUrl(streamId: String, durationMin: Int, startLocal: String, ext: String = "m3u8"): String =
    "${baseUrl()}/timeshift/$username/$password/$durationMin/$startLocal/$streamId.$ext"
```

- `streamId` = `Channel.externalId` (already the Xtream `stream_id`).
- `startLocal` = the programme start formatted `yyyy-MM-dd:HH-mm` in the **provider's timezone** (see
  Resilience — this is the #1 correctness trap).
- Capture per channel: `tv_archive` (0/1) and `tv_archive_duration` (days) — **not read today**; add to
  `XtreamLiveStream` + `getLiveStreams()` (`XtreamClient.kt:15,173`).

### Stalker — highest protocol uncertainty

Extend `StalkerClient.createLink(...)` (`data/parser/StalkerClient.kt:248`) to carry the programme start
so the portal mints an archive link:

```kotlin
// type=itv&action=create_link&cmd=<cmd>&... plus, for archive:
//   &start=<epochSeconds>   (Ministra archive convention)
```

The `extra` string is built inline (`StalkerClient.kt:250`), so this is a localized change: when an
archive start is supplied, append `&start=<epoch>` (and preserve the channel's `tv_genre_id`, currently
dropped after import — thread it onto `StalkerChannel`/`Channel` if the target portal needs it).
Archive-window days come from the portal channel payload (`archive` / `archive_range`) when present.

> ⚠️ Stalker archive is the **least standardized** of the three and the hardest to validate (proprietary
> Ministra builds, ionCube device-auth — see the Stalker doc). Treat the exact param shape as
> **to-be-confirmed on a real portal**; ship behind the same graceful D9 failure so an unsupported portal
> degrades to a clear message, never a crash. Do **not** circumvent device-auth to test.

### M3U — most new code

Nothing catch-up-related is parsed today (`M3uParser.kt` drops `catchup*`/`tvg-rec`). Add:

1. **Parse** on `#EXTINF`: `catchup` / `catchup-type`, `catchup-source`, `catchup-days`, `tvg-rec`
   → new `M3uEntry` fields (`M3uParser.kt:4`).
2. **Persist** into new `Channel` columns at import (`PlaylistRepository.kt:484`).
3. **Expand** a template at play-time — a new pure function (no analog exists in the codebase):

```kotlin
// CatchupTemplate.kt — expand catchup-source placeholders against a programme window.
// Supported types: default/shift, append, flussonic, xc (Xtream-style).
// Placeholders: {utc} {start} {lutc} {utcend} {end} {duration} {offset}
//               {Y}{m}{d}{H}{M}{S}  and ${…} variants.
fun expandCatchup(liveUrl: String, source: String?, type: String?, startMs: Long, endMs: Long, nowMs: Long): String
```

- `default`/`shift`: append the `catchup-source` template (or `?utc={start}&lutc={now}` if none).
- `append`: concatenate `catchup-source` onto the live URL.
- `flussonic`: rewrite `.../mono.m3u8` → `.../mono-{utc}-{duration}.m3u8` style.
- `xc`: build the Xtream `/timeshift/...` form from the live URL parts.

## Data model & migration

**`Channel`** (`data/model/Entities.kt:65`) gains three columns:

```kotlin
val catchupDays: Int = 0,        // 0 = no archive; >0 = window length in days (drives capability + glyph)
val catchupSource: String? = null, // M3U catchup-source template; null for Xtream/Stalker
val catchupType: String? = null,   // M3U catchup-type; null otherwise
```

`catchupDays > 0` **is** the capability flag (D6) — one field powers both "does this channel have
archive" and "how far back is the glyph allowed". Populated per source:

| Source | `catchupDays` | `catchupSource` / `catchupType` |
|---|---|---|
| Xtream | `tv_archive_duration` (0 if `tv_archive` = 0) | null |
| Stalker | portal `archive` window (0 if absent) | null |
| M3U | `catchup-days` (default e.g. 7 if `catchup` present without days) | the parsed template + type |

**Migration `MIGRATION_11_12`** (`data/db/AppDatabase.kt`, bump `version = 12`) — follow the existing
`MIGRATION_5_6` pattern; three `ALTER TABLE channels ADD COLUMN … DEFAULT …`. The repo deliberately
removed `fallbackToDestructiveMigration`, so a real migration is mandatory — no shortcut.

## EPG & the availability rule (D5, D6)

- **EPG rows are unchanged** — `EPGProgram(channelId, title, startMs, endMs, description)` already carries
  what we need (`Entities.kt:142`). Duration = `endMs − startMs`.
- **Glyph rule:** in `GuideScreen`, pass `catchup = true` to `AreGuideCell` (param already exists,
  `GuideCell.kt:54`) **iff** all hold:
  1. `channel.catchupDays > 0`, and
  2. the cell's `endMs < now` (already aired) — or it's the now-airing cell for Start-Over (D8), and
  3. `cell.startMs >= now − catchupDays·24h` (inside the window).
- **Past-programme availability** still depends on the feed carrying those rows (D3). Capability (glyph
  window) and the actual hop targets are therefore two independent things — a channel can be
  archive-capable for 7 days while the Guide only holds 2 days of rows to land on. Acceptable v1 boundary.
- The Guide's **Yesterday** day filter (`GuideViewModel.GuideDay.Yesterday`) is the past-browsing surface —
  no new screen (D10).

## UX flows

**Pick → menu → play (D4):**
`GuideScreen` cell `onClick` currently just plays live (`GuideScreen.kt:203`). Replace with: if the cell
is catch-up-eligible, open the **action menu dialog** (focus-trapped, like `HomeAddSectionDialog`):
- **Watch from start** (default focus) → for an ended programme, navigate
  `player/catchup/{channelId}/{startMs}/{endMs}`; for the now-airing cell, same route with that
  programme's start (Start-Over, D8).
- **Go to live** → existing `player/{channelId}`.
- **Record — SOON** → present but disabled (D11); the seam for future DVR.

Non-eligible cells (live-only or out-of-window) keep today's behaviour: press → live.

**In the player:**
- Enters on the **VOD/timeshift seek path** (scrubbable), not the live no-scrub mode
  (`LivePlayerScreen.kt:837` already distinguishes these).
- Play head starts at `programStartMs`; the **live edge** is the right end of the bar with a red "LIVE"
  marker. Scrub to it, or press **Go Live** → switches to the live channel (D7).
- **⏮ / ⏭** hop to the previous/next `EPGProgram` for the channel within held EPG (D2, D11); disabled at
  the ends of what we hold.
- Title HUD shows a **⟲ CATCH-UP** pill + "aired {day} {time}".

## Component / file map

| Area | File | Change |
|---|---|---|
| Play seam | `data/player/StreamUrlResolver.kt` | `CatchupRequest`; `catchup` param on `resolve`; per-type minting branch |
| Xtream | `data/parser/XtreamClient.kt` | `timeshiftUrl(...)`; read `tv_archive`/`tv_archive_duration` into `XtreamLiveStream` + `getLiveStreams()` |
| Stalker | `data/parser/StalkerClient.kt` | archive `&start=` in `createLink`; capture archive window; (maybe) preserve `tv_genre_id` |
| M3U parse | `data/parser/M3uParser.kt` | parse `catchup*`/`tvg-rec` into `M3uEntry` |
| M3U expand | `data/parser/CatchupTemplate.kt` *(new)* | `expandCatchup(...)` — pure, unit-tested |
| Import | `data/repository/PlaylistRepository.kt` | persist catchup fields per source (M3U `Channel` build + Xtream/Stalker mappers) |
| Schema | `data/model/Entities.kt`, `data/db/AppDatabase.kt` | 3 `Channel` columns; `MIGRATION_11_12`; `version = 12` |
| Playback source | `ui/player/LivePlayerViewModel.kt` | `PlaybackSource.Catchup`; load channel + program; pass `CatchupRequest`; program-hop logic |
| Nav | `MainActivity.kt` | `player/catchup/{channelId}/{startMs}/{endMs}` route |
| Guide | `ui/guide/GuideScreen.kt`, `GuideViewModel.kt` | glyph rule; action-menu dialog; eligible-cell routing |
| Guide cell | `ui/components/GuideCell.kt` | (already has `catchup` param — just wire it) |
| Player HUD | `ui/player/LivePlayerScreen.kt`, `ui/components/PlayerControls.kt` | ⟲ pill, Go Live button, ⏮/⏭ hop, seek-to-live-edge |
| Strings | `values/strings.xml` + 23 locales | menu items, ⟲ desc, failure message, Go Live, "aired …" (per i18n rules) |
| Tests | `data/parser` test dir | `CatchupTemplate` expansion cases; Xtream `timeshiftUrl` format; glyph-window predicate |

## Resilience & variance notes

- **Timezone is the #1 trap (Xtream).** The `/timeshift/…/{YYYY-MM-DD:HH-MM}/…` start is interpreted in
  the **provider's** timezone, not the device's. EPG `startMs` is UTC epoch. Format the start using the
  provider's offset (derive from the XMLTV `date`/programme offsets or a per-source setting) — a wrong
  offset yields the right programme shifted by hours. Validate on-device against a known archive channel.
- **Window boundaries are fuzzy.** Providers trim the oldest edge continuously; a cell that was in-window
  at guide-render can 404 at press. D9 covers it — clear message, stay put. Don't cache "known good".
- **EPG gaps / missing rows.** No `EPGProgram` for a slot → no catch-up target there; the cell simply
  isn't eligible. Program-hop skips gaps by moving to the next existing row.
- **Duration rounding.** `durationMin` from `endMs − startMs`; clamp to ≥1 and round up so the tail of a
  programme isn't cut. Some providers ignore duration and stream to live — fine.
- **Stalker uncertainty** — see the Stalker subsection; ship behind D9, confirm on a real portal, never
  circumvent device-auth.
- **No provider probing from the shell** (repo rule): validate all archive URLs **on-device** in
  ExoPlayer, never via `curl`/`wget` — a stray connection can get the IP rate-limited and kill the live
  stream inside the app.

## Out of scope (v1)

Scheduled/series DVR (separate roadmap item), per-channel catch-up list (surface B, fast-follow),
continue-watching resume for archive, past-EPG retention beyond the feed, Home "Catch up on…" rail,
DVR of a catch-up stream (record-now on archive).

## Phasing

1. **Data + Xtream** — 3 `Channel` columns + `MIGRATION_11_12`; capture `tv_archive*`; `timeshiftUrl`;
   `PlaybackSource.Catchup` + route; resolver branch. Ship the whole engine on the easiest source; validate
   on-device (timezone!).
2. **Guide UX** — glyph rule, action-menu dialog, eligible-cell routing, Start-Over on the now-cell.
3. **Player** — timeshift seek entry, Go Live, ⏮/⏭ hop, ⟲ pill, disabled Record seam.
4. **M3U** — parse `catchup*`, `CatchupTemplate.expandCatchup` (+ unit tests), import persistence.
5. **Stalker** — archive `create_link`; validate against a real portal; graceful degrade.
6. **i18n + polish** — strings across 24 locales, failure states, on-device pass per source.
