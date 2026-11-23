package com.acegears.ace360x.diag

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.acegears.ace360x.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Builds a plain-text diagnostics report and shares it through the normal
 * Android share sheet. Nothing leaves the phone unless the user picks a target
 * (CLAUDE.md §12). The report contains: build/device info, why recent app
 * processes ended (Android 11+), the tail of Xenia's log, and recent logcat
 * lines from this app only.
 */
object Diagnostics {
    private const val MAX_XENIA_LOG_BYTES = 512 * 1024
    private const val SESSION_MARKER = "emu_session_running"

    fun xeniaDir(context: Context) = File(context.filesDir, "xenia")
    private fun marker(context: Context) = File(context.filesDir, SESSION_MARKER)

    // A file (not SharedPreferences) because the game runs in another process.
    fun markSessionStarted(context: Context) {
        runCatching { marker(context).writeText(System.currentTimeMillis().toString()) }
    }

    fun markSessionEndedCleanly(context: Context) {
        runCatching { marker(context).delete() }
    }

    /** True if the last game session didn't close normally (crash or kill). Clears the flag. */
    fun consumeUnexpectedExit(context: Context): Boolean {
        val m = marker(context)
        if (!m.exists()) return false
        // The game process is still running (or still shutting down): not an exit yet.
        if (emuProcessAlive(context)) return false
        m.delete()
        return true
    }

    private fun emuProcessAlive(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            ?: return false
        val name = context.packageName + ":emu"
        return am.runningAppProcesses.orEmpty().any { it.processName == name }
    }

    /** Blocking (reads files, runs logcat); call off the main thread. */
    fun buildReport(context: Context): File {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "ace360x-diagnostics-$stamp.txt")
        out.bufferedWriter().use { w ->
            w.appendLine("Ace360X diagnostics — $stamp")
            w.appendLine("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_SHA})")
            w.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            if (Build.VERSION.SDK_INT >= 31) w.appendLine("SoC: ${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}")
            w.appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            w.appendLine()

            val lastLaunch = runCatching {
                java.io.File(context.filesDir, com.acegears.ace360x.emu.EmulatorActivity.LAST_LAUNCH_FILE).readText()
            }.getOrNull()
            w.appendLine("Last game start: ${lastLaunch ?: "none recorded"}")
            val traces = GpuTrace.traces(context)
            w.appendLine(
                "GPU frame capture: armed for next launch=${GpuTrace.isArmed(context)}, " +
                    "captured files=${traces.size}" +
                    traces.joinToString("") { " [${it.name} ${it.length() / 1024} KB]" },
            )
            w.appendLine()
            w.appendLine("== Recent process exits ==")
            exitReasons(context).forEach { w.appendLine(it) }
            w.appendLine()

            w.appendLine("== Xenia log (tail) ==")
            val log = File(xeniaDir(context), "xenia.log")
            if (log.exists()) {
                w.appendLine(tail(log, MAX_XENIA_LOG_BYTES))
            } else {
                // Upstream Xenia on Android logs only to logcat (tag "xenia"), included below.
                w.appendLine("(Xenia logs to logcat on Android; see the logcat section)")
            }
            w.appendLine()

            w.appendLine("== logcat (this app) ==")
            w.appendLine(logcat())
        }
        return out
    }

    fun shareIntent(context: Context, report: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.diagnostics", report)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, report.name)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, "Share diagnostics")
    }

    private fun exitReasons(context: Context): List<String> {
        if (Build.VERSION.SDK_INT < 30) return listOf("(needs Android 11+)")
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return runCatching {
            am.getHistoricalProcessExitReasons(null, 0, 8).map { e ->
                "${fmt.format(Date(e.timestamp))}  ${e.processName}  ${reasonName(e.reason)}" +
                    " status=${e.status}  ${e.description.orEmpty()}" +
                    "  pss=${e.pss / 1024}MB rss=${e.rss / 1024}MB"
            }.ifEmpty { listOf("(none recorded)") }
        }.getOrElse { listOf("(unavailable: ${it.message})") }
    }

    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "CRASH (Java)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH (native)"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
        ApplicationExitInfo.REASON_OTHER -> "OTHER"
        else -> "reason $reason"
    }

    private fun tail(file: File, maxBytes: Int): String {
        val len = file.length()
        file.inputStream().use { input ->
            if (len > maxBytes) input.skip(len - maxBytes)
            return input.readBytes().toString(Charsets.UTF_8)
        }
    }

    /** Apps may read their own log lines; includes the game process (same app). */
    private fun logcat(): String = runCatching {
        val p = ProcessBuilder("logcat", "-d", "-v", "threadtime", "-t", "3000")
            .redirectErrorStream(true).start()
        val text = p.inputStream.bufferedReader().readText()
        p.waitFor(5, TimeUnit.SECONDS)
        text
    }.getOrElse { "(logcat unavailable: ${it.message})" }
}
