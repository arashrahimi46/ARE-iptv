# `:mobile` UI rewrite — v1 design spec

**Status:** locked contract. Every later agent builds against this document literally.
**Date:** 2026-07-31.
**Scope:** the `:mobile` module only. `tv/` is byte-identical to `6d3d9cc` and MUST NOT be touched.

---

## 0. Locked decisions

These were decided by the user. Do not relitigate, do not "improve", do not ask.

| # | Decision | Locked value |
|---|---|---|
| D1 | **Depth of the rewrite** | UI layer ONLY. `mobile/.../data/**` (Room, DAOs, repositories, parsers, DataStore, `StreamUrlResolver`, `CredentialsStore`) is untouched except for the two additive queries named in §5. ViewModels keep their existing data plumbing; they may gain state fields and actions, never a new data source. |
| D2 | **Visual identity** | The glass language is KEPT verbatim. `mobile/src/main/java/com/arashrahimi46/iptv/ui/theme/{Color,Glass,GlassTier,ControlSkin,Motion,Radius,Spacing,TileWash,AmbientBackdrop,Type}.kt` survive unchanged. Semantic tokens only — never the raw `Ink*`/`Blue*`/`Light*` ramps. Vazirmatn auto-swap for RTL stays. Both themes ship. |
| D3 | **Mechanics** | Touch-first. Material ripple replaces focus scale; 48dp minimum targets; bottom sheets replace TV modal cards where natural; swipe, long-press and pull-to-refresh are first-class. |
| D4 | **`androidx.tv.material3`** | MUST reach **zero** usages in `:mobile`, including `mobile/build.gradle.kts:136 implementation(libs.androidx.tv.material)` which is deleted at the end. Verification: `grep -rn "androidx.tv" mobile/` returns nothing outside comments. |
| D5 | **Screen scope** | Home, Live, Movies, Series (grid), Movie detail, Series detail **including the season/episode playback path**, Guide/EPG, Search, Favorites, Settings, Onboarding, Player, Language select, Splash. Recordings and Streams are lower priority but MUST keep compiling and working. **Do NOT build** multiview, the HUD layout editor, or explore — those are TV-only surfaces and their files are deleted. |
| D6 | **Component names & home** | The `Are*` names are kept for brand continuity. They MOVE to package `com.arashrahimi46.iptv.mobile.ui.components` (directory `mobile/src/main/java/com/arashrahimi46/iptv/mobile/ui/components/`). The old tree `mobile/src/main/java/com/arashrahimi46/iptv/ui/components/` and `.../ui/interaction/` are **deleted wholesale** at the end of the migration. |
| D7 | **The interaction seam dies** | `AreInteractive` / `AreInteractiveSurface` / `AreInteractiveBinding` / `LocalAreInteractiveBinding` / `mobileAreInteractiveBinding` / `mobile/ui/theme/AreTouchable.kt` are all deleted. Their replacement is `Modifier.areTouch(...)` (§2.1) — a plain modifier, no CompositionLocal indirection. |
| D8 | **Strings** | **`CoreR` (`com.arashrahimi46.iptv.core.R`) is the single source for user-facing strings in `:mobile`.** Every rewritten call site imports `com.arashrahimi46.iptv.core.R as CoreR`. New keys go in `core/src/main/res/values/strings.xml` **plus all 23** `core/src/main/res/values-*/`: `ar az b+pt+BR b+pt+PT bg cs da de el es fa fi fr hu it nb nl pl ro ru sv tr uk`. `fa` and `ar` are RTL and are the two everyone forgets — count 23 or don't count. `mobile/src/main/res/values*/strings.xml` is left in place (it still backs unmigrated code) but **no new key is ever added there**, and a migrated file must not import `com.arashrahimi46.iptv.mobile.R` for strings. `mobile.R.drawable`/`R.font`/`R.mipmap` remain legal. |
| D9 | **Glass tier & accent** | `mobile/ui/theme/Theme.kt` starts calling `rememberGlassTier()` and providing `LocalGlassTier`, and applies `withBlurredBackdrop()` when `tier.hasBackdropBlur`. Accent stays hardwired to `AccentPreset.BLUE` (no settings picker in v1). `LocalAppBackdrop` stays unprovided — **no per-surface backdrop blur on phone** (measured cost, see `docs/glass-render-perf-findings.md`). |
| D10 | **Spacing tokens** | The TV-only values in `AreIptvSpacing` (`safeX`, `safeY`, `railGap`, `railPeek`, `sidebar*`, `tilePosterWidth`, `tileLandWidth`, `guide*`) are NEVER read by phone code. Tile widths are an explicit per-screen argument (values fixed per screen in §4). The 8dp scale (`sp0..sp24`) is fine to use. |
| D11 | **Reduced motion** | `Theme.kt` provides `LocalReducedMotion` from `Settings.Global.TRANSITION_ANIMATION_SCALE == 0f`. All animations read `AreIptvTheme.motion` durations so reduced motion is honoured for free. |
| D12 | **No new abstractions** | No shared base classes, no generic "component framework", no `:core` Kotlin, no new Gradle module. Copy-paste over premature extraction — the same rule that governs `:tv` vs `:mobile`. |

### Deletion list (executed by the cleanup agent, §6.9, only after every screen is migrated)

```
mobile/src/main/java/com/arashrahimi46/iptv/ui/components/          (entire directory, 24 files)
mobile/src/main/java/com/arashrahimi46/iptv/ui/interaction/         (entire directory)
mobile/src/main/java/com/arashrahimi46/iptv/mobile/ui/theme/AreTouchable.kt
mobile/src/main/java/com/arashrahimi46/iptv/mobile/ui/player/HudLayout.kt
```
Two files are **moved, not deleted** (content preserved, package/imports rewritten):
- `ui/components/ClockFormat.kt` → `mobile/ui/components/ClockFormat.kt` (zero TV coupling, keep verbatim).
- `ui/components/ParentalBlur.kt` → `mobile/ui/components/ParentalBlur.kt` (`ParentalBlurState`, `LocalParentalBlur`, `ParentalLockOverlay`; swap `androidx.tv.material3.Icon` → `androidx.compose.material3.Icon`).

