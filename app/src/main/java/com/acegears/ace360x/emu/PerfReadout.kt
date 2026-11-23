package com.acegears.ace360x.emu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acegears.ace360x.core.NativeBridge
import kotlinx.coroutines.delay

/**
 * Measured numbers over the game, refreshed once a second (CLAUDE.md §7, §11: sampling at
 * 1 Hz, no continuous animation). Text only; takes no touches.
 */
@Composable
fun PerfReadout(modifier: Modifier = Modifier) {
    var line by remember { mutableStateOf("Measuring…") }
    LaunchedEffect(Unit) {
        val history = ArrayDeque<Long>()
        while (true) {
            delay(1000)
            val raw = NativeBridge.drainPerf() ?: continue
            line = PerfStats.summarize(raw, history).line()
        }
    }
    Text(
        line,
        color = Color.White,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        modifier = modifier.background(Color(0x99000000), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
