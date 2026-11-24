package com.acegears.ace360x.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Settings → Appearance: theme, dark/light, and the look adjustments from AceArcade's editor. */
@Composable
fun AppearanceSection() {
    val context = LocalContext.current
    val repo = remember { ThemeRepository(context.applicationContext) }
    val custom by repo.customization.collectAsStateWithLifecycle(initialValue = null)
    val c = custom ?: return
    val scope = rememberCoroutineScope()
    fun update(t: (ThemeCustomization) -> ThemeCustomization) = scope.launch { repo.update(t) }
    val active = LocalAceTheme.current
    val cs = MaterialTheme.colorScheme
    val systemDark = isSystemInDarkTheme()

    AceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            Text(
                "Applies immediately to the app screens. Games are not affected.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val offered = remember { Themes.ALL.filter { it.id != Themes.DYNAMIC.id || dynamicColorsAvailable } }
                offered.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { spec ->
                            ThemeTile(
                                spec = spec,
                                dark = if (spec.id == active.spec.id) {
                                    active.palette.dark
                                } else {
                                    ResolvedTheme.wantsDark(ThemeCustomization(baseId = spec.id), systemDark)
                                },
                                selected = spec.id == active.spec.id,
                                modifier = Modifier.weight(1f),
                            ) {
                                scope.launch {
                                    // Custom starts from the colours of the theme you were on.
                                    if (spec.id == Themes.CUSTOM.id && c.customColors == null) {
                                        repo.update { it.copy(customColors = active.variant.colors) }
                                    }
                                    repo.selectBase(spec.id)
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            val isCustom = active.spec.id == Themes.CUSTOM.id
            if (isCustom) {
                Label("Your colours")
                Text(
                    "Six-digit colour codes (red, green, blue). The background decides whether the theme is dark " +
                        "or light. Text colours are adjusted automatically so they stay readable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                val colours = c.customColors ?: Themes.CUSTOM.dark.colors
                val names = listOf("Primary", "Secondary", "Accent", "Background", "Cards", "Glow", "Second glow")
                names.forEachIndexed { i, name ->
                    ColourRow(name, colours.asList[i]) { v ->
                        update { it.copy(customColors = BrandColors.fromList(colours.asList.toMutableList().also { l -> l[i] = v })) }
                    }
                }
            }

            if (!isCustom) Label("Dark or light")
            val mode = c.mode ?: ResolvedTheme.defaultMode(active.spec)
            if (!isCustom) ThemeMode.entries.forEach { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(selected = mode == m, role = Role.RadioButton) { update { it.copy(mode = m) } }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == m, onClick = null)
                    Text(m.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }

            val hasBackdrop = active.backdrop != null
            ToggleRow(
                title = "Glass cards",
                subtitle = if (hasBackdrop) "See-through cards over the theme's backdrop." else "Only for the glass themes (the first five).",
                checked = hasBackdrop && (c.glass ?: true),
                enabled = hasBackdrop,
            ) { v -> update { it.copy(glass = if (v) null else false) } }

            IntSlider("Glow", c.glow, ThemeCustomization.DEFAULT_GLOW, ThemeCustomization.GLOW_RANGE, "%") { v -> update { it.copy(glow = v) } }
            IntSlider("Gradient and backdrop light", c.gradient, ThemeCustomization.DEFAULT_GRADIENT, ThemeCustomization.GRADIENT_RANGE, "%") { v ->
                update { it.copy(gradient = v) }
            }
            IntSlider("Corner radius", c.corner, ThemeCustomization.DEFAULT_CORNER, ThemeCustomization.CORNER_RANGE, " dp") { v ->
                update { it.copy(corner = v) }
            }
            IntSlider("Card border", c.border, ThemeCustomization.DEFAULT_BORDER, ThemeCustomization.BORDER_RANGE, " dp") { v ->
                update { it.copy(border = v) }
            }
            IntSlider("Text size", c.textScale, ThemeCustomization.DEFAULT_TEXT_SCALE, ThemeCustomization.TEXT_SCALE_RANGE, "%", step = 5) { v ->
                update { it.copy(textScale = v) }
            }

            val systemOff = remember { systemAnimationsOff(context) }
            val reduced = systemOff || (c.reduceMotion ?: false)
            ToggleRow(
                title = "Reduce motion",
                subtitle = if (systemOff) "Animations are already off in Android settings." else "Turns off press and transition animations.",
                checked = reduced,
                enabled = !systemOff,
            ) { v -> update { it.copy(reduceMotion = if (v) true else null) } }
            ToggleRow(
                title = "Liquid motion",
                subtitle = "The selection highlight stretches toward the tab you pick.",
                checked = !reduced && (c.liquidMotion ?: true),
                enabled = !reduced,
            ) { v -> update { it.copy(liquidMotion = if (v) null else false) } }
            ToggleRow(
                title = "Delete effect",
                subtitle = "Removed items shake and dissolve before they disappear.",
                checked = !reduced && (c.deleteEffect ?: true),
                enabled = !reduced,
            ) { v -> update { it.copy(deleteEffect = if (v) null else false) } }
            if (!reduced) {
                IntSlider(
                    "Animation speed", c.animationSpeed, ThemeCustomization.DEFAULT_ANIMATION_SPEED,
                    ThemeCustomization.ANIMATION_SPEED_RANGE, "%", step = 25,
                ) { v -> update { it.copy(animationSpeed = v) } }
            }

            TextButton(
                onClick = { scope.launch { repo.resetCustomizations() } },
                modifier = Modifier.padding(horizontal = 8.dp).aceFocusRing(),
            ) { Text("Reset appearance options") }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

/**
 * A whole-number setting stored as a diff: choosing the default value stores nothing.
 * The value is saved when the finger lifts, so dragging doesn't write on every step.
 */
@Composable
private fun IntSlider(
    title: String,
    stored: Int?,
    default: Int,
    range: IntRange,
    unit: String,
    step: Int = 1,
    onChange: (Int?) -> Unit,
) {
    val current = stored ?: default
    var dragging by remember(current) { mutableFloatStateOf(current.toFloat()) }
    fun snapped(v: Float) = ((v / step).roundToInt() * step).coerceIn(range.first, range.last)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                "${snapped(dragging)}$unit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AceSlider(
            value = dragging,
            onValueChange = { dragging = it },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            onValueChangeFinished = { snapped(dragging).let { v -> onChange(if (v == default) null else v) } },
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AceSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** One colour of the Custom theme: a swatch and its six-digit hex code. */
@Composable
private fun ColourRow(name: String, value: Long, onChange: (Long) -> Unit) {
    fun hex(v: Long) = java.lang.Long.toHexString(v and 0xFFFFFFL).padStart(6, '0').uppercase()
    var text by remember(value) { mutableStateOf(hex(value)) }
    val valid = text.length == 6 && text.all { it in '0'..'9' || it in 'A'..'F' }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(value.toColor(), CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { entered ->
                text = entered.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                if (text.length == 6) text.toLongOrNull(16)?.let { onChange(0xFF000000L or it) }
            },
            singleLine = true,
            isError = !valid,
            prefix = { Text("#") },
            modifier = Modifier.width(132.dp).semantics { contentDescription = "$name colour code" },
        )
    }
}

/** Preview of a theme drawn with that theme's own backdrop, colours and glow. */
@Composable
private fun ThemeTile(spec: ThemeSpec, dark: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val current = LocalAceTheme.current
    val preview = remember(spec.id, dark, current) {
        when (spec.id) {
            // These two have no fixed colours: show what they look like right now.
            Themes.CUSTOM.id -> if (current.spec.id == spec.id) current else ResolvedTheme.resolve(ThemeCustomization(baseId = spec.id))
            Themes.DYNAMIC.id -> ResolvedTheme.resolve(
                ThemeCustomization(baseId = spec.id, mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT),
                dynamic = dynamicBrandColors(context, dark),
            )
            else -> ResolvedTheme.resolve(ThemeCustomization(baseId = spec.id, mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT))
        }
    }
    val p = preview.palette
    val shape = RoundedCornerShape(12.dp)
    val selRing = MaterialTheme.colorScheme.primary
    Column(
        modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .aceFocusRing()
            .semantics { contentDescription = "${spec.name} theme" + if (selected) ", selected" else "" }
            .then(if (selected) Modifier.border(2.dp, selRing, shape) else Modifier)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(shape)
                .aceBackdrop(preview)
                .border(1.dp, p.outline.toColor(), shape)
                .padding(8.dp),
        ) {
            Column {
                Text(
                    if (spec.premium) "Glass" else "Aa",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = p.onBackground.toColor(),
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(22.dp).height(10.dp).background(p.primary.toColor(), RoundedCornerShape(5.dp)))
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.width(14.dp).height(10.dp).background(p.secondary.toColor(), RoundedCornerShape(5.dp)))
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.size(10.dp).background(p.glow.toColor(), CircleShape))
                }
            }
        }
        Text(
            spec.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
