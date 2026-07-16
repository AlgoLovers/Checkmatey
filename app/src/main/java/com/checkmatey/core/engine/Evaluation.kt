package com.checkmatey.core.engine

import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position

/**
 * Static position evaluation in centipawns, from White's perspective.
 * Material values plus simple piece-square tables (pawns, knights) so the bot develops
 * pieces and fights for the center instead of shuffling. Symmetric, so the start is 0.
 */
object Evaluation {

    fun value(type: PieceType): Int = when (type) {
        PieceType.PAWN -> 100
        PieceType.KNIGHT -> 320
        PieceType.BISHOP -> 330
        PieceType.ROOK -> 500
        PieceType.QUEEN -> 900
        PieceType.KING -> 0 // king safety is handled by search (mate scores), not material.
    }

    /** White-positive evaluation of [position]. */
    fun evaluate(position: Position): Int {
        var score = 0
        for (i in 0..63) {
            val piece = position.squares[i] ?: continue
            val table = pst(piece.type)
            val squareValue = value(piece.type) + table[if (piece.color == PieceColor.WHITE) i else mirror(i)]
            score += if (piece.color == PieceColor.WHITE) squareValue else -squareValue
        }
        return score
    }

    // Flip a square index vertically so a White-oriented table can score Black pieces.
    private fun mirror(index: Int): Int = (7 - index / 8) * 8 + (index % 8)

    private fun pst(type: PieceType): IntArray = when (type) {
        PieceType.PAWN -> PAWN_PST
        PieceType.KNIGHT -> KNIGHT_PST
        else -> ZERO_PST
    }

    private val ZERO_PST = IntArray(64)

    // Index 0 = a1, index 63 = h8 (rank * 8 + file), White's perspective.
    private val PAWN_PST = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        5, 10, 10, -20, -20, 10, 10, 5,
        5, -5, -10, 0, 0, -10, -5, 5,
        0, 0, 0, 20, 20, 0, 0, 0,
        5, 5, 10, 25, 25, 10, 5, 5,
        10, 10, 20, 30, 30, 20, 10, 10,
        50, 50, 50, 50, 50, 50, 50, 50,
        0, 0, 0, 0, 0, 0, 0, 0,
    )

    private val KNIGHT_PST = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20, 0, 5, 5, 0, -20, -40,
        -30, 5, 10, 15, 15, 10, 5, -30,
        -30, 0, 15, 20, 20, 15, 0, -30,
        -30, 5, 15, 20, 20, 15, 5, -30,
        -30, 0, 10, 15, 15, 10, 0, -30,
        -40, -20, 0, 0, 0, 0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50,
    )
}
