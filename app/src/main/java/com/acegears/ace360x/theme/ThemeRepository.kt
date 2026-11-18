package com.acegears.ace360x.theme

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeStore by preferencesDataStore(name = "theme")

/**
 * Stores [ThemeCustomization] as a diff over the base theme: a key is present only
 * when the user changed that value. Versioned by [ThemeCustomization.SCHEMA_VERSION];
 * a diff from another schema version is discarded (base theme kept).
 * Main process only; the game process gets the customisation in its launch Intent.
 */
class ThemeRepository(private val context: Context) {
    private object Keys {
        val schema = intPreferencesKey("schema")
        val base = stringPreferencesKey("base")
        val accent = longPreferencesKey("accent")
        val ambiance = booleanPreferencesKey("ambiance")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val cornerScale = floatPreferencesKey("corner_scale")
    }

    val customization: Flow<ThemeCustomization> = context.themeStore.data.map(::fromPrefs)

    suspend fun update(transform: (ThemeCustomization) -> ThemeCustomization) {
        context.themeStore.edit { prefs ->
            val c = transform(fromPrefs(prefs))
            prefs[Keys.schema] = ThemeCustomization.SCHEMA_VERSION
            prefs[Keys.base] = c.baseId
            prefs.setOrRemove(Keys.accent, c.accent)
            prefs.setOrRemove(Keys.ambiance, c.ambianceEnabled)
            prefs.setOrRemove(Keys.reduceMotion, c.reduceMotion)
            prefs.setOrRemove(Keys.cornerScale, c.cornerScale)
        }
    }

    /** Selecting another base theme keeps the user's diff; invalid accents are ignored on resolve. */
    suspend fun selectBase(id: String) = update { it.copy(baseId = Themes.byId(id).id) }

    suspend fun resetCustomizations() = update { ThemeCustomization(baseId = it.baseId) }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private fun fromPrefs(p: Preferences): ThemeCustomization {
        val base = Themes.byId(p[Keys.base]).id
        if (p[Keys.schema] != ThemeCustomization.SCHEMA_VERSION) return ThemeCustomization(baseId = base)
        return ThemeCustomization(
            baseId = base,
            accent = p[Keys.accent],
            ambianceEnabled = p[Keys.ambiance],
            reduceMotion = p[Keys.reduceMotion],
            cornerScale = p[Keys.cornerScale],
        )
    }
}
