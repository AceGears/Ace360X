package com.acegears.ace360x.settings

/**
 * User settings that affect a game session. Every field maps to a real Xenia
 * cvar or to real behaviour of our game screen (CLAUDE.md §9: no dead controls).
 * Pure Kotlin so the mapping is unit-tested on the JVM.
 */
data class EmulatorSettings(
    /** draw_resolution_scale_x/y: 1 = native 720p-class rendering. */
    val resolutionScale: Int = 1,
    val antiAliasing: AntiAliasing = AntiAliasing.OFF,
    val sharpening: Sharpening = Sharpening.OFF,
    /** present_letterbox: keep the game's aspect ratio (bars) instead of stretching. */
    val keepAspectRatio: Boolean = true,
    /** mute: silence game audio. */
    val mute: Boolean = false,
    /**
     * vsync: in Xenia this paces the *guest's* vertical blank at 60 Hz. Turning
     * it off can make games run too fast. Advanced, off-by-warning.
     */
    val guestVsync: Boolean = true,
    val touchOpacity: Float = 0.75f,
    val touchScale: Float = 1.0f,
    val touchDefault: TouchDefault = TouchDefault.AUTO,
    /** Physical controller stick dead zone (radial, fraction of full deflection). */
    val stickDeadZone: Float = com.acegears.ace360x.emu.GamepadMapper.DEFAULT_DEAD_ZONE,
    /** Xbox button bit → Android key code. */
    val buttonBindings: Map<Int, Int> = com.acegears.ace360x.emu.GamepadMapper.DEFAULT_BINDINGS,
) {
    enum class AntiAliasing(val cvar: String, val label: String) {
        OFF("", "Off"),
        FXAA("fxaa", "FXAA"),
        FXAA_EXTREME("fxaa_extreme", "FXAA (extreme)"),
    }

    enum class Sharpening(val cvar: String, val label: String) {
        OFF("", "Off"),
        CAS("cas", "AMD FidelityFX CAS"),
        FSR("fsr", "AMD FidelityFX FSR"),
    }

    enum class TouchDefault(val label: String) {
        AUTO("Show unless a controller is connected"),
        ALWAYS("Always show"),
        NEVER("Start hidden"),
    }

    /** Graphics presets. Each sets only real options; anything else is Custom. */
    enum class GraphicsPreset(val label: String, val description: String) {
        ORIGINAL("Original", "Native resolution, no post-processing. Lightest on the GPU."),
        ENHANCED("Enhanced", "Native resolution with FXAA and CAS sharpening."),
        HIGH_QUALITY("High quality", "2× resolution with FXAA. Much heavier on the GPU (experimental)."),
        CUSTOM("Custom", "Your own combination."),
    }

    fun withPreset(preset: GraphicsPreset): EmulatorSettings = when (preset) {
        GraphicsPreset.ORIGINAL -> copy(resolutionScale = 1, antiAliasing = AntiAliasing.OFF, sharpening = Sharpening.OFF)
        GraphicsPreset.ENHANCED -> copy(resolutionScale = 1, antiAliasing = AntiAliasing.FXAA, sharpening = Sharpening.CAS)
        GraphicsPreset.HIGH_QUALITY -> copy(resolutionScale = 2, antiAliasing = AntiAliasing.FXAA, sharpening = Sharpening.OFF)
        GraphicsPreset.CUSTOM -> this
    }

    val graphicsPreset: GraphicsPreset
        get() = GraphicsPreset.entries.firstOrNull {
            it != GraphicsPreset.CUSTOM && withPreset(it) == this
        } ?: GraphicsPreset.CUSTOM

    /**
     * Xenia cvars for this session. Value types must match the cvar types,
     * because cvar_android.cc reads Bundle values with the matching getter:
     * Int → int32, Boolean → bool, String → string.
     */
    fun toCvars(): Map<String, Any> = mapOf(
        "draw_resolution_scale_x" to resolutionScale.coerceIn(1, MAX_RESOLUTION_SCALE),
        "draw_resolution_scale_y" to resolutionScale.coerceIn(1, MAX_RESOLUTION_SCALE),
        "postprocess_antialiasing" to antiAliasing.cvar,
        "postprocess_scaling_and_sharpening" to sharpening.cvar,
        "present_letterbox" to keepAspectRatio,
        "mute" to mute,
        "vsync" to guestVsync,
    )

    companion object {
        const val MAX_RESOLUTION_SCALE = 3
        const val MIN_TOUCH_OPACITY = 0.2f
        const val MIN_TOUCH_SCALE = 0.7f
        const val MAX_TOUCH_SCALE = 1.4f
        const val MIN_DEAD_ZONE = 0.02f
        const val MAX_DEAD_ZONE = 0.35f
    }
}
