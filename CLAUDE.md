# Claude Code Configuration - Monomind

## Behavioral Rules (Always Enforced)

- Do what has been asked; nothing more, nothing less
- NEVER create files unless they're absolutely necessary for achieving your goal
- ALWAYS prefer editing an existing file to creating a new one
- NEVER proactively create documentation files (*.md) or README files unless explicitly requested
- NEVER save working files, text/mds, or tests to the root folder
- Never continuously check status after spawning a swarm — wait for results
- ALWAYS read a file before editing it
- NEVER commit secrets, credentials, or .env files
- ALWAYS call `mcp__monomind__monograph_query` BEFORE running grep/rg/find via Bash for code exploration — only fall back to Bash grep if monograph returns 0 results or the DB does not exist
- When starting any task that touches 3+ files: call `mcp__monomind__monograph_suggest` first to get relevant nodes ranked by task relevance

## Coding Principles

### Think Before Coding
- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### Simplicity First
- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

### Surgical Changes
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.
- Every changed line should trace directly to the user's request.

### Goal-Driven Execution
- Transform tasks into verifiable goals with success criteria.
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- For multi-step tasks, state a brief plan with verification steps.

## File Organization

- NEVER save to root folder — use the directories below
- Use `/src` for source code files
- Use `/tests` for test files
- Use `/docs` for documentation and markdown files
- Use `/config` for configuration files
- Use `/scripts` for utility scripts
- Use `/examples` for example code

## Project Architecture

- Follow Domain-Driven Design with bounded contexts
- Keep files under 500 lines
- Use typed interfaces for all public APIs
- Prefer TDD London School (mock-first) for new code
- Use event sourcing for state changes
- Ensure input validation at system boundaries

### Project Config

- **Topology**: hierarchical-mesh
- **Max Agents**: 15
- **Memory**: hybrid
- **HNSW**: Enabled
- **Neural**: Enabled

## Build & Test

```bash
# Build
npm run build

# Test
npm test

# Lint
npm run lint
```

- ALWAYS run tests after making code changes
- ALWAYS verify build succeeds before committing

## Security Rules

- NEVER hardcode API keys, secrets, or credentials in source files
- NEVER commit .env files or any file containing secrets
- Always validate user input at system boundaries
- Always sanitize file paths to prevent directory traversal
- Run `npx monomind@latest security scan` after security-related changes

## Concurrency: 1 MESSAGE = ALL RELATED OPERATIONS

- All operations MUST be concurrent/parallel in a single message
- Use Claude Code's Task tool for spawning agents, not just MCP
- ALWAYS batch ALL todos in ONE TodoWrite call (5-10+ minimum)
- ALWAYS spawn ALL agents in ONE message with full instructions via Task tool
- ALWAYS batch ALL file reads/writes/edits in ONE message
- ALWAYS batch ALL Bash commands in ONE message

## Swarm Orchestration

- MUST initialize the swarm using CLI tools when starting complex tasks
- MUST spawn concurrent agents using Claude Code's Task tool
- Never use CLI tools alone for execution — Task tool agents do the actual work
- MUST call CLI tools AND Task tool in ONE message for complex work

## Swarm Configuration & Anti-Drift

- ALWAYS use hierarchical topology for coding swarms
- Keep maxAgents at 6-8 for tight coordination
- Use specialized strategy for clear role boundaries
- Use `raft` consensus for hive-mind (leader maintains authoritative state)
- Run frequent checkpoints via `post-task` hooks
- Keep shared memory namespace for all agents

```bash
npx monomind@latest swarm init --topology hierarchical --max-agents 8 --strategy specialized
```

## Swarm Execution Rules

- ALWAYS use `run_in_background: true` for all agent Task calls
- ALWAYS put ALL agent Task calls in ONE message for parallel execution
- After spawning, STOP — do NOT add more tool calls or check status
- Never poll TaskOutput or check swarm status — trust agents to return
- When agent results arrive, review ALL results before proceeding

## CLI Commands

### Core Commands

| Command | Subcommands | Description |
|---------|-------------|-------------|
| `init` | 5 | Project initialization |
| `agent` | 7 | Agent lifecycle management |
| `swarm` | 6 | Multi-agent swarm coordination |
| `memory` | 12 | LanceDB memory with ANN search |
| `task` | 5 | Task creation and lifecycle |
| `session` | 6 | Session state management |
| `hooks` | 29 | Self-learning hooks + 15 background workers _(unavailable in this install)_ |

