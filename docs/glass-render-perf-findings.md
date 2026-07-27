# Glass render performance — findings and decisions

_Investigated 2026-07-27, on the TV emulator (`emulator-5554`, API 36), against the Settings
D-pad scroll. Read this before optimizing anything about how this app renders._

## The one thing to take away

**This app's scrolling is draw-bound, not recomposition-bound.** Measured per frame during a
Settings D-pad sweep:

| phase | time |
|---|---|
| composition + measure + layout | **0.04 ms** |
| draw recording | 0.92 ms |
| issuing draw commands (RenderThread) | **7.44 ms** |
| GPU (see caveat below) | ~13 ms |
| **total** | **27.13 ms** |

Compose accounts for about half a percent of the CPU work. Several earlier passes went after
recomposition — unused parameters blocking skipping, effect keys that were secretly root-scope
state reads, unstable `Set`/`List` params, memoizing brushes. Those changes were individually
correct and are worth keeping, but they were aimed at the 0.04 ms. **They could not have fixed
the reported jank, and neither can further work of that kind.**

If someone reports scroll or focus jank again, profile before theorizing. The cheap, decisive
check is below.

## How to reproduce the measurement

```bash
export ANDROID_SERIAL=emulator-5554
adb shell dumpsys gfxinfo com.arashrahimi46.iptv reset
# ...perform the interaction (e.g. 14x KEYCODE_DPAD_DOWN with ~450ms between presses)...
adb shell dumpsys gfxinfo com.arashrahimi46.iptv framestats
```

`framestats` emits one CSV row per frame under `---PROFILEDATA---`. The columns that matter
(0-indexed): `IntendedVsync` 2, `PerformTraversalsStart` 7, `DrawStart` 8, `SyncQueued` 13,
`IssueDrawCommandsStart` 15, `SwapBuffers` 16, `FrameCompleted` 17, `GpuCompleted` 20,
`CommandSubmissionCompleted` 23. So:

- composition + measure + layout = `DrawStart - PerformTraversalsStart`
- draw recording = `SyncQueued - DrawStart`
- **RenderThread = `SwapBuffers - IssueDrawCommandsStart`** ← the number that matters
- total = `FrameCompleted - IntendedVsync`

## What was fixed

Three draw-time costs, all of them work that was being redone every frame to produce an image
that never changed. Each is verified visually identical by direct pixel diff.

| change | what it was doing | commit |
|---|---|---|
| Ambient mesh baked to a bitmap | 2 viewport-sized radial gradients + veil + vignette, re-evaluated per frame — and drawn **twice**, because `layerBackdrop` captures the same subtree | `fa8a826` |
| `softShadow` baked to a bitmap | `drawPath` regenerating a Gaussian mask every frame; ~1.7 ms of RenderThread on its own | `fa8a826` |
| `tvGlow` moved to `drawWithCache` | allocating a `Path`, `NativePaint` and `BlurMaskFilter` on **every draw pass** | `cb1235a` |

Both bakes render at half resolution and upscale. That is lossless in practice because both
outputs are band-limited by construction — wide gradients and a blur fringe carry no detail a
half-res buffer can hold. Pixel diff of the Settings screen: 1.3% of pixels differ, all by at
most 2/255, under the quantisation floor of a shadow drawn at 10% alpha.

Two traps worth remembering, because both would have made things **worse**:

- `drawWithCache` re-runs when the modifier is recreated. Baking the mesh inline in
  `AmbientBackdrop` meant a fresh multi-MB allocation on every artwork change — i.e. every D-pad
  step on a browse screen. It lives in its own skippable composable (`AmbientMesh`, stable `Color`
  params only) so artwork changes can't touch it.
- `drawWithCache` also re-runs on **size** change. The expanding sidebar is remeasured every frame
  by `widthFrom`, so baking its shadow would allocate and rasterize per frame. `softShadow` takes
  a `bake: Boolean` opt-out for exactly this; `frostedPanel` and `glassSurface(sheer = true)` pass
  `false`. **Any future size-animating glass surface must do the same.**

## How much it actually helped: not much

| build | RenderThread | frame total | effective fps |
|---|---|---|---|
| before | 7.44 ms | 27.13 ms | ~37 |
| after the three fixes | 5.25 ms | 24.92 ms | ~40 |
| _hypothetical:_ backdrop removed | 1.33 ms | 17.58 ms | ~57 |

RenderThread dropped 30%, but wall-clock only moved ~2 ms — about 3 fps, which is below what
anyone perceives. **The work removed real waste and is worth keeping, but it did not make the
scroll feel different on the emulator.** Recorded plainly here so nobody re-measures expecting a
dramatic result.

The 30% RenderThread cut should matter more on the Sony XL95 than it does here, because
RenderThread is CPU work and that SoC is much slower — but that is an expectation, **not
something that has been measured**. See the open items.

## Locked decisions

