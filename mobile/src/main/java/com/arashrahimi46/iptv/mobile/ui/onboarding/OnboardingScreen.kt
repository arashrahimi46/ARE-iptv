package com.arashrahimi46.iptv.mobile.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChip

/**
 * Single-screen "add a source" form -- a phone-appropriate collapse of :tv's multi-step wizard
 * (Source -> Credentials -> EPG -> Confirm) into one scrollable form, reusing the same
 * [com.arashrahimi46.iptv.data.repository.PlaylistRepository] calls and the same strings.xml
 * copy so the concepts read identically between the two apps.
 *
 * Step 5 (glass migration): the source-type picker and the submit/skip actions are rebuilt on
 * :core's [AreChip]/[AreButton] (glass fill + accent-lens selection, matching :tv's Tabs.kt/
 * Chip.jsx funnel). The text inputs stay stock M3 [OutlinedTextField] -- :core has no text-field
 * component yet (its one text field, `AreTextField`, lives in :tv and is D-pad-specific; see
 * TextField.kt), so replacing these would mean inventing a new :core component rather than reusing
 * one, out of scope for a nav/splash-chrome milestone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit, viewModel: OnboardingViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = AreIptvMobileTheme.colors

    LaunchedEffect(state.added) {
        if (state.added) onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Text(stringResource(R.string.onboarding_subtitle), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceTypeChip(SourceType.XTREAM, stringResource(R.string.onboarding_xtream_title), state.type, viewModel::setType)
            SourceTypeChip(SourceType.M3U, stringResource(R.string.onboarding_m3u_title), state.type, viewModel::setType)
            SourceTypeChip(SourceType.STALKER, stringResource(R.string.onboarding_stalker_title), state.type, viewModel::setType)
        }
        Text(
            when (state.type) {
                SourceType.XTREAM -> stringResource(R.string.onboarding_xtream_desc)
                SourceType.M3U -> stringResource(R.string.onboarding_m3u_desc)
                SourceType.STALKER -> stringResource(R.string.onboarding_stalker_desc)
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text(stringResource(R.string.onboarding_portal_name_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_portal_name_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        when (state.type) {
            SourceType.XTREAM -> {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    label = { Text(stringResource(R.string.onboarding_server_url_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_server_url_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = state.username,
                    onValueChange = viewModel::setUsername,
                    label = { Text(stringResource(R.string.onboarding_username_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::setPassword,
                    label = { Text(stringResource(R.string.onboarding_password_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
            SourceType.M3U -> {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    label = { Text(stringResource(R.string.onboarding_m3u_url_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_m3u_url_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
            SourceType.STALKER -> {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    label = { Text(stringResource(R.string.onboarding_portal_url_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = state.mac,
                    onValueChange = viewModel::setMac,
                    label = { Text(stringResource(R.string.onboarding_mac_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_mac_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        OutlinedTextField(
            value = state.epgUrl,
            onValueChange = viewModel::setEpgUrl,
            label = { Text(stringResource(R.string.onboarding_epg_url_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_epg_url_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        state.error?.let {
            Text(it, color = colors.danger, style = MaterialTheme.typography.bodySmall)
        }

        AreButton(
            text = stringResource(R.string.onboarding_add_playlist),
            onClick = viewModel::submit,
            variant = AreButtonVariant.Primary,
            full = true,
            disabled = state.isSubmitting,
        )
        if (state.isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.accentFg)
        }

        AreButton(
            text = stringResource(R.string.onboarding_skip_for_now),
            onClick = onDone,
            variant = AreButtonVariant.Ghost,
            full = true,
        )
    }
}

@Composable
private fun SourceTypeChip(
    type: SourceType,
    label: String,
    selected: SourceType,
    onSelect: (SourceType) -> Unit,
) {
    AreChip(
        text = label,
        onClick = { onSelect(type) },
        selected = selected == type,
        modifier = Modifier.fillMaxWidth(),
    )
}
