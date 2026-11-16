package com.acegears.ace360x.device

/**
 * Pure logic for the Device screen, unit-tested on the JVM.
 *
 * These checks compare the phone against the PROVISIONAL minimum in
 * docs/FEASIBILITY.md §5. Passing them says nothing about how fast any game
 * will run; that can only be measured.
 */
object DeviceChecks {
    const val MIN_RAM_BYTES = 7_500_000_000L // "8 GB" phones report a little less
    const val MIN_VULKAN_MAJOR = 1
    const val MIN_VULKAN_MINOR = 1

    data class Check(val label: String, val passed: Boolean?, val detail: String)

    fun evaluate(ramBytes: Long, vulkanApi: String?, supportedAbis: List<String>): List<Check> {
        val arm64 = "arm64-v8a" in supportedAbis
        val vk = parseVersion(vulkanApi)
        val vkOk = vk?.let { (maj, min) ->
            maj > MIN_VULKAN_MAJOR || (maj == MIN_VULKAN_MAJOR && min >= MIN_VULKAN_MINOR)
        }
        return listOf(
            Check("64-bit ARM CPU", arm64, supportedAbis.joinToString()),
            Check(
                "Vulkan 1.1 or newer",
                vkOk,
                vulkanApi ?: "Not reported",
            ),
            Check(
                "8 GB RAM or more",
                ramBytes >= MIN_RAM_BYTES,
                "%.1f GB reported".format(java.util.Locale.US, ramBytes / 1e9),
            ),
        )
    }

    /** "1.3.128" -> (1, 3); null if unparseable. */
    fun parseVersion(v: String?): Pair<Int, Int>? {
        val parts = v?.split('.') ?: return null
        if (parts.size < 2) return null
        val major = parts[0].toIntOrNull() ?: return null
        val minor = parts[1].toIntOrNull() ?: return null
        return major to minor
    }

    /** Names for PowerManager.THERMAL_STATUS_* values (0..6). */
    fun thermalStatusName(status: Int): String = when (status) {
        0 -> "None"
        1 -> "Light"
        2 -> "Moderate"
        3 -> "Severe"
        4 -> "Critical"
        5 -> "Emergency"
        6 -> "Shutdown"
        else -> "Unknown ($status)"
    }

    /** Vendor IDs from the Khronos registry / PCI-SIG. */
    fun vendorName(id: Long): String = when (id) {
        0x5143L -> "Qualcomm"
        0x13B5L -> "ARM"
        0x1010L -> "Imagination"
        0x144DL -> "Samsung"
        0x10DEL -> "NVIDIA"
        0x1002L -> "AMD"
        0x8086L -> "Intel"
        0x10005L -> "Mesa"
        else -> "0x%X".format(id)
    }
}
