package com.acegears.ace360x.theme

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeStore by preferencesDataStore(name = "theme")

/**
 * Stores [ThemeCustomization] as a diff over the base theme: a key is present only
 * when the user changed that value. Versioned by [ThemeCustomization.SCHEMA_VERSION];
 * a diff from another schema version is discarded (default theme and values).
 * Main process only; the game process gets the customisation in its launch Intent.
 */
class ThemeRepository(private val context: Context) {
    private object Keys {
        val schema = intPreferencesKey("schema")
        val base = stringPreferencesKey("base")
        val mode = stringPreferencesKey("mode")
        val glow = intPreferencesKey("glow")
        val gradient = intPreferencesKey("gradient")
        val border = intPreferencesKey("border")
        val corner = intPreferencesKey("corner")
        val glass = booleanPreferencesKey("glass")
        val textScale = intPreferencesKey("text_scale")
        val animationSpeed = intPreferencesKey("animation_speed")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val liquidMotion = booleanPreferencesKey("liquid_motion")
        val deleteEffect = booleanPreferencesKey("delete_effect")

        /** Seven comma-separated ARGB hex values, as in [ThemeCustomization.encode]. */
        val customColors = stringPreferencesKey("custom_colors")
    }

    val customization: Flow<ThemeCustomization> = context.themeStore.data.map(::fromPrefs)

    suspend fun update(transform: (ThemeCustomization) -> ThemeCustomization) {
        context.themeStore.edit { prefs ->
            val c = transform(fromPrefs(prefs))
            prefs[Keys.schema] = ThemeCustomization.SCHEMA_VERSION
            prefs[Keys.base] = c.baseId
            prefs.setOrRemove(Keys.mode, c.mode?.name)
            prefs.setOrRemove(Keys.glow, c.glow)
            prefs.setOrRemove(Keys.gradient, c.gradient)
            prefs.setOrRemove(Keys.border, c.border)
            prefs.setOrRemove(Keys.corner, c.corner)
            prefs.setOrRemove(Keys.glass, c.glass)
            prefs.setOrRemove(Keys.textScale, c.textScale)
            prefs.setOrRemove(Keys.animationSpeed, c.animationSpeed)
            prefs.setOrRemove(Keys.reduceMotion, c.reduceMotion)
            prefs.setOrRemove(Keys.liquidMotion, c.liquidMotion)
            prefs.setOrRemove(Keys.deleteEffect, c.deleteEffect)
            prefs.setOrRemove(
                Keys.customColors,
                c.customColors?.asList?.joinToString(",") { java.lang.Long.toHexString(it and 0xFFFFFFFFL) },
            )
        }
    }

    /** Selecting another theme keeps the user's adjustments but returns to that theme's own mode. */
    suspend fun selectBase(id: String) = update { it.copy(baseId = Themes.byId(id).id, mode = null) }

    /** Back to the defaults; the Custom theme's colours are kept. */
    suspend fun resetCustomizations() = update { ThemeCustomization(baseId = it.baseId, customColors = it.customColors) }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private fun fromPrefs(p: Preferences): ThemeCustomization {
        // Themes from before schema 2 no longer exist, so an older store starts from the default.
        if (p[Keys.schema] != ThemeCustomization.SCHEMA_VERSION) return ThemeCustomization()
        // Out-of-range values are dropped by the same rules as the Intent form.
        return ThemeCustomization.decode(
            ThemeCustomization(
                baseId = Themes.byId(p[Keys.base]).id,
                mode = p[Keys.mode]?.let { n -> ThemeMode.entries.firstOrNull { it.name == n } },
                glow = p[Keys.glow],
                gradient = p[Keys.gradient],
                border = p[Keys.border],
                corner = p[Keys.corner],
                glass = p[Keys.glass],
                textScale = p[Keys.textScale],
                animationSpeed = p[Keys.animationSpeed],
                reduceMotion = p[Keys.reduceMotion],
                liquidMotion = p[Keys.liquidMotion],
                deleteEffect = p[Keys.deleteEffect],
                customColors = p[Keys.customColors]?.split(",")
                    ?.mapNotNull { it.toLongOrNull(16) }?.let { BrandColors.fromList(it) },
            ).encode(),
        )
    }
}
