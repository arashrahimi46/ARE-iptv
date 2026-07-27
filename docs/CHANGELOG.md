# Changelog

All notable user-facing changes to the ARE iptv TV app.

## 1.8.0 (2026-07-27)

### New

- **The player HUD is arranged per context.** Live TV and movies/series no longer
  share one button layout. **Settings → Playback → Rearrange player controls** now
  has a **Live TV** / **Movies & series** switch, and each previews against its own
  sample HUD, so what you arrange is what you'll see. Buttons you push up land in a
  **Hidden buttons** tray instead of vanishing, and can be pulled back down. Any
  arrangement you had already made carries over to Live TV.
- **Opening a tab puts focus on the content.** Every tab now lands focus where
  you're going instead of leaving it parked on the sidebar, so the screen no longer
  looks inert until you press right.
- **Supporting the app hands off to your phone.** Buy me a coffee shows a QR code
  rather than asking you to type a URL on a remote.

### Changed

- **The HUD shows the channel's logo.** It used to guess at initials — "NAU" for
  Nautical Channel. Movies and series get a poster-shaped well at the right aspect
  ratio instead of a letterboxed square.
- **See all** is a glass chip, and its focus ring is no longer clipped.
- A channel tile's category chip recedes instead of competing with the name.

### Performance

- Home no longer composes every rail at once; the Guide stops redrawing itself on
  every keypress; the sidebar stops rebuilding itself to slide; and the player stops
  redrawing to count keypresses.
- The ambient backdrop, the soft shadow under every glass surface, and the focus
  glow are now rasterized once and reused instead of being redrawn every frame.
  Honest note: this is a ~30% cut in RenderThread work but only ~3 fps, which you
  are unlikely to notice. See `docs/glass-render-perf-findings.md` for what was
  measured, what it was worth, and what is still open.

## 1.7.0 (2026-07-27)

### New

- **Explore free IPTV.** Adding a playlist no longer requires already having a
  URL. **Add a playlist → Explore free IPTV playlists** offers around twenty
  vetted, publicly listed free playlists by country and category — pick one,
  name it, and it imports like any other. Playlists added this way are badged
  **From Explore** in the picker. ARE iptv hosts none of these streams.
- **Rename a playlist.** Hold **OK** on a playlist in the picker for a
  Rename / Delete menu. Holding OK used to delete it outright, so an accidental
  long-press can no longer start a destructive action.
- **A real Privacy Policy and Terms.** The placeholder paragraph is replaced by
  a full document — what stays on your device, what diagnostics are collected,
  which third parties your device contacts, your GDPR rights, and a copyright
  takedown route. Readable from **Settings → About**.
- **Crash reports can be switched off.** Diagnostics previously had no opt-out
  at all. **Settings → About → Crash reports** now controls it, and the choice
  is asked for on first run.
- **Guided setup for OMDb and OpenSubtitles keys.** Step-by-step instructions
  with a QR code, so the sign-up happens on your phone instead of being typed
  on a remote.
- **Radio stations are marked as such.** A station with no video is labelled
  instead of showing an empty black frame that looks like a broken stream.
- **Channel tiles show their category.**
- **Multi-view: hold OK on a pane** for a menu — watch that channel full
  screen, or remove it. It used to delete the pane with no confirmation.

### Changed

- **Opening a tab now puts the selection where you are going.** The sidebar
  closes and the first thing on the screen is selected — a channel, a category,
  a recording, a search box — instead of leaving you on the sidebar having to
  press right. Coming back from the player still returns you to what you were
  watching.
- **The player HUD no longer runs out of room.** The button row fits its full
  default set, and scrolls rather than clipping if you enable more.
- **Audio sync is hidden by default** — it repairs a mis-timed stream and isn't
  needed in normal viewing. Re-enable it in **Settings → Playback → Rearrange
  player controls**, which now shows how many buttons you have room for.
- A playlist limit of 10 is now applied consistently.
- The **ON AIR NOW** badge and the sidebar match the app's glass finish.
- New cinematic splash screen.

### Fixed

- **Multi-view crashed every time it was opened.**
- **Channels imported from some playlists had corrupted names** and lost their
  category — a channel could appear as a fragment of browser text instead of
  its name.
- The Privacy & Terms document **could not be scrolled with a remote**.
- Multi-view kept four video decoders running after you left the screen.

### Performance

- Guide reads no longer scan the whole programme table.
- The player stopped redrawing its entire HUD twice a second during playback.
- Cheaper D-pad navigation, sidebar animation and tile rendering on slower TVs.
- The startup profile is generated correctly again, so cold start is faster.

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