`ui/theme/*` is NOT deleted. Two symbols inside it become dead and are removed by the cleanup agent: `tvGlow` stays (renamed usage-wise it is still the badge/health halo — keep the function, it has no TV dependency), `LocalMinTouchTarget` is deleted, `LocalAmbientArtwork` reads are deleted from components and its `:mobile` stub provider is removed from `mobile/ui/theme/Theme.kt`. `motion.focusScale` stays in the data class (`:tv` shares the file's shape) but is never read by phone code.

---

## 1. Ground rules every agent obeys

1. **Modifier order for a glass control** (never reorder): `graphicsLayer{scale}` → `softShadow` → `background(brush ?: color, shape)` → `border(1.dp, borderBrush ?: borderColor, shape)` → `clip(shape)` → `combinedClickable(interactionSource, indication = ripple(), …)`.
2. Glass modifiers stay plain `@Composable` extensions. **Never `Modifier.composed {}`.**
3. **`remember` every `Brush`.** An unremembered gradient mints a new object per recomposition per tile.
4. `softShadow(bake = false)` on any surface whose measured size animates.
5. Heterogeneous lazy lists pass a **`contentType`** per item, not just a `key`.
6. Tile grids/rows use `contentPadding`, not `Modifier.padding` — the title sits below the touch surface.
7. **Glass never stacks.** A control inside glass uses `glassChild()`/`glassTrack()` and `ProvideOnGlass`; fills come from `controlSkin(tone, selected, disabled, selectable)` and are never re-derived at the call site.
8. **Selection is a lens** (`accentLensBrush`/`lensBorderBrush`/`lensContentColor`), **actions are a gradient** (`accentGradientBrush`). Never swap them.
9. RTL: `start`/`end` only, `Arrangement.Start/End` only. Hearts and badges go to the `end`.
10. Every user-facing string via `stringResource(CoreR.string.…)`. Glyphs, numeric counters and the brand mark are exempt.
11. Contrast is the gate: ≥ 4.5:1 for every text token over its real backdrop, in **both** themes. Provider logo wells stay dark in light theme.
12. Every screen root applies the insets it owns: content screens use `Modifier.consumeWindowInsets` + the `Scaffold` padding they are handed; text-entry screens add `imePadding()`; the player goes immersive.
13. `rememberSaveable` for any UI state a user can lose to rotation (selected tab, query text, expanded season).

---

## 2. THE COMPONENT CONTRACT

All of these live in **`mobile/src/main/java/com/arashrahimi46/iptv/mobile/ui/components/`**, package `com.arashrahimi46.iptv.mobile.ui.components`. All imports are `androidx.compose.material3.*` — **never `androidx.tv.material3`**. Theme access is `AreIptvTheme.{colors,typography,spacing,radius,motion}` (the `:core`-tree theme object, which `mobile/ui/theme/Theme.kt` populates with phone-sized typography).

Signatures below are final. A later agent implements exactly these — no extra parameters, no renames, no "while I'm here" additions.

### 2.1 `AreTouch.kt` — the press primitive (replaces `AreInteractive`)

```kotlin
package com.arashrahimi46.iptv.mobile.ui.components

/**
 * Phone press surface. Ripple-indicated, 48dp-guaranteed, no focus machinery.
 *
 * Draw order: scale -> softShadow -> fill -> border -> clip -> combinedClickable(ripple).
 * `pressScale` (0.97) is applied ON TOP of the ripple; it is the brand's press feel and is
 * disabled by `disableScale` for large surfaces (tiles use the ripple only).
 */
@Composable
fun Modifier.areTouch(
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(AreIptvTheme.radius.md),
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    role: Role? = Role.Button,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource? = null,
    backgroundColor: Color = Color.Transparent,
    backgroundBrush: Brush? = null,
    borderColor: Color? = null,
    borderBrush: Brush? = null,
    shadowElevation: Dp = 0.dp,
    minTouchTarget: Dp = 48.dp,
    disableScale: Boolean = false,
    rippleBounded: Boolean = true,
): Modifier

/** Applies a [ControlSkin] wholesale. Every skinned control uses this, never the raw params. */
@Composable
fun Modifier.areTouch(
    onClick: () -> Unit,
    skin: ControlSkin,
    shape: Shape = RoundedCornerShape(AreIptvTheme.radius.md),
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    role: Role? = Role.Button,
    interactionSource: MutableInteractionSource? = null,
    minTouchTarget: Dp = 48.dp,
    disableScale: Boolean = false,
): Modifier
```

Behaviour, mandatory:
- `indication = ripple(bounded = rippleBounded, color = <see below>)`. Ripple colour: on an accent-filled surface (`skin.content == colors.accentFg`) use `colors.textOnAccent.copy(alpha = 0.30f)`; otherwise `colors.accent.copy(alpha = 0.24f)`. Remember the colour.
- `minTouchTarget` is enforced with `Modifier.defaultMinSize(minWidth = it, minHeight = it)` applied to the **touch layer**, outside the visual `background`/`border`, so a 34dp chip still has a 48dp target without growing visually. Pass `minTouchTarget = 0.dp` only for a control already ≥48dp on both axes.
- Scale: `disableScale -> 1f`, `pressed -> motion.pressScale`, else `1f`, `tween(motion.durFastMs, motion.easeEmph)`. Read via `scaleState.value` **inside** `graphicsLayer`, never `by` in composition.
- `combinedClickable(enabled = enabled, onLongClick = onLongClick, onLongClickLabel = …)`. When `onLongClick != null`, the component MUST also fire `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)`.
- `contentDescription` sets `Modifier.semantics { this.contentDescription = it }`.
- No `focusable()`, no `FocusRequester`, no `bringIntoViewRequester`, no `collectIsFocusedAsState` anywhere in this file or any file that uses it.

### 2.2 `Button.kt`

```kotlin
enum class AreButtonVariant { Primary, Secondary, Ghost, Danger }
enum class AreButtonSize { Small, Medium, Large }   // heights 40 / 52 / 62 dp

@Composable
fun AreButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AreButtonVariant = AreButtonVariant.Primary,
    size: AreButtonSize = AreButtonSize.Medium,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    full: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
)
```
- `variant` → `ControlTone.{Primary,Neutral,Ghost,Danger}` → `controlSkin(tone, disabled = !enabled)`. Shape `RoundedCornerShape(radius.md)`.
- Visual heights stay 40/52/62dp; **`Small` passes `minTouchTarget = 48.dp`** so the touch layer is legal while the pill still reads as 40dp. Padding 16/22/30dp horizontal, gap 8/10/12dp, glyph 18/20/24dp.
- `loading = true` → the label is replaced by an 18dp `CircularProgressIndicator(strokeWidth = 2.dp, color = skin.content)`, the button is non-clickable, and `semantics { stateDescription = <CoreR.string.common_loading> }`. Width does not change (`Modifier.widthIn(min = <measured>)` is NOT required — use `Box` with the text at `alpha = 0f` behind the spinner).
- `full = true` → `fillMaxWidth()`.
- **Renamed param:** the old `disabled: Boolean` becomes `enabled: Boolean` (inverted). Every call site flips it.

### 2.3 `Chip.kt`

```kotlin
enum class AreChipSize { Small, Medium }   // visual heights 34 / 42 dp

@Composable
fun AreChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: ImageVector? = null,
    dotColor: Color? = null,
    size: AreChipSize = AreChipSize.Medium,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
)
```
- `controlSkin(ControlTone.Neutral, selected = selected, disabled = !enabled, selectable = true)`, shape `RoundedCornerShape(radius.pill)`, `role = Role.RadioButton`, `semantics { selected = … }`.
- `minTouchTarget = 48.dp` (this is what the deleted `LocalMinTouchTarget` hack was for; it is now intrinsic).
- Horizontal padding 14dp (Small) / 18dp (Medium); leading `dotColor` is an 8dp circle; leading `icon` is 16dp.
- Fill cross-fades over `tween(motion.durFastMs)` when `selected` changes.

### 2.4 `IconButton.kt`

```kotlin
enum class AreIconButtonVariant { Solid, Glass, Ghost }
enum class AreIconButtonSize { Small, Medium, Large }   // visual 40 / 48 / 56 dp, glyph 20 / 24 / 28 dp

@Composable
fun AreIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AreIconButtonVariant = AreIconButtonVariant.Ghost,
    size: AreIconButtonSize = AreIconButtonSize.Medium,
    active: Boolean = false,
    enabled: Boolean = true,
    contentTint: Color? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
)
```
- **`ExtraSmall` is deleted.** `Small` is now 40dp visual with `minTouchTarget = 48.dp`; it is the only size allowed inside a tile.
- `Solid` → `ControlTone.Primary`; `Glass` → `ControlTone.Neutral` under `ProvideOnGlass(true)`; `Ghost` → `ControlTone.Ghost`. `active = true` → `selected = true` in `controlSkin`.
- Shape: `Ghost` and `Glass` use `CircleShape`; `Solid` uses `RoundedCornerShape(radius.md)`. `rippleBounded = false` for `Ghost`.
- `contentDescription` is mandatory and non-null — it is the accessibility label.

### 2.5 `Switch.kt`

```kotlin
@Composable
fun AreSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
)
```
- Keeps the 58×34dp glass track and the 26dp thumb animated 4dp↔28dp with `animateDpAsState(tween(motion.durFastMs, motion.easeEmph))`; ON = `accentLensBrush()` + `lensBorderBrush()`, OFF = `glassTrackTint` + `glassBorderBrush()`.
- **New: it is draggable.** `Modifier.draggable(orientation = Horizontal, state = rememberDraggableState { … })` accumulating offset; on drag end commit `checked = offset > half` and animate to the rest position. A drag shorter than `ViewConfiguration.touchSlop` falls through to the tap.
- `Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, interactionSource = …, indication = ripple(bounded = false, radius = 24.dp))` wraps the whole control inside a `Box(Modifier.defaultMinSize(48.dp, 48.dp))`.

### 2.6 `Tabs.kt` — replaces `AreSegmentedControl` at every call site

```kotlin
@Composable
fun <T> AreTabs(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    badge: (@Composable (T) -> Unit)? = null,
)
```
- Renders a glass track (`glassTrack(RoundedCornerShape(radius.pill))`, height 48dp, inner 6dp padding) with a spring-animated lens (`spring(dampingRatio = 0.82f, stiffness = StiffnessMediumLow)`) behind the selected segment, positioned from per-segment `onGloballyPositioned` bounds. `selectableGroup()`; each segment `Modifier.areTouch(role = Role.Tab, minTouchTarget = 48.dp, disableScale = true)`.
- `scrollable = true` → wrap in `horizontalScroll(rememberScrollState())` and auto-scroll the selected segment into view via `animateScrollTo` (this is the touch replacement for D-pad bring-into-view). `scrollable = false` → segments are `weight(1f)` and equal-width.
- The track **is** clipped now (the TV rationale — room for a focus ring — is gone).
- `AreSegmentedControl` and the `selectedFocusRequester` parameter do not exist. `TabItem` does not exist.

### 2.7 `PosterTile.kt`

```kotlin
@Composable
fun ArePosterTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    posterUrl: String? = null,
    meta: String? = null,
    rating: String? = null,
    progress: Float? = null,
    width: Dp? = null,               // null == fill the parent's width (grid cell)
    badges: (@Composable RowScope.() -> Unit)? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    lockCategory: String? = null,
    interactionSource: MutableInteractionSource? = null,
)
```
- `width = null` replaces the old `fillWidth: Boolean` — a grid caller passes nothing, a rail caller passes an explicit Dp. `AreIptvTheme.spacing.tilePosterWidth` is never the default.
- Poster area: 2:3 `aspectRatio`, `glassSurface(RoundedCornerShape(radius.lg))` + `tileWash(shape, rememberTileWashHue(posterUrl, title))`, image via `rememberTileArtwork(posterUrl, maxDimension = <resolved width> * 2)`, initials fallback. Content is **clipped to the tile shape**.
- Overlays: `badges` TopStart; `rating` pill TopEnd (`glassChild` + `colors.ratingStar` star); `progress` bar BottomStart on `glassTrack`; favourite `AreIconButton(variant = Glass, size = Small)` BottomEnd (RTL-safe: `Alignment.BottomEnd`); `ParentalLockOverlay` when `LocalParentalBlur.current.isObscured(lockCategory)`.
- Below the poster: 10dp gap, `title` (`typography.tile`, `maxLines = 2`, `TextOverflow.Ellipsis`) and optional `meta` (`typography.caption`, `colors.textSecondary`, 1 line). **No marquee** — `tileMarquee()` is deleted; phones ellipsize.
- Press: ripple on the poster only, `disableScale = true`. Long press fires haptics + `onLongClick`.
- No `focusRequester`, no `LocalAmbientArtwork`, no `BringIntoViewRequester`.

### 2.8 `ChannelTile.kt`

```kotlin
enum class AreStreamHealthLevel { Stable, Moderate, Poor }   // moved here from StreamHealth.kt

@Composable
fun AreChannelTile(
    channel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoUrl: String? = null,
    number: String? = null,
    now: String? = null,
    next: String? = null,
    category: String? = null,
    isRadio: Boolean = false,
    health: AreStreamHealthLevel? = null,
    quality: String? = null,
    catchup: Boolean = false,
    width: Dp? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    lockCategory: String? = null,
    interactionSource: MutableInteractionSource? = null,
)
```
- Same `width: Dp?` convention. `codec` is dropped (no call site). `health` becomes nullable and the dot is only drawn when non-null.
- Logo zone 16:9 (was 16:8) with `logoWellScrim` + `glassChild` square logo well at 78% height; LIVE/CATCH-UP `AreBadge` TopStart; `quality` tag + health dot TopEnd; favourite `AreIconButton(Glass, Small)` BottomEnd.
- Info panel: `surfaceGlass`, `number` + `channel` on one line (`typography.tile`, 1 line, ellipsis), a 24dp badge row (radio / category), and the `now`/`next` block rendered **only when `now != null`** — no reserved empty line heights (the TV fixed-height reservation existed to stop focus-ring jitter).
- No marquee, no ambient artwork publish.

### 2.9 `Badge.kt`

```kotlin
enum class AreBadgeTone { Live, New, Quality, Catchup, Smart, Neutral }

@Composable
fun AreBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: AreBadgeTone = AreBadgeTone.Neutral,
    glow: Boolean = false,
    icon: ImageVector? = null,
)
```
Non-interactive. `Live` stays solid `colors.danger` with white text in both themes. `glow` uses `tvGlow(color, shape, spread = 8.dp)` — the helper keeps its name, it has no TV dependency.

### 2.10 `StreamHealth.kt`

```kotlin
enum class AreStreamHealthSize { Small, Medium, Large }   // dots 8 / 11 / 14 dp

@Composable
fun AreStreamHealth(
    level: AreStreamHealthLevel,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    bitrate: String? = null,
    size: AreStreamHealthSize = AreStreamHealthSize.Medium,
)
```
`AreStreamHealthLevel` is declared in `ChannelTile.kt` (§2.8) and imported here.

### 2.11 `Sheet.kt` — the bottom-sheet family (replaces `AreDialog` for non-destructive modals)

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
)

