package com.acegears.ace360x.theme

/**
 * The built-in themes: the same set, names and colours as the owner's AceArcade app
 * (CLAUDE.md §10). Five glass themes with a backdrop, then twelve solid themes.
 * "Dynamic Material You" and "Custom" from AceArcade are not built yet.
 */
object Themes {
    private fun colors(p: Long, s: Long, a: Long, bg: Long, surface: Long, glow: Long, alt: Long) =
        BrandColors(p, s, a, bg, surface, glow, alt)

    private fun glass(
        id: String, name: String, tagline: String,
        dark: BrandColors, darkBackdrop: Backdrop, darkAlpha: Float,
        light: BrandColors, lightBackdrop: Backdrop, lightAlpha: Float,
    ) = ThemeSpec(
        id, name, tagline, premium = true,
        dark = ThemeVariant(dark, darkBackdrop, darkAlpha),
        light = ThemeVariant(light, lightBackdrop, lightAlpha),
    )

    /** A solid theme designed dark; its light version is derived with readable accents. */
    private fun solid(id: String, name: String, tagline: String, dark: BrandColors, light: BrandColors = lightFrom(dark)) =
        ThemeSpec(id, name, tagline, premium = false, dark = ThemeVariant(dark), light = ThemeVariant(light))

    /** Pale tinted background and near-white cards carrying the dark theme's accents. */
    fun lightFrom(d: BrandColors): BrandColors = d.copy(
        background = ColorMath.blend(0xFFF6F6F8, d.glow, 0.05f),
        surface = ColorMath.blend(0xFFFFFFFF, d.glow, 0.015f),
    )

    val AQUA_GLASS = glass(
        "aqua_glass", "Aqua Glass", "Frosted aqua dashboard",
        colors(0xFF22D3EE, 0xFF8FD8E8, 0xFF5EEAD4, 0xFF071B26, 0xFF0F2A37, 0xFF22D3EE, 0xFF3B82F6),
        Backdrop(0xFF0B3446, 0xFF04111A, listOf(Orb(0xFF0E8FA0, 0.15f, 0.08f, 0.75f), Orb(0xFF1E5AA8, 0.95f, 0.55f, 0.6f))), 0.62f,
        colors(0xFF0E7490, 0xFF155E75, 0xFF0891B2, 0xFFE4F3F8, 0xFFFFFFFF, 0xFF06B6D4, 0xFF2563EB),
        Backdrop(0xFFC6E9F2, 0xFFEAF6FA, listOf(Orb(0xFF7DD3E8, 0.1f, 0.05f, 0.7f), Orb(0xFFA5C8F5, 0.9f, 0.6f, 0.55f))), 0.7f,
    )

    val VIOLET_PULSE = glass(
        "violet_pulse", "Violet Pulse", "Violet glow on graphite",
        colors(0xFF8B5CF6, 0xFFC4B5FD, 0xFFA78BFA, 0xFF121016, 0xFF1C1A22, 0xFF8B5CF6, 0xFFF43F5E),
        Backdrop(0xFF1A1424, 0xFF0E0C12, listOf(Orb(0xFF6D28D9, 0.5f, 0.0f, 0.65f))), 0.78f,
        colors(0xFF6D28D9, 0xFF5B21B6, 0xFF7C3AED, 0xFFF5F3FA, 0xFFFFFFFF, 0xFF8B5CF6, 0xFFE11D48),
        Backdrop(0xFFEDE7FA, 0xFFF8F7FC, listOf(Orb(0xFFC4B5FD, 0.5f, 0.0f, 0.6f))), 0.8f,
    )