> Note: there is no `hive-mind` or `neural` CLI command. Hive-mind
> consensus (byzantine/raft/quorum) is available exclusively via MCP tools
> (`hive-mind_*`), not the CLI. Neural pattern learning was merged into
> `hooks intelligence`.

### Quick CLI Examples

```bash
npx monomind@latest init --wizard
npx monomind@latest agent spawn -t coder --name my-coder
npx monomind@latest swarm init --v1-mode
npx monomind@latest memory search --query "authentication patterns"
npx monomind@latest doctor --fix
```

## Available Agents (60+ Types)

### Core Development
`coder`, `reviewer`, `tester`, `planner`, `researcher`

### Specialized
`security-architect`, `security-auditor`, `memory-specialist`, `performance-engineer`

### Swarm Coordination
`hierarchical-coordinator`, `mesh-coordinator`, `adaptive-coordinator`

### GitHub & Repository
`pr-manager`, `code-review-swarm`, `issue-tracker`, `release-manager`

## Memory Commands Reference

```bash
# Store (REQUIRED: --key, --value; OPTIONAL: --namespace, --ttl, --tags)
npx monomind@latest memory store --key "pattern-auth" --value "JWT with refresh" --namespace patterns

# Search (REQUIRED: --query; OPTIONAL: --namespace, --limit, --threshold)
npx monomind@latest memory search --query "authentication patterns"

# List (OPTIONAL: --namespace, --limit)
npx monomind@latest memory list --namespace patterns --limit 10

# Retrieve (REQUIRED: --key; OPTIONAL: --namespace)
npx monomind@latest memory retrieve --key "pattern-auth" --namespace patterns
```

## Second Brain — Document Knowledge Base

If the `documents` capability is active (check `.monomind/capabilities.json`), this project indexes documents (PDF, DOCX, MD, TXT) into a semantic search engine.

**When documents are indexed, search knowledge before answering questions about business, compliance, legal, or organizational topics:**
- Call `mcp__monomind__knowledge_search` with a relevant query
- Use the returned excerpts as grounding context for your answer
- Cite the source document name when referencing specific information

**CLI access:**
```bash
monomind doc search -q "your query"    # Semantic search (project + global brain merged)
monomind doc search -q "..." --store global   # Personal global brain only
monomind doc list                       # List indexed docs (--global for the global brain)
monomind doc ingest ./path              # Ingest new documents (paths outside the project auto-route to the global brain)
monomind doc export                     # Export as OKF bundle (--global to move your brain between machines)
```

**Global brain:** the user has a personal cross-project knowledge store at `~/.monomind/global-brain`. All searches (knowledge_search, doc search, per-prompt injection) automatically merge it with project knowledge — project results win ties, global hits are labeled `[global]`. Cite the label so the user knows which brain answered.

**Re-indexing** happens automatically on session start (unchanged files are skipped via content hash).

## Knowledge Graph — Monograph (Use Before Codebase Exploration)

Built into monomind — no separate install. Pure TypeScript, parses TS/JS/Python/Go/Rust/C/C++/Java/Ruby/Swift into a SQLite graph with BM25 full-text search.

### MANDATORY: Graph-First, Grep-Last

**Before ANY grep/rg/find via Bash for code navigation:**
1. Call `mcp__monomind__monograph_query` first — returns file path + line number
2. Only fall back to Bash grep if monograph returns 0 results or reports DB missing

**When starting any task touching 3+ files:**
1. `mcp__monomind__monograph_suggest` — relevant nodes ranked by task description
2. `mcp__monomind__monograph_context` — 360° view of a symbol (callers, callees, imports)
3. `mcp__monomind__monograph_impact` — blast radius before changing anything

**If graph is empty:** call `mcp__monomind__monograph_build` (runs in background; proceed with grep while it builds).

### Available Tools (prefix: `mcp__monomind__`)

