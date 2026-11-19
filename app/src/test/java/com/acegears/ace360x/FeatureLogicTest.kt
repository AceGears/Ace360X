package com.acegears.ace360x

import com.acegears.ace360x.emu.GamepadMapper
import com.acegears.ace360x.emu.ThermalMessages
import com.acegears.ace360x.library.LibraryFilter
import com.acegears.ace360x.library.PlayStat
import com.acegears.ace360x.library.PlayStats
import com.acegears.ace360x.library.SortMode
import com.acegears.ace360x.settings.EmulatorSettings
import com.acegears.ace360x.settings.EmulatorSettings.AntiAliasing
import com.acegears.ace360x.settings.EmulatorSettings.GraphicsPreset
import com.acegears.ace360x.settings.EmulatorSettings.Sharpening
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FeatureLogicTest {
    @Test fun defaultSettingsAreOriginalPresetAndSafeCvars() {
        val s = EmulatorSettings()
        assertEquals(GraphicsPreset.ORIGINAL, s.graphicsPreset)
        val c = s.toCvars()
        assertEquals(1, c["draw_resolution_scale_x"])
        assertEquals(1, c["draw_resolution_scale_y"])
        assertEquals("", c["postprocess_antialiasing"])
        assertEquals("", c["postprocess_scaling_and_sharpening"])
        assertEquals(true, c["present_letterbox"])
        assertEquals(true, c["vsync"])
        assertEquals(false, c["mute"])
    }

    @Test fun cvarTypesMatchXenia() {
        // int32 cvars must be Int, bool → Boolean, string → String (cvar_android.cc getters).
        val c = EmulatorSettings(resolutionScale = 2).toCvars()
        assert(c["draw_resolution_scale_x"] is Int)
        assert(c["present_letterbox"] is Boolean)
        assert(c["postprocess_antialiasing"] is String)
    }

    @Test fun presetsRoundTrip() {
        val base = EmulatorSettings(mute = true)
        GraphicsPreset.entries.filter { it != GraphicsPreset.CUSTOM }.forEach { p ->
            assertEquals(p, base.withPreset(p).graphicsPreset)
        }
        val custom = EmulatorSettings(resolutionScale = 3, antiAliasing = AntiAliasing.FXAA_EXTREME, sharpening = Sharpening.FSR)
        assertEquals(GraphicsPreset.CUSTOM, custom.graphicsPreset)
        assertEquals("fsr", custom.toCvars()["postprocess_scaling_and_sharpening"])
    }

    @Test fun resolutionScaleIsClamped() {
        assertEquals(3, EmulatorSettings(resolutionScale = 9).toCvars()["draw_resolution_scale_x"])
        assertEquals(1, EmulatorSettings(resolutionScale = 0).toCvars()["draw_resolution_scale_y"])
    }

    private data class G(val name: String, val size: Long)

    private fun filter(
        games: List<G>, q: String = "", favOnly: Boolean = false, fav: Set<String> = emptySet(),
        sort: SortMode = SortMode.NAME, stats: Map<String, PlayStat> = emptyMap(),
    ) = LibraryFilter.apply(games, q, favOnly, fav, sort, { it.name }, { it.name }, { it.size }, { stats[it.name] ?: PlayStat() })
        .map { it.name }

    @Test fun libraryFilterAndSort() {
        val games = listOf(G("halo 3.iso", 7), G("Banjo", 1), G("Alan Wake", 5))
        assertEquals(listOf("Alan Wake", "Banjo", "halo 3.iso"), filter(games))
        assertEquals(listOf("halo 3.iso"), filter(games, q = "HALO"))
        assertEquals(listOf("Banjo"), filter(games, favOnly = true, fav = setOf("Banjo")))
        assertEquals(listOf("halo 3.iso", "Alan Wake", "Banjo"), filter(games, sort = SortMode.SIZE))
        val stats = mapOf("Banjo" to PlayStat(lastPlayedMillis = 100, totalSeconds = 10), "Alan Wake" to PlayStat(50, 999))
        assertEquals(listOf("Banjo", "Alan Wake", "halo 3.iso"), filter(games, sort = SortMode.RECENT, stats = stats))
        assertEquals(listOf("Alan Wake", "Banjo", "halo 3.iso"), filter(games, sort = SortMode.PLAYTIME, stats = stats))
    }

    @Test fun playStatsParsing() {
        assertEquals(PlayStat(123, 45), PlayStats.parse(PlayStats.serialize(PlayStat(123, 45))))
        assertEquals(PlayStat(), PlayStats.parse(null))
        assertEquals(PlayStat(), PlayStats.parse("garbage"))
        assertEquals(PlayStat(), PlayStats.parse("1 2 3"))
        assertEquals(PlayStat(0, 0), PlayStats.parse("-5 -9"))
        assertEquals("1 h 05 min", PlayStats.formatPlaytime(3900))
        assertEquals(40, PlayStats.keyFor("content://x").length)
    }

    @Test fun thermalMessages() {
        assertNull(ThermalMessages.forStatus(0))
        assertNull(ThermalMessages.forStatus(1))
        assertNotNull(ThermalMessages.forStatus(2))
        assertNotNull(ThermalMessages.forStatus(6))
    }

    @Test fun bindingsRoundTripAndLookup() {
        val defaults = GamepadMapper.DEFAULT_BINDINGS
        assertEquals(15, defaults.size)
        assertEquals(defaults, GamepadMapper.decodeBindings(GamepadMapper.encodeBindings(defaults)))
        assertEquals(defaults, GamepadMapper.decodeBindings(null))
        assertEquals(defaults, GamepadMapper.decodeBindings("garbage;;1:x;99999:5"))   // ignored safely

        // Swap A and B.
        val swapped = defaults + (GamepadMapper.A to GamepadMapper.KEYCODE_BUTTON_B) + (GamepadMapper.B to GamepadMapper.KEYCODE_BUTTON_A)
        val lookup = GamepadMapper.lookupFor(GamepadMapper.decodeBindings(GamepadMapper.encodeBindings(swapped)))
        assertEquals(GamepadMapper.B, lookup[GamepadMapper.KEYCODE_BUTTON_A])
        assertEquals(GamepadMapper.A, lookup[GamepadMapper.KEYCODE_BUTTON_B])

        // Two buttons on one key press both.
        val shared = defaults + (GamepadMapper.Y to GamepadMapper.KEYCODE_BUTTON_A)
        assertEquals(GamepadMapper.A or GamepadMapper.Y, GamepadMapper.lookupFor(shared)[GamepadMapper.KEYCODE_BUTTON_A])
    }

    @Test fun defaultLookupMatchesLegacyMapping() {
        val lookup = GamepadMapper.lookupFor(GamepadMapper.DEFAULT_BINDINGS)
        listOf(96, 97, 99, 100, 102, 103, 106, 107, 108, 109, 110, 19, 20, 21, 22).forEach { key ->
            assertEquals(GamepadMapper.buttonForKey(key), lookup[key])
        }
    }
}

