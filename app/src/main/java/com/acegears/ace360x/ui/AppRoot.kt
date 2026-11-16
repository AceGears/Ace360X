package com.acegears.ace360x.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.acegears.ace360x.device.DeviceScreen
import com.acegears.ace360x.library.LibraryScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    LIBRARY("Library", Icons.AutoMirrored.Filled.List),
    DEVICE("Device", Icons.Filled.Build),
    ABOUT("About", Icons.Filled.Info),
}

@Composable
fun AppRoot() {
    var tab by rememberSaveable { mutableStateOf(Tab.LIBRARY) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            Tab.LIBRARY -> LibraryScreen(modifier)
            Tab.DEVICE -> DeviceScreen(modifier)
            Tab.ABOUT -> AboutScreen(modifier)
        }
    }
}