| Tool | Use when |
|------|----------|
| `monograph_suggest` | **Start every multi-file task** — ranked by task relevance |
| `monograph_query` | **Primary code lookup** — BM25 search, returns file + line |
| `monograph_context` | 360° symbol view: callers, callees, imports, community |
| `monograph_impact` | Blast radius before a change — transitive callers + risk score |
| `monograph_build` | Build/rebuild the index (codeOnly:true for code-only) |
| `monograph_god_nodes` | High-centrality files — find the most connected internal nodes |
| `monograph_detect_changes` | Git diff → affected symbols since base branch |
| `monograph_rename` | Dry-run multi-file rename — all reference sites, never writes |
| `monograph_route_map` | List all HTTP routes with handler info |
| `monograph_api_impact` | Blast radius of an API route |
| `monograph_cypher` | Single-hop MATCH query over the graph |
| `monograph_staleness` | Git commits since last index build |
| `monograph_stats` | Node/edge/community counts |
| `monograph_health` | Index freshness vs current HEAD |
| `monograph_shortest_path` | Shortest dependency path between two symbols |
| `monograph_community` | All nodes in a community cluster |
| `monograph_export` | Export graph: json, svg, graphml, cypher, obsidian |
| `monograph_augment` | Graph-RAG context block for AI prompts |
| `monograph_doctor` | Platform diagnostics (Node version, DB health) |
| `monograph_list_repos` | Global registry of indexed repos |

### Skip monograph for
Single-file edits, doc/config changes, quick fixes where you already know the exact file.

## Quick Setup

```bash
# Add MCP server — includes monograph, swarm, memory, hooks, all 200+ tools
claude mcp add monomind -- npx -y monomind@latest mcp start

# Verify everything works
npx monomind@latest doctor --fix
```

> **Package name changed:** Use `monomind@latest` (not `@monomind/cli@latest` which is the old name and returns 404).

## Claude Code vs CLI Tools

- Claude Code's Task tool handles ALL execution: agents, file ops, code generation, git
- CLI tools handle coordination via Bash: swarm init, memory, hooks, routing
- NEVER use CLI tools as a substitute for Task tool agents

## Support

- Documentation: https://github.com/monoes/monomind
- Issues: https://github.com/monoes/monomind/issues

---

# This Repository — are-iptv (Android TV app)

> The generic guidance above is the monomind baseline. The rules below describe THIS actual
> project and override the generic build/test/architecture notes where they conflict.
> This is a **Kotlin + Jetpack Compose for TV** app built with **Gradle** — NOT a Node/npm project,
> so `npm run build`/`npm test` do not apply here.

## What it is

Android **TV** IPTV player. Plays live channels, movies and series from M3U / Xtream playlists,
with EPG, favorites, continue-watching, online subtitles, a customizable Home, and light/dark
theming. App package: `com.arashrahimi46.iptv`.

## Modules

| Module | What it is |
|--------|------------|
| `:tv` | **The app** — nearly all work happens here |
| `:mobile` | The phone app — its own complete, independent source tree |
| `:core` | **Strings only.** 24 `values*/strings.xml` files. No Kotlin, ever. |
| `:baselineprofile` | Startup baseline profile generation |

Layout under `tv/src/main/java/com/arashrahimi46/iptv/`: `data/` (db=Room, repository, parser,
settings=DataStore, model) · `ui/` (one package per screen + `ui/components` + `ui/theme`).
`:mobile` mirrors that layout with its own copy, plus a `mobile/` package for phone-only screens.

### `:tv` and `:mobile` share NOTHING but strings — do not "fix" this

**The only shared module is `:core`, and it contains string resources and nothing else.** No
Kotlin, no Compose, no dependencies, an empty manifest. `:tv` and `:mobile` each own a full,
independent copy of the data layer, the `Are*` components, and the theme. Yes, that is duplicated
code. It is deliberate.

It was tried the other way. Between `a5a7034` and `417fc29` (2026-07-29/30) `:core` held the shared
data layer *and* the whole `Are*` rendering/interaction layer behind a per-platform
`LocalAreInteractiveBinding`. The coupling did not pay for itself: every `:mobile` change had to be
re-verified on `:tv`, and a run of real TV regressions arrived through exactly that seam —

- focus-ring geometry, four separate times (`b23cea1`, `c806da6`, `9d147f3`, `417fc29`), ending in
  a bug where the 1.06× focus scale sat *above* the focus target, so every focus move in every
  scrollable nudged the page a few px;
