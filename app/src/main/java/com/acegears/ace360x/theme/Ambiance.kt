package com.acegears.ace360x.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Theme background for non-game screens. Gameplay runs in a separate process and
 * activity, so this is never drawn during a game (CLAUDE.md §9). When motion is
 * reduced (system or user) a single static frame is drawn.
 */
@Composable
fun AmbianceBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val theme = LocalAceTheme.current
    val motion = LocalMotion.current
    val bg = theme.palette.background.toColor()
    Box(modifier.fillMaxSize().background(bg)) {
        if (theme.ambianceEnabled) {
            val loop = motion.ambianceLoopMs.coerceAtLeast(0)
            val phase = if (loop > 0) {
                val transition = rememberInfiniteTransition(label = "ambiance")
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(loop, easing = LinearEasing), RepeatMode.Restart),
                    label = "phase",
                ).value
            } else {
                0.25f
            }
            val tokens = theme.spec.ambiance
            val seeds = remember(tokens.kind) { AmbianceSeeds(tokens.kind.ordinal) }
            Canvas(Modifier.fillMaxSize()) { drawAmbiance(tokens, phase, seeds) }
        }
        content()
    }
}

/** Fixed pseudo-random layout per theme so stars/rain don't jump between frames. */
class AmbianceSeeds(seed: Int) {
    private val rnd = Random(seed * 7919 + 17)
    val points: List<Triple<Float, Float, Float>> = List(90) { Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()) }
}

private fun c(argb: Long, alpha: Float) = argb.toColor().copy(alpha = alpha.coerceIn(0f, 1f))

private fun DrawScope.drawAmbiance(t: AmbianceTokens, phase: Float, seeds: AmbianceSeeds) {
    val w = size.width
    val h = size.height
    val k = t.intensity
    val c0 = t.colors.getOrElse(0) { 0xFFFFFFFF }
    val c1 = t.colors.getOrElse(1) { c0 }
    val c2 = t.colors.getOrElse(2) { c1 }
    val wave = (sin(phase * 2 * PI).toFloat() + 1f) / 2f // 0..1 smooth
    when (t.kind) {
        AmbianceKind.NONE -> Unit

        AmbianceKind.VIGNETTE_GLINT -> {
            drawRect(Brush.radialGradient(listOf(Color.Transparent, c(c1, 0.9f)), Offset(w / 2, h / 2), maxOf(w, h) * 0.8f))
            val x = -w * 0.3f + phase * w * 1.6f
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, c(c0, k), Color.Transparent),
                    start = Offset(x - w * 0.15f, 0f), end = Offset(x + w * 0.15f, h * 0.35f),
                ),
            )
        }

        AmbianceKind.SYNTH_GRID -> {
            val horizon = h * 0.62f
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, c(c0, k * 1.2f), Color.Transparent), horizon - h * 0.2f, horizon + h * 0.05f))
            val lines = 10
            for (i in 0..lines) {
                // Horizontal lines rush toward the viewer: spacing grows with depth.
                val p = ((i + phase) / lines)
                val y = horizon + (h - horizon) * p * p
                drawLine(c(c1, k * p), Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
            }
            for (i in -8..8) {
                val xb = w / 2 + i * w / 8f
                drawLine(c(c1, k * 0.8f), Offset(w / 2 + i * w / 40f, horizon), Offset(xb, h), strokeWidth = 1.2f)
            }
        }

        AmbianceKind.REACTOR_CORE -> {
            val r = minOf(w, h) * (0.45f + 0.08f * wave)
            drawRect(Brush.radialGradient(listOf(c(c0, k * (0.7f + 0.3f * wave)), c(c1, k * 0.25f), Color.Transparent), Offset(w / 2, h * 0.35f), r))
        }

        AmbianceKind.FROST -> {
            seeds.points.take(7).forEachIndexed { i, (x, y, s) ->
                val drift = sin((phase + s) * 2 * PI).toFloat() * w * 0.03f
                val col = if (i % 2 == 0) c0 else c1
                drawCircle(
                    Brush.radialGradient(listOf(c(col, k), Color.Transparent), Offset(x * w + drift, y * h), w * (0.25f + s * 0.25f)),
                    radius = w * (0.25f + s * 0.25f), center = Offset(x * w + drift, y * h),
                )
            }
        }

        AmbianceKind.AURORA -> {
            listOf(c0, c1, c2).forEachIndexed { i, col ->
                val y0 = h * (0.15f + i * 0.12f)
                val off = sin((phase + i * 0.33f) * 2 * PI).toFloat() * h * 0.04f
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, c(col, k), Color.Transparent), y0 + off, y0 + off + h * 0.22f),
                    topLeft = Offset(0f, y0 + off), size = Size(w, h * 0.22f),
                )
            }
        }

        AmbianceKind.BRUSHED_METAL -> {
            var y = 0f
            var i = 0
            while (y < h) {
                val a = k * (0.4f + seeds.points[i % seeds.points.size].third * 0.6f)
                drawLine(c(c0, a), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                y += 3f + (i % 3)
                i++
            }
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, c(c1, k * 1.5f)), h * 0.85f, h))
        }

        AmbianceKind.DIGITAL_RAIN -> {
            val cols = 28
            val cell = w / cols
            for (col in 0 until cols) {
                val s = seeds.points[col % seeds.points.size]
                val head = ((phase * (0.6f + s.third) + s.first) % 1f) * (h * 1.3f)
                for (j in 0 until 14) {
                    val y = head - j * cell * 1.3f
                    if (y < 0 || y > h) continue
                    drawRect(c(c0, k * (1f - j / 14f)), Offset(col * cell + cell * 0.3f, y), Size(cell * 0.4f, cell * 0.8f))
                }
            }
        }

        AmbianceKind.CORONA -> {
            val center = Offset(w * 0.85f, h * 0.05f)
            drawRect(Brush.radialGradient(listOf(c(c0, k * 1.6f), c(c1, k * 0.5f), Color.Transparent), center, maxOf(w, h) * 0.7f))
            for (i in 0 until 12) {
                val ang = (i / 12f + phase / 12f) * 2 * PI
                val len = maxOf(w, h) * (0.5f + 0.2f * abs(sin(ang * 3 + phase * 2 * PI)).toFloat())
                drawLine(
                    c(c0, k * 0.6f), center,
                    Offset(center.x + (kotlin.math.cos(ang) * len).toFloat(), center.y + (sin(ang) * len).toFloat()),
                    strokeWidth = 2f,
                )
            }
        }

        AmbianceKind.STARFIELD -> {
            seeds.points.forEach { (x, y, s) ->
                val tw = (sin((phase + s) * 2 * PI).toFloat() + 1f) / 2f
                drawCircle(c(if (s > 0.8f) c1 else c0, k * (0.25f + 0.75f * tw) * s), radius = 0.8f + s * 1.8f, center = Offset(x * w, y * h))
            }
        }

        AmbianceKind.SCANLINES -> {
            drawRect(Brush.radialGradient(listOf(c(c1, k * 0.35f), Color.Transparent), Offset(w / 2, h / 2), maxOf(w, h) * 0.75f))
            var y = 0f
            while (y < h) {
                drawLine(c(c0, k), Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                y += 4f
            }
        }

        AmbianceKind.CHROME_SWEEP -> {
            val x = -w + phase * w * 3f
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, c(c1, k), c(c0, k * 1.6f), c(c1, k), Color.Transparent),
                    start = Offset(x, 0f), end = Offset(x + w, h),
                ),
            )
        }
    }
}
