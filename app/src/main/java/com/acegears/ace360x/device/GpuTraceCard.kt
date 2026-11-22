package com.acegears.ace360x.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.acegears.ace360x.diag.GpuTrace
import com.acegears.ace360x.theme.AceCard
import com.acegears.ace360x.theme.AceSwitch
import java.io.File

/** Device tab: arm a one-shot GPU frame capture and share the resulting traces. */
@Composable
fun GpuTraceCard() {
    val context = LocalContext.current
    var armed by remember { mutableStateOf(GpuTrace.isArmed(context)) }
    var traces by remember { mutableStateOf<List<File>>(emptyList()) }
    LifecycleResumeEffect(Unit) {
        armed = GpuTrace.isArmed(context)
        traces = GpuTrace.traces(context)
        onPauseOrDispose { }
    }
    AceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Graphics frame capture", style = MaterialTheme.typography.titleMedium)
            Text(
                "For debugging graphics problems. The next game you start records two frames " +
                    "(about 15 and 30 seconds in) to files you can share. Keep the game running " +
                    "for at least 35 seconds. Recording makes the game pause briefly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Capture on next game start", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                AceSwitch(checked = armed, onCheckedChange = { v ->
                    GpuTrace.setArmed(context, v)
                    armed = v
                })
            }
            if (traces.isNotEmpty()) {
                val mb = traces.sumOf { it.length() } / (1024.0 * 1024.0)
                Text(
                    "${traces.size} captured frame(s), ${"%.1f".format(mb)} MB. They contain the " +
                        "game's graphics data; share them only for debugging.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { context.startActivity(GpuTrace.shareIntent(context, traces)) }) {
                        Text("Share captures")
                    }
                    TextButton(onClick = {
                        GpuTrace.delete(context)
                        traces = emptyList()
                    }) { Text("Delete") }
                }
            }
        }
    }
}
