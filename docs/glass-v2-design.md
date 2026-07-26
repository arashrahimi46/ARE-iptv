# Glass V2 — "Real Glass" Design Spec

> **Status:** Agreed direction · not yet implemented.
> **Type:** Material *correctness* pass. V1 shipped the glass **vocabulary** (translucent fill + lit
> edge). V2 supplies the three things that make it read as glass: **something behind it**, **backdrop
> blur**, and **optics** (vibrancy + refraction + specular).
> **Predecessor:** [`glass-redesign-v1-design.md`](./glass-redesign-v1-design.md) — V1 is not being
> relitigated; every V1 decision stands unless a row below explicitly supersedes it.

---

## Contents

1. [Why V1 doesn't read as glass](#1-why-v1-doesnt-read-as-glass) · 2. [Locked decisions](#2-locked-decisions) ·
3. [The backdrop layer](#3-the-backdrop-layer) · 4. [The library](#4-the-library) ·
5. [Blur over video](#5-blur-over-video) · 6. [Nesting rule](#6-nesting-rule) ·
7. [Tokens](#7-tokens) · 8. [Capability tiers](#8-capability-tiers) · 9. [Coverage gap](#9-coverage-gap) ·
10. [Light theme](#10-light-theme) · 11. [Accessibility & perf](#11-accessibility--perf) ·
12. [Rollout](#12-rollout) · 13. [Open decisions](#13-open-decisions) · 14. [File index](#14-file-index)

---

## 1. Why V1 doesn't read as glass

Four measured root causes, in order of impact. All were verified on `emulator-5554` against the
shipped v1.5.0 build.

### 1.1 There is nothing behind the glass — *the dominant cause*

`ui/shell/AppShell.kt:49` paints one flat opaque `bgBase` (`#0A0B0F`). `surfaceGlass` is
`rgba(30,34,44,.55)`. Composite the two and you get a flat **`#14171E`** — a slightly lighter grey.

> **Glass over an opaque page is arithmetically just a lighter grey.** No border, shadow, blur or
> shader can fix this. Every browse screen (Home, Settings, Movies, Series, Favorites, Guide) is in
> this state today, which is *most of the app*.

V1 locked "no blur over the solid page — nothing to refract" (v1 §5). That was the correct call for
the material, but the missing half was never built: **give the page something worth refracting.**

### 1.2 Real blur over video is physically impossible today

All three player surfaces use media3's default **SurfaceView**:

| Site | File |
|---|---|
| Fullscreen player | `ui/player/LivePlayerScreen.kt:916` |
| Docked mini player | `ui/player/LiveMiniPlayer.kt:224` |
| Multi-view panes | `ui/multiview/MultiViewScreen.kt:401` |

A SurfaceView is composited on its own layer and punches a hole through the window; Compose draws
*above* it and can never sample those pixels. The HUD is therefore translucent but **razor-sharp** —
the video's hard edges pass straight through it. That reads as tinted cellophane, not frosted glass.
`Glass.kt:75-77` and `PlayerControls.kt:173-175` already concede this in comments.

`Dialog.kt:91` works around it with `FLAG_BLUR_BEHIND` (cross-window compositor blur), which is the
one path that *can* blur a SurfaceView. It is **not** extensible to the HUD — see §5.

### 1.3 Glass nested in glass compounds to opaque

HUD buttons (`IconButton.kt:67`, `surfaceGlass` @ 55%) are drawn on top of the HUD bar
(`PlayerControls.kt:176`, `surfaceGlassElevated` @ 72%). Alpha compounds:
`1 − (0.28 × 0.45) ≈ **0.87** effective opacity`. That is why the HUD buttons render as opaque black
squares floating on a translucent bar — they break the material they sit in.

Apple never nests glass. A control inside a glass container gets **tint + border only, no fill**.

### 1.4 Only 2 of 7 optical ingredients are present

| Ingredient | V1 | V2 |
|---|---|---|
| Translucent fill | ✅ | ✅ |
| Lit edge / hairline | ✅ | ✅ |
| **Backdrop blur** | ✖ (dialogs only) | ✅ |
| **Vibrancy** (saturation boost on the blurred backdrop) | ✖ | ✅ |
| **Specular rim** (bright→faint gradient stroke that tracks light) | partial (static border) | ✅ |
| **Refraction / edge lensing** | ✖ | ✅ (API 33+) |
| Adaptive tint (responds to backdrop luminance) | ✖ | V3 |

---

## 2. Locked decisions

| Area | Decision | Rejected alternative (why) |
|---|---|---|
| **Root cause** | Ship an **ambient backdrop layer** under the whole shell (§3). Everything else is secondary. | More token/border tuning (cannot fix opaque-page maths) |
| **Library** | **`io.github.kyant0:backdrop:2.0.0`** (Kyant0/AndroidLiquidGlass) | Haze — excellent blur, but no `vibrancy`/`lens`; we'd hand-write AGSL for the exact effects Kyant ships tested. Hand-rolled AGSL — same work, no maintenance. |
| **Blur over video** | Switch `PlayerView` → **TextureView**, gated by a Settings toggle, **default ON** for API 31+ on non-low-RAM devices (§5) | `Popup` + `FLAG_BLUR_BEHIND` (AOSP: TV devices may disable window blurs *during video playback* — fails exactly where needed) |
| **Optics scope** | **blur + vibrancy + specular** are the shipped baseline. `lens()` refraction is wired but **behind a flag**, default OFF pending on-device review. | Full liquid refraction on by default (API 33+ only, heaviest, easy to overdo on a 10-foot UI) |
| **Nesting** | New `glassChild` treatment: tint + hairline, **never** a second fill (§6) | Lowering child alpha (still compounds; fragile) |
| **Tiles** | Each tile gets a **wash sampled from its own artwork** (§6.3), so tile glass has something to refract too. The tile logo well becomes glass over a scrim floor. | Blurring the logo bitmap per tile (40+ blurred bitmaps in a scrolling grid; mud at tile size) |
| **Selection** | Indicators become a **lens**, not paint: `glassLens()` replaces `accentGradientBrush()` at 6 of 7 sites (§6.2). The sliding segmented indicator is where `lens()` refraction ships first. | Keeping the opaque accent pill (it's the same "hole in the material" defect as the grey switch track, just in accent) |
| **Degradation** | Three capability tiers (§8). Below API 31 the V1 look ships unchanged. | Blocking the feature on old devices |
| **Layout / IA** | Unchanged, again. | Re-flowing screens |

---

## 3. The backdrop layer

**The single highest-value change in this spec.** One new composable behind everything in `AppShell`,
so that every already-glassified surface suddenly has content to refract — with **zero component
changes**.

```
AppShell
└── Box
    ├── AmbientBackdrop()            ← NEW: the thing glass refracts
    ├── rememberLayerBackdrop { … }  ← captures the above as the blur source
    └── Row { SidebarNav | Column { TopBar; content } }   ← unchanged, now .drawBackdrop(...)
```

`ui/theme/AmbientBackdrop.kt` (new). Composition, back to front:

1. **Base** — `bgBase` (unchanged; guarantees contrast floor).
2. **Artwork wash** — the focused item's poster, already loaded by Coil (`coil = 2.7.0` is a current
   dependency), downsampled hard and drawn at low alpha, heavily blurred. Cross-fades on focus change
   using `Motion.kt` `durSlowMs`; honors `LocalReducedMotion` (`Motion.kt:66`) by cutting the animation,
   not the layer.
3. **Accent mesh** — two or three wide radial gradients in the active `AccentPreset` ramp, drifting
   very slowly. This is what keeps the page alive on screens with no artwork (Settings, Search empty
   state, Onboarding).
4. **Vignette** — radial darkening to the edges so the sidebar/HUD still have a contrast anchor.

Constraints:

- Total added alpha over `bgBase` must keep every text token at **≥ 4.5:1** (§11).
- Drawn **once** at the shell level. Never per-screen, never per-card.
- Must be cheap: the artwork layer renders at ≤ ¼ resolution into a cached layer, recomposed only on
  focus change — not per frame.
- The drift animation is decorative: disabled under reduced-motion and on Tier C (§8).

> Ship this alone and the app reads dramatically more like glass before a single blur is applied.

---

## 4. The library

`io.github.kyant0:backdrop:2.0.0` — Compose Multiplatform Liquid Glass. Apache-2.0, ~3k stars,
`minSdk 21`, last pushed 2026-07-16, stable 2.0.0 released 2026-05-28.

It ships precisely the ingredient list from §1.4 as composable effects:

```kotlin
val backdrop = rememberLayerBackdrop {
    drawRect(colors.bgBase)
    drawContent()          // the AmbientBackdrop + page content
}

Modifier.drawBackdrop(
    backdrop = backdrop,
    shape = { RoundedCornerShape(radius.lg) },
    effects = {
        vibrancy()                       // saturation boost — the "alive" ingredient
        blur(20.dp.toPx())
        lens(16.dp.toPx(), 32.dp.toPx()) // refraction — API 33+, flag-gated (§2)
    },
    onDrawSurface = { drawRect(tint) },
)
```

| API level | Behaviour |
|---|---|
| **33+** | Everything, incl. `lens()` and `gammaAdjustment()` (`RuntimeShader`) |
| **31–32** | `blur()` + `vibrancy()` via `RenderEffect`; `lens()` silently no-ops |
| **≤ 30** | No backdrop effects — falls back to the V1 translucent fill, unchanged |

That fallback ladder maps 1:1 onto our tiers (§8) and onto `minSdk 23`.

**Adoption cost — resolved in Phase 0 (spike run, then reverted to a clean tree):**

`checkDebugAarMetadata` does fail on the current `compileSdk 36.1`:

```
Dependency 'io.github.kyant0:backdrop-android:2.0.0' requires libraries and applications
that depend on it to compile against version 37 or later of the Android APIs.
:tv is currently compiled against android-36.1.
```

Bumping `tv/build.gradle.kts:11` to `release(37) { minorApiLevel = 1 }` fixes it: Gradle
auto-installed SDK Platform 37.1, `:tv:assembleDebug` succeeded, and the APK installed and launched
on `emulator-5554` with **no crash and no runtime regression**. `targetSdk` stays at 36 and `minSdk`
stays at 23 — a `compileSdk` bump alone opts into no new runtime behaviour.

**Verdict: the library is viable. The Haze fallback is not needed.**

`glassSurface()` keeps its signature and becomes the single place that applies `drawBackdrop`, so the
~30 existing call sites and every `TvFocusable(backgroundBrush = …)` consumer inherit V2 for free.

---

## 5. Blur over video

Real blur over video requires the video pixels to be inside the Compose/View draw pass. That means
**TextureView**.

```
PlayerView(context).apply { … }        // defaults to SurfaceView — cannot be blurred
→ layout with app:surface_type="texture_view"   // media3 exposes this via XML only
```

media3 has no programmatic surface-type setter, so this needs a small layout XML inflated by the
existing `AndroidView` factories (there is currently **no** player XML in `tv/src/main/res/`).

**Trade-offs — the real cost of this phase:**

| | SurfaceView (today) | TextureView (V2) |
|---|---|---|
| Blur/clip/transform by Compose | ✖ | ✅ |
| GPU cost | lower (own layer) | extra texture copy, renders on the UI thread's GPU context |
| 4K / weak Amlogic + Fire TV Stick | safe | measurable risk |
| Widevine L1 DRM | required | breaks — **irrelevant here**: M3U/Xtream/Stalker are not DRM'd |

**Policy:** Settings toggle *"Enhanced glass over video"*, **default ON** when
`SDK_INT >= 31 && !ActivityManager.isLowRamDevice`, default OFF otherwise. The toggle flips the
surface type, so it needs a player rebuild — acceptable, the player already rebuilds on decoder
toggle (`LivePlayerScreen.kt:931-934` documents that path).

Keep `FLAG_BLUR_BEHIND` in `Dialog.kt` exactly as-is: it is a genuinely different mechanism
(compositor, cross-window) and it is the only thing that works for dialogs on Tier C.

---

## 6. Nesting rule

One rule, enforced everywhere: **glass never stacks.**

| Context | Treatment |
|---|---|
| A glass surface over the backdrop/video | `glassSurface()` — full material |
| A control **inside** a glass surface | `glassChild()` — hairline + tint, **no fill**, no shadow, **no second blur** |
| A *track* or *chip* inside glass (needs its shape to read) | `glassTrack()` — same rule, slightly more tint (~11% vs ~6%) |
| A chip carrying meaning (quality, catch-up) | tonal glass: its own hue @ ~20% + hue-400 hairline + lit edge |
| **A selection indicator** (sliding pill, selected chip/tab/nav row, switch ON) | `glassLens()` — accent @ ~30%, bright specular rim, soft accent glow outward (§6.2) |
| Focus on either | `TvFocusable` ring + glow, on top, unchanged |

> Stacking blur inside blur is the same compounding mistake as stacking fills — and far more
> expensive. A nested control is tint-only, always.

### 6.1 The small controls

These matter more than their size suggests: they repeat down every Settings list and across every
HUD, so one wrong recipe undoes the surface they sit on. Verified defect — the **OFF switch track**
renders as an opaque `surface3` slug punched through the glass panel.

| Control | File | Today | V2 |
|---|---|---|---|
| **Switch — OFF track** | `components/Switch.kt` | opaque `surface3` | `glassTrack()` |
| **Switch — ON track** | `Switch.kt:43` | opaque accent gradient | `glassLens()` (§6.2) |
| **Chip — unselected** | `components/Chip.kt:57` | `surfaceGlass` fill (compounds on glass) | tonal glass |
| **Chip — selected** | `Chip.kt:56, :70` | opaque accent gradient | `glassLens()` (§6.2) |
| **Segmented — sliding indicator** | `SegmentedControl.kt:101` | opaque accent gradient on a glass track | `glassLens()` — **the lead case** (§6.2) |
| **Tabs — selected** | `Tabs.kt:85` | flat solid `colors.accent` | `glassLens()` |
| **Sidebar — selected row** | `SidebarNav.kt:199, :239` | accent gradient | `glassLens()` |
| Primary button / active icon button | `Button.kt:95`, `IconButton.kt:71` | accent gradient | **unchanged — stays solid** (an *action*, not a selection; it must win attention) |
| **Badge — quality/neutral** | `components/Badge.kt:46` | `surfaceGlass` | `glassChild()` |
| Badge — **LIVE** | `Badge.kt` | solid red | **unchanged — stays solid** |
| **Seek track** | `PlayerControls.kt:275` | opaque `surface3` | `glassTrack()` |
| **HUD buttons** | `IconButton.kt:67` `Glass` variant | 55% on a 72% bar ⇒ ~87% | `glassChild()` |
| **Icon wells** | `SettingsScreen.kt:172`, `GuideScreen.kt:387` | `surface3` | `glassChild()` |
| **Logo well — player HUD** | `PlayerControls.kt:183`, `Color.kt:116` | dark plate, both themes | **unchanged — stays solid** (arbitrary video behind it) |
| **Logo well — tile** | `ChannelTile.kt:155` | dark plate, both themes | `glassChild()` over `logoWellScrim` (§6.3) |
| Text field | `TextField.kt:138` | `surfaceGlass` | `glassTrack()` when nested |

**Three deliberate exceptions.** `LIVE` keeps a solid red fill, the **player HUD's** logo well keeps
its dark plate in both themes, and the Primary button keeps its accent gradient. The first two carry
white content that must survive an arbitrary backdrop — arbitrary video, in the HUD's case; the third
is an *action*, not a state, and has to win attention on a 10-foot screen. Legibility and
call-to-action outrank material consistency — the same call V1 §9 already made.

The *tile's* logo well is a different situation and does become glass — see §6.3, where the backdrop
is derived from the logo itself and so can't clash with it.

### 6.2 The selection indicator is a lens, not paint

The one place Apple never paints a solid shape. Their segmented indicator is *clearer and brighter*
than the track it sits in — it separates by being **more glass**, not by being opaque. V1 does the
opposite: `accentGradientBrush()` lays a fully opaque accent pill on top of the glass track
(`SegmentedControl.kt:101`), which is exactly the "hole punched through the material" problem, just
in accent instead of grey.

`glassLens()` = accent @ ~30% (dark) / white @ ~70% over accent @ ~26% (light) · accent-300 hairline ·
strong `inset 0 1px` specular top · a soft outward accent glow so it still lifts off the track.
The label stays at full opacity and the semantics survive — a selected item is still unmistakably
accent-coloured, it just stopped being a paint chip.

> **This is where `lens()` refraction earns its keep.** The indicator is the only element in the app
> that *moves* continuously (`SegmentedControl.kt` springs it between segments). Refraction on a
> static surface is nearly invisible; refraction on a sliding one is the effect people actually
> recognise as liquid glass. If `lens()` ships anywhere on Tier A, it ships here first.

**The change is small.** Every site in the §6.1 table draws its selected state from the *same*
function — `accentGradientBrush()` (`Glass.kt:113`), used at 7 call sites. `glassLens()` is one new
sibling next to it plus a swap at 6 of those 7 (Primary button keeps the gradient). No component
restructuring.

**Contrast is the risk.** A ~30% accent fill has far less contrast against its label than a solid
accent gradient. Two mitigations, both required: the label keeps `accentFg`-weight type (never
dropped to `textSecondary`), and the lens sits on `glassTrack()`, never directly on the ambient
backdrop — the track's own tint provides the contrast floor. This must be measured in Phase 3, not
assumed; if a 4.5:1 label ratio doesn't hold at Tier C (no blur), the lens falls back to the V1
gradient on that tier only.

### 6.3 Tiles — every tile is its own backdrop

§3 gives the *page* something behind the glass. Tiles need the same treatment one level down: a
`ChannelTile` is a glass card whose interior is currently flat, with an **opaque `logoWell` plate**
(`ChannelTile.kt:155`) dropped in the middle of it. Same defect as the OFF switch and the selection
pill, third variation.

The fix costs nothing extra, because the material is already loaded: **the tile's own artwork is its
backdrop.**

| Layer (back → front) | Channel tile | Poster tile (VOD/series) |
|---|---|---|
| **Wash** *(new)* | two-stop gradient in the logo's dominant hue @ ~22% (§6.3.1) | the poster itself — already full-bleed, nothing new |
| **Surface** | `surfaceGlass` — unchanged, but now has something to refract | legibility scrim over the art — unchanged |
| **Logo well** | `glassChild()` **over a fixed dark scrim floor** | n/a |
| **Info panel** | `surfaceGlass` (`ChannelTile.kt:191`) — **no code change**, it just starts working | title strip → glass over the poster |
| **Progress rail** | — | `PosterTile.kt:177` `Black @ .45` → `glassTrack()` |

**The logo well keeps a luminance floor — this is not negotiable.** `Color.kt:112-115` already
records why it is pinned dark in *both* themes: provider logos are overwhelmingly white-on-transparent
PNGs, and a light well made them vanish. So the well becomes `glassChild()` **layered on a
`logoWellScrim` (ink @ ~.55)**, not pure glass. What changes is that it stops being a flat plate: the
scrim is translucent, the wash reads through it, and the well finally belongs to the tile instead of
sitting on it.

> The wash being derived from *the same logo* is what makes this safe. An arbitrary backdrop could
> clash with an arbitrary logo; a backdrop sampled from the logo cannot.

#### 6.3.1 The wash is a gradient, not a blurred bitmap

Extract **one dominant colour** from the ≤64px thumbnail Coil has already decoded, then draw a
two-stop `Brush.linearGradient` (dominant @ ~.22 → transparent) inside the tile shape.

- Extraction runs **once per logo URL**, memoized in an LRU, off the main thread. Never per frame,
  never per recomposition.
- No logo → hue derived deterministically from the channel name's hash, so initials-only tiles still
  get colour and the *same* channel always gets the *same* hue across scroll and sessions.
- **Rejected: blurring the logo bitmap as the wash.** A live grid holds 40+ tiles; forty blurred
  bitmaps in a scrolling grid is exactly the jank budget we can't spend, and at tile size a blurred
  logo is mud, not colour.

Because it's two gradient stops — no bitmap, no blur, no shader — this is the one item in the spec
that ships on **Tier C as well** (§8). Verify by scrolling the Live grid on `emulator-5554` watching
for dropped frames, and by checking a white-logo channel and a logo-less channel in **both** themes.

---

## 7. Tokens

Existing glass tokens (`Color.kt:127-131`, `:156-160`) stay. Because a real blurred backdrop now sits
behind them, the **fills get lighter** — V1's alphas were tuned to compensate for having nothing
behind them.

| Token | V1 dark | V2 dark | Note |
|---|---|---|---|
| `surfaceGlass` | white-ink @ .55 | **@ .38** | blur now carries legibility |
| `surfaceGlassElevated` | @ .72 | **@ .58** | |
| `glassHighlight` | white .22 | **.28** | stronger specular top stop |
| `borderGlass` | white .10 | unchanged | |
| `glassChildTint` *(new)* | white .06 | | nested controls (§6) |
| `glassTrackTint` *(new)* | white .11 | ink .09 (light) | nested tracks/chips that must hold a shape (§6.1) |
| `glassLensTint` *(new)* | accent .30 | white .70 → accent .26 (light) | selection indicators (§6.2) |
| `glassLensRim` *(new)* | accent-300 @ .60 | accent @ .42 (light) | the lens hairline |
| `tileWashAlpha` *(new)* | .22 | .14 (light) | strength of the per-tile artwork wash (§6.3.1) |
| `logoWellScrim` *(new)* | ink .55 | ink .55 — **same in both themes** | luminance floor under the now-translucent logo well |
| `backdropVeil` *(new)* | ink .55 | | contrast floor under the ambient layer (§3) |

Light-mode values mirror these (fills lighten, `borderGlass` stays the **dark** ink edge — V1 §9).
Final alphas are set by on-device contrast measurement in Phase 5, not by eye.

---

## 8. Capability tiers

Resolved once into a `LocalGlassTier` composition local; every glass call site reads the tier rather
than checking `SDK_INT` inline.

| Tier | Condition | What ships |
|---|---|---|
| **A** | API 33+, not low-RAM | Ambient backdrop + blur + vibrancy + specular (+ `lens` if the flag is on) |
| **B** | API 31–32, or low-RAM | Ambient backdrop + blur + vibrancy. No shaders, no drift animation. |
| **C** | API ≤ 30 | **Today's V1 look, plus the tile wash** (§6.3 — two gradient stops, no blur, no shader, so it costs nothing). Static ambient gradient only, no page artwork layer. |

Tier C is a hard requirement of `minSdk 23` and is the safety net for the whole spec: if any of this
regresses on cheap hardware, Tier C is the known-good state to fall back to.

---

## 9. Coverage gap

Surfaces still on opaque `surface1/2/3` that V1 never reached. These change nothing until §3 lands,
then they become visible immediately:

| Area | Sites |
|---|---|
| **Detail** | `DetailScreen.kt:225` poster well, `:415` `EpisodeRow` (opaque `surface1` card) |
| **Recordings** | `RecordingsScreen.kt:176` row, `:259` thumb well |
| **Streams** | `StreamsScreen.kt:188` row, `:197` logo well |
| **Multi-view** | `MultiViewScreen.kt:190`, `:280` panes/rows |
| **Onboarding** | `OnboardingSteps.kt:111, :126, :237, :277`; `PrivacyTermsScreen.kt:69` |
| **Sources / Language** | `SelectSourceScreen.kt` rows, `LanguageSelectScreen.kt` rows |
| **Components** | `Hero.kt:49` (opaque gradient), `OnScreenKeyboard.kt:31`, `NumericKeypad`, `StepIndicator.kt:53`, `RecordingIndicator.kt:65` (hardcoded `Ink950` @ .55, not a token) |
| **Player menus** | `SubtitleMenu.kt:540`, `VolumePicker.kt:84` (opaque `surfaceOverlay` focus fill) |
| **Misc** | `ParentalBlur.kt` — misnamed, applies **no blur** at all; V2 gives it a real one on Tier A/B |

Also: `FeedbackDialog.kt:272` hardcodes `Color.White` (QR holder) — intentional, leave it.

---

## 10. Light theme

The light theme is the harder case and gets equal weight, per the V1 rule that a white card with a
white edge on a near-white page vanishes.

- The ambient backdrop in light mode is a **tinted** wash, not a dark one — artwork at lower alpha
  over `bgBase` `#F3F5F9`, with the accent mesh doing more of the work.
- `borderGlass` stays the **dark** ink edge (`.14`) in light mode — non-negotiable.
- `vibrancy()` must be applied more conservatively in light mode; a saturation boost on an already
  bright backdrop pushes text contrast below AA fast.
- Provider logos keep the dark `logoWell` in both themes (`Color.kt:116-117`).
- Every surface re-measured for ≥ 4.5:1 in **both** themes before Phase 5 closes.

---

## 11. Accessibility & perf

- **Contrast is the gate, not the look.** Any surface that drops below 4.5:1 over its actual backdrop
  gets a scrim stop added under the text, or its fill alpha raised. Measured on-device, both themes.
- Reduced motion (`LocalReducedMotion`, `Motion.kt:66`) disables the ambient drift and artwork
  cross-fade — the layers remain, only the animation stops.
- Every focusable still composes `TvFocusable`. Focus is drawn **on top of** glass, never baked in.
- Perf budget: 60fps on the emulator and a real low-end box. Blur input downscaled (~0.5); the
  backdrop layer cached and invalidated on focus change only, never per frame.
- Regression watch: app startup (baseline profile exists), player start time, and memory on the
  artwork layer.

---

## 12. Rollout

| Phase | Work | Verify |
|---|---|---|
| **0 · Spike** ✅ | ~~Confirm the `compileSdk 37` question~~ — **done**: bump required, bump works, library viable (§4) | `:tv:assembleDebug` + install/launch, no crash |
| **1 · Backdrop** | `AmbientBackdrop.kt`; wire into `AppShell`; artwork palette from Coil; vignette | Screenshots Home/Settings/Movies, both themes — **this is the go/no-go for the whole spec** |
| **2 · Real glass** | Add the library; `glassSurface()` → `drawBackdrop`; tier resolution; token re-tune | Screenshots on the same screens; A/B against Phase 1 |
| **3 · Nesting** | `glassChild()` + `glassTrack()` + `glassLens()`; the full §6.1 small-control table — switch tracks, chips, badges, seek track, wells, HUD buttons, **and every selection indicator** (§6.2) | HUD + Settings screenshots, both themes; the OFF switch is the canary, the sliding segmented indicator is the payoff. **Measure the selected-label contrast ratio** before signing off |
| **3b · Tiles** | Dominant-colour extractor + LRU; tile wash on `ChannelTile`/`PosterTile`; tile logo well → glass over `logoWellScrim`; poster progress rail → `glassTrack()` (§6.3) | Scroll the Live grid watching for dropped frames; a white-logo channel and a logo-less channel, both themes |
| **4 · Video blur** | Player XML + TextureView; Settings toggle; low-RAM auto-off | On-device: HUD over live stream; playback stability; toggle both ways |
| **5 · Coverage** | The §9 list; light-theme contrast pass; 22-locale string sanity | `:tv:assembleDebug`; screenshots per screen, both themes |
| **6 · Polish** | Perf sweep, reduced-motion, a11y contrast sign-off, optional `lens()` review | Low-end device pass; baseline profile regen |

Build with JDK 21. `./gradlew :tv:compileDebugKotlin` to compile-check; `./gradlew :tv:assembleDebug`
for the APK + string validation.

**Expected new strings:** one — the "Enhanced glass over video" Settings toggle (label + description).
Must be added to `values/strings.xml` **and all 21 `values-*/strings.xml`**.

---

## 13. Open decisions

- [x] ~~**`compileSdk 37` bump**~~ — **resolved in Phase 0**: required, works, no regression. Library confirmed (§4).
- [ ] **`lens()` refraction** — ship on Tier A, or keep flag-off? Decide by looking at it on a TV, not in a spec.
- [ ] **Ambient artwork source** — focused item only, or the whole row's dominant palette? (Focused-only is cheaper and more responsive; row-level is calmer.)
- [ ] **Should the ambient backdrop be user-configurable** (off / subtle / full) next to the existing HUD rearrange editor?
- [ ] **Does `ParentalBlur` become a real blur** on Tier A/B, or stay a scrim for predictability?

---

## 14. File index

Verified paths, all under `tv/src/main/java/com/arashrahimi46/iptv/`:

- **New:** `ui/theme/AmbientBackdrop.kt` · `res/layout/player_view.xml`
- **Theme:** `ui/theme/Glass.kt` (`glassSurface`, `softShadow`, `glassBorderBrush`, `accentGradientBrush`;
  **new** `glassChild`, `LocalGlassTier`) · `Color.kt` (tokens §7) · `Focus.kt` (`TvFocusable:266`,
  `backgroundBrush:275`, `shadowElevation:278`, `borderBrush:284`) · `Motion.kt` (`LocalReducedMotion:66`)
- **Shell:** `ui/shell/AppShell.kt:49` (backdrop host) · `TopBar.kt`
- **Player:** `ui/player/LivePlayerScreen.kt:916` · `LiveMiniPlayer.kt:224` ·
  `ui/multiview/MultiViewScreen.kt:401` (all three surface sites) · `ui/components/PlayerControls.kt:176`
- **Dialogs:** `ui/components/Dialog.kt:91` (`FLAG_BLUR_BEHIND` — keep as-is)
- **Settings:** `ui/settings/SettingsPanes.kt` (new toggle) · `data/settings/UserSettings.kt` (persistence)
- **Coverage list:** see §9
