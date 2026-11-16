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

    /** True if this build links the Xenia Canary core. */
    val hasCore: Boolean by lazy { isLoaded && nativeHasCore() }

    @Volatile private var coreInitialised = false

    /** Xenia's Android init; must run on the main thread before any core use. */
    @Synchronized
    fun initCore(context: android.content.Context) {
        if (!hasCore || coreInitialised) return
        nativeInitCore(android.os.Build.VERSION.SDK_INT, context.applicationContext)
        coreInitialised = true
    }

    /** Runs the on-device core self-test (blocking, call off the main thread). JSON result. */
    fun runCoreSelfTestJson(): String = when {
        !hasCore -> """{"error":"This build does not include the emulation core"}"""
        !coreInitialised -> """{"error":"Core not initialised"}"""
        else -> nativeRunCoreSelfTest()
    }

    private external fun nativeHasCore(): Boolean
    private external fun nativeInitCore(apiLevel: Int, context: android.content.Context)
    private external fun nativeRunCoreSelfTest(): String
    private external fun nativeProbeVulkan(): String
    private external fun nativeSniffFormat(fd: Int): Int
}
