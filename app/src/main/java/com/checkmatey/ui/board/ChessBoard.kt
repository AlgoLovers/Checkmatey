package com.checkmatey.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.Piece
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import com.checkmatey.ui.theme.BoardDark
import com.checkmatey.ui.theme.BoardHighlight
import com.checkmatey.ui.theme.BoardLight

/**
 * Renders an 8x8 board. Pieces are drawn as solid, side-colored glyphs (white bodies with a
 * dark edge, black bodies with a light edge) so both are easy to read on any square. The most
 * recent move's squares are tinted, and the moved piece slides from origin to destination.
 *
 * @param lastMove the move to highlight and animate; null on a fresh board.
 * @param onSquareClick invoked with the tapped square; null makes the board display-only.
 */
@Composable
fun ChessBoard(
    position: Position,
    modifier: Modifier = Modifier,
    selected: Square? = null,
    targets: Set<Square> = emptySet(),
    lastMove: Move? = null,
    hintSquares: Set<Square> = emptySet(),
    onSquareClick: ((Square) -> Unit)? = null,
) {
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        val cell = maxWidth / 8
        val glyphSize = with(LocalDensity.current) { (cell * 0.72f).toSp() }

        // Slide the last-moved piece from its origin to its destination.
        val progress = remember { Animatable(1f) }
        LaunchedEffect(lastMove) {
            if (lastMove != null) {
                progress.snapTo(0f)
                progress.animateTo(1f, animationSpec = tween(durationMillis = 180))
            }
        }
        val animating = lastMove != null && progress.value < 1f

        Column(Modifier.fillMaxSize()) {
            for (rank in 7 downTo 0) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (file in 0..7) {
                        val square = Square(file, rank)
                        val isLight = (file + rank) % 2 == 1
                        val piece = position.pieceAt(square)
                        val isLastMoveSquare = lastMove != null && (square == lastMove.from || square == lastMove.to)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isLight) BoardLight else BoardDark)
                                .then(
                                    if (onSquareClick != null) Modifier.clickable { onSquareClick(square) } else Modifier,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                square == selected ->
                                    Box(Modifier.matchParentSize().background(BoardHighlight.copy(alpha = 0.55f)))
                                isLastMoveSquare ->
                                    Box(Modifier.matchParentSize().background(BoardHighlight.copy(alpha = 0.32f)))
                            }
                            // Hide the moved piece in its cell while it's sliding (drawn in the overlay).
                            if (piece != null && !(animating && square == lastMove?.to)) {
                                PieceGlyph(piece, glyphSize)
                            }
                            if (square in hintSquares) {
                                Box(Modifier.matchParentSize().border(3.dp, HintColor, RectangleShape))
                            }
                            if (square in targets) {
                                if (piece != null) {
                                    Box(Modifier.fillMaxSize(0.92f).border(3.dp, Color.Black.copy(alpha = 0.30f), CircleShape))
                                } else {
                                    Box(Modifier.fillMaxSize(0.30f).background(Color.Black.copy(alpha = 0.25f), CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sliding piece overlay.
        if (animating && lastMove != null) {
            position.pieceAt(lastMove.to)?.let { moving ->
                val x = lerp(cell * lastMove.from.file, cell * lastMove.to.file, progress.value)
                val y = lerp(cell * (7 - lastMove.from.rank), cell * (7 - lastMove.to.rank), progress.value)
                Box(
                    modifier = Modifier.offset(x = x, y = y).size(cell),
                    contentAlignment = Alignment.Center,
                ) {
                    PieceGlyph(moving, glyphSize)
                }
            }
        }
    }
}

// Border color marking the hint (best-move) squares.
private val HintColor = Color(0xFF1E88E5)

@Composable
private fun PieceGlyph(piece: Piece, size: TextUnit) {
    val isWhite = piece.color == PieceColor.WHITE
    Text(
        text = filledGlyph(piece.type).toString(),
        style = TextStyle(
            fontSize = size,
            color = if (isWhite) Color(0xFFF7F7F7) else Color(0xFF1A1A1A),
            // Symmetric shadow acts as an outline: dark edge on white pieces, light edge on black.
            shadow = Shadow(
                color = if (isWhite) Color(0xDD000000) else Color(0x88FFFFFF),
                offset = Offset(0f, 0f),
                blurRadius = if (isWhite) 6f else 3f,
            ),
        ),
    )
}

/** The filled (black-series) chess glyph for a type, so both colors render as solid shapes. */
private fun filledGlyph(type: PieceType): Char {
    val offset = when (type) {
        PieceType.KING -> 0
        PieceType.QUEEN -> 1
        PieceType.ROOK -> 2
        PieceType.BISHOP -> 3
        PieceType.KNIGHT -> 4
        PieceType.PAWN -> 5
    }
    return (0x265A + offset).toChar()
}
