package com.acegears.ace360x.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Theme background for non-game screens: the theme's backdrop (a vertical gradient with
 * soft light orbs) or its plain background colour. Static — nothing here animates, so it
 * costs one draw per frame of whatever is on top. Gameplay runs in a separate process and
 * activity, so this is never drawn during a game (CLAUDE.md §9).
 */
@Composable
fun AmbianceBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val theme = LocalAceTheme.current
    Box(modifier.fillMaxSize().aceBackdrop(theme)) { content() }
}

/** Draws [theme]'s backdrop behind the content. */
fun Modifier.aceBackdrop(theme: ResolvedTheme): Modifier = drawBehind {
    val backdrop = theme.backdrop
    if (backdrop == null) {
        drawRect(theme.palette.background.toColor())
        return@drawBehind
    }
    drawRect(Brush.verticalGradient(listOf(backdrop.top.toColor(), backdrop.bottom.toColor())))
    val span = maxOf(size.width, size.height)
    backdrop.orbs.forEach { orb ->
        val centre = Offset(orb.x * size.width, orb.y * size.height)
        val radius = orb.radius * span
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(orb.color.toColor().copy(alpha = theme.orbAlpha), Color.Transparent),
                center = centre,
                radius = radius,
            ),
            radius = radius,
            center = centre,
        )
    }
}
