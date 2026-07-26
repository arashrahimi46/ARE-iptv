# Changelog

All notable user-facing changes to the ARE iptv TV app.

## 1.6.0 (2026-07-26)

### New

- **Glass sidebar.** The left navigation is now a translucent glass object. Pick
  its shape in **Settings → Appearance → Sidebar style**: a **Floating** glass
  box that hovers inset off the screen edge (the new default), or a **Full
  height** rail flush to the edge like before. Either way it samples and blurs
  the wallpaper behind it and carries the app's lit-edge glass finish.

### Changed

- Sidebar selection now **glides** — a springy glass lens slides between items
  when you switch tabs, instead of jumping.
- Nav rows fade softly into the glass at the box's rounded corners.

## 1.5.0 (2026-07-24)

### New

- **Glass redesign.** One translucent "glass" surface language across the whole
  shell — sidebar, player HUD, buttons, cards, tabs, badges, Settings and
  dialogs now share a frosted fill with a lit hairline edge and a soft focus
  glow. New frosted-blue adaptive launcher icon.
- **Customizable player HUD.** Rearrange and hide player controls from
  Settings → Playback → "Rearrange player controls" (rewind / play-pause /
  fast-forward stay locked). Your layout persists across restarts.
- **Stalker Portal / Ministra** support as a third source type — live, movies
  and series, with MAC-based login and resolve-on-play streaming.
- **Catch-up / archive.** Replay past programs from the Guide where the provider
  offers an archive (Xtream and M3U catch-up sources), with a rewind pill,
  "Go Live", and program-hopping in the player.
- **Playback speed** control for movies and recordings.
- **Audio sync** offset and an **audio-track picker**.
- **Per-channel aspect-ratio** memory and an aspect-ratio cycle.
- **Settings enrichment** — pick a start screen, automatic playlist refresh with
  a staleness nudge, and deeper parental controls.

### Changed

- Segmented tab controls with a sliding accent indicator (Settings, Search,
  Favorites, Guide day selector).
- Tighter, lighter sidebar: thinner outlined icons, smaller rows, narrower
  expanded width.
- Animated tab → content transitions.

### Fixed

- Focused movie/series poster keeps its title on screen.
- Collapsed-rail icons centered; Settings chip rows no longer collapse.
- First-row focus glow no longer clipped in the sidebar / settings tab strip.

## 1.4.2

- Onboarding duplicate-source fix and language-change ANR fix.
