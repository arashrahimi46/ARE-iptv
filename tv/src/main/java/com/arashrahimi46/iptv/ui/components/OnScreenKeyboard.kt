package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/**
 * On-screen, D-pad-navigable QWERTY-ish keyboard grid -- there's no physical
 * keyboard on a TV remote and no keyboard primitive in the Phase 0 library
 * (design source doesn't ship one either; Search.jsx just shows a typed value
 * with no interaction model for how it got there). Each key is a real
 * [TvFocusable] button: key presses append/backspace directly into caller
 * state via button *clicks*, not IME/text-input focus, since a remote can't
 * drive [androidx.compose.foundation.text.BasicTextField] IME input.
 *
 * Kept as a generic "grid of keys" primitive (not search-specific) since a
 * numeric keypad for the parental PIN is coming in Phase 4 and can reuse
 * [AreKeyboardKey] directly rather than duplicating the focus/press styling --
 * intentionally not building a numeric-pad variant now, just leaving the seam.
 */
@Composable
fun AreOnScreenKeyboard(
    onCharacter: (Char) -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    rows: List<String> = DefaultKeyboardRows,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { char -> AreKeyboardKey(label = char.toString(), onClick = { onCharacter(char) }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AreKeyboardKey(label = "Space", onClick = onSpace, wide = true)
            AreKeyboardKey(label = "⌫", onClick = onBackspace)
            AreKeyboardKey(label = "Clear", onClick = onClear, wide = true)
        }
    }
}

private val DefaultKeyboardRows = listOf(
    "1234567890",
    "QWERTYUIOP",
    "ASDFGHJKL",
    "ZXCVBNM",
)

/** A single focusable key -- exposed so a future numeric keypad (Phase 4 parental PIN) can reuse it directly. */
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

@Preview(widthDp = 700, heightDp = 320, showBackground = true)
@Composable
private fun AreOnScreenKeyboardPreview() {
    AreIptvTheme {
        AreOnScreenKeyboard(onCharacter = {}, onSpace = {}, onBackspace = {}, onClear = {})
    }
}