    val JADE_GLASS = glass(
        "jade_glass", "Jade Glass", "Jade-lit smoky glass",
        colors(0xFF34D3B8, 0xFFA7C4BC, 0xFF5EEAD4, 0xFF0F1413, 0xFF19211F, 0xFF2DD4BF, 0xFF94A3B8),
        Backdrop(0xFF1D2826, 0xFF0A0E0D, listOf(Orb(0xFF0F766E, 0.5f, 0.95f, 0.6f), Orb(0xFF334155, 0.1f, 0.1f, 0.5f))), 0.68f,
        colors(0xFF0F766E, 0xFF3F5F58, 0xFF0D9488, 0xFFEEF3F1, 0xFFFFFFFF, 0xFF14B8A6, 0xFF64748B),
        Backdrop(0xFFE1EBE7, 0xFFF6F8F7, listOf(Orb(0xFF99E6D8, 0.5f, 0.95f, 0.55f))), 0.74f,
    )

    val SANDSTONE = glass(
        "sandstone", "Sandstone", "Warm sandstone glass",
        colors(0xFFF26B3A, 0xFFE7C9A9, 0xFFFF8A4C, 0xFF17110C, 0xFF241A13, 0xFFF26B3A, 0xFFD4A373),
        Backdrop(0xFF3A2416, 0xFF140E0A, listOf(Orb(0xFFC2672E, 0.85f, 0.85f, 0.7f), Orb(0xFF5A3A22, 0.1f, 0.15f, 0.5f))), 0.7f,
        colors(0xFFC2410C, 0xFF7C4A2D, 0xFFEA580C, 0xFFF5EDE4, 0xFFFFFBF6, 0xFFF97316, 0xFFB45309),
        Backdrop(0xFFEAD7C3, 0xFFF9F2EA, listOf(Orb(0xFFF4B183, 0.85f, 0.85f, 0.65f))), 0.78f,
    )

    val PLASMA = glass(
        "plasma", "Plasma", "Liquid plasma glass",
        colors(0xFF8B7CFF, 0xFFB8C1FF, 0xFF38E1FF, 0xFF07071A, 0xFF12122A, 0xFF7B5CFF, 0xFFFF4FB3),
        Backdrop(
            0xFF0C0B26, 0xFF05040F,
            listOf(Orb(0xFF00B4E6, 0.05f, 0.1f, 0.6f), Orb(0xFF7B2FF7, 0.95f, 0.35f, 0.65f), Orb(0xFFF72585, 0.3f, 1.0f, 0.55f)),
        ), 0.6f,
        colors(0xFF5B3DF5, 0xFF3F3A8C, 0xFF0891B2, 0xFFF1F0FF, 0xFFFFFFFF, 0xFF7C5CFF, 0xFFDB2777),
        Backdrop(
            0xFFE7E5FF, 0xFFF7F6FF,
            listOf(Orb(0xFFA5F3FC, 0.05f, 0.1f, 0.55f), Orb(0xFFC4B5FD, 0.95f, 0.35f, 0.6f), Orb(0xFFFBCFE8, 0.3f, 1.0f, 0.5f)),
        ), 0.7f,
    )

    private val STEEL_DARK = colors(0xFFE3202B, 0xFFC9CDD4, 0xFFFF5A4E, 0xFF0B0B0E, 0xFF16161B, 0xFFFF2A2A, 0xFFC9CDD4)
    private val STEEL_LIGHT = colors(0xFFC4141F, 0xFF4A4D57, 0xFFE3202B, 0xFFF4F4F6, 0xFFFFFFFF, 0xFFE3202B, 0xFF6B6F7A)

