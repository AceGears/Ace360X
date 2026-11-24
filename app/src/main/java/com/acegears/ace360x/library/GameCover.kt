package com.acegears.ace360x.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acegears.ace360x.theme.ColorMath
import com.acegears.ace360x.theme.Contrast
import com.acegears.ace360x.theme.LocalAceTheme
import com.acegears.ace360x.theme.toColor

/**
 * A generated cover: Ace360X has no cover art (it never downloads anything), so each game
 * gets a plate in the theme's two glow tones with its initials. The tones are mixed into
 * the card colour so the letters stay readable.
 */
@Composable
fun GameCover(name: String, shape: Shape, modifier: Modifier = Modifier, letterSize: TextUnit = 34.sp) {
    val p = LocalAceTheme.current.palette
    // Vary the direction a little per game so a grid doesn't look stamped.
    val flip = (name.hashCode() and 1) == 0
    val from = ColorMath.blend(p.surfaceContainerHigh, p.glow, 0.42f)
    val to = ColorMath.blend(p.surfaceContainerHigh, p.glowAlt, 0.30f)
    val letter = Contrast.bestOf(ColorMath.blend(from, to, 0.5f), listOf(0xFFFFFFFFL, 0xFF000000L))
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(if (flip) listOf(from.toColor(), to.toColor()) else listOf(to.toColor(), from.toColor())),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            coverInitials(name),
            fontSize = letterSize,
            fontWeight = FontWeight.Black,
            color = letter.toColor().copy(alpha = 0.92f),
            modifier = Modifier.padding(4.dp),
        )
    }
}
