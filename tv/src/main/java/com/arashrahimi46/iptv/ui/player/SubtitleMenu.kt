package com.arashrahimi46.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.parser.OnlineSubtitle
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreChipSize
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * A subtitle track the user can turn on from the player HUD. [Off] disables text rendering;
 * [Embedded] points at one text track baked into the current stream (identified by its
 * [TrackGroup] + [trackIndex] so it can be applied via [TrackSelectionOverride]).
 *
 * External (online-fetched) subtitles are a later step -- they arrive as an extra text track
 * merged into the MediaItem and then surface here as just another [Embedded] entry, so this
 * model already covers them without change.
 */
sealed interface SubtitleTrack {
    data object Off : SubtitleTrack
    data class Embedded(val group: TrackGroup, val trackIndex: Int, val label: String) : SubtitleTrack
}

/** Text tracks currently exposed by the player, mapped to selectable [SubtitleTrack.Embedded] rows. */
fun textTracksFrom(tracks: Tracks): List<SubtitleTrack.Embedded> {
    val out = mutableListOf<SubtitleTrack.Embedded>()
    var untitled = 0
    for (group in tracks.groups) {
        if (group.type != C.TRACK_TYPE_TEXT) continue
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            val format = group.getTrackFormat(i)
            val langName = format.language
                ?.takeIf { it.isNotBlank() && it != C.LANGUAGE_UNDETERMINED }
                ?.let { displayLanguage(it) }
            val rawLabel = format.label?.takeIf { it.isNotBlank() }
            // Prefer the LANGUAGE for the row -- many muxed tracks put a site/release name in the
            // label ("Bamabin.Com"), which tells the user nothing about the language. Show the
            // language name first, and keep the label only as a trailing hint when it adds something.
            val label = when {
                langName != null && rawLabel != null && !rawLabel.equals(langName, ignoreCase = true) ->
                    "$langName · $rawLabel"
                langName != null -> langName
                rawLabel != null -> rawLabel
                else -> "Subtitle ${++untitled}"
            }
            out += SubtitleTrack.Embedded(group.mediaTrackGroup, i, label)
        }
    }
    return out
}

/** Human-readable language name for an ISO code (e.g. "en" -> "English", "fa" -> "Persian"). */
fun displayLanguage(code: String): String =
    Locale(code).displayLanguage.ifBlank { code }.replaceFirstChar { it.uppercase() }

/** Persian-specific letters (vs Arabic) -- used to split the shared Arabic Unicode block. */
private const val PERSIAN_LETTERS = "پچژگکیۀ"

/**
 * Best-effort language guess from rendered subtitle text, for tracks the container left untagged
 * (many muxed subs carry no language, or a site name like "Bamabin.Com"). Offline, script-based:
 * reliable at naming non-Latin scripts (Persian vs Arabic, Cyrillic, CJK); returns null for
 * Latin/mixed/too-short text, where script alone can't identify the language -- the row then keeps
 * its metadata label rather than guessing wrong.
 */
fun detectSubtitleLanguage(text: String): String? {
    var arabic = 0; var cyrillic = 0; var cjk = 0; var latin = 0; var persianSpecific = 0
    for (c in text) {
        when (c) {
            in '؀'..'ۿ', in 'ݐ'..'ݿ' -> arabic++
            in 'Ѐ'..'ӿ' -> cyrillic++
            in '一'..'鿿', in '぀'..'ヿ', in '가'..'힣' -> cjk++
            in 'A'..'Z', in 'a'..'z' -> latin++
        }
        if (c in PERSIAN_LETTERS) persianSpecific++
    }
    val letters = arabic + cyrillic + cjk + latin
    if (letters < 3) return null
    return when {
        arabic * 2 > letters -> if (persianSpecific > 0) "Persian" else "Arabic"
        cyrillic * 2 > letters -> "Russian"
        cjk * 3 > letters -> "CJK"
        else -> null // Latin or mixed script: can't name the language from script alone.
    }
}

/** Applies [choice] to [params], returning parameters that disable ([Off]) or force ([Embedded]) text. */
fun TrackSelectionParameters.withSubtitle(choice: SubtitleTrack): TrackSelectionParameters =
    buildUpon().apply {
        clearOverridesOfType(C.TRACK_TYPE_TEXT)
        when (choice) {
            is SubtitleTrack.Off -> setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            is SubtitleTrack.Embedded -> {
                setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                setOverrideForType(TrackSelectionOverride(choice.group, choice.trackIndex))
            }
        }
    }.build()

