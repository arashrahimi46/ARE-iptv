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

Fix: gate the capture in **time** as well as in state. No capture while the width is moving; capture
once the tween settles (`durBaseMs + 32 ms`). 28 dp of blur is not readable on a surface travelling
160 dp in 220 ms, and the settled panel is untouched.

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

## Locked decisions

| decision | status | why |
|---|---|---|
| Sample the backdrop only where an effect runs | **Locked 2026-07-28** | Worth 9.5 ms of RenderThread on the XL95 at no visual cost. Measured, pixel-diffed. |
| Keep `AmbientBackdrop` + the Tier B token retune | Locked | The retune *is* the glass look; only the redundant per-surface sampling was removed. |
| Ambient mesh stays static (`t = 0`) | Locked | The bake depends on it. Drift would invalidate the cached bitmap every frame. |
| ~~Keep the glass backdrop system~~ | **Superseded 2026-07-28** | Locked on emulator data that understated the cost ~4×, and on a test that conflated the token retune with the sampling. |

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
