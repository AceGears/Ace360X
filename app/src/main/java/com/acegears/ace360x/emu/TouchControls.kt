package com.acegears.ace360x.emu

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Pure geometry for the virtual sticks (JVM-tested). */
object TouchMath {
    /**
     * Finger offset from the stick centre (pixels, Android convention: +y down)
     * → stick vector clamped to the unit circle.
     */
    fun stickVector(dx: Float, dy: Float, radius: Float): Pair<Float, Float> {
        if (radius <= 0f) return 0f to 0f
        val x = dx / radius
        val y = dy / radius
        val len = sqrt(x * x + y * y)
        return if (len > 1f) (x / len) to (y / len) else x to y
    }

    /** Smaller dead zone than physical sticks: a finger has no spring-return noise. */
    const val DEAD_ZONE = 0.06f
}

/**
 * Mutable touch-pad state; every change produces a fresh snapshot via [onChange].
 * Lives on the UI thread only.
 */
class TouchPadState(private val onChange: (PadSnapshot) -> Unit) {
    var buttons = 0
        private set
    private var lt = 0
    private var rt = 0
    private var left = 0f to 0f
    private var right = 0f to 0f

    fun setButton(bit: Int, down: Boolean) {
        buttons = if (down) buttons or bit else buttons and bit.inv()
        emit()
    }

    fun setTrigger(leftTrigger: Boolean, down: Boolean) {
        if (leftTrigger) lt = if (down) 255 else 0 else rt = if (down) 255 else 0
        emit()
    }

    fun setStick(leftStick: Boolean, v: Pair<Float, Float>) {
        if (leftStick) left = v else right = v
        emit()
    }

    fun snapshot(): PadSnapshot {
        val (lx, ly) = GamepadMapper.stick(left.first, left.second, TouchMath.DEAD_ZONE)
        val (rx, ry) = GamepadMapper.stick(right.first, right.second, TouchMath.DEAD_ZONE)
        return PadSnapshot(true, buttons, lt, rt, lx, ly, rx, ry)
    }

    private fun emit() = onChange(snapshot())
}

private val ControlFill = Color(0x33FFFFFF)
private val ControlPressed = Color(0x88FFFFFF)
private val ControlBorder = Color(0x99FFFFFF)
private val LabelColor = Color(0xEEFFFFFF)

/**
 * Full-screen landscape overlay. Only the controls themselves take touches;
 * touches elsewhere fall through to the emulator surface (Xenia's menus).
 */