/** Single-choice list sheet. The workhorse for every Settings choice row. */
@Composable
fun <T> AreChoiceSheet(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: (@Composable (T) -> String?)? = null,
    leading: (@Composable (T) -> Unit)? = null,
)

/** Long-press context sheet for a tile. Replaces AreTileActionDialog. */
@Composable
fun AreTileActionSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onPlayFromStart: (() -> Unit)? = null,
    onOpenDetails: (() -> Unit)? = null,
)
```
- All three are built on `androidx.compose.material3.ModalBottomSheet` with `dragHandle = { AreSheetHandle() }`, `containerColor = Color.Transparent`, and the sheet body wrapped in `ProvideOnGlass(true)` + `glassSurface(RoundedCornerShape(topStart = radius.xl, topEnd = radius.xl), elevated = true)`.
- `windowInsets = WindowInsets(0)` and the content applies `navigationBarsPadding()` itself; body max height `0.9f * screenHeight`, content scrolls, `actions` row is pinned below the scroll.
- Tapping the scrim and dragging down both dismiss (`onDismiss`). Every row is ≥56dp.
- `AreChoiceSheet` rows: leading optional slot, label + optional supporting text, trailing `RadioButton`-equivalent check (`Icons.Filled.Check` tinted `colors.accent`) on the selected row. Selecting a row calls `onSelect` **and then** `onDismiss`.

### 2.12 `Dialog.kt` — `AreAlertDialog` (destructive/blocking confirms only)

```kotlin
@Composable
fun AreAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = null,
    destructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
)
```
- Built on `androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true))` — **scrim tap and Back both dismiss**, which the TV `AreDialog` never allowed.
- Card: `widthIn(max = 400.dp).fillMaxWidth(0.92f)`, `glassSurface(RoundedCornerShape(radius.xl), elevated = true)`, `ProvideOnGlass(true)`, 24dp padding. **No fixed 520dp width.**
- Buttons: `AreButton(dismissLabel, variant = Ghost)` then `AreButton(confirmLabel, variant = if (destructive) Danger else Primary)`, `Arrangement.spacedBy(12.dp, Alignment.End)`.
- `content` is for the rare dialog that needs a body widget (PIN entry). When `content != null` it is placed between `text` and the actions, capped at `heightIn(max = 420.dp).verticalScroll`.
- **Rule for choosing:** blocking + destructive + short → `AreAlertDialog`. Everything else (choices, pickers, long documents, tile actions, language, legal) → a sheet from §2.11.

### 2.13 `TextField.kt` (existing file, moved and extended)

```kotlin
@Composable
fun AreTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    monospace: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
)
```
- Keeps `glassWell(RoundedCornerShape(radius.md))` + `BasicTextField`; accent border on focus; height `≥ 52dp`.
- New: `label`, `isError` (border → `colors.danger`, supporting text → `colors.danger`), `supportingText`, `keyboardOptions`/`keyboardActions` (so every screen sets a real `imeAction`), `trailingIcon` + `onTrailingIconClick` (the universal clear button), `monospace` (URL fields).
- `activateOnClick` from the TV field does not exist.

### 2.14 `Scaffold.kt` — screen chrome

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
)

/** Glass top bar used by AreScreenScaffold; exposed for screens that need a custom body layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
)
```
- `AreTopBar` = `TopAppBar` with `colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)`, the whole bar drawn on `glassSurface(RectangleShape, elevated = <scrollBehavior is collapsed>)`, `ProvideOnGlass(true)`, title in `typography.h3`, navigation icon = `AreIconButton(Icons.AutoMirrored.Filled.ArrowBack, CoreR.string.common_back, onBack)` when `onBack != null`.
- `AreScreenScaffold` supplies `statusBarsPadding` via the top bar and hands `content` the `PaddingValues`. Content that scrolls MUST apply `contentPadding = paddingValues` to its lazy list, not `Modifier.padding`.
- **Every child screen (Guide, Search, Favorites, Recordings, Streams, Movie detail, Series detail) uses this and therefore gains a visible Back affordance.**

### 2.15 `Rail.kt`

```kotlin
@Composable
fun AreRail(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    itemSpacing: Dp = 12.dp,
    content: LazyListScope.() -> Unit,
)
```
Header `Row`: `title` (`typography.h3`) + optional `subtitle`, and when `onSeeAll != null` a trailing `AreButton(CoreR.string.common_see_all, variant = Ghost, size = Small, trailingIcon = ChevronRight)`. Body: `LazyRow(contentPadding = contentPadding, horizontalArrangement = spacedBy(itemSpacing))`. No `FocusRequester`, no `railPeek`, no `focusGroup`.

### 2.16 `ListRow.kt` — the Settings/list primitive

```kotlin
@Composable
fun AreListRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
)

@Composable
fun AreSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
)

/** Opens an AreChoiceSheet; the current value is the supporting text. */
@Composable
fun <T> AreChoiceRow(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    sheetTitle: String = title,
)

@Composable
fun AreSectionHeader(text: String, modifier: Modifier = Modifier)
```
- All rows: `minHeight = 56.dp`, 16dp horizontal padding, full-width ripple, `leadingIcon` tinted `colors.accent`, `title` = `typography.body`, `supporting` = `typography.caption`/`textSecondary`.
- `AreListRow` with `onClick != null` and no `trailing` draws a trailing `ChevronRight` in `colors.textTertiary` automatically.
- `AreChoiceRow` owns its `rememberSaveable` sheet-open state. **This single component deletes every horizontal chip row in Settings.**

### 2.17 `Empty.kt` — states

```kotlin
@Composable
fun AreEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
)

@Composable
fun AreErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
)

@Composable
fun AreLoadingState(modifier: Modifier = Modifier, message: String? = null)

/** Shimmering placeholder block used by grid/rail skeletons. */
@Composable
fun AreSkeleton(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(AreIptvTheme.radius.lg))
```
`AreSkeleton` animates `glassTrackTint` alpha 0.6↔1.0 with `infiniteRepeatable(tween(900))`, and is **static** when `LocalReducedMotion.current`.

### 2.18 `Refresh.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
)
```
Wraps `PullToRefreshBox` with an indicator tinted `colors.accent` on a `glassChild` circle. Used by Home, Live, Movies, Series, Guide, Favorites, Series detail.

### 2.19 `SwipeAction.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreSwipeToDismissRow(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Delete,
    label: String? = null,
    background: Color? = null,          // defaults to colors.danger
    enabled: Boolean = true,
    content: @Composable () -> Unit,
)
```
`SwipeToDismissBox` with `EndToStart` only, threshold 45% of width, haptic on threshold cross. It calls `onDismissed` immediately; the **caller** is responsible for showing an undo snackbar and re-inserting on undo.

### 2.20 `LanguageSelector.kt`

```kotlin
data class AreLanguageOption(val tag: String, val nativeNameRes: Int)
val AreLanguageOptions: List<AreLanguageOption>   // 24 entries, unchanged content

