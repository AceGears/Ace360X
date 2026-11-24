package com.acegears.ace360x.theme

/*
 * Theme token model, matching the design of the owner's AceArcade app (see
 * docs/research/acearcade-design.md). Written for Ace360X; no AceArcade code is used.
 * Plain Kotlin (ARGB Longs, enums) so contrast and customisation logic is unit-testable
 * on the JVM. Compose mapping lives in AceTheme.kt.
 */

enum class ThemeMode(val label: String) { SYSTEM("Follow system"), DARK("Dark"), LIGHT("Light") }

enum class EasingKind { STANDARD, EMPHASIZED, DECELERATE, LINEAR }

/** Central motion tokens (CLAUDE.md §9 `MotionTokens`). Durations in ms. */
data class MotionTokens(
    val shortMs: Int,
    val mediumMs: Int,
    val longMs: Int,
    val easing: EasingKind,
) {
    /** [factor] > 1 slows animations down, < 1 speeds them up. */
    fun scaled(factor: Float): MotionTokens = copy(
        shortMs = (shortMs * factor).toInt(),
        mediumMs = (mediumMs * factor).toInt(),
        longMs = (longMs * factor).toInt(),
    )

    companion object {
        val DEFAULT = MotionTokens(140, 280, 460, EasingKind.EMPHASIZED)

        /** Used when animations are off system-wide or by the user. */
        val REDUCED = MotionTokens(0, 0, 0, EasingKind.LINEAR)
    }
}

/** A soft radial light on the backdrop; x, y and radius are fractions of the screen. */
data class Orb(val color: Long, val x: Float, val y: Float, val radius: Float)

/** Painted behind every screen: a vertical gradient with light orbs. Static, never animated. */
data class Backdrop(val top: Long, val bottom: Long, val orbs: List<Orb>)

/** The seven hand-picked colours of one theme variant; every other role is derived. */
data class BrandColors(
    val primary: Long,
    val secondary: Long,
    val accent: Long,
    val background: Long,
    val surface: Long,
    /** Neon used for glows, edges and gradients (decorative, never text). */
    val glow: Long,
    /** Second tone of dual-colour edges and gradients. */
    val glowAlt: Long,
) {
    val asList: List<Long> get() = listOf(primary, secondary, accent, background, surface, glow, glowAlt)

    /**
     * These colours made safe to build a theme from, whatever was entered (Custom theme,
     * Android's dynamic colours): everything opaque, and the background and card colour
     * pushed darker (or lighter) just far enough that the theme's text reads at WCAG AA on
     * every surface derived from them. Colours that already work are returned unchanged.
     */
    fun sanitized(): BrandColors {
        fun opaque(c: Long) = c or 0xFF000000L
        val bg0 = opaque(background)
        val dark = Contrast.luminance(bg0) < ThemeVariant.DARK_LUMINANCE
        val text = ThemeVariant.textColor(dark)
        val extreme = if (dark) 0xFF000000L else 0xFFFFFFFFL
        var bg = bg0
        var card = opaque(surface)
        for (step in 0..SANITIZE_STEPS) {
            bg = ColorMath.blend(bg0, extreme, step / SANITIZE_STEPS.toFloat())
            card = ColorMath.blend(opaque(surface), extreme, step / SANITIZE_STEPS.toFloat())
            val worst = ThemeVariant.solidSurfaces(bg, card, text, dark).minOf { Contrast.ratio(text, it) }
            // A little headroom so rounding in later blends can't drop below the limit.
            if (worst >= Contrast.AA_NORMAL + 0.1) break
        }
        return BrandColors(opaque(primary), opaque(secondary), opaque(accent), bg, card, opaque(glow), opaque(glowAlt))
    }

    companion object {
        private const val SANITIZE_STEPS = 50

        fun fromList(v: List<Long>): BrandColors? =
            if (v.size == 7) BrandColors(v[0], v[1], v[2], v[3], v[4], v[5], v[6]) else null
    }
}

/** Full set of Material 3 roles the app uses, plus the decorative tones. */
data class Palette(
    val dark: Boolean,
    val primary: Long, val onPrimary: Long,
    val primaryContainer: Long, val onPrimaryContainer: Long,
    val secondary: Long, val onSecondary: Long,
    val secondaryContainer: Long, val onSecondaryContainer: Long,
    val tertiary: Long, val onTertiary: Long,
    val tertiaryContainer: Long, val onTertiaryContainer: Long,
    val background: Long, val onBackground: Long,
    val surface: Long, val onSurface: Long,
    val surfaceVariant: Long, val onSurfaceVariant: Long,
    val surfaceContainerLowest: Long, val surfaceContainerLow: Long,
    val surfaceContainer: Long, val surfaceContainerHigh: Long,
    val surfaceContainerHighest: Long,
    val outline: Long, val outlineVariant: Long,
    val error: Long, val onError: Long,
    val errorContainer: Long, val onErrorContainer: Long,
    val glow: Long, val glowAlt: Long,
    /** Bright metal tone for bevelled card edges. */
    val chrome: Long,
    /** Focus ring: the glow, adjusted to stay visible (3:1) on every background. */
    val focusRing: Long,
) {
    /** Opaque surfaces on which normal-size text appears. */
    val solidSurfaces: List<Long>
        get() = listOf(
            background, surface, surfaceVariant, surfaceContainerLowest, surfaceContainerLow,
            surfaceContainer, surfaceContainerHigh, surfaceContainerHighest,
        )
}

