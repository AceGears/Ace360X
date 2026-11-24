package com.acegears.ace360x.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.acegears.ace360x.device.DeviceScreen
import com.acegears.ace360x.library.FoldersScreen
import com.acegears.ace360x.library.GameDetailsScreen
import com.acegears.ace360x.library.LibraryScreen
import com.acegears.ace360x.settings.SettingsHomeScreen
import com.acegears.ace360x.settings.SettingsScreen
import com.acegears.ace360x.settings.SettingsSection
import com.acegears.ace360x.theme.AmbianceBackground
import com.acegears.ace360x.theme.LocalMotion
import com.acegears.ace360x.theme.toEasing

/**
 * Screens are pushed on a small back stack (as in AceArcade: home → game page, home →
 * settings → section). Routes are plain strings so the stack survives rotation and
 * process death: "home", "settings", "section:<SettingsSection>", "game:<uri>".
 */
private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    private const val SECTION = "section:"
    private const val GAME = "game:"
    private const val SEPARATOR = "\n"

    fun section(s: SettingsSection) = SECTION + s.name
    fun game(uri: String) = GAME + uri
    fun sectionOf(route: String): SettingsSection? =
        if (route.startsWith(SECTION)) SettingsSection.byName(route.removePrefix(SECTION)) else null
    fun gameOf(route: String): String? = if (route.startsWith(GAME)) route.removePrefix(GAME) else null

    fun decode(saved: String): List<String> = saved.split(SEPARATOR).filter { it.isNotEmpty() }.ifEmpty { listOf(HOME) }
    fun encode(stack: List<String>): String = stack.joinToString(SEPARATOR)
}

@Composable
fun AppRoot() {
    var saved by rememberSaveable { mutableStateOf(Routes.HOME) }
    // True when the last change pushed a screen, false when it went back.
    var forward by rememberSaveable { mutableStateOf(true) }
    val stack = Routes.decode(saved)
    val motion = LocalMotion.current

    fun push(route: String) {
        forward = true
        saved = Routes.encode(stack + route)
    }
    fun pop() {
        if (stack.size > 1) {
            forward = false
            saved = Routes.encode(stack.dropLast(1))
        }
    }
    BackHandler(enabled = stack.size > 1) { pop() }

    AmbianceBackground {
        // The backdrop gives no default content colour; use the theme's text colour.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            AnimatedContent(
                targetState = stack.last(),
                transitionSpec = {
                    val easing = motion.easing.toEasing()
                    val enter = fadeIn(tween(motion.mediumMs, easing = easing)) +
                        slideInHorizontally(tween(motion.mediumMs, easing = easing)) { width ->
                            if (forward) width / 6 else -width / 6
                        }
                    enter togetherWith fadeOut(tween(motion.mediumMs / 2, easing = easing))
                },
                label = "screen",
            ) { route ->
                Box(Modifier.fillMaxSize()) {
                    val section = Routes.sectionOf(route)
                    val game = Routes.gameOf(route)
                    val pushed = Modifier.navigationBarsPadding()
                    when {
                        route == Routes.SETTINGS ->
                            SettingsHomeScreen(onBack = { pop() }, onSection = { push(Routes.section(it)) }, modifier = pushed)
                        section == SettingsSection.FOLDERS -> FoldersScreen(onBack = { pop() }, modifier = pushed)
                        section == SettingsSection.DEVICE -> DeviceScreen(onBack = { pop() }, modifier = pushed)
                        section == SettingsSection.ABOUT -> AboutScreen(onBack = { pop() }, modifier = pushed)
                        section != null -> SettingsScreen(section = section, onBack = { pop() }, modifier = pushed)
                        game != null -> GameDetailsScreen(gameUri = game, onBack = { pop() }, modifier = pushed)
                        else -> LibraryScreen(
                            onOpenGame = { push(Routes.game(it)) },
                            onOpenSettings = { push(Routes.SETTINGS) },
                            onOpenFolders = { push(Routes.section(SettingsSection.FOLDERS)) },
                        )
                    }
                }
            }
        }
    }
}