    val STEEL = solid("steel", "AceArcade Steel", "Neon red on brushed steel", STEEL_DARK, STEEL_LIGHT)
    val STEEL_LIGHT_FIRST = solid("steel_light", "AceArcade Steel Light", "Steel in daylight", STEEL_DARK, STEEL_LIGHT)
    val NEON_CLASSIC = solid(
        "neon_classic", "AceArcade Neon (classic)", "Magenta and electric blue",
        colors(0xFFE5199C, 0xFF2E86FF, 0xFF2ED2FF, 0xFF06051A, 0xFF110C2A, 0xFFF03AD6, 0xFF2E86FF),
    )
    val NEON_PURPLE = solid(
        "neon_purple", "Neon Purple", "Ultraviolet",
        colors(0xFFB026FF, 0xFFE0B3FF, 0xFFD46BFF, 0xFF0A0414, 0xFF170A26, 0xFFB026FF, 0xFFFF2EC4),
    )
    val CYBER_BLUE = solid(
        "cyber_blue", "Cyber Blue", "Cold neon blue",
        colors(0xFF00B3FF, 0xFFA8E6FF, 0xFF00F0FF, 0xFF020A12, 0xFF0A1824, 0xFF00C8FF, 0xFF7A5CFF),
    )
    val MINIMAL = solid(
        "minimal", "Minimal", "Quiet greys",
        colors(0xFFD0D3DA, 0xFF8A8F99, 0xFFFFFFFF, 0xFF0E0F11, 0xFF17181B, 0xFF3A3D44, 0xFF2A2C31),
    )
    val AMOLED = solid(
        "amoled", "AMOLED Black", "True black",
        colors(0xFFE3202B, 0xFFC9CDD4, 0xFFFF5A4E, 0xFF000000, 0xFF0B0B0D, 0xFFFF2A2A, 0xFFC9CDD4),
        // Its light side is plain white with the same red.
        lightFrom(colors(0xFFE3202B, 0xFFC9CDD4, 0xFFFF5A4E, 0xFF000000, 0xFF0B0B0D, 0xFFFF2A2A, 0xFFC9CDD4))
            .copy(background = 0xFFFFFFFF, surface = 0xFFF7F7F8),
    )
    val MIDNIGHT_BLUE = solid(
        "midnight_blue", "Midnight Blue", "Deep navy",
        colors(0xFF4C8DFF, 0xFFB9C6E0, 0xFF7FB2FF, 0xFF070B18, 0xFF111833, 0xFF4C8DFF, 0xFF2ED2FF),
    )
    val CRIMSON_RED = solid(
        "crimson_red", "Crimson Red", "Hot red",
        colors(0xFFFF1E3C, 0xFFFFB3BC, 0xFFFF6B7D, 0xFF140509, 0xFF230A10, 0xFFFF1E3C, 0xFFFF6B7D),
    )
    val ROYAL_PURPLE = solid(
        "royal_purple", "Royal Purple", "Regal violet",
        colors(0xFF9B5CFF, 0xFFD3C2F5, 0xFFC08BFF, 0xFF0D0816, 0xFF1A1129, 0xFF9B5CFF, 0xFFF03AD6),
    )
    val EMERALD_GREEN = solid(
        "emerald_green", "Emerald Green", "Bright emerald",
        colors(0xFF1ED69B, 0xFFB5E8D6, 0xFF5CF0C0, 0xFF04120D, 0xFF0B2219, 0xFF1ED69B, 0xFF2ED2FF),
    )
    val GOLD_EDITION = solid(
        "gold_edition", "Gold Edition", "Black and gold",
        colors(0xFFE8B931, 0xFFF5E6B8, 0xFFFFD866, 0xFF0E0B05, 0xFF1C170B, 0xFFE8B931, 0xFFFFD866),
    )

    val ALL: List<ThemeSpec> = listOf(
        AQUA_GLASS, VIOLET_PULSE, JADE_GLASS, SANDSTONE, PLASMA,
        STEEL, STEEL_LIGHT_FIRST, NEON_CLASSIC, NEON_PURPLE, CYBER_BLUE, MINIMAL, AMOLED,
        MIDNIGHT_BLUE, CRIMSON_RED, ROYAL_PURPLE, EMERALD_GREEN, GOLD_EDITION,
    )

    /** AceArcade's signature look. */
    val DEFAULT: ThemeSpec = STEEL

    fun byId(id: String?): ThemeSpec = ALL.firstOrNull { it.id == id } ?: DEFAULT
}

/**
 * User customisation, stored as a diff over a base theme: null = the default value.
 * Ranges and defaults follow AceArcade's theme editor.
 */