- `AreSegmentedControl` selection breaking, then its track clipping (`917a44c`, `4014aa3`);
- `AreDialog` scrolling the whole card so opening a modal stranded D-pad focus (`101a487`);
- a hard crash that made Settings **unreachable** on `:tv` (`167be2e`).

The unwind is `refactor: unwind the shared-code :core` — `tv/` was restored to `6d3d9cc`, the
commit immediately before the extraction started, verified byte-identical apart from
`import ...iptv.R` → `import ...iptv.core.R`.

Rules that follow from this:

- **Never move Kotlin into `:core`.** If `:tv` and `:mobile` need the same logic, copy it. A phone
  app and a D-pad TV app have different interaction models; a shared component grows a platform
  switch, and the switch is where the TV bugs come from.
- Adding a user-facing string still means **one** edit in `core/src/main/res/values*/strings.xml`
  (all 24 locales — see the i18n section below, which still applies verbatim, just at that path).
- Consumers reference strings as `com.arashrahimi46.iptv.core.R.string.*` (AGP's non-transitive R
  class), aliased to `CoreR` in the few files that also use their own module's `R.drawable`/`R.font`.
- A `:tv` change needs `:tv` verification only. That is the entire point.

## Build / test / run (Gradle, JDK 21)

- **JDK 21 required.** `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`
  (also in `.claude/settings.json`). `/usr/bin/java` is a stub — don't use it.
- Compile check: `./gradlew :tv:compileDebugKotlin`
- Debug APK: `./gradlew :tv:assembleDebug` — also validates all string resources (aapt)
- Release APK: `./gradlew :tv:assembleRelease` → `tv/build/outputs/apk/release/tv-release.apk`
- Unit tests: `./gradlew :tv:testDebugUnitTest` (JUnit, in `tv/src/test`)
- Lint: `./gradlew :tv:lintDebug`
- e2e: TV emulator `emulator-5554`; `adb install -r` the debug APK, launch, screenshot to verify.
- Never commit build outputs (`**/build/`), `.idea/`, or `.kotlin/`.
- **Release:** bump `versionCode` AND `versionName` in `tv/build.gradle.kts`. Signing uses the
  keystore only if `TV_RELEASE_KEYSTORE_PATH`/`TV_RELEASE_KEYSTORE_PASSWORD` (+ key alias/pass)
  are set, else falls back to debug signing.

## Internationalization — READ BEFORE ADDING ANY USER-FACING STRING

Ships **English + 23 translated locales**. A missing translation silently falls back to English
(no build error), so keep them in sync by hand.

**All strings live in `core/src/main/res/`** — that is the whole of the `:core` module, and it is
the one thing `:tv` and `:mobile` share (see the Modules section). Adding a key there covers both
apps at once; there is no per-app strings file to keep in step.

- Every user-facing string goes through `stringResource(CoreR.string.…)` — never a hardcoded
  literal in `Text(...)` / `contentDescription` (glyphs, counters, and the brand mark excepted).
- When you add/rename a key, add it to **`core/…/values/strings.xml` AND all 23
  `core/…/values-*/strings.xml`**: `ar, az, b+pt+BR, b+pt+PT, bg, cs, da, de, el, es, fa, fi, fr,
  hu, it, nb, nl, pl, ro, ru, sv, tr, uk`.
- **`fa` and `ar` are RTL and are the two everyone forgets.** They ship like the rest —
  `android:supportsRtl="true"`, no `locales_config.xml`, no `resourceConfigurations` filter. This
  doc used to say "21 locales" and omit them; contributors followed that list literally and left
  fa/ar **27 keys behind** the other 22 (the whole `hud_ctl_*` / `hud_editor_*` /
  `settings_sidebar_style*` / `player_playback_speed` wave). Count 23 or don't count.
- Preserve positional format args exactly (`%1$s`, `%1$d`, `%2$d`); a locale may reorder them but
  every index must still appear. Escape `'` as `\'`; XML-escape `&`/`<`/`>`.
- Audit gaps: diff each `values-*/strings.xml`'s `name="…"` keys against `values/strings.xml`.
- **Before touching any locale file, read `docs/translation-guide.md`** — the quality bar, the
  per-language traps, and the validation script live there.

