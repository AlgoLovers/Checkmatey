package com.checkmatey.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable

/** A tiny line chart for a rating trend. Draws nothing meaningful with fewer than 2 points. */
@Composable
fun Sparkline(values: List<Int>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val span = (max - min).coerceAtLeast(1).toFloat()
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = size.height - (v - min) / span * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 4f))
        // Mark the latest point.
        val lastX = size.width
        val lastY = size.height - (values.last() - min) / span * size.height
        drawCircle(color = color, radius = 6f, center = Offset(lastX, lastY))
    }
}
