package com.acegears.ace360x.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acegears.ace360x.settings.EmulatorSettings.AntiAliasing
import com.acegears.ace360x.settings.EmulatorSettings.Sharpening
import com.acegears.ace360x.theme.AceDialog
import com.acegears.ace360x.theme.aceFocusRing

/**
 * Per-game settings. Each option is either "Default" (follow the Settings tab,
 * whose current value is shown) or a fixed value for this game only.
 */
@Composable
fun GameSettingsDialog(
    gameName: String,
    global: EmulatorSettings,
    initial: GameOverrides,
    onSave: (GameOverrides) -> Unit,
    onDismiss: () -> Unit,
) {
    var o by remember { mutableStateOf(initial) }
    AceDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings for $gameName", maxLines = 2) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "\"Default\" follows the app's Settings. Other choices apply to this game only, " +
                        "the next time you start it.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Choice(
                    "Resolution", "${global.resolutionScale}×", o.resolutionScale,
                    (1..EmulatorSettings.MAX_RESOLUTION_SCALE).map { it to "$it×" },
                ) { o = o.copy(resolutionScale = it) }
                Choice(
                    "Anti-aliasing", global.antiAliasing.label, o.antiAliasing,
                    AntiAliasing.entries.map { it to it.label },
                ) { o = o.copy(antiAliasing = it) }
                Choice(
                    "Sharpening", global.sharpening.label, o.sharpening,
                    Sharpening.entries.map { it to it.label },
                ) { o = o.copy(sharpening = it) }
                Choice(
                    "Picture shape", if (global.keepAspectRatio) "Keep shape" else "Stretch", o.keepAspectRatio,
                    listOf(true to "Keep shape", false to "Stretch"),
                ) { o = o.copy(keepAspectRatio = it) }
                Choice(
                    "Sound", if (global.mute) "Muted" else "On", o.mute,
                    listOf(false to "On", true to "Muted"),
                ) { o = o.copy(mute = it) }
                Choice(
                    "Guest VSync", if (global.guestVsync) "On" else "Off", o.guestVsync,
                    listOf(true to "On", false to "Off"),
                ) { o = o.copy(guestVsync = it) }
                Choice(
                    "Missing graphics fix (Experimental)", "Off", o.allowUnmappedGpuReads,
                    listOf(false to "Off", true to "On"),
                ) { o = o.copy(allowUnmappedGpuReads = it) }
                if (o.allowUnmappedGpuReads == true) {
                    Text(
                        "Experimental: draws things the emulator would otherwise skip because their " +
                            "data sits in memory it considers unreadable. May fix black or missing " +
                            "parts in some games; may also crash the game.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (o.guestVsync == false) {
                    Text(
                        "With guest VSync off, this game may run too fast.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(o) }) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = { o = GameOverrides() }, enabled = !o.isEmpty) { Text("Clear all") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun <T> Choice(
    title: String,
    globalLabel: String,
    selected: T?,
    options: List<Pair<T, String>>,
    onSelect: (T?) -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("Default ($globalLabel)") },
                modifier = Modifier.heightIn(min = 48.dp).padding(vertical = 4.dp).aceFocusRing(),
            )
            options.forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                    modifier = Modifier.heightIn(min = 48.dp).padding(vertical = 4.dp).aceFocusRing(),
                )
            }
        }
    }
}
