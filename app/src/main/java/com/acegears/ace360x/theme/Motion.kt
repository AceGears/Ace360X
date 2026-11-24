package com.acegears.ace360x.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/*
 * Motion effects in the AceArcade style (docs/research/acearcade-design.md, stage 3),
 * written for Ace360X. All of them do nothing when motion is reduced, and none runs
 * continuously: each plays once in response to a touch or a change.
 */

/** True when the user or the system asked for no animation. */
@Composable
fun motionReduced(): Boolean = LocalMotion.current == MotionTokens.REDUCED

/** Rises a few dp while pressed, then settles with a soft spring. */
@Composable
fun Modifier.pressLift(interaction: MutableInteractionSource, lift: Dp = 4.dp): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val reduced = motionReduced()
    val amount by animateFloatAsState(
        targetValue = if (pressed && !reduced) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "lift",
    )
    return graphicsLayer {
        translationY = -lift.toPx() * amount
        scaleX = 1f + 0.015f * amount
        scaleY = 1f + 0.015f * amount
    }
}

/**
 * The two edges of a selection indicator. With liquid motion the edge facing the
 * direction of travel moves quickly and the other follows slowly, so the indicator
 * stretches toward its target and then contracts. Without it both edges move together;
 * with reduced motion it jumps.
 */
class LiquidIndicator {
    val start = Animatable(0f)
    val end = Animatable(0f)
    private var placed = false

    suspend fun moveTo(newStart: Float, newEnd: Float, liquid: Boolean, reduced: Boolean, durationScale: Float) {
        if (!placed || reduced) {
            start.snapTo(newStart)
            end.snapTo(newEnd)
            placed = true
            return
        }
        // A larger duration scale means slower animation, i.e. softer springs.
        val speed = 1f / durationScale.coerceIn(0.25f, 4f)
        val quick = spring<Float>(dampingRatio = 0.7f, stiffness = 900f * speed)
        val trailing = spring<Float>(dampingRatio = 0.62f, stiffness = (if (liquid) 220f else 900f) * speed)
        val forward = newStart > start.value
        coroutineScope {
            launch { start.animateTo(newStart, if (forward) trailing else quick) }
            launch { end.animateTo(newEnd, if (forward) quick else trailing) }
        }
    }
}

/**
 * Items that are being removed with the delete effect. The screen draws them with
 * [vanishEffect]; the actual removal runs when the effect has finished.
 */
object Vanishing {
    val keys = mutableStateListOf<String>()
    const val DURATION_MS = 680L

    /** Plays the effect on [key] when [effect] is on, then runs [action]. */
    suspend fun run(key: String, effect: Boolean, action: suspend () -> Unit) {
        if (effect) {
            keys.add(key)
            delay(DURATION_MS)
        }
        try {
            action()
        } finally {
            keys.remove(key)
        }
    }
}

private class Speck(val x: Float, val y: Float, val dx: Float, val dy: Float, val sizeDp: Float, val delay: Float, val tone: Int)

/**
 * The delete effect: a short shake, then the element shrinks and fades while small
 * specks in the theme's glow colours drift up and away. Draws nothing extra when
 * [active] is false.
 */
@Composable
fun Modifier.vanishEffect(active: Boolean): Modifier {
    if (!active) return this
    val palette = LocalAceTheme.current.palette
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(Vanishing.DURATION_MS.toInt(), easing = LinearEasing))
    }
    val specks = remember {
        val random = Random(360)
        List(56) {
            Speck(
                x = random.nextFloat(), y = random.nextFloat(),
                dx = random.nextFloat() * 2f - 0.6f, dy = -(0.4f + random.nextFloat()),
                sizeDp = 2f + random.nextFloat() * 4f, delay = random.nextFloat() * 0.25f,
                tone = random.nextInt(3),
            )
        }
    }
    val glow = palette.glow.toColor()
    val glowAlt = palette.glowAlt.toColor()
    val smoke = palette.onSurfaceVariant.toColor()
    val shakeEnd = 0.3f
    return this
        .drawWithContent {
            drawContent()
            val dissolve = ((progress.value - shakeEnd) / (1f - shakeEnd)).coerceIn(0f, 1f)
            if (dissolve <= 0f) return@drawWithContent
            val drift = 28.dp.toPx()
            specks.forEach { s ->
                val k = ((dissolve - s.delay) / (1f - s.delay)).coerceIn(0f, 1f)
                if (k > 0f && k < 1f) {
                    val colour = when (s.tone) {
                        0 -> glow
                        1 -> glowAlt
                        else -> smoke
                    }
                    val side = s.sizeDp.dp.toPx() * (1f - k * 0.7f)
                    drawRoundRect(
                        color = colour.copy(alpha = (1f - k) * 0.9f),
                        topLeft = Offset(s.x * size.width + s.dx * drift * k * 2f, s.y * size.height + s.dy * drift * k * 3f),
                        size = Size(side, side),
                        cornerRadius = CornerRadius(side / 3f),
                    )
                }
            }
        }
        .graphicsLayer {
            val p = progress.value
            val dissolve = ((p - shakeEnd) / (1f - shakeEnd)).coerceIn(0f, 1f)
            translationX = if (p < shakeEnd) {
                sin(p / shakeEnd * 6f * PI).toFloat() * 6.dp.toPx() * (1f - p / shakeEnd * 0.5f)
            } else {
                0f
            }
            alpha = 1f - dissolve
            scaleX = 1f - 0.12f * dissolve
            scaleY = 1f - 0.12f * dissolve
        }
}
