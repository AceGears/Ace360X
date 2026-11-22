package com.acegears.ace360x.diag

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.acegears.ace360x.core.NativeBridge
import com.acegears.ace360x.theme.AceCard
import com.acegears.ace360x.theme.AceCircularProgress
import com.acegears.ace360x.theme.AceTheme
import com.acegears.ace360x.theme.ThemeCustomization
import java.io.File
import kotlin.concurrent.thread

/**
 * Replays the newest GPU frame capture on this device several times, each with
 * different GPU cvars, and shows/shares the resulting images. Whichever variant
 * produces a correct picture points at the cause of a black game image.
 * Runs in its own process (:replay) and kills it on exit.
 */
class TraceReplayActivity : ComponentActivity() {
    data class Variant(val label: String, val cvars: List<Pair<String, String>>)
    data class Outcome(val variant: Variant, val png: File?, val error: String, val millis: Long)

    private val outcomes = mutableStateListOf<Outcome>()
    private val running = mutableStateOf(true)
    private val status = mutableStateOf("Starting…")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NativeBridge.initCore(this)
        val trace = GpuTrace.traces(this).lastOrNull()
        setContent { AceTheme(ThemeCustomization()) { Screen() } }
        if (trace == null) {
            status.value = "No capture found. Capture frames on the Device tab first."
            running.value = false
            return
        }
        val outDir = File(cacheDir, "share/gputrace-replay").apply {
            deleteRecursively()
            mkdirs()
        }
        thread(name = "trace-replay") {
            val done = mutableListOf<Outcome>()
            VARIANTS.forEachIndexed { i, v ->
                status.value = "Replaying ${trace.name}: ${i + 1} of ${VARIANTS.size} (${v.label})"
                val png = File(outDir, "replay-%02d.png".format(i + 1))
                val start = System.currentTimeMillis()
                val error = runCatching { NativeBridge.replayTrace(trace.absolutePath, png.absolutePath, v.cvars) }
                    .getOrElse { "exception: ${it.message}" }
                val took = System.currentTimeMillis() - start
                val outcome = Outcome(v, png.takeIf { it.exists() }, error, took)
                done += outcome
                runOnUiThread { outcomes += outcome }
            }
            // From the thread's own list: the UI list may not have the last result yet.
            File(outDir, "replay-results.txt").writeText(
                done.joinToString("\n", prefix = "Replay of ${trace.name}\n") { o ->
                    "${o.variant.label} [${o.variant.cvars.joinToString { "${it.first}=${it.second}" }}]: " +
                        (if (o.png != null) "image ${o.png.name}" else "no image") +
                        (if (o.error.isNotEmpty()) " error: ${o.error}" else "") + " (${o.millis} ms)"
                },
            )
            status.value = "Done. Share the images and results file, then share diagnostics " +
                "from the About tab (it contains the validation messages)."
            running.value = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Xenia's process-wide state isn't meant to be torn down and reused.
        if (isFinishing) Process.killProcess(Process.myPid())
    }

    private fun share() {
        val dir = File(cacheDir, "share/gputrace-replay")
        val files = dir.listFiles()?.sortedBy { it.name }.orEmpty()
        val uris = ArrayList(files.map { FileProvider.getUriForFile(this, "$packageName.diagnostics", it) })
        val send = Intent(Intent.ACTION_SEND_MULTIPLE)
            .setType("*/*")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, "Share replay results"))
    }

    @Composable
    private fun Screen() {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Replay capture on this phone", style = MaterialTheme.typography.titleLarge)
            Text(status.value, style = MaterialTheme.typography.bodyMedium)
            if (running.value) AceCircularProgress()
            if (!running.value && outcomes.isNotEmpty()) Button(onClick = ::share) { Text("Share results") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(outcomes) { o ->
                    AceCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(o.variant.label, style = MaterialTheme.typography.titleSmall)
                            if (o.error.isNotEmpty()) Text(o.error, color = MaterialTheme.colorScheme.error)
                            o.png?.let { f ->
                                BitmapFactory.decodeFile(f.absolutePath)?.let { bmp ->
                                    Image(bmp.asImageBitmap(), contentDescription = o.variant.label, Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        /** Variants to compare; each changes one GPU path that differs between drivers. */
        val VARIANTS = listOf(
            Variant("1 Default settings", emptyList()),
            Variant("2 No sparse shared memory", listOf("vulkan_sparse_shared_memory" to "false")),
            Variant("3 Gamma render target as 8-bit", listOf("gamma_render_target_as_unorm16" to "false")),
            Variant("4 No discard in transfer pipelines", listOf("no_discard_stencil_in_transfer_pipelines" to "true")),
            Variant("5 No 3D-to-2D texture", listOf("gpu_3d_to_2d_texture" to "false")),
            Variant("6 Depth float24 in pixel shader", listOf("depth_float24_convert_in_pixel_shader" to "true")),
            Variant(
                "7 All of the above",
                listOf(
                    "vulkan_sparse_shared_memory" to "false",
                    "gamma_render_target_as_unorm16" to "false",
                    "no_discard_stencil_in_transfer_pipelines" to "true",
                    "gpu_3d_to_2d_texture" to "false",
                    "depth_float24_convert_in_pixel_shader" to "true",
                ),
            ),
            Variant(
                "8 Default + Vulkan validation (writes errors to the log; slower)",
                listOf("vulkan_validation" to "true"),
            ),
        )
    }
}
