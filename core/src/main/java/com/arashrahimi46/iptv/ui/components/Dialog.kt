package com.arashrahimi46.iptv.ui.components

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.Ink950
import com.arashrahimi46.iptv.ui.theme.ProvideOnGlass
import com.arashrahimi46.iptv.ui.theme.glassSurface

/**
 * Dialog — modal sheet on a scrim (Dialog.jsx): confirm remove, parental PIN,
 * add source. Not a full focus-trap in Phase 0 — a visual recreation only,
 * per the design source's own scope note.
 */
@Composable
fun AreDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    width: Dp = 520.dp,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = AreIptvTheme.colors
    // Glass redesign: real backdrop blur behind the dialog via the window compositor (API 31+),
    // the only path that blurs content over a SurfaceView video. When it takes effect the dark
    // scrim is lightened so the blurred backdrop shows through; the card itself is elevated glass.
    val blurred = rememberDialogBlurBehind()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Ink950.copy(alpha = if (blurred) 0.32f else 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        // Bug fix: this Column had no scroll and no height cap, so a tall `content` (e.g. the
        // 24-option AreLanguageSelector language picker) simply overflowed past the screen edge --
        // the overflowing rows were never measured/placed at all, so they were not just invisible
        // but permanently unreachable by D-pad focus search (confirmed on-device: Hungarian,
        // Bulgarian, Persian and Arabic -- the last 4 of 24 -- could not be focused no matter how
        // many DPAD_DOWN presses). Capping height to the screen and scrolling the overflow fixes
        // both the visual clipping and the focus dead-end.
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.8f
        Column(
            modifier = Modifier
                .widthIn(max = width)
                .heightIn(max = maxHeight)
                .glassSurface(RoundedCornerShape(AreIptvTheme.radius.xl), elevated = true)
                .padding(AreIptvTheme.spacing.sp8)
                .verticalScroll(rememberScrollState()),
        ) {
            // The card is a glass surface, so everything inside it is a nested child (§6): controls
            // switch to tint + hairline instead of laying a second glass fill on the panel, and the
            // action-row's neutral buttons read as glass chips rather than as a full opaque square.
            ProvideOnGlass {
                if (title != null) {
                    Text(text = title, style = AreIptvTheme.typography.h2, color = colors.textPrimary)
                    Box(Modifier.height(AreIptvTheme.spacing.sp4))
                }
                content()
                if (actions != null) {
                    Box(Modifier.height(AreIptvTheme.spacing.sp8))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    ) {
                        actions()
                    }
                }
            }
        }
    }
}

/**
 * Applies a real backdrop blur behind the hosting dialog window (`FLAG_BLUR_BEHIND`, API 31+) when
 * the device's compositor supports cross-window blur. Returns whether it was actually applied, so
 * the caller can lighten its scrim to reveal the blur. No-op (returns false) on API ≤30, when the
 * effect is unsupported, or when [AreDialog] isn't hosted in a real `Dialog` window.
 */
@Composable
private fun rememberDialogBlurBehind(radiusPx: Int = 56): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val view = LocalView.current
    var applied by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val window = (view.parent as? DialogWindowProvider)?.window
        val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (window != null && wm?.isCrossWindowBlurEnabled == true) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.also { it.blurBehindRadius = radiusPx }
            applied = true
        }
        onDispose { }
    }
    return applied
}

@Preview(widthDp = 900, heightDp = 600, showBackground = true)
@Composable
private fun AreDialogPreview() {
    AreIptvTheme {
        AreDialog(
            onDismiss = {},
            title = "Remove playlist?",
            actions = {
                AreButton("Cancel", onClick = {}, variant = AreButtonVariant.Secondary)
                AreButton("Remove", onClick = {}, variant = AreButtonVariant.Danger)
            },
        ) {
            Text(
                text = "This will remove the playlist and all its channels from this device.",
                style = AreIptvTheme.typography.body,
                color = AreIptvTheme.colors.textSecondary,
            )
        }
    }
}
