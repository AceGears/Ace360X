package com.acegears.ace360x.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acegears.ace360x.theme.AceCard
import com.acegears.ace360x.theme.AceIcons
import com.acegears.ace360x.theme.AceTopBar

/**
 * The parts of Settings, each its own screen (as in AceArcade). [emulation] marks the
 * ones whose options only take effect at the next game start.
 */
enum class SettingsSection(val title: String, val summary: String, val emulation: Boolean = false) {
    APPEARANCE("Appearance", "Theme, dark or light, glow, corners, text size, motion"),
    GRAPHICS("Graphics and sound", "Quality preset, resolution, sharpening, picture shape, mute", emulation = true),
    CONTROLS("Controls", "Controller buttons and on-screen touch controls", emulation = true),
    FOLDERS("Game folders", "Where Ace360X looks for your games"),
    ADVANCED("Advanced", "Performance numbers, guest VSync, restore defaults", emulation = true),
    DEVICE("Device and diagnostics", "Graphics support, self-test, temperature, frame capture"),
    ABOUT("About", "Version, licences, diagnostics report"),
    ;

    companion object {
        fun byName(name: String?): SettingsSection? = entries.firstOrNull { it.name == name }
    }
}

/** Settings home: one card per section. */
@Composable
fun SettingsHomeScreen(onBack: () -> Unit, onSection: (SettingsSection) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        AceTopBar("Settings", onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(SettingsSection.entries, key = { it.name }) { section ->
                AceCard(Modifier.fillMaxWidth(), onClick = { onSection(section) }) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(section.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                section.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(AceIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
