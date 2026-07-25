package com.arashrahimi46.iptv.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreSegmentedControl
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassSurface

/**
 * Real Settings screen. Every control persists through [SettingsViewModel] ->
 * [com.arashrahimi46.iptv.data.settings.UserSettings]' DataStore, read live at each consumer so
 * changes apply without a restart (Language is the documented exception -- Activity recreate).
 *
 * The screen is organized into a horizontal [SettingsTab] strip; only the selected tab's pane
 * composes. Each pane (see SettingsPanes.kt) owns its own local state and dialogs and reads the
 * shared [SettingsViewModel] flows, so the shell here stays thin.
 */

/** Top-level Settings tabs (the horizontal strip). Order = left-to-right on screen. */
enum class SettingsTab(val labelRes: Int) {
    GENERAL(R.string.settings_tab_general),
    DISPLAY(R.string.settings_tab_display),
    PLAYBACK(R.string.settings_tab_playback),
    SUBTITLES(R.string.settings_tab_subtitles),
    PARENTAL(R.string.settings_tab_parental),
    ABOUT(R.string.settings_tab_about),
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(context.applicationContext as android.app.Application),
    )
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing
    val motion = AreIptvTheme.motion
    // rememberSaveable so the chosen tab survives the screen pausing while the player is on top.
    var selectedTab by rememberSaveable { mutableStateOf(SettingsTab.GENERAL) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = spacing.safeX).widthIn(max = 900.dp),
    ) {
        // Title sits high (design: pull the page title up to give the content more room).
        Box(Modifier.padding(top = spacing.sp2))
        Text(text = stringResource(R.string.settings_title), style = AreIptvTheme.typography.h1, color = colors.textPrimary)
        Box(Modifier.padding(top = spacing.sp4))
        SettingsTabStrip(selected = selectedTab, onSelect = { selectedTab = it })
        Box(Modifier.padding(top = spacing.sp5))
        // The visible pane cross-fades + slides slightly when the tab changes, so switching tabs
        // (and its new data appearing) reads as a smooth transition rather than a hard cut. Durations
        // come from the motion tokens, so reduced-motion collapses them. AnimatedContent's own
        // weight(1f) gives each pane a bounded height for its LazyColumn.
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (fadeIn(tween(motion.durBaseMs)) +
                    slideInHorizontally(tween(motion.durBaseMs)) { it / 14 }) togetherWith
                    fadeOut(tween(motion.durFastMs))
            },
            label = "settingsPane",
        ) { tab ->
            when (tab) {
                SettingsTab.GENERAL -> GeneralPane(viewModel, Modifier.fillMaxSize())
                SettingsTab.DISPLAY -> DisplayPane(viewModel, Modifier.fillMaxSize())
                SettingsTab.PLAYBACK -> PlaybackPane(viewModel, Modifier.fillMaxSize())
                SettingsTab.SUBTITLES -> SubtitlesPane(viewModel, Modifier.fillMaxSize())
                SettingsTab.PARENTAL -> ParentalPane(viewModel, Modifier.fillMaxSize())
                SettingsTab.ABOUT -> AboutPane(viewModel, Modifier.fillMaxSize())
            }
        }
    }
}

/** Horizontal tab strip -- a glass segmented control (design §6c). Padded so the focused segment's
 *  accent ring/glow has headroom, then offset back so it lines up with the title/cards. */
@Composable
private fun SettingsTabStrip(selected: SettingsTab, onSelect: (SettingsTab) -> Unit) {
    Row(
        modifier = Modifier
            .offset(x = (-14).dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        AreSegmentedControl(
            options = SettingsTab.entries,
            selected = selected,
            label = { stringResource(it.labelRes) },
            onSelect = onSelect,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Shared row primitives (used by every pane in SettingsPanes.kt) -- internal so panes can call
// them across files. No new control primitives (design principle §2): rows compose the existing
// AreSwitch/AreChip/AreButton in their `control` slot.
// ---------------------------------------------------------------------------------------------

/** Uppercased caption header + a surface1 rounded column -- the group container for each section. */
@Composable
internal fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val colors = AreIptvTheme.colors
    Column(modifier = Modifier.padding(bottom = 34.dp)) {
        Text(
            text = title.uppercase(),
            style = AreIptvTheme.typography.caption,
            color = colors.textTertiary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(
            modifier = Modifier.glassSurface(RoundedCornerShape(AreIptvTheme.radius.xl)),
        ) {
            content()
        }
    }
}

/** Icon tile + title/description + a right-hand `control` slot -- one settings row. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SettingsRow(icon: ImageVector, title: String, desc: String? = null, control: @Composable RowScope.() -> Unit) {
    val colors = AreIptvTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(42.dp).glassSurface(RoundedCornerShape(AreIptvTheme.radius.sm), shadow = false),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AreIptvTheme.typography.label, color = colors.textPrimary)
            if (desc != null) {
                Text(text = desc, style = AreIptvTheme.typography.caption, color = colors.textTertiary)
            }
        }
        control()
    }
}

/** Right-hand control shared by the language rows: current selection as text + a "Change" button. */
@Composable
internal fun SelectionChangeControl(current: String, onChange: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = current, style = AreIptvTheme.typography.label, color = AreIptvTheme.colors.textSecondary)
        AreButton(
            text = stringResource(R.string.settings_change),
            onClick = onChange,
            variant = AreButtonVariant.Secondary,
            size = AreButtonSize.Small,
        )
    }
}

/** True when this last-refresh timestamp is missing or older than [staleWindowDays] (the user's
 * Settings "stale-window" choice, in days). A window of 0 means "never nudge" -- nothing is stale,
 * including a never-refreshed source. Backs both the Settings refresh row and the sidebar badge. */
fun Long?.isStale(staleWindowDays: Long): Boolean =
    staleWindowDays > 0 && (this == null || System.currentTimeMillis() - this > staleWindowDays * 24L * 60 * 60 * 1000)

/** "N days/hours/mins ago" label for the last catalog refresh; [neverRefreshedText] when unset. */
@Composable
internal fun lastUpdatedLabel(ts: Long?, neverRefreshedText: String): String {
    if (ts == null) return neverRefreshedText
    val ago = System.currentTimeMillis() - ts
    val days = ago / (24 * 60 * 60 * 1000)
    val hours = ago / (60 * 60 * 1000)
    val mins = ago / (60 * 1000)
    return when {
        days >= 1 -> if (days == 1L) stringResource(R.string.settings_last_updated_days, days) else stringResource(R.string.settings_last_updated_days_plural, days)
        hours >= 1 -> if (hours == 1L) stringResource(R.string.settings_last_updated_hours, hours) else stringResource(R.string.settings_last_updated_hours_plural, hours)
        mins >= 1 -> stringResource(R.string.settings_last_updated_mins, mins)
        else -> stringResource(R.string.settings_last_updated_now)
    }
}
