package com.acegears.ace360x.theme

/** The 12 built-in themes (CLAUDE.md §10). Every token group differs, not just colour. */
object Themes {

    val OBSIDIAN_ELITE = ThemeSpec(
        id = "obsidian_elite", name = "Obsidian Elite", tagline = "Black glass, gold detail",
        seed = PaletteSeed(
            dark = true, background = 0xFF0A0A0B, surface = 0xFF121214, surfaceVariant = 0xFF1D1C1F,
            primary = 0xFFD4AF37, secondary = 0xFFB9B8BE, tertiary = 0xFFC9A66B,
            text = 0xFFEDEAE3, mutedText = 0xFFB5AFA3, outline = 0xFF5A5446,
        ),
        type = TypeTokens(FontKind.SERIF, FontKind.DEFAULT, headingLetterSpacing = 0.4f),
        shapes = ShapeTokens(CornerKind.ROUNDED, 4f, 6f, 10f),
        elevation = ElevationTokens(CardStyle.OUTLINED, 0f, 1f),
        motion = MotionTokens(120, 240, 420, EasingKind.EMPHASIZED, ambianceLoopMs = 9000),
        icons = IconStyle.OUTLINED,
        ambiance = AmbianceTokens(AmbianceKind.VIGNETTE_GLINT, listOf(0xFFD4AF37, 0xFF2A2516), 0.10f),
        components = ComponentTokens(true, 2f, 2f, 0xFFD4AF37),
    )

    val NEON_VELOCITY = ThemeSpec(
        id = "neon_velocity", name = "Neon Velocity", tagline = "Synthwave speed",
        seed = PaletteSeed(
            dark = true, background = 0xFF07060F, surface = 0xFF0F0D1C, surfaceVariant = 0xFF1A1730,
            primary = 0xFF00E5FF, secondary = 0xFFFF7ADF, tertiary = 0xFFB4FF39,
            text = 0xFFF2F0FF, mutedText = 0xFFBDB6DE, outline = 0xFF4A4280,
        ),
        type = TypeTokens(FontKind.CONDENSED, FontKind.DEFAULT, headingLetterSpacing = 1.2f, uppercaseLabels = true),
        shapes = ShapeTokens(CornerKind.CUT, 6f, 10f, 14f),
        elevation = ElevationTokens(CardStyle.GLOW, 0f, 1f),
        motion = MotionTokens(90, 180, 300, EasingKind.EMPHASIZED, ambianceLoopMs = 4000),
        icons = IconStyle.SHARP,
        ambiance = AmbianceTokens(AmbianceKind.SYNTH_GRID, listOf(0xFFFF4FD2, 0xFF00E5FF), 0.16f),
        components = ComponentTokens(false, 4f, 3f, 0xFFFF4FD2, sliderUsesSecondary = true),
    )

    val CRIMSON_REACTOR = ThemeSpec(
        id = "crimson_reactor", name = "Crimson Reactor", tagline = "Red-hot core",
        seed = PaletteSeed(
            dark = true, background = 0xFF0D0606, surface = 0xFF170A0A, surfaceVariant = 0xFF261313,
            primary = 0xFFFF5A52, secondary = 0xFFFFA05C, tertiary = 0xFFFFD166,
            text = 0xFFF7ECEA, mutedText = 0xFFD2B6B2, outline = 0xFF6A3532,
        ),
        type = TypeTokens(FontKind.BLACK, FontKind.DEFAULT, headingLetterSpacing = 0.2f),
        shapes = ShapeTokens(CornerKind.ROUNDED, 6f, 12f, 20f),
        elevation = ElevationTokens(CardStyle.TONAL, 4f, 0f),
        motion = MotionTokens(100, 220, 380, EasingKind.EMPHASIZED, ambianceLoopMs = 3200),
        icons = IconStyle.FILLED,
        ambiance = AmbianceTokens(AmbianceKind.REACTOR_CORE, listOf(0xFFFF3B30, 0xFFFF8A3D), 0.11f),
        components = ComponentTokens(true, 6f, 3f, 0xFFFFA05C, switchUsesTertiary = true),
    )

