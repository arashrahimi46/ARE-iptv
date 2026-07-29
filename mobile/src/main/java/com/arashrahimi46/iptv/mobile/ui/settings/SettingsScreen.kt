package com.arashrahimi46.iptv.mobile.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme

/**
 * Phase 3 scope (subtitles/EPG/quality/parity) intentionally not built yet -- this is the
 * placeholder destination so bottom-nav has somewhere to land on Settings for now.
 */
@Composable
fun SettingsScreen() {
    val colors = AreIptvMobileTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Text(
            stringResource(R.string.mobile_settings_more_coming_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}
