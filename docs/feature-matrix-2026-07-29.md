# Feature Matrix & Competitive Brief — 2026-07-29

> Interactive artifact: `docs/feature-matrix-2026-07-29.html` (open in a browser for the color-coded matrix,
> hidden-features gallery, light-theme showcase, and filterable innovation list).
>
> Supersedes `docs/competitive-analysis-2026.md` where they conflict — that doc predates several features
> shipping (catch-up, timeshift, Stalker Portal, Glass redesign) and is now stale on those rows.

## Method

Three-agent research pass:
- **app-auditor** — direct code read/grep of `tv/src/main/java/com/arashrahimi46/iptv/` (not docs, not UI
  impressions). Found 2 stale design docs and 10 shipped-but-undocumented features.
- **competitive-analyst** — desk research (WebSearch) on 10 benchmark apps: TiviMate, IPTV Smarters Pro, GSE
  Smart IPTV, Perfect Player, XCIPTV, OTT Navigator, Kodi+PVR IPTV Simple Client, IBO Player Pro, Sparkle TV,
  VU Player. Anything unverifiable is marked "unclear" rather than guessed.
- **innovation-strategist** — 22 feature concepts scoped against the actual codebase (what infra each reuses),
  not a generic wishlist.

## Doc corrections (do not cite the old status)

1. **Stalker Portal** — `docs/stalker-portal-v1-design.md` says "not yet built." Reality: **fully shipped**,
   all 5 phases (handshake → catalog → resolve-on-play → EPG/catch-up → onboarding UI). Confirmed present in
   only 3/10 benchmarked competitors (TiviMate, IBO Player, VU Player) — real differentiator.
2. **Glass Design redesign** — `docs/glass-redesign-v1-design.md` says "design only." Reality: **shipped
   app-wide**, ~27 files, device-capability-tiered rendering (Tier A/B/C).

## Where ARE IPTV already leads

- Online subtitle fetching (real OpenSubtitles API client — login, language auto-detect, download)
- 24-language localization incl. RTL (Persian/Arabic)
- All three source types (M3U + Xtream + Stalker) in one app
- 8-preset accent system, independently selectable per light/dark theme mode
- Home rail recommendation engine ("Because you like X" / "For You") — zero-cost personalization from
  existing continue-watching/favorites data
- Fully free, no ads, no paywall (vs. TiviMate's $30/yr Premium gate on DVR/timeshift/sync)
- Tee-based DVR with FAT32 auto-split, stall watchdog, crash-recovery reconciliation
- Multi-view (simultaneous multi-channel grid)

## Confirmed real gaps (verified absent in code)

| Gap | Rivals with it (of 10) | Why it matters |
|---|---|---|
| Backup/restore + cloud settings sync | 1 (TiviMate, paywalled) | GSE's explicit #1 cited churn reason — free sync is genuine whitespace |
| Scheduled/series DVR (EPG-timer recording) | 4 | We only support record-now |
| Native casting (Chromecast/AirPlay) | 3 (weak everywhere) | Total absence is noticeable even though the field is weak here |
| User profiles / kids mode | 2 | Unmet need across the whole category |
| EPG-program text search | 1 (VU Player's "Global Search") | We only search titles, not program guide |
| EPG reminders/notifications | 1 (XCIPTV) | Cheap, high-trust, narrow gap |
| True OS Picture-in-Picture | 0 confirmed | Low urgency, but current mini-player is UI-labeled "PiP" — mislabeling risk, fix regardless |
| Voice control | 0 confirmed | Pure differentiation bet, no competitive pressure |

## Hidden/undocumented shipped features (worth a changelog + positioning pass)

Home recommendation engine · 8-preset per-mode accent picker · per-screen category pinning (Live/Movies/Series
independent) · separate HUD layouts for Live vs VOD · online subtitle search · playback speed control · Explore
screen (curated free-playlist onboarding) · multi-view · privacy/terms consent flow + crash-reporting opt-out ·
in-app mini-player mislabeled as "Picture-in-Picture" (flag, don't market as real PiP).

## Prioritized roadmap (RICE-style, gaps + innovation combined)

**P0 — ship next**
1. Backup/restore + free cloud settings sync
2. Dead-stream auto-fallback (silently retries an alternate source for the same logical channel)
3. Playlist Auto-Groomer + cross-playlist dedup (attacks IPTV's #1 universal complaint; also unlocks #2 and channel numbering)
4. User profiles + kids mode
5. Advanced EPG grid with catch-up scrubbing (builds on shipped catch-up plumbing)

**P1 — next quarter**
6. Playlist/source health dashboard (pairs with #2 as a "Reliability Suite" story)
7. EPG program reminders/notifications
8. EPG-program text search
9. Native casting (Chromecast)
10. Trakt.tv ratings/reviews integration

**P2 — backlog / bets**
11. Fix mini-player mislabeling now (rename away from "PiP"); real OS PiP later
12. Ambient live-preview wall (idle Home shows muted live channel previews — leverages shipped ambient mesh work)
13. Companion mobile remote + seamless handoff (the `:mobile` module is currently a stub)
14. Voice control

**Sequencing note:** items #3, #2, #6, and custom channel numbering share one underlying "logical channel
across merged sources" abstraction — scope as a single Reliability Suite epic rather than four separate
features.

## Full matrix, hidden-feature gallery, and light-theme showcase

See `docs/feature-matrix-2026-07-29.html` — interactive, tabbed, filterable.
