/*
 * Kotlin port of Xenia's android/.../jp/xenia/emulator/WindowedAppActivity.java.
 * Copyright 2021 Ben Vanik and Xenia contributors. Released under the BSD
 * license (see native/xenia-upstream/LICENSE). Ported for Ace360X.
 *
 * The package and class name must stay `jp.xenia.emulator.WindowedAppActivity`:
 * the native side (src/xenia/ui/windowed_app_context_android.cc) binds its JNI
 * functions to this class and calls getIntent(), finish() and
 * postInvalidateWindowSurface() on it.
 */
package jp.xenia.emulator

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.AssetManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import com.acegears.ace360x.core.NativeBridge

abstract class WindowedAppActivity : Activity() {

    companion object {
        /** Bundle extra with Xenia cvar launch arguments; literal also used natively. */
        const val EXTRA_CVARS = "jp.xenia.emulator.WindowedAppActivity.EXTRA_CVARS"

        init {
            // Loads libace360x.so (which contains the Xenia core).
            NativeBridge.isLoaded
        }
    }

    private val windowSurfaceListener = WindowSurfaceListener()

    /** Native app context; 0 while not initialised or while destroying. */
    private var appContext: Long = 0
    private var windowSurfaceView: WindowSurfaceView? = null

    private external fun initializeWindowedAppOnCreate(
        windowedAppIdentifier: String,
        assetManager: AssetManager,
    ): Long

    private external fun onDestroyNative(appContext: Long)
    private external fun onWindowSurfaceLayoutChange(appContext: Long, left: Int, top: Int, right: Int, bottom: Int)
    private external fun onWindowSurfaceMotionEvent(appContext: Long, event: MotionEvent): Boolean
    private external fun onWindowSurfaceChanged(appContext: Long, windowSurface: Surface?)
    private external fun paintWindow(appContext: Long, forcePaint: Boolean)

    protected abstract fun getWindowedAppIdentifier(): String

    /** Called when native initialisation fails; the activity finishes afterwards. */
    protected open fun onNativeInitializationFailed() = Unit

    protected fun setWindowSurfaceView(view: WindowSurfaceView?) {
        if (windowSurfaceView === view) return
        windowSurfaceView?.let { old ->
            old.holder.removeCallback(windowSurfaceListener)
            old.setOnTouchListener(null)
            old.setOnGenericMotionListener(null)
            old.removeOnLayoutChangeListener(windowSurfaceListener)
            windowSurfaceView = null
            if (appContext != 0L) onWindowSurfaceChanged(appContext, null)
        }
        if (view == null) return
        windowSurfaceView = view
        view.addOnLayoutChangeListener(windowSurfaceListener)
        view.setOnGenericMotionListener(windowSurfaceListener)
        view.setOnTouchListener(windowSurfaceListener)
        view.holder.addCallback(windowSurfaceListener)
        if (appContext != 0L) {
            val surface = view.holder.surface
            if (surface != null && surface.isValid) onWindowSurfaceChanged(appContext, surface)
            if (view.isLaidOut) {
                onWindowSurfaceLayoutChange(appContext, view.left, view.top, view.right, view.bottom)
            }
        }
    }

    fun onWindowSurfaceDraw(forcePaint: Boolean) {
        if (appContext == 0L) return
        paintWindow(appContext, forcePaint)
    }

    /** Called from native code, possibly from non-UI threads. */
    @Suppress("unused")
    protected fun postInvalidateWindowSurface() {
        windowSurfaceView?.postInvalidate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!NativeBridge.isLoaded) {
            onNativeInitializationFailed()
            finish()
            return
        }
        appContext = initializeWindowedAppOnCreate(getWindowedAppIdentifier(), assets)
        if (appContext == 0L) {
            onNativeInitializationFailed()
            finish()
        }
    }

    override fun onDestroy() {
        setWindowSurfaceView(null)
        if (appContext != 0L) onDestroyNative(appContext)
        appContext = 0
        super.onDestroy()
    }

    private inner class WindowSurfaceListener :
        View.OnGenericMotionListener,
        View.OnLayoutChangeListener,
        View.OnTouchListener,
        SurfaceHolder.Callback2 {

        override fun onLayoutChange(
            v: View, left: Int, top: Int, right: Int, bottom: Int,
            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int,
        ) {
            if (appContext != 0L) onWindowSurfaceLayoutChange(appContext, left, top, right, bottom)
        }

        override fun onGenericMotion(view: View, event: MotionEvent): Boolean =
            appContext != 0L && onWindowSurfaceMotionEvent(appContext, event)

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean =
            appContext != 0L && onWindowSurfaceMotionEvent(appContext, event)

        override fun surfaceCreated(holder: SurfaceHolder) {
            if (appContext != 0L) onWindowSurfaceChanged(appContext, holder.surface)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            if (appContext != 0L) onWindowSurfaceChanged(appContext, holder.surface)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            if (appContext != 0L) onWindowSurfaceChanged(appContext, null)
        }

        override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
            onWindowSurfaceDraw(true)
        }
    }
}
