package com.arashrahimi46.iptv.mobile.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.model.Channel
import com.arashrahimi46.iptv.mobile.ui.components.AreBottomSheet
import com.arashrahimi46.iptv.mobile.ui.components.AreButton
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreEmptyState
import com.arashrahimi46.iptv.mobile.ui.components.AreSkeleton
import com.arashrahimi46.iptv.mobile.ui.components.AreGuideCell
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButton
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreListRow
import com.arashrahimi46.iptv.mobile.ui.components.AreRefreshBox
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreTabs
import com.arashrahimi46.iptv.mobile.ui.components.LocalParentalBlur
import com.arashrahimi46.iptv.mobile.ui.components.rememberClockFormatter
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** How many programmes a channel block shows before the "More" row: the one on now plus two. */
private const val COLLAPSED_SLOTS = 3

/** The wall clock is only re-read this often -- progress bars and "on now" don't need per-frame
 * precision, and a tighter tick would recompose every visible cell for nothing. */
private const val CLOCK_TICK_MS = 30_000L

/**
 * TV Guide for touch. The two-axis timeline grid is gone: this is a vertical **now/next agenda**,
 * so there is no horizontal scroll gesture anywhere on the page and nothing depends on a D-pad.
 *
 * Each channel is a block -- logo + name + a play button -- followed by the programme on now and
 * the next two, expandable in place. Tapping a programme opens a bottom sheet with its details and
 * the actions that actually apply to it.
 *
 * [onOpenCatchup] is optional: until the phone player has an archive path, the nav layer passes
 * nothing and the "watch from start" action simply isn't offered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    onOpenChannel: (Channel) -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenCatchup: ((Channel, GuideProgramSlot) -> Unit)? = null,
    viewModel: GuideViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = AreIptvTheme.colors
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clock = rememberClockFormatter()
    val blur = LocalParentalBlur.current

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(CLOCK_TICK_MS)
            nowMs = System.currentTimeMillis()
        }
    }

    val expanded = rememberSaveable(saver = expandedSaver) { mutableStateOf(emptySet<Long>()) }
    var sheet by remember { mutableStateOf<GuideSheetTarget?>(null) }

    AreScreenScaffold(
        title = stringResource(CoreR.string.nav_tv_guide),
        onBack = onBack,
        actions = {
            AreIconButton(
                icon = Icons.Filled.Today,
                contentDescription = stringResource(CoreR.string.guide_day_today),
                onClick = {
                    viewModel.selectDay(GuideDay.Today)
                    scope.launch { listState.animateScrollToItem(0) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Loading is checked BEFORE the two empty states, because at first composition the
            // state is legitimately "no source, no rows" and both of those render as errors.
            if (state.isLoading) {
                GuideSkeletons()
                return@Column
            }
            if (!state.hasSource) {
                AreEmptyState(
                    title = stringResource(CoreR.string.guide_no_source),
                    icon = Icons.Outlined.EventBusy,
                )
                return@Column
            }

            AreTabs(
                options = GuideDay.entries.toList(),
                selected = state.day,
                label = { stringResource(it.labelRes) },
                onSelect = viewModel::selectDay,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (state.groups.size > 1) {
                AreTabs(
                    options = state.groups,
                    selected = state.selectedGroup,
                    label = { it },
                    onSelect = viewModel::selectGroup,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    scrollable = true,
                )
            }
            if (state.epgUnavailable) {
                Text(
                    text = stringResource(CoreR.string.guide_epg_unavailable),
                    style = AreIptvTheme.typography.caption,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            AreRefreshBox(
                refreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.rows.isEmpty()) {
                    AreEmptyState(
                        title = stringResource(CoreR.string.live_empty_group),
                        icon = Icons.Outlined.EventBusy,
                    )
                    return@AreRefreshBox
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // The scrollable's own padding, not the root's -- a root inset would shrink the
                    // viewport instead of extending the scrollable content.
                    contentPadding = PaddingValues(top = 8.dp, bottom = 52.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.rows.forEach { row ->
                        val channelId = row.channel.id
                        val isOpen = channelId in expanded.value
                        val visible = visibleSlots(row.slots, nowMs, isOpen)
                        // BLUR mode: the guide draws its own rows rather than AreChannelTile, so it
                        // needs its own lock path. An obscured channel keeps its row (dropping it
                        // would be HIDE semantics, which the view-model handles) but shows only a
                        // blurred header -- its programme titles are as revealing as its name.
                        val obscured = blur.isObscured(row.channel.categoryName)

                        item(key = "h$channelId", contentType = GuideItem.Header) {
                            GuideChannelHeader(
                                channel = row.channel,
                                obscured = obscured,
                                onPlay = if (obscured) blur.onReveal else { { onOpenChannel(row.channel) } },
                            )
                        }
                        if (obscured) return@forEach
                        items(
                            items = visible,
                            key = { "p$channelId-${it.id}-${it.startMs}" },
                            contentType = { GuideItem.Cell },
                        ) { slot ->
                            AreGuideCell(
                                title = slot.title,
                                time = clock.rangeLabel(slot.startMs, slot.endMs),
                                onClick = {
                                    if (!slot.placeholder) sheet = GuideSheetTarget(row.channel, slot)
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                live = slot.isNow(nowMs),
                                now = slot.isNow(nowMs),
                                catchup = slot.catchup,
                                progress = slot.progress(nowMs),
                            )
                        }
                        if (row.slots.size > COLLAPSED_SLOTS) {
                            item(key = "m$channelId", contentType = GuideItem.More) {
                                AreListRow(
                                    title = stringResource(CoreR.string.common_more),
                                    onClick = {
                                        expanded.value = if (isOpen) {
                                            expanded.value - channelId
                                        } else {
                                            expanded.value + channelId
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val target = sheet
    if (target != null) {
        GuideProgrammeSheet(
            target = target,
            timeLabel = clock.rangeLabel(target.slot.startMs, target.slot.endMs),
            onDismiss = { sheet = null },
            onWatch = {
                sheet = null
                onOpenChannel(target.channel)
            },
            onWatchFromStart = if (target.slot.catchup && onOpenCatchup != null) {
                {
                    sheet = null
                    onOpenCatchup(target.channel, target.slot)
                }
            } else {
                null
            },
        )
    }
}

private data class GuideSheetTarget(val channel: Channel, val slot: GuideProgramSlot)

/** `contentType` values so Compose's slot-reuse pool never tries to recycle a header into a cell. */
private enum class GuideItem { Header, Cell, More }

