package com.arashrahimi46.iptv.ui.multiview

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.ui.components.AreBadge
import com.arashrahimi46.iptv.ui.components.AreBadgeTone
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreIconButton
import com.arashrahimi46.iptv.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.ui.components.AreStreamHealth
import com.arashrahimi46.iptv.ui.components.AreStreamHealthLevel
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.Ink950
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/**
 * MultiView -- 2-up/4-up simultaneous live streams, one active (audio) pane
 * (MultiView.jsx). Full-bleed overlay, own NavHost destination outside
 * [com.arashrahimi46.iptv.ui.shell.AreIptvAppShell], reachable from the app
 * shell's top-bar multi-view button and the in-player transport HUD's
 * multi-view button (both previously wired as no-ops -- see report).
 *
 * Each pane is a real Media3/ExoPlayer instance (never the design source's
 * static mock panes): all panes play simultaneously, only the active pane's
 * audio is unmuted, tapping/selecting a pane makes it active.
 */
@Composable
fun MultiViewScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: MultiViewViewModel = viewModel(factory = MultiViewViewModel.factory(context.applicationContext as android.app.Application))
    val state by viewModel.uiState.collectAsState()

    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AreIconButton(Icons.Filled.ArrowBack, "Back", onClick = onBack, variant = AreIconButtonVariant.Glass)
                Text(text = "Multi-view", style = AreIptvTheme.typography.h2, color = Color.White)
                Box(Modifier.weight(1f))
                AreChip(
                    text = "4-up",
                    icon = Icons.Filled.GridView,
                    selected = state.paneCount == 4,
                    onClick = { viewModel.setPaneCount(4) },
                )
                AreChip(
                    text = "2-up",
                    icon = Icons.Filled.ViewColumn,
                    selected = state.paneCount == 2,
                    onClick = { viewModel.setPaneCount(2) },
                )
            }

            val panes = state.panes
            if (!state.hasSource || panes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (!state.hasSource) "No active playlist" else "No live channels available for multi-view",
                        style = AreIptvTheme.typography.body,
                        color = AreIptvTheme.colors.textSecondary,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val rows = panes.chunked(2)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.weight(1f).padding(bottom = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { channel ->
                                val index = panes.indexOf(channel)
                                MultiViewPane(
                                    channel = channel,
                                    active = index == state.activeIndex,
                                    onClick = { viewModel.setActive(index) },
                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                )
                            }
                            // A lone odd-one-out pane (e.g. 3rd of 4 when only 3 channels exist)
                            // still gets an even split rather than stretching to fill the row alone.
                            if (row.size == 1) Box(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiViewPane(channel: Channel, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = AreIptvTheme.colors
    var health by remember(channel.id) { mutableStateOf(AreStreamHealthLevel.Moderate) }

    val exoPlayer = remember(channel.streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                health = when (playbackState) {
                    Player.STATE_READY -> AreStreamHealthLevel.Stable
                    Player.STATE_BUFFERING -> AreStreamHealthLevel.Moderate
                    else -> health
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                health = AreStreamHealthLevel.Poor
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Only the active pane's audio plays -- every pane keeps decoding video so switching
    // the active pane is instant, matching the design's "tap to make active" behavior.
    LaunchedEffect(active, exoPlayer) {
        exoPlayer.volume = if (active) 1f else 0f
    }

    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    TvFocusable(
        onClick = onClick,
        modifier = modifier.aspectRatio(16f / 9f),
        shape = shape,
        glowColor = colors.accent,
    ) { _, _ ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black, shape),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PlayerView(context).apply {
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        player = exoPlayer
                    }
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Ink950.copy(alpha = 0f), Ink950.copy(alpha = 0.85f)),
                            startY = 0.45f,
                        ),
                    ),
            )
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AreBadge("Live", tone = AreBadgeTone.Live, glow = true)
                if (active) AreBadge("Audio", tone = AreBadgeTone.New)
            }
            Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                AreStreamHealth(level = health, showLabel = false)
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    text = channel.name,
                    style = AreIptvTheme.typography.h3,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (channel.categoryName != null) {
                    Text(
                        text = channel.categoryName,
                        style = AreIptvTheme.typography.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
