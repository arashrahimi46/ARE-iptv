# Glass render performance — findings and decisions

_Originally investigated 2026-07-27 on the TV **emulator**. **Rewritten 2026-07-28 against the real
Sony BRAVIA XL95** (`BRAVIA_4K_VH22`, MediaTek MT5895, 4× Cortex-A55, Android 12 / API 31, 60 Hz),
which contradicted the emulator on the single most important point. Read this before optimizing
anything about how this app renders._

## The one thing to take away

**Measure on the TV, not the emulator.** The emulator under-reported the app's dominant render cost
by about 4× and led to a locked decision that was wrong. The emulator's own numbers were also wildly
unstable: three runs of an *identical* build gave RenderThread means of 9.0, 67.0 and 8.4 ms. The TV
reproduces to ±1 ms.

Second: **this app is draw-bound, not recomposition-bound.** That part the emulator got right, and it
holds on hardware — composition + measure + layout is **0.07 ms** of a 34 ms frame. Several earlier
passes went after recomposition (unused params blocking skipping, unstable `Set`/`List` params,
memoizing brushes). Those changes are individually correct and worth keeping, but they were aimed at
0.07 ms. **They could not have fixed the reported jank, and neither can further work of that kind.**

## Real-hardware baseline (XL95, Settings D-pad scroll, release build, AOT-compiled)

p50 over 3 reps × 120 frames. Budget is 16.7 ms.

| phase | XL95 | emulator (same commit) |
|---|---|---|
| input → handle | 0.4 ms | — |
| composition + measure + layout | **0.07 ms** | 0.04 ms |
| draw recording | 1.0 ms | 0.92 ms |
| **sync / upload** | **~4 ms** | _never measured_ |
| **RenderThread (issue → swap)** | **14.5 ms** | 5.25 ms |
| GPU | 5.9 ms | ~13 ms (meaningless, see below) |
| **TOTAL** | **~34 ms** | 24.9 ms debug / 17.0 release |

116–119 of every 120 frames missed the deadline. That is ~29 fps, and it is what the "flaky and
laggy animation" report on the XL95 actually was.

**Input latency is a symptom, not a cause.** `gfxinfo` counts 236–280 "high input latency" events per
sweep, and an earlier version of this document named that the most promising open lead. It is not:
the app begins handling a keypress in **0.4 ms**. Those events are frames queueing behind 34 ms
draws, and they fell by half when draw time fell. Do not optimize input handling for this.

## What the backdrop actually costs

The emulator estimated the whole glass backdrop system at ~4 ms of RenderThread and, on that basis,
this document **locked the decision to keep it** — "the win isn't big enough to pay for it."

On the XL95 it was worth **14.7 ms of a 16.7 ms frame**. Forcing `GlassTier.C` (backdrop off
app-wide) as an experiment:

| p50 | backdrop on (Tier B) | backdrop off (Tier C) |
|---|---|---|
| RenderThread | 14.6 ms | 5.0 ms |
| sync / upload | ~5.7 ms | 0.4 ms |
| GPU | 5.9 ms | ~5.0 ms — **unchanged** |
| TOTAL | ~32.6 ms | ~17.9 ms |
| high input latency | ~289 | ~138 |

GPU time does not move. **This is CPU work on the RenderThread**, which is exactly the axis a
Cortex-A55 is worst at and exactly what a translation-layer emulator on an M-series host hides.

### The fix, and the flaw in the original reasoning

The old decision rested on a test that disabled two things at once. Turning the backdrop system off
changes `Theme.kt`'s `withBlurredBackdrop()` **token retune** *and* removes the per-surface
**sampling**. The observed visual change came from the retune. They are separable.

`glassSurface` already applied **no effects on Tier B** — the per-surface blur was removed in an
earlier pass and `vibrancy()` is Tier A only. So every glass surface on the XL95 was paying for a
full offscreen backdrop capture with an empty effect list. Gating the sample on `tier.hasShaders`
(is there an effect to apply?) rather than `tier.hasBackdropBlur` (could this device blur?):

| p50, 3 reps each | before | after |
|---|---|---|
| RenderThread | 14.5 ms | **5.0 ms** |
| sync / upload | ~4 ms | **0.4 ms** |
| TOTAL frame | ~34 ms | **~18 ms** |
| frames over 16.7 ms | 117 / 119 / 116 of 120 | **89 / 69 / 55** |
| janky | ~17 % | **~7.5 %** |

**~29 → ~55 fps, with the look kept.** Settings is pixel-identical; Home shows no card-shaped
difference at 8× amplification. The only systematic diff is 1 px of antialiasing on shape edges.

Why it is safe: `AmbientBackdrop` is a **real layer in the hierarchy**, so a translucent fill over it
already composites correctly through ordinary alpha blending. Sampling only buys the ability to run
an *effect* on what is behind; with no effect it redraws the pixels that would have shown through
anyway. Tier A keeps the sample (it has `vibrancy()`), and `frostedPanel`'s single genuine blur — the
expanded sidebar, the one blur in the app worth its cost — is untouched.

## The sidebar expand/collapse animation (measured on the XL95, on Home)

Measured on **Home**, breaking this document's own "never measure on Home" rule — deliberately, because
that is the screen the report was about, and the numbers reproduced to ~2 ms across reps. Compare only
Home-to-Home.

The cost was attributed by elimination. Two of the three suspects this document named were innocent:

| suspect | cost |
|---|---|
| unbaked Gaussian `softShadow` (resizes every frame) | **~0 ms** |
| `frostedPanel`'s 28 dp blur | **~1 ms** |
| the page capture — `layerBackdrop`'s double draw | **~14 ms** |

`layerBackdrop` re-records its subtree on **every frame the consumer draws**, and the panel is
remeasured and redrawn on every frame of the width tween — so the whole page was re-photographed 13
times per expand.

First attempt (superseded): gate the capture in **time** — no capture while the width moves, capture
once the tween settles. It hit the numbers below, but the blur then visibly **popped in** a beat after
the motion stopped, which is worse than the jank it fixed. Do not reach for this.

Actual fix: stop the frosted node from resizing at all. `frostedPanel` is now **fill only** and is
pinned at the panel's OPEN width inside its own `graphicsLayer`, so an animation frame never
invalidates its draw; an outer wrapper carries the tween width, the rounded clip, the shadow and the
edge, which are all cheap draw ops. The page is captured once per expand, and the frost is present
from frame 0. Same pixels — the page behind does not move during the tween, so the blur is the same
image either way; only how much of it is revealed changes.

One gotcha: the edge must be stroked in `drawWithContent` after `drawContent()`, not with `.border()`.
Modifier draws land *beneath* a node's children, so a border on the wrapper would sit under the frost.

| p50, 3 reps | before | after |
|---|---|---|
| RenderThread | 13.5 ms | **4.8 ms** |
| sync / upload | 5.5–11.0 ms | **0.45 ms** |
| TOTAL frame | 33–37 ms | **~20 ms** |
| janky | ~18.5 % | **~14 %** |

**Do not "capture once and freeze" by detaching the modifier.** `layerBackdrop` discards its recording
when detached, so that silently turns the frost off entirely — and it lands on the no-frost ceiling
number *exactly*, so it reads as a perfect win in framestats while being a visual regression. This was
built, measured, and nearly shipped before a screenshot caught it. Verify frost with a screenshot, or
with pixel variance just inside the panel edge: frosted ≈ 23–25, unfrosted ≈ 38.

## Home scroll: four hypotheses measured and REJECTED

Profiled 2026-07-28 on the XL95, 6x DPAD_DOWN + 6x DPAD_UP, 3 reps. Read this before optimizing Home
-- these are cheap-looking wins that are not wins, and each one cost a build/install/measure cycle.

Baseline (Home): TOTAL p50 20.5ms, p90 **37.5ms**, max 57-98ms; draw recording p50 1.2ms but max
11-16ms; RenderThread p50 5.4-6.2ms; GPU p50 7.4-8.4ms; janky 13-16.7%. Composition is **0.08ms**.

| hypothesis | result |
|---|---|
| Gate the ambient backdrop capture on Tier A (it has **zero** consumers on Tier B after the sampling fix, so `layerBackdrop` was capturing for nobody) | **no change** -- GPU p50 7.4→7.6 |
| `window.setBackgroundDrawable(null)` -- the decor view paints a full-screen opaque fill under a screen that already paints its own opaque `bgBase` | **no change** -- GPU p50 7.4→7.6 |
| Drop the ambient artwork's full-screen `blur(72.dp)` (a RenderEffect over 1920x1080, re-run whenever focus republishes the artwork) | **no change** -- GPU p50 7.4→7.1 |
| Disable `softShadow` app-wide (it bakes **per node** via `drawWithCache`, so every card scrolling into view rasterizes its own Gaussian mask) | **no change** -- draw recording max stayed 11-16ms |

**`debug.hwui.overdraw show` is misleading here.** It reports dark red (4x+) across the entire Home
frame including empty regions, which looks like a smoking gun. Two separate attempts to remove a
full-screen layer moved nothing. Overdraw depth is not the cost on this device -- do not optimize
against that map without an A/B.

### What the numbers actually say

**p90 is a fixed artefact of the input pattern, not a tail.** It measured 37.3 / 37.6 / 37.6 / 37.6 /
37.5 / 37.5 / 37.5 / 37.4ms across *eight* different builds -- too constant to be noise. 12 keypresses
x one expensive frame each = 12 of 120 frames = exactly the p90 boundary. p90 **is** the first frame
after each keypress. Report p50 and p90 separately; they are two different phenomena, and averaging
them hides both.

**The spike is Home-specific.** The identical sweep on the Movies grid:

| p50 / p90 / max | Home | Movies |
|---|---|---|
| TOTAL | 20.5 / **37.5** / 57-98ms | 19.4 / **19.8-22.1** / 25-27ms |
| draw recording max | **11-16ms** | **4.2-4.6ms** |
| GPU p50 | 7.4-8.4ms | 4.7-5.8ms |
| janky | 13-16.7% | 7.7-9.8% |

So the cost is in bringing a whole new **heterogeneous rail** into view (Home mixes 2:3 poster tiles
with 16:9 live tiles; Movies is a uniform grid that stays warm), and it lands in **draw recording on
the UI thread**, not in GPU or composition. It is not live video -- the "Live now" tiles are logos
with a badge, there is no player on Home.

**The residual p50 overshoot is app-wide, not a Home bug.** Movies sits at 19.4ms against a 16.7ms
budget too. Whatever the last ~3ms is, it is common to every browse screen.

### Next step, and what NOT to do

Stop A/B-ing candidate features -- four in a row returned noise, which means the cost is not in any
single removable layer. The spike frames need per-frame attribution from a **Perfetto trace on the TV**
(`atrace` categories `gfx,view,res`), which will name the slow call instead of inviting another guess.

## Home scroll p90: the Perfetto trace, and what it actually names (2026-07-28)

Captured on the XL95 with `adb shell perfetto -o <file> -t 20s -b 32mb -a com.arashrahimi46.iptv gfx
view res`, then 3x (6x DPAD_DOWN + 6x DPAD_UP) while tracing, pulled and queried with
`trace_processor_shell` (grouping slices by `thread.tid` -- 29648 was the main/UI thread, 29677
`RenderThread`, confirmed via `adb shell ps -T -p <pid>`).

