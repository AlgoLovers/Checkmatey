package com.checkmatey.core.chess

/** What each side has captured so far, plus the material balance in pawns. */
data class CapturedPieces(
    val byWhite: List<PieceType>, // black pieces White has taken, most valuable first
    val byBlack: List<PieceType>, // white pieces Black has taken, most valuable first
    val diffPawns: Int, // captured-value balance: positive = White is up material
)

/**
 * Derives captured pieces by comparing the board against the starting set — no move history
 * needed, so it works for any position (including loaded puzzles). Promotions can create *extra*
 * pieces (a second queen); counts clamp at zero so a promotion never reads as a phantom capture.
 */
object Material {

    private val START = mapOf(
        PieceType.PAWN to 8,
        PieceType.KNIGHT to 2,
        PieceType.BISHOP to 2,
        PieceType.ROOK to 2,
        PieceType.QUEEN to 1,
    )

    private val PAWNS = mapOf(
        PieceType.PAWN to 1,
        PieceType.KNIGHT to 3,
        PieceType.BISHOP to 3,
        PieceType.ROOK to 5,
        PieceType.QUEEN to 9,
    )

    fun captured(position: Position): CapturedPieces {
        val white = HashMap<PieceType, Int>()
        val black = HashMap<PieceType, Int>()
        for (i in 0..63) {
            val p = position.squares[i] ?: continue
            if (p.type == PieceType.KING) continue
            val m = if (p.color == PieceColor.WHITE) white else black
            m[p.type] = (m[p.type] ?: 0) + 1
        }
        fun missing(current: Map<PieceType, Int>): List<PieceType> =
            START.flatMap { (type, start) ->
                List((start - (current[type] ?: 0)).coerceAtLeast(0)) { type }
            }.sortedByDescending { PAWNS[it] }

        val byWhite = missing(black) // black's missing pieces were taken by White
        val byBlack = missing(white)
        val diff = byWhite.sumOf { PAWNS[it] ?: 0 } - byBlack.sumOf { PAWNS[it] ?: 0 }
        return CapturedPieces(byWhite, byBlack, diff)
    }

    /** Classic point value in pawns (P1 N/B3 R5 Q9; king 0) — the one shared material scale. */
    fun pawnValue(type: PieceType): Int = PAWNS[type] ?: 0

    /** Board glyph for a captured piece (black glyphs read best at small sizes). */
    fun glyph(type: PieceType): String = when (type) {
        PieceType.PAWN -> "♟"
        PieceType.KNIGHT -> "♞"
        PieceType.BISHOP -> "♝"
        PieceType.ROOK -> "♜"
        PieceType.QUEEN -> "♛"
        PieceType.KING -> "♚"
    }
}
