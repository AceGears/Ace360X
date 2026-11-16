package com.acegears.ace360x

import com.acegears.ace360x.core.GameFormat
import com.acegears.ace360x.device.DeviceChecks
import com.acegears.ace360x.library.ScanPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PureLogicTest {
    @Test fun gameFormatCodesMatchNativeEnum() {
        // Must mirror native/host/format_sniffer.h
        assertEquals(GameFormat.UNKNOWN, GameFormat.fromCode(0))
        assertEquals(GameFormat.XEX2, GameFormat.fromCode(1))
        assertEquals(GameFormat.DISC_IMAGE, GameFormat.fromCode(2))
        assertEquals(GameFormat.STFS_CON, GameFormat.fromCode(3))
        assertEquals(GameFormat.STFS_LIVE, GameFormat.fromCode(4))
        assertEquals(GameFormat.STFS_PIRS, GameFormat.fromCode(5))
        assertEquals(GameFormat.UNKNOWN, GameFormat.fromCode(99))
    }

    @Test fun scanCandidates() {
        assertTrue(ScanPolicy.isCandidate("Game.ISO", 1_000_000))
        assertTrue(ScanPolicy.isCandidate("default.xex", 10))
        assertTrue(ScanPolicy.isCandidate("4D5307E6", 0xB000))
        assertFalse(ScanPolicy.isCandidate("4D5307E6", 100))
        assertFalse(ScanPolicy.isCandidate("cover.jpg", 1_000_000))
        assertFalse(ScanPolicy.isCandidate("game.iso", 0))
        assertFalse(ScanPolicy.isCandidate("game.iso", -1))
    }

    @Test fun sizeFormatting() {
        assertEquals("512 B", ScanPolicy.formatSize(512))
        assertEquals("1.0 KB", ScanPolicy.formatSize(1024))
        assertEquals("7.3 GB", ScanPolicy.formatSize(7_838_695_424))
    }

    @Test fun deviceChecks() {
        val good = DeviceChecks.evaluate(12_000_000_000, "1.3.128", listOf("arm64-v8a"))
        assertTrue(good.all { it.passed == true })

        val weak = DeviceChecks.evaluate(4_000_000_000, "1.0.61", listOf("armeabi-v7a"))
        assertTrue(weak.all { it.passed == false })

        val unknownVk = DeviceChecks.evaluate(12_000_000_000, null, listOf("arm64-v8a"))
        assertNull(unknownVk[1].passed)
    }

    @Test fun versionParsing() {
        assertEquals(1 to 3, DeviceChecks.parseVersion("1.3.128"))
        assertNull(DeviceChecks.parseVersion("garbage"))
        assertNull(DeviceChecks.parseVersion(""))
    }
}