**The cost is not in draw, GPU, or the View traversal's own measure/layout -- it is Compose
recomposing brand-new `LazyRow` items, and it happens in the Choreographer `ANIMATION` callback,
*before* `PerformTraversalsStart` even fires.** That is why this doc's own framestats-based
"composition + measure + layout = `DrawStart - PerformTraversalsStart`" reads as 0.07-0.08 ms on every
prior measurement in this document: Compose's `Recomposer` schedules its per-frame work via
`Choreographer.postFrameCallback` (the `ANIMATION` callback type), which Android runs *before*
`TRAVERSAL`. `PerformTraversalsStart` was never the right start marker for Compose's own
measure/layout cost -- it only bounds the View system's (now near-instant) pass over output Compose
already produced. `framestats` has an `ANIMATION_START` field that this document never used.

One 66.6 ms frame, broken down by trace slice (`Choreographer#doFrame` -> `animation` (59.4 ms) ->
`Recomposer:animation` (55.4 ms) -> `AndroidOwner:measureAndLayout` (53.7 ms), all *before*
`traversal` (7.2 ms) even starts):

| repeated ~4x in this one frame (one per newly-visible tile) | cost |
|---|---|
| `TextAnnotatedStringNode:measure` (x3, one per `Text`/`AreBadge` on the tile) | ~1.0-1.5 ms each |
| `Compose:recompose` | ~2.5-4.0 ms |
| `Compose:applyChanges` | ~2.0-3.6 ms |
| `rememberAsyncImagePainter` + `AsyncImagePainter.onRemembered` | ~1.0 + ~1.0 ms |

`computePalette` -- initially suspicious, since it's a real per-tile call -- was ruled out by the same
trace: 48 calls total across the whole 20 s capture, 0.79 ms summed. Not the cost.

This matches the doc's "whole new heterogeneous rail" framing exactly, just one level more precise:
the *rail* is the outer `LazyColumn` in `HomeScreen.kt` (each item is one `AreRail`/`LazyRow`), not a
single mixed-shape row -- individual rails are shape-homogeneous (all-poster or all-channel). A new
rail scrolling in means Compose composes ~4 previously-never-composed tiles at once, each paying
several text measures plus an async image painter setup, synchronously inside the animation callback.

**Fix landed, verified by trace-to-trace diffing (not framestats).** The outer `LazyColumn`'s
`itemsIndexed(visibleSections, key = ...)` had no `contentType`, so poster/channel/category-card rails
all shared the default `null` type and Compose's slot-reuse pool couldn't tell them apart -- a textbook
match for "new shape scrolling in costs more than it should." Added a `contentType` lambda
(`homeSectionContentType`, keyed off `HomeSection`/`BuiltinSection`).