/**
 * One version (dark or light) of a theme. [glassAlpha] is the card opacity over the
 * backdrop when glass cards are on; themes without a backdrop have solid cards.
 */
data class ThemeVariant(
    val colors: BrandColors,
    val backdrop: Backdrop? = null,
    val glassAlpha: Float = 1f,
) {
    val dark: Boolean get() = Contrast.luminance(colors.background) < DARK_LUMINANCE

    /**
     * Strongest opacity the orbs may have while text and accents still read at WCAG AA on
     * them and on glass cards over them. The design value is used when it already passes.
     */
    val orbAlphaLimit: Float by lazy {
        val b = backdrop ?: return@lazy 0f
        val text = textColor(dark)
        val card = cardColor()
        var alpha = if (dark) ORB_ALPHA_DARK else ORB_ALPHA_LIGHT
        while (alpha > 0f) {
            val lit = backdropSurfaces(b, alpha, card)
            // The accents may be nudged toward the text colour to stay readable, but only so
            // far (they must still look like the theme); beyond that the orbs are dimmed.
            fun accentOk(c: Long) =
                lit.all { Contrast.ratio(ColorMath.blend(c or 0xFF000000L, text, MAX_ACCENT_SHIFT), it) >= Contrast.AA_NORMAL }
            val ok = lit.all { Contrast.ratio(text, it) >= Contrast.AA_NORMAL } &&
                accentOk(colors.primary) && accentOk(colors.secondary)
            if (ok) break
            alpha = Math.round((alpha - 0.05f) * 100f) / 100f
        }
        alpha.coerceAtLeast(0f)
    }

    private fun cardColor(): Long = colors.surface or 0xFF000000L

    /** Everything text can sit on because of the backdrop: bare, orb-lit, and under glass cards. */
    private fun backdropSurfaces(b: Backdrop, orbAlpha: Float, card: Long): List<Long> {
        val bare = mutableListOf(b.top, b.bottom)
        b.orbs.forEach { o ->
            bare += ColorMath.over(o.color, orbAlpha, b.top)
            bare += ColorMath.over(o.color, orbAlpha, b.bottom)
        }
        return bare + bare.map { ColorMath.over(card, glassAlpha, it) }
    }

    /** All surfaces text is checked against (solid ones plus the backdrop-derived ones). */
    fun textSurfaces(p: Palette): List<Long> =
        p.solidSurfaces + (backdrop?.let { backdropSurfaces(it, orbAlphaLimit, p.surfaceContainer) } ?: emptyList())

    val palette: Palette by lazy { build() }

    private fun build(): Palette {
        val c = colors
        val d = dark
        val text = textColor(d)
        val black = 0xFF000000L
        val white = 0xFFFFFFFFL
        val bg = c.background or 0xFF000000L
        val surface = cardColor()
        val solid = solidSurfaces(bg, surface, text, d)
        val variant = solid[2]
        val lowest = solid[3]
        val low = solid[4]
        val high = solid[6]
        val highest = solid[7]
        val surfaces = solid + (backdrop?.let { backdropSurfaces(it, orbAlphaLimit, surface) } ?: emptyList())

        fun readable(colour: Long) = Contrast.ensure(colour, surfaces, Contrast.AA_NORMAL, toward = text)
        fun on(fill: Long) = Contrast.bestOf(fill, listOf(black, white))
        val primary = readable(c.primary)
        val secondary = readable(c.secondary)
        val pc = ColorMath.blend(bg, primary, 0.28f)
        val sc = ColorMath.blend(bg, secondary, 0.18f)
        val tc = ColorMath.blend(bg, c.accent, 0.2f)
        val err = if (d) {
            listOf(0xFFFFB4ABL, 0xFF690005L, 0xFF5C1A1AL, 0xFFFFDAD6L)
        } else {
            listOf(0xFFBA1A1AL, 0xFFFFFFFFL, 0xFFFFDAD6L, 0xFF410002L)
        }
        val ringOn = listOf(bg, surface) + (backdrop?.let { listOf(it.top, it.bottom) } ?: emptyList())
        return Palette(
            dark = d,
            primary = primary, onPrimary = on(primary),
            primaryContainer = pc, onPrimaryContainer = on(pc),
            secondary = secondary, onSecondary = on(secondary),
            secondaryContainer = sc, onSecondaryContainer = on(sc),
            tertiary = c.accent or 0xFF000000L, onTertiary = on(c.accent),
            tertiaryContainer = tc, onTertiaryContainer = on(tc),
            background = bg, onBackground = text,
            surface = surface, onSurface = text,
            surfaceVariant = variant,
            onSurfaceVariant = readable(ColorMath.over(text, 0.72f, variant)),
            surfaceContainerLowest = lowest, surfaceContainerLow = low,
            surfaceContainer = surface, surfaceContainerHigh = high,
            surfaceContainerHighest = highest,
            outline = ColorMath.over(text, 0.28f, bg),
            outlineVariant = ColorMath.over(text, 0.12f, bg),
            error = readable(err[0]), onError = on(readable(err[0])),
            errorContainer = err[2], onErrorContainer = err[3],
            glow = c.glow or 0xFF000000L, glowAlt = c.glowAlt or 0xFF000000L,
            chrome = if (d) 0xFFD4D8E4L else 0xFF2A2840L,
            focusRing = Contrast.ensure(c.glow, ringOn, Contrast.AA_LARGE_OR_UI, toward = text),
        )
    }

    companion object {
        const val DARK_LUMINANCE = 0.4
        const val ORB_ALPHA_DARK = 0.55f
        const val ORB_ALPHA_LIGHT = 0.7f
        const val MAX_ACCENT_SHIFT = 0.3f

        /**
         * The opaque surface levels derived from a background and a card colour, in the order
         * background, surface, variant, lowest, low, container, high, highest.
         */
        fun solidSurfaces(bg: Long, surface: Long, text: Long, dark: Boolean): List<Long> = listOf(
            bg, surface,
            ColorMath.blend(surface, text, if (dark) 0.08f else 0.05f),
            ColorMath.blend(bg, surface, 0.4f),
            ColorMath.blend(bg, surface, 0.7f),
            surface,
            ColorMath.blend(surface, text, 0.05f),
            ColorMath.blend(surface, text, 0.09f),
        )

        /** Text colour on a theme's surfaces: near-white on dark, near-black on light. */
        fun textColor(dark: Boolean): Long = if (dark) 0xFFF4F3FBL else 0xFF15122AL
    }
}

