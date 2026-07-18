package com.checkmatey.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * The main call-to-action button — a vertical gradient of the brand colour plus a coloured soft
 * shadow, so it reads as raised and inviting. Both shades derive from the theme's [primary]
 * (top a touch lighter, bottom a touch darker), so it looks right in light and dark. Pure Compose,
 * no dependencies; the content (icon + text) inherits [onPrimary] automatically.
 */
@Composable
fun GradientPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val top = lerp(colors.primary, Color.White, HIGHLIGHT_FRACTION)
    val bottom = lerp(colors.primary, Color.Black, SHADE_FRACTION)
    Row(
        modifier = modifier
            .shadow(elevation = 10.dp, shape = shape, spotColor = colors.primary, ambientColor = colors.primary)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onPrimary, content = { content() })
    }
}

private const val HIGHLIGHT_FRACTION = 0.16f
private const val SHADE_FRACTION = 0.14f
