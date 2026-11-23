package com.acegears.ace360x.settings

import android.content.Context
import com.acegears.ace360x.library.PlayStats
import com.acegears.ace360x.settings.EmulatorSettings.AntiAliasing
import com.acegears.ace360x.settings.EmulatorSettings.Sharpening
import java.io.File

/**
 * Per-game settings, stored as a diff over the global [EmulatorSettings]: null
 * means "use the global value". Only options that make sense per game are here;
 * controller and touch options stay global. Pure Kotlin, unit-tested.
 */
data class GameOverrides(
    val resolutionScale: Int? = null,
    val antiAliasing: AntiAliasing? = null,
    val sharpening: Sharpening? = null,
    val keepAspectRatio: Boolean? = null,
    val mute: Boolean? = null,
    val guestVsync: Boolean? = null,
    /** Experimental; see [EmulatorSettings.allowUnmappedGpuReads]. Global value is always off. */
    val allowUnmappedGpuReads: Boolean? = null,
) {
    val isEmpty: Boolean get() = this == GameOverrides()

    fun applyTo(global: EmulatorSettings): EmulatorSettings = global.copy(
        resolutionScale = (resolutionScale ?: global.resolutionScale).coerceIn(1, EmulatorSettings.MAX_RESOLUTION_SCALE),
        antiAliasing = antiAliasing ?: global.antiAliasing,
        sharpening = sharpening ?: global.sharpening,
        keepAspectRatio = keepAspectRatio ?: global.keepAspectRatio,
        mute = mute ?: global.mute,
        guestVsync = guestVsync ?: global.guestVsync,
        allowUnmappedGpuReads = allowUnmappedGpuReads ?: false,
    )

    /** One "key=value" per line after a version line; only overridden keys are written. */
    fun serialize(): String = buildString {
        append("v").append(SCHEMA_VERSION).append('\n')
        resolutionScale?.let { append("scale=").append(it).append('\n') }
        antiAliasing?.let { append("aa=").append(it.name).append('\n') }
        sharpening?.let { append("sharpen=").append(it.name).append('\n') }
        keepAspectRatio?.let { append("aspect=").append(it).append('\n') }
        mute?.let { append("mute=").append(it).append('\n') }
        guestVsync?.let { append("vsync=").append(it).append('\n') }
        allowUnmappedGpuReads?.let { append("gpuunmapped=").append(it).append('\n') }
    }

    companion object {
        const val SCHEMA_VERSION = 1
        private const val MAX_FILE_CHARS = 512

        /** Unknown keys, bad values and other schema versions are ignored (global value used). */
        fun parse(text: String?): GameOverrides {
            if (text == null || text.length > MAX_FILE_CHARS) return GameOverrides()
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.firstOrNull() != "v$SCHEMA_VERSION") return GameOverrides()
            val kv = lines.drop(1).mapNotNull { l ->
                val i = l.indexOf('=')
                if (i <= 0) null else l.substring(0, i) to l.substring(i + 1)
            }.toMap()
            return GameOverrides(
                resolutionScale = kv["scale"]?.toIntOrNull()?.takeIf { it in 1..EmulatorSettings.MAX_RESOLUTION_SCALE },
                antiAliasing = kv["aa"]?.let { n -> AntiAliasing.entries.firstOrNull { it.name == n } },
                sharpening = kv["sharpen"]?.let { n -> Sharpening.entries.firstOrNull { it.name == n } },
                keepAspectRatio = kv["aspect"]?.toBooleanStrictOrNull(),
                mute = kv["mute"]?.toBooleanStrictOrNull(),
                guestVsync = kv["vsync"]?.toBooleanStrictOrNull(),
                allowUnmappedGpuReads = kv["gpuunmapped"]?.toBooleanStrictOrNull(),
            )
        }
    }
}

/**
 * One small file per game with overrides (keyed like [PlayStats]); no file = no
 * overrides. Read and written only by the main process.
 */
object GameOverridesStore {
    private fun file(context: Context, gameKey: String) =
        File(File(context.filesDir, "gamesettings").apply { mkdirs() }, PlayStats.keyFor(gameKey))

    fun read(context: Context, gameKey: String): GameOverrides =
        GameOverrides.parse(runCatching { file(context, gameKey).readText() }.getOrNull())

    fun write(context: Context, gameKey: String, overrides: GameOverrides) {
        val f = file(context, gameKey)
        if (overrides.isEmpty) {
            f.delete()
            return
        }
        // Write-then-rename so a crash never leaves a half-written file.
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(overrides.serialize())
        tmp.renameTo(f)
    }
}
