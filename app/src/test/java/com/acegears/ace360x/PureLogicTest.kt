package com.acegears.ace360x

import com.acegears.ace360x.core.GameFormat
import com.acegears.ace360x.device.DeviceChecks
import com.acegears.ace360x.emu.GamepadMapper
import com.acegears.ace360x.emu.TouchMath
import com.acegears.ace360x.emu.TouchPadState
import com.acegears.ace360x.emu.PadSnapshot
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

    @Test fun launchableFormats() {
        assertTrue(GameFormat.DISC_IMAGE.isLaunchable)
        assertTrue(GameFormat.STFS_LIVE.isLaunchable)
        assertTrue(GameFormat.XEX2.isLaunchable)   // via SAF folder (patch 0008)
        assertFalse(GameFormat.UNKNOWN.isLaunchable)
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

    @Test fun gamepadButtons() {
        assertEquals(GamepadMapper.A, GamepadMapper.buttonForKey(GamepadMapper.KEYCODE_BUTTON_A))
        assertEquals(GamepadMapper.BACK, GamepadMapper.buttonForKey(GamepadMapper.KEYCODE_BUTTON_SELECT))
        assertEquals(0, GamepadMapper.buttonForKey(GamepadMapper.KEYCODE_BUTTON_L2))
        assertTrue(GamepadMapper.isDigitalTrigger(GamepadMapper.KEYCODE_BUTTON_R2))
    }

    @Test fun gamepadSticks() {
        assertEquals(0 to 0, GamepadMapper.stick(0.05f, -0.05f))      // inside dead zone
        assertEquals(32767 to 0, GamepadMapper.stick(1f, 0f))         // full right
        assertEquals(0 to 32767, GamepadMapper.stick(0f, -1f))        // Android up = XInput +Y
        assertEquals(0 to -32768, GamepadMapper.stick(0f, 1f))
        val (x, _) = GamepadMapper.stick(0.2f, 0f)                    // just past dead zone: small
        assertTrue(x in 1..5000)
        assertEquals(0, GamepadMapper.trigger(-0.2f))
        assertEquals(255, GamepadMapper.trigger(1.5f))
    }

    @Test fun hatToDpad() {
        assertEquals(GamepadMapper.DPAD_UP or GamepadMapper.DPAD_LEFT, GamepadMapper.hatButtons(-1f, -1f))
        assertEquals(0, GamepadMapper.hatButtons(0f, 0f))
    }

    @Test fun touchStickClamp() {
        assertEquals(0.5f to 0f, TouchMath.stickVector(50f, 0f, 100f))
        val (x, y) = TouchMath.stickVector(300f, 400f, 100f)   // far outside: unit circle
        assertEquals(0.6f, x, 1e-4f)
        assertEquals(0.8f, y, 1e-4f)
        assertEquals(0f to 0f, TouchMath.stickVector(10f, 10f, 0f))
    }

    @Test fun touchPadStateEmitsSnapshots() {
        var last = PadSnapshot()
        val pad = TouchPadState { last = it }
        pad.setButton(GamepadMapper.A, true)
        assertEquals(GamepadMapper.A, last.buttons)
        pad.setTrigger(leftTrigger = false, down = true)
        assertEquals(255, last.rightTrigger)
        pad.setStick(leftStick = true, v = 0f to -1f)        // finger pushed up
        assertEquals(32767, last.thumbLY)
        pad.setButton(GamepadMapper.A, false)
        assertEquals(0, last.buttons)
        assertTrue(last.connected)
    }

    @Test fun displayNames() {
        assertEquals("Halo 3", ScanPolicy.displayNameFor("Halo 3/default.xex"))
        assertEquals("Games/Halo 3".substringAfterLast('/'), ScanPolicy.displayNameFor("Games/Halo 3/DEFAULT.XEX"))
        // Games on Demand: <game>/<TitleID>/00007000/<hash>
        assertEquals("Forza", ScanPolicy.displayNameFor("Forza/4D5309C9/00007000/0123456789ABCDEF0123"))
        assertEquals("Game.iso", ScanPolicy.displayNameFor("Isos/Game.iso"))
        assertEquals("default.xex", ScanPolicy.displayNameFor("default.xex"))  // no folder to name it after
    }
}
