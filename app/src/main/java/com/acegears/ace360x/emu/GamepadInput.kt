package com.acegears.ace360x.emu

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.acegears.ace360x.core.NativeBridge

/**
 * Collects physical controller input for the game screen and forwards it to
 * the native Xenia input driver. Controllers get player slots 1-4 in the order
 * they are first used; a disconnected controller frees its slot.
 */
class GamepadInput(private val context: Context) : InputManager.InputDeviceListener {

    private class Slot(var deviceId: Int) {
        var keyButtons = 0
        var hatButtons = 0
        var digitalLT = false
        var digitalRT = false
        var analogLT = 0f
        var analogRT = 0f
        var lx = 0f
        var ly = 0f
        var rx = 0f
        var ry = 0f
        var lastRumble = 0

        fun snapshot(): PadSnapshot {
            val (tlx, tly) = GamepadMapper.stick(lx, ly)
            val (trx, try_) = GamepadMapper.stick(rx, ry)
            return PadSnapshot(
                connected = true,
                buttons = keyButtons or hatButtons,
                leftTrigger = if (digitalLT) 255 else GamepadMapper.trigger(analogLT),
                rightTrigger = if (digitalRT) 255 else GamepadMapper.trigger(analogRT),
                thumbLX = tlx, thumbLY = tly, thumbRX = trx, thumbRY = try_,
            )
        }
    }

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
    private val slots = arrayOfNulls<Slot>(4)
    private val handler = Handler(Looper.getMainLooper())
    private val rumblePoll = object : Runnable {
        override fun run() {
            pollRumble()
            handler.postDelayed(this, 50)
        }
    }

    fun start() {
        inputManager.registerInputDeviceListener(this, handler)
        handler.post(rumblePoll)
    }

    fun stop() {
        handler.removeCallbacks(rumblePoll)
        inputManager.unregisterInputDeviceListener(this)
        for (i in slots.indices) {
            if (slots[i] != null) NativeBridge.setPadState(i, PadSnapshot())
            slots[i] = null
        }
    }

    private fun isGamepad(source: Int): Boolean =
        source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK

    private fun slotFor(deviceId: Int): Int? {
        slots.indexOfFirst { it?.deviceId == deviceId }.takeIf { it >= 0 }?.let { return it }
        val free = slots.indexOfFirst { it == null }
        if (free < 0) return null
        slots[free] = Slot(deviceId)
        return free
    }

    /** Returns true if the event was a controller event and has been consumed. */
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isGamepad(event.source)) return false
        val bit = GamepadMapper.buttonForKey(event.keyCode)
        val trigger = GamepadMapper.isDigitalTrigger(event.keyCode)
        if (bit == 0 && !trigger) return false
        val index = slotFor(event.deviceId) ?: return true
        val slot = slots[index] ?: return true
        val down = event.action == KeyEvent.ACTION_DOWN
        when {
            trigger && event.keyCode == GamepadMapper.KEYCODE_BUTTON_L2 -> slot.digitalLT = down
            trigger -> slot.digitalRT = down
            down -> slot.keyButtons = slot.keyButtons or bit
            else -> slot.keyButtons = slot.keyButtons and bit.inv()
        }
        NativeBridge.setPadState(index, slot.snapshot())
        return true
    }

    fun onMotionEvent(event: MotionEvent): Boolean {
        if (!isGamepad(event.source) || event.action != MotionEvent.ACTION_MOVE) return false
        val index = slotFor(event.deviceId) ?: return true
        val slot = slots[index] ?: return true
        slot.lx = event.getAxisValue(MotionEvent.AXIS_X)
        slot.ly = event.getAxisValue(MotionEvent.AXIS_Y)
        slot.rx = event.getAxisValue(MotionEvent.AXIS_Z)
        slot.ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        // Controllers report analog triggers on either LTRIGGER/RTRIGGER or BRAKE/GAS.
        slot.analogLT = maxOf(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE))
        slot.analogRT = maxOf(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS))
        slot.hatButtons = GamepadMapper.hatButtons(
            event.getAxisValue(MotionEvent.AXIS_HAT_X),
            event.getAxisValue(MotionEvent.AXIS_HAT_Y),
        )
        NativeBridge.setPadState(index, slot.snapshot())
        return true
    }

    private fun pollRumble() {
        for ((i, slot) in slots.withIndex()) {
            slot ?: continue
            val (left, right) = NativeBridge.padVibration(i)
            val strength = maxOf(left, right)
            if (strength == slot.lastRumble) continue
            slot.lastRumble = strength
            val vibrator = vibratorFor(slot.deviceId) ?: continue
            if (strength == 0) {
                vibrator.cancel()
            } else {
                val amplitude = (strength * 255L / 65535L).toInt().coerceIn(1, 255)
                vibrator.vibrate(VibrationEffect.createOneShot(120, amplitude))
            }
        }
    }

    private fun vibratorFor(deviceId: Int): Vibrator? {
        val device = InputDevice.getDevice(deviceId) ?: return null
        val v = if (Build.VERSION.SDK_INT >= 31) device.vibratorManager.defaultVibrator else @Suppress("DEPRECATION") device.vibrator
        return v.takeIf { it.hasVibrator() }
    }

    override fun onInputDeviceAdded(deviceId: Int) = Unit
    override fun onInputDeviceChanged(deviceId: Int) = Unit

    override fun onInputDeviceRemoved(deviceId: Int) {
        val i = slots.indexOfFirst { it?.deviceId == deviceId }
        if (i >= 0) {
            slots[i] = null
            NativeBridge.setPadState(i, PadSnapshot())
        }
    }
}
