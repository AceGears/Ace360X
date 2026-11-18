package com.acegears.ace360x.emu

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.view.InputDevice
import android.view.KeyEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.acegears.ace360x.diag.Diagnostics
import jp.xenia.emulator.WindowSurfaceView
import jp.xenia.emulator.WindowedAppActivity
import java.io.File

/**
 * Hosts Xenia's EmulatorApp ("xenia" windowed app) on a native Vulkan surface.
 *
 * Runs in its own process (":emu", see AndroidManifest.xml) because Xenia's
 * Android initialisation happens once per process and reads its launch
 * arguments then; a separate process also keeps an emulator crash or the
 * multi-GB guest address space away from the library UI. The process is
 * ended when the game screen closes so the next launch starts clean.
 */
class EmulatorActivity : WindowedAppActivity() {

    companion object {
        private const val EXTRA_GAME_URI = "com.acegears.ace360x.GAME_URI"
        private const val EXTRA_GAME_NAME = "com.acegears.ace360x.GAME_NAME"

        fun launchIntent(context: Context, gameUri: Uri, displayName: String): Intent {
            val storage = File(context.filesDir, "xenia").apply { mkdirs() }
            val cvars = Bundle().apply {
                // Path cvars are read with Bundle.getString by Xenia (cvar_android.cc).
                putString("target", gameUri.toString())
                putString("storage_root", storage.absolutePath)
                putString("log_file", File(storage, "xenia.log").absolutePath)
                putBoolean("discord", false)
            }
            return Intent(context, EmulatorActivity::class.java)
                .putExtra(EXTRA_CVARS, cvars)
                .putExtra(EXTRA_GAME_URI, gameUri.toString())
                .putExtra(EXTRA_GAME_NAME, displayName)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun getWindowedAppIdentifier(): String = "xenia"

    private lateinit var gamepads: GamepadInput
    private val touchVisible = mutableStateOf(true)

    private fun hasPhysicalGamepad(): Boolean = InputDevice.getDeviceIds().any { id ->
        val d = InputDevice.getDevice(id) ?: return@any false
        !d.isVirtual && (d.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        Diagnostics.markSessionStarted(this)

        touchVisible.value = !hasPhysicalGamepad()
        gamepads = GamepadInput(this) {
            // A real controller took over player 1: hide the on-screen controls.
            touchVisible.value = false
        }
        val touchState = TouchPadState { gamepads.onTouchState(it) }

        val surfaceView = WindowSurfaceView(this)
        val overlay = ComposeView(this).apply {
            setContent {
                Box(Modifier.fillMaxSize()) {
                    if (touchVisible.value) TouchControlsOverlay(touchState)
                    TextButton(
                        onClick = {
                            touchVisible.value = !touchVisible.value
                            gamepads.setTouchEnabled(touchVisible.value)
                        },
                        modifier = Modifier.align(Alignment.TopCenter).alpha(0.6f),
                    ) {
                        Text(if (touchVisible.value) "Hide controls" else "Show controls", color = Color.White)
                    }
                }
            }
        }
        val root = FrameLayout(this).apply {
            addView(surfaceView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(overlay, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        setContentView(root)
        setWindowSurfaceView(surfaceView)
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        if (::gamepads.isInitialized) {
            gamepads.start()
            gamepads.setTouchEnabled(touchVisible.value)
        }
    }

    override fun onPause() {
        if (::gamepads.isInitialized) gamepads.stop()
        super.onPause()
    }

    // Controller events are taken before view dispatch so the on-screen UI
    // (ImGui, which also listens to the surface) doesn't swallow them.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        (::gamepads.isInitialized && gamepads.onKeyEvent(event)) || super.dispatchKeyEvent(event)

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean =
        (::gamepads.isInitialized && gamepads.onMotionEvent(event)) || super.dispatchGenericMotionEvent(event)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onNativeInitializationFailed() {
        Toast.makeText(this, "The emulator couldn't start. See Device → self-test.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        val finishing = isFinishing
        super.onDestroy()
        if (finishing) Diagnostics.markSessionEndedCleanly(this)
        // Xenia's process-wide state can't be re-initialised for a second game.
        if (finishing) Process.killProcess(Process.myPid())
    }
}
