# Live TV Recording — V1 Design Spec

> Status: **Agreed design, not yet implemented.** This is the shared reference for building
> record-now. Every decision below was made deliberately; alternatives considered are noted so we
> don't relitigate. Scope is intentionally narrow — see [Out of scope](#out-of-scope-explicitly-v2).

## 1. Goal & scope

Let the user **record the live channel they're currently watching** to disk (internal or any
external USB drive), then **monitor and play back** those recordings from a dedicated tab —
regardless of where they're stored — with best-in-class fault tolerance.

**V1 is record-now only.** The user presses a `● REC` toggle in the live player; capture runs until
they stop it (or a fault stops it). No scheduling, no series pass, no timeshift, no background
recording.

## 2. Locked decisions

| Area | Decision | Rejected alternative (why) |
|------|----------|----------------------------|
| **Scope** | Record-now only | Full DVR / EPG-scheduled (weeks of work; wants AlarmManager + foreground service) |
| **Capture** | **Tee ExoPlayer's `DataSource`** — one connection feeds screen + disk | Independent 2nd connection (costs a provider connection, hits panel connection limits) |
| **Format** | **MPEG-TS byte-copy** (no re-encode); HLS detected → "not yet recordable" | HLS segment-downloader/remux (bigger engine; deferred) |
| **Stop rule** | **Manual stop** (also stops on disk-full / player-closed) | EPG auto-stop / duration picker |
| **Storage** | **SAF** (`ACTION_OPEN_DOCUMENT_TREE`), **ask which drive every time** | Internal-only; pick-once default |
| **Placement** | **New top-level "Recordings" tab** in the shell | Home row; nested under Live |
| **Timeshift** | Deferred to a separate feature | — |

### Consequence of teeing (accepted)
Recording is **bound to the player lifecycle**. Leaving the player, closing the app, or the screen
going to standby **ends the recording cleanly** (`COMPLETED`). "Record while you watch" is the mental
model. Unattended/background recording is a V2 capability requiring the independent-connection +
foreground-service path.

## 3. Capture pipeline (the tee)

- A `TeeDataSource` wraps Media3's `DataSource` (via a custom `DataSource.Factory` layered onto the
  existing live playback factory). As ExoPlayer reads bytes, the wrapper **also writes them to the
  recording sink**.
- **Playback is sacred.** The tee is **non-blocking w.r.t. the player**: writes go through a bounded
  buffer drained on a dedicated IO thread. If the disk can't keep up and the buffer overflows, we
  **drop recording bytes and mark the recording `DEGRADED`/`INTERRUPTED`** — we never stall or crash
  ExoPlayer.
- **Continuous flush, minimal in-memory buffer** — a half-written `.ts` is always playable up to the
  last flushed byte. This is the core reason we chose TS.
- Only engages when the active stream is `.ts`. If `.m3u8`, the REC button is disabled with a
  "recording not supported for this stream" hint.

## 4. Storage (SAF)

- On each REC press, prompt for the destination drive via the system folder picker; take a
  **persistable URI permission**. Write via `DocumentFile` / `ContentResolver` `OutputStream` (URI,
  not `File`). Internal storage is offered in the same picker.
- **Human-findable layout:** `AreIPTV/Recordings/<Channel> — <Program> — <yyyy-MM-dd>.ts`.
- **Filename sanitization** for FAT (strip/replace `: / \ & < > | ? *`, cap length).
- **FAT32 4 GB ceiling → auto-split** into `…part-001.ts`, `…part-002.ts`, … stitched seamlessly at
  playback. Split boundary is triggered proactively before the 4 GB limit.
- **Drive identity:** verify the volume UUID before trusting a saved `treeUri` (same port, different
  stick).

## 5. Data model

New Room entity (DB **v8 → v9** migration; add to `AppDatabase.entities` + a new DAO):

