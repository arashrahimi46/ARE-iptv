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
| `:mobile` | Phone stub — not the focus |
| `:baselineprofile` | Startup baseline profile generation |

Layout under `tv/src/main/java/com/arashrahimi46/iptv/`: `data/` (db=Room, repository, parser,
settings=DataStore, model) · `ui/` (one package per screen + `ui/components` + `ui/theme`).

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

Ships **English + 21 translated locales**. A missing translation silently falls back to English
(no build error), so keep them in sync by hand.

- Every user-facing string goes through `stringResource(R.string.…)` — never a hardcoded literal
  in `Text(...)` / `contentDescription` (glyphs, counters, and the brand mark excepted).
- When you add/rename a key, add it to **`values/strings.xml` AND all 21 `values-*/strings.xml`**:
  `az, b+pt+BR, b+pt+PT, bg, cs, da, de, el, es, fi, fr, hu, it, nb, nl, pl, ro, ru, sv, tr, uk`.
- Preserve positional format args exactly (`%1$s`, `%1$d`, `%2$d`); a locale may reorder them but
  every index must still appear. Escape `'` as `\'`; XML-escape `&`/`<`/`>`.
- Audit gaps: diff each `values-*/strings.xml`'s `name="…"` keys against `values/strings.xml`.

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

## Rendering performance — READ BEFORE OPTIMIZING ANY JANK

`docs/glass-render-perf-findings.md` — profiled 2026-07-27 with `dumpsys gfxinfo framestats`.

**This app is draw-bound, not recomposition-bound.** On a Settings D-pad scroll, composition +
measure + layout came to **0.04ms** of a 27ms frame; the frame is spent rasterizing glass. Do not
chase recomposition for a jank report — profile first, and read the RenderThread column
(`SwapBuffers - IssueDrawCommandsStart`), which is the only number that transfers to real hardware.
The emulator translates GLES→Metal on the host, so its GPU time and absolute frame time are not
comparable to a TV.

Locked there: the **glass backdrop system stays** (costs ~4ms/frame but removing it is a real
visual change — measured), and the ambient mesh stays static (`t = 0`) because the bake depends on
it. Also: `softShadow`/mesh bakes use `drawWithCache`, which re-runs on size change — any new
size-animating glass surface must pass `bake = false`. Best open lead is **input latency**
(241 high-latency events per 14 keypresses), which is untouched.