The first attempt to verify this via `framestats` percentiles (see paragraph below, kept for the
record) was noise -- aggregate percentiles are sensitive to total-frame-count and view-hierarchy state,
which drifted rep to rep under a scripted `adb shell input keyevent` sweep. The fix: stop comparing
aggregates and instead **diff the same named slice across two Perfetto traces**, since
`trace_processor_shell` was already working tooling. Protocol: force-stop -> fresh launch -> settle ->
`perfetto -o <file> -t 20s -a <pkg> gfx view res` -> identical scripted sweep -> pull -> sum
`AndroidOwner:measureAndLayout` (the exact slice this doc's own trace pinned as the cost) on the main
thread across the whole capture, 2 reps per build:

| | before (2 reps) | after (2 reps) |
|---|---|---|
| `AndroidOwner:measureAndLayout` total | 748.4 / 715.2 ms | 658.8 / 650.9 ms |
| `animation` (Recomposer) callback count | 582 / 595 | 728 / 741 |
| **cost per callback** | **1.24 ms avg** | **0.89 ms avg** |

Both reps per build cluster tightly (before: 748/715; after: 659/651) -- far less noise than the
percentile method -- and the per-callback normalization (which controls for "after" fitting more
callbacks into the same 20 s window) shows a consistent **~28% drop** in average recompose+measure+
layout cost per animation callback, in both reps. Screenshot-verified pixel-identical (expected: this
is a slot-reuse metadata hint, not a visual change). Landed.

(For the record, the abandoned first verification attempt: repeated (reset -> 3x sweep -> framestats)
runs on the *same unchanged build* swung from p50 19 ms/p90 20 ms/janky-legacy 2.7% to p50 19 ms/p90
34 ms/janky-legacy 33%, and one rep's dump showed 94 attached Views where every other rep showed 9-10 --
too noisy to read a verdict from. Trace-to-trace diffing of a specific named slice, not aggregate
`framestats` percentiles, is the correct tool for small deltas on this device.)

## Earlier draw-time fixes (emulator-era, all still valid)

Work that was being redone every frame to produce an image that never changed:

| change | what it was doing | commit |
|---|---|---|
| Ambient mesh baked to a bitmap | 2 viewport-sized radial gradients + veil + vignette per frame, drawn **twice** because `layerBackdrop` captures the same subtree | `fa8a826` |
| `softShadow` baked to a bitmap | `drawPath` regenerating a Gaussian mask every frame; ~1.7 ms of RenderThread alone | `fa8a826` |
| `tvGlow` moved to `drawWithCache` | allocating a `Path`, `NativePaint` and `BlurMaskFilter` on **every draw pass** | `cb1235a` |

Both bakes render at half resolution and upscale — lossless in practice because the outputs are
band-limited by construction. Pixel diff: 1.3 % of pixels differ, all by at most 2/255.

Two traps worth remembering, because both would have made things **worse**:

- `drawWithCache` re-runs when the modifier is recreated. Baking the mesh inline in `AmbientBackdrop`
  meant a fresh multi-MB allocation on every artwork change — i.e. every D-pad step on a browse
  screen. It lives in its own skippable composable (`AmbientMesh`, stable `Color` params only).
- `drawWithCache` also re-runs on **size** change. The expanding sidebar is remeasured every frame by
  `widthFrom`, so baking its shadow would allocate and rasterize per frame. `softShadow` takes a
  `bake: Boolean` opt-out; `frostedPanel` and `glassSurface(sheer = true)` pass `false`. **Any future
  size-animating glass surface must do the same.**

## How to measure — the real device

`adb` is not on PATH; use `~/Library/Android/sdk/platform-tools/adb`. The shell is zsh, which does
not word-split unquoted vars, so use `export ANDROID_SERIAL=…` rather than `-s $VAR`.

```bash
# TV: Settings > System > Developer options > Network debugging. Accept the on-screen prompt.
adb connect <tv-ip>:5555
export ANDROID_SERIAL=<tv-ip>:5555

adb install -r tv/build/outputs/apk/release/tv-release.apk
# A sideload leaves ART at status=verify -- the baseline profile is NOT applied and every number is
# of an uncompiled app. Run the app once first so a profile exists, then:
adb shell killall -s SIGUSR1 com.arashrahimi46.iptv     # flush the profile
adb shell cmd package compile -m speed-profile -f com.arashrahimi46.iptv
adb shell dumpsys package dexopt | grep -A2 'com.arashrahimi46.iptv]'   # want status=speed-profile

adb shell dumpsys gfxinfo com.arashrahimi46.iptv reset
# ...perform the interaction (14x KEYCODE_DPAD_DOWN, ~450ms apart)...
adb shell dumpsys gfxinfo com.arashrahimi46.iptv framestats
```

### Parse `framestats` by COLUMN NAME, never by index

An earlier version of this document hardcoded column indices taken from the emulator (API 36). **The
XL95 (API 31) emits a different layout** — `SyncQueued`, `IssueDrawCommandsStart` and
`FrameCompleted` all sit one index earlier. Fixed indices do not error, they silently produce
nonsense (they read a *duration* field as a timestamp and every row gets filtered out). `framestats`
emits a header row inside `---PROFILEDATA---`; zip against it.

Phases, by name:

- composition + measure + layout = `DrawStart - PerformTraversalsStart`
- draw recording = `SyncQueued - DrawStart`
- sync / upload = `IssueDrawCommandsStart - SyncQueued`
- **RenderThread = `SwapBuffers - IssueDrawCommandsStart`** ← the number that matters
- GPU = `GpuCompleted - SwapBuffers`
- total = `FrameCompleted - IntendedVsync`

Skip rows where `Flags != 0` — HWUI is telling you it is not a valid timing sample.

### Traps in the measurement itself

Each of these produced a confidently wrong answer during this investigation:

- **`PROFILEDATA` is a 120-frame ring buffer.** A sweep longer than ~2 s overflows it and you end up
  reading idle frames. The `Stats since:` aggregate block covers every frame since `reset` — trust
  that for jank rates and percentiles.
- **Never measure on Home.** Its "Live now" rail plays real network streams; decode and network
  jitter move p50 by tens of ms and make a rendering delta unmeasurable. Settings has no video.
- **Wait for genuine idle before `reset`,** and require the app to have actually started drawing. An
  app that has not rendered yet reports 0 frames, which naive idle-detection reads as "settled" — so
  the whole sweep gets typed into a window that is not up.
- **`Total frames rendered: 0` usually means your keypresses did nothing,** not that the instrument
  is broken. Focus parked on the last sidebar row swallows every DPAD_DOWN. Screenshot the state.
- **Blind scripted navigation drifts.** A capture sequence that "goes to Home" landed in Multi-view
  and produced a 100 %-different pixel diff that meant nothing. Screenshot every state you compare;
  prefer a fresh launch (start screen = Home) over a key sequence.
- **The emulator cannot answer absolute questions.** It runs OpenGL ES → Metal on the host
  (`ro.hardware.egl = emulation`), so `GpuCompleted` is host round-trip latency, not GPU work.
- **Debug builds carry non-trivial overhead** — profile release.

## Home cold-vs-warm: three more hypotheses measured and REJECTED (2026-07-28)

The user's own observation — *"the first attempt is so slow, but the follow-up scrolling is much
faster"* — is real and reproducible. Protocol changed to match it: a **complex mixed** sequence
(horizontal runs inside rails, verticals between rails, backtracking: `RIGHT×3 DOWN RIGHT×2 DOWN
LEFT×2 DOWN RIGHT×3 UP×2 LEFT`), captured in three separate `gfxinfo reset` windows — **cold** (first
pass), **warm** (identical moves again), **deep** (further down into rails never reached). A uniform
up/down sweep hides this entirely, because it only ever re-enters warm tiles.

Sums over 120 frames, because *sums and p50 are stable here and maxima are not* (see the caveat below):

| build | cold total | warm total | cold RT | warm RT | cold compose |
|---|---|---|---|---|---|
| current | 4393 | 2902 | 1447 | 788 | 445 |
| hardware bitmaps (`allowRgb565` dropped) | 4324 | 2999 | 1392 | 860 | 435 |
| **no tile images at all** | 4164 | 2837 | 1348 | 829 | 421 |
| no ambient artwork blur | 4628 | 2571 | 1576 | 643 | 436 |

**Cold costs ~50% more frame time than warm, in every build (~4400 vs ~2900), and nothing removed
changes it.**

| hypothesis | result |
|---|---|
| First-time image **decode + GPU upload** — the 137ms `sync`/RenderThread spikes looked exactly like texture upload | **REJECTED.** Disabling tile artwork *entirely* moved cold total 4393→4164 (5%) and left the spikes (sync max 137.7→130.7). Images are not the cold cost. |
| `allowRgb565(true)` forces **software** bitmaps, so every image pays a CPU→GPU upload; hardware bitmaps would skip it | **REJECTED, and the premise was wrong.** Dropping it changed nothing (cold sync max 137.7→139.0) and `dumpsys meminfo` still reported **Graphics: 0 KB**, i.e. hardware bitmaps never engaged. Keep `allowRgb565` — it is a free memory win. Note `TileWash.kt:74` already bails on `Config.HARDWARE`, so anyone re-testing this must check the tile wash still renders. |
| The ambient **artwork wash** (full-screen 72dp `RenderEffect`) is the RenderThread stall | **REJECTED for cold** (4393→4628, i.e. worse), but see the real finding below. |

### The one thing that did move, and it is on the WARM path

Disabling the artwork wash improved the **warm** pass substantially and consistently:
total p90 **37.5→26.2ms**, RenderThread p90 **13.3→8.9ms**, RenderThread sum **788→643**. That is
steady-state browsing, i.e. most of the time the user actually spends. It is a visual change (the
page loses the focused item's colour wash), so it is a product decision, not a free win.

Separately, on the simple up/down protocol, disabling artwork **and** mesh together was worth
total p50 **22.76→19.81ms** (~3ms/frame, ~13%) — the whole "background lighting" system is a modest
steady cost, not the lag.

### Measurement caveat that matters more than any of the above

**Single-rep maxima are noise.** The cold-pass `max` across these four builds read 194 / 188 / 167 /
**326** ms, and the 326ms belongs to the build that was otherwise *best* on the warm path. Do not
attribute a cause from one giant frame in one rep — this document has already been burned once by
reading percentiles off single runs. Sums and p50 reproduced fine; maxima did not.

### Three more rejected (same day, same protocol)

| hypothesis | result |
|---|---|
| First-use **shader / pipeline compilation** on the RenderThread. Fitted every observation, and a baseline profile does not cover shaders. Probed with a `ShaderWarmup` composable on the splash drawing one tiny instance of each recipe (baked shadow, blurred glow, focus ring, sheen, glass fill, gradient hairline, lens, `RenderEffect` blur) at `alpha = 0.02f` so it genuinely rasterizes rather than being culled | **REJECTED.** Cold 4393→4264, inside the 4164–4628 spread of unrelated builds; the ~140ms spike untouched (137.7→139.7). Warm unchanged (2902→2907). The warm-up was reverted rather than left in as dead code. Caveat: this warms the recipes named above, so it does not *disprove* shader compilation for recipes it missed (text, image shaders, `drawBackdrop`) — but it does kill the cheap version of the idea. |
| The cold pass is **background startup work** (Room, paging, coroutines) stealing CPU from the RenderThread on a 4-core A55, not rendering at all | **REJECTED.** Idling 90s instead of 25s before the cold window changed nothing: 4264→4360, spike 139.7→140.2. |
| **`softShadow`'s per-tile bake.** Each newly-visible tile allocates and rasterizes its own ~1MB Gaussian `ImageBitmap` in `drawWithCache` — first-visit only, content-independent, RT-bound | **Not the cause, but the biggest single win found.** Cold 4393→4002 (−9%), p50 25.6→21.2, p90 70.3→59.2, RT sum 1447→1288. The spike survives (137.7→135.6). Note this contradicts the earlier "disable softShadow → no change" entry above, which used the *uniform* sweep; that protocol never exercises first-visit cost. |

### What is left, honestly

Six hypotheses tested, none of them the cause. What survives all six:

- The cold/warm gap is **robust**: ~4300 vs ~2900 over 120 frames across every build, ~12ms/frame.
- A **~140ms single frame** appears on every cold pass in every variant (135–144ms across seven
  builds). Immune to removing images, bitmap config, the ambient blur, shaders, idle time and
  `softShadow`. That consistency means it is structural, not content — and one frame, so it is a
  small share of the 1400ms gap.
- Compose is a flat ~3ms/frame tax everywhere (cold sums 421–461). Not the cold penalty.

The two real (if modest) wins found are both **visual trade-offs**, not free: dropping `softShadow`
(−9% cold) and dropping the ambient artwork wash (warm p90 −30%). Neither has been taken.

Next probe should stop guessing at removals and get **per-frame attribution** for the cold pass
specifically — a Perfetto trace over the mixed cold sequence, which is what finally named the Home
p90 spike earlier. Removal A/Bs have now returned six nulls; that is the signal to change instrument,
exactly as this document concluded once before.

Compose, for the record, is a flat ~3ms/frame tax in every single variant (cold sums 421–445, warm
406–443). It is not the cold penalty and it is not where the next win is.

## MEASURED: the sidebar open/close on Home, XL95 (2026-07-28)

The section below this one describes the composition-side fixes. This is what they were worth on the
device, plus the one that backfired. **Protocol:** release build, `speed-profile` AOT-compiled,
force-stop → launch → 22s settle → `gfxinfo reset` → 8 × (DPAD_LEFT, 1s, DPAD_RIGHT, 1s) →
`framestats`. 2 reps per build, ~120 valid frames each, `Flags != 0` dropped. Reps cluster tightly.

**Use `AnimationStart` as the start marker.** This document spent its whole life computing
"composition + measure + layout" as `DrawStart - PerformTraversalsStart` and reading 0.07ms, then
correctly worked out (via Perfetto) that Compose runs in the Choreographer ANIMATION callback, before
traversal. `framestats` has had an `AnimationStart` column all along. **`PerformTraversalsStart -
AnimationStart` is Compose's own recompose+measure+layout, straight out of `dumpsys`** — no Perfetto,
no `trace_processor_shell`. Parse by column name; API 31 emits a different order than API 36.

| p50 unless noted | before | frost ungated | gated | **gated + cross-fade** |
|---|---|---|---|---|
| compose (`AnimationStart`→`PerformTraversals`) | 1.70 | 1.41 | 1.49 | **1.17** |
| compose, total over run | 724ms | 530ms | 517ms | **411ms** |
| draw recording | 3.34 | 4.74 | 3.41 | **0.75** |
| sync / upload | 2.10 | 9.39 | 2.47 | **0.43** |
| RenderThread | 7.70 | 15.36 | 10.78 | **5.44** |
| **TOTAL frame** | **36.78** | 40.04 | 36.42 | **20.13** |
| TOTAL p90 | 58.85 | 54.09 | 47.70 | **40.76** |
| frames over 16.7ms | 98% | 98% | 98% | **95%** |

**~27 fps → ~50 fps on the interaction the user reported as laggy.**

Three things this run settles:

1. **Removing the frost time-gate is a regression, not a fix.** Commit `3304c50` pinned
   `frostedPanel` at the open width and its comment declared the gate gone — but the gate was still in
   the file, so the claim was never tested. Tested: ungating costs +7ms sync and +5ms RenderThread on
   every frame of the tween. Pinning stops the frosted *node* being invalidated; the page underneath
   is still re-recorded. **Necessary but not sufficient.**
2. **The late blur is fixed at the consumer, not by ungating.** Cross-dissolving the frost in over the
   plain sheer fill turns the pop into a settle. Both alphas are `graphicsLayer` properties, so the
   fade composites an already-captured layer — and it is *not* merely free, it took total p50 from
   36.42 to 20.13ms, because the frosted node now only exists while it is actually frosted rather than
   sitting in the tween's layer tree the whole time. Animating the blur *radius* instead would re-run
   the effect and re-pull the page every fade frame; don't.
3. **Clipping the rows to the panel edge is free.** RenderThread 10.78 vs 10.76 with it removed. Keep
   it; it fixes the ring-leads-the-panel artefact at no cost.

**Verify the frost every time you touch this.** A change that silently kills it lands *exactly* on the
no-frost ceiling and reads as a perfect win. Cheap check, no eyeballing: screenshot the same region
collapsed vs expanded and compare high-frequency detail. Frosted here measured mean|horizontal
gradient| **3.39 → 0.60** and stdev **75.7 → 29.0**.

**Still open:** 95% of frames remain over 16.7ms and p50 is 20.13 against a 16.7 budget. The sidebar
is no longer the dominant cost; whatever is left is shared with the rest of the app.

## The sidebar, revisited (2026-07-28) — and a trap in this document's own record

The expand/collapse was re-read after the frost fix landed, on the theory that the remaining "not
buttery" feel was composition-side. It was, and none of it is visible to `framestats` for the reason
the Home section above establishes.

**The gate that was documented as removed but never was.** Commit `3304c50` pinned `frostedPanel` at
the open width and added a comment to `AppShell.kt` reading "NOT gated in time … the frost is there
from frame 0". It only added the comment. The `capturePage` `LaunchedEffect(delay(durBaseMs + 32))`
stayed in the file directly underneath, so the blur went on popping in a beat after the motion
stopped — the exact defect that commit was written to remove — for as long as both paragraphs sat
there contradicting each other. **A comment is not a diff.** When this document says something was
fixed, check the code.

**`staticCompositionLocalOf` for a value that changes.** `LocalPageBackdrop` was static. Static
locals are not tracked per-reader, so changing one recomposes the provider's entire content subtree
with **no skipping** — and this is the only local in the app whose value changes at runtime: it flips
on every expand and again on every collapse. Every rail and every tile in the app was recomposing on
frame 0 of the width tween. `compositionLocalOf` reaches the one node that reads it (`frostedPanel`).
`LocalAppBackdrop` and `LocalAmbientArtwork` are correctly static — their *values* never change (the
artwork one provides a stable `MutableState`), which is the condition that makes static right.

**`expanded` was a direct read of `focusedItemId`.** That id changes on every D-pad step *inside* the
rail, so each step recomposed the nav, the brand mark, the selection lens and all ten rows — to
recompute a `Boolean` that had not changed. `derivedStateOf` makes it twice per visit instead of once
per step. Worth checking anywhere a coarse flag is derived from a fine-grained state.

**The focus ring led the panel.** The nav content is pinned at the open width so an animation frame
never remeasures a row — but that also meant a focused row's ring, fill and label were drawn at full
width from frame 0 while the panel behind them was still collapsed. For the ~13 frames of an expand
the ring floated *outside* the panel, over the page content. Fixed with a draw-time `clipRect` at the
animating width (`clipToPanel`), which wipes the rows in with the advancing edge and adds no layout
work. **Do not fix this by making the rows track the panel width at layout time** — that reintroduces
a per-frame remeasure of ten rows, each of which re-measures its label, which is the expensive thing
the pinning exists to avoid.

### The same composition-scope bug, found in three more places

`animateFloatAsState` declared with `by` and then *used in composition scope* turns a draw-time
property into a per-frame recomposition of everything around it. `Focus.kt` and `SidebarNav.kt` model
the fix (keep the `State`, read it inside the `graphicsLayer`/`offset`/`layout` lambda). Three sites
did not:

| site | what recomposed per frame | when it fired |
|---|---|---|
| `LiveMiniPlayer` — three tweens resolved to `Float` before the lambdas | `AndroidView`/PlayerView factory block, badge, title | its FADE tween runs on *losing* focus, i.e. while browsing Home |
| `AreSegmentedControl` — `animW` in an `if` guard and `Modifier.width(Dp)` | track, lens, and the whole `forEachIndexed` (N × `TvFocusable` + `Text`) | every tab switch, four call sites, long spring tail |
| `RecordingIndicator` — an **infinite** transition read in composition | the row, both labels, their `stringResource`s, `formatRecElapsed` | continuously, for as long as a recording runs |

The segmented control's guard is the subtle one: `if (animW > 0f)` looks like a null check but is a
composition read of an animating value. `if (target != null)` asks the same "is geometry known yet"
question against the *measured* bounds, which only change on layout.

## Eager lists: three screens that composed everything at once (2026-07-28)

The Home fix (`contentType`, `a565ac6`) made a *virtualized* list reuse slots better. These three were
not virtualized at all along the axis that mattered.

**The Guide's programme lanes.** The rows went lazy in P0.2, but each visible row then composed EVERY
cell of the whole ~6h window regardless of horizontal scroll position — 12-20 cells × 2-3 `Text`s
each × ~6 visible rows, so a few hundred text measures per Guide entry.

It cannot be a `LazyRow`. Every lane and the timeline header share **one pixel `ScrollState`**, which
is what keeps the grid aligned by *time*; a `LazyRow` scrolls by item index, and lanes have different
programme boundaries, so rows would drift out of lockstep. So the lane keeps the shared scroll and
culls by hand: `rememberGuideLane` precomputes each cell's x and width (exactly the values
`spacedBy(6.dp)` produced, so it is pixel-identical), a full-width spacer holds the scroll extent, and
a `derivedStateOf` picks the visible index range.

Two things there are load-bearing and easy to get wrong:
- **The buffer is one full viewport on each side, not a comfort margin.** D-pad focus can only travel
  to a *composed* focusable. Too small a buffer and the guide dead-ends at the edge of the culled
  region — reaching an unbuilt cell would take one keypress crossing the whole visible width.
- **`key(slot.startMs)` around each cell.** A plain loop memoizes by POSITION, so as the range slides,
  slot 0 becomes a different programme and Compose rebuilds each cell's remembered state *under the
  focused node* — dropping D-pad focus mid-scroll. Same guarantee `items(key = …)` gives a lazy list.

**Search composed up to ~90 glass tiles at once** — two eager `FlowRow`s over 30 channels + 60 titles,
each with its own image, shadow bake and several text measures. Migrated to the chunked-rows
`LazyColumn` pattern Favorites and Browse already use (`BoxWithConstraints` + `chunked()` reproduces
FlowRow's ragged left-aligned packing; a `LazyVerticalGrid` would spread the leftover width and widen
the gaps). Consequence, matching those screens: the query field and scope chips are now pinned and
only results scroll, and the route moved from `ScrollableTab` to `FullSizeTab` — a lazy layout cannot
nest in a same-axis unbounded scroll.

**Settings and Recordings got `contentType`.** Recordings is the real one (bare heading vs. glass card
are genuinely different shapes), and its per-group `rows.filter {}` moved out of the `LazyListScope`
builder, where it re-scanned the whole list once per group every time the item provider was rebuilt.
Settings is honestly marginal — four of its six panes have a single item, so the hint is inert there;
applied for uniformity, and **Settings has still never been Perfetto-traced.** Do not cite it as a win.

## Locked decisions

| decision | status | why |
|---|---|---|
| Sample the backdrop only where an effect runs | **Locked 2026-07-28** | Worth 9.5 ms of RenderThread on the XL95 at no visual cost. Measured, pixel-diffed. |
| Keep `AmbientBackdrop` + the Tier B token retune | Locked | The retune *is* the glass look; only the redundant per-surface sampling was removed. |
| Ambient mesh stays static (`t = 0`) | Locked | The bake depends on it. Drift would invalidate the cached bitmap every frame. |
| ~~Keep the glass backdrop system~~ | **Superseded 2026-07-28** | Locked on emulator data that understated the cost ~4×, and on a test that conflated the token retune with the sampling. |
| Sidebar rail has **no** backdrop blur | **Locked 2026-07-28** | Worth −48% RenderThread and 65 → 2 janky frames on the XL95; the rail stays sheer-translucent. See "The sidebar frost removed" below. |
| One D-pad step per frame, extras dropped | **Locked 2026-07-28** | Stops focus running ahead of the renderer without throttling held-key fast scroll. See "D-pad input gating" below. |

## Open items

- **Light theme and dialogs are unverified** against the sampling change. `CLAUDE.md` warns that
  near-white surfaces on the off-white page need a `borderDefault` edge to read, and glass alpha
  behaves differently over a light page. This is the most likely place for a regression.
- **~5 ms of RenderThread remains** at p50, plus ~4 ms of sync/upload. Not yet attributed.
- ~~The sidebar expand/collapse animation is unmeasured on the TV.~~ **Done — see below.**
- **Known composition-level issues, deliberately not fixed.** Unstable `Iterable` params on
  `ChipChoiceRow` (plus `values().asIterable()` allocated per recomposition at four call sites),
  `animateDpAsState` read in composition scope in `AreSwitch`, a `focusRequester` toggling
  structurally mid-animation in `AreSegmentedControl`. All real; all inside the 0.07 ms. Fix them if
  they cause a problem that is *measured*, not on principle.
- **Baseline profile:** one *is* shipped (`assets/dexopt/baseline.prof`, 6367 rules). Sideloads leave
  ART at `status=verify` and never apply it; Play installs do. Forcing `speed-profile` took cold
  start from ~118 ms to ~103 ms on the emulator and made no difference to steady-state scroll, which
  is expected — a baseline profile buys cold start, not sustained scrolling.

---

## Home cold-scroll: the attribution (2026-07-28, Perfetto, XL95)

Six removal A/Bs returned six nulls. Switching instrument — a real Perfetto trace over the mixed
cold sequence, then the identical sequence again warm — named it in one pass. **Two reps, and the
numbers are identical to the millisecond.**

Protocol: force-stop → launch → settle 25 s → `perfetto -d -t 11s -b 96mb -a com.arashrahimi46.iptv
gfx view sched freq am wm` → 16-key mixed sweep → repeat warm. The whole sweep runs in **one**
`adb shell` loop, not one fork per key — the per-key fork was the documented source of rep-to-rep
variance, and it also stretched the sweep past the trace window.

### What it is

| slice | cold r1 / r2 | warm r1 / r2 |
|---|---|---|
| **`flush layers`** | **48 × / 188 ms** | **8 × / 2 ms** |
| `shader_compile` | 23 × / 42 ms | absent |
| `Record View#draw()` | 637 / 632 ms | 471 / 469 ms |
| `AndroidOwner:measureAndLayout` | 622 / 638 ms | 579 / 544 ms |
| `Compose:recompose` | 413 / 413 ms | 444 / 446 ms |

Thread states (`sched`), the ground truth for work vs waiting:

| | cold r1 / r2 | warm r1 / r2 | delta |
|---|---|---|---|
| main thread Running | 2734 / 2696 ms | 2529 / 2491 ms | **+205 ms** |
| RenderThread Running | 2098 / 2099 ms | 1799 / 1729 ms | **+335 ms** |
| main thread R (CPU starved) | 143 / 220 ms | 193 / 155 ms | noise |

Frames: cold 322 frames, **23 over 16.7 ms**; warm 331 frames, **5 over 16.7 ms**.

**The RenderThread is the bigger half of the cold penalty, and `flush layers` is ~60% of it.**
Cold draws tile- and rail-sized offscreen layers — `drawLayer [graphicsLayer] 416×704` ×35,
`416×1000` ×10, `192×1000` ×7 — while warm draws **only** the 1920×1080 root and nothing else.
*Compositing* those layers is trivial (16 ms total); *rasterizing* them the first time is the 188 ms.

### Why every removal A/B failed

The cost is the layer allocation + first rasterization, not what is painted inside it. Removing
`softShadow`, images, or the artwork blur reduces the *content* of a layer that still has to be
allocated and rasterized — which is exactly why `softShadow` gave a real but modest −9 % and
everything else gave nothing.

### Things now definitively ruled out

- **CPU governor / clock ramp.** Cold avg 1571 MHz, warm 1549 MHz — cold ran *faster*. All four
  A55 cores lockstep. This was a genuinely plausible "first pass slow, follow-ups fast" story and
  it is dead.
- **Composition.** `Compose:recompose` is 413 ms cold vs 444 ms warm — warm does *more*. Lazy
  prefetch and `decodeBitmap` are also both higher warm. Composition is not the cold penalty, which
  is why `contentType` (a real win, `a565ac6`) did nothing for it.
- **CPU contention.** Runnable-but-not-running time is 143–220 ms in every condition.
- **Shader compilation** is real but small: 42 ms, ~3 % of the gap. Consistent with the splash
  `ShaderWarmup` test measuring nothing.

### Next

Find what promotes entering tiles to offscreen layers. `TvFocusable` puts
`graphicsLayer { scaleX; scaleY }` on **every** focusable, i.e. every tile — the prime suspect. Test
`CompositingStrategy.ModulateAlpha` there (alpha is always 1, so it is visually a no-op) and re-run
this exact protocol. **Verify against `flush layers` count/total, not against wall-clock percentiles**
— that counter reproduced at 48/188 and 8/2 across independent reps and is the cleanest signal in
this document.

## The tile-chain bisect, and the one real win (2026-07-28, release build)

One release APK with a launch-extra switch (`--ei perfBisect N`), so every candidate in the tile draw
chain could be A/B'd inside a single build instead of one build per variant. Protocol per variant:
force-stop → launch with the extra → settle 25 s → 11 s trace over the mixed sweep, **run twice,
score the second** (see the shader-cache trap below).

| variant | `flush layers` | RenderThread Running | janky |
|---|---|---|---|
| 0 control | 49 × / 184 ms | 2093 ms | 27 |
| 1 no `softShadow` | 48 × / 195 ms | 2011 ms | 26 |
| **2 no glow + ring** | 52 × / 171 ms | **1720 ms** | 21 |
| 3 no `focusSheen` | 49 × / 186 ms | 1964 ms | 27 |
| 4 no background + border | 49 × / 185 ms | 1943 ms | 25 |

**Nothing in the tile draw chain changes the layer count.** `flush layers` sits at 48–52 in every
variant. The layers are not created by any single modifier — they are new tiles' RenderNodes being
recorded for the first time, an inherent cost of new content appearing. Removing a modifier shrinks
what is *inside* a layer that still has to be allocated and rasterized. **Stop trying to delete them.**

### What the bisect did find: the focus glow was blurring on the CPU every frame

`tvGlowCached` hoisted its Path and `BlurMaskFilter` paint out of the draw pass but still *executed*
the mask filter on every draw — and Skia's mask blur is a CPU pass, not a GPU one. The focused
element redraws its glow every frame it holds focus, so a D-pad sweep is a continuous CPU blur.
`softShadow` had already solved exactly this by baking; the glow never got the same treatment.

Baking it (half-res, `pad = 3σ`, alpha modulated at blit) — **landed**:

| | RenderThread Running | janky |
|---|---|---|
| old glow | 2093 / 2128 ms | 27 / 27 |
| **baked glow (shipping)** | **1778 / 1742 / 1729 ms** | 24 / 29 / 29 |
| glow deleted entirely (ceiling) | 1720 / 1713 ms | 21 / 21 |

**−18 % RenderThread, reproduced three times, recovering ~85 % of the ceiling set by deleting the
glow outright.** Pixel-equivalent: screenshot diff max **3/255**, 7 px of 2 M differ by >2, and the
differences fall entirely inside the focused tile's bbox (which also proves the glow rendered in both).

**Honest limit: this did NOT reduce the janky-frame count.** Control 27/27, shipping 29/29. At this
sample size janky count swings ±5 and cannot resolve the change; `RenderThread Running` reproduces to
~1 % and is the metric to use. This buys GPU headroom, not a proven jank reduction.

**Cost:** a baked bitmap per focusable, ~292 KB for a poster tile — versus the ~536 KB `softShadow`
already bakes for the same tile. ~55 % on top of an existing per-tile cost, not a new class of one.

### Two methodology traps found the hard way

- **Reinstalling wipes the shader cache.** The first trace after an `adb install` showed
  `shader_compile` at **500 ms vs a settled 39 ms**, making an unrelated change look catastrophic.
  Always discard the first post-install run. Several earlier A/Bs in this document compared a
  post-install build against a long-installed one and are contaminated by this.
- **Debug vs release is ~2×** on the main thread (Running 4797 ms vs 2770 ms) and it *inverts which
  half dominates*: in debug, `measureAndLayout` swamps everything and the RenderThread looks minor.
  Only ever compare like with like, and prefer release — it is what ships.

---

## The sidebar frost removed — 2026-07-28 (session: real-TV bug sweep)

Reported as two separate complaints: *"when I want to expand the sidebar it first opens it and then
it makes it blurry"* and *"the sidebar animation is a bit laggy, it's a very simple animation that
shouldn't be laggy."* They turned out to be the same finding.

### Protocol

Release build, Sony XL95 (API 31, Tier B), Perfetto `gfx view sched`, 11 s window, 10
expand/collapse cycles driven from a single `adb shell` loop. Scored on **RenderThread `Running`**
from `sched`, which reproduced across three separate batches at **2153 / 2179 / 2156 ms — ±0.6 %**.
Percentiles are from `DrawFrames` on the RenderThread. First post-install run discarded each time.

### What was measured

| variant | RT Running | frames > 16.7 ms |
|---|---|---|
| baseline (frosted) | 2156 ms | ~60–65 |
| **backdrop blur pass removed** | **1502 ms (−30 %)** | **4** |
| frost cross-dissolve removed | 1759 ms (−18 %) | 51 |
| blur radius 28 dp → 14 dp | 2332 ms (**+8 %, worse**) | 72 |
| frosted node follows the animating width | 2448 ms (**+13 %, worse**) | — |
| page captured from frame 0, dissolve kept | 2372 ms (**+10 %, worse**) | 75 |
| panel `softShadow(bake = false)` removed | 2144 ms (−0.4 %) | 56 |
| `scrollEdgeFade` offscreen layer removed | 2116 ms (−1.8 %) | — |

### Conclusion

**The 28 dp backdrop blur was the entire bottleneck and nothing else was close.** Two hypotheses
that looked obvious in the code were tested and killed:

- The panel's `softShadow(bake = false)` runs a real CPU `BlurMaskFilter` over a full-height path on
  every draw. It reads like the worst thing in the file. It is worth **0.4 %**. Left alone.
- Halving the blur radius made things **worse**, not better. The cost is the blur *pass* — the
  offscreen render and backdrop sample — not the kernel width, so there is no cheap-blur setting to
  tune. The choice is binary: have it or not.

Pinning the frosted node at the open width was re-confirmed as correct (letting it follow the
animating width costs +13 %), and the settle gate was re-confirmed as correct (capturing from
frame 0 while keeping the dissolve costs +10 %). Both earlier decisions were right *given* the blur.

### What shipped

The blur is gone. The expanded rail keeps the same `surfaceGlassSheer` fill it already cross-faded
from, so it stays genuinely translucent — the page reads through it, just unblurred. Measured
end state, same protocol:

| | baseline | shipped | |
|---|---|---|---|
| RenderThread Running | 2156 ms | **1113 ms** | **−48 %** |
| frame p50 | 9.46 ms | **6.58 ms** | −30 % |
| frame p90 | 19.21 ms | **10.17 ms** | −47 % |
| frames > 16.7 ms | 65 | **2** | −97 % |

−48 % beats the −30 % of the blur-only variant because removing the blur made the whole page-capture
apparatus dead code: `LocalPageBackdrop`, the gated `layerBackdrop(pageBackdrop)`, the settle delay
and `frostedPanel` are all deleted, so the page subtree is no longer drawn twice while the rail is
open.

It also fixes the "opens, then goes blurry" glitch **by construction rather than by masking it**.
That glitch existed because the blur could only be captured after the tween settled; the
cross-dissolve was a 150 ms alpha fade whose only job was to hide the late arrival. With no blur
there is nothing to arrive late, so the dissolve, its two full-height `graphicsLayer` siblings and
the second fill node all went with it. The panel is correct from frame 0.

**This supersedes "keep the glass backdrop system" for the sidebar specifically.** The ambient
backdrop and every other glass surface are untouched. Do not reintroduce a backdrop blur on the rail
without re-running this measurement.

### Trade-off accepted

The deleted `frostedPanel` KDoc argued the blur was load-bearing: *"what is behind here is sharp
artwork and text, so without it the panel reads as a transparent window and the nav labels fight the
posters underneath."* Screenshot-verified on the XL95 over the Home poster rail — the sheer fill
dims the content enough that the labels stay legible. This was a deliberate product call, not an
oversight; revisit it if the rail ever sits over brighter content than Home.

## D-pad input pacing — 2026-07-28

Reported as *"when I press arrow up/down multiple times, YouTube executes them one by one, our app
tries to handle them at once and it glitches"*, then again after a first attempt: *"step by step
still doesn't work everywhere, I scrolled in Home and Settings and it jumped always."*

**The first attempt gated on the Choreographer frame and was wrong.** It accepted one directional
key per frame and dropped the rest, which sounds equivalent to pacing and is not: once the sidebar
frost was removed frames fell to ~6.6 ms, so the gate reopened every vsync and dropped nothing at
all. Verified on the XL95 — a true back-to-back burst (`input keyevent 20 20 20 20 20 20`, one
process) still moved all six rails. A frame gate only stops input running ahead of a *janking*
renderer.

Note the measurement trap that hid this: an earlier "verified" burst test used one `adb shell` fork
per key, so the presses were ~100 ms apart and never exercised the gate. **Send a burst inside a
single `input keyevent` invocation, or you are testing nothing.**

The real problem is not frames, it is that each focus move *retargets the in-flight bring-into-view
scroll animation*, so several quick presses collapse into one scroll teleport instead of several
visible steps. The pacing therefore has to match the scroll animation, which is many frames long.

**Shipped:** `MainActivity.dispatchKeyEvent` accepts one directional key per **120 ms**
(`DPAD_STEP_MS`, ~8 steps/second) and drops the rest. Verified on device: the same six-press burst
now advances exactly one step. A single isolated press is never delayed, so ordinary navigation
gains no latency — only bursts are paced.

Deliberate choices:

- **Steady, not accelerating** on a long hold. A constant rhythm is what lets you stop on the item
  you want; overshoot was the original complaint.
- **Dropped, not queued.** Queueing preserves every press but lets focus keep travelling after the
  user has stopped, which reads as lag rather than precision.
- `DPAD_CENTER` and `ACTION_UP` are never gated — `TvFocusable` resolves short vs long press from
  its own down→up span, so swallowing either half would break OK entirely.

---

## The focus glow is gone, and the "Live now"/"Categories" rails are a measure cost (2026-07-28)

Measured on the XL95 (`192.168.178.28:5555`), release builds, 12 s Perfetto capture over a scripted
Home **down**-sweep (2 downs + 4 rights, ×5, all inside one `adb shell`), ≥2 reps per variant.

### Focus glow deleted — the one change that moved the needle

`tvFocusable` no longer draws `tvGlowCached`; the crisp `focusRingCached` ring alone carries focus.
`Switch`'s own always-on `tvGlow` halo went with it. Measured, HEAD → shipped:

| | HEAD | shipped | Δ |
|---|---|---|---|
| `AndroidOwner:draw` | 316 ms | 207 ms | **−35 %** |
| frames > 16.7 ms | 17 | 11.5 | **−32 %** |
| RenderThread Running | 1323 ms | 1299 ms | flat |
| `AndroidOwner:measureAndLayout` | 577 ms | 571 ms | flat |

Note the earlier entry claiming the *baked* glow recovered "~85 % of the win" of deleting it: that
was measured before the sidebar-blur removal, when RenderThread dominated. On the current build the
glow's cost shows up in `AndroidOwner:draw` (draw-command recording), and deleting it is worth ~3×
what baking it was. **Do not reintroduce a focus glow without re-running this.**

### The two heavy rails cost measure/layout — and it is NOT the decorations

The user's own A/B (hiding "Live now" + "Browse by category" from Home) was reproduced with a
`perfBisect` launch-extra that forces them back over the persisted layout. Forcing both on:

| | rails off | rails on | Δ |
|---|---|---|---|
| `AndroidOwner:measureAndLayout` | 407 ms | 568 ms | **+40 %** |
| `compose:lazy:prefetch:*` (all) | 305 ms | 565 ms | **+85 %** |
| frames > 16.7 ms | 8 | 13 | +63 % |
| `AndroidOwner:draw` | 232 ms | 176 ms | −24 % |
| RenderThread Running | 1224 ms | 1004 ms | −18 % |

> **Correction (same day, later batch):** this table compares two *separate* capture batches. A
> later run that interleaved rails-on/rails-off within one batch (2 reps each, alternating) showed
> **no measure delta at all** — 574 vs 564 ms. The device baseline drifted between batches (Coil disk
> cache, shader cache, catalogue warmth), so the +40 % above is not trustworthy. Only compare
> variants captured **interleaved inside one batch**. What did survive is that the rails add a few
> janky frames, not that they dominate measure.

Live-now and Categories contribute roughly equally in the first batch (~+90 ms and ~+80 ms each).
**Draw and RenderThread go DOWN** with them on — they displace poster rails, whose bitmaps are the
draw cost. So this is purely a composition/measure problem.

**Three things that look like the culprit and measurably are not** (all within ±2 %, i.e. noise):

- Removing `AreChannelTile`'s LIVE badge **and** the stream-health dot.
- Removing `AreChannelTile`'s category chip row.
- Removing `AreCategoryCard`'s 132 dp folder watermark `Icon` (a `rememberVectorPainter`
  subcomposition per card — the obvious suspect, and it is not one).
