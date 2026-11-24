package com.acegears.ace360x

import com.acegears.ace360x.theme.BrandColors
import com.acegears.ace360x.theme.Contrast
import com.acegears.ace360x.theme.MotionTokens
import com.acegears.ace360x.theme.ResolvedTheme
import com.acegears.ace360x.theme.ThemeChecks
import com.acegears.ace360x.theme.ThemeCustomization
import com.acegears.ace360x.theme.ThemeMode
import com.acegears.ace360x.theme.ThemeVariant
import com.acegears.ace360x.theme.Themes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    /** CLAUDE.md §10: the same theme set as AceArcade. */
    @Test fun aceArcadeThemeSetWithUniqueIds() {
        val expected = listOf(
            "Aqua Glass", "Violet Pulse", "Jade Glass", "Sandstone", "Plasma",
            "AceArcade Steel", "AceArcade Steel Light", "AceArcade Neon (classic)", "Neon Purple",
            "Cyber Blue", "Minimal", "AMOLED Black", "Midnight Blue", "Crimson Red", "Royal Purple",
            "Emerald Green", "Gold Edition",
        )
        assertEquals(expected, Themes.PRESETS.map { it.name })
        assertEquals(expected + listOf("Dynamic Material You", "Custom"), Themes.ALL.map { it.name })
        assertEquals(Themes.ALL.size, Themes.ALL.map { it.id }.toSet().size)
        assertEquals("AceArcade Steel", Themes.DEFAULT.name)
        // The five glass themes have a backdrop and translucent cards in both modes; the rest are solid.
        Themes.ALL.forEach { t ->
            listOf(t.dark, t.light).forEach { v ->
                assertEquals(t.name, t.premium, v.backdrop != null)
                assertEquals(t.name, t.premium, v.glassAlpha < 1f)
            }
            assertTrue("${t.name} dark variant is not dark", t.dark.dark)
            assertFalse("${t.name} light variant is not light", t.light.dark)
        }
    }

    @Test fun contrastMathMatchesWcag() {
        assertEquals(21.0, Contrast.ratio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, Contrast.ratio(0xFF336699, 0xFF336699), 0.0001)
        // #767676 on white is the commonly cited lightest grey passing AA (≈ 4.54).
        assertEquals(4.54, Contrast.ratio(0xFF767676, 0xFFFFFFFF), 0.01)
        assertTrue(Contrast.ratio(0xFF777777, 0xFFFFFFFF) < Contrast.AA_NORMAL)
    }

    @Test fun ensureOnlyMovesColoursThatNeedIt() {
        val white = 0xFFFFFFFFL
        val black = 0xFF000000L
        // Already readable: unchanged.
        assertEquals(0xFF0E7490L, Contrast.ensure(0xFF0E7490, listOf(white), Contrast.AA_NORMAL, toward = black))
        // Too pale on white: darkened until it passes, and no further than needed.
        val fixed = Contrast.ensure(0xFFE8B931, listOf(white), Contrast.AA_NORMAL, toward = black)
        assertTrue(Contrast.ratio(fixed, white) >= Contrast.AA_NORMAL)
        assertTrue(Contrast.ratio(fixed, white) < 6.0)
    }

    /** Every theme, dark and light, on solid surfaces, on the backdrop and on glass cards. */
    @Test fun everyThemeVariantPassesWcagAa() {
        val report = Themes.PRESETS.flatMap { t -> listOf("dark" to t.dark, "light" to t.light).map { t to it } }
            .mapNotNull { (t, v) ->
                ThemeChecks.failures(v.second).takeIf { it.isNotEmpty() }?.let { f ->
                    "${t.name} ${v.first}: " + f.joinToString { "${it.what} ${"%.2f".format(it.ratio)} < ${it.needed}" }
                }
            }
        assertTrue(report.joinToString("\n"), report.isEmpty())
    }

    @Test fun orbsAreNeverBrighterThanDesignedAndStayVisible() {
        Themes.PRESETS.filter { it.premium }.forEach { t ->
            assertTrue(t.name, t.dark.orbAlphaLimit in 0.2f..ThemeVariant.ORB_ALPHA_DARK)
            assertTrue(t.name, t.light.orbAlphaLimit in 0.2f..ThemeVariant.ORB_ALPHA_LIGHT)
            // Whatever the user's gradient setting, the drawn opacity stays within the checked limit.
            listOf(0, 60, 100).forEach { g ->
                val r = ResolvedTheme.resolve(ThemeCustomization(baseId = t.id, gradient = g))
                assertTrue(r.orbAlpha <= t.dark.orbAlphaLimit + 0.0001f)
                assertTrue(r.orbAlpha > 0f)
            }
        }
        assertEquals(0f, ResolvedTheme.resolve(ThemeCustomization(baseId = Themes.STEEL.id)).orbAlpha, 0f)
    }

    @Test fun modeFollowsChoiceThemeDefaultOrSystem() {
        fun dark(c: ThemeCustomization, system: Boolean) = ResolvedTheme.resolve(c, system).palette.dark
        // Themes open dark, "Steel Light" opens light, regardless of the system setting.
        assertTrue(dark(ThemeCustomization(baseId = Themes.PLASMA.id), system = false))
        assertFalse(dark(ThemeCustomization(baseId = Themes.STEEL_LIGHT_FIRST.id), system = true))
        assertFalse(dark(ThemeCustomization(baseId = Themes.PLASMA.id, mode = ThemeMode.LIGHT), system = true))
        assertTrue(dark(ThemeCustomization(baseId = Themes.PLASMA.id, mode = ThemeMode.SYSTEM), system = true))
        assertFalse(dark(ThemeCustomization(baseId = Themes.PLASMA.id, mode = ThemeMode.SYSTEM), system = false))
        // Dynamic follows the system unless a mode is chosen.
        assertFalse(dark(ThemeCustomization(baseId = Themes.DYNAMIC.id), system = false))
        assertTrue(dark(ThemeCustomization(baseId = Themes.DYNAMIC.id), system = true))
        assertTrue(dark(ThemeCustomization(baseId = Themes.DYNAMIC.id, mode = ThemeMode.DARK), system = false))
    }

    @Test fun defaultsAndAdjustmentsResolve() {
        val d = ResolvedTheme.resolve(ThemeCustomization())
        assertEquals(0.45f, d.glowIntensity, 0.0001f)
        assertEquals(0.60f, d.gradientIntensity, 0.0001f)
        assertEquals(1, d.borderDp)
        assertEquals(16, d.cornerDp)
        assertEquals(1f, d.textScale, 0.0001f)
        assertEquals(1f, d.durationScale, 0.0001f)
        assertEquals(1f, d.glassAlpha, 0f) // Steel has solid cards.
        assertFalse(d.reduceMotionByUser)

        val glass = ResolvedTheme.resolve(ThemeCustomization(baseId = Themes.AQUA_GLASS.id))
        assertEquals(0.62f, glass.glassAlpha, 0.0001f)
        assertEquals(1f, ResolvedTheme.resolve(ThemeCustomization(baseId = Themes.AQUA_GLASS.id, glass = false)).glassAlpha, 0f)

        val c = ResolvedTheme.resolve(
            ThemeCustomization(glow = 0, border = 4, corner = 32, textScale = 150, animationSpeed = 200, reduceMotion = true),
        )
        assertEquals(0f, c.glowIntensity, 0f)
        assertEquals(4, c.borderDp)
        assertEquals(32, c.cornerDp)
        assertEquals(1.5f, c.textScale, 0.0001f)
        assertEquals(0.5f, c.durationScale, 0.0001f) // twice as fast
        assertTrue(c.reduceMotionByUser)
    }

    @Test fun customizationIsADiffAndRoundTrips() {
        val c = ThemeCustomization(
            baseId = Themes.PLASMA.id, mode = ThemeMode.LIGHT, glow = 10, gradient = 100, border = 0,
            corner = 32, glass = false, textScale = 150, animationSpeed = 300, reduceMotion = true,
            liquidMotion = false, deleteEffect = false,
            customColors = BrandColors(0xFF112233, 0xFF445566, 0xFF778899, 0xFF000000, 0xFF101010, 0xFFFF0000, 0xFF00FF00),
        )
        assertEquals(c, ThemeCustomization.decode(c.encode()))
        val empty = ThemeCustomization(baseId = Themes.GOLD_EDITION.id)
        assertEquals(empty, ThemeCustomization.decode(empty.encode()))
        assertNull(ThemeCustomization.decode(empty.encode()).glow)
    }

    @Test fun malformedOutOfRangeOrOtherSchemaGivesDefaults() {
        val d = ThemeCustomization()
        assertEquals(d, ThemeCustomization.decode(null))
        assertEquals(d, ThemeCustomization.decode("garbage"))
        // The previous schema (other themes, six fields) is dropped.
        assertEquals(d, ThemeCustomization.decode("v1|neon_velocity|-|-|-|-"))
        assertEquals(d, ThemeCustomization.decode("v999|plasma|-|-|-|-|-|-|-|-|-|-|-|-"))
        assertEquals(Themes.DEFAULT.id, ThemeCustomization.decode("v2|no_such_theme|-|-|-|-|-|-|-|-|-|-|-|-").baseId)
        // Out-of-range numbers and unknown modes fall back to the default for that value only.
        val odd = ThemeCustomization.decode("v2|plasma|SIDEWAYS|500|-1|9|64|maybe|10|9000|true|-|perhaps|ff0000,zz")
        assertEquals(ThemeCustomization(baseId = Themes.PLASMA.id, reduceMotion = true), odd)
        // Too few colours is not a palette.
        assertNull(ThemeCustomization.decode("v2|custom|-|-|-|-|-|-|-|-|-|-|-|ff000000,ffffffff").customColors)
    }

    @Test fun reducedMotionHasNoDurationsAndSpeedScales() {
        val m = MotionTokens.REDUCED
        assertEquals(0, m.shortMs + m.mediumMs + m.longMs)
        assertEquals(140, MotionTokens.DEFAULT.scaled(0.5f).mediumMs)
        val r = ResolvedTheme.resolve(ThemeCustomization())
        assertTrue(r.liquidMotion && r.deleteEffect)
        val off = ResolvedTheme.resolve(ThemeCustomization(liquidMotion = false, deleteEffect = false))
        assertFalse(off.liquidMotion || off.deleteEffect)
    }

    /** Custom and Dynamic themes take any colours, so readability must hold by construction. */
    @Test fun anySevenColoursMakeAReadableTheme() {
        val random = java.util.Random(360)
        repeat(3000) {
            val raw = BrandColors.fromList(List(7) { 0xFF000000L or random.nextInt(0x1000000).toLong() })!!
            val failures = ThemeChecks.failures(ThemeVariant(raw.sanitized()))
            assertTrue("$raw: $failures", failures.isEmpty())
        }
        // Colours that already work are left exactly as chosen.
        Themes.PRESETS.forEach { t ->
            assertEquals(t.name, t.dark.colors, t.dark.colors.sanitized())
            assertEquals(t.name, t.light.colors, t.light.colors.sanitized())
        }
    }

    @Test fun customAndDynamicThemesResolveFromTheirColours() {
        val paper = BrandColors(0xFF224488, 0xFF444444, 0xFF886600, 0xFFFDFBF5, 0xFFFFFFFF, 0xFF224488, 0xFF886600)
        val custom = ResolvedTheme.resolve(ThemeCustomization(baseId = Themes.CUSTOM.id, customColors = paper), systemDark = true)
        // The Custom theme is as light as its background, whatever the system or mode says.
        assertFalse(custom.palette.dark)
        assertEquals(0xFFFDFBF5L, custom.palette.background)
        assertTrue(ThemeChecks.failures(custom.variant).isEmpty())
        // Before any editing it starts from Steel.
        assertEquals(Themes.STEEL.dark.palette, ResolvedTheme.resolve(ThemeCustomization(baseId = Themes.CUSTOM.id)).palette)

        val wallpaper = BrandColors(0xFFB0C6FF, 0xFFBFC6DC, 0xFFDEBCDF, 0xFF121318, 0xFF1E1F25, 0xFFB0C6FF, 0xFFDEBCDF)
        val dynamic = ResolvedTheme.resolve(ThemeCustomization(baseId = Themes.DYNAMIC.id), systemDark = true, dynamic = wallpaper)
        assertEquals(0xFF121318L, dynamic.palette.background)
        assertTrue(ThemeChecks.failures(dynamic.variant).isEmpty())
        // Without system colours (before Android 12) it shows Steel.
        assertEquals(Themes.STEEL.dark.palette, ResolvedTheme.resolve(ThemeCustomization(baseId = Themes.DYNAMIC.id)).palette)
    }
}
