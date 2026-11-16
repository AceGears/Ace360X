package com.acegears.ace360x.core

/** Mirrors ace360x::host::GameFormat in native/host/format_sniffer.h. Keep in sync. */
enum class GameFormat(val code: Int, val label: String) {
    UNKNOWN(0, "Unrecognised"),
    XEX2(1, "XEX executable"),
    DISC_IMAGE(2, "Disc image (ISO)"),
    STFS_CON(3, "STFS package (CON)"),
    STFS_LIVE(4, "STFS package (LIVE)"),
    STFS_PIRS(5, "STFS package (PIRS)");

    val isRecognised: Boolean get() = this != UNKNOWN

    /**
     * Formats the launch path supports from a single Storage Access Framework
     * document (native/patches/0005). A loose XEX needs its whole folder, which
     * isn't supported yet.
     */
    val isLaunchable: Boolean get() = this == DISC_IMAGE || this == STFS_CON || this == STFS_LIVE || this == STFS_PIRS

    companion object {
        fun fromCode(code: Int): GameFormat = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}
