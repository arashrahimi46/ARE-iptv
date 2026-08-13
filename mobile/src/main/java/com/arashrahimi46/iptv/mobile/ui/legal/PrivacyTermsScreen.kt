package com.arashrahimi46.iptv.mobile.ui.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.design.glassSurface
import com.arashrahimi46.iptv.mobile.ui.components.AreBottomSheet
import com.arashrahimi46.iptv.mobile.ui.components.AreButton
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreSwitchRow

/**
 * Every numbered clause of the shipped Privacy Policy / Terms, in order, mirroring :tv's
 * LEGAL_SECTIONS. Kept as one list so the document renders from data rather than fifteen
 * copy-pasted blocks, and so adding or removing a clause is a one-line change here plus the strings.
 *
 * English-only by design -- see the comment above `legal_doc_title` in values/strings.xml.
 */
internal fun legalSections(): List<Pair<Int, Int>> = listOf(
    CoreR.string.legal_s1_title to CoreR.string.legal_s1_body,
    CoreR.string.legal_s2_title to CoreR.string.legal_s2_body,
    CoreR.string.legal_s3_title to CoreR.string.legal_s3_body,
    CoreR.string.legal_s4_title to CoreR.string.legal_s4_body,
    CoreR.string.legal_s5_title to CoreR.string.legal_s5_body,
    CoreR.string.legal_s6_title to CoreR.string.legal_s6_body,
    CoreR.string.legal_s7_title to CoreR.string.legal_s7_body,
    CoreR.string.legal_s8_title to CoreR.string.legal_s8_body,
    CoreR.string.legal_s9_title to CoreR.string.legal_s9_body,
    CoreR.string.legal_s10_title to CoreR.string.legal_s10_body,
    CoreR.string.legal_s11_title to CoreR.string.legal_s11_body,
    CoreR.string.legal_s12_title to CoreR.string.legal_s12_body,
    CoreR.string.legal_s13_title to CoreR.string.legal_s13_body,
    CoreR.string.legal_s14_title to CoreR.string.legal_s14_body,
    CoreR.string.legal_s15_title to CoreR.string.legal_s15_body,
)

/**
 * The same 15-clause document :tv renders, as a scrolling bottom sheet.
 *
 * Lives here rather than in AboutSettingsScreen (where it started) because there are now TWO places
 * that must show the binding text: Settings > About, and the first-run acceptance gate below. One
 * copy means the gate can never show different terms from the ones Settings displays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LegalDocumentSheet(onDismiss: () -> Unit) {
    val sections = legalSections()
    AreBottomSheet(onDismiss = onDismiss, title = stringResource(CoreR.string.legal_doc_title)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            // Keyed by the clause's title resource id -- stable and unique across the 15 sections.
            items(sections, key = { it.first }) { (titleRes, bodyRes) ->
                Text(
                    text = stringResource(titleRes),
                    style = AreIptvTheme.typography.label,
                    color = AreIptvTheme.colors.textPrimary,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                )
                Text(
                    text = stringResource(bodyRes),
                    style = AreIptvTheme.typography.body,
                    color = AreIptvTheme.colors.textSecondary,
                )
            }
        }
    }
}

/**
 * First-run Privacy & Terms acceptance for the PHONE app -- the touch counterpart of :tv's
 * `PrivacyTermsStep`, gated the same way on
 * [com.arashrahimi46.iptv.mobile.data.settings.UserSettings.hasAcceptedTerms].
 *
 * This gate did not exist on phone until now. :mobile already shipped the version machinery
 * (`hasAcceptedTerms`, `acceptCurrentTerms()`, `CURRENT_TERMS_VERSION`) and the document itself in
 * Settings > About, but nothing ever ASKED the user, so `hasAcceptedTerms` was dead code and the
 * phone app entered the catalogue without consent while :tv refused to. Both apps ship the same
 * `CURRENT_TERMS_VERSION`, so the two now agree on what "accepted" means.
 *
 * The on-screen box is a plain-language SUMMARY, not the agreement: the button below it opens the
 * binding text, and what gets accepted is always the full document.
 *
 * Deliberately has NO back affordance -- it is a gate, not a step. Declining means not using the
 * app, which the user does with the system Back gesture leaving the activity.
 *
 * @param isUpdate true when this user accepted an earlier version -- shows the "we've updated" note.
 */
@Composable
fun PrivacyTermsScreen(onAccepted: (crashReportingEnabled: Boolean) -> Unit, isUpdate: Boolean = false) {
    val colors = AreIptvTheme.colors
    // rememberSaveable, not remember: unlike the TV activity this one is rotatable, and a toggle the
    // user has already set must survive both rotation and process death.
    var accepted by rememberSaveable { mutableStateOf(false) }
    // Default ON, matching the documented default; the user can refuse before ever accepting.
    var crashReporting by rememberSaveable { mutableStateOf(true) }
    var showDocument by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            // No scaffold here (a gate has no top bar), so the insets have to be handled directly or
            // the title slides under the status bar and the button under the nav bar.
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
    ) {
        Text(
            text = stringResource(CoreR.string.privacy_title),
            style = AreIptvTheme.typography.display,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(if (isUpdate) CoreR.string.privacy_updated_note else CoreR.string.privacy_subtitle),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(RoundedCornerShape(AreIptvTheme.radius.lg))
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(CoreR.string.privacy_summary),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        AreButton(
            text = stringResource(CoreR.string.privacy_read_full),
            onClick = { showDocument = true },
            variant = AreButtonVariant.Secondary,
            size = AreButtonSize.Large,
            full = true,
        )

        Spacer(Modifier.height(20.dp))

        // AreSwitchRow rather than a Row + AreSwitch: it makes the WHOLE row the tap target, so the
        // consent toggle is not a ~30dp thumb the user has to hit precisely on a phone.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AreSwitchRow(
                title = stringResource(CoreR.string.privacy_accept_label),
                checked = accepted,
                onCheckedChange = { accepted = it },
            )
            AreSwitchRow(
                title = stringResource(CoreR.string.privacy_crash_label),
                checked = crashReporting,
                onCheckedChange = { crashReporting = it },
            )
        }

        Spacer(Modifier.height(24.dp))

        AreButton(
            text = stringResource(CoreR.string.action_continue),
            onClick = { onAccepted(crashReporting) },
            size = AreButtonSize.Large,
            full = true,
            // The gate: Continue stays dead until the accept toggle is on.
            enabled = accepted,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (showDocument) LegalDocumentSheet(onDismiss = { showDocument = false })
}
