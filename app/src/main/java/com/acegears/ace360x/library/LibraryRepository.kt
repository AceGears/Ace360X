package com.acegears.ace360x.library

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.acegears.ace360x.core.GameFormat
import com.acegears.ace360x.core.NativeBridge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryStore by preferencesDataStore(name = "library")
private val FOLDERS_KEY = stringSetPreferencesKey("folder_uris")

data class GameEntry(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val format: GameFormat,
    val folderUri: Uri,
)

data class ScanReport(
    val games: List<GameEntry>,
    val filesVisited: Int,
    val unreadable: Int,
    val hitLimit: Boolean,
    val missingFolders: List<Uri>,
)

/**
 * Game folders are user-picked SAF trees. We keep a persistable read grant
 * and never copy game files into app storage (brief §6).
 */
class LibraryRepository(private val context: Context) {
    private val resolver: ContentResolver get() = context.contentResolver

    val folders: Flow<List<Uri>> = context.libraryStore.data.map { prefs ->
        prefs[FOLDERS_KEY].orEmpty().sorted().map(Uri::parse)
    }

    suspend fun addFolder(treeUri: Uri) {
        resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.libraryStore.edit { it[FOLDERS_KEY] = it[FOLDERS_KEY].orEmpty() + treeUri.toString() }
    }

    /** Removes the folder from the library. Never deletes any files. */
    suspend fun removeFolder(treeUri: Uri) {
        context.libraryStore.edit { it[FOLDERS_KEY] = it[FOLDERS_KEY].orEmpty() - treeUri.toString() }
        runCatching {
            resolver.releasePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Blocking; call from Dispatchers.IO. */
    fun scan(folders: List<Uri>): ScanReport {
        val granted = resolver.persistedUriPermissions.filter { it.isReadPermission }.map { it.uri }.toSet()
        val games = mutableListOf<GameEntry>()
        val missing = mutableListOf<Uri>()
        val state = ScanState()

        for (tree in folders) {
            if (tree !in granted) {
                missing += tree
                continue
            }
            try {
                val rootId = DocumentsContract.getTreeDocumentId(tree)
                walk(tree, rootId, depth = 0, state, games)
            } catch (e: Exception) {
                Log.w(TAG, "Folder unavailable: $tree", e)
                missing += tree
            }
        }
        return ScanReport(
            games = games.sortedBy { it.displayName.lowercase() },
            filesVisited = state.visited,
            unreadable = state.unreadable,
            hitLimit = state.hitLimit,
            missingFolders = missing,
        )
    }

    private class ScanState {
        var visited = 0
        var unreadable = 0
        var hitLimit = false
    }

    private fun walk(tree: Uri, docId: String, depth: Int, state: ScanState, out: MutableList<GameEntry>) {
        if (depth > ScanPolicy.MAX_DEPTH) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        val subdirs = mutableListOf<String>()
        resolver.query(children, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                if (state.visited >= ScanPolicy.MAX_FILES_VISITED) {
                    state.hitLimit = true
                    return
                }
                state.visited++
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val mime = c.getString(2)
                val size = if (c.isNull(3)) -1L else c.getLong(3)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    subdirs += id
                } else if (ScanPolicy.isCandidate(name, size)) {
                    val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                    val format = identify(uri)
                    when {
                        format == null -> state.unreadable++
                        format.isRecognised -> out += GameEntry(uri, name, size, format, tree)
                    }
                }
            }
        }
        for (dir in subdirs) walk(tree, dir, depth + 1, state, out)
    }

    private fun identify(uri: Uri): GameFormat? = try {
        resolver.openFileDescriptor(uri, "r")?.use { pfd -> NativeBridge.sniffFormat(pfd.fd) }
    } catch (e: Exception) {
        Log.w(TAG, "Cannot open $uri", e)
        null
    }

    private companion object {
        const val TAG = "LibraryRepository"
    }
}
