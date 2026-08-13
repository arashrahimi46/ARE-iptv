package com.arashrahimi46.iptv.mobile.data.parser

import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Expands an M3U `catchup-source` template (or derives an archive URL straight from the live URL)
 * for a programme window -- the M3U analog of Xtream's `timeshiftUrl` (docs/catchup-v1-design.md,
 * Phase 4). Pure + top-level so the placeholder/type matrix is unit-testable without Room.
 *
 * Supported [type]s (from `catchup`/`catchup-type`):
 *  - `default` / `shift` / `timeshift` / null / unknown -> use the `catchup-source` template if given,
 *    else append `?utc={start}&lutc={now}` to the live URL (the TiviMate default).
 *  - `append` -> concatenate the expanded template onto the live URL verbatim.
 *  - `flussonic` -> rewrite the live path to Flussonic's `archive-<start>-<duration>.m3u8` form.
 *  - `xc` -> build the Xtream-Codes `/timeshift/<user>/<pass>/<min>/<Y-m-d:H-M>/<id>.<ext>` form.
 *
 * Template placeholders (both `{name}` and `${name}` forms): utc/start, lutc/now/timestamp,
 * utcend/end, duration (seconds), offset (now-start, seconds), and the UTC start components
 * Y m d H M S. [nowMs] is the wall clock at press-time (kept a param so it's deterministic in tests).
 */
fun expandCatchup(liveUrl: String, source: String?, type: String?, startMs: Long, endMs: Long, nowMs: Long): String {
    val startSec = startMs / 1000
    val endSec = endMs / 1000
    val nowSec = nowMs / 1000
    val durationSec = ((endMs - startMs) / 1000).coerceAtLeast(1)
    return when (type?.trim()?.lowercase()) {
        "flussonic", "flussonic-hls", "fs" -> flussonicArchive(liveUrl, startSec, durationSec)
        "xc", "xtream" -> xtreamStyleArchive(liveUrl, startSec, durationSec)
        "append" -> liveUrl + fillTemplate(source.orEmpty(), startSec, endSec, nowSec, durationSec)
        else -> {
            if (source.isNullOrBlank()) {
                val sep = if ('?' in liveUrl) "&" else "?"
                "$liveUrl${sep}utc=$startSec&lutc=$nowSec"
            } else {
                val filled = fillTemplate(source, startSec, endSec, nowSec, durationSec)
                // A full-URL template replaces the live URL; a bare suffix is appended to it.
                if (filled.startsWith("http", ignoreCase = true)) filled else liveUrl + filled
            }
        }
    }
}

/** Substitutes every supported placeholder in [template] (both `{k}` and `${k}` forms). */
private fun fillTemplate(template: String, startSec: Long, endSec: Long, nowSec: Long, durationSec: Long): String {
    val dt = LocalDateTime.ofEpochSecond(startSec, 0, ZoneOffset.UTC)
    val values = mapOf(
        "utc" to startSec.toString(), "start" to startSec.toString(),
        "lutc" to nowSec.toString(), "now" to nowSec.toString(), "timestamp" to nowSec.toString(),
        "utcend" to endSec.toString(), "end" to endSec.toString(),
        "duration" to durationSec.toString(),
        "offset" to (nowSec - startSec).coerceAtLeast(0).toString(),
        "Y" to "%04d".format(dt.year),
        "m" to "%02d".format(dt.monthValue),
        "d" to "%02d".format(dt.dayOfMonth),
        "H" to "%02d".format(dt.hour),
        "M" to "%02d".format(dt.minute),
        "S" to "%02d".format(dt.second),
    )
    var out = template
    for ((k, v) in values) out = out.replace("{$k}", v).replace("\${$k}", v)
    return out
}

/** Flussonic: `.../<stream>/<index>.m3u8[?q]` -> `.../<stream>/archive-<start>-<duration>.m3u8[?q]`;
 *  an mpegts/`.ts` live URL maps to `.../<stream>/timeshift_abs-<start>.ts[?q]`. */
private fun flussonicArchive(liveUrl: String, startSec: Long, durationSec: Long): String {
    val path = liveUrl.substringBefore('?')
    val query = liveUrl.substringAfter('?', "").let { if (it.isNotEmpty()) "?$it" else "" }
    val base = path.substringBeforeLast('/')
    val lastSeg = path.substringAfterLast('/')
    return if (lastSeg.endsWith(".ts", true) || lastSeg.startsWith("mpegts", true)) {
        "$base/timeshift_abs-$startSec.ts$query"
    } else {
        "$base/archive-$startSec-$durationSec.m3u8$query"
    }
}

/** Xtream-Codes style: `.../[live/]<user>/<pass>/<id>.<ext>` -> the `/timeshift/...` archive form. */
private fun xtreamStyleArchive(liveUrl: String, startSec: Long, durationSec: Long): String {
    val path = liveUrl.substringBefore('?')
    val query = liveUrl.substringAfter('?', "").let { if (it.isNotEmpty()) "?$it" else "" }
    val idSeg = path.substringAfterLast('/')                 // <id>.<ext>
    val ext = idSeg.substringAfterLast('.', "ts")
    val id = idSeg.substringBeforeLast('.')
    val beforeId = path.substringBeforeLast('/')             // .../[live/]<user>/<pass>
    val pass = beforeId.substringAfterLast('/')
    val beforePass = beforeId.substringBeforeLast('/')       // .../[live/]<user>
    val user = beforePass.substringAfterLast('/')
    val beforeUser = beforePass.substringBeforeLast('/')     // .../[live] or root
    val root = if (beforeUser.endsWith("/live", true)) beforeUser.dropLast("/live".length) else beforeUser
    val durationMin = (durationSec / 60).coerceAtLeast(1)
    val start = LocalDateTime.ofEpochSecond(startSec, 0, ZoneOffset.UTC)
        .let { "%04d-%02d-%02d:%02d-%02d".format(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute) }
    return "$root/timeshift/$user/$pass/$durationMin/$start/$id.$ext$query"
}