class GameOverridesTest {
    private val global = EmulatorSettings(resolutionScale = 1, antiAliasing = AntiAliasing.FXAA, mute = false, touchScale = 1.2f)

    @Test fun emptyOverridesChangeNothing() {
        val o = com.acegears.ace360x.settings.GameOverrides()
        assert(o.isEmpty)
        assertEquals(global, o.applyTo(global))
        assertEquals(o, com.acegears.ace360x.settings.GameOverrides.parse(o.serialize()))
    }

    @Test fun overridesReplaceOnlyTheirFields() {
        val o = com.acegears.ace360x.settings.GameOverrides(resolutionScale = 2, mute = true, guestVsync = false)
        val s = o.applyTo(global)
        assertEquals(2, s.resolutionScale)
        assertEquals(true, s.mute)
        assertEquals(false, s.guestVsync)
        assertEquals(AntiAliasing.FXAA, s.antiAliasing) // not overridden
        assertEquals(1.2f, s.touchScale) // controller/touch options stay global
        assertEquals(2, s.toCvars()["draw_resolution_scale_x"])
    }

    @Test fun serializationRoundTripsAndWritesOnlyTheDiff() {
        val o = com.acegears.ace360x.settings.GameOverrides(
            resolutionScale = 3, antiAliasing = AntiAliasing.OFF, sharpening = Sharpening.FSR,
            keepAspectRatio = false, mute = true, guestVsync = false,
        )
        assertEquals(o, com.acegears.ace360x.settings.GameOverrides.parse(o.serialize()))
        val one = com.acegears.ace360x.settings.GameOverrides(mute = true)
        assertEquals("v1\nmute=true\n", one.serialize())
    }

    @Test fun malformedFilesAreIgnored() {
        val p = com.acegears.ace360x.settings.GameOverrides.Companion::parse
        val empty = com.acegears.ace360x.settings.GameOverrides()
        assertEquals(empty, p(null))
        assertEquals(empty, p("garbage"))
        assertEquals(empty, p("v2\nmute=true\n"))
        assertEquals(empty, p("v1\n" + "x".repeat(2000)))
        // Out-of-range and unknown values fall back to the global value, valid keys still apply.
        val mixed = p("v1\nscale=99\naa=NOPE\nmute=true\nunknown=1\n=x\n")
        assertEquals(com.acegears.ace360x.settings.GameOverrides(mute = true), mixed)
    }
}