@Composable
fun AreLanguageList(
    selectedTag: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
)
```
A `LazyColumn` of `AreListRow(title = nativeName, trailing = check when selected)` — **not** a chip `FlowRow`. 24 locales do not fit as chips on a handset. The old `AreLanguageSelector` name is gone.

### 2.21 `GuideCell.kt`

```kotlin
@Composable
fun AreGuideCell(
    title: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    live: Boolean = false,
    now: Boolean = false,
    catchup: Boolean = false,
    progress: Float = 0f,
    onLongClick: (() -> Unit)? = null,
)
```
`onFocusChange` and `width` are deleted. The cell fills the width it is given by the caller (Guide is a vertical agenda list now, §4.7). `now = true` → accent hairline border + `progress` bar.

### 2.22 `PlayerControls.kt` — full rewrite, 45 params → 24

```kotlin
@Composable
fun ArePlayerControls(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    live: Boolean = false,
    playing: Boolean = true,
    buffering: Boolean = false,
    position: () -> Long = { 0L },       // ms
    duration: () -> Long = { 0L },       // ms; 0 == unknown/live
    buffered: () -> Long = { 0L },       // ms
    onSeek: (Long) -> Unit = {},         // fired on drag END only
    onSeeking: (Long) -> Unit = {},      // fired continuously while dragging (preview label)
    onPlayPause: () -> Unit = {},
    onBack: () -> Unit = {},
    onGoLive: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onSubtitles: (() -> Unit)? = null,
    subtitlesActive: Boolean = false,
    onAudioTrack: (() -> Unit)? = null,
    onPlaybackSpeed: (() -> Unit)? = null,
    playbackSpeedLabel: String? = null,
    onAspectRatio: (() -> Unit)? = null,
    onPictureInPicture: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
)
```
- Three zones on a `glassSurface(elevated = true)` scrim: **top** = back + title/subtitle + PiP; **centre** = 56dp previous / 72dp play-pause / 56dp next (`buffering` replaces play-pause with a spinner); **bottom** = seek row (`elapsed` — `Slider` — `duration`) then a single row of secondary `AreIconButton`s (subtitles, audio, speed, aspect, more). At most **5** secondary buttons; anything else goes behind `onMore` → a bottom sheet. No `horizontalScroll` row of 13 buttons.
- The seek bar is a **real `Slider`** with drag: `onValueChange` → `onSeeking`, `onValueChangeFinished` → `onSeek`. `live && duration() == 0L` → the slider is replaced by a LIVE `AreBadge` + optional `onGoLive` button.
- `slots`/`swapped`/`editSlot`/`HudControl`/`HudGroup`/`HudSlot`/`DEFAULT_HUD_LAYOUT`/`onMultiView`/`onAddToMultiView`/`onOpenGuide`/`onUpNext`/`onToggleRecord` are all **deleted**. `HudLayout.kt` is deleted.
- No `CompositionLocalProvider(LocalLayoutDirection provides Ltr)` — the transport row is LTR by convention, so wrap **only the centre transport row** in it, not the whole HUD.
- Auto-hide: the caller owns visibility; this composable animates in/out via `AnimatedVisibility(fadeIn/fadeOut(tween(motion.durFastMs)))` supplied by the caller.

### 2.23 `RecordingIndicator.kt`

```kotlin
@Composable
fun RecordingIndicator(
    reconnecting: Boolean,
    elapsedMs: Long,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
)
```
Unchanged apart from the `Text` import swap. Keeps its deliberate theme independence.

### 2.24 Components that are DELETED with no replacement

`AreCategoryCard`, `AreCategoryRow`, `AreContinueCard`, `AreHero`, `AreStepIndicator`, `AreTabs`(old `Tabs.kt` + `TabItem`), `AreSegmentedControl`, `AreTileActionDialog`, `AreDialog`, `AreInteractive`, `AreInteractiveSurface`, `AreTouchable`, `AreLanguageSelector`, `tileMarquee`, `AmbientArtworkSettleMs`, `FocusScrollSettleMs`. No phone screen calls any of them, or their call site is replaced by something named above.

### 2.25 Components kept verbatim (move + import swap only)

`ClockFormat.kt` (`rememberClockFormatter()`), `ParentalBlur.kt` (`ParentalBlurState`, `LocalParentalBlur`, `ParentalLockOverlay`).

---

## 3. TOUCH INTERACTION CONVENTIONS

### 3.1 What replaces each TV affordance

| TV affordance | Phone replacement | Where it lives |
|---|---|---|
| Accent focus ring + glow | **Material ripple**, `indication = ripple(...)` on every touch surface | `Modifier.areTouch` (§2.1) |
| `motion.focusScale` 1.06× on focus | nothing. Focus scale is never applied on phone. | — |
| `motion.pressScale` 0.97 on press | **kept** — it is the brand press feel, layered under the ripple. Disabled (`disableScale = true`) on tiles, tabs and full-width rows, where the ripple alone reads better. | `Modifier.areTouch` |
| `BringIntoViewRequester` + `FocusScrollSettleMs` delay | nothing for scroll; for the one case that needs it (`AreTabs(scrollable = true)`) an explicit `animateScrollTo` on selection change. | `Tabs.kt` |
| `FocusRequester` / `focusProperties` / `focusGroup` | nothing. Zero `FocusRequester` in the rewritten UI, except `AreTextField` auto-focus where a screen explicitly wants the keyboard up (Search only). | — |
| hold-OK `onLongClick` | **long press** + `HapticFeedbackType.LongPress` → opens `AreTileActionSheet` | §2.11, §2.19 |
| D-pad Left/Right seeking on a focused seek bar | **`Slider` drag** + double-tap-to-seek gesture zones | §2.22, §3.6 |
| TV modal card (`AreDialog`) | `ModalBottomSheet` (choices/pickers/long text) or `AlertDialog` (destructive confirm) | §3.3 |
| `LocalMinTouchTarget` CompositionLocal | intrinsic `minTouchTarget: Dp = 48.dp` parameter | §2.1 |
| `LocalAmbientArtwork` publish | deleted | — |
| marquee on focused title | 2-line ellipsis | §2.7 |

### 3.2 Touch targets

- **Every interactive element has a ≥48×48dp touch area.** Visual size may be smaller; `areTouch` inflates the touch layer with `defaultMinSize` outside the background.
- Never `Box(Modifier.size(40.dp).clickable {})`. That pattern currently appears in Recordings (×2), Streams (×3) and Series detail — all of it is replaced by `AreIconButton(size = Small)`.
- List rows are ≥56dp. Sheet rows are ≥56dp. Grid tiles are naturally larger.
- Adjacent targets are separated by ≥8dp.

### 3.3 Bottom sheet vs dialog — the decision rule

Use `AreBottomSheet` / `AreChoiceSheet` / `AreTileActionSheet` when the interaction is **a choice, a picker, a long document, or a set of contextual actions**. Sheets are thumb-reachable and dismiss by drag.

Use `AreAlertDialog` **only** when the action is destructive or blocking AND the copy is short: exit-app confirm, delete recording, clear cache, clear history, reset to defaults, language-restart confirm, PIN entry.

Concrete mapping of today's 9 TV dialogs:

| Today | Becomes |
|---|---|
| MainActivity exit confirm | `AreAlertDialog(destructive = false)` |
| Settings language picker | `AreBottomSheet` + `AreLanguageList` |
| Settings language restart confirm | `AreAlertDialog` (copy still rendered in the target language) |
| Settings storage confirms (×3) | `AreAlertDialog(destructive = true)` |
| Settings legal document | `AreBottomSheet` (full-height, scrolling) |
| `ParentalPinDialog` | `AreAlertDialog(content = { pin field })` — blocking by nature |
| Recordings delete confirm | **deleted** — replaced by swipe-to-dismiss + undo snackbar |
| Streams rename | `AreBottomSheet` with a single `AreTextField` + Save |
| Every Settings chip row | `AreChoiceRow` → `AreChoiceSheet` |

Sheets are hosted by the screen that owns them, driven by a `rememberSaveable` nullable state (`var sheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }`). Never nest a sheet inside a sheet.

### 3.4 Swipe

- **Swipe-to-delete** (`AreSwipeToDismissRow`, end→start only): Recordings rows, Streams history rows, Favorites entries. Always paired with a snackbar carrying `CoreR.string.common_undo`; the ViewModel exposes a `restore(...)` that re-inserts the removed row.
- **Swipe between tabs** (`HorizontalPager`): Favorites (Channels/Movies/Series), Search scopes (All/Live/Movies/Series). The pager and `AreTabs` are bidirectionally bound: `LaunchedEffect(pagerState.currentPage)` updates the tab, `onSelect` calls `pagerState.animateScrollToPage`.
- Settings panes are **not** a pager — they become sub-screens (§4.10).
- No swipe on grids or rails; horizontal scroll in a rail must not fight the vertical page scroll (a `LazyRow` inside a `LazyColumn` already handles this).

### 3.5 Long press

Long press on `ArePosterTile` / `AreChannelTile` opens `AreTileActionSheet` with, in order: Play, Play from start (only when `progress > 0`), Add/Remove favourite, Details. Haptic feedback fires on the long-press detection, before the sheet animates. Long press is never the only way to reach an action.

### 3.6 Player gestures

- Single tap anywhere: toggle HUD visibility (auto-hide after 3500ms of no interaction; never auto-hides while paused).
- Double tap on the left/right third: seek ∓10s, with a transient `glassChild` ripple badge showing `-10s`/`+10s`. Centre third double tap: play/pause.
- Vertical drag on the left half: brightness (`window.attributes.screenBrightness`). Right half: volume (`AudioManager.STREAM_MUSIC`). Both show a transient vertical `glassTrack` gauge. Both are ignored while the HUD is hidden-and-just-tapped (same gesture pass, use `detectVerticalDragGestures` with a `touchSlop` guard).
- Swipe down from the top quarter with the HUD visible: `onBack` (dismiss the player).
- Pinch: toggles `RESIZE_MODE_ZOOM` ↔ `RESIZE_MODE_FIT`.
- All gesture handlers live in `PlayerScreen.kt`, never inside `ArePlayerControls`.

### 3.7 Pull-to-refresh

`AreRefreshBox` wraps the scrollable root of: Home, Live, Movies, Series, Guide, Favorites, Series detail. The action is the screen's existing reload path (`PlaylistRepository.refresh`-backed VM action for catalog screens; `refreshEpisodes()` for series detail; the XMLTV re-fetch for Guide). Detail screens for movies, Settings, Recordings, Streams and the player have **no** pull-to-refresh.

### 3.8 Scroll behaviour

- Every screen that has a top bar uses `TopAppBarDefaults.enterAlwaysScrollBehavior()` and passes it to `AreScreenScaffold`; the bar hides on scroll down and returns on scroll up.
- The bottom bar (`AppBottomBar`) is **always visible** on tab screens (it is the app's primary nav) and hidden on the player.
- Lazy lists use `contentPadding`, never a bottom `Modifier.padding`, and always reserve `PaddingValues(bottom = 24.dp) + scaffold insets`.
- Text-entry screens (Onboarding, Streams, Search, Live search) apply `Modifier.imePadding()` on the scroll container, and set `keyboardActions` so the IME action does the obvious thing.
- Scrolling a list dismisses the keyboard: `LaunchedEffect(listState.isScrollInProgress) { if (it) focusManager.clearFocus() }`.
- Heterogeneous lazy lists pass `contentType`.

### 3.9 Feedback & state

- Every screen that mutates data hosts a `SnackbarHostState` through `AreScreenScaffold`. Destructive actions that are not confirmed by a dialog MUST show an undo snackbar.
- Every list has four rendered states: loading (skeletons, not a full-screen spinner), empty (`AreEmptyState` with an action when one exists), error (`AreErrorState` with retry), content.
- Paged grids use `androidx.paging.compose.items` / `LazyPagingItems` properly and render `loadState.refresh`/`append` (skeleton grid, append spinner, retry row). **The `(0 until itemCount).toList()` index hack is banned.**
- Motion: 150ms (`durFastMs`) for state cross-fades, 220ms (`durBaseMs`) for enter/exit, `EaseEmph` for anything with an overshoot. Reduced motion collapses these via the token, not via `if` at the call site.

---

## 4. SCREEN-BY-SCREEN BUILD SHEET

Every screen: keep the existing ViewModel's data plumbing. Additive state/actions are listed explicitly; anything not listed does not change. Every screen ends with **zero** `androidx.tv` imports and **zero** `com.arashrahimi46.iptv.ui.components` / `ui.interaction` imports.

### 4.0 Shell — `MainActivity.kt`

Uses: `AreAlertDialog`.
Changes: exit confirm becomes `AreAlertDialog(title = CoreR.string.exit_confirm_title, text = …, confirmLabel = …, dismissLabel = …)`; drop the `Dialog(usePlatformDefaultWidth = false)` wrapper. `playerActive` moves out of the composition body into `val playerActive by remember { derivedStateOf { isPlayerRoute(currentRoute) } }`. Provide `LocalGlassTier` / `LocalReducedMotion` through `AreIptvMobileTheme` (done in Theme.kt, §6.2). Keep `AppCompatActivity`, `enableEdgeToEdge()`, the 4-step boot gate and `onUserLeaveHint` PiP exactly as they are.
Acceptance: back on a tab root shows the exit dialog only when `confirmBeforeExit`; scrim tap dismisses it; rotating with the dialog open keeps it open.

### 4.1 Navigation — `ui/nav/AppNav.kt`

Uses: `AreIconButton`.
Changes:
- `AppBottomBar` is rebuilt on `androidx.compose.material3.NavigationBar` + `NavigationBarItem`, with `containerColor = Color.Transparent` and the bar drawn on `glassSurface(RoundedCornerShape(topStart = radius.xl, topEnd = radius.xl), elevated = true)` + `ProvideOnGlass(true)` + `navigationBarsPadding()`. Indicator colour `colors.accentWash`, selected icon/label `colors.accent`, unselected `colors.textSecondary`. The hand-rolled `Row`/`AreInteractive` version is deleted; the `selectedTab` `remember` workaround is deleted (`NavigationBarItem(selected = currentRoute == tab.route)` is enough because tab destinations are top-level siblings).
- Tab labels move from `mobile.R.string.nav_*` to `CoreR.string.nav_*`. **Mapping:** Home→`nav_home`, Live→`nav_live_tv`, Movies→`nav_movies`, Series→`nav_series`, Settings→`nav_settings`. (`nav_live` does not exist in either module — do not invent it.)
- New routes: `settings/playback`, `settings/subtitles`, `settings/parental`, `settings/about` (§4.10). `search` gains an optional arg-free deep entry from the Home top bar.
- Route table otherwise unchanged, including `player/{kind}/{id}` and both detail routes.
- `SeriesDetailScreen` call gains `onPlayTitle = { navController.navigate(playerRoute("movie", it)) }` (§5).
- Keep `fadeIn/fadeOut(tween(motion.durFastMs))` transitions.
Acceptance: all 5 tabs switch with a Material indicator and ripple; system back from a child screen returns to its parent tab; the bottom bar is hidden only on `player/*`; no `AreInteractive` import remains.

### 4.2 Home — `ui/home/HomeScreen.kt` (+ `HomeViewModel.kt`)

Uses: `AreScreenScaffold`, `AreTopBar`, `AreRail`, `ArePosterTile`, `AreChannelTile`, `AreRefreshBox`, `AreEmptyState`, `AreSkeleton`, `AreIconButton`, `AreTileActionSheet`.
Changes:
- Top bar: title = app name, actions = `AreIconButton(Search → route "search")` and `AreIconButton(CalendarMonth → route "guide")`. **The two `AreButton` quick-action rows are deleted**; Streams / Recordings / Favorites move to Settings shortcuts (they already exist there) and Favorites additionally becomes the trailing "See all" of the favourites rails.
- Body: `AreRefreshBox` → `LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp), verticalArrangement = spacedBy(20.dp))` with **`contentType` per item** (`"rail-poster"` / `"rail-channel"`).
- Rails, in order, each rendered only when non-empty: Continue watching (`ArePosterTile(width = 132.dp, progress = …)`) → Live now (`AreChannelTile(width = 168.dp)`) → For you (`ArePosterTile(width = 132.dp)`) → **one** Favorites rail per kind, retitled `CoreR.string.home_favorite_channels` and `CoreR.string.home_favorite_titles` (new keys — the current duplicate `nav_favorites` titles are a bug), each with `onSeeAll` → `favorites`.
- Loading renders a skeleton rail stack, not a full-screen spinner.
- Long press on any tile → `AreTileActionSheet`.
- VM additions: `fun refresh()` (re-triggers the catalog sample flows / repository refresh) and `val isRefreshing: StateFlow<Boolean>`. Nothing else changes.
Acceptance: pull-to-refresh spins and settles; no rail is titled the same as another; skeletons show on cold start; long press opens the sheet; scrolling collapses the top bar.

### 4.3 Live — `ui/live/LiveScreen.kt` (+ `LiveViewModel.kt`)

Uses: `AreScreenScaffold`(no back), `AreTextField`, `AreTabs(scrollable = true)`, `AreChannelTile`, `AreRefreshBox`, `AreEmptyState`, `AreErrorState`, `AreSkeleton`, `AreTileActionSheet`.
Changes:
- Search field gets `leadingIcon = Search`, `trailingIcon = Close` when non-empty (clears), `keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)`, `imePadding()`, and dismisses the keyboard on scroll.
- Category chips become `AreTabs(options = listOf(null) + categories, scrollable = true, label = { it ?: allLabel })`.
- Grid: `LazyVerticalGrid(GridCells.Adaptive(168.dp), contentPadding = PaddingValues(start = 16, end = 16, top = 12, bottom = 24))`, `AreChannelTile(width = null)`. **Paging via `LazyPagingItems` and `items(pagingItems, key = …)`** — index hack removed. `loadState.refresh is Loading` → skeleton grid; `is Error` → `AreErrorState(onRetry = { pagingItems.retry() })`; append states rendered as a full-span footer.
- VM additions: 250ms debounce on `setQuery` (mirror `SearchViewModel.SEARCH_DEBOUNCE_MS`); `fun refresh()` + `isRefreshing`.
Acceptance: typing does not hit Room per keystroke; the clear button empties the field and restores the grid; empty category shows `AreEmptyState`; retry works after airplane mode.

### 4.4 Movies / Series grid — `ui/movies/VodGridScreen.kt` (+ `VodGridViewModel.kt`)

Uses: `AreScreenScaffold`(no back), `AreTabs(scrollable = true)`, `ArePosterTile`, `AreRefreshBox`, `AreEmptyState`, `AreErrorState`, `AreSkeleton`, `AreTileActionSheet`, `AreIconButton`.
Changes: same paging fix and same category-tab change as Live. Grid `GridCells.Adaptive(120.dp)`, `contentPadding = PaddingValues(start = 16, end = 16, top = 12, bottom = 24)`, `ArePosterTile(width = null)`. Top bar action: `AreIconButton(Search)` → the `search` route (removes the "must go to Home first" trap). VM additions: `fun refresh()` + `isRefreshing`.
Acceptance: both routes reuse the same screen; tapping a series opens `seriesDetail`, a movie opens `movieDetail`; favourite toggle from the tile persists across a tab switch.

### 4.5 Movie detail — `ui/detail/MovieDetailScreen.kt`

Uses: `AreScreenScaffold` (transparent/collapsing), `AreButton`, `AreIconButton`, `AreBadge`, `AreEmptyState`, `AreLoadingState`.
Changes:
- Collapsing hero: `LazyColumn` with the poster item drawn edge-to-edge behind the status bar (`AsyncImage` + a bottom `Brush.verticalGradient(Transparent → colors.bgBase)` scrim). The top bar is `AreTopBar` with a transparent container that fades to `glassSurface` as the hero scrolls away (`TopAppBarDefaults.enterAlwaysScrollBehavior` + `scrollBehavior.state.collapsedFraction`).
- **All hardcoded `Color.Black`/`Color.White` are removed** — the back button is `AreIconButton(variant = Glass)`.
- Actions become a **sticky bottom action bar** (`Box(Modifier.align(BottomCenter))` on `glassSurface(elevated = true)` + `navigationBarsPadding()`): `AreButton(Play, Primary, full weight 1f)` + `AreIconButton(Favorite, Glass, Large)`. One-handed reach.
- Body sections unchanged in content (meta line, ratings, storyline, director, cast) but restyled with `AreSectionHeader`.
Acceptance: light theme is legible; the back button is visible over a bright poster; Play launches `player/movie/{id}`; loading shows `AreLoadingState`, not-found shows `AreEmptyState`.

### 4.6 Series detail — see §5 (full plan).

### 4.7 Guide / EPG — `ui/guide/GuideScreen.kt` (+ `GuideViewModel.kt`)

Uses: `AreScreenScaffold`(back), `AreTabs(scrollable = true)`, `AreGuideCell`, `AreListRow`, `AreRefreshBox`, `AreEmptyState`, `AreBottomSheet`, `AreChannelTile`(no).
Changes — **the two-axis grid is deleted**. Replacement: a vertical **now/next agenda**.
- Top bar title = `CoreR.string.nav_tv_guide`, back arrow, action `AreIconButton(Today)` → jump to now.
- Day selection: `AreTabs(options = GuideDay.entries, scrollable = false)`.
- Category selection: `AreTabs(scrollable = true)` when `groups.size > 1`.
- Body: `LazyColumn` of channel blocks. Each block = a sticky-ish header row (32dp `AsyncImage` logo + channel name + a `AreIconButton(PlayArrow)` that opens the channel) followed by **the current programme and the next two**, each an `AreGuideCell(modifier = fillMaxWidth())`. A trailing `AreListRow(CoreR.string.guide_more_programmes)` expands that channel's remaining slots in-place (`rememberSaveable` set of expanded channel ids). No horizontal scrolling anywhere.
- Tapping a cell opens a **programme bottom sheet** (`AreBottomSheet`) with title, time range, description if present, and actions: Watch channel (always), Watch from start / catch-up (only when `catchup`). This is the fix for "programme cells are dead".
- Pull-to-refresh re-fetches XMLTV for the selected category.
- VM additions: `fun refresh()` + `isRefreshing`; `fun expand(channelId: Long)` is UI-local, not VM. Window widens from +6h to +12h so "next two" is always populated. `GUIDE_CHANNEL_LIMIT` stays 300.
Acceptance: no horizontal scroll gesture exists on the page; every channel shows a NOW cell with a progress bar; tapping a programme opens the sheet; "jump to now" scrolls to the first channel and resets the day to Today.

### 4.8 Search — `ui/search/SearchScreen.kt` (+ `SearchViewModel.kt`)

Uses: `AreScreenScaffold`(back), `AreTextField`, `AreTabs`, `HorizontalPager`, `ArePosterTile`, `AreChannelTile`, `AreEmptyState`, `AreListRow`, `AreTileActionSheet`.
Changes:
- The field auto-focuses on entry (the one sanctioned `FocusRequester`) and shows the keyboard; `imeAction = Search`; clear button; `imePadding()`.
- Scopes become `AreTabs` bound to a `HorizontalPager` (4 pages: All, Live, Movies, Series). Swipe works.
- **Results are one vertical grid per page**, not horizontal rails: Live page = `LazyVerticalGrid(Adaptive(168.dp))` of `AreChannelTile`; Movies/Series pages = `Adaptive(120.dp)` of `ArePosterTile`; All page = a `LazyColumn` with an `AreSectionHeader` + a bounded grid per kind (max 6 each) and a "see all in <scope>" row that switches the pager.
- Recent searches: a `LazyColumn` of `AreListRow(leadingIcon = History, trailing = Close)` shown when the query is shorter than `MIN_QUERY_LENGTH`. VM additions: `val recentQueries: StateFlow<List<String>>`, `fun commitQuery(q: String)`, `fun clearRecent(q: String?)`, persisted in the existing `UserSettings` DataStore under a new preferences key `search_recent` (a `Set<String>`, capped at 8, most-recent-first). This is the only DataStore addition in the whole rewrite and is explicitly sanctioned.
Acceptance: keyboard appears on entry and closes on scroll; swiping between scopes works; a committed query appears in Recents next visit; `search_no_results` renders per scope.

### 4.9 Favorites — `ui/favorites/FavoritesScreen.kt` (+ `FavoritesViewModel.kt`)

Uses: `AreScreenScaffold`(back), `AreTabs`, `HorizontalPager`, `ArePosterTile`, `AreChannelTile`, `AreSwipeToDismissRow`(no — grids), `AreRefreshBox`, `AreEmptyState`, `AreTileActionSheet`.
Changes: tab index moves to `rememberSaveable`; `AreTabs` + `HorizontalPager` for Channels/Movies/Series with swipe. Grids as in Live/Movies. Removing a favourite (tile heart or long-press sheet) shows an **undo snackbar**; VM additions: `fun restoreChannelFavorite(id: Long)` / `fun restoreVodFavorite(title: VodTitle)` (re-toggle, preserving order semantics — the repository already keys on the stable stream key).
Acceptance: swipe changes tab and the tab strip follows; unfavouriting shows undo and undo restores the item to the same list position; rotation preserves the tab.

### 4.10 Settings — `ui/settings/` (split into files, §6.7)

Uses: `AreScreenScaffold`, `AreListRow`, `AreSwitchRow`, `AreChoiceRow`, `AreChoiceSheet`, `AreBottomSheet`, `AreAlertDialog`, `AreButton`, `AreSectionHeader`, `AreLanguageList`, `AreSwitch`.
Changes — **the 5-pane segmented control is deleted.** Settings becomes a root list plus four sub-screens with real back stacks:

- **Root (`settings`)** — a single `LazyColumn`:
  - Section "Library": `AreListRow` → Favorites / Recordings / Streams (existing routes).
  - Section "General": theme `AreChoiceRow`; Refresh catalog `AreListRow(supporting = lastUpdatedLabel, trailing = spinner while refreshing)`; stale window `AreChoiceRow`; start screen `AreChoiceRow`; auto-refresh `AreChoiceRow`; confirm-before-exit `AreSwitchRow`; language `AreListRow(supporting = current native name)` → language sheet.
  - Section "Provider": the read-only Xtream/Stalker panels, restyled as `AreListRow`s with supporting text (expiry logic unchanged).
  - Section "More": `AreListRow` → `settings/playback`, `settings/subtitles`, `settings/parental`, `settings/about`, each with `supporting` = a one-line summary.
  - Section "Storage": Clear cache / Clear history / Reset — each `AreListRow` → `AreAlertDialog(destructive = true)`.
- **`settings/playback`** — hardware decoding `AreSwitchRow`; preferred audio language `AreChoiceRow`; autoplay-next delay `AreChoiceRow`.
- **`settings/subtitles`** — language / size / edge style / colour / font, all `AreChoiceRow`. **`SubtitleColorChoice` labels stop using `choice.name`**; add `CoreR` keys `subtitle_color_white|yellow|cyan|green|magenta|…` matching the enum entries exactly (one key per entry actually present in the enum) and map them in a `@StringRes` extension.
- **`settings/parental`** — lock `AreSwitchRow` (gated on `hasPinSet`); Set/Change PIN `AreListRow` → `ParentalPinDialog`; auto-relock `AreChoiceRow`; locked-content display `AreChoiceRow`; PIN-on-launch `AreSwitchRow`.
- **`settings/about`** — version `AreListRow`; Support / Feedback `AreListRow` (Intent, unchanged); analytics + crash reporting `AreSwitchRow`; Legal `AreListRow` → `AreBottomSheet` with the 15 sections.
- The language flow keeps its exact existing mechanics: pick in a sheet → `AreAlertDialog` whose copy is built with `createConfigurationContext` in the **target** language → apply after the 2-frame `withFrameNanos` delay → `AppCompatDelegate.setApplicationLocales`.
- `ParentalPinDialog` is rebuilt on `AreAlertDialog(content = …)` + the new `AreTextField(visualTransformation = PasswordVisualTransformation(), keyboardOptions = NumberPassword)`; the 4-flow `PinFlow` enum and auto-submit at length 4 are unchanged.
- `SettingsViewModel` is unchanged except it gains nothing. All ~25 flows and setters stay.
Acceptance: system back from a sub-screen returns to the Settings root, not out of Settings; no horizontally-scrolling chip row exists anywhere in Settings; every choice row shows its current value as supporting text; subtitle colours are translated.

### 4.11 Onboarding — `ui/onboarding/OnboardingScreen.kt` (+ VM)

Uses: `AreTabs`, `AreTextField`, `AreButton`, `AreErrorState`.
Changes: source-type selection becomes `AreTabs(options = SourceType.entries, scrollable = false)` (the full-width chips-as-radios are deleted). Fields gain `label`, `imeAction = Next` chained with `FocusManager.moveFocus(Down)` and `Done` on the last field, `imePadding()` on the scroll column, `isError` + `supportingText` for per-field validation. Validation runs **before** `submit()`: URL must parse as an absolute `http(s)` URI; MAC must match `^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$`; name non-blank. The submit button uses `AreButton(loading = state.isSubmitting)` (the separate 18dp spinner is deleted). Keep "Skip for now" but relabel it via the existing key and keep it a Ghost button.
Acceptance: the keyboard never covers the submit button; a malformed URL is rejected inline without a network call; the screen never `curl`s or otherwise probes the provider outside the app's own client.

### 4.12 Player — `ui/player/PlayerScreen.kt` (+ `PlayerViewModel.kt`)

Uses: `ArePlayerControls`, `AreBottomSheet`, `AreLoadingState`, `AreErrorState`, `AreBadge`.
Changes:
- `PlayerView(useController = false)`. All chrome comes from `ArePlayerControls` in an `AnimatedVisibility` driven by a `hudVisible` state with a 3500ms auto-hide (`LaunchedEffect(hudVisible, playing)`), never hiding while paused.
- Immersive: `WindowInsetsControllerCompat(window, view).hide(systemBars)` + `systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` in a `DisposableEffect`, restored on dispose. Orientation: `requestedOrientation = SCREEN_ORIENTATION_SENSOR` on entry, `UNSPECIFIED` on exit (the existing restore `DisposableEffect` finally has a matching setter).
- Gestures per §3.6.
- Track pickers: `onSubtitles` / `onAudioTrack` open an `AreBottomSheet` listing `player.currentTracks` groups for `TRACK_TYPE_TEXT` / `TRACK_TYPE_AUDIO`, applying via `player.trackSelectionParameters`. This is what finally makes the existing `subtitleLanguage`/`preferredAudioLanguage` settings do something: on `load()`, seed `trackSelectionParameters` with `setPreferredTextLanguage(settings.subtitleLanguage)` and `setPreferredAudioLanguage(settings.preferredAudioLanguage)`.
- Speed sheet: 0.5/0.75/1/1.25/1.5/2 → `player.setPlaybackSpeed`. Aspect: cycles `PlayerView.resizeMode` FIT → ZOOM → FILL.
- Prev/next episode wired to §5 step 5.
- Errors: `AreErrorState(message, onRetry = { viewModel.retry() })` over a black background; buffering shows a centred spinner instead of the play button.
- VM additions: `retry()`, `playEpisode(Long)`, `prevEpisodeId`/`nextEpisodeId` in `PlayerUiState`, completion-clear and the `onCleared` progress flush (§5).
Acceptance: HUD auto-hides and returns on tap; double-tap seeks ±10s; the slider scrubs; subtitles picked in the sheet appear; leaving the player restores the system bars and the previous orientation; PiP still works from `onUserLeaveHint`.

### 4.13 Language select — `ui/language/LanguageSelectScreen.kt`

Uses: `AreLanguageList`, `AreButton`, `AreTextField`(optional filter).
Changes: chip grid → `AreLanguageList` (a real scrolling list of native names with a check mark). Add a filter `AreTextField` at the top that filters by native name and by tag. `deviceDefaultLanguageTag()` is extended to cover **all 24 shipped tags** by matching `Locale.getDefault().language` against `AreLanguageOptions` (with the `pt-BR`/`pt-PT` region special case), falling back to `en`. Continue button stays; keep the existing `setLanguageTag` + `AppCompatDelegate.setApplicationLocales` writes verbatim.
Acceptance: a Turkish or Russian device preselects its own language; all 24 locales are reachable by scroll; the choice survives a rotate.

### 4.14 Splash — `ui/splash/SplashScreen.kt`

Changes: `Color.Black` → `colors.bgBase`, the wordmark colour → `colors.textPrimary`, the radial glow → `colors.accent`. Everything else (the 96dp `accentGradientBrush()` mark, the static composition) stays. **Do not** migrate to `installSplashScreen()` in v1 — it changes the boot gate and is out of scope.
Acceptance: light theme shows a light splash; no hardcoded colour remains.

### 4.15 Recordings — `ui/recordings/RecordingsScreen.kt` (lower priority)

Uses: `AreScreenScaffold`(back), `AreSwipeToDismissRow`, `AreListRow`, `AreIconButton`, `AreSectionHeader`, `AreEmptyState`, `RecordingIndicator`.
Changes: the 40dp `Box.clickable` play/delete controls become `AreIconButton(size = Small)`; delete moves to swipe + undo snackbar (the confirm dialog is deleted); group headers become `AreSectionHeader`; `android.R.string.cancel` is replaced by `CoreR.string.common_cancel`. VM addition: `fun restore(recording: Recording)` re-inserting the row for undo (a plain DAO insert of the same entity; if a true restore is impossible, defer the actual delete by 4s behind the snackbar instead — **prefer the deferred delete**, it needs no new DAO method).
Acceptance: swipe deletes with undo; nothing is under 48dp; the screen has a back arrow.

### 4.16 Streams — `ui/streams/StreamsScreen.kt` (lower priority)

Uses: `AreScreenScaffold`(back), `AreTextField(monospace)`, `AreButton`, `AreSwipeToDismissRow`, `AreListRow`, `AreIconButton`, `AreBottomSheet`.
Changes: URL field gains a paste `trailingIcon` (reads `ClipboardManager`) and `imeAction = Go`; rename moves from `AreDialog` to an `AreBottomSheet` with one field; delete moves to swipe + undo (deferred delete); the three 40dp `Box`es collapse into one `AreIconButton(PlayArrow)` plus an overflow `AreIconButton(MoreVert)` → sheet with Rename. URL text stays saved in `rememberSaveable`.
Acceptance: paste fills the field; swipe deletes with undo; rename persists.

---

## 5. SERIES — season / episode / playback plan

Context that is already true and must not be re-derived: a "season" is `episodes.groupBy { it.season }`; there is no season entity. `PlaylistRepository.ensureSeriesEpisodesLoaded(title)` populates `SeriesEpisode` rows once per series (Xtream via `getSeriesInfo`, Stalker via `getSeriesEpisodes` with `streamUrl = null` and `externalId = cmd`, M3U at import time). Playback always goes through `StreamUrlResolver.resolve(source, kind, externalId, storedUrl, series = episode.episode)`. Episode bookmarks key on `seriesEpisodeId`, never the parent title id.

Five gaps are closed. Steps 1–4 are one agent's work (`series` owner); step 5 is the `player` owner's; step 6 is the strings owner's.

### Step 1 — `ui/series/SeriesDetailViewModel.kt`

State becomes:

```kotlin
data class SeriesDetailUiState(
    val loading: Boolean = true,
    val title: VodTitle? = null,
    val episodesBySeason: Map<Int, List<SeriesEpisode>> = emptyMap(),
    val episodesLoaded: Boolean = false,                      // NEW
    val isM3uSeriesWithoutEpisodes: Boolean = false,
    val episodesLoadError: String? = null,
    val selectedSeason: Int? = null,                          // NEW
    val resumeEpisodeId: Long? = null,                        // NEW
    val progressByEpisodeId: Map<Long, Float> = emptyMap(),   // NEW
    val refreshing: Boolean = false,                          // NEW (pull-to-refresh)
)
```

- Set `episodesLoaded = true` in the existing `observeSeriesEpisodes(...).onEach { }` — the first Room emission means Room answered, empty or not.
- Default `selectedSeason` to `episodesBySeason.keys.minOrNull()` on the **first non-empty** emission only; never overwrite a user choice afterwards.
- Add, mirroring `MovieDetailViewModel`: `launch { runCatching { repository.ensureMetadataLoaded(title) }.getOrNull()?.let { e -> _uiState.update { it.copy(title = e) } } }`, concurrent with the episode load. Both writers use atomic `update {}`.
- New members: `fun selectSeason(season: Int)`, `fun refreshEpisodes()` (clears `episodesLoadError`, sets `refreshing`, re-runs `ensureSeriesEpisodesLoaded`), `private suspend fun loadResume(episodes: List<SeriesEpisode>)`.
- `loadResume` needs a bulk lookup. **The only two data-layer additions in this whole rewrite:**
  - `mobile/.../data/db/Daos.kt` → `ContinueWatchingDao`:
    `@Query("SELECT * FROM continue_watching WHERE seriesEpisodeId IN (:episodeIds)") suspend fun findByEpisodes(episodeIds: List<Long>): List<ContinueWatchingEntry>`
  - `mobile/.../data/repository/ContinueWatchingRepository.kt` →
    `suspend fun entriesForEpisodes(episodeIds: List<Long>): Map<Long, ContinueWatchingEntry>`
  Both are additive; no schema change, no migration (the column already exists).
  `resumeEpisodeId` = the entry with max `updatedAtMs`; `progressByEpisodeId[id]` = `positionMs / durationMs` clamped to `0f..1f`, omitted when `durationMs <= 0`.

### Step 2 — new file `ui/series/SeriesPlayTarget.kt`

```kotlin
sealed interface SeriesPlayTarget {
    data class Episode(val episodeId: Long) : SeriesPlayTarget
    /** M3U/degenerate series whose parent row is itself playable — routed via player/movie/{id}. */
    data class WholeTitle(val vodTitleId: Long) : SeriesPlayTarget
}

