# Glass Redesign — V1 Design Spec

> Status: **Agreed design direction, not yet implemented.** Shared reference for moving the app to a
> unified "glass" surface language — starting with the player HUD and rolling out to the whole app.
> Every decision below was made deliberately; alternatives considered are noted so we don't
> relitigate.
>
> **Visual companion:** [`glass-redesign-v1.html`](./glass-redesign-v1.html) — an interactive
> before/after pitch (open in a browser). It contains: a live screenshot of the current HUD, three
> HUD directions, the app-wide glass component pairs, the sidebar + interaction-state matrix, tab
> alternatives, and **full-page mockups (with the real sidebar + top menu) for TV Guide, Settings,
> Search, and Favorites**, plus a **dialogs/menus/back-affordance** gallery and a **Persian (RTL)**
> home screen. Hosted copy:
> https://claude.ai/code/artifact/c2a68203-8a4e-4e6b-972f-81dd907019df

## 1. Goal & scope

Make the app look **shinier and more modern** by adopting one **glass surface language** across every
chrome surface — translucent fill, a hairline "lit" edge, and the existing electric-blue focus glow.
The trigger was the **player HUD** (flat, one long 13-icon row); to keep the product coherent, the
same material extends to the rail, buttons, cards, badges, tabs, Settings boxes, and dialogs.

**V1 is a surface/material pass, not a re-layout.** Content, information architecture, D-pad model,
and copy stay as they are. What changes is the *material* a surface is drawn in (plus the one HUD
layout option we pick). No new features, no new strings expected.

## 2. Locked decisions

