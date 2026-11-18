package com.acegears.ace360x.emu

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** One player's controller state in XInput units. */
data class PadSnapshot(
    val connected: Boolean = false,
    val buttons: Int = 0,
    val leftTrigger: Int = 0,
    val rightTrigger: Int = 0,
    val thumbLX: Int = 0,
    val thumbLY: Int = 0,
    val thumbRX: Int = 0,
    val thumbRY: Int = 0,
)

/**
 * Pure mapping from Android gamepad events to Xbox 360 (XInput) controls.
 * Android key codes / axis IDs are passed as plain ints so this is JVM-testable;
 * the constants below mirror android.view.KeyEvent / MotionEvent.
 */
object GamepadMapper {
    // XInput button bits (xenia/hid/input.h).
    const val DPAD_UP = 0x0001
    const val DPAD_DOWN = 0x0002
    const val DPAD_LEFT = 0x0004
    const val DPAD_RIGHT = 0x0008
    const val START = 0x0010
    const val BACK = 0x0020
    const val LEFT_THUMB = 0x0040
    const val RIGHT_THUMB = 0x0080
    const val LEFT_SHOULDER = 0x0100
    const val RIGHT_SHOULDER = 0x0200
    const val GUIDE = 0x0400
    const val A = 0x1000
    const val B = 0x2000
    const val X = 0x4000
    const val Y = 0x8000

    // android.view.KeyEvent codes.
    const val KEYCODE_DPAD_UP = 19
    const val KEYCODE_DPAD_DOWN = 20
    const val KEYCODE_DPAD_LEFT = 21
    const val KEYCODE_DPAD_RIGHT = 22
    const val KEYCODE_BUTTON_A = 96
    const val KEYCODE_BUTTON_B = 97
    const val KEYCODE_BUTTON_X = 99
    const val KEYCODE_BUTTON_Y = 100
    const val KEYCODE_BUTTON_L1 = 102
    const val KEYCODE_BUTTON_R1 = 103
    const val KEYCODE_BUTTON_L2 = 104
    const val KEYCODE_BUTTON_R2 = 105
    const val KEYCODE_BUTTON_THUMBL = 106
    const val KEYCODE_BUTTON_THUMBR = 107
    const val KEYCODE_BUTTON_START = 108
    const val KEYCODE_BUTTON_SELECT = 109
    const val KEYCODE_BUTTON_MODE = 110

    /** Default stick dead zone (fraction of full deflection), radial. */
    const val DEFAULT_DEAD_ZONE = 0.12f

    /** The 15 remappable Xbox buttons, in display order, with labels. */
    val REMAPPABLE: List<Pair<Int, String>> = listOf(
        A to "A", B to "B", X to "X", Y to "Y",
        LEFT_SHOULDER to "LB", RIGHT_SHOULDER to "RB",
        LEFT_THUMB to "Left stick click", RIGHT_THUMB to "Right stick click",
        START to "Start", BACK to "Back", GUIDE to "Guide",
        DPAD_UP to "D-pad up", DPAD_DOWN to "D-pad down", DPAD_LEFT to "D-pad left", DPAD_RIGHT to "D-pad right",
    )

    /** Default Xbox button → Android key code table. */
    val DEFAULT_BINDINGS: Map<Int, Int> = REMAPPABLE.associate { (bit, _) ->
        bit to (DEFAULT_KEY_FOR_BUTTON[bit] ?: 0)
    }

    private val DEFAULT_KEY_FOR_BUTTON: Map<Int, Int>
        get() = mapOf(
            A to KEYCODE_BUTTON_A, B to KEYCODE_BUTTON_B, X to KEYCODE_BUTTON_X, Y to KEYCODE_BUTTON_Y,
            LEFT_SHOULDER to KEYCODE_BUTTON_L1, RIGHT_SHOULDER to KEYCODE_BUTTON_R1,
            LEFT_THUMB to KEYCODE_BUTTON_THUMBL, RIGHT_THUMB to KEYCODE_BUTTON_THUMBR,
            START to KEYCODE_BUTTON_START, BACK to KEYCODE_BUTTON_SELECT, GUIDE to KEYCODE_BUTTON_MODE,
            DPAD_UP to KEYCODE_DPAD_UP, DPAD_DOWN to KEYCODE_DPAD_DOWN,
            DPAD_LEFT to KEYCODE_DPAD_LEFT, DPAD_RIGHT to KEYCODE_DPAD_RIGHT,
        )