```
@Entity(tableName = "recordings")
Recording(
  id: Long (PK, autogen),
  channelId: Long,
  channelName: String,
  programTitle: String?,        // from EPG at record time, if available
  startedAtMs: Long,            // wall-clock captured once at start
  durationMs: Long?,            // null while RECORDING; ≈ for INTERRUPTED (from bytes/bitrate)
  sizeBytes: Long,
  bitrateBps: Long?,            // running estimate, for space math + duration approximation
  status: RecordingStatus,
  statusReason: String?,        // e.g. "disk full", "drive removed", "stream lost"
  storageTreeUri: String,       // SAF tree
  documentId: String,           // SAF document (or first part)
  parts: Int = 1,               // >1 for split recordings
  volumeUuid: String?,          // for drive-identity + UNAVAILABLE resolution
  locked: Boolean = false,      // reserved; see §7 note
)

enum RecordingStatus { RECORDING, COMPLETED, INTERRUPTED, FAILED, UNAVAILABLE, MISSING }
```

- **Resume:** extend continue-watching to recordings (add a nullable `recordingId` to
  `ContinueWatchingEntry`, DB migration) so half-watched recordings resume via the existing player
  path.
- **DB is the source of truth.** Files are payloads the DB points at via `storageTreeUri` +
  `documentId`.

## 6. Resilience model

**One supervisor owns each recording.** Every signal — bytes-flowing, stall-timeout, drive-gone,
disk-low, lifecycle-stop, reconnect — routes through it to drive the state machine. Finalize is
**idempotent** (two faults at once must not double-finalize or crash).

**Invariant: the red REC dot never lies.** If capture dies or is reconnecting, the indicator reflects
it and the user is told. No ghost recordings.

### State machine
`RECORDING → COMPLETED` (clean: user stop / player closed / standby)
`RECORDING → INTERRUPTED` (drive pulled, stream lost, disk full, crash-recovered — *partial, playable*)
`RECORDING → FAILED` (never captured a byte)
`* → UNAVAILABLE` (file intact, its drive currently unplugged) → reconciles back on remount
`* → MISSING` (drive present, file deleted externally)

### Pre-flight (before the dot flips)
1. Test-write to the chosen drive (catches read-only / revoked SAF permission).
2. Free-space vs. bitrate estimate — warn if tight.
3. Confirm stream is `.ts`.
Fail → clear message, **no ghost REC**.

### Fault handling
| Fault | Handling |
|-------|----------|
| **Stream stall (silent, no error)** | ~20 s no-bytes watchdog → "reconnecting" (dot shows it). `StreamRetryPolicy` re-resolves the URL (not the stale one) and **resumes into the same file** (TS tolerates the gap). Grace (~60 s) expires with no recovery → finalize `INTERRUPTED (stream lost)`. |
| **Stream drop / provider kick / network handoff** | Same watchdog/reconnect path. |
| **Disk fills** | Monitor free space; **auto-stop cleanly ~300 MB before full** so the file finalizes properly → `INTERRUPTED (disk full)` + notify. |
| **USB pulled mid-record** | Unmount broadcast → flush+close → `INTERRUPTED (drive removed)`; keep the playable partial. |
| **Drive unplugged later** | `UNAVAILABLE` (not deleted); greyed with "Reconnect <drive> to watch"; remount reconciles. |
| **Slow USB throughput** | Bounded buffer; overflow drops recording bytes (playback protected) → `DEGRADED`/`INTERRUPTED`. One-time write-speed probe warns up front. |
| **App/player closed, standby, input switch** | Clean finalize → `COMPLETED` (teeing behavior). |
| **Crash / power loss** | Launch-time reconciliation finalizes any orphaned `RECORDING` row → `INTERRUPTED`. |
| **DB ↔ file divergence** | Two-way reconcile on launch: row with missing file → `MISSING`; file with no row → surfaced as "found recording". |
| **Delete while drive gone** | Drop the row now; queue the file delete for remount. |
| **Illegal filename / collision** | Sanitize; auto-suffix on collision. |

Clocks: **monotonic** for duration; wall-clock captured once at `startedAtMs`.

## 7. UI