    val ARCTIC_GLASS = ThemeSpec(
        id = "arctic_glass", name = "Arctic Glass", tagline = "Frosted light",
        seed = PaletteSeed(
            dark = false, background = 0xFFEAF2F8, surface = 0xFFF7FAFD, surfaceVariant = 0xFFDCE7F0,
            primary = 0xFF0A5A87, secondary = 0xFF355F78, tertiary = 0xFF4B4F96,
            text = 0xFF0E1A24, mutedText = 0xFF3A4D5C, outline = 0xFF7F98AB,
        ),
        type = TypeTokens(FontKind.LIGHT, FontKind.DEFAULT, headingLetterSpacing = 0.3f),
        shapes = ShapeTokens(CornerKind.ROUNDED, 12f, 20f, 28f),
        elevation = ElevationTokens(CardStyle.GLASS, 0f, 1f, cardAlpha = 0.78f),
        motion = MotionTokens(140, 280, 480, EasingKind.DECELERATE, ambianceLoopMs = 14000),
        icons = IconStyle.ROUNDED,
        ambiance = AmbianceTokens(AmbianceKind.FROST, listOf(0xFFB9D7EC, 0xFFCFE0F2), 0.40f),
        components = ComponentTokens(true, 3f, 2f, 0xFF0A5A87),
    )

    val AURORA_SPECTRUM = ThemeSpec(
        id = "aurora_spectrum", name = "Aurora Spectrum", tagline = "Polar lights",
        seed = PaletteSeed(
            dark = true, background = 0xFF050B12, surface = 0xFF0B1520, surfaceVariant = 0xFF15222F,
            primary = 0xFF5CF2B5, secondary = 0xFFB8AEFF, tertiary = 0xFF4FC3F7,
            text = 0xFFE8F4F2, mutedText = 0xFFA9C4C6, outline = 0xFF35536A,
        ),
        type = TypeTokens(FontKind.MEDIUM, FontKind.DEFAULT, headingLetterSpacing = 0.1f),
        shapes = ShapeTokens(CornerKind.ROUNDED, 16f, 24f, 32f),
        elevation = ElevationTokens(CardStyle.TONAL, 1f, 0f),
        motion = MotionTokens(150, 300, 520, EasingKind.STANDARD, ambianceLoopMs = 16000),
        icons = IconStyle.ROUNDED,
        ambiance = AmbianceTokens(AmbianceKind.AURORA, listOf(0xFF5CF2B5, 0xFFA295FF, 0xFF4FC3F7), 0.16f),
        components = ComponentTokens(true, 4f, 2f, 0xFF5CF2B5, sliderUsesSecondary = true),
    )

    val TITANIUM_FORGE = ThemeSpec(
        id = "titanium_forge", name = "Titanium Forge", tagline = "Machined metal, forge glow",
        seed = PaletteSeed(
            dark = true, background = 0xFF101113, surface = 0xFF17191C, surfaceVariant = 0xFF24272B,
            primary = 0xFFFF8A3D, secondary = 0xFFAAB2BB, tertiary = 0xFF86ACCD,
            text = 0xFFE9EBED, mutedText = 0xFFB2B8BF, outline = 0xFF5A616A,
        ),
        type = TypeTokens(FontKind.CONDENSED, FontKind.DEFAULT, headingLetterSpacing = 0.6f, uppercaseLabels = true),
        shapes = ShapeTokens(CornerKind.CUT, 4f, 6f, 8f),
        elevation = ElevationTokens(CardStyle.OUTLINED, 2f, 1.5f),
        motion = MotionTokens(80, 160, 280, EasingKind.LINEAR, ambianceLoopMs = 0),
        icons = IconStyle.SHARP,
        ambiance = AmbianceTokens(AmbianceKind.BRUSHED_METAL, listOf(0xFFAAB2BB, 0xFFFF8A3D), 0.06f),
        components = ComponentTokens(false, 4f, 2f, 0xFFFF8A3D),
    )

