package com.acegears.ace360x.emu

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        if (isFinishing) return

        val surfaceView = WindowSurfaceView(this)
        setContentView(surfaceView)
        setWindowSurfaceView(surfaceView)
        hideSystemBars()
    }

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
        // Xenia's process-wide state can't be re-initialised for a second game.
        if (finishing) Process.killProcess(Process.myPid())
    }
}
