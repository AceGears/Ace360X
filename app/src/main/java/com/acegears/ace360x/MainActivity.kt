package com.acegears.ace360x

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acegears.ace360x.core.NativeBridge
import com.acegears.ace360x.theme.AceTheme
import androidx.compose.foundation.isSystemInDarkTheme
import com.acegears.ace360x.theme.ResolvedTheme
import com.acegears.ace360x.theme.ThemeRepository
import com.acegears.ace360x.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        NativeBridge.initCore(this)
        val themes = ThemeRepository(applicationContext)
        setContent {
            val custom by themes.customization.collectAsStateWithLifecycle(initialValue = null)
            val c = custom ?: return@setContent // window background shows until the theme loads
            val systemDark = isSystemInDarkTheme()
            val dark = remember(c, systemDark) { ResolvedTheme.resolve(c, systemDark).palette.dark }
            LaunchedEffect(dark) {
                // Status/navigation bar icons must contrast with the theme, not the system mode.
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            AceTheme(c) { AppRoot() }
        }
    }
}
