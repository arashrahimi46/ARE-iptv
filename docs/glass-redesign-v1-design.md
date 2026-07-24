# Glass Design — V1 Design Spec

> **Status:** Agreed design direction · not yet implemented (design only).
> **Type:** Surface / material pass — reskin every chrome surface in one "glass" language. Not a
> re-layout, not new features.
> **Companion:** [`glass-redesign-v1.html`](./glass-redesign-v1.html) — interactive before/after pitch
> (open in a browser; the ◐ button flips light/dark). Hosted:
> https://claude.ai/code/artifact/c2a68203-8a4e-4e6b-972f-81dd907019df
> **Kickoff:** [`glass-redesign-v1-implementation-prompt.md`](./glass-redesign-v1-implementation-prompt.md)

One material, everywhere: a **translucent fill**, a **hairline lit edge**, and the **existing
electric-blue focus glow**. The translucency *is* the glass — real backdrop blur is reserved for the
two surfaces that sit over moving imagery (dialogs and the player HUD). Nothing about content,
information architecture, the D-pad model, or copy changes.

---

## Contents

1. [Scope](#1-scope) · 2. [Locked decisions](#2-locked-decisions) · 3. [Design tokens](#3-design-tokens) ·
4. [The `glassSurface()` modifier](#4-the-glasssurface-modifier) · 5. [Blur policy](#5-blur-policy) ·
6. [Interaction states](#6-interaction-states) · 7. [Component build sheet](#7-component-build-sheet) ·
8. [Screen notes](#8-screen-notes) · 9. [Light theme](#9-light-theme) · 10. [RTL & i18n](#10-rtl--i18n) ·
11. [Accessibility](#11-accessibility) · 12. [Rollout](#12-rollout) · 13. [Open decisions](#13-open-decisions) ·
14. [File & symbol index](#14-file--symbol-index)

---

## 1. Scope

| In scope (V1) | Out of scope (V2+) |
|---|---|
| Reskin chrome to the glass material: rail, top menu, buttons, cards, badges, tabs, Settings, dialogs, HUD | Screen re-layouts / IA changes |
| Pick & apply the HUD **container style** (§8.1) | New features or settings |
| **Reskin** the existing HUD rearrange editor (the feature already ships — see §8.1) | Real-time blur on browse chrome (rail/cards/settings over the solid page) |
| App-icon redraw (open decision) | Ambient/animated glass beyond the focus glow + fill cross-fade |
| Full light-theme + RTL correctness | Adding an RTL locale (fa/ar) |

> **The shell is part of every page.** The nav **sidebar** (`Rail.kt` / `SidebarNav.kt`) and **top menu**
> (`shell/TopBar.kt`) frame every screen except the full-bleed player. Every mock in the companion HTML is
> drawn *with* its sidebar and menu; review each screen the same way.

**Expected new strings:** none for the material pass. (The HUD rearrange editor already added its
`hud_ctl_*` strings when that feature shipped.)

---

## 2. Locked decisions

| Area | Decision | Rejected alternative (why) |
|------|----------|----------------------------|
| **Material source** | Formalise the existing translucent `surfaceGlass` + border pattern (already in `PlayerControls.kt`) into one helper | A brand-new glass token set (duplicates what exists) |
| **"Glass" = translucency** | The translucent fill *is* the glass; content behind shows through | Backdrop blur everywhere (Compose can't cheaply blur an arbitrary backdrop; wasted GPU over the solid page) |
| **Real backdrop blur** | Only on **dialogs + player HUD** (over media), gated `SDK_INT >= 31` | Blur on rail/cards/settings over the solid page (nothing to refract) |
| **Lit top edge** | A vertical **gradient border** (bright top → faint bottom) | `drawWithContent` inner-shadow maths (fragile, per-API) |
| **Focus** | Unchanged — compose `TvFocusable` *on top of* glass | Baking focus into the helper (fights the existing ring/glow) |
| **Layout / IA** | Unchanged | Re-flowing screens (V2+) |
| **HUD container style** | Pick one of A / B / C — recommend **B** (§8.1) | Ship all three / leave the HUD flat |
| **App icon** | Redraw as a frosted-blue glass tile *(pending sign-off)* | Keep the flat cyan mark |

---

## 3. Design tokens

Add to `AreIptvColors` in `ui/theme/Color.kt`, filling both `AreIptvDarkColors` and `AreIptvLightColors`.
`surfaceGlass` already exists — reuse it; add the other three.

| Token | Dark | Light | Use |
|-------|------|-------|-----|
| `surfaceGlass` *(exists)* | `0x8C1E222C` · rgba(30,34,44,.55) | `0x99FFFFFF` · rgba(255,255,255,.60) | Default glass fill — cards, chips, rail, buttons |
| `surfaceGlassElevated` *(new)* | `0xB814161C` · rgba(20,22,28,.72) | `0xD1FFFFFF` · rgba(255,255,255,.82) | Modals/dialogs over video — denser so text stays legible |
| `glassHighlight` *(new)* | `0x38FFFFFF` · white .22 | `0xB3FFFFFF` · white .70 | Top stop of the border gradient (the lit edge) |
| `borderGlass` *(new)* | `0x1AFFFFFF` · white .10 | `0x240F141E` · ink .14 | Bottom stop of the border gradient (light needs a dark edge to read) |

---

## 4. The `glassSurface()` modifier

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

- Keep composing `TvFocusable` / `Modifier.tvFocusable` (`ui/theme/Focus.kt`) **outside** `glassSurface`
  so the accent ring + glow still draw on top.
- Cross-fade fill changes with `ui/theme/Motion.kt` tokens (150–200 ms); honor reduced-motion.

---

## 5. Blur policy

The translucent fill alone reads as glass everywhere. Real backdrop blur is **optional** and only on
surfaces with imagery behind them — **dialogs and the player HUD**:

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

Rail, category rows, Settings cards and tab strips sit over the solid page — **no blur** (nothing to
refract). Never blur the whole browse background.

---

## 6. Interaction states

Three states must stay visually distinct — today, selection (flat teal fill) and focus (hard cyan ring)
compete. Under glass:

| State | Treatment |
|-------|-----------|
| **Rest** | No fill (transparent), muted `textSecondary`. |
| **Focused** (D-pad landing) | `glassSurface` fill + the existing `TvFocusable` accent ring + soft glow. |
| **Selected** (current screen/tab) | Accent-gradient glass chip — denser than plain focus. |

The nav rail is the canonical example: Home = rest, focused item = glass + ring, current screen =
accent-gradient glass. The companion HTML renders all three side by side, plus a rest-vs-focused matrix
for Button / IconButton / Tab / Content card.

---

## 7. Component build sheet

Each surface swaps its solid fill for `glassSurface(shape)` at the given radius. **Blur** = whether real
backdrop blur applies (only where media sits behind). All files are under
`tv/src/main/java/com/arashrahimi46/iptv/`.

| Surface | File(s) | What changes | Shape | Blur |
|---|---|---|---|---|
| **App icon** | `res/…/ic_launcher*` + brand mark in `ui/shell/TopBar.kt` | Adaptive icon: frosted-blue tile, inner highlight, soft glow | squircle | — |
| **Nav rail** | `ui/components/Rail.kt`, `SidebarNav.kt` | Focused/selected chip → `glassSurface(radius.lg)`; keep `TvFocusable` ring; fit all 10 items (§7.1) | `radius.lg` | No |
| **Top menu** | `ui/shell/TopBar.kt` | Menu icon buttons → glass | `radius.md`/circle | No |
| **Icon buttons** | `ui/components/IconButton.kt` (`Glass` variant) | Point the existing Glass variant at `glassSurface` + gradient border | circle / `radius.md` | Over video only |
| **Buttons** | `ui/components/Button.kt` (`AreButton`) | Primary = accent-gradient glass; secondary/ghost = `glassSurface` | `radius.md` | No |
| **Tabs** | `ui/components/Tabs.kt` → reuse `AreSegmentedControl` (`SegmentedControl.kt`) | Inactive = glass; active = accent-gradient glass (see §8.2) | pill | No |
| **Badges & chips** | `ui/components/Badge.kt`, `Chip.kt` | Tonal glass: hue @ ~28% + hue-400 border + highlight (LIVE keeps solid red) | `radius.sm` / pill | No |
| **Content cards** | `ChannelTile.kt`, `PosterTile.kt`, `CategoryCard.kt`, `ContinueCard.kt`, `Hero.kt` | Info footer + focused frame → glass; clip poster to shape | `radius.lg` | Over poster |
| **Category row** | `ui/components/CategoryRow.kt` | Selected row → `glassSurface(radius.lg)` | `radius.lg` | No |
| **Toggle** | `ui/components/Switch.kt` | ON track = accent-gradient glass + glow; thumb stays white | pill | No |
| **Settings boxes** | `ui/settings/SettingsPanes.kt`, `SettingsScreen.kt` | Section card → `glassSurface(radius.xl)`; hairline dividers @ `borderGlass`; leading icon well → glass | `radius.xl` | No |
| **Dialogs / modals** | `ui/components/Dialog.kt` (`AreDialog`), `settings/FeedbackDialog.kt`, `ParentalPinDialog.kt`, `HudLayoutEditor.kt` | `glassSurface(radius.xl, elevated = true)` + real blur (§5); focus the default action | `radius.xl` | Yes |
| **Back affordance** | Detail/Settings header back control | Focusable `Back` pill → `glassSurface` pill + `TvFocusable` ring (remote `Back` still works everywhere) | pill | No |
| **Player HUD** | `ui/components/PlayerControls.kt` (+ model `ui/player/HudLayout.kt`) | `glassSurface` + chosen container style (§8.1) | `radius.xl` | Yes |
| **TV Guide** | `ui/guide/*`, `ui/components/GuideCell.kt` | Channel + programme cells → glass; focused programme = accent ring + glow; on-air = left accent edge; day selector → segmented; groups → glass tabs | `radius.lg` | No |
| **Search** | `ui/search/*`, `TextField.kt`, tiles | Query field wrapped in `glassSurface` (it *is* the entry focus target — carries the ring); filters → segmented; results → glass cards | `radius.lg` | No |
| **Favorites** | `ui/favorites/*`, tiles | Type split → segmented; heart moves into a small glass badge (top-`end`); focused card = accent ring | `radius.lg` | Over poster |

### 7.1 Navigation rail — all 10 items

From `SidebarNav.kt`, in this exact order. **Never truncate a mock to fewer.**

| # | id | String | Icon | · | # | id | String | Icon |
|---|-----|--------|------|---|---|-----|--------|------|
| 1 | `home` | `nav_home` | `Home` | · | 6 | `search` | `nav_search` | `Search` |
| 2 | `live` | `nav_live_tv` | `LiveTv` | · | 7 | `favorites` | `nav_favorites` | `Favorite` |
| 3 | `guide` | `nav_tv_guide` | `TableChart` | · | 8 | `recordings` | `nav_recordings` | `VideoLibrary` |
| 4 | `movies` | `nav_movies` | `Movie` | · | 9 | `streams` | `nav_streams` | `Link` |
| 5 | `series` | `nav_series` | `Theaters` | · | 10 | `settings` | `nav_settings` | `Settings` |

The rail collapses to a 104 dp icon strip and expands to 280 dp with labels while any item inside holds
focus (existing behaviour — unchanged).

---

## 8. Screen notes

### 8.1 Player HUD

**What already ships (do not rebuild — reskin only):** the HUD button row is data-driven. `ui/player/HudLayout.kt`
defines `HudControl` (the catalog), `HudGroup { TRANSPORT, UTILITIES }`, `HudSlot`, and `DEFAULT_HUD_LAYOUT`;
core transport (rewind / play-pause / fast-forward) is `locked` so the player can't be made unusable. The
order/visibility persists via `UserSettings.hudLayout` (DataStore key `hud_layout`, self-healing decode), and
`ui/settings/HudLayoutEditor.kt` — a focus-trapping rearrange modal with a live preview — is already wired into
`SettingsPanes.kt`. Glass work here = apply `glassSurface` to the HUD bar/buttons and to the editor modal.

**Container style — pick one** (the still-open design choice; rendered in the companion HTML):

- **A — Floating Cluster.** Break the bar into frosted pods (metadata / centred transport with an accent-glow
  play disc / utilities). Boldest; needs pod-to-pod focus work.
- **B — Cinematic Unified (recommended).** Keep the single bar; layered glass (gradient + inner highlight +
  accent top hairline), a radial accent play disc, centred transport, utilities that light on focus. Lowest
  risk — reuses the current layout + D-pad model.
- **C — Minimal Now-Playing.** Drop the card; scrim + big title + hairline progress + light controls. Sparest;
  forces some utilities behind a "More" overflow.

### 8.2 Tabs

Today the Settings tabs are five separate pill buttons. `AreSegmentedControl<T>` (`components/SegmentedControl.kt`)
already exists — the recommended target. All options keep the same D-pad left/right behaviour:

| Style | What it is | Best when |
|-------|-----------|-----------|
| Pill row *(current)* | Separate chips; active solid | — |
| **Segmented** *(recommended, component exists)* | One glass track; accent indicator slides between segments | Few tabs; reads as one selector |
| Underline / ink-bar | Labels + animated accent bar under active | Editorial, minimal chrome |
| Vertical sub-rail | Tabs stack down the left, glass-lit active row | Screens with many tabs |

### 8.3 Everything else

TV Guide, Search, Favorites, Settings and the dialogs follow the build sheet (§7) directly — the companion
HTML renders each as a full page with its sidebar + menu, in both themes.

---

## 9. Light theme

Non-negotiable, and the reason `borderGlass` flips to a dark edge: on the near-white page a white glass card
with a white edge vanishes. Concrete light values (verified in the companion showcase):

| Element | Light value |
|---|---|
| Glass fill | near-white translucent — `surfaceGlass` ≈ white .74, modals `surfaceGlassElevated` ≈ white .86 |
| Hairline edge | ink .14 (`borderGlass`) |
| Icon wells / thumbnails | light neutral (`#E6EBF3`) |
| Text | `textPrimary` / `textSecondary` — **never** hardcoded white |
| Accent gradient + focus ring | unchanged (blue is dark enough for white text) |
| Solid badges (LIVE) | keep a solid red so white text stays legible |

On glass over bright video, add a 1-stop scrim under text if contrast drops below 4.5:1. Provider logos stay on
the dark `logoWell` in both themes.

---

## 10. RTL & i18n

The app ships **21 LTR locales** today; the companion HTML includes a Persian (fa) home mock to show the shell
mirrored (sidebar right, content + menu flowing right-to-left). The glass language is direction-agnostic — RTL
correctness is a *layout* rule, not a material one:

- Build with `start` / `end` paddings and `Arrangement.Start/End` (already honor `LayoutDirection.Rtl`) — never
  hardcode left/right.
- Position badges/hearts to the `end`, not a literal right; the gradient border is symmetric.
- Adding an RTL locale is out of scope, but nothing in this pass should *block* it — introduce no new left/right
  hardcodes while touching these files.

---

## 11. Accessibility

- Every glass control still composes `TvFocusable` — nothing focusable without the accent ring.
- Honor reduced-motion for the fill cross-fade and any glow pulse.
- Maintain ≥ 4.5:1 text contrast on every surface, both themes (scrim under text over video if needed).

---

## 12. Rollout

| Phase | Work | Verify |
|---|---|---|
| **1 · Foundation** | Tokens in `Color.kt`; `Glass.kt` helper; previews in `PreviewShowcase.kt` | `:tv:compileDebugKotlin` + preview render |
| **2 · Player HUD** | Chosen container style; `PlayerControls.kt` → `glassSurface`; reskin `HudLayoutEditor`; real blur on HUD + dialogs | On-device: HUD + editor; reorder still persists |
| **3 · Chrome** | Rail, TopBar, IconButton, Button; Tabs → segmented; Badge, Chip; app icon | `:tv:assembleDebug` (validates strings) + screenshots |
| **4 · Surfaces** | Cards (Channel/Poster/Category/Continue/Hero); Search + Favorites; Settings + Switch; all dialogs + back pill | On-device screenshots per screen |
| **5 · Polish** | Light-theme contrast pass; 22-locale string sanity; a11y + perf sweep | Light/dark on emulator-5554 |

Build with JDK 21. Compile check `./gradlew :tv:compileDebugKotlin`; APK + string validation
`./gradlew :tv:assembleDebug`.

---

## 13. Open decisions

- [ ] **HUD container style** — A / **B (rec)** / C (§8.1).
- [ ] **Should the container style also be a user setting** (sitting next to the existing rearrange editor), or a single shipped style?
- [ ] **Tab style** — **Segmented (rec)** / underline / vertical sub-rail / keep pills (§8.2).
- [ ] **App-icon redraw** — ship the frosted-blue tile, or keep the current mark?
- [ ] **Real blur** — enable on HUD + dialogs for API 31+, or defer and rely on translucency alone?

---

## 14. File & symbol index

Verified real paths (all under `tv/src/main/java/com/arashrahimi46/iptv/`):

- **Theme:** `ui/theme/Color.kt` · `Focus.kt` · `Motion.kt` · `Radius.kt` · `Theme.kt` — new: `ui/theme/Glass.kt`
- **Shell:** `ui/shell/AppShell.kt` · `TopBar.kt`
- **Nav / components:** `ui/components/` → `Rail.kt`, `SidebarNav.kt`, `IconButton.kt`, `Button.kt`, `Tabs.kt`,
  `SegmentedControl.kt` (`AreSegmentedControl`), `Badge.kt`, `Chip.kt`, `Switch.kt`, `Dialog.kt`,
  `ChannelTile.kt`, `PosterTile.kt`, `CategoryCard.kt`, `CategoryRow.kt`, `ContinueCard.kt`, `Hero.kt`,
  `GuideCell.kt`, `TextField.kt`, `PlayerControls.kt`, `PreviewShowcase.kt`
- **Screens:** `ui/guide/` · `ui/search/` · `ui/favorites/` · `ui/settings/` · `ui/player/` · `ui/home/` ·
  `ui/browse/` · `ui/detail/`
- **HUD model (already built):** `ui/player/HudLayout.kt` (`HudControl`, `HudGroup`, `HudSlot`,
  `DEFAULT_HUD_LAYOUT`, `encodeHudLayout`/`decodeHudLayout`) · editor `ui/settings/HudLayoutEditor.kt` ·
  persistence `data/settings/UserSettings.kt` (`hudLayout`, key `hud_layout`, `setHudLayout`)
- **Dialogs:** `ui/components/Dialog.kt` · `ui/settings/FeedbackDialog.kt` · `ParentalPinDialog.kt` ·
  volume flow `ui/player/VolumePicker.kt`
