package com.arashrahimi46.iptv.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.components.AreBadge
import com.arashrahimi46.iptv.ui.components.AreBadgeTone
import com.arashrahimi46.iptv.ui.components.AreSwitch
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/** Step 1 — pick Xtream Codes vs M3U (Onboarding.jsx `SourceCard`). */
@Composable
fun SourceStep(
    selected: OnboardingSourceType,
    onSelect: (OnboardingSourceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        SourceCard(
            title = stringResource(R.string.onboarding_xtream_title),
            desc = stringResource(R.string.onboarding_xtream_desc),
            icon = Icons.Filled.Dns,
            selected = selected == OnboardingSourceType.XTREAM,
            onClick = { onSelect(OnboardingSourceType.XTREAM) },
            modifier = Modifier.weight(1f),
        )
        SourceCard(
            title = stringResource(R.string.onboarding_m3u_title),
            desc = stringResource(R.string.onboarding_m3u_desc),
            icon = Icons.Filled.Link,
            selected = selected == OnboardingSourceType.M3U,
            onClick = { onSelect(OnboardingSourceType.M3U) },
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SourceCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.lg)
    TvFocusable(
        onClick = onClick,
        // NOTE: the selected/default border is applied INSIDE the content below, not
        // on this outer modifier. TvFocusable scales its content up on focus; a border
        // on the outer modifier stays outside that scale, so the card's own border and
        // the blue focus ring drift apart on focus ("blue line doesn't match white
        // line"). Inside the content it scales together with the focus ring.
        modifier = modifier,
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = if (selected) colors.accentWash else colors.surface1,
    ) { _, _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) colors.accent else colors.borderDefault,
                    shape = shape,
                )
                .padding(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (selected) colors.accent else colors.surface3, RoundedCornerShape(AreIptvTheme.radius.md)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (selected) colors.accentFg else colors.textSecondary, modifier = Modifier.size(22.dp))
            }
            Box(Modifier.height(12.dp))
            Text(text = title, style = AreIptvTheme.typography.h3, color = colors.textPrimary)
            Box(Modifier.height(6.dp))
            Text(text = desc, style = AreIptvTheme.typography.caption, color = colors.textTertiary)
        }
    }
}

/** Step 2 — server/user/pass for Xtream, or the playlist URL for M3U (Onboarding.jsx step===1). */
@Composable
fun CredentialsStep(
    state: OnboardingUiState,
    onChange: (portalName: String, serverUrl: String, username: String, password: String, m3uUrl: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.widthIn(max = 640.dp)) {
        AreTextField(
            value = state.portalName,
            onValueChange = { onChange(it, state.serverUrl, state.username, state.password, state.m3uUrl) },
            label = stringResource(R.string.onboarding_portal_name_label),
            placeholder = stringResource(R.string.onboarding_portal_name_placeholder),
        )
        Box(Modifier.height(20.dp))
        if (state.sourceType == OnboardingSourceType.XTREAM) {
            AreTextField(
                value = state.serverUrl,
                onValueChange = { onChange(state.portalName, it, state.username, state.password, state.m3uUrl) },
                label = stringResource(R.string.onboarding_server_url_label),
                mono = true,
                placeholder = stringResource(R.string.onboarding_server_url_placeholder),
                icon = Icons.Filled.Dns,
            )
            Box(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AreTextField(
                    value = state.username,
                    onValueChange = { onChange(state.portalName, state.serverUrl, it, state.password, state.m3uUrl) },
                    label = stringResource(R.string.onboarding_username_label),
                    mono = true,
                    modifier = Modifier.weight(1f),
                )
                AreTextField(
                    value = state.password,
                    onValueChange = { onChange(state.portalName, state.serverUrl, state.username, it, state.m3uUrl) },
                    label = stringResource(R.string.onboarding_password_label),
                    mono = true,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            AreTextField(
                value = state.m3uUrl,
                onValueChange = { onChange(state.portalName, state.serverUrl, state.username, state.password, it) },
                label = stringResource(R.string.onboarding_m3u_url_label),
                mono = true,
                placeholder = stringResource(R.string.onboarding_m3u_url_placeholder),
                icon = Icons.Filled.Link,
            )
        }
        if (state.error != null) {
            Box(Modifier.height(16.dp))
            Text(text = state.error, style = AreIptvTheme.typography.caption, color = AreIptvTheme.colors.danger)
        }
    }
}

/** Step 3 — optional EPG override, or note that Xtream auto-derives it (Onboarding.jsx step===2). */
@Composable
fun EpgStep(
    epgAuto: Boolean,
    epgUrl: String,
    onEpgAutoChange: (Boolean) -> Unit,
    onEpgUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AreIptvTheme.colors
    Column(modifier = modifier.widthIn(max = 640.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface1, RoundedCornerShape(AreIptvTheme.radius.md))
                .border(1.dp, colors.borderDefault, RoundedCornerShape(AreIptvTheme.radius.md))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.onboarding_epg_auto_title), style = AreIptvTheme.typography.label, color = colors.textPrimary)
                Box(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.onboarding_epg_auto_desc),
                    style = AreIptvTheme.typography.caption,
                    color = colors.textTertiary,
                )
            }
            AreSwitch(checked = epgAuto, onCheckedChange = onEpgAutoChange)
        }
        Box(Modifier.height(12.dp))
        AreTextField(
            value = epgUrl,
            onValueChange = onEpgUrlChange,
            label = stringResource(R.string.onboarding_epg_url_label),
            mono = true,
            placeholder = stringResource(R.string.onboarding_epg_url_placeholder),
            helper = if (epgAuto) stringResource(R.string.onboarding_epg_disabled_helper) else stringResource(R.string.onboarding_epg_custom_helper),
        )
    }
}

