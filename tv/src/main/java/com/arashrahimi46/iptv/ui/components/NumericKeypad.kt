package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Numeric PIN keypad -- the seam [AreKeyboardKey] was left for in
 * [OnScreenKeyboard.kt] (Phase 3). Same D-pad-focusable [AreKeyboardKey]
 * primitive, no parallel focus/press styling built here. Used by the
 * parental-lock PIN set/confirm/unlock flow (Settings).
 */
@Composable
fun AreNumericKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            "123".forEach { digit -> AreKeyboardKey(label = digit.toString(), onClick = { onDigit(digit) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            "456".forEach { digit -> AreKeyboardKey(label = digit.toString(), onClick = { onDigit(digit) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            "789".forEach { digit -> AreKeyboardKey(label = digit.toString(), onClick = { onDigit(digit) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AreKeyboardKey(label = stringResource(R.string.action_clear), onClick = onClear, wide = true)
            AreKeyboardKey(label = "0", onClick = { onDigit('0') })
            AreKeyboardKey(label = "⌫", onClick = onBackspace)
        }
    }
}

@Preview(widthDp = 260, heightDp = 260, showBackground = true)
@Composable
private fun AreNumericKeypadPreview() {
    AreIptvTheme {
        AreNumericKeypad(onDigit = {}, onBackspace = {}, onClear = {})
    }
}
