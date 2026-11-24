package com.acegears.ace360x.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acegears.ace360x.settings.GameOverrides
import com.acegears.ace360x.settings.GameOverridesStore
import com.acegears.ace360x.theme.AceCard
import com.acegears.ace360x.theme.AceEmptyState
import com.acegears.ace360x.theme.AceIcons
import com.acegears.ace360x.theme.AceSectionHeader
import com.acegears.ace360x.theme.AceTopBar
import com.acegears.ace360x.theme.LocalAceTheme
import com.acegears.ace360x.theme.aceFocusRing
import com.acegears.ace360x.theme.aceGlow
import com.acegears.ace360x.theme.aceHeroBrush
import com.acegears.ace360x.theme.toColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A game's own page: cover, Play, what is known about it, play statistics, its settings. */
@Composable
fun GameDetailsScreen(gameUri: String, onBack: () -> Unit, modifier: Modifier = Modifier, vm: LibraryViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme = LocalAceTheme.current
    val game = state.allGames.firstOrNull { it.uri.toString() == gameUri }

    Column(modifier.fillMaxSize()) {
        if (game == null) {
            AceTopBar("Game", onBack)
            AceEmptyState(
                AceIcons.Library, "Not in the library",
                "This game isn't in your folders any more. Rescan your folders or add the folder again.",
            )
            return@Column
        }
        val favorite = gameUri in state.favorites
        val stat = state.stats[gameUri] ?: PlayStat()
        val launchable = game.launchTarget() != null
        var settingsOpen by remember { mutableStateOf(false) }
        // Null until read from disk; re-read after the settings dialog closes.
        var overrides by remember(gameUri) { mutableStateOf<GameOverrides?>(null) }
        LaunchedEffect(gameUri, settingsOpen) {
            if (!settingsOpen) {
                overrides = withContext(Dispatchers.IO) { GameOverridesStore.read(context.applicationContext, gameUri) }
            }
        }

        AceTopBar("", onBack) {
            IconButton(onClick = { vm.toggleFavorite(game) }, modifier = Modifier.aceFocusRing()) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = if (favorite) "Remove from favourites" else "Add to favourites",
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().background(aceHeroBrush()).padding(16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                GameCover(
                    game.displayName,
                    RoundedCornerShape(theme.cornerDp.dp),
                    Modifier.width(132.dp).aspectRatio(0.75f),
                    letterSize = 44.sp,
                )
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(game.displayName, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        gameMetaLine(game),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.aceGlow(theme.palette.glow.toColor(), if (launchable) theme.glowIntensity * 0.6f else 0f, 28.dp)) {
                    Button(
                        onClick = { scope.launch { launchGame(context, game) } },
                        enabled = launchable,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).aceFocusRing(),
                    ) {
                        Icon(AceIcons.Play, contentDescription = null)
                        Text("Play", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (!launchable) {
                    Text(
                        "Rescan this game's folder to make it startable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AceSectionHeader("Play statistics")
                AceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoLine("Last played", lastPlayedLabel(stat.lastPlayedMillis)?.removePrefix("Last played ") ?: "Never")
                        InfoLine("Time played", if (stat.totalSeconds > 0) PlayStats.formatPlaytime(stat.totalSeconds) else "None yet")
                    }
                }

                AceSectionHeader("About this game")
                AceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoLine("Format", game.format.label)
                        InfoLine("Size", ScanPolicy.formatSize(game.sizeBytes))
                        InfoLine("Compatibility", "Untested")
                        Text(
                            "Ace360X doesn't look games up in its compatibility list yet, so every game shows as Untested.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AceSectionHeader("Settings")
                AceCard(Modifier.fillMaxWidth(), onClick = { settingsOpen = true }) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Settings for this game", style = MaterialTheme.typography.titleMedium)
                            Text(
                                when {
                                    overrides == null -> "Loading…"
                                    overrides?.isEmpty == true -> "Uses the app's settings"
                                    else -> "Has its own settings"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(AceIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        GameSettingsHost(game, open = settingsOpen, onClose = { settingsOpen = false })
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
