package com.acegears.ace360x.emu

import com.acegears.ace360x.theme.ThemeCustomization
import com.acegears.ace360x.theme.AceTheme
import com.acegears.ace360x.BuildConfig
import com.acegears.ace360x.theme.AceDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.acegears.ace360x.library.PlayStats
import android.view.InputDevice
import android.view.KeyEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.OnBackPressedCallback
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
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
import com.acegears.ace360x.settings.EmulatorSettings
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
        private const val EXTRA_STATS_KEY = "com.acegears.ace360x.STATS_KEY"

        private const val EXTRA_TOUCH_OPACITY = "com.acegears.ace360x.TOUCH_OPACITY"
        private const val EXTRA_TOUCH_SCALE = "com.acegears.ace360x.TOUCH_SCALE"
        private const val EXTRA_TOUCH_DEFAULT = "com.acegears.ace360x.TOUCH_DEFAULT"
        private const val EXTRA_DEAD_ZONE = "com.acegears.ace360x.DEAD_ZONE"
        private const val EXTRA_BINDINGS = "com.acegears.ace360x.BINDINGS"
        private const val EXTRA_THEME = "com.acegears.ace360x.THEME"
        private const val DEBUG_LETTERBOX_RGB = 0x10307A

        /** [target] is a content:// document URI or an acetree:// path (see SafTree). */
        fun launchIntent(
            context: Context,
            target: String,
            displayName: String,
            settings: EmulatorSettings,
            statsKey: String = target,
            theme: ThemeCustomization = ThemeCustomization(),
        ): Intent {
            val storage = File(context.filesDir, "xenia").apply { mkdirs() }
            val cvars = Bundle().apply {
                // Path cvars are read with Bundle.getString by Xenia (cvar_android.cc).
                putString("target", target)
                putString("storage_root", storage.absolutePath)
                putString("log_file", File(storage, "xenia.log").absolutePath)
                putBoolean("discord", false)
                // Debug builds: dark blue letterbox bars (patch 0015), so a black game image can
                // be told apart from nothing reaching the screen. Release builds keep black.
                if (BuildConfig.DEBUG) putInt("present_letterbox_color", DEBUG_LETTERBOX_RGB)
                // Bundle value types must match each cvar's type (see EmulatorSettings.toCvars).
                settings.toCvars().forEach { (key, value) ->
                    when (value) {
                        is Int -> putInt(key, value)
                        is Boolean -> putBoolean(key, value)
                        is String -> putString(key, value)
                        else -> error("Unsupported cvar type for $key")
                    }
                }
            }
            return Intent(context, EmulatorActivity::class.java)
                .putExtra(EXTRA_CVARS, cvars)
                .putExtra(EXTRA_GAME_URI, target)
                .putExtra(EXTRA_STATS_KEY, statsKey)
                .putExtra(EXTRA_GAME_NAME, displayName)
                .putExtra(EXTRA_TOUCH_OPACITY, settings.touchOpacity)
                .putExtra(EXTRA_TOUCH_SCALE, settings.touchScale)
                .putExtra(EXTRA_TOUCH_DEFAULT, settings.touchDefault.name)
                .putExtra(EXTRA_DEAD_ZONE, settings.stickDeadZone)
                .putExtra(EXTRA_BINDINGS, GamepadMapper.encodeBindings(settings.buttonBindings))
                .putExtra(EXTRA_THEME, theme.encode())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun getWindowedAppIdentifier(): String = "xenia"

    private lateinit var gamepads: GamepadInput
    private val touchVisible = mutableStateOf(true)
    private val confirmExit = mutableStateOf(false)
    private var gameUri: String? = null
    private var statsKey: String? = null
    private var resumedAtMillis = 0L
    private val playtimeTicker = Handler(Looper.getMainLooper())
    private val playtimeTick = object : Runnable {
        override fun run() {
            flushPlaytime()
            playtimeTicker.postDelayed(this, 60_000)
        }
    }

    /** Adds time since the last flush to this game's playtime (survives crashes, ±1 min). */
    private fun flushPlaytime() {
        val uri = statsKey ?: return
        val now = System.currentTimeMillis()
        if (resumedAtMillis > 0) PlayStats.addPlaytime(this, uri, (now - resumedAtMillis) / 1000)
        resumedAtMillis = now
    }
    private val thermalStatus = mutableIntStateOf(PowerManager.THERMAL_STATUS_NONE)
    private val thermalListener = PowerManager.OnThermalStatusChangedListener { thermalStatus.intValue = it }

    private fun hasPhysicalGamepad(): Boolean = InputDevice.getDeviceIds().any { id ->
        val d = InputDevice.getDevice(id) ?: return@any false
        !d.isVirtual && (d.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Must be registered before Xenia starts and mounts the game.
        SafTree.install(this)
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        Diagnostics.markSessionStarted(this)
        gameUri = intent.getStringExtra(EXTRA_GAME_URI)
        statsKey = intent.getStringExtra(EXTRA_STATS_KEY) ?: gameUri
        statsKey?.let { PlayStats.markPlayed(this, it) }

        val touchOpacity = intent.getFloatExtra(EXTRA_TOUCH_OPACITY, 0.75f)
        val touchScale = intent.getFloatExtra(EXTRA_TOUCH_SCALE, 1f)
        touchVisible.value = when (intent.getStringExtra(EXTRA_TOUCH_DEFAULT)) {
            EmulatorSettings.TouchDefault.ALWAYS.name -> true
            EmulatorSettings.TouchDefault.NEVER.name -> false
            else -> !hasPhysicalGamepad()
        }
        gamepads = GamepadInput(
            this,
            keyLookup = GamepadMapper.lookupFor(GamepadMapper.decodeBindings(intent.getStringExtra(EXTRA_BINDINGS))),
            deadZone = intent.getFloatExtra(EXTRA_DEAD_ZONE, GamepadMapper.DEFAULT_DEAD_ZONE),
        ) {
            // A real controller took over player 1: hide the on-screen controls.
            touchVisible.value = false
        }
        val touchState = TouchPadState { gamepads.onTouchState(it) }

        val surfaceView = WindowSurfaceView(this)
        // The game process can't read the main process's DataStore; the theme comes in the Intent.
        val theme = ThemeCustomization.decode(intent.getStringExtra(EXTRA_THEME))
        val overlay = ComposeView(this).apply {
            setContent {
              AceTheme(theme) {
                Box(Modifier.fillMaxSize()) {
                    if (touchVisible.value) TouchControlsOverlay(touchState, touchOpacity, touchScale)
                    ThermalNotice(thermalStatus.intValue, Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 12.dp))
                    if (confirmExit.value) {
                        AceDialog(
                            onDismissRequest = { confirmExit.value = false },
                            title = { Text("Quit game?") },
                            text = { Text("Progress since your last in-game save will be lost.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    // Before finish(): the Library resumes, and checks the marker,
                                    // before this activity's onDestroy runs (seen on device).
                                    Diagnostics.markSessionEndedCleanly(this@EmulatorActivity)
                                    finish()
                                }) { Text("Quit") }
                            },
                            dismissButton = { TextButton(onClick = { confirmExit.value = false }) { Text("Keep playing") } },
                        )
                    }
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
        }
        val root = FrameLayout(this).apply {
            addView(surfaceView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(overlay, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        setContentView(root)
        setWindowSurfaceView(surfaceView)
        // Back (including controllers that send Back from their B button) asks first.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmExit.value = true
            }
        })
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        resumedAtMillis = System.currentTimeMillis()
        playtimeTicker.postDelayed(playtimeTick, 60_000)
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        thermalStatus.intValue = pm.currentThermalStatus
        pm.addThermalStatusListener(mainExecutor, thermalListener)
        if (::gamepads.isInitialized) {
            gamepads.start()
            gamepads.setTouchEnabled(touchVisible.value)
        }
    }

    override fun onPause() {
        playtimeTicker.removeCallbacks(playtimeTick)
        flushPlaytime()
        resumedAtMillis = 0
        (getSystemService(POWER_SERVICE) as PowerManager).removeThermalStatusListener(thermalListener)
        if (::gamepads.isInitialized) gamepads.stop()
        super.onPause()
    }

    // Controller events are taken before view dispatch so the on-screen UI
    // (ImGui, which also listens to the surface) doesn't swallow them.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        (::gamepads.isInitialized && gamepads.onKeyEvent(event)) || super.dispatchKeyEvent(event)

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean =
        (::gamepads.isInitialized && gamepads.onMotionEvent(event)) || super.dispatchGenericMotionEvent(event)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTask: a second launch while a game is open lands here.
        if (intent.getStringExtra(EXTRA_GAME_URI) != gameUri) {
            Toast.makeText(this, "A game is already running. Quit it before starting another.", Toast.LENGTH_LONG).show()
        }
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
        // Must happen before super.onDestroy(): upstream's native OnDestroy ends the app with
        // std::quick_exit ("Cheap-skate exit!"), so nothing after it runs. Marking afterwards
        // made every normal quit show the "ended unexpectedly" banner (seen on device 2026-11-21).
        if (finishing) Diagnostics.markSessionEndedCleanly(this)
        super.onDestroy()
        // Reached only if native code didn't exit. Xenia's process-wide state can't be
        // re-initialised for a second game.
        if (finishing) Process.killProcess(Process.myPid())
    }
}