## Xtream providers — NEVER curl the stream/playlist for probing

- **Do NOT `curl`/`wget` the provider's stream, `.m3u8`, `.ts`, or `player_api` URLs to "test" them.**
  Xtream servers rate-limit by IP: a stray non-player connection makes the provider **block any new
  connection for several minutes**, which **stops the live stream inside the app**. Verify streams
  on-device (ExoPlayer sends a player User-Agent and is what actually matters), never from the shell.

## TV UX conventions (do not regress)

- **Focus:** every focusable composes `TvFocusable` / `Modifier.tvFocusable` (accent ring + glow).
  Nothing focusable without a visible indicator. `TvFocusable` already handles remote `DPAD_CENTER`.
- **Dialogs must trap focus:** render modals in a real `Dialog(...)` window (see
  `HomeAddSectionDialog`), never inline — inline overlays let D-pad focus leak to the content
  behind. Focus the default action on open.
- **Text inputs in scrolling lists:** `AreTextField(activateOnClick = true)` so D-pad scrolling
  past a field doesn't pop the IME (OK enters edit, Back/Done exits). Leave off for Search.
- **Tile size is a per-screen decision, NOT the `tilePosterWidth`/`tileLandWidth` tokens.** Those
  tokens (208dp / 320dp) are Home-rail sizes. A 2:3 poster at 208dp is **312dp tall**, and with its
  title+meta the item is ~356dp — on a 540dp-tall 1080p screen that is most of the viewport, so on
  any screen with chrome above the grid the label gets cut off mid-glyph. Favorites and Search both
  shipped that bug. Browse renders the *same* content through `GridCells.Adaptive(115.dp)`
  (movies/series) and `(180.dp)` (live) — **Browse's density is the baseline; the tokens were the
  outlier.** New grid screens pass an explicit `width` to `ArePosterTile`/`AreChannelTile` sized
  against their own vertical budget: `540dp − (root insets + any header/tabs/section label) − 52dp
  bottom contentPadding`. Current values: `FavoritePosterWidth = 150dp`,
  `SearchPosterWidth = 120dp`, `SearchChannelWidth = 180dp` (Search has the tallest chrome stack —
  query field *and* scope strip — so it gets the smallest tiles).
- **Every tile grid needs `contentPadding`, not just `Modifier.padding`.** `ArePosterTile` draws its
  title/meta *below* the focusable poster and the focusable scales 1.06x on focus, so a flush
  viewport edge clips both. Use `PaddingValues(top = 10.dp, bottom = 52.dp)` as BrowseLayout does.
  Don't compensate with a large root `bottom` inset instead — that shrinks the scrollable viewport
  rather than extending it, which is the opposite of the fix.

## Theming

- Two themes (`AreIptvDarkColors` / `AreIptvLightColors`) via semantic tokens in `ui/theme/Color.kt`
  (`AreIptvColors`). Reference **semantic** aliases (`colors.surface1`, `colors.textPrimary`,
  `colors.borderDefault`) — never the raw `Ink*/Blue*/Light*` ramps.
- Light-theme gotchas: near-white surfaces on the off-white page need a `borderDefault` edge to
  read; clip tile content to the tile shape so square fills don't poke through rounded focus rings;
  glass/HUD icons use `textPrimary`, not a hardcoded white.

## Commits

Conventional commits scoped to the module: `fix(tv):`, `feat(tv):`, `release(tv):`.

**Work directly on `main`. Do NOT create feature branches** unless the user explicitly asks for
one — commit straight to `main`. (Overrides the generic "branch first" default above.)

## Design specs (`docs/`)

Agreed-but-not-yet-built feature designs live in `docs/*-v1-design.md` (locked-decisions table,
resilience/variance notes, component/file map, phasing). Read the relevant one BEFORE building that
feature — decisions there were made deliberately; don't relitigate them.

- `docs/glass-redesign-v1-design.md` — **Glass Design** initiative: one translucent "glass" surface
  language across the whole app (HUD, sidebar, buttons, cards, tabs, badges, Settings, dialogs). Surface/
  material pass — not a re-layout. Companion visual pitch + build sheet: `docs/glass-redesign-v1.html`. Not
  yet implemented; HUD layout + tab style are open decisions.
