package com.arashrahimi46.iptv.mobile.ui.player

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.parser.OnlineSubtitle
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.ui.components.AreBottomSheet
import com.arashrahimi46.iptv.mobile.ui.components.AreButton
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreChip
import com.arashrahimi46.iptv.mobile.ui.components.AreChipSize
import com.arashrahimi46.iptv.mobile.ui.components.AreListRow
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.mobile.ui.settings.SubtitleLanguageCodes
import com.arashrahimi46.iptv.mobile.ui.settings.languageCodeLabel
import kotlinx.coroutines.launch

/**
 * Online subtitles for touch: search OpenSubtitles for whatever is playing, pick a result, and the
 * caller sideloads it onto the player. Opened from the subtitle sheet's "Search online…" row.
 *
 * OpenSubtitles is the user's own free account, and it needs TWO things -- an API key (search) and an
 * account sign-in (download, which spends the account's daily quota). :tv onboards both in Settings and
 * simply hides its "Search online" row until they exist. The phone has no such Settings surface, so
 * this sheet IS the onboarding: with no credential it shows the key step, with a key but no account the
 * sign-in step, and only then the search. Each step advances on its own once the caller's stored state
 * updates, so the user never has to find their way back here.
 *
 * [onConnectKey] / [onConnectAccount] validate against the service and return an error message, or null
 * on success -- the sheet itself does no networking and holds nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineSubtitleSheet(
    initialQuery: String,
    defaultLanguage: String,
    credential: String?,
    signedIn: Boolean,
    onConnectKey: suspend (value: String) -> String?,
    onConnectAccount: suspend (username: String, phrase: String) -> String?,
    onSearch: suspend (query: String, languageCode: String) -> Result<List<OnlineSubtitle>>,
    onPick: suspend (OnlineSubtitle) -> Result<Unit>,
    onDismiss: () -> Unit,
) {
    AreBottomSheet(onDismiss = onDismiss, title = stringResource(CoreR.string.subtitle_search_title)) {
        when {
            credential == null -> ApiKeyStep(onConnectKey)
            !signedIn -> AccountStep(onConnectAccount)
            else -> SearchStep(
                initialQuery = initialQuery,
                defaultLanguage = defaultLanguage,
                onSearch = onSearch,
                onPick = onPick,
                onDone = onDismiss,
            )
        }
    }
}

/** Step 1 -- paste the API key. Search is impossible without it, so nothing else is offered yet. */
@Composable
private fun ApiKeyStep(onConnectKey: suspend (String) -> String?) {
    val scope = rememberCoroutineScope()
    var value by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    StepColumn {
        Body(stringResource(CoreR.string.guide_opensubs_intro))
        // The :tv guide's step 1 is "scan the QR code with your phone" -- meaningless when you ARE on
        // the phone, so the phone flow starts at its step 2 and keeps the rest verbatim.
        Body(stringResource(CoreR.string.guide_opensubs_step2))
        Body(stringResource(CoreR.string.guide_opensubs_step3))
        Body(stringResource(CoreR.string.guide_opensubs_step4))
        AreTextField(
            value = value,
            onValueChange = { value = it; error = null },
            placeholder = stringResource(CoreR.string.settings_placeholder_api_key),
            label = stringResource(CoreR.string.settings_opensubs_key_title),
            isError = error != null,
            supportingText = error,
            monospace = true,
            modifier = Modifier.fillMaxWidth(),
        )
        AreButton(
            text = if (busy) {
                stringResource(CoreR.string.settings_checking)
            } else {
                stringResource(CoreR.string.action_connect)
            },
            onClick = {
                if (busy || value.isBlank()) return@AreButton
                scope.launch {
                    busy = true
                    error = onConnectKey(value.trim())
                    busy = false
                }
            },
            enabled = !busy && value.isNotBlank(),
            size = AreButtonSize.Small,
        )
    }
}

