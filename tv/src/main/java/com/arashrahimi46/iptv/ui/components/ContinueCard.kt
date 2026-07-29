package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.core.R
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush
import com.arashrahimi46.iptv.ui.theme.tvGlow

/**
 * ContinueCard — landscape "continue watching" tile (ContinueCard.jsx). Shows
 * a play affordance and resume progress; no image loader wired up in Phase 0.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AreContinueCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    progress: Float = 0.6f,
    remaining: String? = null,
    width: Dp = 340.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)

    Column(modifier = modifier.width(width)) {
        TvFocusable(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            interactionSource = interactionSource,
            shape = shape,
            backgroundColor = colors.surfaceGlass,
            borderBrush = glassBorderBrush(),
        ) { focused, _ ->
            Box(Modifier.fillMaxSize()) {
                if (focused) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                            .tvGlow(colors.accent, CircleShape)
                            .background(colors.accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.continue_card_resume_desc), tint = Color.White)
                    }
                }
                if (remaining != null) {
                    Text(
                        text = remaining,
                        style = AreIptvTheme.typography.caption,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(colors.surfaceOverlay, RoundedCornerShape(AreIptvTheme.radius.pill))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxSize()
                            .background(colors.accent),
                    )
                }
            }
        }
        Box(Modifier.height(9.dp))
        Text(text = title, style = AreIptvTheme.typography.tile, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (meta != null) {
            Text(text = meta, style = AreIptvTheme.typography.caption, color = colors.textTertiary)
        }
    }
}

@Preview(widthDp = 700, heightDp = 320, showBackground = true)
@Composable
private fun AreContinueCardPreview() {
    AreIptvTheme {
        Row(modifier = Modifier.padding(24.dp)) {
            AreContinueCard(title = "Oppenheimer", onClick = {}, meta = "Movie", remaining = "42m left", progress = 0.55f)
        }
    }
}
