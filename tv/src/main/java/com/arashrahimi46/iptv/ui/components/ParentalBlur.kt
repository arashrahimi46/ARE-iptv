package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import com.arashrahimi46.iptv.data.settings.AdultContentFilter
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.LocalGlassTier

/**
 * Tile-level context for the parental "blur locked content" mode (Settings > Parental). When
 * [enabled] (lock on, display = BLUR, and the session is not unlocked), tiles whose category
 * matches [AdultContentFilter] render obscured (scrim + lock) and route their click to [onReveal],
 * which prompts for the PIN. HIDE mode never sets [enabled] -- those items are dropped upstream in
 * the view-models instead, so this only governs the BLUR path.
 */
data class ParentalBlurState(
    val enabled: Boolean = false,
    val keywords: Set<String> = emptySet(),
    val onReveal: () -> Unit = {},
) {
    fun isObscured(categoryName: String?): Boolean =
        enabled && AdultContentFilter.isAdult(categoryName, keywords)
}

/** Default is "not blurring" so any tile outside a provider (previews, etc.) renders normally. */
val LocalParentalBlur = staticCompositionLocalOf { ParentalBlurState() }

/**
 * Scrim + centered lock icon drawn over an obscured tile's thumbnail. The scrim guarantees the
 * poster reads as "locked" on every device; on Tier A/B it is also a *real* frosted-glass blur so
 * the plate reads as glass rather than a flat black card. [Modifier.blur] silently no-ops below
 * API 31, so it is gated on [LocalGlassTier]'s `hasBackdropBlur` rather than an inline SDK check.
 * The lock icon is a separate, un-blurred layer on top so it stays crisp.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ParentalLockOverlay(shape: Shape = RoundedCornerShape(0.dp)) {
    val hasBlur = LocalGlassTier.current.hasBackdropBlur
    Box(
        modifier = Modifier.fillMaxSize().clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hasBlur) Modifier.blur(20.dp, BlurredEdgeTreatment(shape)) else Modifier)
                .background(Color.Black.copy(alpha = 0.55f)),
        )
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = AreIptvTheme.colors.textPrimary,
            modifier = Modifier.size(28.dp),
        )
    }
}