data class ThemeCustomization(
    val baseId: String = Themes.DEFAULT.id,
    val mode: ThemeMode? = null,
    /** Glow strength, 0–100. */
    val glow: Int? = null,
    /** Strength of header gradients and backdrop light, 0–100. */
    val gradient: Int? = null,
    /** Card border width, 0–4 dp. */
    val border: Int? = null,
    /** Corner radius, 0–32 dp. */
    val corner: Int? = null,
    /** Translucent cards on themes with a backdrop. */
    val glass: Boolean? = null,
    /** Text size, 80–150 %. */
    val textScale: Int? = null,
    /** Animation speed, 25–300 % (higher = faster). */
    val animationSpeed: Int? = null,
    val reduceMotion: Boolean? = null,
) {
    /** Compact form for passing to the game process in the launch Intent. */
    fun encode(): String = listOf<Any?>(
        "v$SCHEMA_VERSION", baseId, mode?.name, glow, gradient, border, corner, glass, textScale,
        animationSpeed, reduceMotion,
    ).joinToString("|") { it?.toString() ?: "-" }

    companion object {
        /** Bump when the stored keys change meaning; older diffs are then dropped. */
        const val SCHEMA_VERSION = 2
        private const val FIELDS = 11

        const val DEFAULT_GLOW = 45
        const val DEFAULT_GRADIENT = 60
        const val DEFAULT_BORDER = 1
        const val DEFAULT_CORNER = 16
        const val DEFAULT_TEXT_SCALE = 100
        const val DEFAULT_ANIMATION_SPEED = 100
        val GLOW_RANGE = 0..100
        val GRADIENT_RANGE = 0..100
        val BORDER_RANGE = 0..4
        val CORNER_RANGE = 0..32
        val TEXT_SCALE_RANGE = 80..150
        val ANIMATION_SPEED_RANGE = 25..300

        /** Inverse of [encode]; anything malformed or from another schema gives the default. */
        fun decode(s: String?): ThemeCustomization {
            val f = s?.split("|") ?: return ThemeCustomization()
            if (f.size != FIELDS || f[0] != "v$SCHEMA_VERSION") return ThemeCustomization()
            fun <T> opt(v: String, parse: (String) -> T?): T? = if (v == "-") null else parse(v)
            fun int(v: String, range: IntRange) = opt(v) { it.toIntOrNull()?.takeIf { n -> n in range } }
            return ThemeCustomization(
                baseId = Themes.byId(f[1]).id,
                mode = opt(f[2]) { n -> ThemeMode.entries.firstOrNull { it.name == n } },
                glow = int(f[3], GLOW_RANGE),
                gradient = int(f[4], GRADIENT_RANGE),
                border = int(f[5], BORDER_RANGE),
                corner = int(f[6], CORNER_RANGE),
                glass = opt(f[7]) { it.toBooleanStrictOrNull() },
                textScale = int(f[8], TEXT_SCALE_RANGE),
                animationSpeed = int(f[9], ANIMATION_SPEED_RANGE),
                reduceMotion = opt(f[10]) { it.toBooleanStrictOrNull() },
            )
        }
    }
}

