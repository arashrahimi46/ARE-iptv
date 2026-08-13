package com.arashrahimi46.iptv.mobile.data.parser

/** A per-episode M3U entry name split into its series + season/episode + episode title. */
data class SeriesEpisodeInfo(
    val seriesName: String,
    val season: Int,
    val episode: Int,
    val episodeTitle: String,
)

// Season/episode markers. SxxExx is the most reliable; the others cover spelled-out and NxM forms.
// Numbers capped at 3-4 digits so a resolution like "1920x1080" can't be misread as a season.
private val SXXEXX = Regex("""(?i)s(\d{1,3})[\s._\-]*e(\d{1,4})""")
private val NXM = Regex("""(?<!\d)(\d{1,3})x(\d{1,3})(?!\d)""")
private val SEASON_WORD = Regex("""(?i)\bseason[\s._\-]*(\d{1,3})\b""")
private val STANDALONE_SEASON = Regex("""(?i)\bs(\d{1,3})\b""")
private val EPISODE_WORD = Regex("""(?i)\b(?:episode|ep)[\s._\-]*(\d{1,4})\b""")
private val STANDALONE_EPISODE = Regex("""(?i)\be(\d{1,4})\b""")

// Tokens scrubbed out of the series name so all seasons of a show collapse to one grouping key.
private val SEASON_TOKEN = Regex("""(?i)\bs\d{1,3}\b|\bseason[\s._\-]*\d{1,3}\b""")
private val SEPARATORS = Regex("""[\s._\-|:]+""")
private const val TRIM_CHARS = "-._ |:•–—"

/**
 * Extracts series-grouping info from a per-episode M3U entry name.
 *
 * The series name is everything BEFORE the first season marker, with any residual season tokens
 * scrubbed -- so `Breaking Bad S01E02` and `Breaking Bad S02E05` both key to "breaking bad", and a
 * provider that embeds the season mid-name (`12 Monkeys (Foreign) S04 12 Monkeys S04E01`) still
 * collapses every season into one entry instead of one entry per season.
 *
 * Returns null when no season/episode marker is present (caller treats it as a standalone title) or
 * when nothing is left as a name. Pure/top-level so it's unit-testable without Room.
 */
fun parseSeriesEpisode(raw: String): SeriesEpisodeInfo? {
    val name = raw.trim()

    // Season number + cut point: the EARLIEST of any season marker in the name. Using the first
    // occurrence is what merges seasons -- a mid-name "S04" is cut before the trailing "S04E01".
    val seasonMatch = listOfNotNull(
        STANDALONE_SEASON.find(name),
        SEASON_WORD.find(name),
        SXXEXX.find(name),
        NXM.find(name),
    ).minByOrNull { it.range.first } ?: return null
    val season = seasonMatch.groupValues[1].toIntOrNull() ?: return null

    // Episode number: from the SxxExx/NxM pair if present, else a standalone E##/Episode ## anywhere.
    val episode = SXXEXX.find(name)?.groupValues?.get(2)?.toIntOrNull()
        ?: NXM.find(name)?.groupValues?.get(2)?.toIntOrNull()
        ?: EPISODE_WORD.find(name)?.groupValues?.get(1)?.toIntOrNull()
        ?: STANDALONE_EPISODE.find(name)?.groupValues?.get(1)?.toIntOrNull()
        ?: return null

    val seriesName = name.substring(0, seasonMatch.range.first)
        .replace(SEASON_TOKEN, " ")
        .replace(SEPARATORS, " ")
        .trim()
        .trim(*TRIM_CHARS.toCharArray())
        .trim()
    if (seriesName.isEmpty()) return null

    // Episode title, when the provider appends one after the SxxExx marker; else a synthesized label.
    val afterMarker = SXXEXX.find(name)?.let { name.substring(it.range.last + 1) }
        ?.replace(SEPARATORS, " ")?.trim()?.trim(*TRIM_CHARS.toCharArray())?.trim()

    return SeriesEpisodeInfo(
        seriesName = seriesName,
        season = season,
        episode = episode,
        episodeTitle = afterMarker?.takeIf { it.isNotEmpty() } ?: "Episode $episode",
    )
}
