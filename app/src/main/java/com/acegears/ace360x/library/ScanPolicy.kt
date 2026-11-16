package com.acegears.ace360x.library

/**
 * Pure rules for which files the library scanner inspects. Kept free of
 * Android types so it can be unit-tested on the JVM.
 */
object ScanPolicy {
    const val MAX_DEPTH = 8
    const val MAX_FILES_VISITED = 20_000

    /** STFS header plus at least one data block; smaller files can't be packages. */
    private const val MIN_STFS_SIZE = 0xB000L
    private val candidateExtensions = setOf("iso", "xex")

    /**
     * Files worth opening to read their header. STFS packages usually have no
     * extension, so extensionless files above the minimum size are included.
     */
    fun isCandidate(displayName: String, sizeBytes: Long): Boolean {
        if (sizeBytes <= 0) return false
        val dot = displayName.lastIndexOf('.')
        if (dot <= 0 || dot == displayName.length - 1) {
            return sizeBytes >= MIN_STFS_SIZE
        }
        return displayName.substring(dot + 1).lowercase() in candidateExtensions
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = listOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble() / 1024
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        return String.format(java.util.Locale.US, "%.1f %s", value, units[unit])
    }
}
