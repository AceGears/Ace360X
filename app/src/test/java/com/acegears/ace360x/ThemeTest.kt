package com.acegears.ace360x

import com.acegears.ace360x.theme.AmbianceKind
import com.acegears.ace360x.theme.Contrast
import com.acegears.ace360x.theme.MotionTokens
import com.acegears.ace360x.theme.Palette
import com.acegears.ace360x.theme.ResolvedTheme
import com.acegears.ace360x.theme.ThemeChecks
import com.acegears.ace360x.theme.ThemeCustomization
import com.acegears.ace360x.theme.Themes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    @Test fun twelveNamedThemesWithUniqueIds() {
        val expected = listOf(
            "Obsidian Elite", "Neon Velocity", "Crimson Reactor", "Arctic Glass", "Aurora Spectrum",
            "Titanium Forge", "Emerald Matrix", "Solar Flare", "Midnight Cosmos", "Retro Console",
            "Minimal Luxe", "Liquid Chrome",
        )
        assertEquals(expected, Themes.ALL.map { it.name })
        assertEquals(12, Themes.ALL.map { it.id }.toSet().size)
    }

    @Test fun contrastMathMatchesWcag() {
        assertEquals(21.0, Contrast.ratio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, Contrast.ratio(0xFF336699, 0xFF336699), 0.0001)
        // #767676 on white is the commonly cited lightest grey passing AA (≈ 4.54).
        assertEquals(4.54, Contrast.ratio(0xFF767676, 0xFFFFFFFF), 0.01)
        assertTrue(Contrast.ratio(0xFF777777, 0xFFFFFFFF) < Contrast.AA_NORMAL)
    }

    @Test fun everyThemePassesWcagAa() {
        val report = Themes.ALL.mapNotNull { t ->
            ThemeChecks.failures(t).takeIf { it.isNotEmpty() }?.let { f ->
                t.name + ": " + f.joinToString { "${it.what} ${"%.2f".format(it.ratio)} < ${it.needed}" }
            }
        }
        assertTrue(report.joinToString("\n"), report.isEmpty())
    }

    @Test fun everyOfferedAccentPassesWcagAa() {
        Themes.ALL.forEach { t ->
            val accents = Themes.validAccentsFor(t)
            assertTrue("${t.name} offers too few accents", accents.size >= 3)
            accents.forEach { a ->
                val f = ThemeChecks.failures(t, Palette.from(t.seed, a))
                assertTrue("${t.name} accent ${java.lang.Long.toHexString(a)}: $f", f.isEmpty())
            }
        }
    }

    /** CLAUDE.md §10: a theme that only changes colours doesn't count. */
    @Test fun themesDifferBeyondColour() {
        val nonColour = Themes.ALL.map { t ->
            listOf(t.type, t.shapes, t.elevation.card, t.motion, t.icons, t.ambiance.kind)
        }
        assertEquals("two themes share every non-colour token", 12, nonColour.toSet().size)
        assertTrue(Themes.ALL.map { it.ambiance.kind }.toSet().size >= 11)
        assertTrue(Themes.ALL.map { it.type }.toSet().size >= 10)
        assertTrue(Themes.ALL.map { it.shapes }.toSet().size >= 10)
        assertTrue(Themes.ALL.map { it.motion }.toSet().size >= 10)
    }

    @Test fun customizationIsADiffAndRoundTrips() {
        val c = ThemeCustomization(
            baseId = Themes.NEON_VELOCITY.id,
            accent = Themes.validAccentsFor(Themes.NEON_VELOCITY).first(),
            ambianceEnabled = false,
            reduceMotion = true,
            cornerScale = 1.5f,
        )
        assertEquals(c, ThemeCustomization.decode(c.encode()))
        val empty = ThemeCustomization(baseId = Themes.SOLAR_FLARE.id)
        assertEquals(empty, ThemeCustomization.decode(empty.encode()))
        assertNull(ThemeCustomization.decode(empty.encode()).accent)
    }

    @Test fun malformedOrOtherSchemaGivesDefault() {
        val d = ThemeCustomization()
        assertEquals(d, ThemeCustomization.decode(null))
        assertEquals(d, ThemeCustomization.decode("garbage"))
        assertEquals(d, ThemeCustomization.decode("v999|neon_velocity|-|-|-|-"))
        assertEquals(Themes.DEFAULT.id, ThemeCustomization.decode("v1|no_such_theme|-|-|-|-").baseId)
    }

    @Test fun resolveIgnoresAccentsAndScalesThatAreNotOffered() {
        val luxe = Themes.MINIMAL_LUXE
        val bad = Themes.ACCENTS.first { it !in Themes.validAccentsFor(luxe) }
        val r = ResolvedTheme.resolve(ThemeCustomization(baseId = luxe.id, accent = bad, cornerScale = 7f))
        assertEquals(luxe.palette, r.palette)
        assertEquals(luxe.shapes, r.shapes)
        // Minimal Luxe has no ambiance, so it can't be enabled.
        assertEquals(AmbianceKind.NONE, luxe.ambiance.kind)
        assertFalse(ResolvedTheme.resolve(ThemeCustomization(baseId = luxe.id, ambianceEnabled = true)).ambianceEnabled)
    }

    @Test fun reducedMotionHasNoDurations() {
        val m = MotionTokens.REDUCED
        assertEquals(0, m.shortMs + m.mediumMs + m.longMs + m.ambianceLoopMs)
    }
}