internal fun resolveSeriesPlayTarget(state: SeriesDetailUiState): SeriesPlayTarget?
```

Resolution order, exactly:
1. `state.resumeEpisodeId` → `Episode(it)`
2. `episodesBySeason` non-empty → lowest season, lowest `episode` → `Episode(it.id)`
3. `state.title?.streamUrl != null` → `WholeTitle(title.id)`  ← **the branch whose absence is today's dead end**
4. else `null` → the Play button is not rendered.

Pure and top-level so it is unit-testable without Compose.

### Step 3 — `ui/series/SeriesDetailScreen.kt`

```kotlin
@Composable
fun SeriesDetailScreen(
    vodTitleId: Long,
    onOpenEpisode: (Long) -> Unit,
    onPlayTitle: (Long) -> Unit,      // NEW
    onBack: () -> Unit,
    viewModel: SeriesDetailViewModel = viewModel(factory = SeriesDetailViewModel.factory(...)),
)
```

Layout, top to bottom, inside `AreScreenScaffold(title = title.name, onBack = onBack, actions = { favourite AreIconButton })` wrapping an `AreRefreshBox(refreshing = state.refreshing, onRefresh = viewModel::refreshEpisodes)` around a `LazyColumn`:
1. **Header item** (`contentType = "header"`) — poster + name + `year · category · genre` + collapsed plot, composed the same way `MovieDetailScreen` does it. **Copy the composition, do not extract a shared component.**
2. **Primary action item** (`contentType = "action"`) — full-width `AreButton`, rendered only when `resolveSeriesPlayTarget(state) != null`. Label: `Episode` + `resumeEpisodeId != null` → `CoreR.string.series_resume_episode` ("Resume S%1$d · E%2$d"); `Episode` otherwise → `CoreR.string.detail_play_episode` ("Play S%1$d · E%2$d"); `WholeTitle` → `CoreR.string.detail_play`. Dispatch: `Episode → onOpenEpisode(id)`, `WholeTitle → onPlayTitle(id)`.
3. **Season selector item** (`contentType = "seasons"`) — `AreTabs(options = seasons, selected = selectedSeason, scrollable = true, label = { season_label })`, suppressed entirely when `seasons.size <= 1`. Wrap the season list in `remember(state.episodesBySeason) { … }` so its identity is stable.
4. **Episode items** (`contentType = "episode"`, `key = { it.id }`) — only `episodesBySeason[selectedSeason]`. `EpisodeRow` keeps `glassWell(RoundedCornerShape(radius.md))`, gains `Modifier.heightIn(min = 56.dp)` + `areTouch` ripple, shows `S{n}E{n}` + name (2 lines), and a thin `LinearProgressIndicator(progressByEpisodeId[it.id])` when `> 0f`, plus a `Check` glyph when `>= 0.95f`.
5. **Empty/loaded branches** replacing today's infinite spinner:
   - `!state.episodesLoaded` → `AreLoadingState(CoreR.string.detail_episodes_loading)`
   - `episodesLoaded && seasons.isEmpty() && title.streamUrl != null` → `AreEmptyState(CoreR.string.detail_m3u_no_episodes)` — now truthful, because Play exists above.
   - `episodesLoaded && seasons.isEmpty()` → `AreEmptyState(CoreR.string.series_no_episodes, actionLabel = retry, onAction = viewModel::refreshEpisodes)`
   - `episodesLoadError != null` → `AreErrorState(it, onRetry = viewModel::refreshEpisodes)`
`contentPadding = PaddingValues(bottom = 32.dp)` plus the scaffold insets.

### Step 4 — `ui/nav/AppNav.kt` (applied by the nav owner, §6.3)

```kotlin
SeriesDetailScreen(
    vodTitleId = id,
    onOpenEpisode = openEpisode,
    onPlayTitle = { navController.navigate(playerRoute("movie", it)) },
    onBack = { navController.popBackStack() },
)
```
`playerRoute("movie", …)` is correct for `WholeTitle`: `PlayerTarget.Movie` reads `VodTitle.streamUrl` and bookmarks by `vodTitleId`, which is exactly the semantics of an episode-less series row.

### Step 5 — `ui/player/PlayerViewModel.kt` (applied by the player owner, §6.6)

1. **Completion clear.** In `startProgressTracking`, replace the unconditional `updateProgress`:
```kotlin
private const val COMPLETION_THRESHOLD = 0.95f
val finished = duration > 0 && position.toFloat() / duration >= COMPLETION_THRESHOLD
if (finished) continueWatchingRepo.clear(vodTitleId, seriesEpisodeId)
else continueWatchingRepo.updateProgress(vodTitleId, seriesEpisodeId, position, duration)
```
   Also flush once in `onCleared()` **before** `player.release()`, so leaving mid-poll never loses up to 5s.
2. **Prev/next episode.** In the `PlayerTarget.Episode` branch, additionally load `db.seriesEpisodeDao().episodeIdsForSeries(episode.seriesTitleId)` (the DAO exists and currently has zero `:mobile` call sites) and publish `prevEpisodeId` / `nextEpisodeId` in `PlayerUiState` from `siblingIds.indexOf(episode.id) ∓ 1`. Expose `fun playEpisode(episodeId: Long) = load(PlayerTarget.Episode(episodeId))` — the existing same-target guard makes it idempotent.
3. **Autoplay next.** When playback reaches `STATE_ENDED` on an Episode target and `settings.autoplayNextDelaySeconds > 0` and `nextEpisodeId != null`, show an "Up next" card (`CoreR.string.series_up_next`) with a countdown and auto-call `playEpisode(nextEpisodeId)`. `0`/off disables it. This is the phone mechanic replacing `:tv`'s D-pad chapter buttons; `ArePlayerControls` gets `onPrevious`/`onNext` wired to the same ids.

### Step 6 — strings (owner: §6.8)

Reusable as-is from `core`: `detail_play_episode`, `detail_season_label`, `detail_episodes_header{,_seasons,_total}`, `detail_m3u_no_episodes`, `detail_episodes_load_error`, `detail_episodes_loading`, `detail_not_found`, `detail_add_to_favorites`, `detail_remove_from_favorites`.
**New keys (3):** `series_no_episodes`, `series_resume_episode` (`"Resume S%1$d · E%2$d"`), `series_up_next`. Positional args must survive reordering in every locale.

### Series verification

`JAVA_HOME=… ./gradlew :mobile:assembleDebug` (aapt validates all locales), then on-device: Xtream series → season tabs → episode plays; M3U grouped series → episodes play; M3U ungrouped series → the Play button plays the parent row; watch >5s → Home "Continue watching" shows the series → tapping it resumes that episode; watch past 95% → the entry leaves Continue watching. **Never `curl` the provider.**

---

## 6. FILE-OWNERSHIP MAP

Nine owners. **No two owners write the same file.** Two files are shared and are written by exactly one owner each, using the requirement lists below.

All paths are relative to `/Users/arash.rahimi/workspace/iptv/` and, inside `:mobile`, to
`mobile/src/main/java/com/arashrahimi46/iptv/`.

### 6.1 Owner A — `components` (must land FIRST; everyone else depends on it)

Creates, all under `mobile/ui/components/`:
`AreTouch.kt`, `Button.kt`, `Chip.kt`, `IconButton.kt`, `Switch.kt`, `Tabs.kt`, `PosterTile.kt`, `ChannelTile.kt`, `Badge.kt`, `StreamHealth.kt`, `Sheet.kt`, `Dialog.kt`, `Scaffold.kt`, `Rail.kt`, `ListRow.kt`, `Empty.kt`, `Refresh.kt`, `SwipeAction.kt`, `LanguageSelector.kt`, `GuideCell.kt`, `PlayerControls.kt`, `RecordingIndicator.kt`, `ClockFormat.kt`, `ParentalBlur.kt`.
Edits: `mobile/ui/components/TextField.kt` (extend to §2.13).
Touches nothing else. Does **not** delete the old tree (that is Owner I).

### 6.2 Owner B — `theme` (shared file #1)

Edits ONLY `mobile/ui/theme/Theme.kt`. Everything every other owner needs from it:
1. Delete `mobileAreInteractiveBinding` and the `LocalAreInteractiveBinding` provider.
2. Delete the `LocalMinTouchTarget` provider and the `LocalAmbientArtwork` stub provider.
3. Provide `LocalGlassTier provides rememberGlassTier()`.
4. Apply `colors.withBlurredBackdrop()` when `tier.hasBackdropBlur`; keep `AccentPreset.BLUE`.
5. Provide `LocalReducedMotion` from `Settings.Global.TRANSITION_ANIMATION_SCALE == 0f`, and provide `LocalAreIptvMotion` accordingly (`AreIptvMotionReduced` when set).
6. Keep the compose-m3 `darkColorScheme`/`lightColorScheme` mapping verbatim (`outline`/`outlineVariant`, not tv's `border`/`borderVariant`).
7. Keep the `corePhoneTypography*` bridge **until Owner I deletes `ui/components/`**, then delete it in the same pass — Owner B leaves a `// TODO(cleanup, Owner I)` comment marking the two constructors.
8. Keep `AreIptvSpacingDefault` as-is (screens simply do not read the TV tokens).
`mobile/ui/theme/Type.kt` is untouched. `ui/theme/*` (the `:core`-tree glass identity) is untouched by everyone.

