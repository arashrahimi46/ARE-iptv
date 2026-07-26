package com.arashrahimi46.iptv.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassChild

/**
 * The setup walkthroughs behind Settings' "How to get a key" buttons.
 *
 * Both providers hand out free keys, and neither flow is guessable from a TV: OMDb mails the key and
 * leaves it **inert until an activation link is clicked**, and OpenSubtitles needs two separate
 * things (an API consumer key to search with, an account to download with) obtained from two
 * different pages. Every "the key doesn't work" report so far is one of those two facts.
 *
 * The QR is the point: typing a URL, then an email address, then a copied key on a D-pad is the
 * worst input surface in the house, so the whole side trip happens on the user's phone and only the
 * final key comes back to the TV.
 */
internal enum class IntegrationGuide(
    @StringRes val titleRes: Int,
    @StringRes val introRes: Int,
    /** Encoded into the QR **and** printed underneath it, for anyone whose phone can't scan. */
    val url: String,
    /** Ordered `@StringRes` instructions; numbered 1..n by the dialog. */
    val steps: List<Int>,
) {
    Omdb(
        titleRes = R.string.guide_omdb_title,
        introRes = R.string.guide_omdb_intro,
        url = "https://www.omdbapi.com/apikey.aspx",
        steps = listOf(
            R.string.guide_omdb_step1,
            R.string.guide_omdb_step2,
            R.string.guide_omdb_step3,
            R.string.guide_omdb_step4,
            R.string.guide_omdb_step5,
        ),
    ),
    OpenSubsKey(
        titleRes = R.string.guide_opensubs_title,
        introRes = R.string.guide_opensubs_intro,
        url = "https://www.opensubtitles.com/en/consumers",
        steps = listOf(
            R.string.guide_opensubs_step1,
            R.string.guide_opensubs_step2,
            R.string.guide_opensubs_step3,
            R.string.guide_opensubs_step4,
        ),
    ),
    OpenSubsAccount(
        titleRes = R.string.guide_opensubs_account_title,
        introRes = R.string.guide_opensubs_account_intro,
        url = "https://www.opensubtitles.com/en/users/sign_up",
        steps = listOf(
            R.string.guide_opensubs_account_step1,
            R.string.guide_opensubs_account_step2,
            R.string.guide_opensubs_account_step3,
        ),
    ),
}

/**
 * One dialog for every [IntegrationGuide]: numbered steps on the left, phone hand-off on the right.
 *
 * Host it in a real `Dialog(...)` window (as [FeedbackDialog] is hosted) so D-pad focus can't leak
 * into the Settings pane behind it.
 */
@Composable
internal fun IntegrationGuideDialog(guide: IntegrationGuide, onDismiss: () -> Unit) {
    val colors = AreIptvTheme.colors
    // Sole focusable in the dialog -- claim focus on open or the remote has nothing to act on.
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(guide) { closeFocus.requestFocus() }

    AreDialog(
        onDismiss = onDismiss,
        title = stringResource(guide.titleRes),
        // Matches FeedbackDialog: two columns keep the modal short enough to need no scrolling.
        width = 880.dp,
        actions = {
            AreButton(
                text = stringResource(R.string.action_close),
                onClick = onDismiss,
                variant = AreButtonVariant.Primary,
                modifier = Modifier.focusRequester(closeFocus),
            )
        },
    ) {
        Text(
            text = stringResource(guide.introRes),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
        )
        Box(Modifier.size(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                guide.steps.forEachIndexed { i, res -> GuideStep(i + 1, stringResource(res)) }
            }
            Column(Modifier.width(200.dp)) {
                QrCode(guide.url)
                Box(Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.guide_qr_title),
                    style = AreIptvTheme.typography.label,
                    color = colors.textPrimary,
                )
                Box(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.guide_qr_or),
                    style = AreIptvTheme.typography.caption,
                    color = colors.textTertiary,
                )
                Text(
                    // Scheme stripped: it's noise on a card someone is reading off a TV to type
                    // into a phone browser, which supplies https:// itself.
                    text = guide.url.removePrefix("https://"),
                    style = AreIptvTheme.typography.mono,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/** A numbered step: glass counter chip + the instruction. The number is a glyph, so no string res. */
@Composable
private fun GuideStep(number: Int, text: String) {
    val colors = AreIptvTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(26.dp).glassChild(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "$number", style = AreIptvTheme.typography.caption, color = colors.textPrimary)
        }
        Text(
            text = text,
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}
