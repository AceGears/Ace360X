package com.acegears.ace360x.theme

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Shared surfaces and controls in the AceArcade style (CLAUDE.md §10): every card,
 * toggle, slider, progress bar, dialog, nav item and focus ring in the app goes through
 * these. Cards are bevelled plates on solid themes and frosted glass over a backdrop.
 */

/**
 * Soft neon glow behind a rounded element: one blurred rounded rectangle. Draws nothing
 * when the user's glow strength is zero.
 */
fun Modifier.aceGlow(color: Color, intensity: Float, corner: Dp, radius: Dp = 14.dp): Modifier =
    if (intensity <= 0.01f) {
        this
    } else {
        drawBehind {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                this.color = color.copy(alpha = (0.85f * intensity).coerceIn(0f, 1f)).toArgb()
                maskFilter = BlurMaskFilter(radius.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            val r = corner.toPx()
            drawIntoCanvas { it.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint) }
        }
    }

/** Neon wash for headers: the theme's glow melting into its second tone, then the background. */
@Composable
fun aceHeroBrush(): Brush {
    val t = LocalAceTheme.current
    val base = (t.backdrop?.top ?: t.palette.background)
    fun tint(c: Long, alpha: Float) = ColorMath.over(c, alpha * t.gradientIntensity, base).toColor()
    return Brush.verticalGradient(
        0f to tint(t.palette.glow, 0.34f),
        0.38f to tint(t.palette.glowAlt, 0.16f),
        0.75f to base.toColor(),
        1f to base.toColor(),
    )
}

/** Dual-tone neon edge for highlighted cards and dialogs. */
@Composable
fun aceNeonEdge(alpha: Float = 0.85f): Brush {
    val p = LocalAceTheme.current.palette
    return Brush.linearGradient(
        listOf(p.glow.toColor().copy(alpha = alpha), p.glowAlt.toColor().copy(alpha = alpha * 0.8f)),
    )
}

/** Machined-metal edge: a bright highlight at the top-left fading to a faint lower-right edge. */
@Composable
private fun aceBevelEdge(): Brush {
    val p = LocalAceTheme.current.palette
    val chrome = p.chrome.toColor()
    return Brush.linearGradient(
        0f to chrome.copy(alpha = if (p.dark) 0.38f else 0.55f),
        0.45f to p.outlineVariant.toColor(),
        1f to chrome.copy(alpha = if (p.dark) 0.10f else 0.18f),
    )
}

/** Glass edge: light catching the top rim, tinted with the theme's glow at the bottom. */
@Composable
private fun aceGlassEdge(): Brush {
    val p = LocalAceTheme.current.palette
    return Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (p.dark) 0.22f else 0.9f),
        0.5f to Color.White.copy(alpha = if (p.dark) 0.06f else 0.35f),
        1f to p.glow.toColor().copy(alpha = if (p.dark) 0.22f else 0.25f),
    )
}

/** Card colour: translucent over a backdrop when glass cards are on, otherwise solid. */
@Composable
fun aceCardColor(): Color {
    val t = LocalAceTheme.current
    return MaterialTheme.colorScheme.surfaceContainer.copy(alpha = t.glassAlpha)
}

/** Shrinks slightly while pressed, then springs back. Off when motion is reduced. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.96f): Modifier {
    val isPressed by interaction.collectIsPressedAsState()
    val reduced = LocalMotion.current == MotionTokens.REDUCED
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduced) pressed else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "press",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Themed card. [containerColor] marks a semantic card (error, notice): it is drawn
 * opaque with its own colour, keeping the theme's shape and edge. [glow] adds the neon
 * glow behind highlighted cards.
 */
