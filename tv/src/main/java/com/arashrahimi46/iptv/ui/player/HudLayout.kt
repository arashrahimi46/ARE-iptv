package com.arashrahimi46.iptv.ui.player

/**
 * The player HUD's two reorderable button clusters. [TRANSPORT] is the left cluster (playback
 * transport), [UTILITIES] the right cluster (subtitles, audio, record, …). The metadata header and
 * seek bar are NOT part of this model — they stay fixed above the button row.
 */
enum class HudGroup { TRANSPORT, UTILITIES }

/**
 * Every button that can appear in the HUD's button row (block C of [com.arashrahimi46.iptv.ui.components.ArePlayerControls]),
 * as a data-driven catalog so the row can be user-reordered and per-button hidden.
 *
 * - [group] which cluster it lives in.
 * - [locked] core transport (rewind / play-pause / fast-forward) can never be moved or hidden, so
 *   the player can't be made unusable. Locked controls always render first in their default order.
 * - [order] the default position within the group.
 *
 * Whether a control actually appears at runtime still depends on context (nullable callbacks,
 * live vs VOD, catch-up) — the layout only expresses the user's ordering + hide intent on top.
 */
enum class HudControl(val group: HudGroup, val locked: Boolean, val order: Int) {
    REWIND(HudGroup.TRANSPORT, locked = true, order = 0),
    PLAY_PAUSE(HudGroup.TRANSPORT, locked = true, order = 1),
    FAST_FORWARD(HudGroup.TRANSPORT, locked = true, order = 2),
    SKIP_PREVIOUS(HudGroup.TRANSPORT, locked = false, order = 3),
    SKIP_NEXT(HudGroup.TRANSPORT, locked = false, order = 4),
    GO_LIVE(HudGroup.TRANSPORT, locked = false, order = 5),
    PLAYBACK_SPEED(HudGroup.UTILITIES, locked = false, order = 6),
    ASPECT_RATIO(HudGroup.UTILITIES, locked = false, order = 7),
    AUDIO_TRACK(HudGroup.UTILITIES, locked = false, order = 8),
    AUDIO_DELAY(HudGroup.UTILITIES, locked = false, order = 9),
    SUBTITLES(HudGroup.UTILITIES, locked = false, order = 10),
    FAVORITE(HudGroup.UTILITIES, locked = false, order = 11),
    RECORD(HudGroup.UTILITIES, locked = false, order = 12),
    PICTURE_IN_PICTURE(HudGroup.UTILITIES, locked = false, order = 13),
    ADD_MULTIVIEW(HudGroup.UTILITIES, locked = false, order = 14),
    UP_NEXT(HudGroup.UTILITIES, locked = false, order = 15),
    OPEN_GUIDE(HudGroup.UTILITIES, locked = false, order = 16),
}

/**
 * HUD visual style. Scaffolded for the future A/B/C picker; V1 ships only [CINEMATIC] (design
 * option B — the single unified glass bar) and does not expose a picker.
 */
enum class HudStyle { CINEMATIC }

/** One control in a user's HUD layout: its identity + whether the user has hidden it. Locked
 *  controls are always visible regardless of [visible]. */
data class HudSlot(val control: HudControl, val visible: Boolean = true)

/**
 * Most controls a user may switch on at once (locked transport excluded — those are always there
 * and can't be traded away). Enough to keep every genuinely useful control reachable in one press,
 * short of the point where the row becomes a wall of identical glyphs to scan at two metres.
 *
 * This is an editor-side guard rail, not a layout guarantee: the HUD row scrolls, so it renders
 * correctly with any number. Removing this constant would degrade taste, not correctness.
 */
const val MAX_VISIBLE_HUD_CONTROLS = 13

/** Off by default: audio sync is a repair tool for a stream whose track is misaligned, not something
 *  reached for in normal viewing, and it was the least-earning glyph in a crowded row. */
private val HIDDEN_BY_DEFAULT = setOf(HudControl.AUDIO_DELAY)

/** The default HUD button order/visibility — the catalog in its natural order, everything shown
 *  except [HIDDEN_BY_DEFAULT]. */
val DEFAULT_HUD_LAYOUT: List<HudSlot> =
    HudControl.entries.sortedBy { it.order }.map { HudSlot(it, visible = it !in HIDDEN_BY_DEFAULT) }

/**
 * Serializes a HUD layout to a newline-delimited string for DataStore (no kotlinx.serialization in
 * this module), one slot per line: `<CONTROL>|<visible>`. Mirrors [com.arashrahimi46.iptv.ui.home.encodeHomeLayout].
 */
fun encodeHudLayout(slots: List<HudSlot>): String =
    slots.joinToString("\n") { "${it.control.name}|${it.visible}" }

/**
 * Inverse of [encodeHudLayout]. Forward-compatible and self-healing: unknown/malformed lines are
 * skipped, duplicates collapse to first occurrence, and any control missing from the stored value
 * (e.g. one added in a newer app version) is appended in its default position — so the result
 * always contains every [HudControl] exactly once. Blank input yields the default layout.
 */
fun decodeHudLayout(raw: String): List<HudSlot> {
    if (raw.isBlank()) return DEFAULT_HUD_LAYOUT
    val seen = LinkedHashMap<HudControl, HudSlot>()
    raw.split("\n").forEach { line ->
        if (line.isBlank()) return@forEach
        val parts = line.split("|")
        val control = runCatching { HudControl.valueOf(parts[0]) }.getOrNull() ?: return@forEach
        if (seen.containsKey(control)) return@forEach
        val visible = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: true
        seen[control] = HudSlot(control, visible)
    }
    // Append any controls the stored value didn't mention, at their default position.
    HudControl.entries.sortedBy { it.order }.forEach { c -> seen.getOrPut(c) { HudSlot(c) } }
    return seen.values.toList()
}
