package com.arashrahimi46.iptv.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.BlueGlow
import com.arashrahimi46.iptv.ui.theme.BlueWash

/**
 * Cold-start splash (Issue #13). No `androidx.core.splashscreen` dependency is set up in this
 * project yet (manifest has no windowSplashScreen theme), so this is a plain Compose screen
 * shown for a couple of seconds by [com.arashrahimi46.iptv.AreIptvApp] before the app decides
 * between the privacy gate / onboarding / shell -- see that file's `showSplash` state.
 *
 * "3D lighting" treatment: two layered radial gradients (a slow-drifting ambient glow behind
 * the mark, and a tighter highlight glow that pulses) plus a subtle continuous rotation on the
 * logo mark's highlight sweep via [graphicsLayer], all built from existing brand tokens
 * ([BlueGlow]/[BlueWash]) -- no new colors.
 */
@Composable
fun AreSplashScreen() {
    val colors = AreIptvTheme.colors
    val transition = rememberInfiniteTransition(label = "splashGlow")

    val glowPulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowPulse",
    )
    val sweepRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "sweepRotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .drawBehind {
                // Ambient depth: a large, soft wash centered slightly above the mark.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(BlueWash, androidx.compose.ui.graphics.Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.42f),
                        radius = size.minDimension * 0.9f,
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Rotating highlight glow behind the mark -- the "lighting" sweep.
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .graphicsLayer { rotationZ = sweepRotation }
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(BlueGlow.copy(alpha = BlueGlow.alpha * glowPulse), androidx.compose.ui.graphics.Color.Transparent),
                                    center = Offset(size.width * 0.7f, size.height * 0.3f),
                                    radius = size.minDimension * 0.65f,
                                ),
                            )
                        },
                )
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .scale(0.94f + 0.06f * glowPulse)
                        .background(colors.accent, RoundedCornerShape(AreIptvTheme.radius.md)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "A", style = AreIptvTheme.typography.display, color = colors.accentFg)
                }
            }
            Box(Modifier.padding(top = 24.dp))
            Text(text = "ARE iptv", style = AreIptvTheme.typography.h2, color = colors.textPrimary)
        }
    }
}
