package com.arashrahimi46.iptv.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreIconButton
import com.arashrahimi46.iptv.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChipSize
import com.arashrahimi46.iptv.ui.components.ArePlayerControls
import com.arashrahimi46.iptv.ui.player.HudArrangement
import com.arashrahimi46.iptv.ui.player.DEFAULT_HUD_ARRANGEMENT
import com.arashrahimi46.iptv.ui.player.HudControl
import com.arashrahimi46.iptv.ui.player.MAX_VISIBLE_HUD_CONTROLS
import com.arashrahimi46.iptv.ui.player.HudGroup
import com.arashrahimi46.iptv.ui.player.HudSlot
import com.arashrahimi46.iptv.ui.theme.AmbientBackdrop
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import kotlinx.coroutines.delay

/** Human-readable label for a HUD control in the rearrange editor. Reuses the already-translated
 *  player_* strings where a clean noun exists; a few need a dedicated label. */
@Composable
fun hudControlLabel(control: HudControl): String = when (control) {
    HudControl.REWIND -> stringResource(R.string.player_rewind)
    HudControl.PLAY_PAUSE -> stringResource(R.string.player_play)
    HudControl.FAST_FORWARD -> stringResource(R.string.player_fast_forward)
    HudControl.SKIP_PREVIOUS -> stringResource(R.string.hud_ctl_skip_previous)
    HudControl.SKIP_NEXT -> stringResource(R.string.hud_ctl_skip_next)
    HudControl.GO_LIVE -> stringResource(R.string.guide_catchup_go_live)
    HudControl.PLAYBACK_SPEED -> stringResource(R.string.player_playback_speed)
    HudControl.ASPECT_RATIO -> stringResource(R.string.player_aspect_ratio)
    HudControl.AUDIO_TRACK -> stringResource(R.string.player_audio_track)
    HudControl.AUDIO_DELAY -> stringResource(R.string.player_audio_delay)
    HudControl.SUBTITLES -> stringResource(R.string.player_subtitles)
    HudControl.FAVORITE -> stringResource(R.string.hud_ctl_favorite)
    HudControl.RECORD -> stringResource(R.string.hud_ctl_record)
    HudControl.PICTURE_IN_PICTURE -> stringResource(R.string.player_mini_player)
    HudControl.ADD_MULTIVIEW -> stringResource(R.string.multiview_add_to)
    HudControl.UP_NEXT -> stringResource(R.string.player_up_next_action)
    HudControl.OPEN_GUIDE -> stringResource(R.string.player_open_guide)
}

/** The glyph each control wears. Mirrors the icons [ArePlayerControls] renders; the editor draws
 *  its own proxies (they're grab targets, not live controls) so state-dependent faces —
 *  play/pause, filled/outline favourite, the red REC dot — settle on their resting form. */
private fun hudControlIcon(control: HudControl): ImageVector = when (control) {
    HudControl.REWIND -> Icons.Filled.FastRewind
    HudControl.PLAY_PAUSE -> Icons.Filled.PlayArrow
    HudControl.FAST_FORWARD -> Icons.Filled.FastForward
    HudControl.SKIP_PREVIOUS -> Icons.Filled.SkipPrevious
    HudControl.SKIP_NEXT -> Icons.Filled.SkipNext
    HudControl.GO_LIVE -> Icons.Filled.LiveTv
    HudControl.PLAYBACK_SPEED -> Icons.Filled.Speed
    HudControl.ASPECT_RATIO -> Icons.Filled.AspectRatio
    HudControl.AUDIO_TRACK -> Icons.Filled.Audiotrack
    HudControl.AUDIO_DELAY -> Icons.Filled.Sync
    HudControl.SUBTITLES -> Icons.Filled.ClosedCaption
    HudControl.FAVORITE -> Icons.Filled.Favorite
    HudControl.RECORD -> Icons.Filled.FiberManualRecord
    HudControl.PICTURE_IN_PICTURE -> Icons.Filled.PictureInPicture
    HudControl.ADD_MULTIVIEW -> Icons.Filled.AddToQueue
    HudControl.UP_NEXT -> Icons.Filled.Schedule
    HudControl.OPEN_GUIDE -> Icons.Filled.GridView
}

/** What the user currently has picked up. Nothing, one button, or — after escalating with a second
 *  hold — a whole cluster, which moves as a block by swapping the two clusters' sides. */
