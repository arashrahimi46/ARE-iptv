package com.arashrahimi46.iptv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * True when the current content is drawn INSIDE a glass surface (the player HUD bar, a Settings
 * panel, a dialog). Controls read this to decide whether they are a full glass surface or a nested
 * child, because **nesting is contextual, not a variant** (design spec §6).
 *
 * This is the piece V2 got wrong on the first pass: `IconButtonVariant.Glass` was switched to the
 * nested child tint to fix the player HUD's opaque-square defect, which silently made the *top bar's*
 * buttons -- the same variant, but sitting on the page, not on a bar -- render as a 6% white ghost.
 * One variant cannot answer a question that depends on what is behind it. Provide this via
 * [ProvideOnGlass] wherever a glass surface wraps its children.
 */
val LocalOnGlass = staticCompositionLocalOf { false }

/** Mark a subtree as living inside a glass surface, so nested controls pick the child treatment. */
@Composable
fun ProvideOnGlass(onGlass: Boolean = true, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalOnGlass provides onGlass, content = content)
}

/** What a control *means*, independent of which composable renders it. */
enum class ControlTone {
    /** The one call to action on the screen. Solid accent -- deliberately not glass. */
    Primary,

    /** Everything else: toolbar buttons, cards, tabs, chips at rest. */
    Neutral,

    /** Lowest-emphasis affordance; no fill at rest. */
    Ghost,

    /** Destructive. Solid, for the same reason Primary is. */
    Danger,
}

/**
 * The resolved appearance of one control. Every field maps 1:1 onto a [TvFocusable] parameter, so a
 * call site is `val skin = controlSkin(...)` followed by passing the fields through.
 */
@Immutable
data class ControlSkin(
    val fillBrush: Brush?,
    val fillColor: Color,
    val borderBrush: Brush?,
    val borderColor: Color?,
    val content: Color,
    val elevation: Dp,
)

/**
 * **The single funnel for every button, icon button, tab and chip in the app.** Change a rule here
 * and it changes everywhere -- that is the entire point of this function existing, and the reason
 * call sites must not re-derive fills locally.
 *
 * @param selected a *state* (this tab/chip is current), which renders as the glass lens (§6.2).
 *   Distinct from focus, which [TvFocusable] draws on top and this function never touches.
 */
@Composable
fun controlSkin(
    tone: ControlTone,
    selected: Boolean = false,
    disabled: Boolean = false,
    /**
     * True when this control is **one option among several** -- a tab, a chip in a group, a segment.
     * Such a control at rest means "not the current one" and must recede, whereas a plain button is
     * always fully active and keeps primary-weight text. Without this distinction, routing tabs
     * through the funnel promoted every unselected tab label to [AreIptvColors.textPrimary] and the
     * strip lost its hierarchy -- selected and unselected read identically.
     */
    selectable: Boolean = false,
): ControlSkin {
    val c = AreIptvTheme.colors
    val onGlass = LocalOnGlass.current

    // Disabled first: dimming fill and label by the same alpha collapses contrast in light mode (a
    // 40% accent fill under 40% white text is unreadable -- the original "Refresh now" defect). A
    // neutral muted surface plus tertiary text stays legible AND obviously inert in both themes.
    if (disabled) {
        return ControlSkin(
            fillBrush = null,
            fillColor = if (tone == ControlTone.Ghost) Color.Transparent else c.surface3,
            borderBrush = null,
            borderColor = if (tone == ControlTone.Ghost) null else c.borderDefault,
            content = c.textTertiary,
            elevation = 0.dp,
        )
    }

    // Selection outranks tone: a selected neutral chip and a selected tab are the same material.
    if (selected) {
        return ControlSkin(
            fillBrush = accentLensBrush(),
            fillColor = Color.Transparent,
            borderBrush = lensBorderBrush(),
            borderColor = null,
            content = lensContentColor(),
            // No lift: the lens lives inside a track, and a shadow there reads as a hard smudge.
            elevation = 0.dp,
        )
    }

    return when (tone) {
        ControlTone.Primary -> ControlSkin(
            fillBrush = accentGradientBrush(),
            fillColor = Color.Transparent,
            borderBrush = null,
            borderColor = null,
            content = c.accentFg,
            elevation = GlassElevation,
        )

        ControlTone.Danger -> ControlSkin(
            fillBrush = null,
            fillColor = c.danger,
            borderBrush = null,
            borderColor = null,
            content = Color.White,
            elevation = GlassElevation,
        )

        // The glass rule, resolved by CONTEXT rather than by variant (§6): a control on the page is
        // a full glass surface; the same control inside a glass panel is tint + hairline only, and
        // never a second fill -- 55% over a 72% bar compounds to ~87% opacity, which is what made
        // V1's HUD buttons read as opaque squares.
        ControlTone.Neutral -> if (onGlass) {
            ControlSkin(
                fillBrush = null,
                fillColor = c.glassChildTint,
                borderBrush = null,
                borderColor = c.borderGlass,
                content = if (selectable) c.textSecondary else c.textPrimary,
                elevation = 0.dp,
            )
        } else {
            ControlSkin(
                fillBrush = null,
                fillColor = c.surfaceGlass,
                borderBrush = glassBorderBrush(),
                borderColor = null,
                content = if (selectable) c.textSecondary else c.textPrimary,
                elevation = GlassElevation,
            )
        }

        ControlTone.Ghost -> ControlSkin(
            fillBrush = null,
            fillColor = Color.Transparent,
            borderBrush = null,
            borderColor = null,
            content = c.textSecondary,
            elevation = 0.dp,
        )
    }
}
