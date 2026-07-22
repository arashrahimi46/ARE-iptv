package com.arashrahimi46.iptv.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreSwitch
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * First-run Privacy & Terms acceptance (Issue #11). Shown once, before onboarding/the main
 * shell -- gated in [com.arashrahimi46.iptv.AreIptvApp] by
 * [com.arashrahimi46.iptv.data.settings.UserSettings.hasAcceptedTerms], separate from
 * [OnboardingFlow] because that flow is scoped to "add a playlist source" and stays reachable
 * repeatedly afterwards (top bar "Add playlist"), whereas this must only ever show once.
 *
 * PLACEHOLDER COPY: the body text below is not real legal copy -- pending real Privacy Policy /
 * Terms of Service text from product/legal.
 */
@Composable
fun PrivacyTermsStep(onAccepted: () -> Unit) {
    val colors = AreIptvTheme.colors
    var accepted by remember { mutableStateOf(false) }

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
                text = stringResource(R.string.privacy_subtitle),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
            Box(Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface1, RoundedCornerShape(AreIptvTheme.radius.lg))
                    .padding(24.dp),
            ) {
                // PLACEHOLDER: real Privacy Policy / Terms of Service copy is pending from
                // product/legal. This stand-in text exists only so the acceptance gate has
                // something to display.
                Text(
                    text = stringResource(R.string.privacy_body),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }

            Box(Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AreSwitch(checked = accepted, onCheckedChange = { accepted = it })
                Text(
                    text = stringResource(R.string.privacy_accept_label),
                    style = AreIptvTheme.typography.label,
                    color = colors.textPrimary,
                )
            }

            Box(Modifier.height(36.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f))
                AreButton(
                    stringResource(R.string.action_continue),
                    onClick = onAccepted,
                    size = AreButtonSize.Large,
                    disabled = !accepted,
                )
            }
        }
    }
}
