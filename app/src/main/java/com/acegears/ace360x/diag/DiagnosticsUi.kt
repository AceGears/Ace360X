package com.acegears.ace360x.diag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Button that builds the diagnostics report and opens the share sheet. */
@Composable
fun ShareDiagnosticsButton(label: String = "Share diagnostics") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            enabled = !busy,
            onClick = {
                busy = true
                error = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { Diagnostics.buildReport(context) } }
                    busy = false
                    result.onSuccess { context.startActivity(Diagnostics.shareIntent(context, it)) }
                        .onFailure { error = "Couldn't build the report: ${it.message}" }
                }
            },
        ) { Text(label) }
        if (busy) CircularProgressIndicator(Modifier.padding(start = 12.dp))
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

/** Shown on the Library when the last game session didn't close normally. */
@Composable
fun UnexpectedExitBanner(onDismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "The last game session ended unexpectedly (crash, or closed by Android).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                "Sharing diagnostics helps find the cause. The report stays on your phone " +
                    "unless you choose where to send it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShareDiagnosticsButton()
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}