/** Step 2 -- sign in, because `/download` spends the account's own daily quota. */
@Composable
private fun AccountStep(onConnectAccount: suspend (String, String) -> String?) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var phrase by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    StepColumn {
        Body(stringResource(CoreR.string.settings_signin_title))
        Body(stringResource(CoreR.string.settings_signin_desc))
        AreTextField(
            value = username,
            onValueChange = { username = it; error = null },
            placeholder = stringResource(CoreR.string.settings_placeholder_username),
            modifier = Modifier.fillMaxWidth(),
        )
        AreTextField(
            value = phrase,
            onValueChange = { phrase = it; error = null },
            placeholder = stringResource(CoreR.string.settings_placeholder_password),
            visualTransformation = PasswordVisualTransformation(),
            isError = error != null,
            supportingText = error,
            modifier = Modifier.fillMaxWidth(),
        )
        AreButton(
            text = if (busy) {
                stringResource(CoreR.string.settings_signing_in)
            } else {
                stringResource(CoreR.string.action_sign_in)
            },
            onClick = {
                if (busy || username.isBlank() || phrase.isBlank()) return@AreButton
                scope.launch {
                    busy = true
                    error = onConnectAccount(username.trim(), phrase)
                    busy = false
                }
            },
            enabled = !busy && username.isNotBlank() && phrase.isNotBlank(),
            size = AreButtonSize.Small,
        )
    }
}

/**
 * The search itself. Runs once on open from the title's own metadata, re-runs on a language change,
 * and picking a row downloads it -- [onDone] then closes the sheet so the subtitle is simply on.
 */
@Composable
private fun SearchStep(
    initialQuery: String,
    defaultLanguage: String,
    onSearch: suspend (String, String) -> Result<List<OnlineSubtitle>>,
    onPick: suspend (OnlineSubtitle) -> Result<Unit>,
    onDone: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(initialQuery) }
    var language by remember { mutableStateOf(defaultLanguage.ifBlank { "en" }) }
    var searching by remember { mutableStateOf(false) }
    var downloadingId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<OnlineSubtitle>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    val noMatches = stringResource(CoreR.string.subtitle_status_no_matches)
    val failed = stringResource(CoreR.string.player_playback_failed)

    fun runSearch() {
        if (query.isBlank() || searching) return
        scope.launch {
            searching = true
            error = null
            onSearch(query.trim(), language)
                .onSuccess { results = it; searched = true }
                .onFailure { error = it.message ?: noMatches }
            searching = false
        }
    }

    LaunchedEffect(Unit) { if (query.isNotBlank()) runSearch() }

    StepColumn {
        AreTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(CoreR.string.subtitle_search_placeholder),
            leadingIcon = Icons.Filled.Search,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SubtitleLanguageCodes.forEach { code ->
                AreChip(
                    text = languageCodeLabel(code),
                    selected = code == language,
                    onClick = { if (code != language) { language = code; runSearch() } },
                    size = AreChipSize.Small,
                )
            }
        }
        AreButton(
            text = if (searching) {
                stringResource(CoreR.string.subtitle_searching)
            } else {
                stringResource(CoreR.string.subtitle_search_action)
            },
            onClick = { runSearch() },
            enabled = !searching && query.isNotBlank(),
            size = AreButtonSize.Small,
        )
        // Status sits ABOVE the results and never moves: a download error (quota, bad password) must
        // not be pushed off the bottom of the sheet by a long list the user then can't act on.
        val status = when {
            searching -> stringResource(CoreR.string.subtitle_status_searching)
            downloadingId != null -> stringResource(CoreR.string.subtitle_status_downloading)
            error != null -> error
            searched && results.isEmpty() -> noMatches
            else -> null
        }
        if (status != null) {
            Text(
                text = status,
                style = AreIptvTheme.typography.caption,
                color = if (error != null) colors.danger else colors.textTertiary,
            )
        }
    }
    if (results.isNotEmpty()) {
        // Bounded height, and deliberately OUTSIDE [StepColumn] -- a lazy list needs a finite max
        // constraint, which a scrolling parent would not give it.
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
            items(results.take(30), key = { it.fileId }) { sub ->
                AreListRow(
                    title = sub.release,
                    supporting = "${languageCodeLabel(sub.language)} · ${sub.downloads}↓",
                    onClick = {
                        if (downloadingId != null) return@AreListRow
                        scope.launch {
                            downloadingId = sub.fileId
                            error = null
                            onPick(sub)
                                .onSuccess { onDone() }
                                .onFailure { error = it.message ?: failed }
                            downloadingId = null
                        }
                    },
                )
            }
        }
    }
}

/** The padded body every step shares. */
@Composable
private fun StepColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start,
        content = content,
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = AreIptvTheme.typography.caption,
        color = AreIptvTheme.colors.textSecondary,
    )
}
