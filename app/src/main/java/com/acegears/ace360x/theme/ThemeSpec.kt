package com.acegears.ace360x.theme

/*
 * Theme token model. Plain Kotlin (ARGB Longs, enums) so contrast and
 * customisation logic is unit-testable on the JVM. Compose mapping lives in
 * AceTheme.kt. See docs/research/theme-engine.md.
 */

/** Font families available on every Android device (no bundled fonts). */
enum class FontKind { DEFAULT, CONDENSED, LIGHT, MEDIUM, BLACK, SERIF, MONOSPACE }

enum class CornerKind { ROUNDED, CUT }

enum class EasingKind { STANDARD, EMPHASIZED, DECELERATE, LINEAR }

enum class IconStyle { FILLED, OUTLINED, ROUNDED, SHARP }

enum class AmbianceKind {
    NONE, VIGNETTE_GLINT, SYNTH_GRID, REACTOR_CORE, FROST, AURORA, BRUSHED_METAL,
    DIGITAL_RAIN, CORONA, STARFIELD, SCANLINES, CHROME_SWEEP,
}

enum class CardStyle { TONAL, OUTLINED, GLASS, GLOW }

data class TypeTokens(
    val headingFont: FontKind,
    val bodyFont: FontKind,
    /** Extra tracking for titles/headlines, in sp. */
    val headingLetterSpacing: Float = 0f,
    val uppercaseLabels: Boolean = false,
)

data class ShapeTokens(
    val kind: CornerKind,
    val smallDp: Float,
    val mediumDp: Float,
    val largeDp: Float,
) {
    fun scaled(factor: Float): ShapeTokens =
        copy(smallDp = smallDp * factor, mediumDp = mediumDp * factor, largeDp = largeDp * factor)
}

data class ElevationTokens(
    val card: CardStyle,
    val cardElevationDp: Float,
    val borderDp: Float,
    /** Opacity of the card container (GLASS only; otherwise 1). */
    val cardAlpha: Float = 1f,
)

/** Central motion tokens (CLAUDE.md §9 `MotionTokens`). Durations in ms. */
data class MotionTokens(
    val shortMs: Int,
    val mediumMs: Int,
    val longMs: Int,
    val easing: EasingKind,
    /** One loop of the ambiance animation; 0 = static ambiance. */
    val ambianceLoopMs: Int,
) {
    fun scaled(factor: Float): MotionTokens = copy(
        shortMs = (shortMs * factor).toInt(),
        mediumMs = (mediumMs * factor).toInt(),
        longMs = (longMs * factor).toInt(),
    )

    companion object {
        /** Used when animations are off system-wide or by the user. */
        val REDUCED = MotionTokens(0, 0, 0, EasingKind.LINEAR, 0)
    }
}

data class ComponentTokens(
    val navIndicatorFromPrimary: Boolean,
    val progressStrokeDp: Float,
    val focusRingDp: Float,
    val focusRingColor: Long,
    val switchUsesTertiary: Boolean = false,
    val sliderUsesSecondary: Boolean = false,
)

data class AmbianceTokens(
    val kind: AmbianceKind,
    /** Colours the renderer draws with; the strongest is used in contrast checks. */
    val colors: List<Long>,
    /** Peak opacity of ambiance strokes/fills. */
    val intensity: Float,
)

/** The few hand-picked colours of a theme; all other roles are derived. */
data class PaletteSeed(
    val dark: Boolean,
    val background: Long,
    val surface: Long,
    val surfaceVariant: Long,
    val primary: Long,
    val secondary: Long,
    val tertiary: Long,
    val text: Long,
    val mutedText: Long,
    val outline: Long,
)

/** Full set of Material 3 roles the app uses. */
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
) {
    /** Surfaces on which normal-size body/label text appears. */
    val textSurfaces: List<Long>
        get() = listOf(
            background, surface, surfaceVariant, surfaceContainerLowest, surfaceContainerLow,
            surfaceContainer, surfaceContainerHigh, surfaceContainerHighest,
        )

    companion object {
        fun from(seed: PaletteSeed, accentOverride: Long? = null): Palette {
            val primary = accentOverride ?: seed.primary
            val s = seed
            val black = 0xFF000000L
            val white = 0xFFFFFFFFL
            val onCandidates = listOf(s.text, s.background, black, white)
            fun on(bg: Long) = Contrast.bestOf(bg, onCandidates)
            fun container(c: Long) = ColorMath.blend(s.surface, c, if (s.dark) 0.24f else 0.16f)
            val pc = container(primary)
            val sc = container(s.secondary)
            val tc = container(s.tertiary)
            val towardText = if (s.dark) 0.05f else 0.035f
            val lowest = if (s.dark) ColorMath.blend(s.background, black, 0.25f) else white
            val low = ColorMath.blend(s.surface, s.surfaceVariant, 0.3f)
            val mid = ColorMath.blend(s.surface, s.surfaceVariant, 0.6f)
            val high = s.surfaceVariant
            val highest = ColorMath.blend(s.surfaceVariant, s.text, towardText)
            val err = if (s.dark) {
                listOf(0xFFFFB4ABL, 0xFF690005L, 0xFF5C1A1AL, 0xFFFFDAD6L)
            } else {
                listOf(0xFFBA1A1AL, 0xFFFFFFFFL, 0xFFFFDAD6L, 0xFF410002L)
            }
            return Palette(
                dark = s.dark,
                primary = primary, onPrimary = on(primary),
                primaryContainer = pc, onPrimaryContainer = on(pc),
                secondary = s.secondary, onSecondary = on(s.secondary),
                secondaryContainer = sc, onSecondaryContainer = on(sc),
                tertiary = s.tertiary, onTertiary = on(s.tertiary),
                tertiaryContainer = tc, onTertiaryContainer = on(tc),
                background = s.background, onBackground = s.text,
                surface = s.surface, onSurface = s.text,
                surfaceVariant = s.surfaceVariant, onSurfaceVariant = s.mutedText,
                surfaceContainerLowest = lowest, surfaceContainerLow = low,
                surfaceContainer = mid, surfaceContainerHigh = high,
                surfaceContainerHighest = highest,
                outline = s.outline,
                outlineVariant = ColorMath.blend(s.surfaceVariant, s.outline, 0.5f),
                error = err[0], onError = err[1], errorContainer = err[2], onErrorContainer = err[3],
            )
        }
    }
}

data class ThemeSpec(
    val id: String,
    val name: String,
    val tagline: String,
    val seed: PaletteSeed,
    val type: TypeTokens,
    val shapes: ShapeTokens,
    val elevation: ElevationTokens,
    val motion: MotionTokens,
    val icons: IconStyle,
    val ambiance: AmbianceTokens,
    val components: ComponentTokens,
) {
    val palette: Palette by lazy { Palette.from(seed) }
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
}