- `docs/recording-v1-design.md` — Live TV recording (record-now, tee-based). **Implemented.**
- `docs/stalker-portal-v1-design.md` — **Stalker Portal / Ministra** as a third source type
  (live + VOD + series). Not yet built; sequenced behind catch-up. Key shape: a stateful
  `StalkerClient` (handshake→token→get_profile), **resolve-on-play** via a `StreamUrlResolver` seam
  (`create_link` mints session-scoped URLs — they can't be precomputed like Xtream's), MAC stored in
  `CredentialsStore`, `cmd` in `Channel.externalId`, **no DB migration** (schema stays v11).
  Visual one-pager: https://claude.ai/code/artifact/d34083e0-3255-41ce-8189-dbb84e9c237c

## Competitive position (`docs/feature-matrix-2026-07-29.html`)

Code-verified feature audit of the app benchmarked against 10 competing IPTV players, dated
2026-07-29. Open it in a browser, or read it as an artifact:
https://claude.ai/code/artifact/a688611e-a2ba-480f-a7f9-0c71c80cd2ea

It supersedes `docs/competitive-analysis-2026.md` wherever the two conflict — catch-up, timeshift,
recording and Stalker have all shipped since that doc was written. Two entries above are stale
against it: **Stalker Portal is fully shipped** (all 5 phases, not "not yet built") and the **glass
redesign shipped app-wide** (~27 files, device-capability-tiered, not "design only"). The matrix
also flags that the in-app corner player is UI-labeled "Picture-in-Picture" but is *not* OS PiP.

## Rendering performance — READ BEFORE OPTIMIZING ANY JANK

`docs/glass-render-perf-findings.md` — profiled 2026-07-27 with `dumpsys gfxinfo framestats`,
corrected 2026-07-28 with a real Perfetto trace on the XL95.

**framestats cannot see Compose's recompose/measure/layout — do not conclude "not
recomposition-bound" from it.** Its composition+measure+layout column starts at
`PerformTraversalsStart`, but Compose schedules its own pass in the Choreographer **ANIMATION**
callback, which Android runs *before* traversal. That is the only reason the column reads ~0.04ms.
A Perfetto trace of a Home D-pad sweep found one 66.6ms frame spending
`animation(59ms) → Recomposer:animation(55ms) → AndroidOwner:measureAndLayout(53ms)` — all of it
invisible to framestats. Per newly-visible tile: ~3× `TextAnnotatedStringNode:measure` (1–1.5ms
each) + `Compose:recompose` (2.5–4ms) + `Compose:applyChanges` (2–3.6ms) + image-painter
`onRemembered` (~2ms).

So: **Settings scroll is draw-bound (glass rasterization); Home scroll is recomposition-bound**
(new LazyRow tiles entering). Profile with Perfetto (`adb shell perfetto -t 20s -b 32mb -a
com.arashrahimi46.iptv gfx view res`) before assuming either. The RenderThread column
(`SwapBuffers - IssueDrawCommandsStart`) is still the number that transfers to real hardware, and
the emulator translates GLES→Metal on the host so its GPU/absolute frame times aren't comparable
to a TV.

**Measurement harness caveat:** `adb shell input keyevent` sweeps (one process fork per key) do
not land on the same view-hierarchy state rep to rep — same-build reruns swung p90 20–46ms and
janky 2.7–56%, and one rep showed 94 attached Views vs the usual 9–10. Aggregate framestats
percentiles are useless at that noise level. Use **trace-to-trace diffing** of a named slice
(e.g. `AndroidOwner:measureAndLayout` total) over a fixed protocol: force-stop → fresh launch →
settle → 20s capture → identical scripted sweep, ≥2 reps per build.

Locked there: the ambient mesh stays static (`t = 0`) because the bake depends on it. Also:
`softShadow`/mesh bakes use `drawWithCache`, which re-runs on size change — any new size-animating
glass surface must pass `bake = false`.