/**
 * A theme: a dark and a light variant sharing one identity. Typography, shapes and
 * motion are common to all themes (as in AceArcade) and adjustable by the user.
 */
data class ThemeSpec(
    val id: String,
    val name: String,
    val tagline: String,
    /** Glass themes: translucent cards over a coloured backdrop. */
    val premium: Boolean,
    val dark: ThemeVariant,
    val light: ThemeVariant,
) {
    fun variant(dark: Boolean): ThemeVariant = if (dark) this.dark else light
}

object ColorMath {
    fun a(c: Long) = ((c shr 24) and 0xFF).toInt()
    fun r(c: Long) = ((c shr 16) and 0xFF).toInt()
    fun g(c: Long) = ((c shr 8) and 0xFF).toInt()
    fun b(c: Long) = (c and 0xFF).toInt()

    fun argb(a: Int, r: Int, g: Int, b: Int): Long =
        ((a.toLong() and 0xFF) shl 24) or ((r.toLong() and 0xFF) shl 16) or
            ((g.toLong() and 0xFF) shl 8) or (b.toLong() and 0xFF)

    /** Linear mix in sRGB: t = 0 gives [from], t = 1 gives [to]. Result is opaque. */
    fun blend(from: Long, to: Long, t: Float): Long {
        fun mix(x: Int, y: Int) = Math.round(x + (y - x) * t)
        return argb(255, mix(r(from), r(to)), mix(g(from), g(to)), mix(b(from), b(to)))
    }

    /** [top] drawn with [alpha] over opaque [bottom]. */
    fun over(top: Long, alpha: Float, bottom: Long): Long = blend(bottom, top, alpha)

    fun withAlpha(c: Long, alpha: Float): Long =
        (c and 0x00FFFFFFL) or ((Math.round(alpha * 255).toLong() and 0xFF) shl 24)
}

/** WCAG 2.2 relative luminance and contrast ratio. */
object Contrast {
    const val AA_NORMAL = 4.5
    const val AA_LARGE_OR_UI = 3.0

    private fun channel(v: Int): Double {
        val s = v / 255.0
        return if (s <= 0.04045) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
    }

    fun luminance(c: Long): Double =
        0.2126 * channel(ColorMath.r(c)) + 0.7152 * channel(ColorMath.g(c)) +
            0.0722 * channel(ColorMath.b(c))

    fun ratio(fg: Long, bg: Long): Double {
        val l1 = luminance(fg)
        val l2 = luminance(bg)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    fun bestOf(bg: Long, candidates: List<Long>): Long = candidates.maxBy { ratio(it, bg) }

    /**
     * [colour] moved toward [toward] in small steps until it reaches [target] contrast on
     * every one of [surfaces]. Returned unchanged when it already does; returns [toward]
     * itself if nothing in between is enough.
     */
    fun ensure(colour: Long, surfaces: List<Long>, target: Double, toward: Long): Long {
        val opaque = colour or 0xFF000000L
        fun ok(c: Long) = surfaces.all { ratio(c, it) >= target }
        if (ok(opaque)) return opaque
        for (step in 1..50) {
            val c = ColorMath.blend(opaque, toward, step / 50f)
            if (ok(c)) return c
        }
        return toward or 0xFF000000L
    }
}
