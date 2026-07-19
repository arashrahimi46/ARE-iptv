# ARE iptv — Design System

A design system for **ARE iptv**, a free, feature-rich **IPTV player for Android TV** (also Fire TV / Google TV). It is a *player*, not a service: users bring their own M3U playlists or Xtream Codes credentials and ARE iptv turns them into a polished, cable-TV-grade streaming experience — live TV with a full EPG, VOD movies & series with rich metadata, catch-up/TimeShift, multi-view, favorites, and AI-assisted organization.

This system is **dark-first** (lean-back 10-foot viewing) with a fully specified **light theme**, built for **D-pad remote navigation** where the focus state is the single most important interaction.

## Sources & provenance
- **No existing codebase, Figma, or brand assets were provided.** This system was authored from scratch against a written product brief (feature discovery of TiviMate, Sparkle TV, OTT Navigator, Televizo, IPTV Smarters) plus a direction Q&A with the user.
- Brand decisions confirmed with the user: name **"ARE iptv"**, **dark-first**, **electric-blue** accent (#3b82f6), full TV-focused component set, guided multi-step playlist onboarding, and next-gen features (universal search, stream-health indicators, continue watching, catch-up/TimeShift, smart favorites, multi-view, AI auto-categories & recommendations).
- **Logo designed in-system.** `assets/logo.svg` (full lockup), `assets/logo-mark.svg` (icon), `assets/favicon.svg` (small-scale) — a solid electric-blue focus tile with a play triangle and broadcast arcs ("Broadcast play" direction), tying the mark to the focus-tile hero interaction. Selected from a 3-direction exploration (`guidelines/Logo Exploration.html`).
- **Fonts are Google Fonts substitutions** chosen for the brief (see below). Swap for licensed files if the brand adopts specific faces.

---

## CONTENT FUNDAMENTALS — how ARE iptv writes

**Voice:** confident, quiet, and functional — like a premium AV device, not a chatty app. The interface gets out of the way of the content (posters, live video, the guide). Copy is sparse; imagery leads.

- **Person:** address the viewer as **you** ("Continue watching", "Pick up where you left off"). The app refers to itself in third person only in legal/onboarding ("ARE iptv never stores your credentials off-device").
- **Casing:** **Sentence case** for everything readable — titles, buttons, menu items ("Add playlist", "Live TV", "Continue watching"). **UPPERCASE** reserved only for tiny overlines and status badges ("LIVE", "4K", "NEW", "CATCH-UP"), always with wide letter-spacing.
- **Tone:** calm and precise. Verbs are short and direct: *Add, Play, Resume, Record, Pin, Hide, Sort, Refresh guide*. No exclamation marks in UI. No hype.
- **Numbers & metadata:** terse, dot-separated — `Drama · Crime · 2019 · 5 seasons`, `★ 9.5`, `1080p · H.265`. Time in the guide is 24h-capable (`20:00 – 20:45`).
- **Empty & setup states** are instructive, never cute: "No playlists yet. Add an M3U link or Xtream Codes login to get started." Explain the *next action*, not the feeling.
- **Technical fields** (URLs, usernames, ports, EPG links) render in the **mono** typeface so users can verify them character-by-character.
- **Emoji:** **not used** in UI copy. The one exception is the **stream-health traffic light**, expressed as colored dots (🟢🟡🔴 semantics) — but implemented as styled dots/tokens, not literal emoji glyphs.
- **AI features** are labeled honestly and quietly: a small violet "Smart" / "AI" tag, never anthropomorphized ("Suggested for you", "Auto-organized").

Examples:
- Button: `Add playlist` · `Play` · `Resume` · `Open guide` · `Multi-view`
- Overline/badge: `LIVE` · `CATCH-UP` · `4K HDR` · `NEW EPISODE` · `SMART`
- Metadata line: `BBC One HD · Now: The News at Ten · 22:00 – 22:30`
- Empty state: `Nothing recorded yet — schedule a recording from any live channel.`

---

## VISUAL FOUNDATIONS — the look of ARE iptv

**Overall mood:** cinematic, dark, poster-forward — closer to Apple TV / Netflix on a big panel than to a playlist viewer. The chrome is minimal and glassy; the *content* provides the color.

**Color:**
- Dark-first neutral ramp is a cool, near-black blue-black (`--ink-950 #06070a` → surfaces `#14161c / #1f232c / #262a34`). Backgrounds are deep so posters and live video pop.
- One signature accent: **electric blue `#3b82f6`** — focus rings, active nav, primary buttons, progress. Used sparingly; it means "this is where you are / this is the action."
- Status is a strict traffic-light set: green `#22c55e` (stable), amber `#eab308` (moderate), red `#ef4444` (poor / **LIVE**). Violet `#8b5cf6` denotes **AI/smart** surfaces only.
- Max 1–2 background tones per screen; never gradient-wash a whole screen. Gradients appear only as **protection scrims** over artwork (`--scrim-bottom`, `--scrim-left`) so text stays legible on posters and live video.
- Light theme inverts to soft cool greys (`#f3f5f9` base, white cards) with a slightly darker blue accent (`#2563eb`) for contrast.

**Type:** **Space Grotesk** (display — screen titles, hero, headings; distinctive, premium), **Manrope** (body/UI — humanist, extremely legible at distance), **JetBrains Mono** (technical fields). Sizes run large for a 10-foot UI — hero 64px, screen titles 44px, base UI 16px, nothing below 14px. Display headings use tight tracking (`-0.02em`); badges use wide caps tracking (`0.10em`).

**Spacing & layout:** 8px grid. Generous **TV overscan safe area** (`--safe-x 64px`, `--safe-y 40px`) keeps content off the panel edge. Content is organized as horizontal **rails** of tiles with a peeking next-tile (`--rail-peek 80px`) to signal scrollability. A slim left **rail nav** (104px collapsed → 280px expanded on focus).

**Backgrounds:** flat deep surfaces, no textures or patterns. Featured content uses a **full-bleed hero image** with a bottom+left scrim. Behind detail pages, a large **blurred backdrop** of the poster art (heavy blur + dark overlay) sets mood.

**Focus state (the hero interaction):** on D-pad focus a tile **scales to 1.06** with an **accent glow ring** (`--focus-glow`: inset base ring + blue ring + soft blue bloom) and a stronger shadow lift. Focus travel is **fast (150ms)** with a slight overshoot ease (`--ease-emph`) so movement feels responsive but alive. Everything focusable has a visible indicator — non-negotiable for remote use.

**Hover/press:** hover (for pointer users / mouse on Google TV) lightens surface one step and brightens accent. Press **shrinks to 0.97** and darkens the accent (`--accent-press`). Buttons transition color in 150ms.

**Borders:** hairline, low-opacity white (`--border-subtle 6%` → `--border-strong 18%` in dark). Used to separate glass panels and list rows; content tiles rely on shadow + spacing, not borders.

**Elevation:** dark UI leans on **layered surfaces + glow** more than drop shadow. Resting tiles get a soft ambient lift (`--shadow-tile`); focused tiles get a deeper lift plus the accent ring. Overlays (player controls, guide scrim, dialogs) are **glass**: `--surface-glass` + `backdrop-filter: blur(20px)` + `--shadow-glass`.

**Radius:** soft, not pill-y. Tiles/buttons/inputs `--r-md 14px`, cards/panels `--r-lg 20px`, hero/dialogs/sheets `--r-xl 28px`. Chips, toggles, and badges are full pills (`--r-pill`). Posters keep `--r-md`.

**Transparency & blur:** used deliberately — only for **overlays on top of content** (player HUD, guide over live video, dialogs, the expanded search). Never blur for decoration on solid screens.

**Motion:** snappy. Focus/nav 90–150ms; overlays and hero crossfades 220–360ms. Entrances ease-out; focus pop uses a gentle overshoot; nav never bounces. Respects `prefers-reduced-motion` (durations collapse, scale softens).

**Imagery vibe:** true-color, high-contrast poster/still artwork — cinematic and saturated, cool-leaning in the dark theme thanks to the deep blue-black surround. Channel logos sit on subtle rounded chips so mixed-quality logos read consistently.

---

## ICONOGRAPHY

- **Icon set: [Lucide](https://lucide.dev) (CDN).** Clean 2px-stroke line icons, rounded joins — matched to the humanist body type and the calm, functional voice. This is a **substitution** (no brand icon set was provided) — flagged for the user; swap if the brand adopts its own set.
- Icons are **line/stroke, never filled**, except a few "active/playing" states (filled play triangle, filled heart for a favorited item) and the LIVE dot.
- Default icon size 24px (nav 28px, inline-with-text 20px, tile overlays 20px). Stroke color follows text tokens; active icons take `--accent`.
- **No emoji** as icons. **No hand-drawn SVG** icons in this system — always use Lucide (or a copied brand set later). The stream-health indicator uses colored **dots** (tokens), not emoji.
- Channel logos and movie/series posters are **imagery**, supplied by the playlist/metadata provider (TMDB-style); the system provides consistent rounded containers and fallbacks (a mono channel-initial chip when no logo exists).
- Lucide glyphs used throughout: `home, tv, radio, film, clapperboard, layout-grid, search, settings, heart, star, play, pause, rewind, fast-forward, skip-forward, list, folder, signal, wifi, cast, picture-in-picture-2, columns-2, grid-2x2, plus, chevron-right, lock, sparkles, refresh-cw, clock, calendar, sliders-horizontal`.

---

## INDEX — what's in this system

**Foundations (root):**
- `styles.css` — global entry point (import this one file). `@import`s everything below.
- `tokens/fonts.css` · `colors.css` · `typography.css` · `spacing.css` · `radius.css` · `shadows.css` · `motion.css` · `base.css`
- `assets/logo.svg` — brand lockup · `assets/logo-mark.svg` — icon mark · `assets/favicon.svg` — small-scale mark.
- Foundation specimen cards live in `guidelines/` (Colors, Type, Spacing, Radius, Elevation, Motion, Brand groups on the Design System tab).

**Components** (`components/<group>/` — reusable primitives, see each `.prompt.md`):

- **core/** — `Button`, `IconButton`, `Chip`, `Badge`, `Icon` (Lucide wrapper).
- **category/** — `CategoryCard` (folder browse tile with poster mosaic), `CategoryRow` (filter-column list row). The grouping every content type shares — Live TV, Movies, Series, EPG.
- **media/** — `PosterTile`, `ChannelTile`, `Rail`, `Hero`, `ContinueCard`.
- **navigation/** — `SidebarNav`, `Tabs`.
- **guide/** — `GuideCell`, `StreamHealth`.
- **forms/** — `TextField`, `Switch`, `StepIndicator`.
- **player/** — `PlayerControls`.
- **overlay/** — `Dialog`.

**UI kit** (`ui_kits/are-tv/` — full-screen recreations):
- Home dashboard, Live TV player + EPG overlay, full EPG guide grid, Movie/Series detail, Channel browse, Universal search, Playlist onboarding wizard, Settings, Multi-view, Favorites.

**Meta:**
- `thumbnail.html` — homepage tile.
- `SKILL.md` — portable Agent Skill wrapper.

---

## CAVEATS / substitutions to confirm
1. **Logo** designed in-system ("Broadcast play" mark); confirm before any external/print use.
2. **Fonts** (Space Grotesk / Manrope / JetBrains Mono) are Google Fonts choices for the brief, not confirmed brand faces.
3. **Icons** are Lucide (substitution), not a supplied brand set.