**The sidebar rail has NO backdrop blur, and that is measured, not taste (2026-07-28).** It used to
frost the page behind it. On the XL95 that one blur was **30% of the whole RenderThread budget** and
turned 4 janky frames into 60; deleting it (and the now-dead page-capture apparatus with it) took
RenderThread 2156 → 1113ms (−48%), p90 19.2 → 10.2ms, janky frames 65 → 2. Crucially, **halving the
blur radius made it worse** — the cost is the blur *pass*, not the kernel, so there is no cheap-blur
setting. The rail keeps `surfaceGlassSheer`, so it is still see-through, just unblurred. Do not
reintroduce a backdrop blur there without re-running the measurement in
`docs/glass-render-perf-findings.md`. Two things that *look* like the culprit and are not: the
panel's `softShadow(bake = false)` (0.4%) and `scrollEdgeFade`'s offscreen layer (1.8%).

**D-pad input is paced to one directional key per 120ms** (`DPAD_STEP_MS` in `MainActivity`), extras
dropped, giving ~8 steps/s on a held key. Do NOT "optimise" this into a per-frame gate: that was
tried and is a no-op, because frames are ~6.6ms and the thing being paced is the bring-into-view
scroll animation, which is many frames long. When testing this, send the burst inside a SINGLE
`input keyevent 20 20 20 ...` call — one `adb shell` per key is ~100ms apart and exercises nothing.

**Landed (2026-07-28, `a565ac6`):** the Home `LazyColumn`'s `itemsIndexed` now passes a
`contentType` (poster / channel / category) so Compose's slot-reuse pool can tell rail shapes
apart. Measured: `AndroidOwner:measureAndLayout` total 748/715ms → 659/651ms, per-animation-
callback cost −28% (1.24ms → 0.89ms), screenshot-verified pixel-identical. **Any new
heterogeneous lazy list needs the same treatment** — `key` alone is not enough.

Open leads: ~5ms RenderThread + ~4ms sync/upload still unattributed, and **input latency**
(241 high-latency events per 14 keypresses), untouched. Ruled out: `computePalette` (0.79ms
across a whole 20s trace — a red herring).

### Baseline profile: generate it on the EMULATOR, never the TV (round 7, 2026-08-08)

The shipped profile contained **zero app classes** for months, silently, and fixing it took Settings'
first paint from **244ms to 85ms** (JIT 130ms → 1ms). Three things to keep straight:

- **`:tv:generateReleaseBaselineProfile` must run on the `Television_1080p` AVD (API 36).** Collection
  needs API 33+ (or root on 28+); the XL95 is **API 31 and unrootable**, so aiming the task at it just
  fails. It is also a connected-android-test task that **uninstalls the app when it finishes** — it wiped
  the real TV's playlist, favourites and settings once. Never point it at a device you care about.
- **R8 must be off for `nonMinifiedRelease`, and only `finalizeDsl` can do it.** `buildTypes { all { } }`
  runs before the plugin's `initWith(release)` copies `enable = true` back; `afterEvaluate` runs after AGP
  locks the DSL. Verify by counting readable classes, not by reading the flag:
  `dexdump -f tv/build/outputs/apk/nonMinifiedRelease/*.apk | grep -c "arashrahimi46/iptv/ui"` — 0 is
  broken, ~24000 is right. (AGP 9: `optimization { enable }` drives R8; `isMinifyEnabled` reads false on a
  fully-minified `release`.)
- Coverage is still partial — no Home/Browse/Guide/Settings *screen* code, because the generating emulator
  has no catalogue and the journey can't reach those screens. **Seeding a playlist there is the open
  lever.**

### Two measurement traps that produce believable wrong numbers

- **Disable the TV screensaver before any capture** (`adb shell settings put secure screensaver_enabled 0`
  and `sleep_timeout -1`). The settle windows are long enough to trigger Google TV's ambient mode, and the
  resulting trace is well-formed but contains the screensaver, not the app. It read as a 5× improvement.
- **Screenshot at the end of every trace and look at it.** Two round-7 captures were invalid (one
  screensaver, one that landed in Home's *edit* mode) and both looked like good news.

### media3 `PlayerView` builds its controller even when `useController = false`

That flag only hides an already-built `PlayerControlView`; the constructor builds one whenever
`exo_controller_placeholder` is present in the layout. Every video surface therefore inflates it via
`are_player_view.xml` (→ `are_player_content.xml`), which omits the placeholder: **60.4ms → 14.3ms** of
main-thread inflation per player open, ×N panes in Multi-View. Do not go back to `PlayerView(context)` —
`player_layout_id` is constructor-only and unreachable from Kotlin.
