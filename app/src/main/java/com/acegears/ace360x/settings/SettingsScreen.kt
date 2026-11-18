package com.acegears.ace360x.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context.applicationContext) }
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = EmulatorSettings())
    val scope = rememberCoroutineScope()
    fun update(t: (EmulatorSettings) -> EmulatorSettings) = scope.launch { repo.update(t) }
    var confirmVsyncOff by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(
                    "Changes apply the next time you start a game.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Section("Graphics") {
                    EmulatorSettings.GraphicsPreset.entries.forEach { preset ->
                        val selected = settings.graphicsPreset == preset
                        RadioRow(
                            title = preset.label,
                            subtitle = preset.description,
                            selected = selected,
                            enabled = preset != EmulatorSettings.GraphicsPreset.CUSTOM || selected,
                            onClick = { update { it.withPreset(preset) } },
                        )
                    }
                    SubHeading("Internal resolution")
                    (1..EmulatorSettings.MAX_RESOLUTION_SCALE).forEach { scale ->
                        RadioRow(
                            title = if (scale == 1) "1× (native)" else "$scale× (experimental)",
                            subtitle = if (scale == 1) null else "Sharper image, $scale× the pixels per axis. Needs a strong GPU.",
                            selected = settings.resolutionScale == scale,
                            onClick = { update { it.copy(resolutionScale = scale) } },
                        )
                    }
                    SubHeading("Anti-aliasing")
                    EmulatorSettings.AntiAliasing.entries.forEach { aa ->
                        RadioRow(aa.label, null, settings.antiAliasing == aa, onClick = { update { it.copy(antiAliasing = aa) } })
                    }
                    SubHeading("Sharpening / upscaling")
                    EmulatorSettings.Sharpening.entries.forEach { sh ->
                        RadioRow(sh.label, null, settings.sharpening == sh, onClick = { update { it.copy(sharpening = sh) } })
                    }
                    SwitchRow(
                        "Keep aspect ratio",
                        "Black bars instead of stretching to fill the screen.",
                        settings.keepAspectRatio,
                    ) { v -> update { it.copy(keepAspectRatio = v) } }
                }
            }
            item {
                Section("Audio") {
                    SwitchRow("Mute game audio", null, settings.mute) { v -> update { it.copy(mute = v) } }
                }
            }
            item {
                Section("Touch controls") {
                    SubHeading("When a game starts")
                    EmulatorSettings.TouchDefault.entries.forEach { td ->
                        RadioRow(td.label, null, settings.touchDefault == td, onClick = { update { it.copy(touchDefault = td) } })
                    }
                    SliderRow(
                        "Opacity",
                        "${(settings.touchOpacity * 100).roundToInt()}%",
                        settings.touchOpacity,
                        EmulatorSettings.MIN_TOUCH_OPACITY..1f,
                    ) { v -> update { it.copy(touchOpacity = v) } }
                    SliderRow(
                        "Size",
                        "${(settings.touchScale * 100).roundToInt()}%",
                        settings.touchScale,
                        EmulatorSettings.MIN_TOUCH_SCALE..EmulatorSettings.MAX_TOUCH_SCALE,
                    ) { v -> update { it.copy(touchScale = v) } }
                }
            }
            item {
                Section("Advanced") {
                    SwitchRow(
                        "Guest VSync (60 Hz pacing)",
                        "Xenia uses this to pace the game's own frame timing. Turning it off can make " +
                            "games run too fast or behave incorrectly. Leave on unless you know you need it.",
                        settings.guestVsync,
                    ) { v -> if (!v) confirmVsyncOff = true else update { it.copy(guestVsync = true) } }
                    TextButton(onClick = { confirmReset = true }) { Text("Restore defaults") }
                }
            }
        }
    }

    if (confirmVsyncOff) {
        AlertDialog(
            onDismissRequest = { confirmVsyncOff = false },
            title = { Text("Turn off guest VSync?") },
            text = { Text("Many games will run at the wrong speed. This is mainly useful for testing.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmVsyncOff = false
                    update { it.copy(guestVsync = false) }
                }) { Text("Turn off") }
            },
            dismissButton = { TextButton(onClick = { confirmVsyncOff = false }) { Text("Cancel") } },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Restore default settings?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch { repo.reset() }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            content()
        }
    }
}

@Composable
private fun SubHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun RadioRow(title: String, subtitle: String?, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    var local by remember(value) { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = range,
        )
    }
}
