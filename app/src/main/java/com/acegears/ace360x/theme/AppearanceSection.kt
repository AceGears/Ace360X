package com.acegears.ace360x.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/** Settings → Appearance: base theme, accent, background effect, motion, corners. */
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
                Themes.ALL.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { spec ->
                            ThemeTile(
                                spec = spec,
                                selected = spec.id == active.spec.id,
                                modifier = Modifier.weight(1f),
                            ) { scope.launch { repo.selectBase(spec.id) } }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Label("Accent colour")
            val accents = remember(active.spec.id) { Themes.validAccentsFor(active.spec) }
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (listOf<Long?>(null) + accents).chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { accent ->
                            val selected = (c.accent?.takeIf { it in accents }) == accent
                            AccentSwatch(accent, active.spec.seed.primary, selected) {
                                update { it.copy(accent = accent) }
                            }
                        }
                    }
                }
            }
            Text(
                "Only accents that keep text readable (WCAG AA) in this theme are shown.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            val hasAmbiance = active.spec.ambiance.kind != AmbianceKind.NONE
            ToggleRow(
                title = "Background effect",
                subtitle = if (hasAmbiance) "Decorative background on app screens. Never shown during games." else "This theme has no background effect.",
                checked = active.ambianceEnabled,
                enabled = hasAmbiance,
            ) { v -> update { it.copy(ambianceEnabled = v) } }

            val systemOff = remember { systemAnimationsOff(context) }
            ToggleRow(
                title = "Reduce motion",
                subtitle = if (systemOff) "Animations are already off in Android settings." else "Stops animated backgrounds and shortens transitions.",
                checked = systemOff || (c.reduceMotion ?: false),
                enabled = !systemOff,
            ) { v -> update { it.copy(reduceMotion = v) } }

            Label("Corner style")
            listOf(0.5f to "Sharper", 1f to "Theme default", 1.5f to "Rounder").forEach { (scale, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(selected = (c.cornerScale ?: 1f) == scale, role = Role.RadioButton) {
                            update { it.copy(cornerScale = if (scale == 1f) null else scale) }
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = (c.cornerScale ?: 1f) == scale, onClick = null)
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                }
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

/** A swatch showing [accent] (null = the theme's own primary). 48 dp touch target. */
@Composable
private fun AccentSwatch(accent: Long?, themePrimary: Long, selected: Boolean, onClick: () -> Unit) {
    val ring = MaterialTheme.colorScheme.onSurface
    val color = (accent ?: themePrimary).toColor()
    val label = if (accent == null) "Theme default accent" else "Accent #" + java.lang.Long.toHexString(accent and 0xFFFFFF).padStart(6, '0').uppercase()
    Box(
        Modifier
            .size(48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .aceFocusRing()
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(if (selected) 34.dp else 28.dp)
                .then(if (selected) Modifier.border(3.dp, ring, CircleShape) else Modifier)
                .padding(if (selected) 5.dp else 0.dp)
                .background(color, CircleShape),
        )
    }
}

/** Preview of a theme drawn with that theme's own palette, shape and heading font. */
@Composable
private fun ThemeTile(spec: ThemeSpec, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = spec.palette
    val shape = spec.shapes.corner(spec.shapes.mediumDp.coerceAtMost(14f))
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
                .background(p.background.toColor(), shape)
                .border(1.dp, p.outline.toColor(), shape)
                .padding(8.dp),
        ) {
            Column {
                Text(
                    "Aa",
                    style = TextStyle(
                        fontFamily = spec.type.headingFont.toFamily(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = spec.type.headingLetterSpacing.sp,
                    ),
                    color = p.onBackground.toColor(),
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(22.dp).height(10.dp).background(p.primary.toColor(), spec.shapes.corner(spec.shapes.smallDp.coerceAtMost(5f))))
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.width(14.dp).height(10.dp).background(p.secondary.toColor(), spec.shapes.corner(spec.shapes.smallDp.coerceAtMost(5f))))
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.width(10.dp).height(10.dp).background(p.tertiary.toColor(), CircleShape))
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
