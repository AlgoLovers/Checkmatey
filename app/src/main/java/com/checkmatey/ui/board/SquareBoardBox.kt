package com.checkmatey.ui.board

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The largest a board grows on a big screen, so it never becomes an unwieldy wall on tablets. */
val MaxBoardSide: Dp = 900.dp

/**
 * Centres a square board in the space it's given, sized to the smaller of width/height (capped at
 * [maxSide]). Every board screen used to inline the same
 * `BoxWithConstraints { minOf(maxWidth, maxHeight).coerceAtMost(900.dp) }` block; they now share it.
 * The [content] runs in a [BoxScope] and receives the computed `side`, so it can size the board with
 * `Modifier.size(side)` and align overlays (e.g. the capture callout) with `Modifier.align(...)`.
 */
@Composable
fun SquareBoardBox(
    modifier: Modifier = Modifier,
    maxSide: Dp = MaxBoardSide,
    content: @Composable BoxScope.(side: Dp) -> Unit,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight).coerceAtMost(maxSide)
        content(side)
    }
}
