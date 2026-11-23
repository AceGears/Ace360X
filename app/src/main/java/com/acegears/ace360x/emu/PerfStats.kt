package com.acegears.ace360x.emu

import kotlin.math.sqrt

/**
 * Measured performance of the running game (CLAUDE.md §7). Pure Kotlin so the maths is
 * unit-tested on the JVM. "Frames" are frames the game itself finished (guest swaps),
 * never interpolated or repeated frames.
 */
data class PerfSummary(
    /** Frames the game finished in the window. */
    val frames: Int,
    /** Average frames per second over the window; null without frames. */
    val avgFps: Double?,
    /** FPS equivalent of the slowest 1 % of frames (at least the single slowest frame). */
    val low1Fps: Double?,
    /** Standard deviation of frame time, in milliseconds. */
    val frameTimeStdevMs: Double?,
    /** Average and longest wait between a touch/button change and the game reading it, ms. */
    val inputAvgMs: Double?,
    val inputMaxMs: Double?,
    /** How often the game read the controller, per second of the window. */
    val pollsPerSecond: Double?,
) {
    /** One short line for the on-screen readout. */
    fun line(): String {
        if (avgFps == null) return "No frames from the game yet"
        val fps = "%.1f FPS (1%% low %.1f, ±%.1f ms)".format(avgFps, low1Fps, frameTimeStdevMs)
        val input = if (inputAvgMs != null) {
            " · input→game %.0f ms (max %.0f)".format(inputAvgMs, inputMaxMs)
        } else {
            ""
        }
        return fps + input
    }
}

object PerfStats {
    private const val HEADER = 4

    /** Keeps the most recent [WINDOW] frame intervals so the 1 % low has enough samples. */
    const val WINDOW = 600

    /**
     * @param raw a sample from NativeBridge.drainPerf()
     * @param history frame intervals (µs) kept from earlier samples; updated in place
     */
    fun summarize(raw: LongArray, history: ArrayDeque<Long>): PerfSummary {
        if (raw.size < HEADER) return PerfSummary(0, null, null, null, null, null, null)
        for (i in HEADER until raw.size) if (raw[i] > 0) history.addLast(raw[i])
        while (history.size > WINDOW) history.removeFirst()

        val delivered = raw[0]
        val inputAvg = if (delivered > 0) raw[1].toDouble() / delivered / 1000.0 else null
        val inputMax = if (delivered > 0) raw[2] / 1000.0 else null

        if (history.isEmpty()) return PerfSummary(0, null, null, null, inputAvg, inputMax, null)
        val totalUs = history.sum().toDouble()
        val meanUs = totalUs / history.size
        val variance = history.sumOf { (it - meanUs) * (it - meanUs) } / history.size
        val slowestCount = maxOf(1, history.size / 100)
        val slowestMeanUs = history.sortedDescending().take(slowestCount).average()
        val newFrames = raw.size - HEADER
        val newUs = (HEADER until raw.size).sumOf { raw[it] }.toDouble()
        return PerfSummary(
            frames = history.size,
            avgFps = 1_000_000.0 / meanUs,
            low1Fps = 1_000_000.0 / slowestMeanUs,
            frameTimeStdevMs = sqrt(variance) / 1000.0,
            inputAvgMs = inputAvg,
            inputMaxMs = inputMax,
            pollsPerSecond = if (newFrames > 0 && newUs > 0) raw[3] * 1_000_000.0 / newUs else null,
        )
    }
}
