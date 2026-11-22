package com.acegears.ace360x.diag

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * One-shot GPU frame capture for debugging graphics on a device. When armed, the
 * next game launch passes upstream Xenia's `trace_gpu_prefix`/`trace_gpu_frames`
 * cvars, and the command processor writes `.xtr` traces of those guest frames.
 * The traces can be replayed with `xenia-gpu-vulkan-trace-dump` on another machine.
 * Traces contain the game's graphics data (textures, vertex data), so they are
 * kept in the app's private cache and only leave the phone if the user shares them.
 */
object GpuTrace {
    private const val PREFS = "gpu_trace"
    private const val KEY_ARMED = "armed"

    /** Guest frames to capture: ~15 s (intro) and ~30 s (menu) at 30 swaps/s. */
    const val FRAMES = "450,900"

    /** Inside cache/share/, which the diagnostics FileProvider already exposes. */
    fun dir(context: Context): File = File(context.cacheDir, "share/gputrace")

    fun isArmed(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ARMED, false)

    fun setArmed(context: Context, armed: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ARMED, armed).apply()
    }

    /** Called when launching a game: returns the trace directory and disarms, or null. */
    fun consumeForLaunch(context: Context): File? {
        if (!isArmed(context)) return null
        setArmed(context, false)
        val d = dir(context)
        d.deleteRecursively()
        d.mkdirs()
        return d
    }

    fun traces(context: Context): List<File> =
        dir(context).listFiles { f -> f.isFile && f.name.endsWith(".xtr") }?.sortedBy { it.name }.orEmpty()

    fun delete(context: Context) {
        dir(context).deleteRecursively()
    }

    /**
     * Some apps (e.g. the Claude app) don't accept binary `.xtr` files but do accept
     * text files. Writes each trace as gzip + Base64 text with a one-line header, in
     * share/gputrace-text/. Blocking; call off the main thread.
     */
    fun writeTextCopies(context: Context, files: List<File>): List<File> {
        val out = File(context.cacheDir, "share/gputrace-text")
        out.deleteRecursively()
        out.mkdirs()
        return files.map { src ->
            val gz = java.io.ByteArrayOutputStream()
            java.util.zip.GZIPOutputStream(gz).use { z -> src.inputStream().use { it.copyTo(z) } }
            val dst = File(out, src.name + ".gz.b64.txt")
            dst.bufferedWriter().use { w ->
                w.write("ACE360X-GPU-TRACE ${src.name} bytes=${src.length()} format=gzip+base64\n")
                w.write(android.util.Base64.encodeToString(gz.toByteArray(), android.util.Base64.DEFAULT))
            }
            dst
        }
    }

    fun shareTextIntent(context: Context, files: List<File>): Intent {
        val uris = ArrayList(
            files.map { FileProvider.getUriForFile(context, "${context.packageName}.diagnostics", it) },
        )
        val send = Intent(Intent.ACTION_SEND_MULTIPLE)
            .setType("text/plain")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            .putExtra(Intent.EXTRA_SUBJECT, "Ace360X GPU frame traces (text)")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, "Share GPU captures as text")
    }

    fun shareIntent(context: Context, files: List<File>): Intent {
        val uris = ArrayList(
            files.map { FileProvider.getUriForFile(context, "${context.packageName}.diagnostics", it) },
        )
        val send = Intent(Intent.ACTION_SEND_MULTIPLE)
            .setType("application/octet-stream")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            .putExtra(Intent.EXTRA_SUBJECT, "Ace360X GPU frame traces")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, "Share GPU traces")
    }
}
