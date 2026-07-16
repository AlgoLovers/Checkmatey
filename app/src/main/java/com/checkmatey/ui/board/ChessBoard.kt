package com.checkmatey.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import com.checkmatey.ui.theme.BoardDark
import com.checkmatey.ui.theme.BoardHighlight
import com.checkmatey.ui.theme.BoardLight

/**
 * Renders an 8x8 board with the pieces from [position]. Stays square via [aspectRatio], so it
 * scales cleanly from phone to tablet — the caller decides the size.
 *
 * @param selected the currently selected square (tinted).
 * @param targets legal destination squares for the selection (dots for moves, rings for captures).
 * @param onSquareClick invoked with the tapped square; when null the board is display-only.
 */
@Composable
fun ChessBoard(
    position: Position,
    modifier: Modifier = Modifier,
    selected: Square? = null,
    targets: Set<Square> = emptySet(),
    onSquareClick: ((Square) -> Unit)? = null,
) {
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        val cellSize = maxWidth / 8
        val glyphSize = with(LocalDensity.current) { (cellSize * 0.72f).toSp() }
        Column(Modifier.fillMaxSize()) {
            for (rank in 7 downTo 0) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (file in 0..7) {
                        val square = Square(file, rank)
                        val isLight = (file + rank) % 2 == 1
                        val piece = position.pieceAt(square)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isLight) BoardLight else BoardDark)
                                .then(
                                    if (onSquareClick != null) {
                                        Modifier.clickable { onSquareClick(square) }
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (square == selected) {
                                Box(Modifier.matchParentSize().background(BoardHighlight.copy(alpha = 0.55f)))
                            }
                            if (piece != null) {
                                Text(text = piece.glyph.toString(), fontSize = glyphSize, color = Color.Black)
                            }
                            if (square in targets) {
                                if (piece != null) {
                                    // Capture: ring around the piece.
                                    Box(
                                        Modifier
                                            .fillMaxSize(0.92f)
                                            .border(3.dp, Color.Black.copy(alpha = 0.30f), CircleShape),
                                    )
                                } else {
                                    // Quiet move: centered dot.
                                    Box(
                                        Modifier
                                            .fillMaxSize(0.30f)
                                            .background(Color.Black.copy(alpha = 0.25f), CircleShape),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
