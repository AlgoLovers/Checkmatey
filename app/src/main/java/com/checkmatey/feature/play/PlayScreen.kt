package com.checkmatey.feature.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.Board
import com.checkmatey.ui.board.ChessBoard

/**
 * Play tab. For now it shows the starting position; interactive move handling and
 * the pure-Kotlin move engine land in later loop iterations.
 */
@Composable
fun PlayScreen(modifier: Modifier = Modifier) {
    val board = remember { Board.startingPosition() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Cap the board on large screens (tablet) so it doesn't stretch edge to edge.
        ChessBoard(
            board = board,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
        )
    }
}
