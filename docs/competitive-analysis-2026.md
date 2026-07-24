# Competitive Analysis — ARE iptv vs the Android TV IPTV field

> Snapshot: **July 2026**. App audited: `com.arashrahimi46.iptv` **v1.4.2** (versionCode 7).
> Interactive version (color-coded matrix): https://claude.ai/code/artifact/fe1a9752-4241-4768-aa97-ce210699db70
>
> This is a strategy artifact, not a design doc. It exists to feed the product roadmap.
> Feature designs still live in `docs/*-v1-design.md`.

## Verdict

A polished, source-flexible player that is **one table-stakes feature cluster away from the front row**.
On playback craft, localization, subtitles and source breadth we already lead or match the best in the
field. The exposure is concentrated in a small cluster of **time-of-broadcast** features — catch-up,
timeshift, scheduled DVR — that nearly every rival ships and we do not yet.

| | Count | What |
|---|---|---|
| **Areas we lead** | 9 | Online subtitles, 24-language reach, subtitle styling, audio-sync, all-in-one source support, theming, free/ad-free model |
| **At parity** | 14 | Live / VOD / series, EPG grid, multi-view, favorites, continue-watching, parental PIN, aspect & audio control, record-now, metadata |
| **Gaps to close** | 8 | Catch-up, timeshift, scheduled recording, cast-out, external-player handoff, true PiP, backup/sync, auto-refresh |

## Positioning — premium challenger to TiviMate

TiviMate wins on catch-up, scheduled recording and a polished guide — but **gates all three behind
Premium** and stays a live-TV-first app with thin VOD, no online subtitles, and modest localization.

Our wedge is a **complete, unpaywalled player**: TiviMate-grade browsing plus real movies/series,
OpenSubtitles, 24 languages, and Stalker + Xtream + M3U in one app. Closing the catch-up/DVR cluster
removes the only reason a power user picks TiviMate over us — while everything they'd pay Premium for,
we give away.

## Benchmark set

TiviMate · IPTV Smarters Pro · OTT Navigator · Sparkle TV · Televizo · XCIPTV · Purple Player ·
GSE Smart IPTV · Perfect Player · Kodi + PVR IPTV Simple.

## Feature matrix

Legend: ● full · ◐ partial/limited · **$** behind a paywall · ○ not supported.

| Feature | **ARE** | TiviMate | Smarters | OTT Nav | Sparkle | Televizo | XCIPTV | Purple | GSE | Perfect | Kodi |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **Sources** |
| M3U / M3U8 | ● | ● | ● | ● | ● | ● | ● | ● | ● | ● | ● |
| Xtream Codes API | ● | ● | ● | ● | ● | ● | ● | ● | ● | ● | ○ |
| Stalker / Ministra | ● | ○ | ◐ | ○ | ● | ● | ● | ● | ◐ | ● | ○ |
| Direct stream URL | ● | ○ | ● | ● | ○ | ◐ | ◐ | ◐ | ◐ | ◐ | ◐ |
| Multiple playlists | ● | ● | ● | ● | ● | ● | ● | ● | ● | ● | ● |
| **Content types** |
| Live TV | ● | ● | ● | ● | ● | ● | ● | ● | ● | ● | ● |
| Movies (VOD) | ● | ◐ | ● | ● | ● | ◐ | ● | ● | ◐ | ○ | ◐ |
| Series & episodes | ● | ◐ | ● | ● | ● | ◐ | ● | ● | ○ | ○ | ◐ |
| Catch-up / archive | ○ | **$** | ● | ● | ● | ● | ● | ● | ● | ◐ | ● |
| Timeshift (pause live) | ○ | ◐ | ◐ | ● | ● | ◐ | ◐ | ◐ | ○ | ○ | ● |
| **Recording / DVR** |
| Record now (live) | ● | **$** | ◐ | ● | ● | ○ | ◐ | ● | ○ | ○ | ● |
| Scheduled / series DVR | ○ | **$** | ○ | ● | ● | ○ | ◐ | ◐ | ○ | ○ | ● |
| **Guide (EPG)** |
| XMLTV EPG | ● | ● | ● | ● | ● | ● | ● | ● | ● | ● | ● |
| Full guide grid | ● | ● | ● | ● | ● | ● | ● | ● | ◐ | ● | ● |
| Xtream native EPG | ● | ● | ● | ● | ● | ● | ● | ● | ● | ◐ | ○ |
| **Playback** |
| Multi-view | ● | **$** | ● | ● | ● | ○ | ● | ◐ | ○ | ○ | ○ |
| Aspect-ratio control | ● | ● | ● | ● | ● | ◐ | ● | ● | ◐ | ● | ● |
| Audio-track selection | ● | ● | ● | ● | ● | ● | ● | ● | ◐ | ● | ● |
| Online subtitles | ● | ○ | ○ | ○ | ○ | ○ | ○ | ○ | ○ | ○ | ◐ |
| Subtitle styling | ● | ○ | ◐ | ● | ◐ | ◐ | ◐ | ◐ | ◐ | ◐ | ● |
| Audio-sync / delay | ● | ○ | ◐ | ● | ○ | ○ | ◐ | ◐ | ○ | ○ | ● |
| Hardware decode toggle | ● | ● | ● | ● | ● | ◐ | ● | ● | ◐ | ● | ● |
| External-player handoff | ○ | ○ | ● | ● | ◐ | ◐ | ● | ◐ | ◐ | ● | ◐ |
| True system PiP | ◐ | ○ | ● | ◐ | ◐ | ◐ | ◐ | ◐ | ● | ○ | ◐ |
| **Cast & connectivity** |
| Chromecast / cast-out | ○ | ○ | ● | ○ | ○ | ○ | ● | ● | ● | ○ | ◐ |
| DLNA / AirPlay | ○ | ○ | ◐ | ○ | ○ | ○ | ◐ | ◐ | ● | ○ | ◐ |
| **Organization** |
| Favorites | ● | ● | ● | ● | ● | ● | ● | ● | ● | ● | ● |
| Continue watching | ● | ◐ | ● | ● | ● | ◐ | ● | ● | ◐ | ○ | ◐ |
| Search | ● | ◐ | ● | ● | ● | ● | ● | ● | ● | ◐ | ● |
| Customizable home | ● | ● | ◐ | ● | ◐ | ◐ | ◐ | ◐ | ◐ | ○ | ◐ |
| Parental PIN + adult filter | ● | ◐ | ● | ● | ◐ | ● | ● | ● | ● | ○ | ◐ |
| **Metadata & platform** |
| VOD ratings / metadata | ● | ○ | ● | ◐ | ◐ | ◐ | ● | ◐ | ◐ | ○ | ◐ |
| Localization breadth | ● | ◐ | ● | ● | ◐ | ◐ | ◐ | ◐ | ◐ | ◐ | ● |
| Light / dark / system theme | ● | ◐ | ◐ | ◐ | ◐ | ◐ | ◐ | ● | ● | ◐ | ◐ |
| Backup / restore / sync | ○ | ◐ | ◐ | ◐ | ◐ | ● | ◐ | ◐ | ◐ | ◐ | ◐ |
| Free · no ads · no paywall | ● | **$** | ◐ | ◐ | ◐ | ● | ◐ | ◐ | ◐ | ◐ | ● |

