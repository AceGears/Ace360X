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

class PerfStatsTest {
    private fun raw(delivered: Long, sumUs: Long, maxUs: Long, polls: Long, vararg intervals: Long) =
        longArrayOf(delivered, sumUs, maxUs, polls, *intervals)

    @org.junit.Test
    fun steadyThirtyFps() {
        val s = com.acegears.ace360x.emu.PerfStats.summarize(
            raw(2, 20_000, 15_000, 60, *LongArray(30) { 33_333L }), ArrayDeque(),
        )
        org.junit.Assert.assertEquals(30.0, s.avgFps!!, 0.01)
        org.junit.Assert.assertEquals(30.0, s.low1Fps!!, 0.01)
        org.junit.Assert.assertEquals(0.0, s.frameTimeStdevMs!!, 0.001)
        org.junit.Assert.assertEquals(10.0, s.inputAvgMs!!, 0.001)
        org.junit.Assert.assertEquals(15.0, s.inputMaxMs!!, 0.001)
        org.junit.Assert.assertEquals(60.0, s.pollsPerSecond!!, 0.1)
    }

    @org.junit.Test
    fun oneSlowFrameShowsInTheLowAndStdev() {
        val intervals = LongArray(100) { 20_000L }.also { it[50] = 100_000L }
        val s = com.acegears.ace360x.emu.PerfStats.summarize(raw(0, 0, 0, 0, *intervals), ArrayDeque())
        org.junit.Assert.assertEquals(1_000_000.0 / 20_800.0, s.avgFps!!, 0.01)
        org.junit.Assert.assertEquals(10.0, s.low1Fps!!, 0.01)
        org.junit.Assert.assertTrue(s.frameTimeStdevMs!! > 7.0)
        org.junit.Assert.assertNull(s.inputAvgMs)
    }

    @org.junit.Test
    fun noFramesYetAndHistoryIsBounded() {
        val history = ArrayDeque<Long>()
        val empty = com.acegears.ace360x.emu.PerfStats.summarize(raw(0, 0, 0, 0), history)
        org.junit.Assert.assertNull(empty.avgFps)
        org.junit.Assert.assertEquals("No frames from the game yet", empty.line())
        repeat(5) {
            com.acegears.ace360x.emu.PerfStats.summarize(raw(0, 0, 0, 0, *LongArray(256) { 16_667L }), history)
        }
        org.junit.Assert.assertEquals(com.acegears.ace360x.emu.PerfStats.WINDOW, history.size)
        org.junit.Assert.assertNull(com.acegears.ace360x.emu.PerfStats.summarize(longArrayOf(1), ArrayDeque()).avgFps)
    }
}

class UnmappedGpuReadsTest {
    @org.junit.Test
    fun offByDefaultAndOnlyPerGame() {
        val global = EmulatorSettings()
        org.junit.Assert.assertEquals(false, global.toCvars()["gpu_allow_invalid_upload_range"])
        val on = com.acegears.ace360x.settings.GameOverrides(allowUnmappedGpuReads = true)
        org.junit.Assert.assertEquals(true, on.applyTo(global).toCvars()["gpu_allow_invalid_upload_range"])
        // A global value can't leak in: without the override it is always off.
        org.junit.Assert.assertEquals(
            false,
            com.acegears.ace360x.settings.GameOverrides().applyTo(global.copy(allowUnmappedGpuReads = true))
                .allowUnmappedGpuReads,
        )
        val parsed = com.acegears.ace360x.settings.GameOverrides.parse(on.serialize())
        org.junit.Assert.assertEquals(on, parsed)
    }
}

class HomeTabsTest {
    private data class G(val name: String, val last: Long)
    private val games = listOf(G("Halo 3", 30), G("Fable II", 0), G("Forza Horizon", 90), G("Gears of War", 10))
    private fun on(tab: com.acegears.ace360x.library.HomeTab, query: String = "", fav: Set<String> = emptySet()) =
        com.acegears.ace360x.library.LibraryFilter.forTab(
            games, tab, query, fav, com.acegears.ace360x.library.SortMode.NAME,
            name = { it.name }, key = { it.name }, size = { 0L },
            stat = { com.acegears.ace360x.library.PlayStat(lastPlayedMillis = it.last) },
        ).map { it.name }

    @org.junit.Test
    fun tabsFilterAndRecentIsNewestFirst() {
        org.junit.Assert.assertEquals(listOf("Fable II", "Forza Horizon", "Gears of War", "Halo 3"), on(com.acegears.ace360x.library.HomeTab.ALL))
        org.junit.Assert.assertEquals(listOf("Halo 3"), on(com.acegears.ace360x.library.HomeTab.FAVOURITES, fav = setOf("Halo 3")))
        // Only played games, most recent first, whatever the chosen sort.
        org.junit.Assert.assertEquals(listOf("Forza Horizon", "Halo 3", "Gears of War"), on(com.acegears.ace360x.library.HomeTab.RECENT))
        org.junit.Assert.assertEquals(listOf("Forza Horizon"), on(com.acegears.ace360x.library.HomeTab.RECENT, query = "forza"))
    }