/**
 * Placeholder agenda shown while the first emission resolves. It mirrors the real layout (day
 * strip, then channel-header + two programme cells per channel) so the screen doesn't visibly
 * re-flow when the data lands.
 */
@Composable
private fun GuideSkeletons() {
    val sm = RoundedCornerShape(AreIptvTheme.radius.sm)
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AreSkeleton(
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(AreIptvTheme.radius.pill),
        )
        repeat(5) {
            AreSkeleton(modifier = Modifier.padding(top = 6.dp).size(width = 160.dp, height = 20.dp), shape = sm)
            AreSkeleton(modifier = Modifier.fillMaxWidth().height(56.dp))
            AreSkeleton(modifier = Modifier.fillMaxWidth().height(56.dp))
        }
    }
}

@Composable
private fun GuideChannelHeader(channel: Channel, obscured: Boolean, onPlay: () -> Unit) {
    val colors = AreIptvTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            // Logo and name blur together, the same treatment AreChannelTile gives its plate. The
            // action button stays sharp so the unlock affordance is legible.
            modifier = Modifier
                .weight(1f)
                .then(if (obscured) Modifier.blur(12.dp) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = channel.name,
                style = AreIptvTheme.typography.label,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        AreIconButton(
            icon = if (obscured) Icons.Filled.Lock else Icons.Filled.PlayArrow,
            contentDescription = stringResource(
                if (obscured) CoreR.string.pin_enter_title else CoreR.string.detail_play,
            ),
            onClick = onPlay,
            size = AreIconButtonSize.Small,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuideProgrammeSheet(
    target: GuideSheetTarget,
    timeLabel: String,
    onDismiss: () -> Unit,
    onWatch: () -> Unit,
    onWatchFromStart: (() -> Unit)?,
) {
    val colors = AreIptvTheme.colors
    AreBottomSheet(
        onDismiss = onDismiss,
        title = target.slot.title,
        actions = {
            if (onWatchFromStart != null) {
                AreButton(
                    text = stringResource(CoreR.string.guide_catchup_watch_from_start),
                    onClick = onWatchFromStart,
                    variant = AreButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
            AreButton(
                text = stringResource(CoreR.string.detail_play),
                onClick = onWatch,
                modifier = Modifier.weight(1f),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = target.channel.name,
                style = AreIptvTheme.typography.label,
                color = colors.accent,
            )
            Text(
                text = timeLabel,
                style = AreIptvTheme.typography.mono,
                color = colors.textTertiary,
            )
            val description = target.slot.description
            if (description != null) {
                Text(
                    text = description,
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/**
 * The collapsed block shows the programme on now and the next two. With no "now" (Yesterday /
 * Tomorrow, or a channel whose EPG has a gap) it falls back to the first thing that hasn't ended,
 * and failing that to the top of the list -- a block is never empty.
 */
private fun visibleSlots(slots: List<GuideProgramSlot>, nowMs: Long, expanded: Boolean): List<GuideProgramSlot> {
    if (expanded || slots.size <= COLLAPSED_SLOTS) return slots
    val start = slots.indexOfFirst { it.endMs > nowMs }.let { if (it < 0) 0 else it }
        .coerceAtMost((slots.size - COLLAPSED_SLOTS).coerceAtLeast(0))
    return slots.subList(start, (start + COLLAPSED_SLOTS).coerceAtMost(slots.size))
}

private fun GuideProgramSlot.isNow(nowMs: Long): Boolean =
    !placeholder && nowMs >= startMs && nowMs < endMs

private fun GuideProgramSlot.progress(nowMs: Long): Float {
    if (!isNow(nowMs)) return 0f
    val span = (endMs - startMs).toFloat()
    if (span <= 0f) return 0f
    return ((nowMs - startMs) / span).coerceIn(0f, 1f)
}

private fun DateTimeFormatter.rangeLabel(startMs: Long, endMs: Long): String {
    val zone = ZoneId.systemDefault()
    val from = format(Instant.ofEpochMilli(startMs).atZone(zone))
    val to = format(Instant.ofEpochMilli(endMs).atZone(zone))
    // U+2009 thin spaces around the dash keep the range readable in both LTR and RTL -- the
    // bidi algorithm reorders the whole run as one neutral-separated pair, as it should.
    return "$from – $to"
}

/** Expanded-channel ids survive rotation; a Set isn't natively saveable, so it round-trips as a
 * LongArray. */
private val expandedSaver = androidx.compose.runtime.saveable.Saver<
    androidx.compose.runtime.MutableState<Set<Long>>,
    LongArray,
    >(
    save = { it.value.toLongArray() },
    restore = { mutableStateOf(it.toSet()) },
)