/**
 * Subtitle picker shown from the player HUD's CC button. Lists **Off** plus every embedded text
 * track; the active one carries a check. Same Dialog/[AreDialog] pattern as the up-next panel so it
 * traps D-pad focus and Back dismisses. [selectedKey] identifies the active row for the checkmark.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SubtitleMenuDialog(
    tracks: List<SubtitleTrack.Embedded>,
    selectedKey: String?,
    onSelectOff: () -> Unit,
    onSelectTrack: (SubtitleTrack.Embedded) -> Unit,
    onDismiss: () -> Unit,
    /** Languages sniffed from rendered subtitle text, keyed by [SubtitleTrack.Embedded.rowKey].
     * Overrides an uninformative metadata label (e.g. a site name) when present. */
    detectedLangs: Map<String, String> = emptyMap(),
    /** Opens online subtitle search; null (e.g. live channels) hides the "Search online" row. */
    onSearchOnline: (() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(onDismiss = onDismiss, title = "Subtitles", width = 420.dp) {
            Column {
                SubtitleRow(label = "Off", selected = selectedKey == null, onClick = onSelectOff)
                if (tracks.isEmpty()) {
                    Box(Modifier.height(8.dp))
                    Text(
                        text = "No subtitles in this stream.",
                        style = AreIptvTheme.typography.caption,
                        color = AreIptvTheme.colors.textTertiary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                } else {
                    tracks.forEach { track ->
                        val key = track.rowKey()
                        val detected = detectedLangs[key]
                        // Detected language wins over an uninformative metadata label (site/release
                        // name), keeping the label as a hint only when it's meaningful (not a
                        // generic "Subtitle N" placeholder and not just the language name again).
                        val genericLabel = track.label.startsWith("Subtitle ")
                        val shown = when {
                            detected != null && !genericLabel && !track.label.equals(detected, ignoreCase = true) ->
                                "$detected · ${track.label}"
                            detected != null -> detected
                            else -> track.label
                        }
                        SubtitleRow(label = shown, selected = selectedKey == key) { onSelectTrack(track) }
                    }
                }
                if (onSearchOnline != null) {
                    Box(
                        Modifier
                            .padding(vertical = 6.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(AreIptvTheme.colors.borderDefault),
                    )
                    SubtitleRow(label = "Search online…", selected = false, onClick = onSearchOnline)
                }
            }
        }
    }
}

/** Row key for an embedded track, stable within a single stream's Tracks snapshot. */
fun SubtitleTrack.Embedded.rowKey(): String = "${System.identityHashCode(group)}:$trackIndex"

/** Languages offered in the online-search picker, code -> name. Persian/Arabic first for this catalog. */
val SUBTITLE_LANGUAGES: List<Pair<String, String>> = listOf(
    "en" to "English", "fa" to "Persian", "ar" to "Arabic", "fr" to "French",
    "es" to "Spanish", "de" to "German", "it" to "Italian", "tr" to "Turkish",
    "ru" to "Russian", "pt" to "Portuguese", "nl" to "Dutch", "hi" to "Hindi",
)

/**
 * Online subtitle search over OpenSubtitles, opened from the CC menu's "Search online" row. Runs an
 * initial search on open from the title's own metadata, lets the user change the language (re-searches)
 * or edit the query, and downloads the picked result via [onPick] -- which sideloads it and dismisses
 * this dialog. Both callbacks are suspend-friendly: they're launched on this dialog's scope.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OnlineSubtitleSearchDialog(
    initialQuery: String,
    defaultLang: String,
    onSearch: suspend (query: String, langCode: String) -> Result<List<OnlineSubtitle>>,
    onPick: suspend (OnlineSubtitle) -> Result<Unit>,
    onDismiss: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(initialQuery) }
    var langCode by remember { mutableStateOf(defaultLang) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<OnlineSubtitle>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var downloadingId by remember { mutableStateOf<Long?>(null) }

    fun runSearch() {
        if (query.isBlank() || loading) return
        scope.launch {
            loading = true; error = null
            onSearch(query.trim(), langCode)
                .onSuccess { results = it; searched = true }
                .onFailure { error = it.message ?: "Search failed." }
            loading = false
        }
    }

    LaunchedEffect(Unit) { if (query.isNotBlank()) runSearch() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(onDismiss = onDismiss, title = "Search subtitles online", width = 560.dp) {
            Column {
                AreTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Title",
                    icon = Icons.Filled.Search,
                )
                Box(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SUBTITLE_LANGUAGES.forEach { (code, name) ->
                        AreChip(
                            text = name,
                            selected = code == langCode,
                            onClick = { if (code != langCode) { langCode = code; runSearch() } },
                            size = AreChipSize.Small,
                        )
                    }
                }
                Box(Modifier.height(12.dp))
                AreButton(
                    text = if (loading) "Searching…" else "Search",
                    onClick = { runSearch() },
                    disabled = loading || query.isBlank(),
                    size = AreButtonSize.Small,
                )
                Box(Modifier.height(12.dp))
                when {
                    loading -> StatusLine("Searching OpenSubtitles…")
                    error != null -> StatusLine(error!!, danger = true)
                    searched && results.isEmpty() -> StatusLine("No matches -- try a different title or language.")
                    else -> Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                    ) {
                        results.take(30).forEach { sub ->
                            val busy = downloadingId == sub.fileId
                            val label = if (busy) "Downloading…" else
                                "${sub.release}  ·  ${displayLanguage(sub.language)} · ${sub.downloads}↓"
                            SubtitleRow(label = label, selected = false) {
                                if (downloadingId != null) return@SubtitleRow
                                scope.launch {
                                    downloadingId = sub.fileId; error = null
                                    onPick(sub)
                                        .onSuccess { onDismiss() }
                                        .onFailure { error = it.message ?: "Download failed." }
                                    downloadingId = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatusLine(text: String, danger: Boolean = false) {
    Text(
        text = text,
        style = AreIptvTheme.typography.caption,
        color = if (danger) AreIptvTheme.colors.danger else AreIptvTheme.colors.textTertiary,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SubtitleRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AreIptvTheme.colors
    TvFocusable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AreIptvTheme.radius.md),
    ) { focused, _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (focused) colors.surfaceOverlay else androidx.compose.ui.graphics.Color.Transparent,
                    RoundedCornerShape(AreIptvTheme.radius.md),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = colors.accent, modifier = Modifier.size(18.dp))
            }
            Box(Modifier.size(10.dp))
            Text(
                text = label,
                style = AreIptvTheme.typography.body,
                color = if (selected) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
