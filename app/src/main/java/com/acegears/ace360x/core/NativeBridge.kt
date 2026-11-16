package com.acegears.ace360x.core

import android.util.Log

/**
 * The only Kotlin entry point into native code (CLAUDE.md §3).
 * Loading is attempted once; if it fails, every call reports the failure
 * instead of crashing the app.
 */
object NativeBridge {
    private const val TAG = "NativeBridge"

    val loadError: String? = try {
        System.loadLibrary("ace360x")
        null
    } catch (e: UnsatisfiedLinkError) {
        Log.e(TAG, "Failed to load native library", e)
        e.message ?: "UnsatisfiedLinkError"
    }

    val isLoaded: Boolean get() = loadError == null

    /** JSON describing the Vulkan device, or {"error": ...}. */
    fun probeVulkanJson(): String =
        if (isLoaded) nativeProbeVulkan()
        else """{"error":"Native library not loaded: ${loadError.orEmpty().replace("\"", "'")}"}"""

    /** Identifies the format of an open file descriptor (not owned/closed here). */
    fun sniffFormat(fd: Int): GameFormat? {
        if (!isLoaded) return null
        val code = nativeSniffFormat(fd)
        return if (code < 0) null else GameFormat.fromCode(code)
    }

    private external fun nativeProbeVulkan(): String
    private external fun nativeSniffFormat(fd: Int): Int
}