| decision | status | why |
|---|---|---|
| **Keep the glass backdrop system** | **Locked 2026-07-27** | It is the largest remaining draw cost (~4 ms of RenderThread, every frame, every screen), but removing it is a real visual change and the win isn't big enough to pay for it. See below. |
| Ambient mesh stays static (`t = 0`) | Locked | The bake depends on it. Re-introducing drift would invalidate the cached bitmap every frame and undo the fix. |
| `vibrancy()` stays | Locked | Measured at ~0.5 ms, inside run-to-run noise. Not worth a visual change. |

### Why the backdrop was kept

The hypothesis was that since only the ambient wash sits *behind* a glass card, sampling that
wash and redrawing it should be a no-op. **That was tested and it is false** — the theme also
retunes token alphas when backdrop blur is active (`Theme.kt`, `withBlurredBackdrop()`), so
removing it changes the surfaces themselves. Pixel diff, backdrop on vs off:

| screen | pixels differing | max channel delta |
|---|---|---|
| Settings | 95% | 103/255 |
| Home | 11% | 90/255 |
| Expanded sidebar | **0%** | **0** |

Cards flatten toward a plain tint — a loss of depth rather than a different design, but real and
app-wide. Against ~4 ms and an unproven perceptual benefit, that trade was declined.

The sidebar result is the useful one: the expanded panel frosts through `LocalPageBackdrop`, a
**separate** layer that samples page content, so it is untouched by the ambient backdrop either
way. The app's one genuinely visible blur — and the one most worth its cost — is not part of this
trade-off.

## The emulator cannot answer absolute questions

It runs an OpenGL ES → Metal translator on the host (`ro.hardware.egl = emulation`), so the
`GpuCompleted` timestamp is dominated by host round-trip latency rather than real GPU work. In
practice:

- **Trustworthy:** RenderThread CPU (`issue draw commands`). Stable, and it transfers to real
  hardware.
- **Noisy:** GPU time. Per-frame minimum ranged 1.1–5.2 ms across runs of the *same* build.
- **Not comparable to a TV:** absolute frame time. Even with the entire backdrop system disabled
  the emulator still sat at 17.6 ms/frame, so it cannot demonstrate 60 fps for this screen under
  any configuration.

Run-to-run variance on total frame time is roughly ±1.5 ms. Treat smaller differences as noise.

## Open items

- **Input latency is unexamined and is the better lead.** `gfxinfo` counted **241 high
  input-latency events** across a 14-keypress sweep. That is a different failure from a low frame
  rate, and it matches the original report ("it goes down with a glitch, with some delays") better
  than 37-vs-40 fps does. Nothing here addressed it. Suspects not yet looked at: the LazyColumn's
  focus-driven `bringIntoView` scroll animation, and D-pad autorepeat handling.
- **Nothing has been profiled on the actual Sony XL95.** Everything above is emulator data. The
  decisive test is the same scripted sweep over network `adb` against the TV.
- **Known composition-level issues, deliberately not fixed.** An audit found unstable `Iterable`
  params on `ChipChoiceRow` (plus `values().asIterable()` allocated per recomposition at four call
  sites), `animateDpAsState` read in composition scope in `AreSwitch`, and a `focusRequester`
  element toggling structurally mid-animation in `AreSegmentedControl`. All real; all in the
  0.04 ms. Fix them if they cause a correctness or jank problem that is *measured*, not on
  principle.
## Two corrections to an earlier version of this document

**A baseline profile _is_ shipped.** An earlier draft here claimed it wasn't, on the grounds that
`tv/src/release/generated/` doesn't exist. That was wrong — the `baselineprofile` module generates
it at build time, and `tv-release.apk` contains `assets/dexopt/baseline.prof`: 6367 rules, 1123 of
them in `ui/`, composables included. Verified by unzipping the APK.

**But `adb install` does not apply it.** A sideloaded install leaves ART at `status=verify` — no
AOT compilation, profile unused. Play Store installs apply it; sideloads don't. So **any on-device
perf impression from a sideloaded build is of an uncompiled app.** After installing, run:

```bash
adb shell cmd package compile -m speed-profile -f com.arashrahimi46.iptv
adb shell dumpsys package dexopt | grep -A2 com.arashrahimi46.iptv   # want status=speed-profile
```

Measured effect (emulator, 4 cold starts each): `verify` ~118 ms → `speed-profile` ~103 ms, about
12% off cold start. It made **no** measurable difference to the steady-state scroll (17.03 ms vs
17.43 ms, inside noise) — which is expected, because a baseline profile buys cold start and the
first run through a path, not sustained scrolling after JIT has warmed up.

## Debug vs release: the numbers above are all DEBUG builds

Every measurement in this document was taken on `:tv:assembleDebug`. The release build of the same
commit runs the same Settings scroll at **17.0 ms/frame against debug's 24.9 ms**. Debug builds
carry non-trivial overhead, so this document overstates the absolute problem — but the relative
comparisons in it are all debug-vs-debug and remain valid.

Practical consequence: profile release builds when you want to know how the app actually behaves,
and force `speed-profile` afterwards. Profiling a sideloaded debug build measures two layers of
overhead the user will never see.
