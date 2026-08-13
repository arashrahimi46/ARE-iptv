package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.design.ProvideOnGlass
import com.arashrahimi46.iptv.mobile.design.glassSurface

/**
 * The destructive/blocking confirm. Everything that is a choice, a picker, a long document or a set
 * of contextual actions belongs in a sheet instead (§3.3).
 *
 * Both the scrim tap and the system Back dismiss it -- the TV `AreDialog` allowed neither, because
 * on a remote there is no scrim to tap and Back was owned by the screen.
 */
@Composable
fun AreAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = null,
    destructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = AreIptvTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true),
    ) {
        ProvideOnGlass(true) {
            Column(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 400.dp)
                    // Opaque base under the glass: see the note in Sheet.kt. A dialog floats over
                    // sharp page content with no blur pass behind it, so the translucent fill alone
                    // let the page read straight through the card.
                    .background(colors.surface1, RoundedCornerShape(AreIptvTheme.radius.xl))
                    .glassSurface(RoundedCornerShape(AreIptvTheme.radius.xl), elevated = true)
                    .padding(24.dp),
            ) {
                Text(text = title, style = AreIptvTheme.typography.h3, color = colors.textPrimary)
                if (text != null) {
                    Text(
                        text = text,
                        style = AreIptvTheme.typography.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                if (content != null) {
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        content = content,
                    )
                }
                if (confirmLabel != null || dismissLabel != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (dismissLabel != null) {
                            AreButton(
                                text = dismissLabel,
                                onClick = onDismiss,
                                variant = AreButtonVariant.Ghost,
                                size = AreButtonSize.Small,
                            )
                        }
                        if (confirmLabel != null) {
                            AreButton(
                                text = confirmLabel,
                                onClick = { onConfirm?.invoke() },
                                variant = if (destructive) AreButtonVariant.Danger else AreButtonVariant.Primary,
                                size = AreButtonSize.Small,
                            )
                        }
                    }
                }
            }
        }
    }
}
