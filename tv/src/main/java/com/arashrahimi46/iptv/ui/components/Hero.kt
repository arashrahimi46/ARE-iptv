package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassSurface

/**
 * Hero — full-bleed featured banner for the top of Home / a detail page
 * (Hero.jsx). No image loader wired up in Phase 0; renders the scrim +
 * gradient backdrop that art would normally sit under.
 */
@Composable
fun AreHero(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    meta: String? = null,
    synopsis: String? = null,
    height: Dp = 520.dp,
    badges: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    val colors = AreIptvTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .glassSurface(RoundedCornerShape(AreIptvTheme.radius.xl)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.bgSunken.copy(alpha = 0.92f)),
                        startY = 0f,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(AreIptvTheme.spacing.sp12)
                .widthIn(max = 640.dp),
        ) {
            if (kicker != null) {
                Text(
                    text = kicker.uppercase(),
                    style = AreIptvTheme.typography.caption,
                    color = colors.accentHover,
                )
                Box(Modifier.height(14.dp))
            }
            if (badges != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { badges() }
                Box(Modifier.height(16.dp))
            }
            // textPrimary, not white: the hero has no backdrop image -- it renders over the
            // surface2->bgSunken gradient, which is LIGHT in light theme, so a white title
            // washed out entirely (same defect class as the invisible glass back button).
            Text(text = title, style = AreIptvTheme.typography.hero, color = colors.textPrimary)
            if (meta != null) {
                Box(Modifier.height(14.dp))
                Text(text = meta, style = AreIptvTheme.typography.label, color = colors.textSecondary)
            }
            if (synopsis != null) {
                Box(Modifier.height(14.dp))
                Text(
                    text = synopsis,
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (actions != null) {
                Box(Modifier.height(26.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { actions() }
            }
        }
    }
}

@Preview(widthDp = 1600, heightDp = 560, showBackground = true)
@Composable
private fun AreHeroPreview() {
    AreIptvTheme {
        AreHero(
            title = "The Last Frontier",
            kicker = "Featured Series",
            meta = "2026 · Drama · 8 episodes",
            synopsis = "A gripping survival drama that follows a crew stranded on the edge of known space.",
            badges = {
                AreBadge("New", tone = AreBadgeTone.New)
                AreBadge("4K", tone = AreBadgeTone.Quality)
            },
            actions = {
                AreButton("Play now", onClick = {}, variant = AreButtonVariant.Primary)
                AreButton("More info", onClick = {}, variant = AreButtonVariant.Secondary)
            },
        )
    }
}
