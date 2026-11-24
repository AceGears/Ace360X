package com.acegears.ace360x.library

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.acegears.ace360x.diag.GpuTrace
import com.acegears.ace360x.emu.EmulatorActivity
import com.acegears.ace360x.settings.EmulatorSettings
import com.acegears.ace360x.settings.GameOverrides
import com.acegears.ace360x.settings.GameOverridesStore
import com.acegears.ace360x.settings.GameSettingsDialog
import com.acegears.ace360x.settings.SettingsRepository
import com.acegears.ace360x.theme.ThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Starts [game] with the global settings plus its own overrides. False if it can't be started. */
suspend fun launchGame(context: Context, game: GameEntry): Boolean {
    val target = game.launchTarget() ?: return false
    val app = context.applicationContext
    val gameKey = game.uri.toString()
    val global = SettingsRepository(app).current()
    val settings = withContext(Dispatchers.IO) { GameOverridesStore.read(app, gameKey) }.applyTo(global)
    val theme = ThemeRepository(app).customization.first()
    context.startActivity(
        EmulatorActivity.launchIntent(
            context, target, game.title, settings, statsKey = gameKey, theme = theme,
            gpuTraceDir = GpuTrace.consumeForLaunch(app),
        ),
    )
    return true
}

fun lastPlayedLabel(millis: Long): String? {
    if (millis <= 0) return null
    return "Last played " + DateUtils.getRelativeTimeSpanString(
        millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
    )
}

/** One line about a game: format, size and compatibility status (always Untested for now). */
fun gameMetaLine(game: GameEntry): String =
    "${game.format.label} · ${ScanPolicy.formatSize(game.sizeBytes)} · Untested"

/**
 * Per-game settings dialog for [game], shown while [open] is true. Loads the game's
 * overrides and the global settings, and saves the result.
 */
@Composable
fun GameSettingsHost(game: GameEntry, open: Boolean, onClose: () -> Unit) {
    if (!open) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gameKey = game.uri.toString()
    // Null until both are loaded from disk.
    var loaded by remember(gameKey) { mutableStateOf<Pair<EmulatorSettings, GameOverrides>?>(null) }
    LaunchedEffect(gameKey) {
        val app = context.applicationContext
        val global = SettingsRepository(app).current()
        val overrides = withContext(Dispatchers.IO) { GameOverridesStore.read(app, gameKey) }
        loaded = global to overrides
    }
    loaded?.let { (global, overrides) ->
        GameSettingsDialog(
            gameName = game.title,
            global = global,
            initial = overrides,
            onSave = { o ->
                scope.launch(Dispatchers.IO) { GameOverridesStore.write(context.applicationContext, gameKey, o) }
                onClose()
            },
            onDismiss = onClose,
        )
    }
}
