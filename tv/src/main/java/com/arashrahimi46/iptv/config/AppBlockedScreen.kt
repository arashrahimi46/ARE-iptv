package com.arashrahimi46.iptv.config

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.BuildConfig
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Terminal screen shown when [RemoteFlags] has retired this build.
 *
 * Deliberately a dead end with exactly one way forward rather than a bare error: an app that stops
 * working and offers the user nothing to do is both hostile and the shape Play review objects to.
 * When a newer build exists ([updateAvailable]) the button opens this app's store listing; when the
 * app has been killed outright there is no honest action to offer, so no button is shown rather
 * than a button that cannot help.
 */
@Composable
fun AppBlockedScreen(updateAvailable: Boolean) {
    val colors = AreIptvTheme.colors
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(CoreR.string.blocked_title),
                style = AreIptvTheme.typography.display,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Box(Modifier.height(14.dp))
            Text(
                text = stringResource(
                    if (updateAvailable) CoreR.string.blocked_body_update else CoreR.string.blocked_body_unavailable,
                ),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            if (updateAvailable) {
                Box(Modifier.height(32.dp))
                AreButton(
                    text = stringResource(CoreR.string.blocked_action_update),
                    onClick = {
                        val id = BuildConfig.APPLICATION_ID
                        // market:// opens the Play app directly; the https fallback covers devices
                        // where it is absent (some TV boxes ship without the Play client).
                        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))
                        val web = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$id"),
                        )
                        try {
                            context.startActivity(market)
                        } catch (_: ActivityNotFoundException) {
                            runCatching { context.startActivity(web) }
                        }
                    },
                    icon = Icons.Filled.SystemUpdateAlt,
                    size = AreButtonSize.Large,
                )
            }
        }
    }
}