### 6.3 Owner C — `nav + shell` (shared file #2)

Edits ONLY `mobile/ui/nav/AppNav.kt` and `MainActivity.kt`. Everything every other owner needs from `AppNav.kt`:
- `NavigationBar`-based `AppBottomBar`; `CoreR.string.nav_home|nav_live_tv|nav_movies|nav_series|nav_settings`.
- New destinations: `settings/playback`, `settings/subtitles`, `settings/parental`, `settings/about`.
- `SeriesDetailScreen(..., onPlayTitle = { navController.navigate(playerRoute("movie", it)) })` (§5 step 4).
- `SearchScreen` reachable from the Home top bar and from the Movies/Series top bars.
- `GuideScreen`, `FavoritesScreen`, `RecordingsScreen`, `StreamsScreen`, both detail screens and both settings sub-screens receive `onBack = { navController.popBackStack() }`.
- Unchanged: `player/{kind}/{id}` route + `PlayerTarget` mapping, `isPlayerRoute`, `openTitle`, `openEpisode`, the fade transitions, `startRoute` resolution, `lastUsedTab` persistence.
From `MainActivity.kt`: the exit `AreAlertDialog`, `derivedStateOf` for `playerActive`, unchanged boot gate and PiP.

### 6.4 Owner D — `catalog screens`
Edits: `mobile/ui/home/HomeScreen.kt`, `mobile/ui/home/HomeViewModel.kt`, `mobile/ui/live/LiveScreen.kt`, `mobile/ui/live/LiveViewModel.kt`, `mobile/ui/movies/VodGridScreen.kt`, `mobile/ui/movies/VodGridViewModel.kt`.
(`mobile/ui/home/HomeRailCurator.kt` and `HomeSection.kt` are pure logic — do not touch.)