## Moats — where we already lead

- **Online subtitles built in** — OpenSubtitles search + download, sideload, full styling. Effectively
  unique; no rival in the set ships it natively (10/10 lack it).
- **24-language localization** — full translations, RTL, bundled Persian font. Only Kodi/OTT come close.
- **All-in-one source support** — M3U + Xtream + Stalker + direct URL together. TiviMate has no Stalker;
  Kodi has no Xtream; we cover every provider shape.
- **Audio-sync & subtitle craft** — ±ms A-V delay, per-channel aspect memory, 4-axis subtitle styling.
  Depth only OTT Navigator matches.
- **Genuinely free, ad-free** — no paywall on catch-up-class features, no ad SDK. Undercuts TiviMate
  Premium and the ad-supported Smarters/GSE/XCIPTV.
- **Polished TV UX** — D-pad focus system, reorderable home rails, light/dark/system + accents,
  encrypted credential storage.

## Exposure — gaps ranked by how universal they are

| Gap | % of rivals with it | Why it matters |
|---|---|---|
| **Catch-up / archive** | 100% | Most universal feature we lack — every rival offers it (TiviMate gates it to Premium). The #1 reason a power user would choose a competitor. |
| Timeshift (pause live) | 60% | Pause/rewind live. Expected alongside catch-up; OTT, Sparkle, Kodi do it fully. |
| External-player handoff | 50% | Open in MX/VLC when our decoder struggles. Common power-user escape hatch. |
| Scheduled / series DVR | 45% | EPG-timed & series recording. We record-now only. |
| Chromecast / cast-out | 40% | Throw to another screen. Smarters, XCIPTV, Purple, GSE support it. |
| Backup / restore / sync | 40% | Export & move to a new device. Televizo sells on this. |
| True system PiP | 35% | We have a mini-player, not an OS floating window. |
| Auto catalog refresh | 30% | Scheduled background refresh; today it's manual. |

## Roadmap tiers (ordered by competitive cost of *not* having it, not build effort)

**P0 — table stakes we're missing** (where we lose direct comparisons today)
- Catch-up / archive (EPG-driven, provider archive windows)
- Timeshift on live (pause/rewind the live buffer)
- Scheduled & series DVR (extend record-now with EPG timers)
- Auto catalog refresh (background playlist/EPG sync)

**P1 — reach parity** (common asks, remove friction)
- Chromecast cast-out
- External-player handoff (MX/VLC fallback)
- True system PiP
- Backup / restore (export settings + playlists)

**P2 — extend the lead** (deepen the moats)
- EPG reminders / notifications
- Richer metadata (TMDB — trailers, cast art, collections)
- Multi-profile (household separation)
- Autoplay-next runner (wire the stored setting)

## Method & caveats

- **Our column is a code audit.** Every ● traces to shipping code in v1.4.2; every ○ to a confirmed
  absence or a design doc marked "planned".
- **Competitor cells are best-effort** from public feature listings as of July 2026. Reseller-rebranded
  builds and provider-side toggles can shift a cell.
- **`$` = exists but gated to a paid tier.**
- **Not scored:** stream quality/uptime (provider-dependent), install base, store ratings — feature
  parity only.
