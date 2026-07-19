package com.checkmatey.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.exp

/**
 * Who's winning, at a glance — deliberately minimal: one thin bar, White's share filled from the
 * left, a faint centre tick marking equality. No numbers; the balance IS the reading. The value is
 * the engine's full evaluation (material + positioning) mapped through the calibrated cp→win%
 * curve, animated so advantage visibly ebbs and flows move by move.
 */
@Composable
fun EvalBar(evalCp: Int, modifier: Modifier = Modifier) {
    val target = (winPct(evalCp) / 100.0).coerceIn(0.05, 0.95).toFloat() // never fully empty/full
    val fraction by animateFloatAsState(targetValue = target, animationSpec = tween(450), label = "eval")

    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF3A3F3A)), // black's side
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(Color(0xFFF2F0E4)), // white's share, from the left
        )
        // Equality tick — a quiet reference point in the middle.
        Box(
            Modifier
                .align(Alignment.Center)
                .width(2.dp)
                .fillMaxHeight()
                .background(Color(0x66808080)),
        )
    }
}

// Same cp -> win% mapping the coach grades with (Lichess's fitted logistic).
private fun winPct(cp: Int): Double {
    val c = cp.coerceIn(-1500, 1500)
    return 100.0 / (1.0 + exp(-0.00368208 * c))
}
