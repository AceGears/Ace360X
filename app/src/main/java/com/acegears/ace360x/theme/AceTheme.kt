package com.acegears.ace360x.theme

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalAceTheme = staticCompositionLocalOf { ResolvedTheme.resolve(ThemeCustomization()) }

/** Effective motion tokens: scaled by the user's animation speed, or [MotionTokens.REDUCED]. */
val LocalMotion = staticCompositionLocalOf { MotionTokens.DEFAULT }

fun Long.toColor(): Color = Color(this.toInt())

/** True when the user turned animations off system-wide (developer options / a11y). */
fun systemAnimationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

fun EasingKind.toEasing(): Easing = when (this) {
    EasingKind.STANDARD -> FastOutSlowInEasing
    EasingKind.EMPHASIZED -> CubicBezierEasing(0.2f, 0f, 0f, 1f)
    EasingKind.DECELERATE -> LinearOutSlowInEasing
    EasingKind.LINEAR -> LinearEasing
}

private fun Palette.toColorScheme(): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary.toColor(), onPrimary = onPrimary.toColor(),
        primaryContainer = primaryContainer.toColor(), onPrimaryContainer = onPrimaryContainer.toColor(),
        secondary = secondary.toColor(), onSecondary = onSecondary.toColor(),
        secondaryContainer = secondaryContainer.toColor(), onSecondaryContainer = onSecondaryContainer.toColor(),
        tertiary = tertiary.toColor(), onTertiary = onTertiary.toColor(),
        tertiaryContainer = tertiaryContainer.toColor(), onTertiaryContainer = onTertiaryContainer.toColor(),
        background = background.toColor(), onBackground = onBackground.toColor(),
        surface = surface.toColor(), onSurface = onSurface.toColor(),
        surfaceVariant = surfaceVariant.toColor(), onSurfaceVariant = onSurfaceVariant.toColor(),
        surfaceContainerLowest = surfaceContainerLowest.toColor(),
        surfaceContainerLow = surfaceContainerLow.toColor(),
        surfaceContainer = surfaceContainer.toColor(),
        surfaceContainerHigh = surfaceContainerHigh.toColor(),
        surfaceContainerHighest = surfaceContainerHighest.toColor(),
        surfaceBright = surfaceContainerHighest.toColor(),
        surfaceDim = background.toColor(),
        outline = outline.toColor(), outlineVariant = outlineVariant.toColor(),
        error = error.toColor(), onError = onError.toColor(),
        errorContainer = errorContainer.toColor(), onErrorContainer = onErrorContainer.toColor(),
        inverseSurface = onSurface.toColor(), inverseOnSurface = background.toColor(),
    )
}

/** Heavy, tight sans-serif headings and widely tracked labels (the AceArcade look). */
private fun aceTypography(): Typography {
    val base = Typography()
    fun TextStyle.set(weight: FontWeight, tracking: Float = 0f) =
        copy(fontFamily = FontFamily.SansSerif, fontWeight = weight, letterSpacing = tracking.sp)
    return base.copy(
        displaySmall = base.displaySmall.set(FontWeight.Black, -0.5f),
        headlineLarge = base.headlineLarge.set(FontWeight.ExtraBold, -0.3f),
        headlineMedium = base.headlineMedium.set(FontWeight.ExtraBold, -0.2f),
        headlineSmall = base.headlineSmall.set(FontWeight.Bold),
        titleLarge = base.titleLarge.set(FontWeight.Bold),
        titleMedium = base.titleMedium.set(FontWeight.SemiBold, 0.1f),
        titleSmall = base.titleSmall.set(FontWeight.SemiBold, 0.1f),
        labelLarge = base.labelLarge.set(FontWeight.SemiBold, 0.4f),
        labelMedium = base.labelMedium.set(FontWeight.SemiBold, 0.5f),
        labelSmall = base.labelSmall.set(FontWeight.Bold, 0.8f),
    )
}

/** Material shapes from the one corner radius the user can adjust. */
private fun shapesFor(cornerDp: Int): Shapes {
    val c = cornerDp.toFloat()
    return Shapes(
        extraSmall = RoundedCornerShape((c * 0.35f).dp),
        small = RoundedCornerShape((c * 0.6f).dp),
        medium = RoundedCornerShape(c.dp),
        large = RoundedCornerShape((c * 1.4f).dp),
        extraLarge = RoundedCornerShape((c * 1.8f).dp),
    )
}

/** Applies a customised theme: Material colours, type, shapes, text size plus Ace-specific locals. */
@Composable
fun AceTheme(customization: ThemeCustomization, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val wantsDark = ResolvedTheme.wantsDark(customization, systemDark)
    val dynamic = if (customization.baseId == Themes.DYNAMIC.id) dynamicBrandColors(context, wantsDark) else null
    val resolved = remember(customization, systemDark, dynamic) {
        ResolvedTheme.resolve(customization, systemDark, dynamic)
    }
    val scheme = remember(resolved.palette) { resolved.palette.toColorScheme() }
    val typography = remember { aceTypography() }
    val shapes = remember(resolved.cornerDp) { shapesFor(resolved.cornerDp) }
    // Read once per theme change; a system toggle applies on the next activity start.
    val systemOff = remember(context) { systemAnimationsOff(context) }
    val motion = if (resolved.reduceMotionByUser || systemOff) {
        MotionTokens.REDUCED
    } else {
        MotionTokens.DEFAULT.scaled(resolved.durationScale)
    }
    val density = LocalDensity.current
    val scaled = remember(density, resolved.textScale) {
        Density(density.density, density.fontScale * resolved.textScale)
    }
    CompositionLocalProvider(
        LocalAceTheme provides resolved,
        LocalMotion provides motion,
        LocalDensity provides scaled,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
    }
}

/** True where Android offers wallpaper-based colours (Android 12+). */
val dynamicColorsAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

/** Android's dynamic colours as the seven brand colours, or null before Android 12. */
fun dynamicBrandColors(context: Context, dark: Boolean): BrandColors? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val s = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    return BrandColors(
        primary = s.primary.toArgbLong(),
        secondary = s.secondary.toArgbLong(),
        accent = s.tertiary.toArgbLong(),
        background = s.background.toArgbLong(),
        surface = s.surfaceContainer.toArgbLong(),
        glow = s.primary.toArgbLong(),
        glowAlt = s.tertiary.toArgbLong(),
    )
}

/** The app's icons (rounded set). Toggle-state icons (favourite star) stay as chosen. */
object AceIcons {
    val Library: ImageVector get() = Icons.AutoMirrored.Rounded.List
    val Device: ImageVector get() = Icons.Rounded.Build
    val Settings: ImageVector get() = Icons.Rounded.Settings
    val Info: ImageVector get() = Icons.Rounded.Info
    val Add: ImageVector get() = Icons.Rounded.Add
    val Close: ImageVector get() = Icons.Rounded.Close
    val Refresh: ImageVector get() = Icons.Rounded.Refresh
    val Search: ImageVector get() = Icons.Rounded.Search
    val Back: ImageVector get() = Icons.AutoMirrored.Rounded.ArrowBack
    val Forward: ImageVector get() = Icons.AutoMirrored.Rounded.KeyboardArrowRight
    val More: ImageVector get() = Icons.Rounded.MoreVert
    val Play: ImageVector get() = Icons.Rounded.PlayArrow
}
