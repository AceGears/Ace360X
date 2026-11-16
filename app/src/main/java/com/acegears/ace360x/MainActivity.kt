package com.acegears.ace360x

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.acegears.ace360x.core.NativeBridge
import com.acegears.ace360x.ui.Ace360XTheme
import com.acegears.ace360x.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        NativeBridge.initCore(this)
        setContent {
            Ace360XTheme { AppRoot() }
        }
    }
}
