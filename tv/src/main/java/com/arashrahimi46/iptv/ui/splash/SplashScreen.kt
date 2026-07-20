package com.arashrahimi46.iptv.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.Blue400
import com.arashrahimi46.iptv.ui.theme.Blue500
import com.arashrahimi46.iptv.ui.theme.BlueGlow
import com.arashrahimi46.iptv.ui.theme.Ink950
import com.arashrahimi46.iptv.ui.theme.Violet400

/**
 * Cold-start splash (Issue #13, reworked per design review). Abstract
 * neon/laser lighting look (ref: vecteezy 3047433) -- a near-black backdrop
 * with several blurred, diagonal glowing streaks drifting/pulsing behind the
 * mark -- replacing the earlier plain radial-glow treatment and the
 * placeholder "A" letter box with the real design-system logo mark
 * ([R.drawable.ic_logo_mark], ported from
 * are-iptv-design-system/project/assets/logo-mark.svg).
 *
 * No `androidx.core.splashscreen` dependency is set up in this project yet
 * (manifest has no windowSplashScreen theme), so this is a plain Compose
 * screen shown for a couple of seconds by [com.arashrahimi46.iptv.AreIptvApp]
 * before the app decides between the privacy gate / onboarding / shell --
 * see that file's `showSplash` state.
 */
@Composable
fun AreSplashScreen() {
    val transition = rememberInfiniteTransition(label = "splashGlow")

    val glowPulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowPulse",
    )
    val streakDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "streakDrift",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink950),
        contentAlignment = Alignment.Center,
    ) {
        // Neon/laser streaks: thin, blurred, diagonal glows crossing the dark backdrop.
        LaserStreak(color = Blue500, rotation = -28f, offsetFraction = 0.28f + 0.05f * streakDrift, alpha = 0.55f)
        LaserStreak(color = Violet400, rotation = 18f, offsetFraction = 0.62f - 0.05f * streakDrift, alpha = 0.4f)
        LaserStreak(color = Blue400, rotation = -12f, offsetFraction = 0.82f + 0.04f * streakDrift, alpha = 0.35f)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Ambient glow behind the mark, pulsing with the same rhythm as before.
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(BlueGlow.copy(alpha = BlueGlow.alpha * glowPulse), Color.Transparent),
                                    radius = size.minDimension * 0.55f,
                                ),
                            )
                        },
                )
                Image(
                    painter = painterResource(R.drawable.ic_logo_mark),
                    contentDescription = "ARE iptv",
                    modifier = Modifier
                        .size(96.dp)
                        .scale(0.94f + 0.06f * glowPulse),
                )
            }
            Box(Modifier.padding(top = 24.dp))
            Text(text = "ARE iptv", style = AreIptvTheme.typography.h2, color = AreIptvTheme.colors.textPrimary)
        }
    }
}

/** One blurred, glowing diagonal streak -- a rotated, elongated gradient bar. */
@Composable
private fun LaserStreak(color: Color, rotation: Float, offsetFraction: Float, alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val y = size.height * offsetFraction
                rotate(rotation, pivot = Offset(size.width / 2f, y)) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, color.copy(alpha = alpha), Color.Transparent),
                        ),
                        topLeft = Offset(0f, y - 20f),
                        size = androidx.compose.ui.geometry.Size(size.width, 40f),
                    )
                }
            }
            .blur(36.dp),
    )
}
