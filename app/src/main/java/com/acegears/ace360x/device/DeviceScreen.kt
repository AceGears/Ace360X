package com.acegears.ace360x.device

import com.acegears.ace360x.theme.AceCard
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.acegears.ace360x.core.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class ThermalSample(val status: Int, val headroom: Float?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var vulkan by remember { mutableStateOf<JSONObject?>(null) }
    var thermal by remember { mutableStateOf<ThermalSample?>(null) }

    LaunchedEffect(Unit) {
        vulkan = withContext(Dispatchers.Default) {
            runCatching { JSONObject(NativeBridge.probeVulkanJson()) }
                .getOrElse { JSONObject().put("error", "Unreadable probe result") }
        }
    }
    // Thermal sampling only while this screen is visible and the app is resumed;
    // 2 s interval respects the platform's headroom rate limit (CLAUDE.md §11).
    LaunchedEffect(lifecycle) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val headroom = if (Build.VERSION.SDK_INT >= 30) {
                    pm.getThermalHeadroom(10).takeUnless { it.isNaN() }
                } else null
                thermal = ThermalSample(pm.currentThermalStatus, headroom)
                delay(2_000)
            }
        }
    }

    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mem = remember { ActivityManager.MemoryInfo().also(am::getMemoryInfo) }
    val vk = vulkan
    val checks = DeviceChecks.evaluate(
        ramBytes = mem.totalMem,
        vulkanApi = vk?.optString("apiVersion")?.ifBlank { null },
        supportedAbis = Build.SUPPORTED_ABIS.toList(),
    )

    Column(modifier.fillMaxSize()) {
        com.acegears.ace360x.theme.AceTopBar("Device and diagnostics", onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                InfoCard("Provisional minimum") {
                    checks.forEach { c -> CheckRow(c) }
                    Text(
                        "Meeting these doesn't mean games will run well. Real performance " +
                            "has to be measured per game, and none has been measured yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            item {
                InfoCard("Phone") {
                    KeyValue("Model", "${Build.MANUFACTURER} ${Build.MODEL}")
                    if (Build.VERSION.SDK_INT >= 31) KeyValue("SoC", "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}")
                    KeyValue("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    KeyValue("RAM", "%.1f GB".format(mem.totalMem / 1e9))
                }
            }
            item {
                InfoCard("Thermal") {
                    val t = thermal
                    val headroom = t?.headroom
                    KeyValue("Status", t?.let { DeviceChecks.thermalStatusName(it.status) } ?: "…")
                    KeyValue(
                        "Headroom",
                        when {
                            Build.VERSION.SDK_INT < 30 -> "Needs Android 11+"
                            t == null -> "…"
                            headroom == null -> "Not reported by this phone"
                            else -> "%.2f (1.0 = throttling)".format(headroom)
                        },
                    )
                }
            }
            item { CoreSelfTestCard() }
            item { GpuTraceCard() }
            item { VulkanCard(vk) }
        }
    }
}

@Composable
private fun VulkanCard(vk: JSONObject?) {
    InfoCard("Vulkan GPU") {
        when {
            vk == null -> Text("Probing…")
            vk.has("error") -> Text(vk.getString("error"), color = MaterialTheme.colorScheme.error)
            else -> {
                KeyValue("GPU", vk.optString("deviceName"))
                KeyValue("Vendor", DeviceChecks.vendorName(vk.optLong("vendorId")))
                KeyValue("Vulkan API", vk.optString("apiVersion"))
                val driver = listOf(vk.optString("driverName"), vk.optString("driverInfo"))
                    .filter { it.isNotBlank() }.joinToString(" · ")
                KeyValue("Driver", driver.ifBlank { "raw ${vk.optLong("driverVersionRaw")}" })
                KeyValue("Extensions", vk.optInt("extensionCount").toString())
                val features = vk.optJSONObject("features")
                val exts = vk.optJSONObject("extensions")
                listOfNotNull(features, exts).forEach { obj ->
                    obj.keys().forEach { key -> KeyValue(key, if (obj.optBoolean(key)) "Yes" else "No") }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    AceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            key,
            Modifier.weight(0.45f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, Modifier.weight(0.55f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CheckRow(c: DeviceChecks.Check) {
    val (mark, color) = when (c.passed) {
        true -> "✓" to Color(0xFF8BD39B)
        false -> "✗" to MaterialTheme.colorScheme.error
        null -> "?" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(Modifier.fillMaxWidth()) {
        Text(mark, color = color, modifier = Modifier.padding(end = 8.dp))
        Text(c.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(c.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