- Tuning `LazyListPrefetchStrategy(nestedPrefetchItemCount = 0 / 1 / 2)` on the Home `LazyColumn`.

The cost is the **aggregate layout-node count** of the tiles (`AreChannelTile` is ~24 layout nodes
vs `ArePosterTile`'s ~10), amplified by the outer `LazyColumn`'s unit of prefetch being *a whole
rail*: individual `compose:lazy:prefetch:measure` slices run **10–15 ms each**, i.e. one of them can
eat a whole frame. Shaving individual elements cannot fix that — only a genuinely flatter tile
would, and that is a rewrite, not a tweak.

`TextAnnotatedStringNode:measure` is 83 → 126 ms across the whole 12 s sweep (~1.1 ms per call): real
but a minority of the delta. The earlier framing that text measure dominates a rail is overstated.


## The focus *sheen* + *ring* interaction — the real draw cost (2026-07-28)

Chasing the above further, the biggest lever turned out to have nothing to do with the rails. In
`TvFocusable` each focusable carries three draw decorations: the focus **sheen**, the glass
**border**, and the focus **ring**. Removing them one at a time, and in pairs, over the same
interleaved protocol (`AndroidOwner:draw`, 2 reps each, controls re-run in the same batch):

| variant | draw | vs control |
|---|---|---|
| control (all three) | 204 ms | — |
| no sheen | 200 ms | −2 % |
| no border | 210 ms | 0 % |
| no ring | 207 ms | −1 % |
| no sheen + no border | 183 ms | −10 % |
| no ring + no border | 187 ms | −8 % |
| **no sheen + no ring** | **135 ms** | **−34 %** |
| all three off | 126 ms | −38 % |

**Sheen and ring are superadditive and the border is irrelevant.** Reproduced across three separate
batches. No mechanism was identified — and one plausible one was ruled out:

- **It is NOT per-node draw overhead.** Merging fill + sheen + border + ring into a single
  `drawWithCache`/`onDrawWithContent` node (four draw nodes → one, on every focusable in the app)
  bought **−4 %**, not the −34 %. That experiment was written, verified pixel-identical on device,
  measured, and then deleted — do not re-attempt it.
- It is not the pixels either: the ring only draws on the *one* focused element (alpha 0 elsewhere),
  so its cost cannot be proportional to what is on screen.

**Shipped:** the sheen is deleted (`focusSheen`, and `TvFocusable`'s `showFocusSheen` opt-out with
it). The ring stays — it is the focus indicator and non-negotiable. That caps the recoverable win at
about **−10 %** of draw; the other ~25 % is locked behind the ring.

### Cumulative, original HEAD → shipped

Same protocol, user's real Home layout, 2 reps each:

| | HEAD | shipped |
|---|---|---|
| `AndroidOwner:draw` | 316 ms | **194 ms (−39 %)** |
| frames > 16.7 ms | 17 | **10 (−41 %)** |
| `AndroidOwner:measureAndLayout` | 577 ms | 501 ms (−13 %) |
| main-thread Running | 2500 ms | 2358 ms (−6 %) |

The `measureAndLayout` drop is the deleted "Browse by category" rail (removed as a product call, not
a perf one).

### Harness note

Add a `perfBisect` `var` read from a launch extra (`am start … --ei perfBisect N`) so one release APK
A/Bs every variant. **Always interleave variants and re-run the control inside the same batch** —
cross-batch baselines on this device drift by more than most of the effects being measured.

---

## Round 2: the focus tweens were read in composition (2026-07-28)

Same protocol, all variants interleaved inside one batch with the control re-run alongside.

### Shipped: defer the focus-tween reads to draw/layer time

`tvFocusable` held both focus tweens as `val x by animateFloatAsState(...)`. The `by` delegate reads
`.value` **during composition**, so every one of the ~8-15 tween frames recomposed `tvFocusable` and
rebuilt the whole modifier chain — on both the element losing focus and the one gaining it, for every
D-pad step. A previous pass believed it had fixed this by passing the ring alpha as a lambda
(`{ ringAlpha }`), but by then `ringAlpha` was already an unwrapped `Float`, so the lambda captured a
value rather than a state read and changed nothing. Holding the `State` and reading `.value` inside
the `graphicsLayer` / `focusRingCached` lambdas is the actual fix.

| | control | draw-time read | no animation at all |
|---|---|---|---|
| `Compose:recompose` | 419 ms | **343 ms (−18 %)** | 353 ms |
| main-thread Running | 2532 ms | **2372 ms (−6 %)** | 2311 ms |
| worst frame | 123 ms | **107 ms** | 104 ms |
| `AndroidOwner:draw` | 216 ms | 214 ms | 180 ms |

Deferring the read captures the **entire** recomposition win with zero visual change. Deleting the
tweens outright buys a further ~16 % of draw — that is the cost of actually animating two values on
two elements per step, and it is the only thing still on the table there.

### Where the remaining jank is: composing a rail costs ~90 ms

There is one ~105 ms frame at t≈2170 ms in **every** trace: the first DOWN press, i.e. Home's first
vertical scroll. Inside it, `AndroidOwner:measureAndLayout` is a single **89 ms** call, and its
contents are lazy-list *subcomposition happening inside the measure pass*: `Compose:recompose` 31 ms,
`Compose:applyChanges` 22 ms, `TextAnnotatedStringNode:measure` 13 ms, Coil painter creation 11 ms.
Composing one new rail (header + LazyRow + ~7 tiles) is ~90 ms of work and Compose does it inside
measure. The `compose:lazy:prefetch:*` chunks (30-37 ms, top-level) are the framework trying to hide
this in idle time and failing — no idle window at 60 Hz is 35 ms long.

### More things that are NOT the problem (all measured, all ~0)

- `blur(72.dp)` on the ambient artwork — a full-screen RenderEffect, and removing it did nothing.
- The ambient artwork wash entirely.
- Every `softShadow` in the app (`glassSurface` + `TvFocusable`).
- `tileWash` on every tile.
- The focus-scale `graphicsLayer` — removing it made things slightly *worse*.
- The five duplicate `collectIsFocusedAsState`/`collectIsPressedAsState` calls per tile
  (PosterTile ×1, TvFocusable ×2, tvFocusable ×2 on the same interaction source). Deduplicating them
  to one pair changed nothing — they are cheap.

Draw is now near its floor (`AndroidOwner:draw` 316 → ~198 ms since the start of this work). The
remaining lever is **composition cost per tile**, paid in bursts when a rail scrolls in.

---

## Round 3: the focus ring was re-recording every tile it ringed (2026-07-29)

`focusRingCached` wraps the element's own draw — `onDrawWithContent { drawContent(); ring }`. So
animating its alpha invalidates and **re-records the entire element's display list on every tween
frame**: poster bitmap, texts, badges and all, on both the tile losing focus and the one gaining it,
for every single D-pad step.

Split the two focus tweens and it is unmistakable (`AndroidOwner:draw`, interleaved batch):

| variant | draw |
|---|---|
| control | 112 ms |
| snap the **scale** (no grow animation) | 119 ms — nothing |
| snap the **ring alpha** (no fade) | 71 ms — **−36 %** |
| snap both | 70 ms |
| **ring as a sibling overlay, fade intact** | **61 ms — −45 %** |

**Shipped: the ring is now a sibling `Box` with its own `graphicsLayer` whose alpha animates.** A
RenderNode alpha property update costs nothing to re-record, and the overlay never wraps the content
draw. It beats *deleting the animation entirely* (61 vs 71 ms) while keeping the fade. Screenshot-
verified: 0.17 % of sampled pixels differ, all anti-aliasing along the 3 dp stroke.

`Modifier.tvFocusable` keeps a `drawRing` parameter (default true) for `AreTextField`, the one
standalone user — a single control, not one of forty tiles, and it has no `BoxScope` for an overlay.

This also retroactively explains round 2's sheen+ring superadditivity: **both** wrapped `drawContent`,
so removing either one alone left the other still invalidating the element every frame.

### Composing a rail: where the ~90 ms actually goes

A flattening ladder on `ArePosterTile`, measured against the reproducible first-DOWN frame:

| tile content | worst frame | measureAndLayout | recompose |
|---|---|---|---|
| full (control) | 107 ms | 265 ms | 182 ms |
| no `AsyncImage` | 78 ms | 221 ms | 149 ms |
| `TvFocusable` + 2 texts only | 68 ms | 203 ms | 149 ms |
| bare focusable Box + 2 texts | 60 ms | 193 ms | 140 ms |

So of the ~107 ms: **Coil's `AsyncImage` is ~29 ms (27 %)**, the rest of PosterTile ~10 ms, and
`TvFocusable` itself ~8 ms. Flattening the tile's *layout* is not where the money is — the image is.

Tried and worth **nothing**: giving `AsyncImage` a remembered, explicitly-sized `ImageRequest` to skip
Coil's `ConstraintsSizeResolver` (111 vs 112 ms). If the Coil cost is to be attacked, it is
`rememberAsyncImagePainter` + `onRemembered` themselves, not request construction.

---

## Round 4: Coil's `AsyncImagePainter` was a third of the rail-composition cost (2026-07-29)

Round 3's flattening ladder put ~29 ms of the ~107 ms first-DOWN frame inside `AsyncImage`. That is
not the decode (which is off the main thread) — it is `AsyncImagePainter`, a `RememberObserver` that
resolves its own size from the layout constraints, tracks load state and drives recomposition
through it.

**Shipped: `rememberTileArtwork` in `TileWash.kt`** — one suspend `imageLoader.execute` in a
`LaunchedEffect` plus a plain `Image(bitmap)`, used by `ArePosterTile` and `AreChannelTile` (the only
two composables that appear ~9-at-a-time in a scrolling rail). Everything Coil is actually needed for
still runs — HTTP, disk and memory caches, RGB565, the video-frame decoder; only the Compose painter
layer is bypassed.

| | control | hand-rolled |
|---|---|---|
| worst frame (first DOWN) | 114 ms | **76 ms (−33 %)** |
| `AndroidOwner:measureAndLayout` | 286 ms | 243 ms (−15 %) |
| `Compose:recompose` | 183 ms | 169 ms (−8 %) |

That is within noise of the "no artwork at all" rung of the round-3 ladder (78 ms) — i.e. the painter
was essentially the *entire* cost of having images in a rail.

Two details that are load-bearing, not incidental:

- `execute` with **no size** decodes at full source resolution. `maxDimension` is mandatory; on a
  20k-title catalogue an unbounded decode is how a rail scroll runs the heap out.
- The result goes through `Drawable.toBitmap()`, **not** a `BitmapDrawable` cast. Providers do serve
  animated GIF logos, and a cast would silently strand those tiles on their initials forever.

`sampleTileWashHue` now takes the `Bitmap` directly (it was reaching into the drawable anyway), fed
from `rememberTileArtwork`'s `onBitmap` — still exactly one request per logo, which is the constraint
that matters (Xtream rate-limits by IP).

**Not attempted: the Coil 3 migration.** The hand-rolled path already reaches the no-image floor, so
there is nothing left for a faster painter to win. Coil 2.7.0 stays.

### Cumulative, original HEAD → here

| | HEAD | now |
|---|---|---|
| `AndroidOwner:draw` | 316 ms | **~58 ms (−82 %)** |
| worst frame | ~110 ms | **83 ms** |
| `AndroidOwner:measureAndLayout` | 577 ms | 238 ms |

Draw-command recording is no longer a meaningful cost on this screen. What remains is composition of
new rails, and the largest single item left in it is now Compose's own text layout.

---

## Round 5: text layout is settled — nothing meaningful left (2026-07-29)

Traces name `TextAnnotatedStringNode:measure`, which is Compose's *expensive* text path — plain-String
text should land on the cheaper `TextStringSimpleNode`. `androidx.tv.material3.Text` routes through the
annotated node regardless. That is real, and it is not worth acting on.

Routing the rail tiles' text through `BasicText(String)` does move them to the cheap node
(`TextStringSimpleNode::measure`, 24 calls, ~1.06 ms each vs ~1.28 ms), but the totals barely shift.
So the ceiling was measured directly: **every** text lever pulled at once — cheap node, no
letter-spacing, system font instead of Manrope/Space Grotesk, `Clip` instead of `Ellipsis` — 3 reps
each, interleaved:

| | control | every lever pulled |
|---|---|---|
| total text measure | 47 / 47 / 46 ms | 43 / 42 / 42 ms |
| `AndroidOwner:measureAndLayout` | 246 / 255 / 242 ms | 251 / 239 / 243 ms |
| main-thread Running | 1015 / 1018 / 1030 ms | 1016 / 1006 / 1016 ms |
| worst frame | 82 / 82 / 71 ms | 76 / 77 / 79 ms |

**−11 % of a cost that is itself 4.6 % of main-thread Running**, i.e. ~0.5 % overall — and it is the
only lever left that would cost real design (the brand fonts, the tracking, ellipsis on a TV where
titles genuinely overflow). Everything was reverted. Do not revisit text layout without a new profile
showing it has grown.

Individually measured on the way, all in the same noise band: dropping `letterSpacing` (em-based),
forcing `FontFamily.SansSerif`, `Clip` instead of `Ellipsis`, and `softWrap = false`.

### Where things actually stand

Draw-command recording went 316 → ~58 ms over rounds 1-4 and is no longer a meaningful cost. What is
left on a Home sweep is **composition and measure of new rails**, and after the Coil painter went it
has no single dominant item — text is ~19 % of `measureAndLayout`, the rest is spread across the
layout tree. The next real win would have to be structural (fewer rails composed per scroll, or a
tile with materially fewer layout nodes), not another modifier-level fix.

---

## Round 6: the shell recomposed on every frame of every scroll (2026-08-05)

Sweep of the whole app, not just Home: a 9-dimension static audit (shell, Home, Guide, Settings,
grids, theme-draw, animations, data-load, build config) with every finding adversarially verified,
plus the first **Perfetto traces of Settings and the Guide**, which this document had never measured.

### Where the app actually stands now

Release build, `speed-profile` AOT-compiled, XL95, RTL (the user's real config), 11 s captures.

| screen | main-thread `draw`/frame | worst frame | frames > 16.7 ms |
|---|---|---|---|
| Home + sidebar (16 expand/collapse) | 1.8 ms | 8 ms | **0** |
| Guide | 2.0 ms | 55 ms | 3 |
| Settings | 3.2 ms | 75 ms | 5 |

**Home and the sidebar are finished.** Rounds 1-5 worked; zero janky frames. RenderThread is a flat
~6 ms/frame on all three screens. Everything left is a handful of large **first-paint** frames on
Settings and the Guide, and Settings' worst frame is not draw-bound at all: of 156 ms,
`AndroidOwner:measureAndLayout` is **113 ms**, and 39 ms of that is 31 `TextAnnotatedStringNode:measure`
calls — lazy-list subcomposition inside the measure pass. Settings' `LazyColumn` emits a whole
section *card* per item, so entering a pane composes every row in it regardless of visibility. That is
the next structural lever there; it was not taken this round.

### Compose compiler metrics (wire up `composeCompiler { metricsDestination/reportsDestination }` to reproduce)

**0 non-skippable composables**, strong skipping and `IntrinsicRemember` already on (Kotlin 2.2.10).
79 unstable classes, all `*UiState`/row types made unstable by plain `List`/`Map`/`Set` fields. The
composition layer is clean; do not go looking for skippability wins.

### Shipped, and what it was worth

Scored with the `perfBisect` launch-extra so both variants ran **interleaved inside one batch** —
mandatory here, see the drift warning below. Movies grid (uniform tiles, no video), D-pad sweep
driving bring-into-view scrolls, 2 reps per variant.

| | before | after | Δ |
|---|---|---|---|
| `Compose:recompose` | 357 / 361 ms | **290 / 281 ms** | **−20 %** |
| main-thread Running | 2094 / 2100 ms | **1973 / 1957 ms** | **−6.3 %** |
| RenderThread Running | 974 / 985 ms | 955 / 945 ms | −2.3 % |

Reps cluster to ±8 ms inside each variant and all four runs move the same way. Three changes:

**1. `onFocusedBoundsChanged` was recomposing the entire shell on every frame of every scroll.**
`MainActivity` wrote the focused content's `Rect` into a `ShellHost`-scoped state and **read it back in
`ShellHost`'s own scope** to hand to the mini-player overlay. Foundation's `FocusableNode` fires
`onFocusedBoundsChanged` whenever the focused node is **repositioned**, not only when focus moves — so
during a bring-into-view scroll the rect changes every frame and invalidated the whole shell every
frame, on the app's dominant interaction, on every screen. Now passed as a **lambda** and invoked
inside `LiveMiniPlayerOverlay` below its not-docked early return, so the subscription only exists
while a mini is actually docked.

**2. `ParentalBlurState` was rebuilt every recomposition.** It holds a `Set<String>`, so Compose infers
it unstable and compares by identity; the `content` lambda handed to `AreIptvAppShell` captures it, so
a fresh instance defeated that lambda's memoization and **`AreIptvAppShell` could never skip**. Now
`remember`ed. This compounds with (1): every shell invalidation was re-running the whole app shell.

**3. `softShadow`'s bake was thrown away on every recomposition of its owner.** The KDoc promised
"RASTERIZED ONCE"; it never was for any owner that recomposes. `drawWithCache`'s element compares its
lambda by **reference identity** (`DrawWithCacheElement.equals` uses `!==`, verified in the
compose-ui 1.11.1 sources), and `softShadow` was a plain non-composable function minting a fresh
capturing lambda per call → `update(node)` → `block` setter → `invalidateDrawCache()` → the whole
`ImageBitmap` allocation plus CPU `BlurMaskFilter` pass re-ran inside the next draw. Guide's
`FocusedInfoBar` recomposes on **every D-pad step**, so it was allocating a ~630 KB bitmap and running
a Gaussian blur ~8×/second; every elevated chip/button/tab re-baked twice per keypress through
`TvFocusable`. `softShadow` is now `@Composable` and `remember`s its element. Alone it measured only
main −1.3 % / RT −1.8 % on Settings — real, free and pixel-identical, but small; the win above is
mostly (1)+(2). Remembering cannot stale the bake: `CacheDrawModifierNodeImpl` still invalidates on
measure-result, density and layout-direction changes, which is exactly what the `bake = false`
sidebar relies on.

### Also shipped: main-thread work during page load

Neutral on a steady-state grid sweep (confirmed, no regression), but all three were blocking the UI
thread while a screen was first painting:

- **`HomeViewModel`'s curation pipeline** — a `combine` transform runs in the **collector's** context,
  and `launchIn(viewModelScope)` is `Dispatchers.Main.immediate`; the Room flows' dispatcher only
  governs where the *query* runs. So `HomeRailCurator` ran on the UI thread: `baseName()` applies 12
  compiled quality regexes plus two more per title over 200-title pools ×3 and a 400-title merged pool
  for `recommend` — ~14 k regex passes, plus the `groupBy`/sort work in `diversify` — on every catalog,
  parental or layout emission. Added `.flowOn(Dispatchers.Default)`; the `_uiState` write stays on Main.
- **`GuideViewModel.buildRows`** — same mechanism: a `groupBy` over every programme in the 6 h window
  plus a per-channel sort/map/filter for up to 300 channels (~1800 slot allocations), on Main, on every
  day-chip and category change and every `epg_programs` invalidation. Wrapped in
  `withContext(Dispatchers.Default)`.
- **`OkHttpClient` per repository** — OkHttp 4 eagerly builds a platform `X509TrustManager` and an
  `SSLContext` per instance when using default TLS specs. `PlaylistRepositoryImpl` and `EpgRepository`
  each built their own as eager constructor fields, and both are constructed in ViewModel constructors,
  i.e. synchronously on the composition thread as a destination composes — Home and Guide paid it twice.
  Now one process-wide `catalogHttpClient`, which also shares the connection pool.

### Latent bug: the shipped baseline profile contains ZERO app classes

`tv/src/release/generated/baselineProfiles/` **does not exist**, so the 6367-rule
`assets/dexopt/baseline.prof` this document calls "one IS shipped" is entirely library consumer
profiles (androidx, coil, kotlin). No composable, focus or tile code is AOT-compiled on a cold start.
Regeneration was also impossible: `BaselineProfileGenerator.PACKAGE_NAME` still held the Kotlin
**namespace** (`com.arashrahimi46.iptv`) while the plugin installs the nonMinifiedRelease APK under the
**applicationId** (`com.areiptv.tv`), so `BaselineProfileRule.collect` fails with "Unable to find target
package". The constant is fixed; **generating the profile is still to do**, and whoever does it must
seed a playlist into the nonMinifiedRelease install first — it gets a fresh empty DataStore and Room DB
under the new applicationId, so the sweep would otherwise profile onboarding and an empty Home and miss
`ArePosterTile`/`AreChannelTile`/`tvFocusable` entirely.

### Method notes

- **Cross-batch drift is still larger than most effects.** A first attempt to A/B `softShadow` by
  building it out entirely showed draw total 883 → 712 ms *and* `animation` 391 → 956 ms in the same
  run — removing a shadow cannot triple animation cost. Two builds compared across batches is not a
  measurement. Re-add a `perfBisect` launch-extra (`--ei perfBisect N`) and interleave.
- `-a` for `perfetto` is the **applicationId** (`com.areiptv.tv`); this document's older commands say
  `com.arashrahimi46.iptv` and capture nothing.
- Of 43 audit findings, **21 were refuted** on inspection — including several that read as obvious
  (marquee re-recording the rail at 60 fps, the Guide's shared `ScrollState` read in composition,
  `AreSegmentedControl`'s `onGloballyPositioned` writes, Favorites' missing lazy keys). Verify against
  the source before acting; plausible mechanisms here are usually already handled.

## Round 7: the baseline profile was never real, and a player controller nobody sees (2026-08-08)

First round measured on the **user's own catalogue** (9020 channels) on the XL95, English/LTR. Round 6
closed with "generating the profile is still to do". Doing it turned out to be the single largest win
recorded in this document — and the reason it had never worked was not the constant Round 6 fixed.

### The headline: Settings first paint, 244 ms → 85 ms

Same harness, same navigation (cold launch → settle 14 s → walk the sidebar to the gear → start the
trace → open Settings), same catalogue, 2 reps after.

| | before | after (rep 1 / rep 2) | Δ |
|---|---|---|---|
| worst frame | **244 ms** | **90 / 83 ms** | **−65 %** |
| `AndroidOwner:measureAndLayout` | 188 ms | 57 / 59 ms | −69 % |
| `Compose:recompose` | 134 ms | 42 / 35 ms | −71 % |
| `TextAnnotatedStringNode:measure` | 33 calls, 43 ms | **33 calls**, 15 ms | −65 % at equal count |
| main-thread Running | 574 ms | 231 / 225 ms | −60 % |
| **`Jit thread pool` Running** | **130 ms** | **1 ms** | **−99 %** |

The text-measure row is the proof of mechanism: **the same 33 measures cost a third as much.** No work
was removed. The code was simply compiled instead of interpreted-then-JIT-ed, and the JIT row collapsing
to 1 ms says the same thing from the other side.

Note this "before" (244 ms) is larger than the 75 ms in Round 6's table. Not a contradiction: that
number came from a settle-then-sweep capture, this harness traces the **genuine first paint** of the
screen after a cold launch. Round 7's before/after share one harness, so the delta stands regardless.

### Why the profile had never worked — two independent silent failures

**1. R8 ran on the variant the profile is generated from.** So every collected rule came back as an
obfuscated name (`La0;`) belonging to a mapping the shipped build does not share, and all of them were
dropped at merge. What shipped had 204 app rules: 193 Room DAOs (whose names R8 keeps) and 8
`MainActivity` entries. Zero composables, zero theme, zero focus, zero tile code.

A mitigation for exactly this already existed in `buildTypes { all { optimization { enable = false } } }`
and it *did* fire — and was then overwritten. The baselineprofile plugin creates
`nonMinifiedRelease`/`benchmarkRelease` with `initWith(release)` **after** the `android { }` block is
evaluated, copying release's `enable = true` straight back. Probed: `all` set false, and by
`afterEvaluate` the value was `true`.

Moving it to `project.afterEvaluate { }` fails the other way — AGP registers its own DSL finalization
when it is applied, i.e. before this file's callback, so the write lands after the DSL is locked and
Gradle reports every task `UP-TO-DATE`. The one hook in the window is
**`androidComponents { finalizeDsl { } }`**: after the plugin's copy, before variants are created.

Verification that beats reading the flag — count readable app classes in the generation APK:

```
~/Library/Android/sdk/build-tools/36.0.0/dexdump -f \
  tv/build/outputs/apk/nonMinifiedRelease/tv-nonMinifiedRelease.apk | grep -c "arashrahimi46/iptv/ui"
# broken: 0        fixed: 24130
```

> **AGP 9 has two minify switches and only one of them matters.** `release` reports
> `isMinifyEnabled == false` while being fully minified, because `optimization { enable }` is what
> drives R8 now. The legacy flag is the one the baselineprofile plugin clears, which is why the plugin
> does not get this right by itself.

**2. The profile can never be generated on this TV.** Collection needs **API 33+**, or root on API 28+.
The XL95 is API 31 and unrootable, so pointing `:tv:generateReleaseBaselineProfile` at it fails with
"Baseline Profile collection requires API 33+…". Use the `Television_1080p` AVD (API 36). This is now in
the generator's KDoc, along with the LTR requirement its `pressDPadLeft` navigation silently assumes —
in an RTL locale every one of those presses walks *away* from the nav rail and the tab half of the
journey profiles nothing.

**Do not aim that task at a device you care about.** It is a connected-android-test task and it
**uninstalls the app when it finishes**, which wipes app data. It destroyed the playlist, favourites and
settings on the real TV mid-session.

### Result, and the honest limit of it

| | before | after |
|---|---|---|
| generated profile, app rules | 204 | **1107** |
| merged release profile, app rules | **0** | **1107** |
| shipped `assets/dexopt/baseline.prof` | 12985 B | 15033 B |

Coverage is **partial**: theme 290 rules, components 67, player 23, data layer 442 — but Home, Browse,
Guide and the Settings *screens* are absent, because the profile must be generated on the emulator and
the emulator has no catalogue, so the journey cannot reach those screens. What is covered is the shared
hot path every screen pays (`ColorKt`, `AreIptvColors`, `FocusKt`, `GlassKt`, `AreIptvTypography`,
`UserSettings`), which is where the win above comes from. **Seeding a playlist on the generating
emulator is the remaining lever here** and should extend the same effect to every screen's own code.

### `PlayerView` inflated a controller the app never shows — 60.4 ms → 14.3 ms

Opening a player spent **60.4 ms inflating Android Views on the main thread**; ~44 ms of it was media3
UI that is never visible.

`PlayerView`'s constructor looks up `exo_controller_placeholder` in its layout and, finding one, builds a
full `PlayerControlView` in its place — transport controls, a `DefaultTimeBar`, a settings
`RecyclerView`, a `HorizontalScrollView`. **It does this regardless of `useController`**; that flag only
decides whether the already-built controller is ever shown. Confirmed in the media3 1.10.0 bytecode
(`exo_controller_placeholder` → `new PlayerControlView`, with no `useController` test on the path). All
three call sites set `useController = false` *after* construction, so they paid for every view and
displayed none of them.

`player_layout_id` is only readable from XML attrs in the constructor, so the fix is a layout:
`are_player_view.xml` → `are_player_content.xml`, which keeps only what is load-bearing (the
`AspectRatioFrameLayout` that `resizeMode` needs, the shutter, the `SubtitleView`) and drops the
placeholder along with media3's own buffering spinner — at 6.2 ms the single most expensive child, and a
second spinner competing with the app's own `BufferingIndicator` — plus its error `TextView`, the
artwork/image views and the ad/overlay frames, none of which this app enables.

| | before | after |
|---|---|---|
| `inflate` total, player open | 60.4 ms | **14.3 ms** |
| `ProgressBar` / `RecyclerView` / `HorizontalScrollView` slices | 6.2 / 3.4 / 2.3 ms | **gone** |

**Multi-View is where this compounds** — the controller was inflated once *per pane*, so a four-up grid
paid it four times on open.

### `tvFocusable` rebuilt two modifier elements on every focus and press change

Same reference-identity mechanism Round 6 found in `softShadow`, in the modifier that every focusable in
the app composes. `Modifier.graphicsLayer { }` and `Modifier.drawWithCache { }` hold their block as a
lambda and compare it by **identity**, so an unremembered capturing lambda replaces the node on every
call — and for `drawWithCache`, re-runs the cache block (`shape.createOutline` + a `Stroke` alloc).
`tvFocusable` reads focused/pressed state in composition, so it recomposes twice per D-pad step, on the
element losing focus *and* the one gaining it, across every focusable on screen.

Both are now `remember`ed **on the `State` objects rather than their values**, which keeps the deferred
draw-time reads from Round 2 intact — the tweens still animate without recomposing. `glassWell` had the
same bug, which silently defeated the comment directly above it.

Honest scoring: this is composition-side and lands below the noise floor of the harnesses here. It is
directionally right, free and pixel-identical, **not a measured win**.

### Where the app stands now — XL95, the user's 9020-channel catalogue, English/LTR, `speed-profile` AOT

| journey | frames | frames > 16.7 ms | worst frame | dominant cost in it |
|---|---|---|---|---|
| **Home**, D-pad sweep (down/up + rail) | 181 | **0** | 8 ms | — |
| **Settings** first paint | — | — | 83–90 ms | `draw` 32 ms ≈ `measure` 31 ms |
| **Guide** first paint | 101 | 4 | 64 ms | `measureAndLayout` 39.5 ms |
| cold start (`am start -W` TotalTime, ×3) | — | — | 346 / 456 / 367 ms | — |

**Home is finished** — 181 frames, zero over budget, worst frame 8 ms on a 16.7 ms budget. Settings is no
longer measure-bound: it is now an even draw/measure split, so the next lever there is glass
rasterisation, not subcomposition. The Guide's first paint is the last clearly-jank surface.

### Refuted this round

- **Persian is not slower than English.** The standing report was that Settings is terrible in Persian.
  Measured with the locale flipped between interleaved runs on one build: worst frame **en 30/30 ms vs
  fa 27 ms**, composition en 55/60 ms vs fa 39 ms. Vazirmatn shapes no slower than Manrope here.
  Whatever the user is seeing on that screen, RTL/complex-script text measurement is not it.
- **Round 6's structural claim about Settings' panes is wrong.** It reads: "Settings' `LazyColumn` emits
  a whole section *card* per item, so entering a pane composes every row in it regardless of
  visibility." `GeneralPane` splits into **7** `item {}`s (playlists / preferences / provider ×2 /
  language / metadata / storage), and the 33 text measures in its first-paint frame are the ~5 rows that
  are genuinely on screen, not off-screen waste. There is no free structural win there.
- **The composition layer has no skippability wins left.** Compose compiler metrics over the whole
  module: **190 of 190 restartable composables are skippable**, zero exceptions. (Round 6 said "0
  non-skippable"; this confirms it against a changed codebase.) The 45 non-restartable ones are
  inline/lambda/return-value composables, which is correct.
- **Coil is already correctly configured** — explicit `size()` on every request, `crossfade(false)`,
  tuned memory/disk caches. No unsized decode anywhere.

### Not taken: the 3400 ms splash floor

`SPLASH_FLOOR_MS = 3400` holds the splash for 3.4 s. The reveal choreography is a 1900 ms tween and
`am start -W` reports the Activity up in **346–456 ms**, so ~1.5 s of every cold start is deliberate
dwell on a finished animation over ready data. Cutting it to 1900 ms is by far the largest
perceived-performance win still available in the app.

**Asked and declined — the product call is to keep the brand moment.** Recorded here so nobody
re-derives it as a bug: it is a decision, and the 1.5 s is its known price.

### Method notes

- **The TV screensaver silently invalidates traces, and it will happen to you.** A capture that looks
  perfectly well-formed can contain Google TV's ambient screensaver instead of the app, because the
  harness's settle windows are long enough to trigger it. It produced a *plausible* "244 ms → 47 ms"
  before the screenshot showed the app was never on screen. Disable it first:
  ```
  adb shell settings put secure screensaver_enabled 0
  adb shell settings put secure sleep_timeout -1
  ```
- **Screenshot every trace and look at it before believing a number.** Two of this round's captures were
  invalid — one screensaver, one that had landed in Home's *edit* mode rather than Home — and in both
  cases the numbers looked like good news. `set.sh`/`j.sh` in the scratchpad now `screencap` at the end
  of every run for this reason.
- **`adb` CAN type into `AreTextField`, and `TAB` is how you leave one.** The older note that
  `input text` never reaches these fields is wrong for the current build — text lands fine. What does
  not work is D-pad focus travel *between* fields: `DPAD_DOWN` is swallowed as cursor movement, and
  `BACK` propagates past edit mode and navigates the whole step away (losing what was typed).
  `input keyevent 61` (TAB) moves focus and is the only way through a multi-field form from adb.
  **This is a real D-pad bug worth chasing** — a remote has no TAB key, and Onboarding's Credentials
  step is the first screen a new user meets.
- The splash mark was a 320×405 webp in `drawable-xxxhdpi` (an 80 dp natural size) drawn at **132 dp** —
  a 1.65× upscale, which is why it read as soft while the 32 dp sidebar mark off the same asset looked
  fine. Both glyph layers are now VectorDrawables traced from the alpha channel (marching squares →
  circular box-smooth → Douglas-Peucker, `fillType="evenOdd"`, IoU 0.965/0.934 against the source).
  A density-qualified resource beats a default-config one, so the `.webp` files had to be **deleted** —
  left in place they would have kept winning and the vector would never have been used.
