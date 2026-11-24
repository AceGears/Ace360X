package com.acegears.ace360x.emu

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acegears.ace360x.core.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/** Where the last session's summary is kept for the diagnostics report. */
const val PERF_SESSION_FILE = "perf-last.txt"

/**
 * Samples the game's performance once a second (CLAUDE.md §7, §11: 1 Hz, nothing
 * animated) for the whole session and keeps a summary on disk for the diagnostics report.
 * With [show] it also displays the current numbers. Takes no touches.
 */
@Composable
fun PerfMonitor(show: Boolean, title: String, thermalStatus: State<Int>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var line by remember { mutableStateOf("Measuring…") }
    LaunchedEffect(Unit) {
        val history = ArrayDeque<Long>()
        val session = PerfSession(title)
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val file = File(context.filesDir, PERF_SESSION_FILE)
        var tick = 0
        var headroom = Float.NaN
        while (true) {
            delay(1000)
            val raw = NativeBridge.drainPerf() ?: continue
            tick++
            // Android asks not to poll headroom more than once a second; every 5 s is plenty.
            if (tick % 5 == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                headroom = power.getThermalHeadroom(10)
            }
            session.add(raw, thermalStatus.value, headroom)
            if (show) line = PerfStats.summarize(raw, history).line()
            // The game process can be killed at any time, so keep the file current.
            if (tick % 5 == 0) {
                val text = session.report()
                withContext(Dispatchers.IO) { runCatching { file.writeText(text) } }
            }
        }
    }
    if (show) {
        Text(
            line,
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = modifier.background(Color(0x99000000), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
