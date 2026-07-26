# Sidebar Glass V1 — "The Rail Becomes an Object" Design Spec

> **Status:** Agreed direction · not yet implemented.
> **Type:** Surface pass on one component, plus a user setting. No navigation-behaviour change.
> **Predecessor:** [`glass-v2-design.md`](./glass-v2-design.md) — every Glass V2 primitive
> (`glassSurface`, `softShadow`, `accentLensBrush`, the capability-tier fallback) is reused as-is.
> Nothing new is invented; this spec plumbs a setting and swaps a hand-rolled surface for the
> shared recipe.
> **Visual pitch + live prototype:** https://claude.ai/code/artifact/7ecc886c-203c-4de9-92c4-f69eb76ed630

---

## Contents

1. [Why](#1-why) · 2. [Locked decisions](#2-locked-decisions) · 3. [The two modes](#3-the-two-modes) ·
4. [Geometry & tokens](#4-geometry--tokens) · 5. [Light theme](#5-light-theme) ·
6. [Behaviour that must not regress](#6-behaviour-that-must-not-regress) · 7. [The setting](#7-the-setting) ·
8. [Capability tiers](#8-capability-tiers) · 9. [Strings](#9-strings) · 10. [Risks](#10-risks) ·
11. [Build plan / file index](#11-build-plan--file-index) · 12. [Open decisions](#12-open-decisions)

---

## 1. Why

Glass V2 gave a real backdrop blur and a lit edge to nearly every surface in the app — dialogs, the
HUD, cards, tabs, fields. The **sidebar is the most persistent chrome we own** and it is the one
surface still painted flat onto the screen edge: `SidebarNav.kt:132` hand-rolls a `drawBehind`
vertical gradient + a 1px right-edge seam. It is full-height, square-cornered, and flush to the
bezel.

The request: make the rail float — a glass *box* hovering off the edge when expanded, a small glassy
capsule when collapsed — and let the user choose between that and the current full-height rail in
Settings.

There is no behaviour problem to fix here. This is purely a **material + shape** change to one
component, gated behind a preference. That framing is what keeps it a ~75-line change rather than a
rework.

---

## 2. Locked decisions

Decided with the product lead (question round, this session). Do not relitigate.

| # | Decision | Value | Rationale |
|---|---|---|---|
| D1 | **Default style for existing users** | **Floating box** | The redesign becomes the app's identity; the setting exists to go back. |
| D2 | **What happens on expand** | **Pushes content** | Content slides right on focus entry. No overlap, nothing hidden under glass — same `Row`-sibling layout as today. |
| D3 | **Box height** | **Full height minus a 20dp inset on all four sides** | Architectural, matches the top bar's rhythm, and avoids scroll/clip problems that a content-hugging capsule (~590dp of items) would hit on a 540dp panel. |
| D4 | **Both styles share one nav** | Same items, focus model, lens, expand trigger | Only the container shape/surface differs → a single ternary, not two code paths. |
| D5 | **Setting placement** | Settings → Appearance, directly under Theme | Same class of decision; instant + reversible, no confirm, no restart. |

---

## 3. The two modes

Both render `DefaultSidebarNavItems` (10 rows) with identical behaviour. Only the container changes.

### 3.1 Floating box — `SidebarStyle.FLOATING` (new default)

Reads as an **object sitting on the page**.

- Inset 20dp on all four sides → 20dp of the ambient backdrop runs behind every edge. *That gap is
  the whole idea.*
- 28dp corner radius (`radius.xl`). At 96dp collapsed width the capsule is nearly pill-shaped — it
  looks deliberately small, not truncated.
- Surface: `Modifier.glassSurface(RoundedCornerShape(radius.xl), elevated = true)` — the exact
  recipe dialogs use. Elevated fill so the 10 labels stay legible; the gradient hairline wraps all
  four sides; `softShadow` carries the lift.
- The item column overflows a 540dp panel, so a **vertical gradient mask** dissolves rows at the
  top/bottom radius instead of the `clip()` slicing them.

### 3.2 Full height — `SidebarStyle.EDGE` (today's rail, kept verbatim)

Reads as **architecture holding the page**.

- Flush to the bezel, square corners, 104 → 212dp, separated from content by a single lit hairline
  on the right edge only.
- Keeps the existing `drawBehind` seam exactly — **no change** to the current render path.
- More screen for content, and the safe choice on TVs with heavy overscan (see §10).

---

## 4. Geometry & tokens

Every value below is a real token or a new constant named alongside the existing ones. `220ms
easeOut` is `motion.durBaseMs` (verified — not 250).

| Property | Floating box | Full height | Source |
|---|---|---|---|
| Collapsed width | 96dp | 104dp | `spacing.sidebarBoxWidth` *(new)* / `spacing.sidebarWidth` |
| Expanded width | 236dp | 212dp | `spacing.sidebarBoxWidthOpen` *(new)* / `spacing.sidebarWidthOpen` |
| Total reserved gutter | 136 → 276dp | 104 → 212dp | box width + 2×inset; sidebar reports its own width to the `Row` |
| Inset (all four sides) | 20dp | 0 | `spacing.sidebarInset` *(new)* |
| Corner radius | 28dp | 0 | `radius.xl` |
| Surface recipe | `glassSurface(…, elevated = true)` | `drawBehind` gradient (unchanged) | `Glass.kt` |
| Blur radius | 32dp | 24dp | `BlurElevatedDp` / `BlurBaseDp` |
| Edge treatment | gradient hairline, all round | hairline on right edge only | `glassBorderBrush()` |
| Lift | `softShadow` · .10 dark / .14 light | none | `softShadow()` |
| Row height / icon | 44dp / 22dp | 44dp / 22dp | unchanged |
| Row start inset | 15dp | 23dp | function of style; centres the 22dp icon in each container's own width |
| Inner padding (focus-glow bleed) | 10dp | existing 16dp | keeps the ring inside the clipped 28dp shape |
| Expand animation | 220ms `easeOut` | 220ms `easeOut` | `motion.durBaseMs` |

**New tokens (Spacing.kt):**

```kotlin
val sidebarInset: Dp = 20.dp,
val sidebarBoxWidth: Dp = 96.dp,
val sidebarBoxWidthOpen: Dp = 236.dp,
```

**Accent / glass values (already shipped, listed for completeness):**

| Token | Dark | Light |
|---|---|---|
| Accent | `Blue500 #3B82F6` | `Blue600 #2563EB` |
| Selection lens | `accentHover .40 → accent .26` | `white .72 → accentHover .26` |
| Elevated glass fill | `rgba(20,22,28, .72)` | `rgba(255,255,255, .82)` |
| Hairline (`borderGlass`) | white .10 | ink .14 |

---

## 5. Light theme

A floating box is **where the light theme goes wrong**, so it gets first-class attention (this was
called out explicitly). White glass on the off-white page (`#F3F5F9`) has almost no edge: in dark
mode the lit top edge separates the box; in light mode there is no light to catch.

Three corrections — all already supported by shipped tokens, none new:

1. **The hairline turns dark.** `borderGlass` is already ink·14 (not white) in the light palette, so
   `glassBorderBrush()` gives the box a *drawn* edge instead of a lit one. Nothing to change.
2. **The shadow does the lifting.** `softShadow` already branches on `c.isDark`; in light it must be
   the denser alpha (.14 / blur 24dp) so the box reads as raised without an edge to rely on. In dark
   it stays a whisper at .10.
3. **Selection inverts, as Apple's does.** `accentLensBrush()` already flips in light to a white·72
   lens over an accent wash with a **dark** label (`lensContentColor` = `textPrimary` in both
   themes). No tinted label — measured contrast on the pale lens drops toward 3:1 for a hued label.

The prototype renders all four combinations (float/edge × dark/light) so light can be signed off
against dark before build.

---

## 6. Behaviour that must not regress

Behaviour is **identical** in both modes. The sidebar stays a `Row` sibling of the content in
`AppShell.kt`; it simply reports a larger total width in floating mode.

| Behaviour | Rule |
|---|---|
| Expand trigger | Any row holding D-pad focus (`focusedItemId != null`). Never a click, never a timer. Unchanged. |
| Entry from content | `◀` from the leftmost tile. Directional focus resolves as today — same sibling layout. |
| **No auto-focus** | The sidebar must still NOT request focus on recomposition. That was the "Back always lands on the sidebar" bug (`SidebarNav.kt:108-114`). The inset changes nothing here — do not add a restorer. |
| Overflow | 10 items over ~484dp usable panel: keep `verticalScroll`, add a vertical gradient mask so rows dissolve at the radius. |
| Focus-glow clipping | `glassSurface` ends in `.clip(shape)`; a row's outer glow would be cut at the 28dp corner. 10dp inner padding keeps the ring inside the material. |
| RTL | Inset is `PaddingValues(start = …)`, never `left`. 21 translated locales. |

---

## 7. The setting

**Settings → Appearance**, one segmented row directly under Theme. `AreSegmentedControl` (the
control we already ship). Two options: **Floating** / **Full height**. Change is instant and
reversible → no confirm, no restart.

```
Sidebar style     [ Floating ] [ Full height ]
A floating glass panel, or a full-height rail against the edge
```

Persistence follows `ThemeMode` exactly:

```kotlin
enum class SidebarStyle { FLOATING, EDGE }
// UserSettings.kt: SIDEBAR_STYLE key + Flow<SidebarStyle> (default FLOATING) + suspend setter
```

`MainActivity` collects `settings.sidebarStyle` next to `themeMode` and threads it through
`AreIptvAppShell` → `AreSidebarNav(style = …)`.

---

## 8. Capability tiers

No new tier logic. `glassSurface` already branches on `LocalGlassTier`:

- **Tier A/B** (backdrop blur available): the box samples the ambient backdrop through its shape.
- **Tier C** (no blur): falls through to the opaque path with denser token alphas — exactly as
  every other glass surface does. **Floating still reads** (inset + shape + shadow); it just doesn't
  sample. No special-casing required.

Over the player's video **SurfaceView**, backdrop sampling is impossible (Glass V2 §5). Not relevant
here — the sidebar never overlaps the fullscreen player — but if a future layout puts it there, it
falls back to the denser opaque fill like the HUD.

---

## 9. Strings

Four keys, added to `values/strings.xml` **and all 21 translated locales** in the same pass, or they
silently fall back to English (project i18n rule):

| Key | English |
|---|---|
| `settings_sidebar_style` | Sidebar style |
| `settings_sidebar_style_desc` | A floating glass panel, or a full-height rail against the edge |
| `settings_sidebar_style_floating` | Floating |
| `settings_sidebar_style_edge` | Full height |

Locales: `az, b+pt+BR, b+pt+PT, bg, cs, da, de, el, es, fi, fr, hu, it, nb, nl, pl, ro, ru, sv, tr, uk`.

---

## 10. Risks

| Severity | Risk | Mitigation |
|---|---|---|
| **High** | **Overscan eats the inset.** TVs that crop 3–5% can vanish the 20dp gap and land the box edge on the bezel. | Verify on the emulator's overscan profile before shipping. This is the strongest argument for keeping EDGE as a genuine option, not a legacy toggle. |
| Medium | **A second full-height blur.** Making the rail elevated raises the radius 24→32dp over the tallest surface in the app. | Measure frame time on Tier B before committing to 32dp; drop to 24dp if it costs. |
| Medium | **Glass over video.** Backdrop sampling can't reach a SurfaceView. | Not currently a code path; documented fallback (§8) if it ever becomes one. |
| Low | **Two more dp constants.** Four width tokens where there were two. | Name them so the pair is obvious and the branch reads as one ternary. |

---

## 11. Build plan / file index

Seven files. Every primitive already shipped in Glass V2.

| File | ~LOC | Change |
|---|---|---|
| `data/settings/UserSettings.kt` | 14 | `enum class SidebarStyle { FLOATING, EDGE }`, `SIDEBAR_STYLE` key, Flow (default `FLOATING`), setter. Same shape as `ThemeMode`. |
| `ui/theme/Spacing.kt` | 3 | `sidebarInset`, `sidebarBoxWidth`, `sidebarBoxWidthOpen` alongside the existing rail widths. |
| `ui/components/SidebarNav.kt` | 35 | A `style` param. Wrap the Column in an inset `Box` and branch the surface: `glassSurface(RoundedCornerShape(radius.xl), elevated = true)` for FLOATING, keep the current `drawBehind` seam for EDGE. Add the scroll mask. Row start inset becomes a function of style. |
| `ui/shell/AppShell.kt` | 4 | Thread `sidebarStyle` into `AreSidebarNav`. Row layout unchanged — the sidebar reports its own total width, so push is free. |
| `MainActivity.kt` | 3 | Collect `settings.sidebarStyle` next to `themeMode`; pass into the shell. |
| `ui/settings/SettingsScreen.kt` | 16 | One `AreSegmentedControl` row in Appearance, under Theme. |
| `res/values*/strings.xml` | 4 keys × 22 | The four keys of §9 across every locale. |

---

## 12. Open decisions

None blocking. To confirm on-device during build:

- **Inset value (20dp)** vs overscan — may need to bump to 24dp or expose an "extra safe area"
  interaction; decide after the overscan-profile check (§10).
- **Blur radius 32 vs 24dp** for the elevated rail — decide on the Tier B frame-time measurement.
- **Collapsed width 96dp** — confirm the 22dp icon still centres cleanly at the 15dp start inset on
  device; nudge if it reads off-centre.