    @org.junit.Test
    fun continuePlayingIsTheLastPlayedGameOrNothing() {
        val stat = { g: G -> com.acegears.ace360x.library.PlayStat(lastPlayedMillis = g.last) }
        org.junit.Assert.assertEquals("Forza Horizon", com.acegears.ace360x.library.LibraryFilter.continuePlaying(games, stat)?.name)
        org.junit.Assert.assertNull(com.acegears.ace360x.library.LibraryFilter.continuePlaying(games.filter { it.last == 0L }, stat))
    }

    @org.junit.Test
    fun coverInitials() {
        org.junit.Assert.assertEquals("H3", com.acegears.ace360x.library.coverInitials("Halo 3"))
        org.junit.Assert.assertEquals("NR", com.acegears.ace360x.library.coverInitials("Naruto: Rise of a Ninja"))
        org.junit.Assert.assertEquals("F", com.acegears.ace360x.library.coverInitials("fable"))
        org.junit.Assert.assertEquals("?", com.acegears.ace360x.library.coverInitials(" -- "))
        // Filler words are skipped, unless they are all there is.
        org.junit.Assert.assertEquals("NB", com.acegears.ace360x.library.coverInitials("Naruto - The Broken Bond (Europe)"))
        org.junit.Assert.assertEquals("T", com.acegears.ace360x.library.coverInitials("The"))
    }

    @org.junit.Test
    fun gameTitleDropsOnlyGameFileEndings() {
        org.junit.Assert.assertEquals("Naruto - The Broken Bond (Europe)", com.acegears.ace360x.library.gameTitle("Naruto - The Broken Bond (Europe).iso"))
        org.junit.Assert.assertEquals("default", com.acegears.ace360x.library.gameTitle("default.XEX"))
        org.junit.Assert.assertEquals("Halo 3", com.acegears.ace360x.library.gameTitle("Halo 3"))
        org.junit.Assert.assertEquals("Vol. 2", com.acegears.ace360x.library.gameTitle("Vol. 2"))
        org.junit.Assert.assertEquals(".iso", com.acegears.ace360x.library.gameTitle(".iso"))
    }
}

class PerfSessionTest {
    @org.junit.Test
    fun totalsStallsInputAndHeat() {
        val s = com.acegears.ace360x.emu.PerfSession("Test game")
        // Second 1: 29 frames at 30 FPS and one 500 ms stall; two inputs, 10 ms and 30 ms.
        s.add(longArrayOf(2, 40_000, 30_000, 60, *LongArray(29) { 33_333L }, 500_000L), thermalStatus = 0, headroom = Float.NaN)
        // Seconds 2-3: steady, phone warm from second 2, headroom known.
        repeat(2) { s.add(longArrayOf(0, 0, 0, 30, *LongArray(30) { 33_333L }), thermalStatus = 2, headroom = 0.93f) }
        val frames = 90
        val micros = 29 * 33_333L + 500_000L + 60 * 33_333L
        org.junit.Assert.assertEquals(frames * 1_000_000.0 / micros, s.averageFps!!, 0.001)
        val r = s.report()
        org.junit.Assert.assertTrue(r, r.contains("Game: Test game"))
        org.junit.Assert.assertTrue(r, r.contains("Measured for: 0 min 3 s"))
        org.junit.Assert.assertTrue(r, r.contains("Stalls of 100 ms or longer: 1 (longest frame 500 ms)"))
        org.junit.Assert.assertTrue(r, r.contains("average 20 ms, longest 30 ms over 2 changes"))
        org.junit.Assert.assertTrue(r, r.contains("highest Android thermal status 2"))
        org.junit.Assert.assertTrue(r, r.contains("warm or hotter for 2 s, first after 2 s"))
        org.junit.Assert.assertTrue(r, r.contains("0.93"))
    }

    @org.junit.Test
    fun emptySessionSaysSo() {
        val s = com.acegears.ace360x.emu.PerfSession("Nothing")
        s.add(longArrayOf(0, 0, 0, 0), thermalStatus = 0, headroom = Float.NaN)
        org.junit.Assert.assertNull(s.averageFps)
        val r = s.report()
        org.junit.Assert.assertTrue(r, r.contains("none finished by the game"))
        org.junit.Assert.assertTrue(r, r.contains("no input was read"))
        org.junit.Assert.assertFalse(r, r.contains("headroom"))
    }
}