    val EMERALD_MATRIX = ThemeSpec(
        id = "emerald_matrix", name = "Emerald Matrix", tagline = "Terminal green",
        seed = PaletteSeed(
            dark = true, background = 0xFF020805, surface = 0xFF06120B, surfaceVariant = 0xFF0E2115,
            primary = 0xFF2BFF88, secondary = 0xFF7FDBA3, tertiary = 0xFFC4FF6B,
            text = 0xFFD9FFE6, mutedText = 0xFF95C9A8, outline = 0xFF22603D,
        ),
        type = TypeTokens(FontKind.MONOSPACE, FontKind.MONOSPACE),
        shapes = ShapeTokens(CornerKind.ROUNDED, 0f, 2f, 4f),
        elevation = ElevationTokens(CardStyle.OUTLINED, 0f, 1f),
        motion = MotionTokens(60, 120, 200, EasingKind.LINEAR, ambianceLoopMs = 6000),
        icons = IconStyle.SHARP,
        ambiance = AmbianceTokens(AmbianceKind.DIGITAL_RAIN, listOf(0xFF2BFF88), 0.20f),
        components = ComponentTokens(true, 3f, 2f, 0xFF2BFF88),
    )

    val SOLAR_FLARE = ThemeSpec(
        id = "solar_flare", name = "Solar Flare", tagline = "Molten amber",
        seed = PaletteSeed(
            dark = true, background = 0xFF120A03, surface = 0xFF1C1107, surfaceVariant = 0xFF2B1B0D,
            primary = 0xFFFFB000, secondary = 0xFFFF9466, tertiary = 0xFFFFE082,
            text = 0xFFFFF3E3, mutedText = 0xFFDBC0A0, outline = 0xFF74512B,
        ),
        type = TypeTokens(FontKind.MEDIUM, FontKind.DEFAULT, headingLetterSpacing = 0.5f),
        shapes = ShapeTokens(CornerKind.ROUNDED, 10f, 18f, 26f),
        elevation = ElevationTokens(CardStyle.TONAL, 3f, 0f),
        motion = MotionTokens(110, 230, 400, EasingKind.EMPHASIZED, ambianceLoopMs = 12000),
        icons = IconStyle.FILLED,
        ambiance = AmbianceTokens(AmbianceKind.CORONA, listOf(0xFFFFB000, 0xFFFF7A4D), 0.16f),
        components = ComponentTokens(true, 5f, 3f, 0xFFFFE082, switchUsesTertiary = true),
    )

    val MIDNIGHT_COSMOS = ThemeSpec(
        id = "midnight_cosmos", name = "Midnight Cosmos", tagline = "Deep space",
        seed = PaletteSeed(
            dark = true, background = 0xFF04040F, surface = 0xFF0B0B1E, surfaceVariant = 0xFF16162F,
            primary = 0xFFA996FF, secondary = 0xFF79AEFF, tertiary = 0xFFFF95DB,
            text = 0xFFECEBFF, mutedText = 0xFFB5B3D9, outline = 0xFF3D3B69,
        ),
        type = TypeTokens(FontKind.LIGHT, FontKind.DEFAULT, headingLetterSpacing = 1.0f),
        shapes = ShapeTokens(CornerKind.ROUNDED, 12f, 20f, 28f),
        elevation = ElevationTokens(CardStyle.TONAL, 2f, 0f),
        motion = MotionTokens(160, 320, 560, EasingKind.DECELERATE, ambianceLoopMs = 8000),
        icons = IconStyle.OUTLINED,
        ambiance = AmbianceTokens(AmbianceKind.STARFIELD, listOf(0xFFFFFFFF, 0xFFA996FF), 0.70f),
        components = ComponentTokens(true, 3f, 2f, 0xFFA996FF),
    )