/** A base theme with a customisation applied, for the current system dark/light state. */
data class ResolvedTheme(
    val spec: ThemeSpec,
    val variant: ThemeVariant,
    val palette: Palette,
    /** 0–1. */
    val glowIntensity: Float,
    val gradientIntensity: Float,
    val borderDp: Int,
    val cornerDp: Int,
    /** Card opacity over the backdrop; 1 = solid cards. */
    val glassAlpha: Float,
    /** Orb opacity on the backdrop (0 when there is none). */
    val orbAlpha: Float,
    val textScale: Float,
    /** Multiplier for animation durations (2 = half speed). */
    val durationScale: Float,
    val reduceMotionByUser: Boolean,
) {
    val backdrop: Backdrop? get() = variant.backdrop

    companion object {
        fun resolve(c: ThemeCustomization, systemDark: Boolean = true): ResolvedTheme {
            val spec = Themes.byId(c.baseId)
            val dark = when (c.mode ?: defaultMode(spec)) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }
            val variant = spec.variant(dark)
            val gradient = (c.gradient ?: ThemeCustomization.DEFAULT_GRADIENT)
                .coerceIn(ThemeCustomization.GRADIENT_RANGE) / 100f
            val designOrb = (if (dark) ThemeVariant.ORB_ALPHA_DARK else ThemeVariant.ORB_ALPHA_LIGHT) *
                (0.55f + 0.45f * gradient)
            val speed = (c.animationSpeed ?: ThemeCustomization.DEFAULT_ANIMATION_SPEED)
                .coerceIn(ThemeCustomization.ANIMATION_SPEED_RANGE)
            return ResolvedTheme(
                spec = spec,
                variant = variant,
                palette = variant.palette,
                glowIntensity = (c.glow ?: ThemeCustomization.DEFAULT_GLOW).coerceIn(ThemeCustomization.GLOW_RANGE) / 100f,
                gradientIntensity = gradient,
                borderDp = (c.border ?: ThemeCustomization.DEFAULT_BORDER).coerceIn(ThemeCustomization.BORDER_RANGE),
                cornerDp = (c.corner ?: ThemeCustomization.DEFAULT_CORNER).coerceIn(ThemeCustomization.CORNER_RANGE),
                glassAlpha = if (variant.backdrop != null && (c.glass ?: true)) variant.glassAlpha else 1f,
                orbAlpha = if (variant.backdrop != null) minOf(designOrb, variant.orbAlphaLimit) else 0f,
                textScale = (c.textScale ?: ThemeCustomization.DEFAULT_TEXT_SCALE)
                    .coerceIn(ThemeCustomization.TEXT_SCALE_RANGE) / 100f,
                durationScale = 100f / speed,
                reduceMotionByUser = c.reduceMotion ?: false,
            )
        }

        /** "Steel Light" opens light; every other theme opens dark until the user picks a mode. */
        fun defaultMode(spec: ThemeSpec): ThemeMode =
            if (spec.id == Themes.STEEL_LIGHT_FIRST.id) ThemeMode.LIGHT else ThemeMode.DARK
    }
}

/** WCAG AA checks for one theme variant; used by the unit test. */
object ThemeChecks {
    data class Failure(val what: String, val ratio: Double, val needed: Double)

    fun failures(variant: ThemeVariant): List<Failure> {
        val p = variant.palette
        val out = mutableListOf<Failure>()
        fun check(what: String, fg: Long, bg: Long, need: Double = Contrast.AA_NORMAL) {
            val r = Contrast.ratio(fg, bg)
            if (r < need) out += Failure(what, r, need)
        }
        // Text on every opaque surface, on the backdrop (bare and orb-lit) and on glass cards.
        variant.textSurfaces(p).forEachIndexed { i, s ->
            check("onSurface on surface#$i", p.onSurface, s)
            check("onSurfaceVariant on surface#$i", p.onSurfaceVariant, s)
            // Primary is used as text (TextButton, links, selected labels).
            check("primary text on surface#$i", p.primary, s)
            check("error text on surface#$i", p.error, s)
            // Secondary is used for sub-headings.
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
        // Non-text UI: the focus ring must be visible (WCAG 1.4.11, 3:1).
        check("focus ring/background", p.focusRing, p.background, Contrast.AA_LARGE_OR_UI)
        check("focus ring/surface", p.focusRing, p.surface, Contrast.AA_LARGE_OR_UI)
        variant.backdrop?.let {
            check("focus ring/backdrop top", p.focusRing, it.top, Contrast.AA_LARGE_OR_UI)
            check("focus ring/backdrop bottom", p.focusRing, it.bottom, Contrast.AA_LARGE_OR_UI)
        }
        return out
    }
}
