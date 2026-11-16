package com.acegears.ace360x.library

import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.acegears.ace360x.emu.EmulatorActivity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(modifier: Modifier = Modifier, vm: LibraryViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(uri)
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Library") },
            actions = {
                if (state.folders.isNotEmpty()) {
                    IconButton(onClick = vm::rescan, enabled = !state.scanning) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Rescan folders")
                    }
                }
                IconButton(onClick = { picker.launch(null) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add game folder")
                }
            },
        )
        if (state.scanning) LinearProgressIndicator(Modifier.fillMaxWidth())

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { StatusNote() }

            if (state.folders.isEmpty()) {
                item { EmptyLibrary(onAdd = { picker.launch(null) }) }
            } else {
                item { SectionTitle("Folders") }
                items(state.folders, key = { it.toString() }) { folder ->
                    val missing = state.report?.missingFolders?.contains(folder) == true
                    FolderRow(folder, missing, onRemove = { vm.removeFolder(folder) })
                }
                val report = state.report
                if (report != null) {
                    item {
                        SectionTitle("Games (${report.games.size})")
                        Text(
                            scanSummary(report),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(report.games, key = { it.uri.toString() }) { GameRow(it) }
                }
            }
        }
    }

    state.error?.let { message ->
        AlertDialog(
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(
            "Experimental build. Tap a disc image or package to start it. Expect many games " +
                "not to boot or to run slowly. Connect a Bluetooth/USB controller to play; " +
                "on-screen touch controls aren't available yet.",
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
    Card {
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
                Icon(Icons.Filled.Close, contentDescription = "Remove folder from library (files are not deleted)")
            }
        }
    }
}

private fun folderLabel(uri: Uri): String =
    uri.lastPathSegment?.substringAfterLast(':')?.ifBlank { null } ?: uri.toString()

@Composable
private fun GameRow(game: GameEntry) {
    val context = LocalContext.current
    val launchable = game.format.isLaunchable
    Card(
        onClick = { context.startActivity(EmulatorActivity.launchIntent(context, game.uri, game.displayName)) },
        enabled = launchable,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(game.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${game.format.label} · ${ScanPolicy.formatSize(game.sizeBytes)} · Untested",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!launchable) {
                Text(
                    "Can't be started yet: a loose XEX needs its whole game folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
