package com.acegears.ace360x.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortMode(val label: String) { NAME("Name"), RECENT("Recently played"), PLAYTIME("Most played"), SIZE("Size") }

data class LibraryUiState(
    val folders: List<Uri> = emptyList(),
    val scanning: Boolean = false,
    val report: ScanReport? = null,
    /** Games to show before the first scan of this launch finishes (from the cache). */
    val cachedGames: List<GameEntry> = emptyList(),
    val error: String? = null,
    val query: String = "",
    val sort: SortMode = SortMode.NAME,
    val favoritesOnly: Boolean = false,
    val favorites: Set<String> = emptySet(),
    val stats: Map<String, PlayStat> = emptyMap(),
) {
    val allGames: List<GameEntry> get() = report?.games ?: cachedGames

    /** Search, favourites filter and sort applied (pure; unit-tested via LibraryFilter). */
    val visibleGames: List<GameEntry>
        get() = LibraryFilter.apply(allGames, query, favoritesOnly, favorites, sort) { stats[it.uri.toString()] ?: PlayStat() }
}

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LibraryRepository(app)
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { repo.loadCache() }
            _state.update { it.copy(cachedGames = cached) }
            refreshStats()
        }
        viewModelScope.launch {
            repo.folders.distinctUntilChanged().collect { folders ->
                _state.update { it.copy(folders = folders) }
                rescan()
            }
        }
        viewModelScope.launch {
            repo.favorites.collect { fav -> _state.update { it.copy(favorites = fav) } }
        }
    }

    fun addFolder(uri: Uri) = viewModelScope.launch {
        runCatching { repo.addFolder(uri) }
            .onFailure { e -> _state.update { it.copy(error = "Couldn't keep access to that folder: ${e.message}") } }
    }

    fun removeFolder(uri: Uri) = viewModelScope.launch { repo.removeFolder(uri) }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setSort(s: SortMode) = _state.update { it.copy(sort = s) }
    fun setFavoritesOnly(v: Boolean) = _state.update { it.copy(favoritesOnly = v) }
    fun toggleFavorite(game: GameEntry) = viewModelScope.launch {
        repo.setFavorite(game.uri, game.uri.toString() !in _state.value.favorites)
    }

    /** Re-reads play stats written by the game process (call when the screen resumes). */
    fun refreshStats() {
        viewModelScope.launch {
            val games = _state.value.allGames
            val ctx = getApplication<Application>()
            val stats = withContext(Dispatchers.IO) {
                games.associate { it.uri.toString() to PlayStats.read(ctx, it.uri.toString()) }
            }
            _state.update { it.copy(stats = stats) }
        }
    }

    fun rescan() {
        scanJob?.cancel()
        val folders = _state.value.folders
        scanJob = viewModelScope.launch {
            _state.update { it.copy(scanning = true) }
            val result = runCatching { withContext(Dispatchers.IO) { repo.scan(folders) } }
            result.getOrNull()?.let { report -> withContext(Dispatchers.IO) { repo.saveCache(report.games) } }
            _state.update {
                it.copy(
                    scanning = false,
                    report = result.getOrNull() ?: it.report,
                    error = result.exceptionOrNull()?.let { e -> "Scan failed: ${e.message}" },
                )
            }
            refreshStats()
        }
    }
}
