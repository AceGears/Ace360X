/*
 * Kotlin port of Xenia's jp/xenia/emulator/WindowSurfaceView.java.
 * Copyright 2021 Ben Vanik and Xenia contributors. BSD license. Ported for Ace360X.
 */
package jp.xenia.emulator

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceView

class WindowSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : SurfaceView(context, attrs, defStyleAttr) {

    init {
        // Native drawing is invoked from onDraw.
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        (context as? WindowedAppActivity)?.onWindowSurfaceDraw(false)
    }
}
