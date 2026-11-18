package com.acegears.ace360x.library

/** Pure search / filter / sort for the library list. */
object LibraryFilter {
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
