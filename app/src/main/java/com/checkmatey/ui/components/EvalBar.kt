package com.checkmatey.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.exp

/**
 * Who's winning, at a glance: a horizontal bar filled from the left with White's share of the win
 * probability (evaluation includes material AND positioning — the same calibrated function the
 * engine plays by). The label shows the classic pawn units ("+1.2"). Smoothly animated so
 * advantage visibly ebbs and flows move by move.
 */
@Composable
fun EvalBar(evalCp: Int, modifier: Modifier = Modifier) {
    val target = (winPct(evalCp) / 100.0).coerceIn(0.05, 0.95).toFloat() // never fully empty/full
    val fraction by animateFloatAsState(targetValue = target, animationSpec = tween(450), label = "eval")
    val pawns = evalCp / 100.0
    val label = if (pawns >= 0) "+%.1f".format(pawns) else "%.1f".format(pawns)

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF3A3F3A)), // black's side of the bar
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(Color(0xFFF2F0E4)), // white's share, filled from the left
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Same cp -> win% mapping the coach grades with (Lichess's fitted logistic).
private fun winPct(cp: Int): Double {
    val c = cp.coerceIn(-1500, 1500)
    return 100.0 / (1.0 + exp(-0.00368208 * c))
}