    val RETRO_CONSOLE = ThemeSpec(
        id = "retro_console", name = "Retro Console", tagline = "Amber phosphor CRT",
        seed = PaletteSeed(
            dark = true, background = 0xFF14110A, surface = 0xFF1E1A10, surfaceVariant = 0xFF2B2617,
            primary = 0xFFFFC94A, secondary = 0xFF9FD66F, tertiary = 0xFFF07A6C,
            text = 0xFFF5E9C8, mutedText = 0xFFCDBE96, outline = 0xFF6A5F3A,
        ),
        type = TypeTokens(FontKind.MONOSPACE, FontKind.DEFAULT, headingLetterSpacing = 0.8f, uppercaseLabels = true),
        shapes = ShapeTokens(CornerKind.ROUNDED, 0f, 0f, 2f),
        elevation = ElevationTokens(CardStyle.OUTLINED, 0f, 2f),
        motion = MotionTokens(50, 100, 160, EasingKind.LINEAR, ambianceLoopMs = 0),
        icons = IconStyle.SHARP,
        ambiance = AmbianceTokens(AmbianceKind.SCANLINES, listOf(0xFF000000, 0xFFFFC94A), 0.13f),
        components = ComponentTokens(false, 8f, 3f, 0xFFFFC94A),
    )

    val MINIMAL_LUXE = ThemeSpec(
        id = "minimal_luxe", name = "Minimal Luxe", tagline = "Quiet, paper-white",
        seed = PaletteSeed(
            dark = false, background = 0xFFFAF8F5, surface = 0xFFFFFFFF, surfaceVariant = 0xFFF0ECE6,
            primary = 0xFF1F1F1F, secondary = 0xFF665A4B, tertiary = 0xFF7E6331,
            text = 0xFF1A1A1A, mutedText = 0xFF55504A, outline = 0xFFC9C1B6,
        ),
        type = TypeTokens(FontKind.SERIF, FontKind.DEFAULT, headingLetterSpacing = 0.2f),
        shapes = ShapeTokens(CornerKind.ROUNDED, 2f, 4f, 8f),
        elevation = ElevationTokens(CardStyle.OUTLINED, 0f, 0.5f),
        motion = MotionTokens(130, 260, 440, EasingKind.STANDARD, ambianceLoopMs = 0),
        icons = IconStyle.OUTLINED,
        ambiance = AmbianceTokens(AmbianceKind.NONE, emptyList(), 0f),
        components = ComponentTokens(true, 2f, 1.5f, 0xFF1F1F1F),
    )

    val LIQUID_CHROME = ThemeSpec(
        id = "liquid_chrome", name = "Liquid Chrome", tagline = "Polished reflections",
        seed = PaletteSeed(
            dark = true, background = 0xFF0C0D10, surface = 0xFF15171B, surfaceVariant = 0xFF23262C,
            primary = 0xFFD8E1EA, secondary = 0xFFA3B8CB, tertiary = 0xFFCDB0FF,
            text = 0xFFF0F3F6, mutedText = 0xFFB8C0C9, outline = 0xFF5A626D,
        ),
        type = TypeTokens(FontKind.MEDIUM, FontKind.DEFAULT, headingLetterSpacing = 0.3f),
        shapes = ShapeTokens(CornerKind.ROUNDED, 18f, 26f, 34f),
        elevation = ElevationTokens(CardStyle.GLOW, 2f, 1f),
        motion = MotionTokens(140, 300, 500, EasingKind.EMPHASIZED, ambianceLoopMs = 10000),
        icons = IconStyle.ROUNDED,
        ambiance = AmbianceTokens(AmbianceKind.CHROME_SWEEP, listOf(0xFFD8E1EA, 0xFF7C8796), 0.10f),
        components = ComponentTokens(true, 4f, 2f, 0xFFCDB0FF),
    )

