package com.checkmatey.core.chess

/**
 * Threefold repetition — a draw once the same position has occurred three times. Two positions count
 * as "the same" when their piece placement, side to move, castling rights, and en passant target
 * match (the first four FEN fields); the halfmove/fullmove counters are ignored, per the FIDE rule.
 *
 * A rule of chess, so it lives in `core/chess` as pure, tested logic rather than in the UI — the
 * Play screen just supplies the position history it already keeps for undo.
 */
object Repetition {

    /** The comparison key: the FEN with its move counters stripped. */
    fun key(position: Position): String = position.toFen().split(" ").take(4).joinToString(" ")

    /** How many times [position] occurs in [history] (which is expected to already include it). */
    fun count(history: List<Position>, position: Position): Int {
        val k = key(position)
        return history.count { key(it) == k }
    }

    fun isThreefold(history: List<Position>, position: Position): Boolean =
        count(history, position) >= 3
}
