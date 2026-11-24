package com.acegears.ace360x.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acegears.ace360x.theme.AceCard
import com.acegears.ace360x.theme.AceEmptyState
import com.acegears.ace360x.theme.AceIcons
import com.acegears.ace360x.theme.AceLinearProgress
import com.acegears.ace360x.theme.AceTopBar
import com.acegears.ace360x.theme.LocalAceTheme
import com.acegears.ace360x.theme.Vanishing
import com.acegears.ace360x.theme.aceFocusRing
import com.acegears.ace360x.theme.motionReduced
import com.acegears.ace360x.theme.vanishEffect
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

private fun folderLabel(uri: Uri): String =
    uri.lastPathSegment?.substringAfterLast(':')?.ifBlank { null } ?: uri.toString()

private fun scanSummary(r: ScanReport): String = buildString {
    append("Found ${r.games.size} games in ${r.filesVisited} files")
    if (r.unreadable > 0) append(" · ${r.unreadable} couldn't be read")
    if (r.hitLimit) append(" · stopped at ${ScanPolicy.MAX_FILES_VISITED} files")
}

/** Settings → Game folders: the folders Ace360X scans. Removing one never deletes files. */
@Composable
fun FoldersScreen(onBack: () -> Unit, modifier: Modifier = Modifier, vm: LibraryViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val deleteEffect = LocalAceTheme.current.deleteEffect && !motionReduced()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(uri)
    }
    Column(modifier.fillMaxSize()) {
        AceTopBar("Game folders", onBack) {
            if (state.folders.isNotEmpty()) {
                IconButton(onClick = vm::rescan, enabled = !state.scanning, modifier = Modifier.aceFocusRing()) {
                    Icon(AceIcons.Refresh, contentDescription = "Rescan folders")
                }
            }
            IconButton(onClick = { picker.launch(null) }, modifier = Modifier.aceFocusRing()) {
                Icon(AceIcons.Add, contentDescription = "Add game folder")
            }
        }
        if (state.scanning) AceLinearProgress(Modifier.fillMaxWidth())
        if (state.folders.isEmpty()) {
            AceEmptyState(
                AceIcons.Library, "No game folders yet",
                "Pick a folder with your own game backups (ISO, XEX or STFS). Files stay where they are; " +
                    "Ace360X only reads them.",
                Modifier.padding(top = 24.dp),
            ) { Button(onClick = { picker.launch(null) }) { Text("Add a game folder") } }
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    state.report?.let(::scanSummary) ?: "Checking folders…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.folders, key = { it.toString() }) { folder ->
                val key = "folder:$folder"
                val missing = state.report?.missingFolders?.contains(folder) == true
                AceCard(Modifier.fillMaxWidth().vanishEffect(key in Vanishing.keys)) {
                    Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                            Text(folderLabel(folder), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                            if (missing) {
                                Text(
                                    "Not accessible — add this folder again",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                // Finish the removal even if this screen is left during the effect.
                                scope.launch(NonCancellable) { Vanishing.run(key, deleteEffect) { vm.removeFolder(folder) } }
                            },
                            enabled = key !in Vanishing.keys,
                        ) {
                            Icon(AceIcons.Close, contentDescription = "Remove folder from library (files are not deleted)")
                        }
                    }
                }
            }
        }
    }
}