/** Step 4 — review + real derived counts from the parse result, then persist (Onboarding.jsx step===3). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConfirmStep(
    state: OnboardingUiState,
    modifier: Modifier = Modifier,
) {
    val colors = AreIptvTheme.colors
    Column(modifier = modifier.widthIn(max = 640.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface1, RoundedCornerShape(AreIptvTheme.radius.lg))
                .border(1.dp, colors.borderDefault, RoundedCornerShape(AreIptvTheme.radius.lg))
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.isSubmitting) {
                    AreSpinner(color = colors.accent)
                } else {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (state.error != null) colors.danger else colors.success,
                    )
                }
                Text(
                    text = when {
                        state.isSubmitting -> stringResource(R.string.onboarding_confirm_adding)
                        state.error != null -> stringResource(R.string.onboarding_confirm_error_title)
                        state.result != null -> stringResource(R.string.onboarding_confirm_added)
                        else -> stringResource(R.string.onboarding_confirm_ready)
                    },
                    style = AreIptvTheme.typography.h3,
                    color = colors.textPrimary,
                )
                Box(Modifier.weight(1f))
                state.result?.let { AreBadge(stringResource(R.string.onboarding_confirm_channels_badge, it.channels), tone = AreBadgeTone.Catchup) }
            }
            Box(Modifier.height(14.dp))
            if (state.isSubmitting) {
                Text(
                    text = stringResource(R.string.onboarding_confirm_downloading),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            } else if (state.error != null) {
                Text(text = state.error, style = AreIptvTheme.typography.body, color = colors.danger)
            } else {
                val xtream = stringResource(R.string.onboarding_xtream_title)
                val m3u = stringResource(R.string.onboarding_m3u_title)
                val dash = stringResource(R.string.onboarding_confirm_dash)
                val rows = buildList {
                    add(stringResource(R.string.onboarding_confirm_row_portal) to state.portalName.ifBlank { stringResource(R.string.onboarding_confirm_default_portal_name) })
                    add(stringResource(R.string.onboarding_confirm_row_source) to if (state.sourceType == OnboardingSourceType.XTREAM) xtream else m3u)
                    add(
                        stringResource(R.string.onboarding_confirm_row_server) to if (state.sourceType == OnboardingSourceType.XTREAM) state.serverUrl.ifBlank { dash } else state.m3uUrl.ifBlank { dash },
                    )
                    add(stringResource(R.string.onboarding_confirm_row_epg) to if (state.epgAuto) stringResource(R.string.onboarding_confirm_auto_matched) else stringResource(R.string.onboarding_confirm_custom_xmltv))
                    state.result?.let { add(stringResource(R.string.onboarding_confirm_row_catalog) to stringResource(R.string.onboarding_confirm_catalog_summary, it.channels, it.movies, it.series)) }
                }
                rows.forEach { (key, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(text = key, style = AreIptvTheme.typography.caption, color = colors.textTertiary)
                        // A long get.php URL is a single unbreakable token; bound it to the remaining
                        // width and ellipsize so it can't clip/overflow against the key label.
                        Text(
                            text = value,
                            style = AreIptvTheme.typography.mono,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Indeterminate loading spinner -- a rotating arc drawn on a Canvas (no material dep). */
@Composable
private fun AreSpinner(color: Color, size: Dp = 22.dp) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val start by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "sweep",
    )
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 3.dp.toPx()
        drawArc(
            color = color,
            startAngle = start,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
            size = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Preview(widthDp = 1000, heightDp = 420, showBackground = true)
@Composable
private fun SourceStepPreview() {
    AreIptvTheme {
        Box(Modifier.padding(24.dp)) {
            SourceStep(selected = OnboardingSourceType.XTREAM, onSelect = {})
        }
    }
}
