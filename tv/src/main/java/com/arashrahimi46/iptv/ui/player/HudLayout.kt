package com.arashrahimi46.iptv.ui.player

/**
 * The player HUD's two reorderable button clusters. [TRANSPORT] is the left cluster (playback
 * transport), [UTILITIES] the right cluster (subtitles, audio, record, …). The metadata header and
 * seek bar are NOT part of this model — they stay fixed above the button row.
 */
enum class HudGroup { TRANSPORT, UTILITIES }

/**
 * Which playback context a control can ever appear in. Mirrors the runtime gating in
 * [com.arashrahimi46.iptv.ui.components.ArePlayerControls]'s `renderable` — declared statically here
 * so the layout editor can show each context's real catalog without a player to ask.
 */
enum class HudAvailability { BOTH, LIVE_ONLY, VOD_ONLY }

/**
 * Every button that can appear in the HUD's button row (block C of [com.arashrahimi46.iptv.ui.components.ArePlayerControls]),
 * as a data-driven catalog so the row can be user-reordered and per-button hidden.
 *
 * - [group] which cluster it lives in.
 * - [locked] core transport (rewind / play-pause / fast-forward) can never be moved or hidden, so
 *   the player can't be made unusable. Locked controls always render first in their default order.
 * - [order] the default position within the group.
 * - [availability] the contexts it can ever render in (live TV vs movies/series).
 *
 * Whether a control actually appears at runtime still depends on finer context (nullable callbacks,
 * catch-up, a recordable stream) — the layout only expresses the user's ordering + hide intent on top.
 */