### 6.5 Owner E — `detail + series`
Edits: `mobile/ui/detail/MovieDetailScreen.kt`, `mobile/ui/detail/MovieDetailViewModel.kt`, `mobile/ui/series/SeriesDetailScreen.kt`, `mobile/ui/series/SeriesDetailViewModel.kt`.
Creates: `mobile/ui/series/SeriesPlayTarget.kt`.
Also edits (the only data-layer touch in the project, §5 step 1): `data/db/Daos.kt` (`ContinueWatchingDao.findByEpisodes`) and `data/repository/ContinueWatchingRepository.kt` (`entriesForEpisodes`). Additive only — no entity, no schema version, no migration.

### 6.6 Owner F — `player`
Edits: `mobile/ui/player/PlayerScreen.kt`, `mobile/ui/player/PlayerViewModel.kt`.
Deletes: `mobile/ui/player/HudLayout.kt`.

### 6.7 Owner G — `settings + onboarding + language + splash`
Edits: `mobile/ui/settings/SettingsScreen.kt` (becomes the root list only), `mobile/ui/settings/ParentalPinDialog.kt`, `mobile/ui/onboarding/OnboardingScreen.kt`, `mobile/ui/onboarding/OnboardingViewModel.kt`, `mobile/ui/language/LanguageSelectScreen.kt`, `mobile/ui/splash/SplashScreen.kt`.
Creates: `mobile/ui/settings/PlaybackSettingsScreen.kt`, `mobile/ui/settings/SubtitleSettingsScreen.kt`, `mobile/ui/settings/ParentalSettingsScreen.kt`, `mobile/ui/settings/AboutSettingsScreen.kt`, `mobile/ui/settings/SettingsLabels.kt` (the `@StringRes` mappers, incl. `SubtitleColorChoice`).
`mobile/ui/settings/SettingsViewModel.kt` is edited only if a sub-screen needs a flow it already lacks — it should not.

