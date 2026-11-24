package com.acegears.ace360x.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acegears.ace360x.diag.Diagnostics
import com.acegears.ace360x.diag.UnexpectedExitBanner
import com.acegears.ace360x.theme.AceCard
import com.acegears.ace360x.theme.AceDialog
import com.acegears.ace360x.theme.AceEmptyState
import com.acegears.ace360x.theme.AceIcons
import com.acegears.ace360x.theme.AceLinearProgress
import com.acegears.ace360x.theme.AceSectionHeader
import com.acegears.ace360x.theme.LocalAceTheme
import com.acegears.ace360x.theme.LocalMotion
import com.acegears.ace360x.theme.aceCardColor
import com.acegears.ace360x.theme.aceFocusRing
import com.acegears.ace360x.theme.aceGlow
import com.acegears.ace360x.theme.aceHeroBrush
import com.acegears.ace360x.theme.pressLift
import com.acegears.ace360x.theme.toColor
import kotlinx.coroutines.launch

/**
 * Home: the library, laid out as in AceArcade — a neon header with search, layout and
 * settings; "Continue playing"; the games as covers or a list; and a dock of tabs at the
 * bottom. Tap a game to start it, hold it for more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenGame: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolders: () -> Unit,
    modifier: Modifier = Modifier,
    vm: LibraryViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var layoutOpen by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<GameEntry?>(null) }
    var settingsFor by remember { mutableStateOf<GameEntry?>(null) }
    fun play(game: GameEntry) {
        scope.launch { launchGame(context, game) }
    }

    Column(modifier.fillMaxSize()) {
        HomeHeader(
            searchOpen = searchOpen,
            onSearch = {
                searchOpen = !searchOpen
                if (!searchOpen) vm.setQuery("")
            },
            onLayout = { layoutOpen = true },
            onSettings = onOpenSettings,
        )
        if (state.scanning) AceLinearProgress(Modifier.fillMaxWidth())
        AnimatedVisibility(searchOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            SearchField(state.query, vm::setQuery)
        }

        // Changing tab: the new games slide in from the side of the new tab.
        val motion = LocalMotion.current
        val tabIndex = state.tab.ordinal
        var previousIndex by remember { mutableIntStateOf(tabIndex) }
        val enter = remember { Animatable(1f) }
        val direction = if (tabIndex >= previousIndex) 1f else -1f
        LaunchedEffect(tabIndex) {
            if (tabIndex != previousIndex && motion.mediumMs > 0) {
                enter.snapTo(0f)
                enter.animateTo(1f, tween(motion.mediumMs))
            }
            previousIndex = tabIndex
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                alpha = 0.35f + 0.65f * enter.value
                translationX = (1f - enter.value) * direction * 32.dp.toPx()
            },
        ) {
            val games = state.tabGames
            when {
                state.folders.isEmpty() -> AceEmptyState(
                    AceIcons.Library, "Your library is empty",
                    "Add a folder with your own game backups (ISO, XEX or STFS). Files stay where they are; " +
                        "Ace360X only reads them.",
                    Modifier.padding(top = 40.dp),
                ) { Button(onClick = { picker.launch(null) }) { Text("Add a game folder") } }

                else -> GameGrid(
                    state = state,
                    games = games,
                    showExitBanner = showExitBanner,
                    onDismissBanner = { showExitBanner = false },
                    onPlay = { play(it) },
                    onMenu = { menuFor = it },
                    onToggleFavorite = vm::toggleFavorite,
                    onOpenFolders = onOpenFolders,
                )
            }
        }
        if (state.folders.isNotEmpty()) {
            HomeDock(
                selected = state.tab,
                counts = HomeTab.entries.associateWith { state.gamesOn(it).size },
                onSelect = vm::setTab,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }

    if (layoutOpen) {
        LayoutSheet(state, vm, onFolders = { layoutOpen = false; onOpenFolders() }, onDismiss = { layoutOpen = false })
    }
    menuFor?.let { game ->
        GameMenuSheet(
            game = game,
            favorite = game.uri.toString() in state.favorites,
            onPlay = { menuFor = null; play(game) },
            onDetails = { menuFor = null; onOpenGame(game.uri.toString()) },
            onSettings = { menuFor = null; settingsFor = game },
            onToggleFavorite = { vm.toggleFavorite(game) },
            onDismiss = { menuFor = null },
        )
    }
    settingsFor?.let { game -> GameSettingsHost(game, open = true, onClose = { settingsFor = null }) }

    state.error?.let { message ->
        AceDialog(
            onDismissRequest = vm::dismissError,
            confirmButton = { TextButton(onClick = vm::dismissError) { Text("OK") } },
            title = { Text("Library") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun HomeHeader(searchOpen: Boolean, onSearch: () -> Unit, onLayout: () -> Unit, onSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(aceHeroBrush())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Wordmark: the name set in the heading type, the number in the theme's accent.
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = cs.onBackground)) { append("ACE") }
                withStyle(SpanStyle(color = cs.primary)) { append("360X") }
            },
            style = MaterialTheme.typography.headlineMedium,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f).semantics { contentDescription = "Ace360X" },
        )
        IconButton(onClick = onSearch, modifier = Modifier.aceFocusRing()) {
            Icon(if (searchOpen) AceIcons.Close else AceIcons.Search, if (searchOpen) "Close search" else "Search", tint = cs.onBackground)
        }
        IconButton(onClick = onLayout, modifier = Modifier.aceFocusRing()) {
            Icon(AceIcons.More, "Layout and sorting", tint = cs.onBackground)
        }
        IconButton(onClick = onSettings, modifier = Modifier.aceFocusRing()) {
            Icon(AceIcons.Settings, "Settings", tint = cs.onBackground)
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        placeholder = { Text("Search games") },
        leadingIcon = { Icon(AceIcons.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) { Icon(AceIcons.Close, "Clear search") }
            }
        },
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun GameGrid(
    state: LibraryUiState,
    games: List<GameEntry>,
    showExitBanner: Boolean,
    onDismissBanner: () -> Unit,
    onPlay: (GameEntry) -> Unit,
    onMenu: (GameEntry) -> Unit,
    onToggleFavorite: (GameEntry) -> Unit,
    onOpenFolders: () -> Unit,
) {
    val cells = when (state.layout) {
        LibraryLayout.GRID -> GridCells.Adaptive(minSize = 140.dp)
        LibraryLayout.LARGE -> GridCells.Adaptive(minSize = 220.dp)
        LibraryLayout.LIST -> GridCells.Fixed(1)
    }
    LazyVerticalGrid(
        columns = cells,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (showExitBanner) item(span = { GridItemSpan(maxLineSpan) }) { UnexpectedExitBanner(onDismiss = onDismissBanner) }
        if (state.tab == HomeTab.ALL && state.query.isBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) { StatusNote() }
            state.continuePlaying?.let { game ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ContinuePlaying(game, state.stats[game.uri.toString()] ?: PlayStat(), onPlay = { onPlay(game) })
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            AceSectionHeader(
                when (state.tab) {
                    HomeTab.ALL -> "Games (${games.size})"
                    HomeTab.FAVOURITES -> "Favourites (${games.size})"
                    HomeTab.RECENT -> "Recently played (${games.size})"
                },
            )
        }
        if (games.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                when {
                    state.query.isNotBlank() ->
                        AceEmptyState(AceIcons.Search, "No matches", "No game matches “${state.query}”.")
                    state.tab == HomeTab.FAVOURITES ->
                        AceEmptyState(Icons.Rounded.Star, "No favourites yet", "Hold a game and choose “Add to favourites”.")
                    state.tab == HomeTab.RECENT ->
                        AceEmptyState(AceIcons.Play, "Nothing played yet", "Games you start will show up here.")
                    state.scanning -> AceEmptyState(AceIcons.Refresh, "Looking for games", "Checking your folders…")
                    else -> AceEmptyState(
                        AceIcons.Library, "No games found",
                        "None of your folders contains a disc image, XEX or package Ace360X recognises.",
                    ) { TextButton(onClick = onOpenFolders) { Text("Game folders") } }
                }
            }
        }
        items(games, key = { it.uri.toString() }) { game ->
            val favorite = game.uri.toString() in state.favorites
            val stat = state.stats[game.uri.toString()] ?: PlayStat()
            if (state.layout == LibraryLayout.LIST) {
                GameListRow(game, favorite, stat, onClick = { onPlay(game) }, onMenu = { onMenu(game) }, onToggleFavorite = { onToggleFavorite(game) })
            } else {
                GameTile(game, favorite, onClick = { onPlay(game) }, onMenu = { onMenu(game) })
            }
        }
    }
}

@Composable
private fun StatusNote() {
    AceCard(
        Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            "Experimental build. Tap a game to start it, hold it for more. Expect many games not to " +
                "boot or to run slowly. Play with a Bluetooth/USB controller or the on-screen controls.",
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ContinuePlaying(game: GameEntry, stat: PlayStat, onPlay: () -> Unit) {
    val theme = LocalAceTheme.current
    AceCard(Modifier.fillMaxWidth(), onClick = onPlay, glow = true) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            GameCover(game.displayName, RoundedCornerShape((theme.cornerDp * 0.6f).dp), Modifier.size(64.dp), letterSize = 22.sp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("CONTINUE PLAYING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(game.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                lastPlayedLabel(stat.lastPlayedMillis)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(AceIcons.Play, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
    }
}

/** A game as a cover with its name underneath. Tap starts it; holding opens the menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameTile(game: GameEntry, favorite: Boolean, onClick: () -> Unit, onMenu: () -> Unit) {
    val theme = LocalAceTheme.current
    val cs = MaterialTheme.colorScheme
    val launchable = game.launchTarget() != null
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(theme.cornerDp.dp)
    Column(
        Modifier
            .pressLift(interaction)
            .then(if (favorite) Modifier.aceGlow(theme.palette.glow.toColor(), theme.glowIntensity * 0.5f, theme.cornerDp.dp) else Modifier)
            .background(aceCardColor(), shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = if (launchable) "Start ${game.displayName}" else null,
                onLongClickLabel = "More about ${game.displayName}",
                onLongClick = onMenu,
                onClick = { if (launchable) onClick() else onMenu() },
            )
            .aceFocusRing()
            .padding(6.dp),
    ) {
        Box {
            GameCover(game.displayName, RoundedCornerShape((theme.cornerDp * 0.75f).dp), Modifier.fillMaxWidth().aspectRatio(0.75f))
            if (favorite) {
                Icon(
                    Icons.Rounded.Star, contentDescription = "Favourite", tint = cs.onPrimaryContainer,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp),
                )
            }
        }
        Text(
            game.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = cs.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
        )
        Text(
            if (launchable) game.format.label else "Rescan needed",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun GameListRow(
    game: GameEntry,
    favorite: Boolean,
    stat: PlayStat,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val theme = LocalAceTheme.current
    val launchable = game.launchTarget() != null
    AceCard(Modifier.fillMaxWidth(), onClick = { if (launchable) onClick() else onMenu() }) {
        Row(Modifier.fillMaxWidth().padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            GameCover(game.displayName, RoundedCornerShape((theme.cornerDp * 0.5f).dp), Modifier.size(48.dp), letterSize = 18.sp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(game.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                Text(gameMetaLine(game), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val played = listOfNotNull(
                    lastPlayedLabel(stat.lastPlayedMillis),
                    stat.totalSeconds.takeIf { it > 0 }?.let { "played " + PlayStats.formatPlaytime(it) },
                ).joinToString(" · ")
                if (played.isNotEmpty()) {
                    Text(played, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!launchable) {
                    Text(
                        "Rescan this folder to make this game startable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = if (favorite) "Remove from favourites" else "Add to favourites",
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
            IconButton(onClick = onMenu) { Icon(AceIcons.More, contentDescription = "More about ${game.displayName}") }
        }
    }
}

/** Layout, sorting and a way to the game folders. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayoutSheet(state: LibraryUiState, vm: LibraryViewModel, onFolders: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            SheetTitle("Layout")
            LibraryLayout.entries.forEach { l -> SheetChoice(l.label, state.layout == l) { vm.setLayout(l) } }
            SheetTitle("Sort by")
            SortMode.entries.forEach { s -> SheetChoice(s.label, state.sort == s) { vm.setSort(s) } }
            SheetAction("Game folders", onFolders)
        }
    }
}

/** What you can do with one game (opened by holding it). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameMenuSheet(
    game: GameEntry,
    favorite: Boolean,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onSettings: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            SheetTitle(game.displayName)
            if (game.launchTarget() != null) SheetAction("Play", onPlay)
            SheetAction("Game page", onDetails)
            SheetAction("Settings for this game", onSettings)
            SheetAction(if (favorite) "Remove from favourites" else "Add to favourites", onToggleFavorite)
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun SheetChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
    }
}
