package com.arashrahimi46.iptv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.home.HomeSectionEditRow
import com.arashrahimi46.iptv.ui.player.DEFAULT_HUD_LAYOUT
import com.arashrahimi46.iptv.ui.player.HudControl
import com.arashrahimi46.iptv.ui.player.HudGroup
import com.arashrahimi46.iptv.ui.player.HudSlot
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

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

/**
 * The Rearrange-HUD editor (the one net-new feature of the glass initiative). A focus-trapping
 * modal (host it in a real `Dialog`) that lets the user reorder and hide the player-HUD buttons
 * within each cluster, with a live preview. Core transport (rewind/play/fast-forward) is locked and
 * shown as a note, so the player can never be made unusable.
 *
 * Reorder uses the same proven grab->move->drop D-pad model as Home's layout editor
 * ([HomeSectionEditRow]): focus a row, OK to grab, ▲▼ to move within its cluster, OK/Back to drop.
 * Every mutation persists immediately via [onApply] so the change survives an app restart.
 */
@Composable
fun HudLayoutEditor(
    slots: List<HudSlot>,
    onApply: (List<HudSlot>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    // Normalized working layout: locked core first (fixed order), then the editable transport and
    // utility clusters in their current order.
    fun normalize(s: List<HudSlot>): List<HudSlot> {
        val byControl = s.associateBy { it.control }
        val core = HudControl.entries.filter { it.locked }.sortedBy { it.order }.map { byControl[it] ?: HudSlot(it) }
        val tExtra = s.filter { it.control.group == HudGroup.TRANSPORT && !it.control.locked }
        val util = s.filter { it.control.group == HudGroup.UTILITIES }
        return core + tExtra + util
    }

    var working by remember { mutableStateOf(normalize(slots)) }
    var grabbed by remember { mutableStateOf<HudControl?>(null) }
    val focusRequesters = remember { HudControl.entries.associateWith { FocusRequester() } }

    fun commit(next: List<HudSlot>) {
        working = next
        onApply(next)
    }

    fun move(control: HudControl, delta: Int) {
        val transport = control.group == HudGroup.TRANSPORT
        val group = working.filter {
            if (transport) it.control.group == HudGroup.TRANSPORT && !it.control.locked
            else it.control.group == HudGroup.UTILITIES
        }.toMutableList()
        val i = group.indexOfFirst { it.control == control }
        val j = (i + delta).coerceIn(0, group.size - 1)
        if (i == j) return
        group.add(j, group.removeAt(i))
        val core = working.filter { it.control.locked }
        val tExtra = if (transport) group else working.filter { it.control.group == HudGroup.TRANSPORT && !it.control.locked }
        val util = if (transport) working.filter { it.control.group == HudGroup.UTILITIES } else group
        commit(core + tExtra + util)
    }

    fun toggleHidden(control: HudControl) {
        commit(working.map { if (it.control == control) it.copy(visible = !it.visible) else it })
    }

    val transportExtra = working.filter { it.control.group == HudGroup.TRANSPORT && !it.control.locked }
    val utilities = working.filter { it.control.group == HudGroup.UTILITIES }

    // Focus the first editable row on open so the user can grab immediately.
    val firstEditable = (transportExtra + utilities).firstOrNull()?.control
    LaunchedEffect(Unit) { firstEditable?.let { runCatching { focusRequesters.getValue(it).requestFocus() } } }

    @Composable
    fun editRow(slot: HudSlot, index: Int, total: Int) {
        HomeSectionEditRow(
            label = hudControlLabel(slot.control),
            position = index,
            total = total,
            hidden = !slot.visible,
            grabbed = grabbed == slot.control,
            focusRequester = focusRequesters.getValue(slot.control),
            onGrabToggle = { grabbed = slot.control },
            onMove = { delta -> move(slot.control, delta) },
            onDrop = { grabbed = null },
            onToggleHidden = { toggleHidden(slot.control) },
            content = {},
        )
    }

    AreDialog(onDismiss = onDismiss, title = stringResource(R.string.settings_rearrange_hud), width = 1200.dp) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(text = stringResource(R.string.hud_editor_hint), style = AreIptvTheme.typography.caption, color = colors.textTertiary)
            Box(Modifier.height(12.dp))
            Text(text = stringResource(R.string.hud_editor_locked_note), style = AreIptvTheme.typography.caption, color = colors.textTertiary)

            Box(Modifier.height(20.dp))
            Text(text = stringResource(R.string.hud_editor_transport_group), style = AreIptvTheme.typography.label, color = colors.textSecondary)
            transportExtra.forEachIndexed { i, slot -> editRow(slot, i, transportExtra.size) }

            Box(Modifier.height(20.dp))
            Text(text = stringResource(R.string.hud_editor_utilities_group), style = AreIptvTheme.typography.label, color = colors.textSecondary)
            utilities.forEachIndexed { i, slot -> editRow(slot, i, utilities.size) }

            Box(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AreButton(text = stringResource(R.string.hud_editor_reset), onClick = { commit(DEFAULT_HUD_LAYOUT) }, variant = AreButtonVariant.Ghost)
                AreButton(text = stringResource(R.string.hud_editor_done), onClick = onDismiss, variant = AreButtonVariant.Primary)
            }
        }
    }
}
