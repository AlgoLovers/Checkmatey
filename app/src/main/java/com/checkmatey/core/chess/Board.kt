package com.checkmatey.core.chess

/**
 * An immutable snapshot of piece placement on an 8x8 board.
 *
 * Only placement is modeled for now. Side-to-move, castling rights, en passant and
 * the clocks (the rest of a full FEN) are added when move generation lands.
 */
class Board private constructor(private val squares: Map<Square, Piece>) {

    /** The piece on [square], or null if empty. */
    fun pieceAt(square: Square): Piece? = squares[square]

    /** All occupied squares mapped to their pieces. */
    val pieces: Map<Square, Piece> get() = squares

    /** Number of pieces currently on the board. */
    val pieceCount: Int get() = squares.size

    companion object {
        /** Piece-placement field of the standard starting position. */
        const val STARTING_PLACEMENT = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"

        /** The standard chess starting position. */
        fun startingPosition(): Board = fromFenPlacement(STARTING_PLACEMENT)

        /**
         * Parses the piece-placement field of a FEN string. Ranks are listed 8..1,
         * separated by '/', digits mean consecutive empty squares.
         */
        fun fromFenPlacement(placement: String): Board {
            val rows = placement.split('/')
            require(rows.size == 8) { "FEN placement must have 8 ranks, got ${rows.size}" }

            val map = mutableMapOf<Square, Piece>()
            for ((rowIndex, row) in rows.withIndex()) {
                val rank = 7 - rowIndex // FEN lists rank 8 first
                var file = 0
                for (c in row) {
                    if (c.isDigit()) {
                        file += c - '0'
                    } else {
                        map[Square(file, rank)] = Piece.fromFenChar(c)
                        file++
                    }
                }
                require(file == 8) { "Rank ${rank + 1} does not describe exactly 8 files" }
            }
            return Board(map)
        }
    }
}
