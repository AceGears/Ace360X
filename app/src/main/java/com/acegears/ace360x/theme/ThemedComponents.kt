package com.acegears.ace360x.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/*
 * Component overrides (CLAUDE.md §10): every card, toggle, slider, progress bar,
 * dialog, nav item and focus ring in the app goes through these so a theme
 * changes their shape, border, elevation and colour roles, not just colour.
 */

/**
 * Themed card. [containerColor] marks a semantic card (error, notice): it is drawn
 * opaque with its own colour, keeping the theme's shape and border style.
 */
@Composable
fun AceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = LocalAceTheme.current
    val e = t.spec.elevation
    val cs = MaterialTheme.colorScheme
    val container = containerColor ?: cs.surfaceContainerHighest.copy(alpha = e.cardAlpha)
    val onContainer = contentColor ?: cs.onSurface
    val border = when (e.card) {
        CardStyle.TONAL -> null
        CardStyle.OUTLINED -> BorderStroke(e.borderDp.dp, cs.outline)
        CardStyle.GLASS -> BorderStroke(e.borderDp.dp, cs.surface.copy(alpha = 0.9f))
        CardStyle.GLOW -> BorderStroke(
            e.borderDp.dp,
            Brush.linearGradient(listOf(cs.primary.copy(alpha = 0.7f), cs.secondary.copy(alpha = 0.35f))),
        )
    }
    val colors = CardDefaults.cardColors(
        containerColor = container,
        contentColor = onContainer,
        disabledContainerColor = container,
        disabledContentColor = onContainer.copy(alpha = 0.6f),
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = e.cardElevationDp.dp)
    val shape = MaterialTheme.shapes.medium
    if (onClick != null) {
        Card(
            onClick = onClick, modifier = modifier.aceFocusRing(), enabled = enabled, shape = shape,
            colors = colors, elevation = elevation, border = border, content = content,
        )
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation, border = border, content = content)
    }
}

@Composable
fun AceSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val t = LocalAceTheme.current
    val cs = MaterialTheme.colorScheme
    val on = if (t.spec.components.switchUsesTertiary) cs.tertiary else cs.primary
    val onContent = if (t.spec.components.switchUsesTertiary) cs.onTertiary else cs.onPrimary
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.aceFocusRing(),
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = on,
            checkedThumbColor = onContent,
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
    val t = LocalAceTheme.current
    val cs = MaterialTheme.colorScheme
    val active = if (t.spec.components.sliderUsesSecondary) cs.secondary else cs.primary
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.aceFocusRing(),
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = active,
            activeTrackColor = active,
            inactiveTrackColor = cs.surfaceContainerHighest,
            activeTickColor = cs.surface,
            inactiveTickColor = cs.outline,
        ),
    )
}

@Composable
fun AceLinearProgress(modifier: Modifier = Modifier, progress: (() -> Float)? = null) {
    val t = LocalAceTheme.current
    val cs = MaterialTheme.colorScheme
    val cap = if (t.spec.shapes.kind == CornerKind.ROUNDED && t.shapes.smallDp > 0f) StrokeCap.Round else StrokeCap.Butt
    val stroke = t.spec.components.progressStrokeDp.dp
    if (progress != null) {
        LinearProgressIndicator(
            progress = progress, modifier = modifier.height(stroke), color = cs.primary,
            trackColor = cs.surfaceContainerHighest, strokeCap = cap,
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier.height(stroke), color = cs.primary,
            trackColor = cs.surfaceContainerHighest, strokeCap = cap,
        )
    }
}

@Composable
fun AceCircularProgress(modifier: Modifier = Modifier) {
    val t = LocalAceTheme.current
    CircularProgressIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = t.spec.components.progressStrokeDp.coerceAtLeast(2f).dp,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}

@Composable
fun aceNavItemColors(): NavigationBarItemColors {
    val t = LocalAceTheme.current
    val cs = MaterialTheme.colorScheme
    return if (t.spec.components.navIndicatorFromPrimary) {
        NavigationBarItemDefaults.colors(
            selectedIconColor = cs.onPrimaryContainer,
            selectedTextColor = cs.onSurface,
            indicatorColor = cs.primaryContainer,
            unselectedIconColor = cs.onSurfaceVariant,
            unselectedTextColor = cs.onSurfaceVariant,
        )
    } else {
        // No pill: the selected item is marked by its primary-coloured icon and label.
        NavigationBarItemDefaults.colors(
            selectedIconColor = cs.primary,
            selectedTextColor = cs.primary,
            indicatorColor = cs.surfaceContainer,
            unselectedIconColor = cs.onSurfaceVariant,
            unselectedTextColor = cs.onSurfaceVariant,
        )
    }
}

/** Themed AlertDialog: theme shape and container; content slots as in Material. */
@Composable
fun AceDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    val t = LocalAceTheme.current
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
        tonalElevation = t.spec.elevation.cardElevationDp.dp,
    )
}

/**
 * Visible focus ring for keyboard/controller navigation, in the theme's ring
 * colour and width (checked for 3:1 contrast in ThemeChecks).
 */
fun Modifier.aceFocusRing(): Modifier = composed {
    val t = LocalAceTheme.current
    var focused by remember { mutableStateOf(false) }
    val ring = t.spec.components
    this
        .onFocusChanged { focused = it.isFocused }
        .then(
            if (focused) {
                Modifier.border(ring.focusRingDp.dp, ring.focusRingColor.toColor(), MaterialTheme.shapes.small)
            } else {
                Modifier
            },
        )
}