    val ALL: List<ThemeSpec> = listOf(
        OBSIDIAN_ELITE, NEON_VELOCITY, CRIMSON_REACTOR, ARCTIC_GLASS, AURORA_SPECTRUM,
        TITANIUM_FORGE, EMERALD_MATRIX, SOLAR_FLARE, MIDNIGHT_COSMOS, RETRO_CONSOLE,
        MINIMAL_LUXE, LIQUID_CHROME,
    )

    val DEFAULT: ThemeSpec = OBSIDIAN_ELITE

    fun byId(id: String?): ThemeSpec = ALL.firstOrNull { it.id == id } ?: DEFAULT

    /** Accent choices offered as an override; filtered per theme by [validAccentsFor]. */
    val ACCENTS: List<Long> = listOf(
        0xFFD4AF37, 0xFF00E5FF, 0xFFFF5A52, 0xFF5CF2B5, 0xFFFF8A3D, 0xFFA996FF,
        0xFFFF4FD2, 0xFF2BFF88, 0xFFFFC94A, 0xFF79AEFF,
        0xFF0A5A87, 0xFF8E2A5B, 0xFF1F1F1F, 0xFF2E6B3A, 0xFF6A3FB5, 0xFFA33A12,
    )

    fun validAccentsFor(theme: ThemeSpec): List<Long> = ACCENTS.filter { accent ->
        ThemeChecks.failures(theme, Palette.from(theme.seed, accent)).isEmpty()
    }
}

/** User customisation, stored as a diff over a base theme. Null = use the base value. */
data class ThemeCustomization(
    val baseId: String = Themes.DEFAULT.id,
    val accent: Long? = null,
    val ambianceEnabled: Boolean? = null,
    val reduceMotion: Boolean? = null,
    val cornerScale: Float? = null,
) {
    /** Compact form for passing to the game process in the launch Intent. */
    fun encode(): String = listOf(
        "v$SCHEMA_VERSION", baseId,
        accent?.let { java.lang.Long.toHexString(it) } ?: "-",
        ambianceEnabled?.toString() ?: "-",
        reduceMotion?.toString() ?: "-",
        cornerScale?.toString() ?: "-",
    ).joinToString("|")

    companion object {
        /** Bump when the stored keys change meaning; older diffs are then dropped. */
        const val SCHEMA_VERSION = 1
        val CORNER_SCALES = listOf(0.5f, 1f, 1.5f)

        /** Inverse of [encode]; anything malformed or from another schema gives the default. */
        fun decode(s: String?): ThemeCustomization {
            val f = s?.split("|") ?: return ThemeCustomization()
            if (f.size != 6 || f[0] != "v$SCHEMA_VERSION") return ThemeCustomization()
            fun <T> opt(v: String, parse: (String) -> T?): T? = if (v == "-") null else parse(v)
            return ThemeCustomization(
                baseId = Themes.byId(f[1]).id,
                accent = opt(f[2]) { it.toLongOrNull(16) },
                ambianceEnabled = opt(f[3]) { it.toBooleanStrictOrNull() },
                reduceMotion = opt(f[4]) { it.toBooleanStrictOrNull() },
                cornerScale = opt(f[5]) { it.toFloatOrNull() },
            )
        }
    }
}

/** A base theme with a customisation applied. */
data class ResolvedTheme(
    val spec: ThemeSpec,
    val palette: Palette,
    val shapes: ShapeTokens,
    val ambianceEnabled: Boolean,
    val reduceMotionByUser: Boolean,
) {
    companion object {
        fun resolve(c: ThemeCustomization): ResolvedTheme {
            val spec = Themes.byId(c.baseId)
            // An accent that is not valid for this theme (e.g. after a base switch) is ignored.
            val accent = c.accent?.takeIf { it in Themes.validAccentsFor(spec) }
            val scale = c.cornerScale?.takeIf { it in ThemeCustomization.CORNER_SCALES } ?: 1f
            return ResolvedTheme(
                spec = spec,
                palette = if (accent != null) Palette.from(spec.seed, accent) else spec.palette,
                shapes = spec.shapes.scaled(scale),
                ambianceEnabled = (c.ambianceEnabled ?: true) && spec.ambiance.kind != AmbianceKind.NONE,
                reduceMotionByUser = c.reduceMotion ?: false,
            )
        }
    }
}

