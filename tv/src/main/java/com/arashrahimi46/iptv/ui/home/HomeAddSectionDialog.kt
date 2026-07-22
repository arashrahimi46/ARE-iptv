package com.arashrahimi46.iptv.ui.home

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreCategoryCard
import com.arashrahimi46.iptv.ui.components.AreCategoryKind
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/** [CategoryKind]'s label in the picker, e.g. "Sports · Live". */
private fun CategoryKind.pickerLabel(): String = when (this) {
    CategoryKind.LIVE -> "Live"
    CategoryKind.MOVIE -> "Movie"
    CategoryKind.SERIES -> "Series"
}

private fun CategoryKind.toAreCategoryKind(): AreCategoryKind = when (this) {
    CategoryKind.LIVE -> AreCategoryKind.Live
    CategoryKind.MOVIE -> AreCategoryKind.Movies
    CategoryKind.SERIES -> AreCategoryKind.Series
}

/**
 * Step 6 "+ Add section" picker (Home layout customization): lists every real source category not
 * already pinned to Home ([categories], see [HomeViewModel]'s `availableCategories`), each labeled
 * with its [CategoryKind] since a pin is always single-type. Picking one calls [onPick]; the caller
 * appends the [HomeSection.Category] and persists via [HomeViewModel.addCategorySection].
 *
 * [AreDialog] is only the visual scrim+card -- it doesn't host itself in its own platform window,
 * so it must be wrapped in a real [Dialog] to actually pop up above the screen (same pattern as
 * [com.arashrahimi46.iptv.ui.settings.ParentalPinDialog] and [com.arashrahimi46.iptv.ui.browse.BrowseLayout]'s
 * pin/unpin overlay) -- otherwise it just composes inline in the caller's normal layout flow and
 * never becomes visible above the rest of Home.
 */
@Composable
fun HomeAddSectionDialog(
    categories: List<HomeAvailableCategory>,
    onPick: (HomeAvailableCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AreDialog(
            onDismiss = onDismiss,
            title = "Add a section",
            actions = { AreButton("Close", onClick = onDismiss, variant = AreButtonVariant.Ghost) },
        ) {
            if (categories.isEmpty()) {
                Text(
                    text = "Every category from your playlist is already pinned to Home.",
                    style = AreIptvTheme.typography.body,
                    color = AreIptvTheme.colors.textSecondary,
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    items(categories, key = { "${it.kind}|${it.name}" }) { category ->
                        AreCategoryCard(
                            name = "${category.name} · ${category.kind.pickerLabel()}",
                            onClick = { onPick(category) },
                            count = category.count,
                            kind = category.kind.toAreCategoryKind(),
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