    /**
     * Key code → XInput bit lookup for a binding table (Xbox bit → key code).
     * If two buttons share a key, the key presses both.
     */
    fun lookupFor(bindings: Map<Int, Int>): Map<Int, Int> {
        val out = HashMap<Int, Int>()
        bindings.forEach { (bit, key) -> if (key != 0) out[key] = (out[key] ?: 0) or bit }
        return out
    }

    /** "bit:key;bit:key" — compact persistence format. Unknown/garbled entries are ignored. */
    fun encodeBindings(bindings: Map<Int, Int>): String =
        bindings.entries.sortedBy { it.key }.joinToString(";") { "${it.key}:${it.value}" }

    fun decodeBindings(text: String?): Map<Int, Int> {
        val result = DEFAULT_BINDINGS.toMutableMap()
        text?.split(';')?.forEach { pair ->
            val parts = pair.split(':')
            val bit = parts.getOrNull(0)?.toIntOrNull()
            val key = parts.getOrNull(1)?.toIntOrNull()
            if (bit != null && key != null && bit in result) result[bit] = key
        }
        return result
    }

    /** XInput button bit for a key code with the default bindings, or 0 if unmapped. */
    fun buttonForKey(keyCode: Int): Int = when (keyCode) {
        KEYCODE_BUTTON_A -> A
        KEYCODE_BUTTON_B -> B
        KEYCODE_BUTTON_X -> X
        KEYCODE_BUTTON_Y -> Y
        KEYCODE_BUTTON_L1 -> LEFT_SHOULDER
        KEYCODE_BUTTON_R1 -> RIGHT_SHOULDER
        KEYCODE_BUTTON_THUMBL -> LEFT_THUMB
        KEYCODE_BUTTON_THUMBR -> RIGHT_THUMB
        KEYCODE_BUTTON_START -> START
        KEYCODE_BUTTON_SELECT -> BACK
        KEYCODE_BUTTON_MODE -> GUIDE
        KEYCODE_DPAD_UP -> DPAD_UP
        KEYCODE_DPAD_DOWN -> DPAD_DOWN
        KEYCODE_DPAD_LEFT -> DPAD_LEFT
        KEYCODE_DPAD_RIGHT -> DPAD_RIGHT
        else -> 0
    }

    /** True for L2/R2 sent as digital keys by controllers without analog triggers. */
    fun isDigitalTrigger(keyCode: Int): Boolean = keyCode == KEYCODE_BUTTON_L2 || keyCode == KEYCODE_BUTTON_R2

    /** 0..1 trigger axis → 0..255. */
    fun trigger(value: Float): Int = (value.coerceIn(0f, 1f) * 255f).roundToInt()

    /**
     * Stick (x, y) in Android convention (-1..1, +y = down) → XInput (-32768..32767,
     * +y = up), with a radial dead zone rescaled so output starts at 0 at its edge.
     */
    fun stick(x: Float, y: Float, deadZone: Float = DEFAULT_DEAD_ZONE): Pair<Int, Int> {
        val magnitude = sqrt(x * x + y * y)
        if (magnitude <= deadZone || magnitude == 0f) return 0 to 0
        val clamped = magnitude.coerceAtMost(1f)
        val scale = ((clamped - deadZone) / (1f - deadZone)) / magnitude
        return axisToShort(x * scale) to axisToShort(-y * scale)
    }

    /** Hat axes (-1/0/1) → d-pad bits. */
    fun hatButtons(hatX: Float, hatY: Float): Int {
        var b = 0
        if (hatX < -0.5f) b = b or DPAD_LEFT
        if (hatX > 0.5f) b = b or DPAD_RIGHT
        if (hatY < -0.5f) b = b or DPAD_UP
        if (hatY > 0.5f) b = b or DPAD_DOWN
        return b
    }

    private fun axisToShort(v: Float): Int {
        val c = v.coerceIn(-1f, 1f)
        return if (c >= 0) (c * 32767f).roundToInt() else (c * 32768f).roundToInt()
    }

    /** Small helper so callers can ignore noise from analog triggers. */
    fun nearlyZero(v: Float): Boolean = abs(v) < 0.01f
}
