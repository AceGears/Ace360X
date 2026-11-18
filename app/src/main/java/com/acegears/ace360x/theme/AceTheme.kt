package com.acegears.ace360x.theme

import android.content.Context
import android.graphics.Typeface
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.sharp.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.Build
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalAceTheme = staticCompositionLocalOf { ResolvedTheme.resolve(ThemeCustomization()) }

/** Effective motion tokens: the theme's, or [MotionTokens.REDUCED] when motion is off. */
val LocalMotion = staticCompositionLocalOf { Themes.DEFAULT.motion }

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

internal fun FontKind.toFamily(): FontFamily = when (this) {
    FontKind.DEFAULT -> FontFamily.Default
    FontKind.SERIF -> FontFamily.Serif
    FontKind.MONOSPACE -> FontFamily.Monospace
    FontKind.CONDENSED -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
    FontKind.LIGHT -> FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL))
    FontKind.MEDIUM -> FontFamily(Typeface.create("sans-serif-medium", Typeface.NORMAL))
    FontKind.BLACK -> FontFamily(Typeface.create("sans-serif-black", Typeface.NORMAL))
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
    )
}

private fun TypeTokens.toTypography(): Typography {
    val base = Typography()
    val heading = headingFont.toFamily()
    val body = bodyFont.toFamily()
    fun TextStyle.h() = copy(fontFamily = heading, letterSpacing = (letterSpacing.value + headingLetterSpacing).sp)
    fun TextStyle.b() = copy(fontFamily = body)
    return base.copy(
        displayLarge = base.displayLarge.h(), displayMedium = base.displayMedium.h(),
        displaySmall = base.displaySmall.h(), headlineLarge = base.headlineLarge.h(),
        headlineMedium = base.headlineMedium.h(), headlineSmall = base.headlineSmall.h(),
        titleLarge = base.titleLarge.h(), titleMedium = base.titleMedium.h(),
        titleSmall = base.titleSmall.h(),
        bodyLarge = base.bodyLarge.b(), bodyMedium = base.bodyMedium.b(), bodySmall = base.bodySmall.b(),
        labelLarge = base.labelLarge.b(), labelMedium = base.labelMedium.b(), labelSmall = base.labelSmall.b(),
    )
}

fun ShapeTokens.corner(sizeDp: Float): CornerBasedShape = when (kind) {
    CornerKind.ROUNDED -> RoundedCornerShape(sizeDp.dp)
    CornerKind.CUT -> CutCornerShape(sizeDp.dp)
}

private fun ShapeTokens.toShapes(): Shapes = Shapes(
    extraSmall = corner(smallDp / 2),
    small = corner(smallDp),
    medium = corner(mediumDp),
    large = corner(largeDp),
    extraLarge = corner(largeDp * 1.3f),
)

/** Applies a customised theme: Material colours, type, shapes plus Ace-specific locals. */
@Composable
fun AceTheme(customization: ThemeCustomization, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val resolved = remember(customization) { ResolvedTheme.resolve(customization) }
    val scheme = remember(resolved.palette) { resolved.palette.toColorScheme() }
    val typography = remember(resolved.spec.type) { resolved.spec.type.toTypography() }
    val shapes = remember(resolved.shapes) { resolved.shapes.toShapes() }
    // Read once per theme change; a system toggle applies on the next activity start.
    val systemOff = remember(context) { systemAnimationsOff(context) }
    val motionOff = resolved.reduceMotionByUser || systemOff
    val motion = if (motionOff) MotionTokens.REDUCED else resolved.spec.motion
    CompositionLocalProvider(LocalAceTheme provides resolved, LocalMotion provides motion) {
        MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
    }
}

/** Icons in the active theme's icon style. Toggle-state icons (favourite star) stay as chosen. */
object AceIcons {
    private val style: IconStyle
        @Composable @ReadOnlyComposable get() = LocalAceTheme.current.spec.icons

    private fun pick(s: IconStyle, f: ImageVector, o: ImageVector, r: ImageVector, sh: ImageVector) =
        when (s) {
            IconStyle.FILLED -> f
            IconStyle.OUTLINED -> o
            IconStyle.ROUNDED -> r
            IconStyle.SHARP -> sh
        }

    val Library: ImageVector
        @Composable @ReadOnlyComposable get() = pick(
            style, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List,
            Icons.AutoMirrored.Rounded.List, Icons.AutoMirrored.Sharp.List,
        )
    val Device: ImageVector
        @Composable @ReadOnlyComposable get() =
            pick(style, Icons.Filled.Build, Icons.Outlined.Build, Icons.Rounded.Build, Icons.Sharp.Build)
    val Settings: ImageVector
        @Composable @ReadOnlyComposable get() =
            pick(style, Icons.Filled.Settings, Icons.Outlined.Settings, Icons.Rounded.Settings, Icons.Sharp.Settings)
    val Info: ImageVector
        @Composable @ReadOnlyComposable get() =
            pick(style, Icons.Filled.Info, Icons.Outlined.Info, Icons.Rounded.Info, Icons.Sharp.Info)
    val Add: ImageVector
        @Composable @ReadOnlyComposable get() =
            pick(style, Icons.Filled.Add, Icons.Outlined.Add, Icons.Rounded.Add, Icons.Sharp.Add)
    val Close: ImageVector
        @Composable @ReadOnlyComposable get() =
            pick(style, Icons.Filled.Close, Icons.Outlined.Close, Icons.Rounded.Close, Icons.Sharp.Close)
    val Refresh: ImageVector
        @Composable @ReadOnlyComposable get() =
            pick(style, Icons.Filled.Refresh, Icons.Outlined.Refresh, Icons.Rounded.Refresh, Icons.Sharp.Refresh)
    val Search: ImageVector
        @Composable @ReadOnlyComposable get() =
            pick(style, Icons.Filled.Search, Icons.Outlined.Search, Icons.Rounded.Search, Icons.Sharp.Search)
}
