package com.arashrahimi46.iptv.data.parser

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Hard cap on total playlists. Keeps the picker navigable by remote and the catalog bounded. */
const val MAX_PLAYLISTS = 10

/** One curated entry in the Explore catalogue. [id] is stored as `PlaylistSource.origin`. */
data class ExploreEntry(
    val id: String,
    val glyph: String,
    val name: String,
    /** Indicative only -- surfaced as "approximate" because these lists change daily. */
    val channels: Int,
    val url: String,
)

/**
 * Reads the curated Explore catalogue.
 *
 * Bundled in `assets/explore.json` rather than fetched: the list is only names and URLs and changes
 * rarely, while the channels behind each URL are maintained continuously by iptv-org -- so a static
 * list still serves live content, with no backend and no hosting to pay for. The cost is that a dead
 * URL stays dead until the next app release.
 *
 * [SOURCE] is the single seam to change if that trade stops being worth it: point it at a hosted
 * manifest, add a cache, and nothing above this file has to know.
 */
object ExploreCatalog {
    // monolean: bundled asset — upgrade path is a hosted manifest + 24h cache behind this same call.
    private const val SOURCE = "explore.json"

    suspend fun load(context: Context): List<ExploreEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.assets.open(SOURCE).bufferedReader().use { it.readText() }
            val array = JSONObject(raw).getJSONArray("entries")
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                ExploreEntry(
                    id = o.getString("id"),
                    glyph = o.getString("glyph"),
                    name = o.getString("name"),
                    channels = o.optInt("channels", 0),
                    url = o.getString("url"),
                )
            }
            // A malformed bundled asset is a build mistake, not a user-facing failure mode: degrade to
            // an empty catalogue (the screen shows its empty state) rather than crashing the app.
        }.getOrDefault(emptyList())
    }
}
