package com.checkmatey.core.chess

/** What a tap on the board means, once the shared selection bookkeeping has interpreted it. */
sealed interface TapResult {
    /** Start (or move) the selection to this square — it holds a piece the player may move. */
    data class Select(val square: Square) : TapResult

    /** Clear the current selection (tapped the selected square again, or an empty/enemy square). */
    data object Clear : TapResult

    /** The selected piece can move to the tapped square: these are the matching legal moves
     *  (more than one only for a promotion, where the caller picks the piece). Never empty. */
    data class Moves(val candidates: List<Move>) : TapResult

    /** Nothing to do (tapped an empty/enemy square with no piece selected). */
    data object Ignore : TapResult
}

/**
 * The tap-to-move state machine shared by every playable board (Play, puzzles, lessons, review
 * retry, …). The selection bookkeeping — "first tap picks a piece, second tap moves it or reselects"
 * — was copy-pasted into seven screens, differing only in whose pieces may move and what to do once
 * a move is chosen. That prefix now lives here as pure, tested logic; screens supply [mover] and act
 * on the [TapResult]'s tail (apply / grade / open a promotion dialog).
 */
object MoveSelection {
    fun onTap(position: Position, selected: Square?, tapped: Square, mover: PieceColor): TapResult {
        if (selected == null) {
            return if (position.pieceAt(tapped)?.color == mover) TapResult.Select(tapped) else TapResult.Ignore
        }
        if (tapped == selected) return TapResult.Clear
        val candidates = position.legalMoves().filter { it.from == selected && it.to == tapped }
        if (candidates.isEmpty()) {
            // Tapped elsewhere: switch selection to another of my pieces, otherwise deselect.
            return if (position.pieceAt(tapped)?.color == mover) TapResult.Select(tapped) else TapResult.Clear
        }
        return TapResult.Moves(candidates)
    }
}
