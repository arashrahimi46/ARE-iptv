package com.arashrahimi46.iptv.mobile.ui.home

import com.arashrahimi46.iptv.mobile.data.model.Channel
import com.arashrahimi46.iptv.mobile.data.model.VodTitle
import kotlin.random.Random

/**
 * Turns a bounded, diverse candidate pool into a curated Home rail (the "best-feeling" selection
 * that replaced the old alphabetical `ORDER BY name LIMIT` head). Four moves, in order:
 *
 *  1. **De-dup edition/quality variants** -- "AD Sport Asia 1 4K / HD / SD" collapse to one entry
 *     (the highest-quality one), so the same channel/title can't fill the whole rail. This is the
 *     exact defect the screenshot showed.
 *  2. **Rank by personal taste** -- categories the user actually watches ([ContinueWatchingEntry])
 *     or favorites weigh heaviest, with intrinsic quality (rating) and recency (year) for titles.
 *  3. **Rotate daily** -- a small day-seeded jitter reshuffles ties once per day, so Home feels
 *     fresh without ever jittering within a session (the seed is stable for the whole day).
 *  4. **Diversify** -- round-robin across categories so one genre can't monopolize the first tiles.
 *
 * Pure and deterministic: the same (pool, weights, daySeed) always yields the same list, so it's
 * unit-testable and stable within a day. The ViewModel supplies the pool (a DB id-hash sample) and
 * the taste weights; no Android/DB types leak in here.
 */
object HomeRailCurator {

    /** Jitter magnitude added to each item's score for daily rotation. ~0.5 on a taste/quality
     *  scale of 0..1 means a strong signal still wins, but weak/absent signals rotate freely. */
    private const val DAILY_JITTER = 0.5

    /** Edition/quality tokens stripped when collapsing variants, high-to-low so the survivor with
     *  the best available quality is preferred (4K over SD). Matched as whole words only. */
    private val QUALITY_TOKENS = listOf("4K", "UHD", "FHD", "HD", "SD", "HQ", "H265", "HEVC", "1080P", "720P", "576P", "480P")

    private val bracketed = Regex("[\\[(][^\\])]*[\\])]")
    private val nonAlnum = Regex("[^a-z0-9]+")
    private val qualityWordRegexes = QUALITY_TOKENS.map { Regex("(?<![A-Za-z0-9])${Regex.escape(it)}(?![A-Za-z0-9])", RegexOption.IGNORE_CASE) }

    /** De-dup identity: lowercased, bracketed segments and edition/quality tokens removed,
     *  punctuation collapsed. Country/provider prefixes are KEPT ("US: ESPN" != "UK: ESPN" --
     *  different channels); only edition noise is stripped. */
    fun baseName(raw: String): String {
        var s = bracketed.replace(raw, " ")
        for (r in qualityWordRegexes) s = r.replace(s, " ")
        s = nonAlnum.replace(s.lowercase(), " ").trim()
        return s.ifEmpty { raw.lowercase().trim() }
    }

    /** Higher = better quality tag present (4K -> tokens.size, none -> 0). */
    private fun qualityRank(name: String): Int {
        qualityWordRegexes.forEachIndexed { i, r -> if (r.containsMatchIn(name)) return QUALITY_TOKENS.size - i }
        return 0
    }

    /** IMDb/provider rating parsed to 0..1 (10-point scale), or 0 when absent/unparseable. */
    private fun ratingNorm(t: VodTitle): Double {
        val raw = (t.imdbRating ?: t.rating)?.trim()?.substringBefore('/') ?: return 0.0
        val v = raw.toDoubleOrNull() ?: return 0.0
        return (v / 10.0).coerceIn(0.0, 1.0)
    }

    /** Release year mapped to 0..1 over 1980..2040, or 0 when absent/unparseable. */
    private fun yearNorm(t: VodTitle): Double {
        val y = t.year?.let { Regex("(19|20)\\d{2}").find(it)?.value?.toIntOrNull() } ?: return 0.0
        return ((y - 1980).coerceIn(0, 60)) / 60.0
    }

    /** Live-now rail: de-dup quality variants, favor favorited categories, diversify + daily rotate. */
    fun curateChannels(
        pool: List<Channel>,
        categoryWeights: Map<String, Double>,
        daySeed: Long,
        limit: Int = 20,
    ): List<Channel> {
        if (pool.isEmpty()) return pool
        val deduped = pool
            .groupBy { baseName(it.name) }
            .map { (_, variants) -> variants.maxByOrNull { qualityRank(it.name) * 2 + (if (it.logoUrl != null) 1 else 0) }!! }
        return diversify(deduped, daySeed, limit, categoryOf = { it.categoryName }) { ch ->
            categoryWeights[ch.categoryName] ?: 0.0
        }
    }

    /** Movies/Series rail: de-dup title variants, rank taste + rating + recency, diversify + rotate. */
    fun curateTitles(
        pool: List<VodTitle>,
        categoryWeights: Map<String, Double>,
        daySeed: Long,
        limit: Int = 20,
    ): List<VodTitle> {
        if (pool.isEmpty()) return pool
        // Key on base name + year so a remake (same name, different year) stays distinct while
        // language/quality dupes of the same release collapse to their best-rated, poster-bearing one.
        val deduped = pool
            .groupBy { baseName(it.name) + "|" + (it.year ?: "") }
            .map { (_, variants) -> variants.maxByOrNull { (if (it.posterUrl != null) 2.0 else 0.0) + ratingNorm(it) }!! }
        return diversify(deduped, daySeed, limit, categoryOf = { it.categoryName }) { t ->
            0.6 * (categoryWeights[t.categoryName] ?: 0.0) + 0.25 * ratingNorm(t) + 0.15 * yearNorm(t)
        }
    }

    /** Recommended rail: a personalized best-of across movies + series (fed the raw pools, not the
     *  already-trimmed rails, so it has the whole sample to pick from). */
    fun recommend(
        moviePool: List<VodTitle>,
        seriesPool: List<VodTitle>,
        categoryWeights: Map<String, Double>,
        daySeed: Long,
        limit: Int = 12,
    ): List<VodTitle> = curateTitles(moviePool + seriesPool, categoryWeights, daySeed, limit)

    /**
     * Score every item, add a stable day-seeded jitter, then round-robin across categories: each
     * category contributes its best remaining item per pass, categories ordered by their strongest
     * item. This spreads genres across the visible head instead of front-loading one category.
     */
    private fun <T> diversify(
        items: List<T>,
        daySeed: Long,
        limit: Int,
        categoryOf: (T) -> String?,
        score: (T) -> Double,
    ): List<T> {
        val rnd = Random(daySeed)
        val scored = items.map { it to (score(it) + rnd.nextDouble() * DAILY_JITTER) }
        val groups = scored
            .groupBy { categoryOf(it.first) ?: "" }
            .map { (_, g) -> g.sortedByDescending { it.second } }
            .sortedByDescending { it.first().second }
        val result = ArrayList<T>(minOf(limit, items.size))
        var depth = 0
        while (result.size < limit) {
            var advanced = false
            for (g in groups) {
                if (depth < g.size) {
                    result.add(g[depth].first)
                    advanced = true
                    if (result.size == limit) break
                }
            }
            if (!advanced) break
            depth++
        }
        return result
    }
}
