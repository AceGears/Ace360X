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

/**
 * Running totals for one play session, written to the diagnostics report so a session's
 * numbers don't depend on screenshots (CLAUDE.md §7, §11). Pure Kotlin; fed once a second.
 */
class PerfSession(private val title: String) {
    private var frames = 0L
    private var frameMicros = 0L
    private var hitches = 0
    private var worstFrameMicros = 0L
    private var inputChanges = 0L
    private var inputMicros = 0L
    private var inputMaxMicros = 0L
    private var seconds = 0
    private var hottestStatus = 0
    private var secondsWarm = 0
    private var firstWarmAtSecond = -1
    private var headroomMax = Float.NaN

    /**
     * @param raw a sample from NativeBridge.drainPerf()
     * @param thermalStatus PowerManager.THERMAL_STATUS_* at this moment
     * @param headroom PowerManager.getThermalHeadroom (1.0 = throttling starts), or NaN if unknown
     */
    fun add(raw: LongArray, thermalStatus: Int, headroom: Float) {
        seconds++
        if (raw.size >= HEADER) {
            inputChanges += raw[0]
            inputMicros += raw[1]
            inputMaxMicros = maxOf(inputMaxMicros, raw[2])
            for (i in HEADER until raw.size) {
                val us = raw[i]
                if (us <= 0) continue
                frames++
                frameMicros += us
                if (us >= HITCH_MICROS) hitches++
                worstFrameMicros = maxOf(worstFrameMicros, us)
            }
        }
        hottestStatus = maxOf(hottestStatus, thermalStatus)
        if (thermalStatus >= ThermalMessages.PowerManager_THERMAL_STATUS_MODERATE) {
            secondsWarm++
            if (firstWarmAtSecond < 0) firstWarmAtSecond = seconds
        }
        if (!headroom.isNaN() && (headroomMax.isNaN() || headroom > headroomMax)) headroomMax = headroom
    }

    val averageFps: Double? get() = if (frameMicros > 0) frames * 1_000_000.0 / frameMicros else null

    /** A few plain lines for the diagnostics report. */
    fun report(): String = buildString {
        appendLine("Game: $title")
        appendLine("Measured for: ${seconds / 60} min ${seconds % 60} s")
        val fps = averageFps
        if (fps == null) {
            appendLine("Frames: none finished by the game")
        } else {
            appendLine("Frames: $frames, average %.1f FPS".format(fps))
            appendLine("Stalls of ${HITCH_MICROS / 1000} ms or longer: $hitches (longest frame ${worstFrameMicros / 1000} ms)")
        }
        if (inputChanges > 0) {
            appendLine(
                "Input reaching the game: average %.0f ms, longest %.0f ms over $inputChanges changes"
                    .format(inputMicros / 1000.0 / inputChanges, inputMaxMicros / 1000.0),
            )
        } else {
            appendLine("Input reaching the game: no input was read")
        }
        appendLine(
            "Heat: highest Android thermal status $hottestStatus (0 none … 6 shutdown); warm or hotter for " +
                "$secondsWarm s" + if (firstWarmAtSecond >= 0) ", first after $firstWarmAtSecond s" else "",
        )
        if (!headroomMax.isNaN()) appendLine("Highest thermal headroom reading: %.2f (1.00 = throttling starts)".format(headroomMax))
    }

    companion object {
        private const val HEADER = 4

        /** A frame this long is felt as a stall. */
        const val HITCH_MICROS = 100_000L
    }
}
