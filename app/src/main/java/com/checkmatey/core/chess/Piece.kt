package com.checkmatey.core.chess

/** Side to move / piece owner. */
enum class PieceColor {
    WHITE,
    BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

/** The six chess piece types. [letter] is the lowercase FEN letter. */
enum class PieceType(val letter: Char) {
    PAWN('p'),
    KNIGHT('n'),
    BISHOP('b'),
    ROOK('r'),
    QUEEN('q'),
    KING('k'),
}

/** Korean name used throughout the coach, tutor, and capture callouts — one source of truth. */
fun PieceType.koreanName(): String = when (this) {
    PieceType.PAWN -> "폰"
    PieceType.KNIGHT -> "나이트"
    PieceType.BISHOP -> "비숍"
    PieceType.ROOK -> "룩"
    PieceType.QUEEN -> "퀸"
    PieceType.KING -> "킹"
}

/**
 * A single piece on the board.
 *
 * This is intentionally minimal. Move generation, check detection and game rules
 * are added incrementally in [core.chess] as the pure-Kotlin engine grows.
 */
data class Piece(val color: PieceColor, val type: PieceType) {

    /** FEN character: uppercase for white, lowercase for black (e.g. white knight = 'N'). */
    val fenChar: Char
        get() = if (color == PieceColor.WHITE) type.letter.uppercaseChar() else type.letter

    /** Unicode glyph for rendering the piece (e.g. ♞). */
    val glyph: Char
        get() {
            val base = if (color == PieceColor.WHITE) 0x2654 else 0x265A
            val offset = when (type) {
                PieceType.KING -> 0
                PieceType.QUEEN -> 1
                PieceType.ROOK -> 2
                PieceType.BISHOP -> 3
                PieceType.KNIGHT -> 4
                PieceType.PAWN -> 5
            }
            return (base + offset).toChar()
        }

    companion object {
        /** Builds a piece from a FEN character, e.g. 'N' -> white knight, 'q' -> black queen. */
        fun fromFenChar(c: Char): Piece {
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            val type = PieceType.entries.firstOrNull { it.letter == c.lowercaseChar() }
                ?: throw IllegalArgumentException("Unknown FEN piece char: $c")
            return Piece(color, type)
        }
    }
}
