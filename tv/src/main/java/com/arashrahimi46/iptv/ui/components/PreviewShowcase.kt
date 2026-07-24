package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassSurface

/**
 * A single scrollable grid of every Phase 0 component for quick visual QA —
 * screenshot this preview to review the whole library at once. Focus states
 * aren't capturable in a static preview, so components render in their
 * default (unfocused) state here; see each component's own file for
 * focused-state previews where relevant.
 */
@Composable
private fun ShowcaseSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(text = title, style = AreIptvTheme.typography.h2, color = AreIptvTheme.colors.textPrimary)
        Box(Modifier.height(12.dp))
        content()
        Box(Modifier.height(32.dp))
    }
}

@Composable
fun PreviewShowcaseContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AreIptvTheme.colors.bgBase)
            .padding(32.dp),
    ) {
        ShowcaseSection("Buttons") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AreButton("Play now", onClick = {}, variant = AreButtonVariant.Primary, icon = Icons.Filled.PlayArrow)
                AreButton("More info", onClick = {}, variant = AreButtonVariant.Secondary)
                AreButton("Skip", onClick = {}, variant = AreButtonVariant.Ghost)
                AreButton("Remove", onClick = {}, variant = AreButtonVariant.Danger)
            }
        }
        ShowcaseSection("Icon buttons, chips, badges") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AreIconButton(Icons.Filled.PlayArrow, "Play", onClick = {}, variant = AreIconButtonVariant.Solid)
                AreChip("Sports", onClick = {}, selected = true)
                AreChip("Movies", onClick = {})
                AreBadge("Live", tone = AreBadgeTone.Live, glow = true)
                AreBadge("Smart", tone = AreBadgeTone.Smart, glow = true)
            }
        }
        ShowcaseSection("Forms") {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                AreTextField(value = "http://provider/get.php", onValueChange = {}, label = "Playlist URL", mono = true)
                AreSwitch(checked = true, onCheckedChange = {})
            }
        }
        ShowcaseSection("Category") {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AreCategoryCard(name = "Sports", onClick = {}, count = 42, kind = AreCategoryKind.Live, smart = true)
                AreCategoryCard(name = "Movies", onClick = {}, count = 512, kind = AreCategoryKind.Movies)
            }
        }
        ShowcaseSection("Media tiles") {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ArePosterTile(title = "Dune Part Two", onClick = {}, meta = "2024 · Sci-Fi", rating = "8.6")
                AreContinueCard(title = "Oppenheimer", onClick = {}, meta = "Movie", remaining = "42m left", progress = 0.55f)
                AreChannelTile(channel = "Sky Sports HD", onClick = {}, number = "101", now = "Premier League", quality = "1080p")
            }
        }
        ShowcaseSection("Guide + stream health") {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AreGuideCell(title = "Premier League Live", time = "20:00", onClick = {}, live = true, now = true, progress = 0.4f)
                AreStreamHealth(level = AreStreamHealthLevel.Stable, bitrate = "8.2 Mbps")
            }
        }
        ShowcaseSection("Rail") {
            AreRail(title = "Continue Watching", smart = true) {
                items(listOf("Oppenheimer", "The Bear", "Dune Part Two")) { name ->
                    AreContinueCard(title = name, onClick = {}, meta = "Movie")
                }
            }
        }
        ShowcaseSection("Glass surfaces") {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(
                    Modifier
                        .glassSurface(RoundedCornerShape(AreIptvTheme.radius.lg))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Text("Glass · translucent", style = AreIptvTheme.typography.body, color = AreIptvTheme.colors.textPrimary)
                }
                Box(
                    Modifier
                        .glassSurface(RoundedCornerShape(AreIptvTheme.radius.xl), elevated = true)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Text("Glass · elevated (modal/HUD)", style = AreIptvTheme.typography.body, color = AreIptvTheme.colors.textPrimary)
                }
            }
        }
    }
}

@Preview(widthDp = 1920, heightDp = 2200, showBackground = true)
@Composable
private fun PreviewShowcasePreview() {
    AreIptvTheme {
        PreviewShowcaseContent()
    }
}
