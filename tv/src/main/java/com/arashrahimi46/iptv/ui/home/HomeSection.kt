package com.arashrahimi46.iptv.ui.home

/**
 * The six built-in Home rails, in their historical/default display order (Home.jsx). Each maps
 * 1:1 to a hardcoded rail block that used to live directly in [HomeScreen] before Home layout
 * customization: [CONTINUE_WATCHING] -> "Continue Watching", [LIVE_NOW] -> "Live now",
 * [CATEGORIES] -> "Browse by category", [RECOMMENDED] -> "Browse movies & series" (placeholder
 * recommendation rail), [MOVIES] -> "Movies", [SERIES] -> "Series".
 */
enum class BuiltinSection { CONTINUE_WATCHING, LIVE_NOW, CATEGORIES, RECOMMENDED, MOVIES, SERIES }

/** Which catalog a user-added category rail pulls from -- used by later Home customization steps. */
enum class CategoryKind { LIVE, MOVIE, SERIES }

/** One row in a user's customizable Home layout -- either a [Builtin] rail or a user-added
 * [Category] rail (categories land in a later step; the type exists now so the persisted format
 * is stable from the start). */
sealed interface HomeSection {
    /** True hides this section from Home without removing it from the layout (so re-showing it
     * later preserves its position instead of re-appending at the end). */
    val hidden: Boolean

    data class Builtin(val key: BuiltinSection, override val hidden: Boolean = false) : HomeSection

    data class Category(val kind: CategoryKind, val name: String, override val hidden: Boolean = false) : HomeSection
}

/** Today's fixed Home rail order, wrapped as the default customizable layout -- rendering this
 * list must be indistinguishable from the old hardcoded rail sequence. */
val DEFAULT_HOME_LAYOUT: List<HomeSection> = BuiltinSection.values().map { HomeSection.Builtin(it) }

/**
 * Serializes a Home layout to a plain newline-delimited string for DataStore (no
 * kotlinx.serialization dependency in this module). One section per line:
 * - Builtin: `builtin|<KEY>|<hidden>`
 * - Category: `cat|<KIND>|<hidden>|<name>` (name is last since it may itself contain `|`)
 *
 * The format is intentionally forward-compatible: [decodeHomeLayout] skips any line it doesn't
 * understand (unknown type tag, malformed field, unrecognized enum value) rather than throwing,
 * so a layout written by a future app version degrades gracefully on an older one instead of
 * corrupting the whole list.
 */
fun encodeHomeLayout(sections: List<HomeSection>): String =
    sections.joinToString("\n") { section ->
        when (section) {
            is HomeSection.Builtin -> "builtin|${section.key.name}|${section.hidden}"
            is HomeSection.Category -> "cat|${section.kind.name}|${section.hidden}|${section.name}"
        }
    }

/** Inverse of [encodeHomeLayout]. Never throws -- blank input yields an empty list, and any
 * malformed or unrecognized line is skipped rather than aborting the whole decode. */
fun decodeHomeLayout(raw: String): List<HomeSection> {
    if (raw.isBlank()) return emptyList()
    return raw.split("\n").mapNotNull { line ->
        if (line.isBlank()) return@mapNotNull null
        val parts = line.split("|")
        when (parts.getOrNull(0)) {
            "builtin" -> {
                if (parts.size < 3) return@mapNotNull null
                val key = runCatching { BuiltinSection.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
                val hidden = parts[2].toBooleanStrictOrNull() ?: return@mapNotNull null
                HomeSection.Builtin(key, hidden)
            }
            "cat" -> {
                if (parts.size < 4) return@mapNotNull null
                val kind = runCatching { CategoryKind.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
                val hidden = parts[2].toBooleanStrictOrNull() ?: return@mapNotNull null
                val name = parts.subList(3, parts.size).joinToString("|")
                if (name.isBlank()) return@mapNotNull null
                HomeSection.Category(kind, name, hidden)
            }
            else -> null
        }
    }
}
