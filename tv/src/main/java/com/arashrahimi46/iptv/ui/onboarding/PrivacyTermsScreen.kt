package com.arashrahimi46.iptv.ui.onboarding

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.core.R
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreSwitch
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassSurface

/** One D-pad press of travel through the document, in px. Roughly two thirds of the visible body. */
private const val PAGE_SCROLL_PX = 380f

/**
 * Every numbered clause of the shipped Privacy Policy / Terms, in order. Kept as one list so the
 * document renders from data rather than fifteen copy-pasted blocks, and so adding or removing a
 * clause is a one-line change here plus the strings.
 *
 * English-only by design -- see the comment above `legal_doc_title` in values/strings.xml.
 */
private val LEGAL_SECTIONS = listOf(
    R.string.legal_s1_title to R.string.legal_s1_body,
    R.string.legal_s2_title to R.string.legal_s2_body,
    R.string.legal_s3_title to R.string.legal_s3_body,
    R.string.legal_s4_title to R.string.legal_s4_body,
    R.string.legal_s5_title to R.string.legal_s5_body,
    R.string.legal_s6_title to R.string.legal_s6_body,
    R.string.legal_s7_title to R.string.legal_s7_body,
    R.string.legal_s8_title to R.string.legal_s8_body,
    R.string.legal_s9_title to R.string.legal_s9_body,
    R.string.legal_s10_title to R.string.legal_s10_body,
    R.string.legal_s11_title to R.string.legal_s11_body,
    R.string.legal_s12_title to R.string.legal_s12_body,
    R.string.legal_s13_title to R.string.legal_s13_body,
    R.string.legal_s14_title to R.string.legal_s14_body,
    R.string.legal_s15_title to R.string.legal_s15_body,
)

/**
 * The full legal document as a scrollable modal. Shared by the first-run acceptance gate and
 * Settings > About, so there is exactly one copy of the text on screen anywhere in the app.
 *
 * Wrapped in a real [Dialog] window (not an inline overlay) so D-pad focus is trapped and can't
 * leak to the content behind it -- same rule as every other modal in the app.
 */
@Composable
fun LegalDocumentDialog(onDismiss: () -> Unit) {
    val colors = AreIptvTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(
            onDismiss = onDismiss,
            title = stringResource(R.string.legal_doc_title),
            width = 900.dp,
            actions = { AreButton(stringResource(R.string.action_close), onClick = onDismiss) },
        ) {
            val bodyFocus = remember { FocusRequester() }
            val scroll = rememberScrollState()
            val scope = rememberCoroutineScope()
            // Focus the body on open so Up/Down read the document immediately, rather than starting
            // on Close -- the whole point of opening this is to read it.
            LaunchedEffect(Unit) { runCatching { bodyFocus.requestFocus() } }
            Column(
                modifier = Modifier
                    // A 1080p TV panel is only ~540dp tall, so the body has to stay well under that
                    // for the title and Close button to survive alongside it.
                    .heightIn(max = 300.dp)
                    .focusRequester(bodyFocus)
                    // A scrolling container with no focusable CHILDREN never scrolls on a remote:
                    // Compose only scrolls to reveal focus, and plain Text isn't focusable. So the
                    // container takes focus itself and drives the scroll from the D-pad directly --
                    // without this the reader is stranded on clause 3 of 15.
                    .focusable()
                    .onKeyEvent { ev ->
                        if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                        val delta = when (ev.key) {
                            Key.DirectionDown -> PAGE_SCROLL_PX
                            Key.DirectionUp -> -PAGE_SCROLL_PX
                            else -> return@onKeyEvent false
                        }
                        // Let focus escape once the edge is reached, so Down at the bottom still
                        // gets you to Close instead of trapping you in the document.
                        val atEdge = (delta > 0 && scroll.value >= scroll.maxValue) ||
                            (delta < 0 && scroll.value <= 0)
                        if (atEdge) return@onKeyEvent false
                        scope.launch { scroll.animateScrollBy(delta) }
                        true
                    }
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = stringResource(R.string.legal_doc_meta),
                    style = AreIptvTheme.typography.caption,
                    color = colors.textTertiary,
                )
                LEGAL_SECTIONS.forEach { (titleRes, bodyRes) ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(titleRes),
                            style = AreIptvTheme.typography.label,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = stringResource(bodyRes),
                            style = AreIptvTheme.typography.body,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * First-run Privacy & Terms acceptance, gated in [com.arashrahimi46.iptv.MainActivity] by
 * [com.arashrahimi46.iptv.data.settings.UserSettings.hasAcceptedTerms]. Separate from
 * [OnboardingFlow] because that flow is scoped to "add a playlist source" and stays reachable
 * afterwards, whereas this shows only when the accepted terms version is behind the shipped one.
 *
 * The on-screen box is a plain-language SUMMARY, not the agreement: nobody reads a fifteen-clause
 * document at two metres, so the summary earns the informed part of informed consent while the
 * button above it opens the binding text. What gets accepted is always the full document.
 *
 * @param isUpdate true when this user accepted an earlier version -- shows the "we've updated" note.
 */
@Composable
fun PrivacyTermsStep(onAccepted: (crashReportingEnabled: Boolean) -> Unit, isUpdate: Boolean = false) {
    val colors = AreIptvTheme.colors
    var accepted by remember { mutableStateOf(false) }
    // Default ON, matching the documented default; the user can refuse before ever accepting.
    var crashReporting by remember { mutableStateOf(true) }
    var showDocument by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 40.dp, vertical = 56.dp),
        ) {
            Text(text = stringResource(R.string.privacy_title), style = AreIptvTheme.typography.display, color = colors.textPrimary)
            Box(Modifier.height(6.dp))
            Text(
                text = stringResource(if (isUpdate) R.string.privacy_updated_note else R.string.privacy_subtitle),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
            Box(Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(RoundedCornerShape(AreIptvTheme.radius.lg))
                    .padding(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.privacy_summary),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
            Box(Modifier.height(14.dp))
            AreButton(
                text = stringResource(R.string.privacy_read_full),
                onClick = { showDocument = true },
                variant = AreButtonVariant.Secondary,
                size = AreButtonSize.Large,
                full = true,
            )

            Box(Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AreSwitch(checked = accepted, onCheckedChange = { accepted = it })
                Text(
                    text = stringResource(R.string.privacy_accept_label),
                    style = AreIptvTheme.typography.label,
                    color = colors.textPrimary,
                )
            }
            Box(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AreSwitch(checked = crashReporting, onCheckedChange = { crashReporting = it })
                Text(
                    text = stringResource(R.string.privacy_crash_label),
                    style = AreIptvTheme.typography.label,
                    color = colors.textPrimary,
                )
            }

            Box(Modifier.height(36.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f))
                AreButton(
                    stringResource(R.string.action_continue),
                    onClick = { onAccepted(crashReporting) },
                    size = AreButtonSize.Large,
                    disabled = !accepted,
                )
            }
        }
    }

    if (showDocument) LegalDocumentDialog(onDismiss = { showDocument = false })
}
