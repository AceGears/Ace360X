package com.acegears.ace360x.device

import com.acegears.ace360x.theme.AceCircularProgress
import com.acegears.ace360x.theme.AceCard
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.acegears.ace360x.core.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class SelfTestStep(val name: String, val ok: Boolean, val detail: String, val ms: Double)

private sealed interface SelfTestState {
    data object Idle : SelfTestState
    data object Running : SelfTestState
    data object CrashedLastTime : SelfTestState
    data class Done(val steps: List<SelfTestStep>, val ok: Boolean, val error: String?) : SelfTestState
}

private const val PREFS = "core_selftest"
private const val KEY_RUNNING = "running"

/**
 * Runs the native core self-test: guest memory reservation, guest page access,
 * ARM64 JIT start-up, and translation + execution of a tiny PowerPC function.
 * A native crash would kill the app, so a "running" flag is written first; if
 * it is still set on the next launch, the crash is reported instead of hidden.
 */
@Composable
fun CoreSelfTestCard() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var state by remember {
        val crashed = prefs.getBoolean(KEY_RUNNING, false)
        if (crashed) prefs.edit().putBoolean(KEY_RUNNING, false).commit()
        mutableStateOf(if (crashed) SelfTestState.CrashedLastTime else SelfTestState.Idle)
    }

    AceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Emulation core self-test", style = MaterialTheme.typography.titleSmall)
            Text(
                "Checks that Xenia's foundations work on this phone: reserving the Xbox 360 " +
                    "memory space and running PowerPC code through the ARM64 JIT. " +
                    "It does not test any game.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (val s = state) {
                SelfTestState.CrashedLastTime -> Text(
                    "The previous self-test crashed the app. Please report this with your phone model.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                is SelfTestState.Done -> ResultView(s)
                else -> Unit
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    enabled = state != SelfTestState.Running && NativeBridge.hasCore,
                    onClick = {
                        state = SelfTestState.Running
                        scope.launch {
                            prefs.edit().putBoolean(KEY_RUNNING, true).commit()
                            val json = withContext(Dispatchers.Default) { NativeBridge.runCoreSelfTestJson() }
                            prefs.edit().putBoolean(KEY_RUNNING, false).commit()
                            state = parse(json)
                        }
                    },
                ) { Text(if (state is SelfTestState.Done) "Run again" else "Run self-test") }
                if (state == SelfTestState.Running) {
                    AceCircularProgress(Modifier.padding(start = 12.dp).padding(4.dp))
                }
            }
            if (!NativeBridge.hasCore) {
                Text("This build doesn't include the emulation core.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ResultView(s: SelfTestState.Done) {
    s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    s.steps.forEach { step ->
        Row(Modifier.fillMaxWidth()) {
            Text(
                if (step.ok) "✓" else "✗",
                color = if (step.ok) Color(0xFF8BD39B) else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(step.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${step.detail} · %.1f ms".format(step.ms),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (s.error == null) {
        Text(
            if (s.ok) "All core checks passed." else "Some core checks failed.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun parse(json: String): SelfTestState.Done = try {
    val o = JSONObject(json)
    if (o.has("error")) {
        SelfTestState.Done(emptyList(), false, o.getString("error"))
    } else {
        val arr = o.getJSONArray("steps")
        val steps = (0 until arr.length()).map { i ->
            val st = arr.getJSONObject(i)
            SelfTestStep(st.getString("name"), st.getBoolean("ok"), st.optString("detail"), st.optDouble("ms"))
        }
        SelfTestState.Done(steps, o.optBoolean("ok"), null)
    }
} catch (e: Exception) {
    SelfTestState.Done(emptyList(), false, "Unreadable result: ${e.message}")
}