- **New "Recordings" tab** in the shell (add `"recordings"` to `KnownRoutes`, a `TopBar` entry, and a
  `FullSizeTab` branch in `ShellHost`). DB-driven, grouped:
  - **● Recording now** — live elapsed, size, target drive, stop button, "reconnecting" state.
  - **✓ Completed** — frame-grab poster, channel + program + date + duration + size + **location
    badge** (e.g. "SanDisk USB"); plays via the existing ExoPlayer path; resume supported.
  - **⚠ Interrupted / Failed** — with reason; partials play (duration shown as `≈`).
  - **⏏ Unavailable / Missing** — greyed with a clear reconnect/why message.
  - Per-drive **free space** shown; multi-part recordings stitched seamlessly on playback.
- **Live player:** a `● REC` toggle (in `PlayerControls`) with truthful state
  (idle / recording / reconnecting / stopped-reason). Disabled for non-`.ts` streams.
- **Full i18n** — every string through `stringResource`, added to `values/strings.xml` **and all 21
  `values-*/strings.xml`**. **TV focus** (`TvFocusable`), **RTL**, and the drive-picker/dialogs
  trapping focus per repo conventions.
- Android 13+ **`POST_NOTIFICATIONS`** permission for the recording notification.

### Accepted trade-off
Recordings of **parental-locked** channels play **without** PIN in V1 (a known gap in parental
controls). The `locked` field is reserved so this can be tightened later without a migration.

## 8. Component / file map

**New**
- `data/recording/TeeDataSource.kt` (+ factory) — the non-blocking byte tee.
- `data/recording/RecordingSupervisor.kt` — owns the state machine, watchdog, fault routing.
- `data/recording/RecordingStorage.kt` — SAF writes, split, pre-flight, drive identity, free space.
- `data/repository/RecordingRepository.kt` — DB access + launch-time reconciliation.
- `data/model` — `Recording` entity + `RecordingStatus` (in `Entities.kt`); new DAO in `Daos.kt`.
- `ui/recordings/` — `RecordingsScreen.kt` + `RecordingsViewModel.kt`.

**Modified**
- `data/db/AppDatabase.kt` — add entity, bump to **v9**, migration, export schema `9.json`.
- `data/model/Entities.kt` — `ContinueWatchingEntry.recordingId` (resume).
- `ui/components/PlayerControls.kt` — `● REC` toggle + state.
- `ui/player/LivePlayerViewModel.kt` / `LivePlaybackController.kt` — wire the tee into the live
  playback factory; expose recording state; hook `StreamRetryPolicy` reconnect into resume-same-file.
- `ui/shell/AppShell.kt` (+ `MainActivity` `KnownRoutes`/`ShellHost`) — the Recordings tab.
- `ui/shell/TopBar.kt` — tab entry.
- `data/settings/UserSettings.kt` — last-used recording drive (optional convenience only; still
  ask-every-time by default).
- `values*/strings.xml` ×22 — all new strings.
- `AndroidManifest.xml` — `POST_NOTIFICATIONS`.

## 9. Out of scope (explicitly V2)
- EPG-scheduled recording, series/season pass, conflict resolution.
- Background / unattended recording (independent connection + foreground service + wake locks).
- Recording while the TV is powered off (not deliverable on consumer TV hardware).
- Timeshift / pause-live-TV (shares the disk-write pipeline; separate feature).
- HLS (`.m3u8`) live recording.
- Auto-failover of an in-progress recording to internal storage on drive removal.
- PIN-gating recordings of locked channels (field reserved).

## 10. Phasing
1. **Data + storage foundation** — `Recording` entity, DB v9, `RecordingStorage` (SAF + pre-flight +
   split), `RecordingRepository` + reconciliation. Verifiable via unit tests + a manual write.
2. **Capture** — `TeeDataSource` + `RecordingSupervisor`; wire into live playback; `● REC` in
   controls. Verifiable: record a channel, get a playable `.ts`.
3. **Resilience** — watchdog/reconnect-resume, disk-full auto-stop, unplug handling, launch
   reconciliation. Verifiable: pull the drive / kill the app / stall the stream → correct states.
4. **Recordings tab** — screen, grouping, playback + resume, location badges, delete, i18n. Verifiable
   on the emulator via screenshots.