private sealed interface Grab {
    data object None : Grab
    data class One(val control: HudControl) : Grab
    /** [anchor] is the button the escalation started from. A swap rebuilds the row and Compose drops
     *  focus off the moved node, so focus is re-requested onto the anchor -- otherwise it lands on
     *  whatever composes first (the tray) and the user's hand leaves the thing they're holding. */
    data class Whole(val group: HudGroup, val anchor: HudControl) : Grab
}

/** The control focus should chase after a mutation reshuffles the row. */
private val Grab.focusAnchor: HudControl?
    get() = when (this) {
        is Grab.One -> control
        is Grab.Whole -> anchor
        Grab.None -> null
    }

/**
 * The Rearrange-HUD editor: a direct-manipulation, what-you-see-is-what-you-get surface. Rather than
 * a list of names, it renders the REAL player HUD ([ArePlayerControls] in edit mode) over the app's
 * ambient backdrop, with a tray above it holding the buttons that are switched off. Every change is
 * reflected in the HUD instantly and persisted immediately, so the thing being arranged and the
 * thing that ships are the same object.
 *
 * Gestures (D-pad):
 * - OK or hold on a button picks it up. Held again while grabbed, it escalates to the whole cluster.
 * - Grabbed single: ◀▶ reorders it within its cluster, ▲ lifts it into the tray (hides it).
 * - Grabbed cluster: ◀▶ swaps which cluster sits left and which right.
 * - In the tray: OK or ▼ drops the button back at the end of its own cluster.
 * - OK or Back drops the grab; Back with nothing grabbed leaves the editor.
 *
 * Live TV and movies/series keep separate arrangements ([live]) because the two contexts genuinely
 * offer different controls, and each shows only the catalog it can actually render.
 */
