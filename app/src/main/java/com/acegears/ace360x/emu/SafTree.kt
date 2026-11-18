package com.acegears.ace360x.emu

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.acegears.ace360x.core.NativeBridge
import java.util.concurrent.ConcurrentHashMap

/**
 * Backs Xenia's "acetree://" paths (native/host/saf_tree_provider.cc, upstream
 * patch 0008) with Storage Access Framework folders. Path format:
 *
 *   acetree://<Uri.encode(treeUri)>[/<name>/<name>/…]
 *
 * The encoded tree URI contains no '/', so the first '/' after the scheme
 * starts the path inside the folder. Names are matched exactly (Xenia's VFS
 * does guest case-insensitivity itself, using the names we list).
 *
 * Called from native emulator threads; everything here is thread-safe.
 */
object SafTree {
    const val SCHEME = "acetree"
    private const val PREFIX = "$SCHEME://"
    private const val TAG = "SafTree"

    @Volatile private var appContext: Context? = null

    /** relative-path key → document id, per tree. Filled by listings. */
    private val ids = ConcurrentHashMap<String, String>()

    /** Registers the provider with Xenia. Call before the emulator starts. */
    fun install(context: Context): Boolean {
        appContext = context.applicationContext
        return NativeBridge.installSafTree(SafTree::class.java)
    }

    fun pathFor(treeUri: Uri, relativePath: String): String {
        val rel = relativePath.trim('/')
        return PREFIX + Uri.encode(treeUri.toString()) + if (rel.isEmpty()) "" else "/$rel"
    }

    /** (tree URI, relative path) for a path, or null if it isn't ours. Pure; unit-tested. */
    fun parse(path: String): Pair<String, String>? {
        if (!path.startsWith(PREFIX)) return null
        val rest = path.substring(PREFIX.length)
        val slash = rest.indexOf('/')
        val encodedTree = if (slash < 0) rest else rest.substring(0, slash)
        if (encodedTree.isEmpty()) return null
        val rel = if (slash < 0) "" else rest.substring(slash + 1).split('/').filter { it.isNotEmpty() }.joinToString("/")
        return Uri.decode(encodedTree) to rel
    }

    private data class Row(val id: String, val name: String, val dir: Boolean, val size: Long, val mtime: Long) {
        fun encode() = "${if (dir) "d" else "f"}\t${size.coerceAtLeast(0)}\t${mtime.coerceAtLeast(0)}\t$name"
    }

    private val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )

    private fun key(tree: String, rel: String) = "$tree\u0000$rel"

    private fun children(tree: Uri, treeKey: String, parentId: String, parentRel: String): List<Row> {
        val ctx = appContext ?: return emptyList()
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val rows = mutableListOf<Row>()
        ctx.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val row = Row(
                    id, name,
                    dir = c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
                    size = if (c.isNull(3)) 0 else c.getLong(3),
                    mtime = if (c.isNull(4)) 0 else c.getLong(4),
                )
                rows += row
                ids[key(treeKey, if (parentRel.isEmpty()) name else "$parentRel/$name")] = id
            }
        }
        return rows
    }

    /** Document id for a relative path, walking (and caching) from the tree root. */
    private fun resolve(treeKey: String, rel: String): String? {
        val tree = Uri.parse(treeKey)
        val root = DocumentsContract.getTreeDocumentId(tree)
        if (rel.isEmpty()) return root
        ids[key(treeKey, rel)]?.let { return it }
        var currentId = root
        var currentRel = ""
        for (part in rel.split('/')) {
            val nextRel = if (currentRel.isEmpty()) part else "$currentRel/$part"
            val cached = ids[key(treeKey, nextRel)]
            currentId = cached ?: run {
                children(tree, treeKey, currentId, currentRel)
                ids[key(treeKey, nextRel)]
            } ?: return null
            currentRel = nextRel
        }
        return currentId
    }

    @JvmStatic
    fun stat(path: String): String? = try {
        val (treeKey, rel) = parse(path) ?: return null
        val ctx = appContext ?: return null
        val tree = Uri.parse(treeKey)
        val id = resolve(treeKey, rel) ?: return null
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, id)
        ctx.contentResolver.query(doc, projection, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return null
            Row(
                id, rel.substringAfterLast('/'),
                dir = c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
                size = if (c.isNull(3)) 0 else c.getLong(3),
                mtime = if (c.isNull(4)) 0 else c.getLong(4),
            ).encode()
        }
    } catch (e: Exception) {
        Log.w(TAG, "stat($path) failed", e)
        null
    }

    @JvmStatic
    fun list(path: String): Array<String>? = try {
        val (treeKey, rel) = parse(path) ?: return null
        val id = resolve(treeKey, rel) ?: return null
        children(Uri.parse(treeKey), treeKey, id, rel).map { it.encode() }.toTypedArray()
    } catch (e: Exception) {
        Log.w(TAG, "list($path) failed", e)
        null
    }

    @JvmStatic
    fun openFd(path: String, write: Boolean): Int = try {
        val (treeKey, rel) = parse(path) ?: return -1
        val ctx = appContext ?: return -1
        val tree = Uri.parse(treeKey)
        val id = resolve(treeKey, rel) ?: return -1
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, id)
        ctx.contentResolver.openFileDescriptor(doc, if (write) "rw" else "r")?.detachFd() ?: -1
    } catch (e: Exception) {
        Log.w(TAG, "openFd($path, write=$write) failed", e)
        -1
    }
}