| Area | Decision | Rejected alternative (why) |
|------|----------|----------------------------|
| **Material source** | Reuse the existing translucent `surfaceGlass` + border pattern (already in `PlayerControls.kt`), formalised into one helper | Invent a new glass token set (duplicates what exists) |
| **"Glass" = translucency, not blur** | The translucent fill *is* the glass — content behind shows through | Real-time backdrop blur everywhere (Compose can't cheaply blur an arbitrary backdrop; wasted GPU over the solid browse page) |
| **Real backdrop blur** | **Only** on dialogs + player HUD (surfaces over media), gated `SDK_INT >= 31` | Blur on rail/cards/settings over the solid page (nothing to refract) |
| **Lit top edge** | Vertical **gradient border** (bright top → faint bottom) | `drawWithContent` inner-shadow maths (fragile, per-API) |
| **Focus** | Unchanged — keep composing `TvFocusable` on top of glass | Bake focus into the glass helper (would fight the existing ring/glow) |
| **Layout / IA** | Unchanged (surface pass only) | Re-flow screens (out of scope for V1) |
| **HUD layout** | **Pick one** of A/B/C (see §7) — recommend **B (Cinematic Unified)** | Ship all three / leave HUD flat |
| **App icon** | Redraw as a frosted-blue glass tile | Keep the flat cyan mark |

## 3. New semantic tokens

Add to `AreIptvColors` in `ui/theme/Color.kt`, filling both `AreIptvDarkColors` and
`AreIptvLightColors`. `surfaceGlass` already exists — reuse it; add the other three.

| Token | Dark | Light | Use |
|-------|------|-------|-----|
| `surfaceGlass` *(exists)* | `0x8C1E222C` — rgba(30,34,44,.55) | `0x99FFFFFF` — rgba(255,255,255,.60) | Default glass fill (cards, chips, rail, buttons) |
| `surfaceGlassElevated` *(new)* | `0xB814161C` — rgba(20,22,28,.72) | `0xD1FFFFFF` — rgba(255,255,255,.82) | Modals/dialogs over video — denser so text stays legible |
| `glassHighlight` *(new)* | `0x38FFFFFF` — white .22 | `0xB3FFFFFF` — white .70 | Top stop of the border gradient (the lit edge) |
| `borderGlass` *(new)* | `0x1AFFFFFF` — white .10 | `0x240F141E` — ink .14 | Bottom stop of the border gradient (light needs a dark edge to read) |

## 4. The shared modifier

The whole language is one helper. New file `ui/theme/Glass.kt`:

```kotlin
/** Glass surface: translucent fill + gradient hairline border (lit top edge).
 *  The fill alpha IS the glass — content behind shows through. No backdrop blur. */
fun Modifier.glassSurface(
    shape: Shape,
    elevated: Boolean = false,
): Modifier = composed {
    val c = AreIptvTheme.colors
    background(if (elevated) c.surfaceGlassElevated else c.surfaceGlass, shape)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(listOf(c.glassHighlight, c.borderGlass)),
            shape = shape,
        )
        .clip(shape)
}
```

- Focus is unchanged: keep composing `TvFocusable` / `Modifier.tvFocusable` (`ui/theme/Focus.kt`)
  **outside** `glassSurface` so the accent ring + glow still draw on top.
- Cross-fade fill changes with `ui/theme/Motion.kt` tokens (150–200ms); honor reduced-motion.

## 5. Per-component build table

Each component swaps its current solid fill for `glassSurface(shape)` at the given radius. "Blur?" =
whether real backdrop blur is worth it (only where media sits behind).

| Component | File(s) | Change | Shape | Blur? |
|-----------|---------|--------|-------|-------|
| **App icon** | launcher `ic_launcher` + brand mark in `shell/TopBar.kt` | New adaptive icon: frosted-blue tile, inner highlight, soft glow | squircle | n/a |
| **Nav rail item** | `components/Rail.kt`, `components/SidebarNav.kt` | Focused/selected chip → `glassSurface(radius.lg)`; keep `TvFocusable` ring | `radius.lg` 14dp | No — over solid page |
| **Icon buttons** | `components/IconButton.kt` (`AreIconButtonVariant.Glass`) | Point the existing Glass variant at `glassSurface`; add gradient border | circle / `radius.md` | Only over video |
| **Buttons** | `components/Button.kt` (`AreButton`) | Primary = accent-gradient glass; secondary/ghost = `glassSurface` | `radius.md` 12dp | No |
| **Content cards** | `ChannelTile.kt`, `PosterTile.kt`, `CategoryCard.kt`, `ContinueCard.kt`, `Hero.kt` | Info footer + focused frame → glass; clip poster to shape | `radius.lg` | Yes — over poster |
| **Category row** | `components/CategoryRow.kt` | Selected row → `glassSurface(radius.lg)` | `radius.lg` | No |
| **Badges & chips** | `components/Badge.kt`, `components/Chip.kt` | Tonal glass: hue @ ~28% + hue-400 border + highlight (LIVE keeps red) | `radius.sm` / pill | No |
| **Tabs** | `components/Tabs.kt` | Inactive = glass pill; active = accent-gradient glass | pill | No |
| **Settings boxes** | `settings/SettingsPanes.kt`, `settings/SettingsScreen.kt` | Section card → `glassSurface(radius.xl)`; hairline dividers @ `borderGlass`; leading icon well → glass | `radius.xl` 22dp | No |
| **Toggle** | `components/Switch.kt` | ON track = accent-gradient glass + glow; thumb stays white | pill | No |
| **Dialogs / modals** | `components/Dialog.kt` (`AreDialog`), `settings/*Dialog.kt`, `RecordVolumeDialog` | `glassSurface(radius.xl, elevated = true)` + real blur (§6); focus default action | `radius.xl` | Yes |
| **Player HUD** | `components/PlayerControls.kt` | Upgrade to `glassSurface` + chosen HUD option (§7) | `radius.xl` | Yes |
| **TV Guide / EPG** | `ui/guide/*` (screen), `components/GuideCell.kt` | Channel cells + programme cells → glass; focused programme = accent ring + glow; on-air = left accent edge; day selector → segmented control; groups → glass tabs | `radius.lg` | No |

| **Search page** | `ui/search/*`, `components/TextField.kt` (Search variant), `PosterTile.kt`/`ChannelTile.kt` | Query field wrapped in `glassSurface` (it *is* the entry focus target — carries the ring); result-type filters → segmented control; results → glass cards | `radius.lg` | No |
| **Favorites page** | `ui/favorites/*`, `ChannelTile.kt`/`PosterTile.kt` | Type split → segmented control; heart moves into a small glass badge (top-`end`); focused card = accent ring | `radius.lg` | Yes — over poster |
| **Back affordance** | Detail/Settings header back control | Real focusable `Back` pill → `glassSurface` pill + `TvFocusable` ring (remote `Back` still works everywhere) | pill | No |

> **Shell is part of every page.** The nav **sidebar** (`Rail.kt`/`SidebarNav.kt`) and top **menu**
> (`shell/TopBar.kt`) frame every screen except the full-bleed player. Design and review each page *with*
> its sidebar and menu — the companion HTML renders TV Guide, Settings, Search and Favorites in full-page
> context for this reason.

### 5a. The full navigation rail (fit ALL items)

The sidebar is **10 items**, in this exact order (from `SidebarNav.kt` — never truncate a mock to fewer):

| # | id | String | Material icon |
|---|-----|--------|---------------|
| 1 | `home` | `nav_home` | `Home` |
| 2 | `live` | `nav_live_tv` | `LiveTv` |
| 3 | `guide` | `nav_tv_guide` | `TableChart` |
| 4 | `movies` | `nav_movies` | `Movie` |
| 5 | `series` | `nav_series` | `Theaters` |
| 6 | `search` | `nav_search` | `Search` |
| 7 | `favorites` | `nav_favorites` | `Favorite` |
| 8 | `recordings` | `nav_recordings` | `VideoLibrary` |
| 9 | `streams` | `nav_streams` | `Link` |
| 10 | `settings` | `nav_settings` | `Settings` |

The rail collapses to a 104dp icon strip and expands to 280dp with labels while any item inside holds
focus (existing behaviour — unchanged). Under glass: rest = transparent, focused = `glassSurface` + ring,
current screen = accent-gradient chip (see §6b).

## 6. Blur policy

The translucent fill alone reads as glass everywhere. Real backdrop blur is **optional** and only on
surfaces with imagery behind them — **dialogs and the player HUD only**:

```kotlin
// API 31+ only; on <=30 the translucent fill already carries the look.
val blurMod = if (Build.VERSION.SDK_INT >= 31)
    Modifier.graphicsLayer {
        renderEffect = RenderEffect
            .createBlurEffect(24f, 24f, Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
    }
else Modifier
```

Rail, category rows, settings cards and tab strips sit over the solid dark page — **no blur** there
(nothing to refract, wasted GPU). Never blur the whole browse background.

## 6b. Interaction states (sidebar & every control)

Three distinct states must stay visually separated — today selection (flat teal fill) and focus (hard cyan
ring) compete. Under glass:

- **Rest** — no fill (transparent), muted `textSecondary`.
- **Focused** (D-pad landing) — `glassSurface` fill + the existing `TvFocusable` accent ring + soft glow.
- **Selected** (current screen/tab) — accent-gradient glass chip (denser than plain focus).

The sidebar (`components/Rail.kt` / `SidebarNav.kt`) is the canonical example: Home = rest, focused item = glass +
ring, current screen = accent-gradient glass. The companion HTML renders all three side by side, plus a
rest-vs-focused matrix for Button / IconButton / Tab / Content card.

## 6c. Tab style (open decision)

Today the Settings tabs are five independent pill buttons in a row. Better alternatives (all keep the same D-pad
left/right behaviour) — rendered in the companion HTML:

| Style | What it is | Best when |
|-------|-----------|-----------|
| Pill row *(current)* | Five separate chips; active solid | — |
| **Segmented control** *(recommended)* | One glass track; accent indicator slides between segments | Few tabs; reads as one selector |
| Underline / ink-bar | Text labels + animated accent bar under active | Editorial, minimal chrome |
| Vertical sub-rail | Tabs stack down the left, glass-lit active row | Screens with many tabs |

Component: `components/Tabs.kt`. **Open decision:** which style ships.

## 7. HUD layout options

The material pass upgrades the HUD to `glassSurface`, but the HUD's *layout* is also being modernised.
Pick one (rendered in the companion HTML):

- **A — Floating Cluster.** Break the monolith into three frosted pods (metadata / centred primary
  transport with an accent-glow play disc / utilities). Boldest; needs pod-to-pod focus work.
- **B — Cinematic Unified (recommended).** Keep the single bar; add layered glass (gradient + inner
  highlight + accent top hairline), a radial accent play disc, centred transport, dimmed utilities
  that light on focus/active, prominent tabular timecode. Lowest risk — reuses the current layout and
  D-pad model.
- **C — Minimal Now-Playing.** Drop the card; scrim + big title + hairline progress + light controls.
  Sparest, but forces some utilities behind a "More" overflow.

**Open decision:** which base, how much focus glow, and whether utilities stay all-visible or move
some behind "More".

## 8. Light theme & accessibility (non-negotiable)

- Light glass over the near-white page needs the darker `borderGlass` edge (in the token table) or
  the card vanishes — same rule the theme file already notes.
- Keep text on `textPrimary`/`textSecondary` — never hardcode white. On glass over bright video, add
  a 1-stop scrim under text if contrast drops below 4.5:1.
- Provider logos stay on the dark `logoWell` in both themes (unchanged).
- Every glass control still composes `TvFocusable` — nothing focusable without the accent ring.
- Honor reduced-motion for the fill cross-fade and any glow pulse.

**RTL / bidi.** The app ships 21 **LTR** locales today; the companion HTML includes a Persian (fa) home
mock to show the mirrored shell — sidebar on the **right**, content and menu flowing right-to-left. The
glass language is direction-agnostic; getting it right is a *layout* rule, not a material one:

- Build with `start`/`end` paddings and `Arrangement.Start/End` (they already honor `LayoutDirection.Rtl`)
  — never hardcode `paddingStart`-as-left or absolute left/right.
- Position badges/hearts to the `end`, not a literal right; the gradient border is symmetric so it needs
  no change.
- Adding an RTL locale (fa/ar) is out of V1 scope, but nothing in this material pass should *block* it —
  don't introduce new left/right hardcodes while touching these files.

## 9. Phasing

1. **Foundation** — tokens in `Color.kt`; `Glass.kt` helper; previews in `PreviewShowcase.kt`.
2. **Player HUD** — chosen option (A/B/C); `PlayerControls.kt` → `glassSurface`; real blur on HUD + dialogs.
3. **Chrome** — Rail, TopBar, IconButton, Button; Tabs, Badge, Chip; app icon redraw.
4. **Surfaces** — cards (Channel/Poster/Category/Continue/Hero); Search + Favorites pages; Settings boxes
   + Switch; all dialogs/menus + the back-pill affordance.
5. **Polish** — light-theme contrast pass; 21-locale string check (no new strings expected); on-device
   a11y + perf sweep.

## 10. Out of scope (explicitly V2+)

- Screen re-layouts / IA changes (surface pass only).
- New features or settings.
- Real-time backdrop blur on browse chrome (rail/cards/settings over the solid page).
- Animated/ambient glass effects beyond the focus glow and fill cross-fade.