@Composable
fun HudLayoutEditor(
    liveArrangement: HudArrangement,
    vodArrangement: HudArrangement,
    onApply: (live: Boolean, HudArrangement) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    var live by remember { mutableStateOf(true) }
    var grab by remember { mutableStateOf<Grab>(Grab.None) }
    // State HOLDERS, not delegated vars: these two are written on every focus step, and a `by`
    // delegate read in this function's body would make each step invalidate the whole editor --
    // including the 45-argument [ArePlayerControls] -- to retitle one line of text. Passed down to
    // [EditorStatus], which is the only thing that reads them, so focus travel recomposes two Texts.
    val focusedControl = remember { mutableStateOf<HudControl?>(null) }
    // Set when the user acts on something that can't move, so the hint line can say why instead of
    // the press reading as a dead button.
    val refusal = remember { mutableStateOf<String?>(null) }
    // The button that just moved between the row and the tray. Focus chases it so the user's hand
    // follows the thing they moved -- otherwise a mutation that rebuilds the row drops focus and it
    // reappears wherever composes first, with the label naming the wrong control.
    var focusChase by remember { mutableStateOf<HudControl?>(null) }

    // The editor OWNS the arrangement while it is open; the DataStore write is a debounced echo, not
    // the render path. It used to be the other way round: every ◀▶ press called through to
    // `dataStore.edit {}` and the row did not move until that suspend write had hit disk and come
    // back out through the settings Flow. That is tens of milliseconds of dead time per press on a TV
    // SoC, arriving on an arbitrary frame -- so the reorder never landed in the same frame as the
    // keypress and the lift animation, which is exactly the lag and the stutter being reported.
    var liveArr by remember { mutableStateOf(liveArrangement) }
    var vodArr by remember { mutableStateOf(vodArrangement) }
    val arrangement = if (live) liveArr else vodArr
    val focusRequesters = remember { HudControl.entries.associateWith { FocusRequester() } }

    fun commit(next: HudArrangement) {
        if (live) liveArr = next else vodArr = next
    }

    // Debounced persist: a burst of reorder presses writes once, after the user pauses. Each effect
    // is keyed on its own arrangement, so a new press cancels the pending write and restarts it.
    LaunchedEffect(liveArr) {
        if (liveArr != liveArrangement) { delay(PERSIST_DEBOUNCE_MS); onApply(true, liveArr) }
    }
    LaunchedEffect(vodArr) {
        if (vodArr != vodArrangement) { delay(PERSIST_DEBOUNCE_MS); onApply(false, vodArr) }
    }
    // Closing inside the debounce window must not lose the last move. Flush on the way out, reading
    // the newest values (the effect itself is keyed on Unit so it survives every recomposition).
    val latestLive by rememberUpdatedState(liveArr)
    val latestVod by rememberUpdatedState(vodArr)
    val latestApply by rememberUpdatedState(onApply)
    DisposableEffect(Unit) {
        onDispose {
            latestApply(true, latestLive)
            latestApply(false, latestVod)
        }
    }

    /** Controls this context can ever render — the editor's whole catalog, row plus tray. */
    fun inContext(slots: List<HudSlot>) = slots.filter { it.control.availableIn(live) }

    val visibleCount = inContext(arrangement.slots).count { !it.control.locked && it.visible }
    val atCap = visibleCount >= MAX_VISIBLE_HUD_CONTROLS
    val tray = inContext(arrangement.slots).filter { !it.control.locked && !it.visible }

    /** Reorders [control] within its own cluster. Locked core keeps its fixed lead position, so the
     *  clamp is over the cluster's movable members only. */
    fun move(control: HudControl, delta: Int) {
        val cluster = arrangement.slots.filter {
            it.control.group == control.group && !it.control.locked && it.control.availableIn(live) && it.visible
        }.toMutableList()
        val i = cluster.indexOfFirst { it.control == control }
        val j = (i + delta).coerceIn(0, cluster.lastIndex)
        if (i < 0 || i == j) return
        cluster.add(j, cluster.removeAt(i))
        // Splice the reordered cluster back over the positions its members occupied in the full
        // list, leaving every other slot (other cluster, hidden, other-context) exactly where it was.
        val slotIndices = arrangement.slots.withIndex()
            .filter { (_, s) -> s.control.group == control.group && !s.control.locked && s.control.availableIn(live) && s.visible }
            .map { it.index }
        val next = arrangement.slots.toMutableList()
        slotIndices.forEachIndexed { k, idx -> next[idx] = cluster[k] }
        commit(arrangement.copy(slots = next))
    }

    fun setVisible(control: HudControl, visible: Boolean) {
        commit(
            arrangement.copy(
                slots = arrangement.slots.map { if (it.control == control) it.copy(visible = visible) else it },
            ),
        )
    }

    /** Restores a tray button to the END of its own cluster: move it to just after that cluster's
     *  last visible member so it appears where the user was told it would. */
    fun restoreFromTray(control: HudControl) {
        if (atCap) return
        focusChase = control
        val next = arrangement.slots.filter { it.control != control }.toMutableList()
        val lastOfCluster = next.indexOfLast {
            it.control.group == control.group && it.control.availableIn(live) && it.visible
        }
        val at = if (lastOfCluster >= 0) lastOfCluster + 1 else next.size
        next.add(at, HudSlot(control, visible = true))
        commit(arrangement.copy(slots = next))
    }

    // A move rebuilds the row, which drops focus off the button that moved. Re-request it so the
    // grabbed button stays under the user's thumb across consecutive ◀▶ presses -- and, after a
    // restore, so focus lands ON the button that just came out of the tray rather than nowhere.
    // Keyed on the arrangement itself: the old key built a joined String of all 17 slots on EVERY
    // recomposition just to compare it, when the data class's own equality says the same thing free.
    LaunchedEffect(arrangement, grab) {
        val follow = grab.focusAnchor ?: focusChase
        follow?.let { runCatching { focusRequesters.getValue(it).requestFocus() } }
    }

    val lockedHint = stringResource(R.string.hud_editor_locked_note)
    val capHint = stringResource(R.string.hud_editor_at_cap)

    /**
     * The whole D-pad grammar, resolved against the currently focused control. Returns true when the
     * key was consumed (so focus does NOT move) — which is exactly while something is grabbed.
     */
    fun onKey(control: HudControl, key: Key, inTray: Boolean): Boolean {
        when (val g = grab) {
            is Grab.Whole -> when (key) {
                Key.DirectionLeft, Key.DirectionRight -> {
                    commit(arrangement.copy(swapped = !arrangement.swapped))
                    return true
                }
                // ▲▼ are inert on a cluster: a group grab is a side-swap, not a bulk hide.
                Key.DirectionUp, Key.DirectionDown -> return true
                else -> return false
            }
            is Grab.One -> {
                if (g.control != control) return false
                // A held TRAY button has one move: down, back onto the HUD. Its left/right position
                // in the tray is meaningless (the tray is ordered by the catalog), so those are
                // swallowed rather than acted on.
                if (inTray) {
                    return when (key) {
                        Key.DirectionDown -> {
                            if (atCap) refusal.value = capHint else { restoreFromTray(control); grab = Grab.None }
                            true
                        }
                        Key.DirectionUp, Key.DirectionLeft, Key.DirectionRight -> true
                        else -> false
                    }
                }
                when (key) {
                    Key.DirectionLeft -> { move(control, -1); return true }
                    Key.DirectionRight -> { move(control, +1); return true }
                    Key.DirectionUp -> { setVisible(control, false); focusChase = control; grab = Grab.None; return true }
                    Key.DirectionDown -> return true
                    else -> return false
                }
            }
            // Nothing held: EVERY arrow is plain focus movement. The tray used to treat ▼ as
            // "restore", which meant a non-empty tray was a wall -- pressing down to reach the HUD
            // row just kept putting buttons back instead of moving focus past it. Placement now
            // requires picking the button up first, exactly like the row.
            Grab.None -> return false
        }
    }

    /** Picking a tray button up is refused when the HUD is already full -- holding something you
     *  provably cannot place is a dead end, so the cap is enforced at the grab, not at the drop. */
    fun grabFromTray(control: HudControl) {
        if (atCap) refusal.value = capHint else grab = Grab.One(control)
    }

    fun onSelect(control: HudControl, inTray: Boolean) {
        refusal.value = null
        val heldThis = grab is Grab.One && (grab as Grab.One).control == control
        when {
            heldThis -> grab = Grab.None
            inTray -> grabFromTray(control)
            control.locked -> refusal.value = lockedHint
            // OK on a member of the held cluster drops it, as the group hint promises. Without this
            // the else below would silently demote a group grab back to a single button.
            (grab as? Grab.Whole)?.group == control.group -> grab = Grab.None
            else -> grab = Grab.One(control)
        }
    }

    fun onHold(control: HudControl, inTray: Boolean) {
        refusal.value = null
        when {
            // A tray button has no cluster to escalate into, so a hold is just a pick-up.
            inTray -> if (grab !is Grab.One || (grab as Grab.One).control != control) grabFromTray(control)
            control.locked -> refusal.value = lockedHint
            // Second hold escalates from the single button to its whole cluster.
            grab is Grab.One && (grab as Grab.One).control == control -> grab = Grab.Whole(control.group, control)
            else -> grab = Grab.One(control)
        }
    }

    /** One grabbable proxy: the control's glyph in a focusable well that shows whether it's merely
     *  focused, picked up on its own, or riding along inside a grabbed cluster. */
    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun ControlProxy(control: HudControl, inTray: Boolean) {
        val interaction = remember { MutableInteractionSource() }
        val focused by interaction.collectIsFocusedAsState()
        val heldAlone = grab == Grab.One(control)
        val heldInGroup = (grab as? Grab.Whole)?.group == control.group && !inTray
        val held = heldAlone || heldInGroup
        LaunchedEffect(focused) {
            if (focused) {
                focusedControl.value = control
                // A refusal explains the press that just happened; moving on retires it.
                refusal.value = null
            }
        }
        // FOCUSED vs HELD have to read as different KINDS of thing, not two shades of accent --
        // focus is already an accent ring, so an accent fill alone left the two nearly identical.
        // A held button physically lifts out of the row and casts a shadow: it is in the user's
        // hand, no longer seated with its neighbours. That's a difference in position, which the
        // eye reads instantly from across a room, rather than a difference in colour.
        //
        // Animated as a raw pixel TRANSLATION, not a layout offset. `Modifier.offset {}` defers the
        // read to layout, which still re-measures and re-lays-out the whole scrolling button row on
        // every one of the tween's frames; `graphicsLayer` moves the already-rasterized layer and
        // touches neither measure nor layout, so the lift is pure GPU work.
        val liftPx by animateFloatAsState(
            targetValue = if (held) -LIFT_DP.value * LocalDensity.current.density else 0f,
            animationSpec = tween(AreIptvTheme.motion.durFastMs, easing = AreIptvTheme.motion.easeOut),
            label = "hudGrabLift",
        )
        // The proxy IS an AreIconButton -- same component, same rounded-square skin and focus ring as
        // the live HUD renders, so the row being arranged is visually the row that ships. Only the
        // click handlers and the lift differ.
        AreIconButton(
            icon = hudControlIcon(control),
            contentDescription = hudControlLabel(control),
            onClick = { onSelect(control, inTray) },
            onLongClick = { onHold(control, inTray) },
            interactionSource = interaction,
            // Tray buttons are parked, not placed: Ghost drops the glass fill so the HUD row reads
            // as the only real surface.
            variant = if (inTray) AreIconButtonVariant.Ghost else AreIconButtonVariant.Glass,
            // Play/pause carries the accent fill in the live HUD too (it's the one lit control), so
            // the mock has to wear it or the row the user arranges isn't the row they'll see. The
            // lift, not the fill, is what marks a held button -- which is why the two can share
            // `active` without becoming ambiguous.
            active = held || (control == HudControl.PLAY_PAUSE && !inTray),
            modifier = Modifier
                .graphicsLayer { translationY = liftPx }
                .focusRequester(focusRequesters.getValue(control))
                // Preview, not onKeyEvent: while grabbed these presses must be swallowed BEFORE
                // Compose's focus system reads them, or the arrow moves focus away and the grab is
                // lost after a single step.
                .onPreviewKeyEvent { e ->
                    e.type == KeyEventType.KeyDown && onKey(control, e.key, inTray)
                },
        )
    }

    // Back puts down whatever is held instead of leaving the editor -- losing an in-progress move
    // because you wanted to cancel it is the worst possible reading of the key. Only an empty hand
    // falls through to the dialog's dismiss.
    //
    // BackHandler, not onPreviewKeyEvent: a Dialog is dismissed through the back DISPATCHER, so a
    // key-event handler never sees the press and the editor closed with a button still held.
    BackHandler(enabled = grab != Grab.None) { grab = Grab.None }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AmbientBackdrop(Modifier.fillMaxSize())
        // Scrollable, not just padded: the editor grows with the hint line and the tray, and on a
        // shorter panel the Done button must stay reachable rather than falling off the bottom.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Back arrow at the start of the header, the same affordance every other full-screen
            // surface carries (Detail, Multi-view) -- a full screen with no visible way out reads as
            // a trap even when the remote's Back key works.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AreIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onDismiss,
                    variant = AreIconButtonVariant.Glass,
                )
                Text(
                    text = stringResource(R.string.settings_rearrange_hud),
                    style = AreIptvTheme.typography.h2,
                    color = colors.textPrimary,
                )
            }
            Box(Modifier.height(16.dp))

            // Context switch: the two arrangements are stored separately, so this changes which one
            // is being edited, not merely what's previewed.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AreChip(
                    text = stringResource(R.string.hud_editor_context_live),
                    selected = live,
                    onClick = { grab = Grab.None; live = true },
                    size = AreChipSize.Small,
                )
                AreChip(
                    text = stringResource(R.string.hud_editor_context_vod),
                    selected = !live,
                    onClick = { grab = Grab.None; live = false },
                    size = AreChipSize.Small,
                )
            }

            Box(Modifier.height(20.dp))

            // The tray. Always present (even empty) so its role as "where hidden buttons live" is
            // learnable before the user has hidden anything.
            Text(
                text = stringResource(R.string.hud_editor_tray_title),
                style = AreIptvTheme.typography.label,
                color = colors.textSecondary,
            )
            Box(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface1, RoundedCornerShape(AreIptvTheme.radius.lg))
                    // heightIn before padding: a fixed .height() applied AFTER padding leaves only
                    // 36dp of content box and crops the 52dp buttons.
                    .heightIn(min = 68.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (tray.isEmpty()) {
                    Text(
                        text = stringResource(R.string.hud_editor_tray_empty),
                        style = AreIptvTheme.typography.caption,
                        color = colors.textTertiary,
                    )
                } else {
                    // Keyed for the same reason as the HUD row: hiding or restoring a button
                    // reshuffles the tray, and unkeyed children would hand the departing button's
                    // focus state to its neighbour.
                    tray.forEach { key(it.control) { ControlProxy(it.control, inTray = true) } }
                }
            }

            Box(Modifier.height(20.dp))

            // The real HUD, in edit mode. Artwork/title/seek bar render as they always do; only the
            // button row is swapped for grabbable proxies.
            ArePlayerControls(
                // The sample metadata follows the context: a movie has no channel and no "on now",
                // and its artwork well is the tall poster shape, not the channel-logo square.
                title = stringResource(
                    if (live) R.string.hud_editor_sample_title else R.string.hud_editor_sample_title_vod,
                ),
                subtitle = stringResource(
                    if (live) R.string.hud_editor_sample_subtitle else R.string.hud_editor_sample_subtitle_vod,
                ),
                artworkIsPoster = !live,
                live = live,
                playing = true,
                // Hoisted constants, not fresh lambdas per recomposition. (`editSlot` below cannot
                // be remembered the same way: it closes over `arrangement`, a plain val recomputed
                // each composition, so a cached lambda would keep handing out a stale layout.)
                position = SAMPLE_POSITION,
                buffered = SAMPLE_BUFFERED,
                slots = arrangement.slots,
                swapped = arrangement.swapped,
                editSlot = { control -> ControlProxy(control, inTray = false) },
            )

            Box(Modifier.height(16.dp))

            EditorStatus(
                anchor = grab.focusAnchor,
                focusedControl = focusedControl,
                refusal = refusal,
                hint = when (grab) {
                    is Grab.Whole -> stringResource(R.string.hud_editor_hint_group)
                    // A held tray button can only go one way -- down onto the HUD -- so it gets its
                    // own line rather than the row's move/hide grammar, none of which applies.
                    is Grab.One ->
                        if (tray.any { it.control == (grab as Grab.One).control }) {
                            stringResource(R.string.hud_editor_hint_grabbed_tray)
                        } else {
                            stringResource(R.string.hud_editor_hint_grabbed)
                        }
                    Grab.None -> stringResource(R.string.hud_editor_hint_idle)
                },
            )

            Box(Modifier.height(12.dp))
            // Always visible, not only once full: a budget you can watch fill explains the refusal
            // BEFORE it happens. Turns amber at the cap so a toggle that declines to move isn't
            // mistaken for a dead control.
            Text(
                text = stringResource(R.string.hud_editor_budget, visibleCount, MAX_VISIBLE_HUD_CONTROLS),
                style = AreIptvTheme.typography.caption,
                color = if (atCap) colors.warning else colors.textTertiary,
            )

            Box(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AreButton(
                    text = stringResource(R.string.hud_editor_reset),
                    onClick = { grab = Grab.None; commit(DEFAULT_HUD_ARRANGEMENT) },
                    variant = AreButtonVariant.Ghost,
                )
                AreButton(
                    text = stringResource(R.string.hud_editor_done),
                    onClick = onDismiss,
                    variant = AreButtonVariant.Primary,
                )
            }
        }
    }
}

