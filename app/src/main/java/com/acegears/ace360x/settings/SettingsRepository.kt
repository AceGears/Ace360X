package com.acegears.ace360x.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

/**
 * Persists [EmulatorSettings]. Used only in the main process (DataStore is not
 * multi-process safe); the game process receives settings via the launch Intent.
 */
class SettingsRepository(private val context: Context) {
    private object Keys {
        val scale = intPreferencesKey("resolution_scale")
        val aa = stringPreferencesKey("anti_aliasing")
        val sharpen = stringPreferencesKey("sharpening")
        val aspect = booleanPreferencesKey("keep_aspect")
        val mute = booleanPreferencesKey("mute")
        val vsync = booleanPreferencesKey("guest_vsync")
        val touchOpacity = floatPreferencesKey("touch_opacity")
        val touchScale = floatPreferencesKey("touch_scale")
        val touchDefault = stringPreferencesKey("touch_default")
    }

    val settings: Flow<EmulatorSettings> = context.settingsStore.data.map(::fromPrefs)

    suspend fun current(): EmulatorSettings = settings.first()

    suspend fun update(transform: (EmulatorSettings) -> EmulatorSettings) {
        context.settingsStore.edit { prefs ->
            val s = transform(fromPrefs(prefs))
            prefs[Keys.scale] = s.resolutionScale
            prefs[Keys.aa] = s.antiAliasing.name
            prefs[Keys.sharpen] = s.sharpening.name
            prefs[Keys.aspect] = s.keepAspectRatio
            prefs[Keys.mute] = s.mute
            prefs[Keys.vsync] = s.guestVsync
            prefs[Keys.touchOpacity] = s.touchOpacity
            prefs[Keys.touchScale] = s.touchScale
            prefs[Keys.touchDefault] = s.touchDefault.name
        }
    }

    suspend fun reset() {
        context.settingsStore.edit { it.clear() }
    }

    private fun fromPrefs(p: Preferences): EmulatorSettings {
        val d = EmulatorSettings()
        return EmulatorSettings(
            resolutionScale = (p[Keys.scale] ?: d.resolutionScale).coerceIn(1, EmulatorSettings.MAX_RESOLUTION_SCALE),
            antiAliasing = enumOr(p[Keys.aa], d.antiAliasing),
            sharpening = enumOr(p[Keys.sharpen], d.sharpening),
            keepAspectRatio = p[Keys.aspect] ?: d.keepAspectRatio,
            mute = p[Keys.mute] ?: d.mute,
            guestVsync = p[Keys.vsync] ?: d.guestVsync,
            touchOpacity = (p[Keys.touchOpacity] ?: d.touchOpacity).coerceIn(EmulatorSettings.MIN_TOUCH_OPACITY, 1f),
            touchScale = (p[Keys.touchScale] ?: d.touchScale)
                .coerceIn(EmulatorSettings.MIN_TOUCH_SCALE, EmulatorSettings.MAX_TOUCH_SCALE),
            touchDefault = enumOr(p[Keys.touchDefault], d.touchDefault),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
}
