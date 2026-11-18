package com.acegears.ace360x.emu

import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Small notice while Android reports the phone is hot (CLAUDE.md §11). We only
 * inform: Android itself decides throttling; nothing here bypasses it.
 */
object ThermalMessages {
    /** Message for a PowerManager.THERMAL_STATUS_* value, or null if no notice is needed. */
    fun forStatus(status: Int): String? = when {
        status >= PowerManager_THERMAL_STATUS_SEVERE ->
            "Phone is very hot — Android is slowing it down. Consider pausing to let it cool."
        status >= PowerManager_THERMAL_STATUS_MODERATE ->
            "Phone is getting warm — the game may slow down."
        else -> null
    }

    // Mirrors android.os.PowerManager constants so this stays JVM-testable.
    const val PowerManager_THERMAL_STATUS_MODERATE = 2
    const val PowerManager_THERMAL_STATUS_SEVERE = 3
}

@Composable
fun ThermalNotice(status: Int, modifier: Modifier = Modifier) {
    val text = ThermalMessages.forStatus(status) ?: return
    val bg = if (status >= PowerManager.THERMAL_STATUS_SEVERE) Color(0xCC8B1E1E) else Color(0xCC7A5A12)
    Text(
        text,
        color = Color.White,
        fontSize = 13.sp,
        modifier = modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
