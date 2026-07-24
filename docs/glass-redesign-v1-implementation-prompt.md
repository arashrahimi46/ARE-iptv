# Glass Design V1 — Implementation Kickoff Prompt

> Paste the block below to the implementing agent to start in **plan mode** with a
> multi-agent fan-out. Companion spec: [`glass-redesign-v1-design.md`](./glass-redesign-v1-design.md).
> Visual: [`glass-redesign-v1.html`](./glass-redesign-v1.html).

---

Enter PLAN MODE. Do not write any code until I approve the plan.

You are implementing the "Glass Design" initiative for the are-iptv Android TV app
(Kotlin + Jetpack Compose for TV, module :tv, package com.arashrahimi46.iptv).

## Read these first (source of truth — do not relitigate locked decisions)
- docs/glass-redesign-v1-design.md   — spec: locked decisions, new tokens, the
  Modifier.glassSurface() helper, per-component build table, the 10-item nav rail,
  blur policy, interaction states, tab options, HUD options, light-theme/RTL a11y, phasing.
- docs/glass-redesign-v1.html        — visual companion (before/after + full-page mocks).
- CLAUDE.md                          — project rules: JDK 21, gradle build/test commands,
  i18n (English + 21 locales must stay in sync), TV UX conventions, semantic color tokens
  only, dialogs must trap focus in a real Dialog(), commit straight to main, never curl
  provider streams.

## Ground truth before planning
Use the code graph / read the real files — don't trust the spec's file paths blindly.
Confirm the actual files for: ui/theme/Color.kt, Focus.kt, Motion.kt; components/
(Rail.kt, SidebarNav.kt, IconButton.kt, Button.kt, Tabs.kt, Badge.kt, Chip.kt, Switch.kt,
Dialog.kt, PlayerControls.kt, ChannelTile.kt, PosterTile.kt, CategoryCard.kt, GuideCell.kt);
shell/TopBar.kt; settings/* (SettingsPanes, SettingsScreen, SettingsViewModel, and the
DataStore-backed settings store); and the Search/Favorites/Guide screen packages.
Flag any spec/code mismatch in your plan instead of silently diverging.

## SCOPE CHANGE — HUD becomes user-customizable (net-new feature, NOT just a surface pass)
The spec treats "HUD layout A/B/C" as a one-time design pick. Override that: the HUD style
must be a USER SETTING, and users must be able to REARRANGE the HUD button groups.
Plan for:
  - A persisted setting in the DataStore settings store: HUD style (the A/B/C options as an
    enum) + a persisted ordering of the HUD control groups (transport / metadata / utilities,
    and ideally per-button-group order within them).
  - A Settings entry (in settings/SettingsPanes.kt) to pick the HUD style AND an in-place
    editor to move the button groups — D-pad friendly (select a group, move left/right,
    confirm), with a live preview and a "reset to default" action. Follow TV UX conventions:
    every control uses TvFocusable; any modal uses a real focus-trapping Dialog().
  - PlayerControls.kt reads the style + ordering from the setting and renders accordingly,
    with a sensible default (spec recommends style B) when unset.
  - New user-facing strings: add to values/strings.xml AND all 21 values-*/strings.xml.
  - Consider ArePlayerControls being driven by a small ordered list of "control group" slots
    so reordering is data, not layout forks.
Call this out as its own workstream; it is the one place we deliberately add a feature.

## How to plan: fan out with multiple subagents, then synthesize
Spawn parallel investigator subagents (one per area) to map exact call sites, current
fills/shapes, and blast radius before proposing changes — each returns files, exact edit
points, risks, and open questions, NOT edits. Suggested split:
  1) Theme/foundation — Color.kt tokens + new Glass.kt helper + previews
  2) Chrome — Rail/SidebarNav, TopBar, IconButton, Button, Tabs, Badge, Chip
  3) Surfaces — Channel/Poster/Category/Continue/Hero cards, Switch, Settings panes
  4) Dialogs/HUD glass — Dialog.kt + all *Dialog modals + PlayerControls glass material
     (real blur, SDK_INT>=31)
  5) HUD customization feature — DataStore setting + Settings editor + reorder interaction
     + PlayerControls slot model (the scope-change workstream above)
  6) Pages — Search, Favorites, TV Guide/EPG (+ verify sidebar shows all 10 nav items)

## Deliverable of plan mode
A phased plan following the spec's phasing (1 Foundation → 2 HUD → 3 Chrome → 4 Surfaces →
5 Polish), with the HUD-customization feature slotted into/after phase 2, including:
- the OPEN decisions surfaced for me to pick before coding: default HUD style (A/B/C, spec
  recommends B), tab style (spec recommends Segmented), and how granular the reorder is
  (whole groups vs individual buttons);
- verification per phase: ./gradlew :tv:compileDebugKotlin, then :tv:assembleDebug
  (validates all string resources), plus on-device screenshot check on emulator-5554 —
  including verifying the HUD reorder persists across app restart;
- a full list of any new strings introduced (the HUD feature will add several).

Constraints: everything except the HUD-customization feature stays a surface/material pass —
no other re-layouts or features. Keep TvFocusable on every focusable, semantic color tokens
only, files under 500 lines, i18n synced across all 22 locale files.

Present the plan for my approval. Do not start editing until I say go.
