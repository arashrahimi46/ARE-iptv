# Mobile Phase 3 follow-ups

Scope cuts from the Phase 1+2 mobile build (commits `a5a7034`, `5672f89`), approved by
product-lead as reasonable for v1. Phase 3 (below, items 1-4) shipped and QA'd on main.

1. **DONE — Settings screen.** Real touch-first Settings with General (catalog/EPG refresh +
   Appearance theme picker), Playback, Subtitles, Parental panes. Commits f3f1495 (panes),
   c73a654 (Dark/Light/System Appearance picker, added mid-QA when the light-theme pass found
   there was no toggle at all). Scope call: lean mobile-only SettingsViewModel over :core's
   shared UserSettings DataStore rather than porting :tv's full SettingsViewModel (which also
   owns OMDb/OpenSubtitles config, accent picker, HUD layout editor — no mobile UI for those
   yet). PIN entry is a Material3 dialog, not :tv's on-screen keypad.

2. **DONE — Favorites.** Heart toggle (48dp touch target, commit a471353) on Home/Live/Movies/
   Series tiles, dedicated Favorites screen (Channels/Movies/Series tabs) reachable via a
   Settings row (qa-reviewer's call: not promoted to bottom nav — 5 items is already the right
   one-handed density; revisit if usage data supports a 6th). Commit 316936c wired the toggle;
   commit 5a7ee3c fixed a critical shared-`:core` bug QA found — `FavoritesRepository`/
   `FavoriteDao` keyed membership on `COALESCE(externalId, name, streamUrl)`, so providers that
   reuse one EPG `externalId` across regional affiliates (e.g. "10 Comedy — Sydney/Melbourne/
   Brisbane/...") collapsed them onto one favorites entry. Fixed by keying on
   `COALESCE(externalId || '|' || name, name, streamUrl)`. **This was pre-existing in shared
   `:core` code from :tv's original Phase 4 Favorites — :tv was never QA'd against a
   regional-dupe playlist and likely has the same latent bug. Worth a follow-up ticket on :tv.**

3. **PARTIAL — Continue Watching.** `HomeViewModel.resolveContinueWatching` now resolves
   `seriesEpisodeId` entries to their parent series title (commit 007ae71), mirroring :tv.
   `recordingId` entries remain dropped, as specced. **Not fully verifiable yet**: qa-reviewer
   found the resolver has no way to be exercised end-to-end because :mobile's Series tab has no
   playback path at all (see item 5) — nothing in the UI can generate a series watch-progress
   entry to resolve. Code review passed; on-device confirmation is blocked on item 5.

4. **DONE — RTL fonts.** Vazirmatn swap for fa/ar ported into `mobile/.../ui/theme/Type.kt` /
   `AreIptvMobileTheme`, mirroring `:tv`'s `LocalLayoutDirection` check (commit ccd37b6).
   QA-verified: proper Vazirmatn glyphs, full layout mirror, Persian numerals.

## New gaps found during Phase 3 QA (not yet scoped/built)

5. **Series has no playback path on mobile.** The Series tab routes through the same generic
   grid/player as Movies. There's no season/episode picker (`ui/series/` package is empty) and
   a series' grouped entry has no stream URL of its own — only individual episodes do. Tapping
   any series item errors "no playable stream." This is materially larger than the original
   4-item Phase 3 list (needs an episode list/picker screen + player route wiring, mirroring
   :tv's series browse) — tracking as its own future phase, not folded into this one. Blocks
   full verification of item 3 above until built.

6. **Light-theme `PosterTile` needs a border edge.** Near-white surface, same pattern already
   documented in project CLAUDE.md (near-white surfaces need `borderDefault` to read against the
   off-white page) — just not applied to this tile yet. Small fix, filed with android-engineer.

7. **`:mobile` manifest missing `usesCleartextTraffic`.** Can't fetch any `http://`
   (non-https) playlist — onboarding silently fails for those providers. Filed with
   android-engineer.
