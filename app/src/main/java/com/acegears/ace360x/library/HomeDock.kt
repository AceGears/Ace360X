package com.acegears.ace360x.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acegears.ace360x.theme.AceIcons
import com.acegears.ace360x.theme.LocalAceTheme
import com.acegears.ace360x.theme.LiquidIndicator
import com.acegears.ace360x.theme.aceCardColor
import com.acegears.ace360x.theme.aceFocusRing
import com.acegears.ace360x.theme.aceGlow
import com.acegears.ace360x.theme.motionReduced
import com.acegears.ace360x.theme.toColor

private fun HomeTab.icon(): ImageVector = when (this) {
    HomeTab.ALL -> AceIcons.Library
    HomeTab.FAVOURITES -> Icons.Rounded.Star
    HomeTab.RECENT -> AceIcons.Play
}

/**
 * The dock at the bottom of the home screen: one tab per [HomeTab] with its game count,
 * and a highlight that moves to the selected tab (stretching toward it with liquid motion).
 */
@Composable
fun HomeDock(selected: HomeTab, counts: Map<HomeTab, Int>, onSelect: (HomeTab) -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalAceTheme.current
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape((theme.cornerDp * 1.4f).dp)
    val indicator = remember { LiquidIndicator() }
    val reduced = motionReduced()
    val tabs = HomeTab.entries
    val pill = cs.primaryContainer
    val glow = theme.palette.glow.toColor()
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .aceGlow(glow, theme.glowIntensity * 0.35f, (theme.cornerDp * 1.4f).dp)
            .clip(shape)
            .background(aceCardColor())
            .border(1.dp, cs.outlineVariant, shape),
    ) {
        val density = LocalDensity.current
        val slot = with(density) { maxWidth.toPx() } / tabs.size
        val inset = with(density) { 6.dp.toPx() }
        val index = tabs.indexOf(selected)
        LaunchedEffect(index, slot) {
            indicator.moveTo(
                index * slot + inset, (index + 1) * slot - inset,
                liquid = theme.liquidMotion, reduced = reduced, durationScale = theme.durationScale,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .drawBehind {
                    val start = indicator.start.value
                    val width = (indicator.end.value - start).coerceAtLeast(0f)
                    val radius = (size.height - 2 * inset) / 2
                    drawRoundRect(
                        color = pill,
                        topLeft = Offset(start, inset),
                        size = Size(width, size.height - 2 * inset),
                        cornerRadius = CornerRadius(radius),
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selected
                val colour = if (isSelected) cs.onPrimaryContainer else cs.onSurfaceVariant
                Box(
                    Modifier
                        .weight(1f)
                        .height(64.dp)
                        .selectable(
                            selected = isSelected,
                            interactionSource = remember { MutableInteractionSource() },
                            // The moving highlight is the feedback; a ripple would spill outside it.
                            indication = null,
                            role = Role.Tab,
                        ) { onSelect(tab) }
                        .aceFocusRing(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(tab.icon(), contentDescription = null, tint = colour)
                        Text(
                            "${tab.label} · ${counts[tab] ?: 0}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colour,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
