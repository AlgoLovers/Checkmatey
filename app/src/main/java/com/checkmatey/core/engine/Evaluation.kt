package com.checkmatey.core.engine

import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position

/**
 * Static evaluation in centipawns from White's perspective: material + piece-square tables for
 * every piece (with a separate king table once queens are off) + the bishop pair. Symmetric, so
 * the starting position evaluates to 0. Tables follow the classic "simplified evaluation" values.
 */
object Evaluation {

    fun value(type: PieceType): Int = when (type) {
        PieceType.PAWN -> 100
        PieceType.KNIGHT -> 320
        PieceType.BISHOP -> 330
        PieceType.ROOK -> 500
        PieceType.QUEEN -> 900
        PieceType.KING -> 0 // king safety comes from its table and the search's mate scores.
    }

    /** White-positive evaluation of [position]. */
    fun evaluate(position: Position): Int {
        var score = 0
        var whiteBishops = 0
        var blackBishops = 0
        var queens = 0
        for (i in 0..63) {
            val p = position.squares[i] ?: continue
            if (p.type == PieceType.QUEEN) queens++
            if (p.type == PieceType.BISHOP) {
                if (p.color == PieceColor.WHITE) whiteBishops++ else blackBishops++
            }
        }
        val endgame = queens == 0
        for (i in 0..63) {
            val piece = position.squares[i] ?: continue
            val table = pst(piece.type, endgame)
            val sq = if (piece.color == PieceColor.WHITE) i else mirror(i)
            val v = value(piece.type) + table[sq]
            score += if (piece.color == PieceColor.WHITE) v else -v
        }
        if (whiteBishops >= 2) score += BISHOP_PAIR
        if (blackBishops >= 2) score -= BISHOP_PAIR
        return score
    }

    private const val BISHOP_PAIR = 30

    // Flip a square index vertically so White-oriented tables score Black pieces.
    private fun mirror(index: Int): Int = (7 - index / 8) * 8 + (index % 8)

    private fun pst(type: PieceType, endgame: Boolean): IntArray = when (type) {
        PieceType.PAWN -> PAWN_PST
        PieceType.KNIGHT -> KNIGHT_PST
        PieceType.BISHOP -> BISHOP_PST
        PieceType.ROOK -> ROOK_PST
        PieceType.QUEEN -> QUEEN_PST
        PieceType.KING -> if (endgame) KING_END_PST else KING_MID_PST
    }

    // All tables are index 0 = a1 … 63 = h8 (rank * 8 + file), from White's perspective.
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

    private val BISHOP_PST = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10, 5, 0, 0, 0, 0, 5, -10,
        -10, 10, 10, 10, 10, 10, 10, -10,
        -10, 0, 10, 10, 10, 10, 0, -10,
        -10, 5, 5, 10, 10, 5, 5, -10,
        -10, 0, 5, 10, 10, 5, 0, -10,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -20, -10, -10, -10, -10, -10, -10, -20,
    )

    private val ROOK_PST = intArrayOf(
        0, 0, 0, 5, 5, 0, 0, 0,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        5, 10, 10, 10, 10, 10, 10, 5,
        0, 0, 0, 0, 0, 0, 0, 0,
    )

    private val QUEEN_PST = intArrayOf(
        -20, -10, -10, -5, -5, -10, -10, -20,
        -10, 0, 5, 0, 0, 0, 0, -10,
        -10, 5, 5, 5, 5, 5, 0, -10,
        0, 0, 5, 5, 5, 5, 0, -5,
        -5, 0, 5, 5, 5, 5, 0, -5,
        -10, 0, 5, 5, 5, 5, 0, -10,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -20, -10, -10, -5, -5, -10, -10, -20,
    )

    private val KING_MID_PST = intArrayOf(
        20, 30, 10, 0, 0, 10, 30, 20,
        20, 20, 0, 0, 0, 0, 20, 20,
        -10, -20, -20, -20, -20, -20, -20, -10,
        -20, -30, -30, -40, -40, -30, -30, -20,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
    )

    private val KING_END_PST = intArrayOf(
        -50, -30, -30, -30, -30, -30, -30, -50,
        -30, -30, 0, 0, 0, 0, -30, -30,
        -30, -10, 20, 30, 30, 20, -10, -30,
        -30, -10, 30, 40, 40, 30, -10, -30,
        -30, -10, 30, 40, 40, 30, -10, -30,
        -30, -10, 20, 30, 30, 20, -10, -30,
        -30, -20, -10, 0, 0, -10, -20, -30,
        -50, -40, -30, -20, -20, -30, -40, -50,
    )
}
