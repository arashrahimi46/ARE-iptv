package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x99060708)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = width)
                .background(colors.surface2, RoundedCornerShape(AreIptvTheme.radius.xl))
                .border(1.dp, colors.borderDefault, RoundedCornerShape(AreIptvTheme.radius.xl))
                .padding(AreIptvTheme.spacing.sp8),
        ) {
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

@Preview(widthDp = 900, heightDp = 600, showBackground = true)
@Composable
private fun AreDialogPreview() {
    AreIptvTheme {
        AreDialog(
            onDismiss = {},
            title = "Remove playlist?",
            actions = {
                AreButton("Cancel", onClick = {}, variant = AreButtonVariant.Ghost)
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
