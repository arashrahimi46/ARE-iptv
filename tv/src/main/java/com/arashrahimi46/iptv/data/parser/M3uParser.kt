package com.arashrahimi46.iptv.data.parser

/** One parsed `#EXTINF` entry from an M3U playlist, prior to being persisted as a Channel/VodTitle. */
data class M3uEntry(
    val name: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
)

/**
 * Tolerant `#EXTM3U` parser: walks `#EXTINF:` metadata lines paired with the
 * following non-comment line as the stream URL. Malformed/incomplete entries
 * (no matching URL line, unparsable duration) are skipped rather than
 * aborting the whole parse.
 */
object M3uParser {
    private val attrRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")

    /**
     * Extracts the XMLTV EPG feed URL declared on the `#EXTM3U` header line
     * (`url-tvg` / `x-tvg-url` / `tvg-url` -- providers use all three spellings).
     * Returns the first URL of a comma-separated list (EpgRepository fetches one),
     * or null for a non-header line or a header without an EPG attribute.
     */
    fun extractEpgUrl(headerLine: String): String? {
        if (!headerLine.trim().startsWith("#EXTM3U", ignoreCase = true)) return null
        val attrs = attrRegex.findAll(headerLine).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
        return (attrs["url-tvg"] ?: attrs["x-tvg-url"] ?: attrs["tvg-url"])
            ?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }

    fun parse(playlist: String): List<M3uEntry> = parse(playlist.lineSequence()).toList()

    /**
     * Streaming variant: yields entries lazily from a line sequence so a large
     * playlist (100s of MB) is never fully materialized in memory. Only the
     * pending `#EXTINF` metadata is held until its URL line arrives; other
     * directive lines (`#EXTGRP`, etc.) between them are ignored. Behaviour is
     * identical to [parse] on a String -- which now delegates here.
     */
    fun parse(lines: Sequence<String>): Sequence<M3uEntry> = sequence {
        var pendingName: String? = null
        var pendingAttrs: Map<String, String> = emptyMap()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                // First #EXTINF before a URL wins; a second one before any URL is dropped
                // (matches the non-streaming parser -- see M3uParserTest for the pinned behavior).
                if (pendingName == null) {
                    val info = line.substringAfter(":", "")
                    val commaIdx = info.indexOf(',')
                    val attrsPart = if (commaIdx >= 0) info.substring(0, commaIdx) else info
                    val displayName = if (commaIdx >= 0) info.substring(commaIdx + 1).trim() else ""
                    val attrs = attrRegex.findAll(attrsPart).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    pendingName = attrs["tvg-name"]?.takeIf { it.isNotBlank() } ?: displayName.takeIf { it.isNotBlank() } ?: "Unnamed"
                    pendingAttrs = attrs
                }
            } else if (line.startsWith("#")) {
                // Directive between #EXTINF and the URL -- keep the pending metadata, skip.
                continue
            } else if (pendingName != null) {
                // First non-comment line after an #EXTINF is that entry's stream URL.
                yield(
                    M3uEntry(
                        name = pendingName,
                        streamUrl = line,
                        tvgId = pendingAttrs["tvg-id"],
                        logoUrl = pendingAttrs["tvg-logo"],
                        groupTitle = pendingAttrs["group-title"],
                    ),
                )
                pendingName = null
                pendingAttrs = emptyMap()
            }
        }
    }
}
