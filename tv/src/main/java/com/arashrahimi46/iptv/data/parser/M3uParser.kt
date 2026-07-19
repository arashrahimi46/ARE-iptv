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

    fun parse(playlist: String): List<M3uEntry> {
        val lines = playlist.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val entries = mutableListOf<M3uEntry>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                val info = line.substringAfter(":", "")
                val commaIdx = info.indexOf(',')
                val attrsPart = if (commaIdx >= 0) info.substring(0, commaIdx) else info
                val displayName = if (commaIdx >= 0) info.substring(commaIdx + 1).trim() else ""

                val attrs = attrRegex.findAll(attrsPart).associate { it.groupValues[1].lowercase() to it.groupValues[2] }

                // Find the next non-comment, non-empty line as the stream URL.
                var j = i + 1
                while (j < lines.size && lines[j].startsWith("#")) j++
                val streamUrl = lines.getOrNull(j)

                if (!streamUrl.isNullOrBlank()) {
                    val name = attrs["tvg-name"]?.takeIf { it.isNotBlank() } ?: displayName.takeIf { it.isNotBlank() } ?: "Unnamed"
                    entries += M3uEntry(
                        name = name,
                        streamUrl = streamUrl,
                        tvgId = attrs["tvg-id"],
                        logoUrl = attrs["tvg-logo"],
                        groupTitle = attrs["group-title"],
                    )
                    i = j + 1
                    continue
                }
            }
            i++
        }
        return entries
    }
}