@Composable
fun TouchControlsOverlay(state: TouchPadState, opacity: Float = 0.75f) {
    Box(Modifier.fillMaxSize().alpha(opacity).padding(horizontal = 24.dp, vertical = 16.dp)) {
        // Shoulders and triggers.
        Column(Modifier.align(Alignment.TopStart), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TriggerButton("LT", state, left = true)
            PadButton("LB", GamepadMapper.LEFT_SHOULDER, state, width = 72.dp, height = 40.dp, shape = RoundedCornerShape(10.dp))
        }
        Column(Modifier.align(Alignment.TopEnd), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
            TriggerButton("RT", state, left = false)
            PadButton("RB", GamepadMapper.RIGHT_SHOULDER, state, width = 72.dp, height = 40.dp, shape = RoundedCornerShape(10.dp))
        }

        // Left side: stick (bottom-left), d-pad above-right of it.
        Box(Modifier.align(Alignment.BottomStart)) {
            VirtualStick(state, leftStick = true, size = 132.dp)
        }
        Box(Modifier.align(Alignment.BottomStart).offset(x = 150.dp, y = (-4).dp)) {
            DPad(state)
        }
        PadButton("LS", GamepadMapper.LEFT_THUMB, state, Modifier.align(Alignment.CenterStart).offset(y = 8.dp), 44.dp, 44.dp, CircleShape)

        // Right side: face buttons (bottom-right), right stick left of them.
        Box(Modifier.align(Alignment.BottomEnd)) { FaceButtons(state) }
        Box(Modifier.align(Alignment.BottomEnd).offset(x = (-170).dp, y = (-4).dp)) {
            VirtualStick(state, leftStick = false, size = 112.dp)
        }
        PadButton("RS", GamepadMapper.RIGHT_THUMB, state, Modifier.align(Alignment.CenterEnd).offset(y = 8.dp), 44.dp, 44.dp, CircleShape)

        // Back / Start, bottom centre.
        Row(Modifier.align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PadButton("Back", GamepadMapper.BACK, state, width = 64.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
            PadButton("Start", GamepadMapper.START, state, width = 64.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
        }
    }
}

@Composable
private fun FaceButtons(state: TouchPadState) {
    val s = 56.dp
    Box(Modifier.size(s * 3)) {
        PadButton("Y", GamepadMapper.Y, state, Modifier.align(Alignment.TopCenter), s, s, CircleShape, Color(0x66E8C547))
        PadButton("X", GamepadMapper.X, state, Modifier.align(Alignment.CenterStart), s, s, CircleShape, Color(0x664A8FE7))
        PadButton("B", GamepadMapper.B, state, Modifier.align(Alignment.CenterEnd), s, s, CircleShape, Color(0x66E05A5A))
        PadButton("A", GamepadMapper.A, state, Modifier.align(Alignment.BottomCenter), s, s, CircleShape, Color(0x6652C27A))
    }
}

@Composable
private fun DPad(state: TouchPadState) {
    val s = 44.dp
    val shape = RoundedCornerShape(8.dp)
    Box(Modifier.size(s * 3)) {
        PadButton("▲", GamepadMapper.DPAD_UP, state, Modifier.align(Alignment.TopCenter), s, s, shape)
        PadButton("◀", GamepadMapper.DPAD_LEFT, state, Modifier.align(Alignment.CenterStart), s, s, shape)
        PadButton("▶", GamepadMapper.DPAD_RIGHT, state, Modifier.align(Alignment.CenterEnd), s, s, shape)
        PadButton("▼", GamepadMapper.DPAD_DOWN, state, Modifier.align(Alignment.BottomCenter), s, s, shape)
    }
}

@Composable
private fun PadButton(
    label: String,
    bit: Int,
    state: TouchPadState,
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    height: Dp = 56.dp,
    shape: Shape = CircleShape,
    tint: Color = ControlFill,
) {
    val view = LocalView.current
    var pressed by remember { mutableIntStateOf(0) }
    Box(
        modifier
            .size(width, height)
            .background(if (pressed > 0) ControlPressed else tint, shape)
            .border(1.5.dp, ControlBorder, shape)
            .pointerInput(bit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    pressed = 1
                    state.setButton(bit, true)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    try {
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    } finally {
                        pressed = 0
                        state.setButton(bit, false)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = LabelColor, fontSize = 14.sp) }
}

@Composable
private fun TriggerButton(label: String, state: TouchPadState, left: Boolean) {
    val view = LocalView.current
    var pressed by remember { mutableIntStateOf(0) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(72.dp, 40.dp)
            .background(if (pressed > 0) ControlPressed else ControlFill, shape)
            .border(1.5.dp, ControlBorder, shape)
            .pointerInput(left) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    pressed = 1
                    state.setTrigger(left, true)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    try {
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    } finally {
                        pressed = 0
                        state.setTrigger(left, false)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = LabelColor, fontSize = 14.sp) }
}

@Composable
private fun VirtualStick(state: TouchPadState, leftStick: Boolean, size: Dp) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .size(size)
            .background(ControlFill, CircleShape)
            .border(1.5.dp, ControlBorder, CircleShape)
            .pointerInput(leftStick) {
                val radius = this.size.width / 2f
                val centre = Offset(radius, this.size.height / 2f)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var pos = down.position
                    fun update() {
                        val v = TouchMath.stickVector(pos.x - centre.x, pos.y - centre.y, radius)
                        knobX = v.first * radius
                        knobY = v.second * radius
                        state.setStick(leftStick, v)
                    }
                    update()
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            if (change.positionChange() != Offset.Zero) {
                                pos = change.position
                                update()
                            }
                            change.consume()
                        }
                    } finally {
                        knobX = 0f
                        knobY = 0f
                        state.setStick(leftStick, 0f to 0f)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.42f).offset { IntOffset(knobX.roundToInt(), knobY.roundToInt()) }) {
            drawCircle(ControlPressed)
        }
    }
}
