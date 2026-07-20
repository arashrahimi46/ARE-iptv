package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/**
 * A single D-pad-focusable key -- originally built as a building block for a
 * QWERTY on-screen keyboard for Search (removed, issue #10: Search now uses
 * Android TV's native IME via [AreTextField] instead). Kept here since
 * [AreNumericKeypad] (parental PIN entry, see NumericKeypad.kt) still reuses
 * it directly rather than duplicating the focus/press styling.
 */
@Composable
fun AreKeyboardKey(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, wide: Boolean = false) {
    val colors = AreIptvTheme.colors
    TvFocusable(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .width(if (wide) 120.dp else 48.dp),
        shape = RoundedCornerShape(AreIptvTheme.radius.sm),
        backgroundColor = colors.surface2,
    ) { _, _ ->
        Box(Modifier.height(48.dp).width(if (wide) 120.dp else 48.dp), contentAlignment = Alignment.Center) {
            Text(text = label, style = AreIptvTheme.typography.label, color = colors.textPrimary)
        }
    }
}
