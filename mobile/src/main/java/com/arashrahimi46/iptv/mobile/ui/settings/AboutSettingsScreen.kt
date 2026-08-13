package com.arashrahimi46.iptv.mobile.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.mobile.BuildConfig
import com.arashrahimi46.iptv.mobile.ui.components.AreListRow
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSectionHeader
import com.arashrahimi46.iptv.mobile.ui.components.AreSwitchRow
import com.arashrahimi46.iptv.mobile.ui.legal.LegalDocumentSheet
import com.arashrahimi46.iptv.core.R as CoreR

private const val SUPPORT_URL = "https://buymeacoffee.com/arashrahimi46"
private const val FEEDBACK_FORM_URL =
    "https://docs.google.com/forms/d/e/1FAIpQLSdxmmP_5GBRVY3gVWMQh4a_W4DrVfLy-vaxfBkPM29N94Cr-A/viewform"

/**
 * `settings/about` -- version, support/feedback hand-offs, analytics + crash opt-outs, legal.
 *
 * :tv's About pane also carries a QR-code feedback flow and an on-screen changelog; both exist
 * there because a TV has no browser or keyboard. A phone does, so Support/Feedback are plain
 * `ACTION_VIEW` intents. The legal document moved from a TV dialog to a full-height bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val isAnalyticsEnabled by viewModel.isAnalyticsEnabled.collectAsState()
    val isCrashReportingEnabled by viewModel.isCrashReportingEnabled.collectAsState()
    var showLegal by rememberSaveable { mutableStateOf(false) }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

    AreScreenScaffold(
        title = stringResource(CoreR.string.settings_tab_about),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item(contentType = "header") { AreSectionHeader(stringResource(CoreR.string.settings_section_about)) }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_version_title),
                    supporting = stringResource(CoreR.string.settings_version_value, BuildConfig.VERSION_NAME),
                    leadingIcon = Icons.Filled.Info,
                )
            }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_support_title),
                    supporting = stringResource(CoreR.string.settings_support_desc),
                    leadingIcon = Icons.Filled.VolunteerActivism,
                    onClick = { openUrl(SUPPORT_URL) },
                )
            }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_feedback_title),
                    supporting = stringResource(CoreR.string.settings_feedback_desc),
                    leadingIcon = Icons.Filled.Feedback,
                    onClick = { openUrl(FEEDBACK_FORM_URL) },
                )
            }
            item(contentType = "row") {
                AreSwitchRow(
                    title = stringResource(CoreR.string.settings_analytics_title),
                    supporting = stringResource(CoreR.string.settings_analytics_desc),
                    checked = isAnalyticsEnabled,
                    onCheckedChange = viewModel::setAnalyticsEnabled,
                )
            }
            item(contentType = "row") {
                AreSwitchRow(
                    title = stringResource(CoreR.string.settings_crash_title),
                    supporting = stringResource(CoreR.string.settings_crash_desc),
                    checked = isCrashReportingEnabled,
                    onCheckedChange = viewModel::setCrashReportingEnabled,
                )
            }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.settings_legal_title),
                    supporting = stringResource(CoreR.string.settings_legal_desc),
                    leadingIcon = Icons.Filled.Policy,
                    onClick = { showLegal = true },
                )
            }
        }
    }

    if (showLegal) LegalDocumentSheet(onDismiss = { showLegal = false })
}

// LegalDocumentSheet and legalSections() moved to ui/legal/PrivacyTermsScreen.kt. The first-run
// acceptance gate has to show the same binding text this screen shows, and two copies could drift
// into showing different terms in the two places. Imported above.
