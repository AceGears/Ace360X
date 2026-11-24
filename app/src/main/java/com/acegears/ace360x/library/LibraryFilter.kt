package com.acegears.ace360x.library

private val GAME_FILE_EXTENSIONS = setOf("iso", "xex", "zar")
private val FILLER_WORDS = setOf("the", "of", "a", "an", "and")

/** A game's name for display: the file name without its ".iso"/".xex" ending. */
fun gameTitle(fileName: String): String {
    val dot = fileName.lastIndexOf('.')
    val stripped = if (dot > 0 && fileName.substring(dot + 1).lowercase() in GAME_FILE_EXTENSIONS) fileName.substring(0, dot) else fileName
    return stripped.trim().ifEmpty { fileName }
}

/**
 * Up to two letters standing in for a cover: the first letters of the first two words that
 * carry meaning ("The", "of" and the like are skipped when there are other words).
 */
fun coverInitials(name: String): String {
    val all = name.split(' ', '-', '_', ':', '.', '(', ')').filter { w -> w.any { it.isLetterOrDigit() } }
    val words = all.filter { it.lowercase() !in FILLER_WORDS }.ifEmpty { all }
    val letters = words.take(2).mapNotNull { w -> w.firstOrNull { it.isLetterOrDigit() } }
    return letters.joinToString("").uppercase().ifEmpty { "?" }
}

/** The tabs of the home dock. */
enum class HomeTab(val label: String) { ALL("All games"), FAVOURITES("Favourites"), RECENT("Recent") }

/** How games are laid out on the home screen. */
enum class LibraryLayout(val label: String) { GRID("Grid"), LARGE("Large covers"), LIST("List") }

/** Pure search / filter / sort for the library list. */
object LibraryFilter {
    /**
     * Games shown on [tab]: everything, favourites only, or the games that have been
     * played, most recent first (the chosen sort applies to the other two tabs).
     */
    fun <T> forTab(
        games: List<T>,
        tab: HomeTab,
        query: String,
        favorites: Set<String>,
        sort: SortMode,
        name: (T) -> String,
        key: (T) -> String,
        size: (T) -> Long,
        stat: (T) -> PlayStat,
    ): List<T> = when (tab) {
        HomeTab.ALL -> apply(games, query, false, favorites, sort, name, key, size, stat)
        HomeTab.FAVOURITES -> apply(games, query, true, favorites, sort, name, key, size, stat)
        HomeTab.RECENT ->
            apply(games.filter { stat(it).lastPlayedMillis > 0 }, query, false, favorites, SortMode.RECENT, name, key, size, stat)
    }

    /** The game to offer under "Continue playing": the most recently played one, if any. */
    fun <T> continuePlaying(games: List<T>, stat: (T) -> PlayStat): T? =
        games.filter { stat(it).lastPlayedMillis > 0 }.maxByOrNull { stat(it).lastPlayedMillis }

    fun <T> apply(
        games: List<T>,
        query: String,
        favoritesOnly: Boolean,
        favorites: Set<String>,
        sort: SortMode,
        name: (T) -> String,
        key: (T) -> String,
        size: (T) -> Long,
        stat: (T) -> PlayStat,
    ): List<T> {
        val q = query.trim().lowercase()
        val filtered = games.filter { g ->
            (q.isEmpty() || name(g).lowercase().contains(q)) && (!favoritesOnly || key(g) in favorites)
        }
        val byName = compareBy<T> { name(it).lowercase() }
        return when (sort) {
            SortMode.NAME -> filtered.sortedWith(byName)
            SortMode.SIZE -> filtered.sortedWith(compareByDescending<T> { size(it) }.then(byName))
            SortMode.RECENT -> filtered.sortedWith(compareByDescending<T> { stat(it).lastPlayedMillis }.then(byName))
            SortMode.PLAYTIME -> filtered.sortedWith(compareByDescending<T> { stat(it).totalSeconds }.then(byName))
        }
    }

    fun apply(
        games: List<GameEntry>,
        query: String,
        favoritesOnly: Boolean,
        favorites: Set<String>,
        sort: SortMode,
        stat: (GameEntry) -> PlayStat,
    ): List<GameEntry> = apply(
        games, query, favoritesOnly, favorites, sort,
        name = { it.displayName }, key = { it.uri.toString() }, size = { it.sizeBytes }, stat = stat,
    )
}
