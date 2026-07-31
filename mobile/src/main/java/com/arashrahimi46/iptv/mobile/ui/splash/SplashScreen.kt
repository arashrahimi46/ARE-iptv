package com.arashrahimi46.iptv.mobile.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.accentGradientBrush

/**
 * Cold-start splash-equivalent for the blank frame [com.arashrahimi46.iptv.mobile.MainActivity]
 * shows while it checks for an existing source (`hasSource == null`). Mirrors :tv's
 * [com.arashrahimi46.iptv.ui.splash.AreSplashScreen] glass look -- dark backdrop, a soft accent
 * bloom the brand mark floats in, brand wordmark beneath it -- WITHOUT porting that screen's D-pad
 * -era choreography (it lives in :tv, which :mobile cannot depend on; see core/build.gradle.kts).
 * Deliberately static (no infinite-transition animation, no ambient rings/particles): this frame is
 * up for a DB check, typically a handful of frames, so the elaborate multi-second reveal TV plays
 * would barely be visible and isn't worth the extra composition cost.
 */
@Composable
fun MobileSplashScreen() {
    val accent = AreIptvTheme.colors.accent
    val bgBase = AreIptvTheme.colors.bgBase

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(bgBase)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.42f),
                        radius = size.maxDimension * 0.5f,
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(accentGradientBrush(), RoundedCornerShape(AreIptvTheme.radius.lg)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(CoreR.string.brand_name).take(1),
                    style = AreIptvTheme.typography.h1,
                    color = AreIptvTheme.colors.accentFg,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(CoreR.string.brand_name),
                style = AreIptvTheme.typography.h2,
                color = AreIptvTheme.colors.textPrimary,
            )
        }
    }
}
