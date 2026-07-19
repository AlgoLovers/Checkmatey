package com.checkmatey.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The shared layout for every screen with a chess board, so the board is always as large as the
 * screen allows and controls never fight it — on phones and tablets alike.
 *
 * - Narrow (phone / portrait): [top], then the board taking the space that's left, then [bottom].
 * - Wide (tablet / landscape, ≥ [twoPaneMinWidth]): the board fills the full height on the left,
 *   with [top] + [bottom] stacked in a scrollable side panel on the right — so the board uses the
 *   whole height and the buttons move off the bottom edge (clear of the system gesture bar).
 *
 * [board] receives the sizing Modifier (weight + fill) and should render a centred, square board:
 * `{ m -> BoxWithConstraints(m, contentAlignment = Center) { ChessBoard(Modifier.size(minOf(maxWidth, maxHeight))) } }`.
 */
@Composable
fun ResponsiveBoardLayout(
    board: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    top: (@Composable ColumnScope.() -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    panelWidth: Dp = 320.dp,
    twoPaneMinWidth: Dp = 700.dp,
) {
    BoxWithConstraints(modifier.fillMaxSize().padding(contentPadding)) {
        if (maxWidth >= twoPaneMinWidth) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                board(Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.width(panelWidth).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    top?.invoke(this)
                    if (top != null && bottom != null) Spacer(Modifier.height(10.dp))
                    bottom?.invoke(this)
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                top?.invoke(this)
                if (top != null) Spacer(Modifier.height(8.dp))
                board(Modifier.weight(1f).fillMaxWidth())
                if (bottom != null) Spacer(Modifier.height(8.dp))
                bottom?.invoke(this)
            }
        }
    }
}