@Composable
fun AceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    glow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = LocalAceTheme.current
    val cs = MaterialTheme.colorScheme
    val glass = t.glassAlpha < 1f
    val container = containerColor ?: aceCardColor()
    val onContainer = contentColor ?: cs.onSurface
    val border = when {
        glass -> BorderStroke(maxOf(t.borderDp, 1).dp, aceGlassEdge())
        t.borderDp > 0 -> BorderStroke(t.borderDp.dp, aceBevelEdge())
        else -> null
    }
    val colors = CardDefaults.cardColors(
        containerColor = container,
        contentColor = onContainer,
        disabledContainerColor = container,
        disabledContentColor = onContainer.copy(alpha = 0.6f),
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val shape = MaterialTheme.shapes.medium
    val glowing = if (glow) {
        Modifier.aceGlow(t.palette.glow.toColor(), t.glowIntensity * 0.6f, t.cornerDp.dp)
    } else {
        Modifier
    }
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Card(
            onClick = onClick,
            modifier = modifier.pressScale(interaction).then(glowing).aceFocusRing(),
            enabled = enabled, shape = shape, colors = colors, elevation = elevation, border = border,
            interactionSource = interaction, content = content,
        )
    } else {
        Card(
            modifier = modifier.then(glowing), shape = shape, colors = colors, elevation = elevation,
            border = border, content = content,
        )
    }
}

/** Section title with an optional action on the right. */
@Composable
fun AceSectionHeader(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun AceSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.aceFocusRing(),
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = cs.primary,
            checkedThumbColor = cs.onPrimary,
            uncheckedBorderColor = cs.outline,
            uncheckedThumbColor = cs.outline,
            uncheckedTrackColor = cs.surfaceContainerHighest,
        ),
    )
}

@Composable
fun AceSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.aceFocusRing(),
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = cs.primary,
            activeTrackColor = cs.primary,
            inactiveTrackColor = cs.surfaceContainerHighest,
            activeTickColor = cs.surface,
            inactiveTickColor = cs.outline,
        ),
    )
}

@Composable
fun AceLinearProgress(modifier: Modifier = Modifier, progress: (() -> Float)? = null) {
    val cs = MaterialTheme.colorScheme
    val cap = if (LocalAceTheme.current.cornerDp > 0) StrokeCap.Round else StrokeCap.Butt
    if (progress != null) {
        LinearProgressIndicator(
            progress = progress, modifier = modifier.height(4.dp), color = cs.primary,
            trackColor = cs.surfaceContainerHighest, strokeCap = cap,
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier.height(4.dp), color = cs.primary,
            trackColor = cs.surfaceContainerHighest, strokeCap = cap,
        )
    }
}

@Composable
fun AceCircularProgress(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 3.dp,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}

@Composable
fun aceNavItemColors(): NavigationBarItemColors {
    val cs = MaterialTheme.colorScheme
    return NavigationBarItemDefaults.colors(
        selectedIconColor = cs.onPrimaryContainer,
        selectedTextColor = cs.onSurface,
        indicatorColor = cs.primaryContainer,
        unselectedIconColor = cs.onSurfaceVariant,
        unselectedTextColor = cs.onSurfaceVariant,
    )
}

/** Themed AlertDialog. Dialogs stay solid (never glass) so they read over anything. */
@Composable
fun AceDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = cs.surfaceContainerHigh,
        titleContentColor = cs.onSurface,
        textContentColor = cs.onSurfaceVariant,
        tonalElevation = 0.dp,
    )
}

/**
 * Visible focus ring for keyboard/controller navigation, in the theme's ring colour
 * (checked for 3:1 contrast in ThemeChecks).
 */
fun Modifier.aceFocusRing(): Modifier = composed {
    val ring = LocalAceTheme.current.palette.focusRing.toColor()
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.border(2.dp, ring, MaterialTheme.shapes.small) else Modifier)
}

/** Top bar of a pushed screen: transparent over the backdrop, with a back arrow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AceTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.aceFocusRing()) {
                    Icon(AceIcons.Back, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

/** Centred message for an empty screen, with an optional action below it. */
@Composable
fun AceEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )
        action?.invoke()
    }
}
