package com.acegears.ace360x.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.acegears.ace360x.device.DeviceScreen
import com.acegears.ace360x.library.LibraryScreen
import com.acegears.ace360x.settings.SettingsScreen
import com.acegears.ace360x.theme.AceIcons
import com.acegears.ace360x.theme.AmbianceBackground
import com.acegears.ace360x.theme.aceCardColor
import com.acegears.ace360x.theme.LocalMotion
import com.acegears.ace360x.theme.aceFocusRing
import com.acegears.ace360x.theme.aceNavItemColors
import com.acegears.ace360x.theme.toEasing

private enum class Tab(val label: String) { LIBRARY("Library"), DEVICE("Device"), SETTINGS("Settings"), ABOUT("About") }

@Composable
private fun Tab.icon(): ImageVector = when (this) {
    Tab.LIBRARY -> AceIcons.Library
    Tab.DEVICE -> AceIcons.Device
    Tab.SETTINGS -> AceIcons.Settings
    Tab.ABOUT -> AceIcons.Info
}

@Composable
fun AppRoot() {
    var tab by rememberSaveable { mutableStateOf(Tab.LIBRARY) }
    val motion = LocalMotion.current
    AmbianceBackground {
        Scaffold(
            containerColor = Color.Transparent,
            // A transparent container gives no default content colour, and plain Text
            // then drew black on the dark theme; use the theme's text colour explicitly.
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                NavigationBar(containerColor = aceCardColor()) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon(), contentDescription = null) },
                            label = { Text(t.label) },
                            colors = aceNavItemColors(),
                            modifier = Modifier.aceFocusRing(),
                        )
                    }
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(tween(motion.mediumMs, easing = motion.easing.toEasing())) togetherWith
                        fadeOut(tween(motion.shortMs, easing = motion.easing.toEasing()))
                },
                label = "tab",
            ) { current ->
                val modifier = Modifier.padding(padding)
                when (current) {
                    Tab.LIBRARY -> LibraryScreen(modifier)
                    Tab.DEVICE -> DeviceScreen(modifier)
                    Tab.SETTINGS -> SettingsScreen(modifier)
                    Tab.ABOUT -> AboutScreen(modifier)
                }
            }
        }
    }
}
