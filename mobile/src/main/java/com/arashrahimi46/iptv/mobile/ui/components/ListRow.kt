package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

private val RowMinHeight = 56.dp

/**
 * The Settings/list primitive. Full-width ripple, >= 56dp, leading glyph tinted `accent`.
 *
 * A clickable row with no [trailing] slot gets a chevron automatically -- that is the phone
 * affordance for "this opens something", and it is what the TV build expressed with a focus ring.
 */
@Composable
fun AreListRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AreIptvTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.areTouch(
                        onClick = onClick,
                        shape = RoundedCornerShape(0.dp),
                        enabled = enabled,
                        minTouchTarget = 0.dp,
                        disableScale = true,
                    )
                } else {
                    Modifier
                },
            )
            .heightIn(min = RowMinHeight)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = if (enabled) colors.accent else colors.textTertiary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = AreIptvTheme.typography.body,
                color = if (enabled) colors.textPrimary else colors.textTertiary,
            )
            if (supporting != null) {
                Text(text = supporting, style = AreIptvTheme.typography.caption, color = colors.textSecondary)
            }
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** A list row whose trailing slot is an [AreSwitch]; tapping anywhere on the row toggles it. */
@Composable
fun AreSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    AreListRow(
        title = title,
        modifier = modifier,
        supporting = supporting,
        leadingIcon = leadingIcon,
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
        enabled = enabled,
        trailing = {
            AreSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        },
    )
}

/**
 * A row that opens an [AreChoiceSheet]; the current value is the supporting text.
 *
 * This single component deletes every horizontal chip row in Settings -- 24 locales, 8 accents or
 * 12 subtitle sizes never fitted on a handset as chips.
 */
@Composable
fun <T> AreChoiceRow(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    sheetTitle: String = title,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    AreListRow(
        title = title,
        modifier = modifier,
        supporting = selected?.let { label(it) },
        leadingIcon = leadingIcon,
        onClick = { open = true },
        enabled = enabled,
    )
    if (open) {
        AreChoiceSheet(
            title = sheetTitle,
            options = options,
            selected = selected,
            label = label,
            onSelect = onSelect,
            onDismiss = { open = false },
        )
    }
}

/** Section label above a group of rows. */
@Composable
fun AreSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AreIptvTheme.typography.label,
        color = AreIptvTheme.colors.accent,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}
