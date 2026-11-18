package com.acegears.ace360x.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acegears.ace360x.BuildConfig
import com.acegears.ace360x.core.NativeBridge
import com.acegears.ace360x.diag.ShareDiagnosticsButton

private val sections = listOf(
    "Status" to "Early development (Phase 3 of 12). Working today: game folder scanning, " +
        "file identification, a device capability check and an emulation-core self-test. " +
        "Not yet working: running games. Compatibility for every title is Untested.",
    "Legal" to "Ace360X does not include any Xbox 360 firmware, keys or games. " +
        "Use only backups of games you own. Xbox and Xbox 360 are trademarks of Microsoft; " +
        "this project is not affiliated with or endorsed by Microsoft.",
    "Privacy" to "No ads, no tracking, no analytics, no internet access. " +
        "Ace360X only reads the folders you choose.",
    "Open-source licences" to "Ace360X — BSD 3-Clause.\n" +
        "Xenia Canary — BSD 3-Clause, © Ben Vanik and Xenia contributors.\n" +
        "FFmpeg (XMA audio decoding, built from source) — LGPL 2.1+.\n" +
        "fmt, xxhash, imgui, glslang, Vulkan-Headers, VulkanMemoryAllocator, capstone, " +
        "xbyak_aarch64, zstd, zlib-ng, snappy and other libraries bundled by Xenia — " +
        "see docs/LICENSES.md in the source repository.\n" +
        "AndroidX, Jetpack Compose, Kotlin coroutines — Apache 2.0.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text("About") })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Ace360X", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Version ${BuildConfig.VERSION_NAME} · native library " +
                        if (NativeBridge.isLoaded) "loaded" else "FAILED: ${NativeBridge.loadError}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Diagnostics", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Creates a text report (emulator log, recent app log lines, why recent " +
                                "app processes ended, phone model) and opens the share sheet. " +
                                "Nothing is sent unless you choose a destination.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                        ShareDiagnosticsButton()
                    }
                }
            }
            sections.forEach { (title, body) ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(title, style = MaterialTheme.typography.titleSmall)
                            Text(body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
