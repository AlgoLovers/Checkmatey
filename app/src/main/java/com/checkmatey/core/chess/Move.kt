package com.checkmatey.core.chess

/**
 * A single move. [promotion] is set only for pawn promotions. The castle / en passant
 * flags let [Position.applyMove] update the extra state a plain from→to can't express.
 */
data class Move(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
    val isEnPassant: Boolean = false,
    val isCastleKingSide: Boolean = false,
    val isCastleQueenSide: Boolean = false,
) {
    val isCastle: Boolean get() = isCastleKingSide || isCastleQueenSide

    /** Long algebraic (UCI) notation, e.g. "e2e4" or "e7e8q". */
    fun uci(): String = from.name + to.name + (promotion?.letter?.toString() ?: "")

    override fun toString(): String = uci()
}