/**
 * Strongest opacity with which an ambiance covers an area (radial/linear fills peak
 * at up to 1.6 × intensity in Ambiance.kt). Starfield draws only 1–3 px dots, so its
 * area tint is negligible.
 */
fun ambianceAreaAlpha(t: AmbianceTokens): Float = when (t.kind) {
    AmbianceKind.NONE -> 0f
    AmbianceKind.STARFIELD -> 0.05f
    else -> minOf(1f, t.intensity * 1.6f)
}

/** WCAG AA checks for a theme's palette; used by the unit test and accent filtering. */
object ThemeChecks {
    data class Failure(val what: String, val ratio: Double, val needed: Double)

    fun failures(theme: ThemeSpec, p: Palette = theme.palette): List<Failure> {
        val out = mutableListOf<Failure>()
        fun check(what: String, fg: Long, bg: Long, need: Double = Contrast.AA_NORMAL) {
            val r = Contrast.ratio(fg, bg)
            if (r < need) out += Failure(what, r, need)
        }
        // Body text and secondary text on every surface level.
        val surfaces = mutableListOf<Long>().apply {
            addAll(p.textSurfaces)
            // Translucent cards: what text actually sits on.
            if (theme.elevation.card == CardStyle.GLASS) {
                val card = p.surfaceContainerHighest
                add(ColorMath.over(card, theme.elevation.cardAlpha, p.background))
                theme.ambiance.colors.forEach { amb ->
                    val under = ColorMath.over(amb, ambianceAreaAlpha(theme.ambiance), p.background)
                    add(ColorMath.over(card, theme.elevation.cardAlpha, under))
                }
            }
        }
        // Text drawn straight on the background sits on the ambiance's tint.
        val tint = ambianceAreaAlpha(theme.ambiance)
        theme.ambiance.colors.forEach { amb -> surfaces += ColorMath.over(amb, tint, p.background) }
        surfaces.forEachIndexed { i, s ->
            check("onSurface on surface#$i", p.onSurface, s)
            check("onSurfaceVariant on surface#$i", p.onSurfaceVariant, s)
            // Primary is used as text (TextButton, links, selected labels).
            check("primary text on surface#$i", p.primary, s)
            check("error text on surface#$i", p.error, s)
            // Secondary is used for sub-headings and "custom" markers.
            check("secondary text on surface#$i", p.secondary, s)
        }
        check("onBackground/background", p.onBackground, p.background)
        check("onPrimary/primary", p.onPrimary, p.primary)
        check("onPrimaryContainer/primaryContainer", p.onPrimaryContainer, p.primaryContainer)
        check("onSecondary/secondary", p.onSecondary, p.secondary)
        check("onSecondaryContainer/secondaryContainer", p.onSecondaryContainer, p.secondaryContainer)
        check("onTertiary/tertiary", p.onTertiary, p.tertiary)
        check("onTertiaryContainer/tertiaryContainer", p.onTertiaryContainer, p.tertiaryContainer)
        check("onError/error", p.onError, p.error)
        check("onErrorContainer/errorContainer", p.onErrorContainer, p.errorContainer)
        // Non-text UI: focus ring and outline must be visible (WCAG 1.4.11, 3:1).
        check("focus ring/background", theme.components.focusRingColor, p.background, Contrast.AA_LARGE_OR_UI)
        check("focus ring/surface", theme.components.focusRingColor, p.surface, Contrast.AA_LARGE_OR_UI)
        return out
    }
}