### 6.8 Owner H — `strings`
Edits ONLY `core/src/main/res/values/strings.xml` and the 23 `core/src/main/res/values-*/strings.xml`.
Adds (final list; every key in all 24 files):
`home_favorite_channels`, `home_favorite_titles`, `common_see_all`, `common_back`, `common_cancel`, `common_undo`, `common_loading`, `common_retry`, `common_clear`, `common_paste`, `common_more`, `guide_more_programmes`, `guide_jump_to_now`, `guide_programme_watch`, `guide_programme_from_start`, `search_recent`, `search_clear_recent`, `player_seek_forward`, `player_seek_back`, `player_brightness`, `player_volume`, `player_audio_track`, `player_subtitles`, `player_aspect_ratio`, `player_more`, `series_no_episodes`, `series_resume_episode` (`"Resume S%1$d · E%2$d"`), `series_up_next`, `settings_section_library`, `settings_section_general`, `settings_section_provider`, `settings_section_more`, `settings_section_storage`, `onboarding_invalid_url`, `onboarding_invalid_mac`, plus one `subtitle_color_*` key per `SubtitleColorChoice` entry.
Rules: preserve positional args (`%1$d`, `%2$d`) in every locale; escape `'` as `\'`; XML-escape `&`/`<`/`>`; read `docs/translation-guide.md` before touching a locale file; `fa` and `ar` are RTL and are not optional. Audit by diffing each `values-*/` key set against `values/`.

### 6.9 Owner I — `cleanup` (runs LAST, after every other owner's build is green)
Deletes `mobile/src/main/java/com/arashrahimi46/iptv/ui/components/` and `.../ui/interaction/` in full; deletes `mobile/ui/theme/AreTouchable.kt`; removes the `corePhoneTypography*` bridge from `mobile/ui/theme/Theme.kt`; removes `implementation(libs.androidx.tv.material)` from `mobile/build.gradle.kts`; removes `libs.versions.toml`'s `androidx-tv-material` entry **only if `:tv` does not use it** (it does — so leave `libs.versions.toml` alone).
Verifies: `grep -rn "androidx\.tv" mobile/src mobile/build.gradle.kts` returns nothing.

### Ordering

A (components) → then D, E, F, G in parallel → C (nav/shell) and B (theme) can start with A and must land before the first full build → H (strings) runs in parallel throughout, but every new key must exist before the owner that uses it compiles → I last.

---

## 7. Definition of done

1. `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew :mobile:assembleDebug` succeeds (aapt validates all 24 locale files).
2. `grep -rn "androidx\.tv" mobile/` → no hits outside comments; the Gradle dependency is gone.
3. `grep -rn "com\.arashrahimi46\.iptv\.ui\.components\|ui\.interaction" mobile/` → no hits.
4. `git status` shows **no** modified file under `tv/`, and no Kotlin or non-strings resource added under `core/`.
5. Every `values-*/strings.xml` has the same key set as `values/strings.xml`.
6. On-device (phone or phone emulator, not the TV emulator): every screen in §4 renders in both dark and light theme, in `en` and in `fa` (RTL + Vazirmatn), with no clipped labels, no sub-48dp control, and a visible back affordance on every child screen.
7. No provider URL was ever fetched from a shell.
