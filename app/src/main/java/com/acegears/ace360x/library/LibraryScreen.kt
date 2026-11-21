package com.acegears.ace360x.library

import com.acegears.ace360x.settings.EmulatorSettings
import com.acegears.ace360x.settings.GameSettingsDialog
import com.acegears.ace360x.settings.GameOverridesStore
import com.acegears.ace360x.settings.GameOverrides
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.first
import com.acegears.ace360x.theme.ThemeRepository
import com.acegears.ace360x.theme.AceIcons
import com.acegears.ace360x.theme.AceDialog
import com.acegears.ace360x.theme.AceLinearProgress
import com.acegears.ace360x.theme.AceCard
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.acegears.ace360x.settings.SettingsRepository
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.acegears.ace360x.diag.Diagnostics
import com.acegears.ace360x.diag.UnexpectedExitBanner
import com.acegears.ace360x.emu.EmulatorActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(modifier: Modifier = Modifier, vm: LibraryViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExitBanner by remember { mutableStateOf(Diagnostics.consumeUnexpectedExit(context)) }
    // Play stats are written by the game process; re-read them when we come back.
    LifecycleResumeEffect(Unit) {
        vm.refreshStats()
        if (Diagnostics.consumeUnexpectedExit(context)) showExitBanner = true
        onPauseOrDispose { }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(uri)
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Library") },
            actions = {
                if (state.folders.isNotEmpty()) {
                    IconButton(onClick = vm::rescan, enabled = !state.scanning) {
                        Icon(AceIcons.Refresh, contentDescription = "Rescan folders")
                    }
                }
                IconButton(onClick = { picker.launch(null) }) {
                    Icon(AceIcons.Add, contentDescription = "Add game folder")
                }
            },
        )
        if (state.scanning) AceLinearProgress(Modifier.fillMaxWidth())

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showExitBanner) item { UnexpectedExitBanner(onDismiss = { showExitBanner = false }) }
            item { StatusNote() }

            if (state.folders.isEmpty()) {
                item { EmptyLibrary(onAdd = { picker.launch(null) }) }
            } else {
                item { SectionTitle("Folders") }
                items(state.folders, key = { it.toString() }) { folder ->
                    val missing = state.report?.missingFolders?.contains(folder) == true
                    FolderRow(folder, missing, onRemove = { vm.removeFolder(folder) })
                }
                item {
                    SectionTitle("Games (${state.allGames.size})")
                    state.report?.let {
                        Text(
                            scanSummary(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } ?: if (state.cachedGames.isNotEmpty()) {
                        Text(
                            "From the last scan — checking folders…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else Unit
                }
                if (state.allGames.isNotEmpty()) {
                    item { LibraryControls(state, vm) }
                }
                val games = state.visibleGames
                if (state.allGames.isNotEmpty() && games.isEmpty()) {
                    item { Text("No games match.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(games, key = { it.uri.toString() }) { game ->
                    GameRow(
                        game = game,
                        favorite = game.uri.toString() in state.favorites,
                        stat = state.stats[game.uri.toString()] ?: PlayStat(),
                        onToggleFavorite = { vm.toggleFavorite(game) },
                    )
                }
            }
        }
    }

    state.error?.let { message ->
        AceDialog(
            onDismissRequest = vm::dismissError,
            confirmButton = { TextButton(onClick = vm::dismissError) { Text("OK") } },
            title = { Text("Library") },
            text = { Text(message) },
        )
    }
}

private fun scanSummary(r: ScanReport): String = buildString {
    append("Checked ${r.filesVisited} files")
    if (r.unreadable > 0) append(" · ${r.unreadable} couldn't be read")
    if (r.hitLimit) append(" · stopped at ${ScanPolicy.MAX_FILES_VISITED} files")
}

@Composable
private fun StatusNote() {
    AceCard(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            "Experimental build. Tap a disc image or package to start it. Expect many games " +
                "not to boot or to run slowly. Play with a Bluetooth/USB controller or the " +
                "on-screen controls.",
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EmptyLibrary(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No game folders yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick a folder with your own game backups (ISO, XEX or STFS). " +
                "Files stay where they are; Ace360X only reads them.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onAdd) { Text("Add folder") }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun FolderRow(folder: Uri, missing: Boolean, onRemove: () -> Unit) {
    AceCard {
        Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(
                    folderLabel(folder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (missing) {
                    Text(
                        "Not accessible — re-add this folder",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(AceIcons.Close, contentDescription = "Remove folder from library (files are not deleted)")
            }
        }
    }
}

private fun folderLabel(uri: Uri): String =
    uri.lastPathSegment?.substringAfterLast(':')?.ifBlank { null } ?: uri.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryControls(state: LibraryUiState, vm: LibraryViewModel) {
    var sortOpen by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search games") },
            leadingIcon = { Icon(AceIcons.Search, contentDescription = null) },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.favoritesOnly,
                onClick = { vm.setFavoritesOnly(!state.favoritesOnly) },
                label = { Text("Favourites") },
                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
            )
            Box {
                AssistChip(onClick = { sortOpen = true }, label = { Text("Sort: ${state.sort.label}") })
                DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                vm.setSort(mode)
                                sortOpen = false
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun lastPlayedLabel(millis: Long): String? {
    if (millis <= 0) return null
    return "Last played " + DateUtils.getRelativeTimeSpanString(
        millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
    )
}

@Composable
private fun GameRow(game: GameEntry, favorite: Boolean, stat: PlayStat, onToggleFavorite: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val target = game.launchTarget()
    val launchable = target != null
    val gameKey = game.uri.toString()
    // Null until loaded from disk.
    var loaded by remember(gameKey) { mutableStateOf<GameOverrides?>(null) }
    val overrides = loaded ?: GameOverrides()
    LaunchedEffect(gameKey) {
        loaded = withContext(Dispatchers.IO) { GameOverridesStore.read(context.applicationContext, gameKey) }
    }
    // Non-null while the per-game settings dialog is open; holds the global settings it shows.
    var dialogGlobal by remember { mutableStateOf<EmulatorSettings?>(null) }
    dialogGlobal?.let { global ->
        GameSettingsDialog(
            gameName = game.displayName,
            global = global,
            initial = overrides,
            onSave = { o ->
                loaded = o
                dialogGlobal = null
                scope.launch(Dispatchers.IO) { GameOverridesStore.write(context.applicationContext, gameKey, o) }
            },
            onDismiss = { dialogGlobal = null },
        )
    }
    AceCard(
        onClick = {
            scope.launch {
                val global = SettingsRepository(context.applicationContext).current()
                // UI state includes a just-saved change; fall back to disk if not loaded yet.
                val settings = (
                    loaded ?: withContext(Dispatchers.IO) {
                        GameOverridesStore.read(context.applicationContext, gameKey)
                    }
                    ).applyTo(global)
                val theme = ThemeRepository(context.applicationContext).customization.first()
                val t = target ?: return@launch
                context.startActivity(
                    EmulatorActivity.launchIntent(
                        context, t, game.displayName, settings, statsKey = game.uri.toString(), theme = theme,
                    ),
                )
            }
        },
        enabled = launchable,
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(game.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${game.format.label} · ${ScanPolicy.formatSize(game.sizeBytes)} · Untested",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val played = listOfNotNull(
                    lastPlayedLabel(stat.lastPlayedMillis),
                    stat.totalSeconds.takeIf { it > 0 }?.let { "played " + PlayStats.formatPlaytime(it) },
                ).joinToString(" · ")
                if (played.isNotEmpty()) {
                    Text(played, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!overrides.isEmpty) {
                    Text(
                        "Has its own settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if (!launchable) {
                    Text(
                        "Rescan this folder to make this game startable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = { scope.launch { dialogGlobal = SettingsRepository(context.applicationContext).current() } },
                enabled = loaded != null,
            ) {
                Icon(AceIcons.Settings, contentDescription = "Settings for ${game.displayName}")
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (favorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (favorite) "Remove from favourites" else "Add to favourites",
                    tint = if (favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
