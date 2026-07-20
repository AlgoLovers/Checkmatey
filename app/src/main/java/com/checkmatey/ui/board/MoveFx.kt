package com.checkmatey.ui.board

import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position
import com.checkmatey.sound.Sfx

/**
 * All the sensory feedback for one applied move — the resulting position, the capture pop, the named
 * callout, and the sound — computed in one place so every "play a move" surface (Play, review retry,
 * …) reacts identically instead of each re-deriving it. [after] is the position once [move] is
 * applied, so callers don't apply it twice.
 */
data class MoveFeedback(
    val after: Position,
    val capture: CaptureFx?,
    val note: CaptureNote?,
    val sfx: Sfx,
)

/**
 * Derive the feedback for [move] played from [before] by the side to move, seen from [myColor]'s
 * point of view (drives win/lose and "I took it" vs "I lost it"). [prevCounter] is the last
 * CaptureFx/CaptureNote counter, so repeated captures on the same square still re-fire the effects.
 */
fun moveFeedback(before: Position, move: Move, myColor: PieceColor, prevCounter: Int): MoveFeedback {
    val mover = before.sideToMove
    val victim = before.pieceAt(move.to)?.type ?: if (move.isEnPassant) PieceType.PAWN else null
    val after = before.applyMove(move)
    val counter = prevCounter + 1
    val sfx = when {
        after.isCheckmate() -> if (after.sideToMove == myColor) Sfx.LOSE else Sfx.WIN
        after.isInCheck() -> Sfx.CHECK
        victim != null -> Sfx.forCapture(victim.letter)
        else -> Sfx.MOVE
    }
    return MoveFeedback(
        after = after,
        capture = victim?.let { CaptureFx(move.to, it, counter) },
        note = victim?.let { CaptureNote(it, byMe = mover == myColor, counter) },
        sfx = sfx,
    )
}