enum class HudControl(
    val group: HudGroup,
    val locked: Boolean,
    val order: Int,
    val availability: HudAvailability = HudAvailability.BOTH,
) {
    REWIND(HudGroup.TRANSPORT, locked = true, order = 0),
    PLAY_PAUSE(HudGroup.TRANSPORT, locked = true, order = 1),
    FAST_FORWARD(HudGroup.TRANSPORT, locked = true, order = 2),
    SKIP_PREVIOUS(HudGroup.TRANSPORT, locked = false, order = 3),
    SKIP_NEXT(HudGroup.TRANSPORT, locked = false, order = 4),
    GO_LIVE(HudGroup.TRANSPORT, locked = false, order = 5, availability = HudAvailability.LIVE_ONLY),
    PLAYBACK_SPEED(HudGroup.UTILITIES, locked = false, order = 6, availability = HudAvailability.VOD_ONLY),
    ASPECT_RATIO(HudGroup.UTILITIES, locked = false, order = 7),
    AUDIO_TRACK(HudGroup.UTILITIES, locked = false, order = 8),
    AUDIO_DELAY(HudGroup.UTILITIES, locked = false, order = 9),
    SUBTITLES(HudGroup.UTILITIES, locked = false, order = 10),
    FAVORITE(HudGroup.UTILITIES, locked = false, order = 11),
    RECORD(HudGroup.UTILITIES, locked = false, order = 12, availability = HudAvailability.LIVE_ONLY),
    PICTURE_IN_PICTURE(HudGroup.UTILITIES, locked = false, order = 13, availability = HudAvailability.LIVE_ONLY),
    ADD_MULTIVIEW(HudGroup.UTILITIES, locked = false, order = 14, availability = HudAvailability.LIVE_ONLY),
    UP_NEXT(HudGroup.UTILITIES, locked = false, order = 15, availability = HudAvailability.LIVE_ONLY),
    OPEN_GUIDE(HudGroup.UTILITIES, locked = false, order = 16, availability = HudAvailability.LIVE_ONLY);

    /** True when this control can ever render in the given context. */
    fun availableIn(live: Boolean): Boolean = when (availability) {
        HudAvailability.BOTH -> true
        HudAvailability.LIVE_ONLY -> live
        HudAvailability.VOD_ONLY -> !live
    }
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
 * and can't be traded away).
 *
 * This is the width the row ACTUALLY has, not a taste judgement. On a 960dp TV the HUD's inner row
 * measures 816dp (960 − 2×48 editor margin − 2×24 panel padding), and N buttons occupy
 * `58N + 29`dp (52dp buttons, 6dp gaps, the 1dp cluster divider, the 12dp cluster gap and 4dp of
 * trailing space). Solving `58N + 29 ≤ 816` gives N ≤ 13 total, i.e. 3 locked + 10 optional.
 *
 * Past this the row starts scrolling and the last glyph is sliced at the panel edge — the defect
 * that used to cut the Guide button in half. Enforcing the real limit is what keeps the HUD the
 * user arranges identical to the HUD that ships.
 */
const val MAX_VISIBLE_HUD_CONTROLS = 10

/**
 * Off by default. The row only fits [MAX_VISIBLE_HUD_CONTROLS] optional buttons, and live TV offers
 * thirteen — so four have to start switched off or the shipped default is itself an overflowing,
 * sliced row. These are the four that earn their slot least in normal viewing:
 *
 * - audio sync is a repair tool for a stream whose track is misaligned, not something reached for;
 * - mini-player, add-to-multi-view and up-next all have another route in (Back, the multi-view
 *   screen, the guide), so hiding them costs reach, not access.
 *
 * All four sit in the editor's tray, one press from being placed.
 */
private val HIDDEN_BY_DEFAULT = setOf(
    HudControl.AUDIO_DELAY,
    HudControl.PICTURE_IN_PICTURE,
    HudControl.ADD_MULTIVIEW,
    HudControl.UP_NEXT,
)

/** The default HUD button order/visibility — the catalog in its natural order, everything shown
 *  except [HIDDEN_BY_DEFAULT]. */
val DEFAULT_HUD_LAYOUT: List<HudSlot> =
    HudControl.entries.sortedBy { it.order }.map { HudSlot(it, visible = it !in HIDDEN_BY_DEFAULT) }

/**
 * A complete HUD arrangement for one playback context: the ordered slots plus whether the two
 * clusters have been swapped (utilities on the left, transport on the right).
 *
 * [slots] always holds every [HudControl] exactly once, including ones irrelevant to the context
 * it's used in — the render and editor layers filter by [HudControl.availableIn]. Keeping the list
 * complete is what lets [decodeHudLayout] self-heal across app versions.
 */
data class HudArrangement(
    val slots: List<HudSlot> = DEFAULT_HUD_LAYOUT,
    val swapped: Boolean = false,
)

/** The untouched factory arrangement. */
val DEFAULT_HUD_ARRANGEMENT = HudArrangement()

/** Marks the swap flag line in the serialized form; not a [HudControl] name, so older builds that
 *  only knew control lines skip it as unknown rather than choking. */
private const val SWAP_TOKEN = "@SWAP"

/**
 * Serializes a HUD arrangement to a newline-delimited string for DataStore (no kotlinx.serialization
 * in this module), one slot per line: `<CONTROL>|<visible>`, preceded by `@SWAP|<bool>` when the
 * clusters are swapped. Mirrors [com.arashrahimi46.iptv.ui.home.encodeHomeLayout].
 */
fun encodeHudLayout(arrangement: HudArrangement): String {
    val slotLines = arrangement.slots.map { "${it.control.name}|${it.visible}" }
    val lines = if (arrangement.swapped) listOf("$SWAP_TOKEN|true") + slotLines else slotLines
    return lines.joinToString("\n")
}

/**
 * Inverse of [encodeHudLayout]. Forward-compatible and self-healing: unknown/malformed lines are
 * skipped, duplicates collapse to first occurrence, and any control missing from the stored value
 * (e.g. one added in a newer app version) is appended in its default position — so the result
 * always contains every [HudControl] exactly once. Blank input yields the default arrangement.
 */
fun decodeHudLayout(raw: String): HudArrangement {
    if (raw.isBlank()) return DEFAULT_HUD_ARRANGEMENT
    var swapped = false
    val seen = LinkedHashMap<HudControl, HudSlot>()
    raw.split("\n").forEach { line ->
        if (line.isBlank()) return@forEach
        val parts = line.split("|")
        if (parts[0] == SWAP_TOKEN) {
            swapped = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: false
            return@forEach
        }
        val control = runCatching { HudControl.valueOf(parts[0]) }.getOrNull() ?: return@forEach
        if (seen.containsKey(control)) return@forEach
        val visible = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: true
        seen[control] = HudSlot(control, visible)
    }
    // Append any controls the stored value didn't mention, at their default position.
    HudControl.entries.sortedBy { it.order }.forEach { c -> seen.getOrPut(c) { HudSlot(c) } }
    return HudArrangement(seen.values.toList(), swapped)
}
