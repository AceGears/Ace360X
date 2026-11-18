package com.acegears.ace360x.library

import android.content.Context
import java.io.File
import java.security.MessageDigest

/** Last-played time and total playtime of one game. */
data class PlayStat(val lastPlayedMillis: Long = 0, val totalSeconds: Long = 0)

/**
 * File-based play statistics, one small file per game (keyed by a hash of the
 * game URI). Files rather than DataStore because the game process writes them
 * and the library process reads them.
 */
object PlayStats {
    private fun dir(context: Context) = File(context.filesDir, "playstats").apply { mkdirs() }

    fun keyFor(uri: String): String =
        MessageDigest.getInstance("SHA-1").digest(uri.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun file(context: Context, uri: String) = File(dir(context), keyFor(uri))

    fun read(context: Context, uri: String): PlayStat = parse(runCatching { file(context, uri).readText() }.getOrNull())

    fun markPlayed(context: Context, uri: String, nowMillis: Long = System.currentTimeMillis()) {
        val f = file(context, uri)
        val old = parse(runCatching { f.readText() }.getOrNull())
        write(f, old.copy(lastPlayedMillis = nowMillis))
    }

    fun addPlaytime(context: Context, uri: String, seconds: Long) {
        if (seconds <= 0) return
        val f = file(context, uri)
        val old = parse(runCatching { f.readText() }.getOrNull())
        write(f, old.copy(totalSeconds = old.totalSeconds + seconds))
    }

    /** Format: "<lastPlayedMillis> <totalSeconds>". Anything malformed reads as empty. */
    fun parse(text: String?): PlayStat {
        val parts = text?.trim()?.split(' ') ?: return PlayStat()
        if (parts.size != 2) return PlayStat()
        val last = parts[0].toLongOrNull() ?: return PlayStat()
        val total = parts[1].toLongOrNull() ?: return PlayStat()
        return PlayStat(last.coerceAtLeast(0), total.coerceAtLeast(0))
    }

    fun serialize(stat: PlayStat): String = "${stat.lastPlayedMillis} ${stat.totalSeconds}"

    private fun write(f: File, stat: PlayStat) {
        // Write-then-rename so a crash never leaves a half-written file.
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(serialize(stat))
        tmp.renameTo(f)
    }

    fun formatPlaytime(seconds: Long): String = when {
        seconds < 60 -> "under a minute"
        seconds < 3600 -> "${seconds / 60} min"
        else -> "%d h %02d min".format(seconds / 3600, (seconds % 3600) / 60)
    }
}
