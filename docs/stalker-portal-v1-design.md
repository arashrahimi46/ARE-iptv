# Stalker Portal (Ministra) Source — V1 Design Spec

> Status: **Agreed design, not yet implemented.** Shared reference for adding Stalker Portal as a
> third source type alongside M3U and Xtream. Every decision below was made deliberately; rejected
> alternatives are noted so we don't relitigate. Scope is **full** (live + VOD + series) by product
> decision — see [Scope](#1-goal--scope).

## 1. Goal & scope

Let a user connect a **Stalker Portal / Ministra** subscription — the third way IPTV providers hand
out access, alongside M3U and Xtream — and get the **same in-app experience** we already give Xtream:
live TV + EPG, movies, and series, all browsable, favoritable, resumable, recordable.

**Why:** a large pool of subscriptions (MAG-box era) are **Stalker-only** — provider gives a portal
URL + a MAC address, no username/password. Today we turn those users away at onboarding. This closes
that gap and completes our provider-compatibility set (M3U · Xtream · Stalker).

**V1 covers the full catalog: live, VOD, and series.** (Live-only was considered and rejected —
product wants parity with our Xtream experience.)

## 2. Why Stalker is not "just a third source type"

Two protocol facts make Stalker structurally different from M3U/Xtream, and they drive the whole
design. Read these first.

1. **Auth is stateful.** Xtream is stateless — every request carries `?username=&password=`. Stalker
   requires a **handshake** (`action=handshake`) that returns a short-lived **bearer token**, sent as
   an `Authorization: Bearer <token>` header on every subsequent call, plus a `Cookie: mac=<MAC>`.
   Tokens expire; a `get_profile` step often follows handshake to fully authorize the session. Our
   `XtreamClient.fetch()` (`data/parser/XtreamClient.kt:135`) has **no header/cookie support** — this
   is new HTTP plumbing.

2. **Playback URLs cannot be precomputed.** For Xtream we build the final playable URL at import
   (`XtreamClient.streamUrl()`, line 129) and store it in `Channel.streamUrl`; the player just reads
   the column. **Stalker has no static stream URL.** You must call `create_link` **at the moment of
   play**, and the portal returns a **short-lived, tokenized URL** valid only for that session. This
   forces a **resolve-on-play** step into the player path — a genuinely new pattern for us (every
   existing source resolves everything up front and keeps the player dumb).

Everything unusual in this spec — the session client, the resolver, storing a `cmd` instead of a URL —
traces back to these two facts.

## 3. Locked decisions

| Area | Decision | Rejected alternative (why) |
|------|----------|----------------------------|
| **Scope** | Full: live + VOD + series | Live-only (product wants Xtream parity) |
| **Auth** | New `StalkerClient` with stateful handshake→token→get_profile; re-handshake on 401/expiry | Reuse `XtreamClient` (wrong auth model) |
| **Stream URLs** | **Resolve-on-play** via `create_link`; store the portal `cmd` in `Channel/VodTitle/SeriesEpisode.externalId`, not a URL | Precompute at import (impossible — URLs are session-scoped & expire) |
| **Resolver seam** | A single `StreamUrlResolver` between "pick `PlaybackSource`" and "build `MediaItem`"; identity pass-through for M3U/Xtream, `create_link` for Stalker | Branch inside the player (leaks Stalker into every playback site) |
| **MAC storage** | Encrypted in `CredentialsStore` (new `mac` accessor); portal URL in plaintext `PlaylistSource.url` | New Room column (MAC is a secret → belongs off the entity, like Xtream creds) |
| **DB schema** | **No migration.** MAC→creds, portal→`url`, `cmd`→existing `externalId`, session/profile blob→existing `accountInfoJson` | Add columns + bump to v12 (unnecessary; avoids risk) |
| **MAC entry** | User **pastes** provider-issued portal URL + MAC | App-generated MAC (rare; provider must pre-register it) |
| **Device identity** | Derive `sn` / `device_id` / `device_id2` / `signature` from MAC (community-standard derivation), send when the portal demands them | Assume MAC-only (many portals reject the session without them) |
| **EPG** | Native Stalker per-channel EPG (`get_short_epg`/`get_epg_info`) + explicit XMLTV `epgUrl` override | XMLTV-only (loses portals that only serve EPG over the API) |

## 4. The `StalkerClient`

New file `data/parser/StalkerClient.kt`, **mirroring the shape** of `XtreamClient` (OkHttp,
`Dispatchers.IO`, `org.json`, a `StalkerException(message, cause, isAuthError)`), but session-aware.

```
class StalkerClient(
  portalUrl: String,           // PlaylistSource.url — base, e.g. http://host:port/
  mac: String,                 // from CredentialsStore
  client: OkHttpClient = defaultClient,
)
```

- **Endpoint base.** Portals expose the API at `/portal.php` or `/stalker_portal/server/load.php`
  (varies — probe both, cache which one answered in `accountInfoJson`). All calls are
  `GET {base}?type=<t>&action=<a>&...`, `Js(...)` JSON.
- **Session:**
  - `handshake()` → `type=stb&action=handshake` → stores `token`.
  - `getProfile()` → `type=stb&action=get_profile` with derived `sn`/`device_id`/`device_id2`/
    `signature` → confirms authorization; stores account/expiry metadata → `accountInfoJson`.
  - Every request adds `Authorization: Bearer <token>` + `Cookie: mac=<MAC>; stb_lang=en; timezone=…`.
  - **Auto-reauth:** a `401`/empty-token response triggers one transparent re-handshake + retry;
    second failure throws `StalkerException(isAuthError=true)`.
- **Catalog (paged — Stalker paginates via `p=<n>`, loop until empty):**
  - `getLiveCategories()` → `type=itv&action=get_genres`
  - `getLiveChannels()` → `type=itv&action=get_all_channels` (or paged `get_ordered_list`) → items
    carry `id`, `name`, `number`, `logo`, `xmltv_id`, and **`cmd`** (the play command).
  - `getVodCategories()` → `type=vod&action=get_categories`; `getVod()` → `type=vod&action=get_ordered_list`
  - `getSeriesCategories()`/`getSeries()` → `type=series&action=…`; `getSeriesEpisodes(id)` for drilldown.
- **Resolve-on-play:**
  `createLink(cmd: String, type: String): String` → `type=<itv|vod>&action=create_link&cmd=<cmd>`
  → returns a `cmd` string like `ffmpeg http://…/live/…`; **strip the leading `ffmpeg `/`auto `** →
  the real, short-lived URL for ExoPlayer.
- **EPG:** `getShortEpg(channelId)` → `type=itv&action=get_short_epg` — mirrors Xtream's per-channel
  fallback.

## 5. Resolve-on-play architecture (the one new pattern)

Introduce a thin seam so the player stays source-agnostic:

```
interface StreamUrlResolver { suspend fun resolve(source: PlaylistSource, item: Playable): String }
```

- **M3U / Xtream:** identity — return the already-stored `streamUrl`. Zero behavior change.
- **Stalker:** construct `StalkerClient(source.url, credentials.mac(source.id))`, ensure session,
  call `createLink(item.externalId, kind)`, return the resolved URL.

The player (`ui/player/…`) calls `resolver.resolve(...)` **immediately before building the
`MediaItem`**, replacing the direct read of `streamUrl`. This is the **only** player-path change and
the single place Stalker's session-scoped URLs live. `StreamRetryPolicy`'s re-resolve on failure
naturally routes back through the resolver → a fresh `create_link` (stale-URL recovery for free).

**Consequence (accepted):** Stalker playback needs network at press-time to mint the URL; a portal
outage means "can't start," surfaced as a clear player error (not a silent hang).

## 6. Data model & storage — no migration

Reuse existing fields (`data/model/Entities.kt`):

- `SourceType` (line 11): **add `STALKER`**. (Room stores the enum name as `TEXT` — no schema change.)
- `PlaylistSource.url` → portal base URL. `PlaylistSource.accountInfoJson` → session/profile blob
  (endpoint variant, token expiry, account status) via a `StalkerAccountInfo.toJson()` mirroring
  `XtreamAccountInfo`.
- `Channel.externalId` / `VodTitle.externalId` / `SeriesEpisode.externalId` → the portal **`cmd`**
  (not a URL). `streamUrl` is left blank/placeholder for Stalker rows; the resolver fills it live.
- **MAC** → `CredentialsStore` (`data/settings/CredentialsStore.kt`), new keys:
  `macKey(id) = "mac_$id"` + `saveStalker(sourceId, mac)` / `mac(sourceId)`; clear it in the existing
  `clear(sourceId)` path. (The MAC grants portal access → it is a secret, kept off the Room row exactly
  like Xtream user/pass.)

DB stays at **v11** — no new columns, no migration, consistent with `MIGRATION_6_7`/`7_8` precedent of
*only* migrating when the entity genuinely changes.

## 7. Import pipeline

`data/repository/PlaylistRepository.kt`, mirroring `addXtreamSource` (impl lines 554–647):

- **Interface:** add `addStalkerSource(name, portalUrl, mac, epgUrl): ImportSummary`.
- **Impl:** `StalkerClient(portalUrl, mac).handshake()+getProfile()` → fetch genres/channels/vod/series
  → `playlistSourceDao().insert(source)` → `credentials.saveStalker(sourceId, mac)` → build
  `Category`/`Channel`/`VodTitle`/`SeriesEpisode` rows with **`externalId = cmd`** (URL left blank) →
  `upsert…` → `replaceDuplicateSources(sourceId)` → `settings.setActiveSourceId(...)`. Same
  all-or-nothing rollback (rows + creds) on failure.
- **`refreshSource`** (line 657): add a `SourceType.STALKER` branch reusing the id-preserving
  `upsertChannels`/`upsertTitles` diff (lines 773–846). Re-handshake happens inside the client.
- **`ensureSeriesEpisodesLoaded`** (line 291) / **`ensureMetadataLoaded`** (line 328): add
  `SourceType.STALKER` branches (episodes via `getSeriesEpisodes`; Stalker carries its own VOD
  metadata — no OMDb dependency, though OMDb enrichment still works since it keys off title/year).
- **`deleteSource`** (lines 742–764): already source-agnostic; just ensure the new `mac` cred key is
  cleared (it is, via `clear`).

## 8. EPG

`data/repository/EpgRepository.refresh` (line 131) resolves the XMLTV URL by type. Add a
`SourceType.STALKER` branch:
1. `source.epgUrl` if the user supplied an XMLTV override (already type-agnostic — free path).
2. else native Stalker **`getShortEpg(channelId)`** per channel, matched by `xmltv_id → tvgId`
   (mirrors the existing Xtream `get_short_epg` fallback at lines 178–201).

Channel `tvgId` is populated from the portal's `xmltv_id` at import so guide matching "just works."

## 9. Onboarding UI

Wizard is `ui/onboarding/` (`OnboardingFlow.kt` / `OnboardingSteps.kt` / `OnboardingViewModel.kt`),
routes `["source","credentials","epg","confirm"]`.

- **`OnboardingSourceType`** (`OnboardingViewModel.kt:19`): add `STALKER`.
- **Step 1 `SourceStep`** (`OnboardingSteps.kt:53`): add a **third `SourceCard`** ("Stalker Portal",
  subtitle "Portal URL + MAC address"). The `Row` already uses `weight(1f)` — three cards fit; verify
  TV focus order left→right.
- **Step 2 `CredentialsStep`** (line 131): add a Stalker branch with two fields — **Portal URL** and
  **MAC address**. MAC field: monospace, auto-format to `AA:BB:CC:DD:EE:FF` as typed, inline validate
  against `^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$`. Widen `OnboardingUiState` (line 21) with `mac` (reuse
  `serverUrl` for the portal URL) and the `updateCredentials(...)` mutator (line 53) accordingly.
- **`canContinue`** (`OnboardingFlow.kt:177`): Stalker → portal URL non-blank **and** MAC matches the
  regex.
- **Step 4 `ConfirmStep`** (line 233) & **`SelectSourceScreen`** display label (line 118): add Stalker
  summary/label cases.
- **`submit()`** (`OnboardingViewModel.kt:85`): add
  `OnboardingSourceType.STALKER -> repository.addStalkerSource(name, portalUrl, mac, epgUrl)`.
- **i18n & TV conventions:** every new string through `stringResource`, added to **all
  `values-*/strings.xml` locales**; `TvFocusable` on every focusable; dialogs trap focus; MAC-format
  help text localized. (RTL for fa/ar remains the app-wide future round.)

## 10. Portal-variance risks (be honest — this is the fragile part)

Stalker/Ministra is semi-proprietary and portals differ. Known variances to design defensively for:

| Variance | Handling |
|----------|----------|
| **API path differs** (`/portal.php` vs `/stalker_portal/server/load.php`) | Probe both on first handshake; cache the winner in `accountInfoJson`. |
| **`get_profile` demands `sn`/`device_id`/`signature`** | Derive from MAC (standard community algorithm); send on every session. Portals that ignore them still accept. |
| **Token TTL varies (minutes)** | Re-handshake transparently on `401`/empty; the resolver always mints a fresh `create_link`. |
| **`cmd` format differs** (`ffmpeg `/`auto `/bare URL) | Strip known prefixes; if it's already a URL, pass through. |
| **Connection-limit kicks** (portal ties MAC to one active stream) | Same `StreamRetryPolicy` path as Xtream; clear "portal busy / connection limit" error. |
| **VOD/series absent or empty** on a portal | Skip gracefully; a live-only portal still fully works. |
| **Portal blocks non-MAG User-Agent** | Send a MAG-style `User-Agent`/`X-User-Agent` (`Model: MAG254; Link: WiFi`). |

**Accepted reality:** we cannot guarantee every portal in the wild. V1 targets standard
Ministra/Stalker portals; genuinely non-conforming ones fail with a clear diagnostic, not a hang.

## 11. Component / file map

**New**
- `data/parser/StalkerClient.kt` — session client (handshake, get_profile, catalog, `create_link`,
  short-EPG) + `StalkerAccountInfo` + `StalkerException`.
- `data/parser/StalkerIdentity.kt` — MAC → `sn`/`device_id`/`device_id2`/`signature` derivation.
- `data/player/StreamUrlResolver.kt` — the resolve-on-play seam (identity for M3U/Xtream, `create_link`
  for Stalker).

**Modified**
- `data/model/Entities.kt` — `SourceType.STALKER`.
- `data/settings/CredentialsStore.kt` — `saveStalker`/`mac` accessors + `macKey`; clear on delete.
- `data/repository/PlaylistRepository.kt` — `addStalkerSource`; `STALKER` branches in `refreshSource`,
  `ensureSeriesEpisodesLoaded`, `ensureMetadataLoaded`.
- `data/repository/EpgRepository.kt` — `STALKER` EPG branch (native short-EPG + XMLTV override).
- `ui/player/…` (`LivePlayerScreen` / playback controller) — call `StreamUrlResolver.resolve(...)`
  before building the `MediaItem`; route `StreamRetryPolicy` re-resolve through it.
- `ui/onboarding/OnboardingViewModel.kt` — `OnboardingSourceType.STALKER`, `mac` state, widened
  `updateCredentials`, `submit()` branch.
- `ui/onboarding/OnboardingSteps.kt` — third `SourceCard`; Stalker credentials fields + MAC validation.
- `ui/onboarding/OnboardingFlow.kt` — `canContinue` Stalker branch.
- `ui/sources/SelectSourceScreen.kt` — Stalker display label.
- `ui/settings/SettingsScreen.kt` — Stalker account panel (reuse the Xtream read-only panel shape) &
  "Refresh now".
- `values*/strings.xml` (all locales) — new strings.
- `AndroidManifest.xml` — none required (INTERNET already present).

## 12. Out of scope (explicitly V2)
- **App-generated MAC / portal registration** — V1 is paste-only (provider pre-registers the MAC).
- **Stalker recording** — the DVR tee records `.ts`; Stalker `create_link` URLs *may* be `.ts` and
  work through the existing pipeline, but this is **not validated in V1**; treat as best-effort/off.
- **Timeshift/catch-up over Stalker** — shares the catch-up feature track, not this one.
- **Auto-refresh / scheduled portal sync** — manual "Refresh now" only (app-wide gap).
- **Multi-portal load-balancing / failover MAC lists.**
- **Non-conforming/proprietary portal dialects** beyond the variance table in §10.

## 13. Phasing
1. **Session client** — `StalkerClient` handshake/get_profile/auth-retry + `StalkerIdentity`.
   Verifiable: unit test the handshake/token/reauth against recorded fixtures.
2. **Catalog import** — `addStalkerSource` + `refreshSource` branch; live+VOD+series rows land with
   `cmd` in `externalId`. Verifiable: import a portal, browse channels/movies/series in-app.
3. **Resolve-on-play** — `StreamUrlResolver` seam + player wiring + retry re-resolve. Verifiable:
   play a live channel, a movie, an episode end-to-end **on device** (never curl the portal — same
   rate-limit rule as Xtream).
4. **EPG + onboarding UX** — native short-EPG/XMLTV branch; onboarding third card + MAC entry/validation
   + account panel; full i18n. Verifiable on the emulator via screenshots.
5. **Variance hardening** — endpoint probing, MAG User-Agent, `cmd`-prefix handling, clear error
   states (§10). Verifiable against 2–3 real portals.
