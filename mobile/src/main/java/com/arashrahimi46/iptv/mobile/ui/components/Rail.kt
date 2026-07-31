package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * A titled horizontal shelf. No `FocusRequester`, no `railPeek`, no `focusGroup` -- a phone rail is
 * just a `LazyRow` you can flick.
 *
 * [contentPadding] rather than `Modifier.padding` on the row: tile titles sit BELOW the press
 * surface, so a flush viewport edge clips them.
 */
@Composable
fun AreRail(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    itemSpacing: Dp = 12.dp,
    content: LazyListScope.() -> Unit,
) {
    val colors = AreIptvTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AreIptvTheme.typography.h3,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AreIptvTheme.typography.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onSeeAll != null) {
                AreButton(
                    text = stringResource(CoreR.string.browse_see_all),
                    onClick = onSeeAll,
                    variant = AreButtonVariant.Ghost,
                    size = AreButtonSize.Small,
                )
            }
        }
        LazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            content = content,
        )
    }
}