/** The mock HUD's fixed sample progress. Hoisted to constants so the editor never hands
 *  [ArePlayerControls] a freshly-allocated lambda and defeat its skipping. */
private val SAMPLE_POSITION: () -> Float = { 0.4f }
private val SAMPLE_BUFFERED: () -> Float = { 0.6f }

/** How far a held button rises out of the row. */
private val LIFT_DP = 14.dp

/** How long the editor waits after the last change before writing to DataStore. */
private const val PERSIST_DEBOUNCE_MS = 350L

/**
 * The two status lines under the HUD: which control is under the cursor, and what the D-pad will do
 * to it.
 *
 * Its own composable, taking the two hot states as [State] holders rather than values, so the reads
 * happen HERE. Every D-pad step writes both of them; when the editor's own body read them, one step
 * of focus travel invalidated the entire editor -- the tray, the 45-argument [ArePlayerControls] and
 * all thirteen glass buttons inside it -- to retitle one line of text. Now it recomposes two Texts.
 */
@Composable
private fun EditorStatus(
    anchor: HudControl?,
    focusedControl: State<HudControl?>,
    refusal: State<String?>,
    hint: String,
) {
    val colors = AreIptvTheme.colors
    // Whatever is held outranks whatever is focused: during a mutation the two can disagree for a
    // frame, and naming the button in the user's hand is never wrong.
    val named = anchor ?: focusedControl.value
    Text(
        text = named?.let { hudControlLabel(it) } ?: "",
        style = AreIptvTheme.typography.h3,
        color = colors.textPrimary,
    )
    val refused = refusal.value
    Text(
        text = refused ?: hint,
        style = AreIptvTheme.typography.caption,
        color = if (refused != null) colors.warning else colors.textTertiary,
    )
}
