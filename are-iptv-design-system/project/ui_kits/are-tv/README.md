# ARE iptv — Android TV UI kit

High-fidelity, click-through recreation of the ARE iptv player for Android TV / Fire TV. Built entirely from this design system's components (`window.AREIptvDesignSystem_632b75`) + `data.js` (mock content, images via picsum.photos).

## Run
Open `index.html`. It boots into the **playlist onboarding wizard**; complete or skip it to enter the app.

## Flow
- **Onboarding** → guided wizard: Source (Xtream Codes vs M3U link) → Credentials → EPG → Confirm.
- **Home** → featured hero + rails (Continue watching, Live now, AI Recommended, Movies, Series).
- **TV Guide** → full EPG grid: timeline header, channel column, proportional program cells (now/live/catch-up).
- **Movies / Series** → category-chip browse grids.
- **Search** → universal results across live, catch-up, movies & series (AI-ranked).
- **Favorites** → per-type favorite lists + smart favorites + custom groups.
- **Settings** → appearance (theme toggle, wired live), playback, parental, playlists & sync.
- **Overlays:** Detail page (backdrop, cast, episodes), Live player (glass HUD + TimeShift bar + mini channel strip), Multi-view (2/4 simultaneous streams).

## Files
- `index.html` — mounts everything; also the Design System card + a starting point.
- `data.js` — mock catalog, EPG, detail data.
- `app.jsx` — shell: rail nav + screen router + overlays + theme state.
- `screens/` — `Home, LivePlayer, Guide, Detail, Browse, Search, Onboarding, Settings, MultiView, Favorites`.

## Notes
- Requires the Lucide CDN script (icons) — already included in `index.html`.
- Theme: toggle in Settings flips `data-theme` on `<html>` live across the whole app.
- These are cosmetic recreations; playback is faked (still images), navigation is real.
