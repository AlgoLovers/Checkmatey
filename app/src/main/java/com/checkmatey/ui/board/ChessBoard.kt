package com.checkmatey.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.checkmatey.core.chess.Board
import com.checkmatey.core.chess.Square
import com.checkmatey.ui.theme.BoardDark
import com.checkmatey.ui.theme.BoardLight

/**
 * Renders an 8x8 board with the pieces from [board]. Stays square via [aspectRatio],
 * so it scales cleanly from phone to tablet — the caller decides the width.
 */
@Composable
fun ChessBoard(board: Board, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        val cellSize = maxWidth / 8
        val glyphSize = with(LocalDensity.current) { (cellSize * 0.72f).toSp() }
        Column(Modifier.fillMaxSize()) {
            for (rank in 7 downTo 0) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (file in 0..7) {
                        val square = Square(file, rank)
                        val isLight = (file + rank) % 2 == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isLight) BoardLight else BoardDark),
                            contentAlignment = Alignment.Center,
                        ) {
                            board.pieceAt(square)?.let { piece ->
                                Text(
                                    text = piece.glyph.toString(),
                                    fontSize = glyphSize,
                                    color = Color.Black,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
